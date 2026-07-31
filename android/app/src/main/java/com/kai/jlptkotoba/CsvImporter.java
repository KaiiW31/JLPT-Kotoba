package com.kai.jlptkotoba;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class CsvImporter {
    private CsvImporter() {
    }

    static List<Word> parse(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("The CSV file is empty.");
            }
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            List<String> headers = parseLine(headerLine);
            Map<String, Integer> columns = resolveColumns(headers);
            if (!columns.containsKey("furigana")
                    || !columns.containsKey("romaji")
                    || !columns.containsKey("meaning")) {
                throw new IOException("CSV needs furigana, romaji, and meaning columns.");
            }

            List<Word> words = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> values = parseLine(line);
                Word word = new Word(
                        value(values, columns.get("kanji")),
                        value(values, columns.get("furigana")),
                        value(values, columns.get("romaji")),
                        value(values, columns.get("meaning")),
                        true
                );
                if (!word.furigana.isEmpty() && !word.romaji.isEmpty() && !word.meaning.isEmpty()) {
                    words.add(word);
                }
            }
            if (words.isEmpty()) {
                throw new IOException("No valid vocabulary rows were found.");
            }
            return words;
        }
    }

    static List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(value.toString().trim());
                value.setLength(0);
            } else {
                value.append(character);
            }
        }
        values.add(value.toString().trim());
        return values;
    }

    private static Map<String, Integer> resolveColumns(List<String> headers) {
        Map<String, Integer> columns = new HashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            String header = normalize(headers.get(index));
            if (matches(header, "kanji", "word", "vocab", "vocabulary", "term", "japanese")) {
                columns.putIfAbsent("kanji", index);
            } else if (matches(header, "furigana", "kana", "reading", "hiragana")) {
                columns.putIfAbsent("furigana", index);
            } else if (matches(header, "romaji", "romanji", "romanization", "romanisation")) {
                columns.putIfAbsent("romaji", index);
            } else if (matches(header, "meaning", "english", "definition", "translation")) {
                columns.putIfAbsent("meaning", index);
            }
        }
        return columns;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private static boolean matches(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String value(List<String> values, Integer index) {
        return index == null || index < 0 || index >= values.size() ? "" : values.get(index);
    }
}
