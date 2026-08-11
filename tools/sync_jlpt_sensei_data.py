"""Build JLPT Kotoba's offline study dataset from the user-selected JLPT Sensei lists."""

from __future__ import annotations

import argparse
import json
import re
import shutil
import time
import unicodedata
from html.parser import HTMLParser
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[1]
DESKTOP_OUTPUT = ROOT / "desktop" / "data" / "study_data.json"
ANDROID_OUTPUT = ROOT / "android" / "app" / "src" / "main" / "assets" / "study_data.json"
BASE_URL = "https://jlptsensei.com"
LEVELS = ("N5", "N4", "N3", "N2", "N1")
VOCABULARY_LEVELS = LEVELS[:-1]
USER_AGENT = "JLPT-Kotoba-data-sync/1.0 (+offline personal study app)"


class JLPTTableParser(HTMLParser):
    def __init__(self, table_id: str):
        super().__init__(convert_charrefs=True)
        self.table_id = table_id
        self.in_table = False
        self.in_body = False
        self.in_paragraph = False
        self.current_cell: dict[str, list[str]] | None = None
        self.current_row: list[dict[str, str]] | None = None
        self.current_url = ""
        self.rows: list[tuple[list[dict[str, str]], str]] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attributes = dict(attrs)
        if tag == "table" and attributes.get("id") == self.table_id:
            self.in_table = True
            return
        if not self.in_table:
            return
        if tag == "tbody":
            self.in_body = True
        elif tag == "tr" and self.in_body:
            self._finish_row()
            self.current_row = []
            self.current_url = ""
        elif tag == "td" and self.in_body:
            self._finish_cell()
            self.current_cell = {"main": [], "sub": []}
        elif tag == "p" and self.current_cell is not None:
            self.in_paragraph = True
        elif tag == "a" and self.current_row is not None and not self.current_url:
            self.current_url = attributes.get("href") or ""

    def handle_endtag(self, tag: str) -> None:
        if not self.in_table:
            return
        if tag == "p":
            self.in_paragraph = False
        elif tag == "td":
            self._finish_cell()
        elif tag == "tr" and self.in_body:
            self._finish_row()
        elif tag == "tbody":
            self._finish_row()
            self.in_body = False
        elif tag == "table":
            self._finish_row()
            self.in_table = False

    def handle_data(self, data: str) -> None:
        if self.current_cell is None:
            return
        bucket = "sub" if self.in_paragraph else "main"
        self.current_cell[bucket].append(data)

    def _finish_cell(self) -> None:
        if self.current_cell is None or self.current_row is None:
            return
        self.current_row.append(
            {
                "main": clean("".join(self.current_cell["main"])),
                "sub": clean("".join(self.current_cell["sub"])),
            }
        )
        self.current_cell = None
        self.in_paragraph = False

    def _finish_row(self) -> None:
        self._finish_cell()
        if self.current_row:
            self.rows.append((self.current_row, self.current_url))
        self.current_row = None
        self.current_url = ""


def clean(value: str) -> str:
    value = unicodedata.normalize("NFKC", value)
    value = value.replace("\u200b", "").replace("\ufeff", "")
    return re.sub(r"\s+", " ", value).strip()


def absolute_url(value: str) -> str:
    if value.startswith("//"):
        return "https:" + value
    if value.startswith("/"):
        return BASE_URL + value
    return value


def fetch(url: str) -> str:
    request = Request(url, headers={"User-Agent": USER_AGENT})
    with urlopen(request, timeout=45) as response:
        return response.read().decode("utf-8", "replace")


def parse_table(html: str, table_id: str) -> list[tuple[list[dict[str, str]], str]]:
    parser = JLPTTableParser(table_id)
    parser.feed(html)
    return parser.rows


def list_page_url(section: str, level: str, page: int) -> str:
    slug = f"jlpt-{level.lower()}-{section}-list"
    return f"{BASE_URL}/{slug}/" if page == 1 else f"{BASE_URL}/{slug}/page/{page}/"


def paginated_rows(
    section: str,
    level: str,
    table_id: str,
) -> list[tuple[list[dict[str, str]], str]]:
    result: list[tuple[list[dict[str, str]], str]] = []
    for page in range(1, 30):
        url = list_page_url(section, level, page)
        try:
            rows = parse_table(fetch(url), table_id)
        except HTTPError as error:
            if error.code == 404 and page > 1:
                break
            raise
        if not rows:
            break
        result.extend(rows)
        time.sleep(0.15)
    return result


def parse_vocabulary(level: str) -> list[dict[str, str]]:
    result: list[dict[str, str]] = []
    seen: set[tuple[str, str, str]] = set()
    for cells, source_url in paginated_rows("vocabulary", level, "jl-vocab"):
        if len(cells) < 5 or not cells[0]["main"].isdigit():
            continue
        word = cells[1]["main"]
        romaji = cells[2]["main"]
        reading = cells[2]["sub"] or word
        key = (word, reading, romaji)
        if not word or key in seen:
            continue
        seen.add(key)
        result.append(
            {
                "word": word,
                "reading": reading,
                "romaji": romaji,
                "type": cells[3]["main"],
                "meaning": cells[4]["main"],
                "source_url": absolute_url(source_url),
            }
        )
    return result


def parse_grammar(level: str) -> list[dict[str, str]]:
    result: list[dict[str, str]] = []
    for cells, source_url in paginated_rows("grammar", level, "jl-grammar"):
        if len(cells) < 4 or not cells[0]["main"].isdigit():
            continue
        result.append(
            {
                "romaji": cells[1]["main"],
                "pattern": cells[2]["main"],
                "meaning": cells[3]["main"],
                "source_url": absolute_url(source_url),
            }
        )
    return result


def reading(cell: dict[str, str]) -> str:
    if cell["sub"] and cell["main"]:
        return f'{cell["main"]} · {cell["sub"]}'
    return cell["main"] or cell["sub"]


def parse_kanji(level: str) -> list[dict[str, str]]:
    result: list[dict[str, str]] = []
    for cells, source_url in paginated_rows("kanji", level, "jl-kanji"):
        if len(cells) < 5 or not cells[0]["main"].isdigit():
            continue
        result.append(
            {
                "kanji": cells[1]["main"],
                "onyomi": reading(cells[2]),
                "kunyomi": reading(cells[3]),
                "meaning": cells[4]["main"],
                "source_url": absolute_url(source_url),
            }
        )
    return result


def build_dataset() -> dict[str, object]:
    vocabulary = {level: parse_vocabulary(level) for level in VOCABULARY_LEVELS}
    grammar = {level: parse_grammar(level) for level in LEVELS}
    kanji = {level: parse_kanji(level) for level in LEVELS}
    return {
        "schema_version": 1,
        "source": {
            "name": "JLPT Sensei",
            "url": BASE_URL,
            "note": "Unofficial study lists selected by the app owner; the JLPT publishes no official vocabulary, grammar, or kanji lists.",
        },
        "vocabulary": vocabulary,
        "grammar": grammar,
        "kanji": kanji,
    }


def validate(dataset: dict[str, object]) -> None:
    expected_minimums = {
        "vocabulary": {"N5": 640, "N4": 570, "N3": 190, "N2": 95},
        "grammar": {"N5": 80, "N4": 130, "N3": 180, "N2": 195, "N1": 250},
        "kanji": {"N5": 80, "N4": 165, "N3": 365, "N2": 370, "N1": 1500},
    }
    for section, levels in expected_minimums.items():
        section_data = dataset[section]
        assert isinstance(section_data, dict)
        for level, minimum in levels.items():
            count = len(section_data[level])
            if count < minimum:
                raise ValueError(f"{section} {level}: expected at least {minimum}, got {count}")

    for section in ("vocabulary", "grammar", "kanji"):
        section_data = dataset[section]
        assert isinstance(section_data, dict)
        for level, items in section_data.items():
            serialized = [json.dumps(item, ensure_ascii=False, sort_keys=True) for item in items]
            if len(serialized) != len(set(serialized)):
                raise ValueError(f"duplicate {section} entries found in {level}")


def write_dataset(dataset: dict[str, object]) -> None:
    DESKTOP_OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(dataset, ensure_ascii=False, indent=2) + "\n"
    DESKTOP_OUTPUT.write_text(text, encoding="utf-8")
    ANDROID_OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(DESKTOP_OUTPUT, ANDROID_OUTPUT)


def print_summary(dataset: dict[str, object]) -> None:
    for section in ("vocabulary", "grammar", "kanji"):
        levels = dataset[section]
        assert isinstance(levels, dict)
        counts = ", ".join(f"{level}={len(items)}" for level, items in levels.items())
        print(f"{section}: {counts}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check-only", action="store_true")
    args = parser.parse_args()
    dataset = build_dataset()
    validate(dataset)
    print_summary(dataset)
    if not args.check_only:
        write_dataset(dataset)


if __name__ == "__main__":
    main()
