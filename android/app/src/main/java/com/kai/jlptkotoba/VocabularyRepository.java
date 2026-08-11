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
    private static final String REMOVED_PREFIX = "removed_words_";

    private final Context context;
    private final SharedPreferences preferences;
    private final Map<String, List<Word>> baseWords = new HashMap<>();
    private final Map<String, Map<String, List<StudyRow>>> referenceRows = new HashMap<>();

    VocabularyRepository(Context context, SharedPreferences preferences) throws IOException, JSONException {
        this.context = context;
        this.preferences = preferences;
        loadBaseVocabulary();
    }

    List<Word> words(String level) {
        Set<String> removed = new HashSet<>(preferences.getStringSet(
                REMOVED_PREFIX + level,
                Collections.emptySet()
        ));
        List<Word> result = new ArrayList<>();
        for (Word word : baseWords.getOrDefault(level, Collections.emptyList())) {
            if (!removed.contains(word.contentKey())) {
                result.add(word);
            }
        }
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
                    word.type,
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

    void replaceWord(String level, Word target, Word replacement) {
        List<Word> custom = customWords(level);
        if (target != null) {
            if (target.custom) {
                custom.removeIf(word -> word.contentKey().equals(target.contentKey()));
            } else {
                Set<String> removed = new HashSet<>(preferences.getStringSet(
                        REMOVED_PREFIX + level,
                        Collections.emptySet()
                ));
                removed.add(target.contentKey());
                preferences.edit().putStringSet(REMOVED_PREFIX + level, removed).apply();
            }
        }
        custom.add(new Word(
                replacement.kanji,
                replacement.furigana,
                replacement.romaji,
                replacement.type,
                replacement.meaning,
                true
        ));
        saveCustomWords(level, custom);
    }

    List<StudyRow> studyRows(String section, String level) {
        Map<String, List<StudyRow>> levels = referenceRows.get(section);
        if (levels == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(levels.getOrDefault(level, Collections.emptyList()));
    }

    private void loadBaseVocabulary() throws IOException, JSONException {
        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open("study_data.json"),
                StandardCharsets.UTF_8
        ))) {
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        }

        JSONObject root = new JSONObject(json.toString());
        JSONObject levels = root.getJSONObject("vocabulary");
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

        loadReferenceRows(root, "grammar");
        loadReferenceRows(root, "kanji");
    }

    private void loadReferenceRows(JSONObject root, String section) throws JSONException {
        JSONObject levelsObject = root.getJSONObject(section);
        Map<String, List<StudyRow>> levels = new HashMap<>();
        for (String level : MainActivity.LEVELS) {
            JSONArray array = levelsObject.optJSONArray(level);
            List<StudyRow> rows = new ArrayList<>();
            if (array != null) {
                for (int index = 0; index < array.length(); index++) {
                    JSONObject item = array.getJSONObject(index);
                    rows.add("grammar".equals(section)
                            ? StudyRow.grammar(item)
                            : StudyRow.kanji(item));
                }
            }
            levels.put(level, rows);
        }
        referenceRows.put(section, levels);
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
