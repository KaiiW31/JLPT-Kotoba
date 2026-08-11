package com.kai.jlptkotoba;

import org.json.JSONObject;

import java.util.Locale;

final class StudyRow {
    final String[] values;

    StudyRow(String... values) {
        this.values = values;
    }

    static StudyRow grammar(JSONObject item) {
        return new StudyRow(
                item.optString("pattern"),
                item.optString("romaji"),
                item.optString("meaning")
        );
    }

    static StudyRow kanji(JSONObject item) {
        return new StudyRow(
                item.optString("kanji"),
                item.optString("onyomi"),
                item.optString("kunyomi"),
                item.optString("meaning")
        );
    }

    boolean matches(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return true;
        }
        return String.join(" ", values).toLowerCase(Locale.ROOT).contains(normalized);
    }
}
