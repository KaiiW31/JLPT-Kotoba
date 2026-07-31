package com.kai.jlptkotoba;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MainActivity extends Activity {
    static final String[] LEVELS = {"N5", "N4", "N3", "N2", "N1"};

    private static final int IMPORT_CSV_REQUEST = 41;
    private static final String PREFS_NAME = "jlpt_kotoba";
    private static final String PREF_DARK_MODE = "dark_mode";
    private static final String PREF_LEVEL = "current_level";
    private static final String PREF_MODE = "current_mode";

    private SharedPreferences preferences;
    private VocabularyRepository repository;
    private boolean darkMode;
    private String currentLevel;
    private String currentMode;
    private String searchQuery = "";

    private LinearLayout root;
    private LinearLayout content;
    private TextView vocabularyCount;
    private ListView vocabularyList;
    private WordAdapter wordAdapter;

    private List<Word> flashDeck = new ArrayList<>();
    private int flashIndex = -1;
    private boolean flashAnswerVisible;
    private boolean flashComplete;
    private String loadedFlashLevel;

    private int paper;
    private int panel;
    private int ink;
    private int muted;
    private int line;
    private int accent;
    private int accentHover;
    private int topbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        darkMode = preferences.getBoolean(PREF_DARK_MODE, false);
        currentLevel = preferences.getString(PREF_LEVEL, "N5");
        currentMode = preferences.getString(PREF_MODE, "vocabulary");
        if (!isLevel(currentLevel)) {
            currentLevel = "N5";
        }
        if (!"flashcards".equals(currentMode)) {
            currentMode = "vocabulary";
        }

        try {
            repository = new VocabularyRepository(this, preferences);
            buildInterface();
        } catch (Exception error) {
            showFatalError(error);
        }
    }

    private void buildInterface() {
        applyPalette();
        configureWindow();

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(paper);
        setContentView(root);

        root.addView(buildToolbar(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(buildModeSelector(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(buildLevelSelector(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
        ));

        FrameLayout contentHost = new FrameLayout(this);
        contentHost.setPadding(dp(16), dp(12), dp(16), dp(14));
        root.addView(contentHost, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        contentHost.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        if ("flashcards".equals(currentMode)) {
            renderFlashcards();
        } else {
            renderVocabulary();
        }
    }

    private View buildToolbar() {
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(20), dp(14), dp(14), dp(12));
        toolbar.setBackgroundColor(topbar);

        TextView title = label("JLPT Kotoba", 25, Color.rgb(255, 247, 237), Typeface.BOLD);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1));

        Button theme = button(darkMode ? "Light" : "Dark", false, false);
        theme.setOnClickListener(view -> {
            darkMode = !darkMode;
            preferences.edit().putBoolean(PREF_DARK_MODE, darkMode).apply();
            buildInterface();
        });
        LinearLayout.LayoutParams themeParams = new LinearLayout.LayoutParams(dp(84), dp(44));
        themeParams.setMargins(dp(8), 0, 0, 0);
        toolbar.addView(theme, themeParams);
        return toolbar;
    }

    private View buildModeSelector() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(16), dp(10), dp(16), dp(4));
        row.setBackgroundColor(topbar);

        Button vocabulary = button("Vocabulary", "vocabulary".equals(currentMode), true);
        vocabulary.setOnClickListener(view -> selectMode("vocabulary"));
        row.addView(vocabulary, weightedButtonParams(0, dp(4)));

        Button flashcards = button("Flashcards", "flashcards".equals(currentMode), true);
        flashcards.setOnClickListener(view -> selectMode("flashcards"));
        row.addView(flashcards, weightedButtonParams(dp(4), 0));
        return row;
    }

    private View buildLevelSelector() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setFillViewport(true);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setBackgroundColor(topbar);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(dp(12), dp(4), dp(12), dp(9));

        for (String level : LEVELS) {
            Button item = button(level, level.equals(currentLevel), true);
            item.setOnClickListener(view -> selectLevel(level));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(68), dp(42));
            params.setMargins(dp(3), 0, dp(3), 0);
            row.addView(item, params);
        }
        scroll.addView(row);
        return scroll;
    }

    private void selectMode(String mode) {
        if (mode.equals(currentMode)) {
            return;
        }
        hideKeyboard();
        currentMode = mode;
        preferences.edit().putString(PREF_MODE, currentMode).apply();
        buildInterface();
    }

    private void selectLevel(String level) {
        if (level.equals(currentLevel)) {
            return;
        }
        hideKeyboard();
        currentLevel = level;
        loadedFlashLevel = null;
        preferences.edit().putString(PREF_LEVEL, currentLevel).apply();
        buildInterface();
    }

    private void renderVocabulary() {
        List<Word> allWords = repository.words(currentLevel);

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout headingText = new LinearLayout(this);
        headingText.setOrientation(LinearLayout.VERTICAL);
        headingText.addView(label("JLPT " + currentLevel + " Vocabulary", 25, ink, Typeface.BOLD));
        headingText.addView(label(
                allWords.isEmpty() ? "This level is coming soon." : "Search or browse the complete list.",
                14,
                muted,
                Typeface.NORMAL
        ));
        heading.addView(headingText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button importButton = button("Import CSV", false, false);
        importButton.setOnClickListener(view -> openCsvPicker());
        LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(dp(112), dp(44));
        importParams.setMargins(dp(8), 0, 0, 0);
        heading.addView(importButton, importParams);
        content.addView(heading);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setText(searchQuery);
        search.setSelection(search.getText().length());
        search.setHint("Search kanji, kana, romaji, or meaning");
        search.setHintTextColor(muted);
        search.setTextColor(ink);
        search.setTextSize(15);
        search.setPadding(dp(15), 0, dp(15), 0);
        search.setBackground(roundedBackground(panel, line, 10));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        searchParams.setMargins(0, dp(14), 0, dp(8));
        content.addView(search, searchParams);

        vocabularyCount = label("", 13, muted, Typeface.BOLD);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        countParams.setMargins(dp(2), 0, 0, dp(7));
        content.addView(vocabularyCount, countParams);

        vocabularyList = new ListView(this);
        vocabularyList.setDivider(new ColorDrawable(line));
        vocabularyList.setDividerHeight(1);
        vocabularyList.setBackground(roundedBackground(panel, line, 12));
        vocabularyList.setClipToOutline(true);
        content.addView(vocabularyList, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        updateVocabularyFilter(allWords, searchQuery);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                searchQuery = value.toString();
                updateVocabularyFilter(allWords, searchQuery);
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });

        vocabularyList.setOnItemLongClickListener((parent, view, position, id) -> {
            Word selected = wordAdapter.getItem(position);
            if (!selected.custom) {
                Toast.makeText(this, "Bundled words cannot be deleted.", Toast.LENGTH_SHORT).show();
                return true;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Delete custom word?")
                    .setMessage(displayWord(selected))
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, which) -> {
                        repository.deleteCustomWord(currentLevel, selected);
                        renderCurrentScreen();
                        Toast.makeText(this, "Custom word deleted.", Toast.LENGTH_SHORT).show();
                    })
                    .show();
            return true;
        });
    }

    private void updateVocabularyFilter(List<Word> allWords, String query) {
        List<Word> filtered = new ArrayList<>();
        for (Word word : allWords) {
            if (word.matches(query)) {
                filtered.add(word);
            }
        }
        wordAdapter = new WordAdapter(filtered);
        vocabularyList.setAdapter(wordAdapter);
        vocabularyCount.setText(filtered.isEmpty() ? "Coming soon" : filtered.size() + " words");
    }

    private void renderFlashcards() {
        ensureFlashState();
        List<Word> available = repository.words(currentLevel);

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.addView(label("JLPT " + currentLevel + " Flashcards", 25, ink, Typeface.BOLD));
        heading.addView(label(
                available.isEmpty() ? "This deck is coming soon." : "Tap the card to reveal and continue.",
                14,
                muted,
                Typeface.NORMAL
        ));
        content.addView(heading);

        TextView progress = label(flashProgressText(available.size()), 13, muted, Typeface.BOLD);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        progressParams.setMargins(0, dp(12), 0, dp(8));
        content.addView(progress, progressParams);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(24), dp(26), dp(24), dp(26));
        card.setBackground(roundedBackground(panel, line, 18));
        card.setOnClickListener(view -> handleCardTap(available));
        content.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));
        populateCard(card, available);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
        );
        actionsParams.setMargins(0, dp(10), 0, 0);
        content.addView(actions, actionsParams);

        if (flashDeck.isEmpty()) {
            Button start = button(available.isEmpty() ? "No cards yet" : "Start Deck", true, false);
            start.setEnabled(!available.isEmpty());
            start.setAlpha(available.isEmpty() ? 0.55f : 1f);
            start.setOnClickListener(view -> startDeck(available));
            actions.addView(start, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(50)
            ));
        } else {
            Button back = button("Back", false, false);
            back.setEnabled(flashIndex > 0 || flashComplete);
            back.setAlpha(back.isEnabled() ? 1f : 0.55f);
            back.setOnClickListener(view -> previousCard());
            actions.addView(back, weightedButtonParams(0, dp(4)));

            Button primary = button(
                    flashComplete ? "Keep Completed" : (flashAnswerVisible ? "Next" : "Reveal"),
                    true,
                    false
            );
            primary.setOnClickListener(view -> {
                if (flashComplete) {
                    Toast.makeText(this, "Progress is saved automatically.", Toast.LENGTH_SHORT).show();
                } else if (flashAnswerVisible) {
                    nextCard();
                } else {
                    flashAnswerVisible = true;
                    saveFlashState();
                    renderCurrentScreen();
                }
            });
            actions.addView(primary, weightedButtonParams(dp(4), dp(4)));

            Button restart = button("Restart", false, false);
            restart.setOnClickListener(view -> confirmRestart(available));
            actions.addView(restart, weightedButtonParams(dp(4), 0));
        }
    }

    private void populateCard(LinearLayout card, List<Word> available) {
        if (available.isEmpty()) {
            card.addView(label("JLPT " + currentLevel, 34, ink, Typeface.BOLD));
            card.addView(spacedLabel("Coming soon", 17, muted, Typeface.NORMAL, 10));
            return;
        }
        if (flashDeck.isEmpty()) {
            card.addView(label("JLPT " + currentLevel, 34, ink, Typeface.BOLD));
            card.addView(spacedLabel("Ready", 18, muted, Typeface.NORMAL, 10));
            card.addView(spacedLabel(available.size() + " cards", 14, muted, Typeface.NORMAL, 5));
            return;
        }
        if (flashComplete) {
            card.addView(label("Deck completed!", 31, ink, Typeface.BOLD));
            card.addView(spacedLabel(
                    "You finished all " + flashDeck.size() + " flashcards.",
                    17,
                    muted,
                    Typeface.NORMAL,
                    12
            ));
            return;
        }

        Word word = flashDeck.get(flashIndex);
        String front = word.kanji.isEmpty() ? word.furigana : word.kanji;
        card.addView(label(front, 42, ink, Typeface.BOLD));
        if (flashAnswerVisible) {
            if (!word.kanji.isEmpty()) {
                card.addView(spacedLabel(word.furigana, 24, ink, Typeface.NORMAL, 14));
            }
            card.addView(spacedLabel(word.romaji, 18, muted, Typeface.NORMAL, 8));
            card.addView(spacedLabel(word.meaning, 21, ink, Typeface.BOLD, 12));
        } else {
            card.addView(spacedLabel("Tap to reveal", 15, muted, Typeface.NORMAL, 14));
        }
    }

    private void handleCardTap(List<Word> available) {
        if (available.isEmpty()) {
            return;
        }
        if (flashDeck.isEmpty()) {
            startDeck(available);
        } else if (!flashComplete && !flashAnswerVisible) {
            flashAnswerVisible = true;
            saveFlashState();
            renderCurrentScreen();
        } else if (!flashComplete) {
            nextCard();
        }
    }

    private void startDeck(List<Word> available) {
        flashDeck = new ArrayList<>(available);
        Collections.shuffle(flashDeck);
        flashIndex = 0;
        flashAnswerVisible = false;
        flashComplete = false;
        loadedFlashLevel = currentLevel;
        saveFlashState();
        renderCurrentScreen();
    }

    private void previousCard() {
        if (flashDeck.isEmpty()) {
            return;
        }
        if (flashComplete) {
            flashComplete = false;
            flashIndex = flashDeck.size() - 1;
        } else {
            flashIndex = Math.max(0, flashIndex - 1);
        }
        flashAnswerVisible = false;
        saveFlashState();
        renderCurrentScreen();
    }

    private void nextCard() {
        if (flashDeck.isEmpty() || flashComplete) {
            return;
        }
        if (flashIndex >= flashDeck.size() - 1) {
            flashComplete = true;
            flashAnswerVisible = true;
        } else {
            flashIndex++;
            flashAnswerVisible = false;
        }
        saveFlashState();
        renderCurrentScreen();
    }

    private void confirmRestart(List<Word> available) {
        new AlertDialog.Builder(this)
                .setTitle("Restart this deck?")
                .setMessage("Your saved position for JLPT " + currentLevel + " will be replaced.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Restart", (dialog, which) -> startDeck(available))
                .show();
    }

    private void ensureFlashState() {
        if (currentLevel.equals(loadedFlashLevel)) {
            return;
        }
        flashDeck = new ArrayList<>();
        flashIndex = -1;
        flashAnswerVisible = false;
        flashComplete = false;
        loadedFlashLevel = currentLevel;

        List<Word> available = repository.words(currentLevel);
        Map<String, Word> byId = new HashMap<>();
        for (Word word : available) {
            byId.put(word.id(), word);
        }

        String rawDeck = preferences.getString("flash_deck_" + currentLevel, "[]");
        try {
            JSONArray ids = new JSONArray(rawDeck);
            for (int index = 0; index < ids.length(); index++) {
                Word word = byId.get(ids.getString(index));
                if (word != null) {
                    flashDeck.add(word);
                }
            }
        } catch (JSONException ignored) {
            flashDeck.clear();
        }

        if (!flashDeck.isEmpty()) {
            flashIndex = Math.min(
                    Math.max(preferences.getInt("flash_index_" + currentLevel, 0), 0),
                    flashDeck.size() - 1
            );
            flashAnswerVisible = preferences.getBoolean("flash_revealed_" + currentLevel, false);
            flashComplete = preferences.getBoolean("flash_complete_" + currentLevel, false);
        }
    }

    private void saveFlashState() {
        JSONArray ids = new JSONArray();
        for (Word word : flashDeck) {
            ids.put(word.id());
        }
        preferences.edit()
                .putString("flash_deck_" + currentLevel, ids.toString())
                .putInt("flash_index_" + currentLevel, Math.max(0, flashIndex))
                .putBoolean("flash_revealed_" + currentLevel, flashAnswerVisible)
                .putBoolean("flash_complete_" + currentLevel, flashComplete)
                .apply();
    }

    private String flashProgressText(int availableCount) {
        if (availableCount == 0) {
            return "0 cards";
        }
        if (flashDeck.isEmpty()) {
            return availableCount + " cards • progress saves automatically";
        }
        if (flashComplete) {
            return flashDeck.size() + " / " + flashDeck.size() + " completed";
        }
        return (flashIndex + 1) + " / " + flashDeck.size() + " • saved automatically";
    }

    private void renderCurrentScreen() {
        hideKeyboard();
        content.removeAllViews();
        if ("flashcards".equals(currentMode)) {
            renderFlashcards();
        } else {
            renderVocabulary();
        }
    }

    private void openCsvPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, IMPORT_CSV_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != IMPORT_CSV_REQUEST || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IllegalStateException("The selected file could not be opened.");
            }
            List<Word> imported = CsvImporter.parse(input);
            int added = repository.addCustomWords(currentLevel, imported);
            loadedFlashLevel = null;
            searchQuery = "";
            renderCurrentScreen();
            Toast.makeText(
                    this,
                    added == 0 ? "Those words already exist." : "Imported " + added + " words.",
                    Toast.LENGTH_LONG
            ).show();
        } catch (Exception error) {
            new AlertDialog.Builder(this)
                    .setTitle("Import failed")
                    .setMessage(error.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void applyPalette() {
        if (darkMode) {
            paper = Color.rgb(18, 13, 19);
            panel = Color.rgb(33, 23, 32);
            ink = Color.rgb(255, 243, 232);
            muted = Color.rgb(212, 183, 186);
            line = Color.rgb(106, 58, 71);
            accent = Color.rgb(161, 29, 38);
            accentHover = Color.rgb(95, 17, 17);
            topbar = Color.rgb(74, 11, 20);
        } else {
            paper = Color.rgb(255, 249, 241);
            panel = Color.rgb(255, 253, 248);
            ink = Color.rgb(36, 25, 29);
            muted = Color.rgb(118, 94, 100);
            line = Color.rgb(232, 206, 194);
            accent = Color.rgb(185, 28, 28);
            accentHover = Color.rgb(153, 27, 27);
            topbar = Color.rgb(143, 23, 23);
        }
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(topbar);
        window.setNavigationBarColor(paper);
        window.getDecorView().setSystemUiVisibility(
                darkMode ? 0 : View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );
    }

    private Button button(String text, boolean active, boolean segmented) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
        button.setTextColor(active || segmented ? Color.rgb(255, 247, 237) : ink);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(10), 0, dp(10), 0);
        int background = active ? accent : (segmented ? accentHover : panel);
        button.setBackground(roundedBackground(background, active || segmented ? Color.rgb(248, 113, 113) : line, 9));
        return button;
    }

    private TextView label(String text, float size, int color, int style) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(size);
        label.setTextColor(color);
        label.setTypeface(Typeface.DEFAULT, style);
        label.setGravity(Gravity.CENTER_VERTICAL);
        return label;
    }

    private TextView spacedLabel(String text, float size, int color, int style, int topMargin) {
        TextView label = label(text, size, color, style);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(topMargin), 0, 0);
        label.setLayoutParams(params);
        return label;
    }

    private GradientDrawable roundedBackground(int background, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(background);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams weightedButtonParams(int leftMargin, int rightMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1);
        params.setMargins(leftMargin, 0, rightMargin, 0);
        return params;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean isLevel(String value) {
        for (String level : LEVELS) {
            if (level.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private String displayWord(Word word) {
        String front = word.kanji.isEmpty() ? word.furigana : word.kanji + " (" + word.furigana + ")";
        return front + " — " + word.meaning;
    }

    private void hideKeyboard() {
        View focused = getCurrentFocus();
        if (focused == null) {
            return;
        }
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        focused.clearFocus();
    }

    private void showFatalError(Exception error) {
        TextView message = new TextView(this);
        message.setPadding(dp(24), dp(24), dp(24), dp(24));
        message.setText(getString(R.string.vocabulary_load_error, error.getMessage()));
        message.setTextSize(17);
        message.setTextColor(Color.rgb(185, 28, 28));
        setContentView(message);
    }

    private final class WordAdapter extends BaseAdapter {
        private final List<Word> words;

        WordAdapter(List<Word> words) {
            this.words = words;
        }

        @Override
        public int getCount() {
            return words.size();
        }

        @Override
        public Word getItem(int position) {
            return words.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Word word = getItem(position);
            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(16), dp(12), dp(16), dp(12));
            row.setBackgroundColor(panel);

            LinearLayout topLine = new LinearLayout(MainActivity.this);
            topLine.setOrientation(LinearLayout.HORIZONTAL);
            topLine.setGravity(Gravity.CENTER_VERTICAL);

            String titleText = word.kanji.isEmpty() ? word.furigana : word.kanji;
            TextView title = label(titleText, 21, ink, Typeface.BOLD);
            topLine.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            if (word.custom) {
                TextView badge = label("CUSTOM", 10, accent, Typeface.BOLD);
                badge.setGravity(Gravity.CENTER);
                badge.setPadding(dp(8), dp(3), dp(8), dp(3));
                badge.setBackground(roundedBackground(darkMode ? Color.rgb(50, 21, 29) : Color.rgb(255, 241, 242), accent, 8));
                topLine.addView(badge);
            }
            row.addView(topLine);

            String reading = word.kanji.isEmpty()
                    ? word.romaji
                    : word.furigana + "  •  " + word.romaji;
            row.addView(spacedLabel(reading, 14, muted, Typeface.NORMAL, 4));

            TextView meaning = spacedLabel(word.meaning, 16, ink, Typeface.NORMAL, 6);
            meaning.setGravity(Gravity.START);
            row.addView(meaning);
            return row;
        }
    }
}
