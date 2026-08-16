package com.kai.jlptkotoba;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

final class SavedItem {
    final String section;
    final String level;
    final String kana;
    final String[] values;

    SavedItem(String section, String level, String kana, String... values) {
        this.section = clean(section);
        this.level = clean(level);
        this.kana = clean(kana);
        this.values = values == null ? new String[0] : values.clone();
        for (int index = 0; index < this.values.length; index++) {
            this.values[index] = clean(this.values[index]);
        }
    }

    static SavedItem vocabulary(String level, String kana, Word word) {
        return new SavedItem(
                "vocabulary",
                level,
                kana,
                word.kanji,
                word.furigana,
                word.romaji,
                word.type,
                word.meaning
        );
    }

    static SavedItem study(String section, String level, String kana, StudyRow row) {
        return new SavedItem(section, level, kana, row.values);
    }

    String key() {
        return (section + "\u001f" + level + "\u001f" + String.join("\u001f", values))
                .toLowerCase(Locale.ROOT);
    }

    String displayName() {
        if (values.length == 0) {
            return "Item";
        }
        if ("vocabulary".equals(section) && values[0].isEmpty() && values.length > 1) {
            return values[1];
        }
        return values[0].isEmpty() ? "Item" : values[0];
    }

    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("section", section);
        object.put("level", level);
        object.put("kana", kana);
        JSONArray storedValues = new JSONArray();
        for (String value : values) {
            storedValues.put(value);
        }
        object.put("values", storedValues);
        return object;
    }

    static SavedItem fromJson(JSONObject object) throws JSONException {
        JSONArray storedValues = object.optJSONArray("values");
        if (storedValues == null) {
            throw new JSONException("Saved item has no values");
        }
        String[] values = new String[storedValues.length()];
        for (int index = 0; index < storedValues.length(); index++) {
            values[index] = storedValues.optString(index);
        }
        return new SavedItem(
                object.optString("section"),
                object.optString("level"),
                object.optString("kana"),
                values
        );
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
