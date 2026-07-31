package com.kai.jlptkotoba;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Objects;

final class Word {
    final String kanji;
    final String furigana;
    final String romaji;
    final String meaning;
    final boolean custom;

    Word(String kanji, String furigana, String romaji, String meaning, boolean custom) {
        this.kanji = clean(kanji);
        this.furigana = clean(furigana);
        this.romaji = clean(romaji);
        this.meaning = clean(meaning);
        this.custom = custom;
    }

    String id() {
        return Integer.toHexString(contentKey().hashCode());
    }

    String contentKey() {
        return (kanji + "\u001f" + furigana + "\u001f" + romaji + "\u001f" + meaning)
                .toLowerCase(Locale.ROOT);
    }

    boolean matches(String query) {
        String normalized = clean(query).toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return true;
        }
        return (kanji + " " + furigana + " " + romaji + " " + meaning)
                .toLowerCase(Locale.ROOT)
                .contains(normalized);
    }

    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("kanji", kanji);
        object.put("furigana", furigana);
        object.put("romaji", romaji);
        object.put("meaning", meaning);
        return object;
    }

    static Word fromJson(JSONObject object, boolean custom) {
        return new Word(
                object.optString("kanji"),
                object.optString("furigana"),
                object.optString("romaji"),
                object.optString("meaning"),
                custom
        );
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Word)) {
            return false;
        }
        return contentKey().equals(((Word) other).contentKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentKey());
    }
}
