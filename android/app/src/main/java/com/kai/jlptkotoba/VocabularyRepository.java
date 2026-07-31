package com.kai.jlptkotoba;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class VocabularyRepository {
    private static final String CUSTOM_PREFIX = "custom_words_";

    private final Context context;
    private final SharedPreferences preferences;
    private final Map<String, List<Word>> baseWords = new HashMap<>();

    VocabularyRepository(Context context, SharedPreferences preferences) throws IOException, JSONException {
        this.context = context;
        this.preferences = preferences;
        loadBaseVocabulary();
    }

    List<Word> words(String level) {
        List<Word> result = new ArrayList<>(
                baseWords.getOrDefault(level, Collections.emptyList())
        );
        result.addAll(customWords(level));
        return result;
    }

    int addCustomWords(String level, List<Word> additions) {
        List<Word> custom = customWords(level);
        Set<String> existing = new HashSet<>();
        for (Word word : words(level)) {
            existing.add(word.contentKey());
        }

        int added = 0;
        for (Word word : additions) {
            if (existing.add(word.contentKey())) {
                custom.add(new Word(
                        word.kanji,
                        word.furigana,
                        word.romaji,
                        word.meaning,
                        true
                ));
                added++;
            }
        }
        if (added > 0) {
            saveCustomWords(level, custom);
        }
        return added;
    }

    void deleteCustomWord(String level, Word target) {
        List<Word> custom = customWords(level);
        custom.removeIf(word -> word.contentKey().equals(target.contentKey()));
        saveCustomWords(level, custom);
    }

    private void loadBaseVocabulary() throws IOException, JSONException {
        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open("vocabulary.json"),
                StandardCharsets.UTF_8
        ))) {
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        }

        JSONObject levels = new JSONObject(json.toString()).getJSONObject("levels");
        for (String level : MainActivity.LEVELS) {
            JSONArray array = levels.optJSONArray(level);
            List<Word> words = new ArrayList<>();
            if (array != null) {
                for (int index = 0; index < array.length(); index++) {
                    words.add(Word.fromJson(array.getJSONObject(index), false));
                }
            }
            baseWords.put(level, words);
        }
    }

    private List<Word> customWords(String level) {
        List<Word> words = new ArrayList<>();
        String raw = preferences.getString(CUSTOM_PREFIX + level, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int index = 0; index < array.length(); index++) {
                words.add(Word.fromJson(array.getJSONObject(index), true));
            }
        } catch (JSONException ignored) {
            // A malformed preference should not prevent the bundled vocabulary from opening.
        }
        return words;
    }

    private void saveCustomWords(String level, List<Word> words) {
        JSONArray array = new JSONArray();
        for (Word word : words) {
            try {
                array.put(word.toJson());
            } catch (JSONException ignored) {
                // Word fields are plain strings, so serialization should not fail.
            }
        }
        preferences.edit().putString(CUSTOM_PREFIX + level, array.toString()).apply();
    }
}
