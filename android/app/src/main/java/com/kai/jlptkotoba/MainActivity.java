package com.kai.jlptkotoba;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MainActivity extends Activity {
    static final String[] LEVELS = {"N5", "N4", "N3", "N2", "N1"};
    private static final String[][] KANA_ROWS = {
            {"あ", "い", "う", "え", "お"},
            {"か", "き", "く", "け", "こ"},
            {"さ", "し", "す", "せ", "そ"},
            {"た", "ち", "つ", "て", "と"},
            {"な", "に", "ぬ", "ね", "の"},
            {"は", "ひ", "ふ", "へ", "ほ"},
            {"ま", "み", "む", "め", "も"},
            {"や", "ゆ", "よ"},
            {"ら", "り", "る", "れ", "ろ"},
            {"わ", "を", "ん"}
    };
    private static final String[][] KANA_ROMAJI_ROWS = {
            {"a", "i", "u", "e", "o"},
            {"ka", "ki", "ku", "ke", "ko"},
            {"sa", "shi", "su", "se", "so"},
            {"ta", "chi", "tsu", "te", "to"},
            {"na", "ni", "nu", "ne", "no"},
            {"ha", "hi", "fu", "he", "ho"},
            {"ma", "mi", "mu", "me", "mo"},
            {"ya", "yu", "yo"},
            {"ra", "ri", "ru", "re", "ro"},
            {"wa", "wo", "n"}
    };

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

    private FrameLayout root;
    private LinearLayout mainColumn;
    private LinearLayout topChrome;
    private LinearLayout content;
    private View drawerScrim;
    private LinearLayout drawer;
    private boolean drawerOpen;
    private boolean drawerGestureTracking;
    private boolean drawerGestureStartedOpen;
    private float drawerGestureStartX;
    private float drawerGestureStartY;
    private final Set<String> expandedKanaGroups = new HashSet<>();
    private String activeKana;
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
            registerBackHandler();
        } catch (Exception error) {
            showFatalError(error);
        }
    }

    private void buildInterface() {
        applyPalette();

        root = new FrameLayout(this);
        root.setBackgroundColor(paper);
        setContentView(root);
        configureWindow();

        mainColumn = new LinearLayout(this);
        mainColumn.setOrientation(LinearLayout.VERTICAL);
        mainColumn.setBackgroundColor(paper);
        root.addView(mainColumn, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        topChrome = new LinearLayout(this);
        topChrome.setOrientation(LinearLayout.VERTICAL);
        topChrome.setBackgroundColor(topbar);
        topChrome.addView(buildToolbar(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        mainColumn.addView(topChrome, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        FrameLayout contentHost = new FrameLayout(this);
        contentHost.setPadding(dp(16), dp(12), dp(16), dp(14));
        mainColumn.addView(contentHost, new LinearLayout.LayoutParams(
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

        buildDrawerLayer();
        installSystemBarInsets();
    }

    private View buildToolbar() {
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(12), dp(8), dp(10), dp(7));
        toolbar.setBackgroundColor(topbar);

        Button menu = button("☰", false, true);
        menu.setTextSize(20);
        menu.setContentDescription("Open menu");
        menu.setOnClickListener(view -> openDrawer());
        toolbar.addView(menu, new LinearLayout.LayoutParams(dp(48), dp(44)));

        TextView title = label("JLPT Kotoba", 22, Color.rgb(255, 247, 237), Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        titleParams.setMargins(dp(10), 0, dp(6), 0);
        toolbar.addView(title, titleParams);

        Button theme = button(darkMode ? "Light" : "Dark", false, false);
        theme.setOnClickListener(view -> {
            darkMode = !darkMode;
            preferences.edit().putBoolean(PREF_DARK_MODE, darkMode).apply();
            buildInterface();
        });
        LinearLayout.LayoutParams themeParams = new LinearLayout.LayoutParams(dp(72), dp(44));
        toolbar.addView(theme, themeParams);
        return toolbar;
    }

    private void buildDrawerLayer() {
        drawerScrim = new View(this);
        drawerScrim.setBackgroundColor(Color.argb(150, 0, 0, 0));
        drawerScrim.setAlpha(0f);
        drawerScrim.setVisibility(View.GONE);
        drawerScrim.setOnClickListener(view -> closeDrawer(null));
        root.addView(drawerScrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(panel);
        drawer.setElevation(dp(14));

        int drawerWidth = Math.min(dp(340), Math.round(getResources().getDisplayMetrics().widthPixels * 0.86f));
        FrameLayout.LayoutParams drawerParams = new FrameLayout.LayoutParams(
                drawerWidth,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.START
        );
        root.addView(drawer, drawerParams);

        LinearLayout drawerHeader = new LinearLayout(this);
        drawerHeader.setOrientation(LinearLayout.HORIZONTAL);
        drawerHeader.setGravity(Gravity.CENTER_VERTICAL);
        drawerHeader.setPadding(dp(18), dp(12), dp(12), dp(8));
        TextView menuTitle = label("JLPT Kotoba", 21, ink, Typeface.BOLD);
        drawerHeader.addView(menuTitle, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button close = button("×", false, false);
        close.setTextSize(23);
        close.setContentDescription("Close menu");
        close.setOnClickListener(view -> closeDrawer(null));
        drawerHeader.addView(close, new LinearLayout.LayoutParams(dp(46), dp(44)));
        drawer.addView(drawerHeader);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout menuContent = new LinearLayout(this);
        menuContent.setOrientation(LinearLayout.VERTICAL);
        menuContent.setPadding(dp(14), dp(4), dp(14), dp(24));
        scroll.addView(menuContent, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        drawer.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        menuContent.addView(drawerSectionLabel("STUDY"));
        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        Button vocabulary = button("Vocabulary", "vocabulary".equals(currentMode), false);
        vocabulary.setOnClickListener(view -> closeDrawer(() -> selectMode("vocabulary")));
        modes.addView(vocabulary, weightedButtonParams(0, dp(4)));
        Button flashcards = button("Flashcards", "flashcards".equals(currentMode), false);
        flashcards.setOnClickListener(view -> closeDrawer(() -> selectMode("flashcards")));
        modes.addView(flashcards, weightedButtonParams(dp(4), 0));
        menuContent.addView(modes);

        LinearLayout levels = new LinearLayout(this);
        levels.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams levelsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
        );
        levelsParams.setMargins(0, dp(8), 0, 0);
        menuContent.addView(levels, levelsParams);
        for (String level : LEVELS) {
            Button item = button(level, level.equals(currentLevel), false);
            item.setOnClickListener(view -> closeDrawer(() -> selectLevel(level)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1);
            params.setMargins(dp(2), 0, dp(2), 0);
            levels.addView(item, params);
        }

        menuContent.addView(drawerSectionLabel("BROWSE BY KANA"));

        Set<String> available = availableKana();
        for (String[] kanaRow : KANA_ROWS) {
            String groupKana = kanaRow[0];
            boolean rowAvailable = false;
            for (String kana : kanaRow) {
                rowAvailable |= available.contains(kana);
            }

            LinearLayout group = new LinearLayout(this);
            group.setOrientation(LinearLayout.HORIZONTAL);
            group.setGravity(Gravity.CENTER_VERTICAL);
            group.setPadding(dp(14), 0, dp(10), 0);
            group.setBackground(roundedBackground(panel, line, 9));
            group.setAlpha(rowAvailable ? 1f : 0.42f);
            group.setEnabled(rowAvailable);
            group.setContentDescription(groupKana + " row");

            TextView groupLabel = label(groupKana + " row", 14, ink, Typeface.BOLD);
            group.addView(groupLabel, new LinearLayout.LayoutParams(0, dp(44), 1));

            ImageView chevron = new ImageView(this);
            chevron.setImageResource(R.drawable.ic_chevron_down);
            chevron.setColorFilter(ink);
            chevron.setRotation(expandedKanaGroups.contains(groupKana) ? 180f : 0f);
            group.addView(chevron, new LinearLayout.LayoutParams(dp(28), dp(28)));
            menuContent.addView(group, drawerItemParams(dp(6)));

            LinearLayout children = new LinearLayout(this);
            children.setOrientation(LinearLayout.VERTICAL);
            children.setPadding(dp(12), 0, 0, 0);
            for (String kana : kanaRow) {
                boolean childAvailable = available.contains(kana);
                Button child = drawerButton(kana + "     " + kanaRomaji(kana), childAvailable);
                child.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                child.setTypeface(Typeface.DEFAULT, kana.equals(activeKana) ? Typeface.BOLD : Typeface.NORMAL);
                if (kana.equals(activeKana)) {
                    child.setBackground(roundedBackground(accent, accent, 9));
                    child.setTextColor(Color.rgb(255, 247, 237));
                }
                child.setOnClickListener(view -> {
                    if (childAvailable) {
                        closeDrawer(() -> jumpToKana(kana));
                    }
                });
                children.addView(child, drawerItemParams(dp(4)));
            }
            children.setVisibility(expandedKanaGroups.contains(groupKana) ? View.VISIBLE : View.GONE);
            menuContent.addView(children);

            boolean finalRowAvailable = rowAvailable;
            group.setOnClickListener(view -> {
                if (finalRowAvailable) {
                    toggleKanaGroup(groupKana, chevron, children);
                }
            });
        }

        drawer.setTranslationX(-drawerWidth);
        drawer.setVisibility(View.INVISIBLE);
    }

    private TextView drawerSectionLabel(String text) {
        TextView section = label(text, 11, muted, Typeface.BOLD);
        section.setLetterSpacing(0.12f);
        section.setPadding(dp(4), dp(18), 0, dp(8));
        return section;
    }

    private Button drawerButton(String text, boolean enabled) {
        Button item = button(text, false, false);
        item.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        item.setEnabled(enabled);
        item.setAlpha(enabled ? 1f : 0.42f);
        return item;
    }

    private LinearLayout.LayoutParams drawerItemParams(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
        );
        params.setMargins(0, topMargin, 0, 0);
        return params;
    }

    private void toggleKanaGroup(String groupKana, ImageView chevron, LinearLayout children) {
        boolean expanding = !expandedKanaGroups.contains(groupKana);
        if (expanding) {
            expandedKanaGroups.add(groupKana);
        } else {
            expandedKanaGroups.remove(groupKana);
        }
        chevron.animate()
                .rotation(expanding ? 180f : 0f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        int targetHeight = children.getChildCount() * dp(48);
        int startHeight = expanding ? 0 : Math.max(children.getHeight(), targetHeight);
        int endHeight = expanding ? targetHeight : 0;
        if (expanding) {
            children.setVisibility(View.VISIBLE);
            children.setAlpha(0f);
        }
        ValueAnimator animator = ValueAnimator.ofInt(startHeight, endHeight);
        animator.setDuration(220);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(value -> {
            ViewGroup.LayoutParams params = children.getLayoutParams();
            params.height = (int) value.getAnimatedValue();
            children.setLayoutParams(params);
            float fraction = value.getAnimatedFraction();
            children.setAlpha(expanding ? fraction : 1f - fraction);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (expanding) {
                    children.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    children.setAlpha(1f);
                } else {
                    children.setVisibility(View.GONE);
                    children.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
            }
        });
        animator.start();
    }

    private void openDrawer() {
        if (drawerOpen || drawer == null) {
            return;
        }
        hideKeyboard();
        drawerOpen = true;
        drawerScrim.setVisibility(View.VISIBLE);
        drawer.setVisibility(View.VISIBLE);
        drawer.bringToFront();
        drawerScrim.bringToFront();
        drawer.bringToFront();
        drawerScrim.animate().alpha(1f).setDuration(220).start();
        drawer.animate()
                .translationX(0f)
                .setDuration(260)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void closeDrawer(Runnable afterClose) {
        if (!drawerOpen || drawer == null) {
            if (afterClose != null) {
                afterClose.run();
            }
            return;
        }
        drawerOpen = false;
        drawerScrim.animate().alpha(0f).setDuration(180).start();
        drawer.animate()
                .translationX(-drawer.getWidth())
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    drawer.setVisibility(View.INVISIBLE);
                    drawerScrim.setVisibility(View.GONE);
                    if (afterClose != null) {
                        afterClose.run();
                    }
                })
                .start();
    }

    private void installSystemBarInsets() {
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset;
            int bottomInset;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                topInset = bars.top;
                bottomInset = bars.bottom;
            } else {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
            }
            topChrome.setPadding(0, topInset, 0, 0);
            mainColumn.setPadding(0, 0, 0, bottomInset);
            drawer.setPadding(0, topInset, 0, bottomInset);
            return insets;
        });
        root.requestApplyInsets();
    }

    private void registerBackHandler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    () -> {
                        if (drawerOpen) {
                            closeDrawer(null);
                        } else {
                            finishAfterTransition();
                        }
                    }
            );
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            drawerGestureStartX = event.getX();
            drawerGestureStartY = event.getY();
            drawerGestureStartedOpen = drawerOpen;
            drawerGestureTracking = drawerOpen
                    ? drawer != null && drawerGestureStartX <= drawer.getWidth()
                    : drawerGestureStartX <= dp(28);
        } else if (action == MotionEvent.ACTION_MOVE && drawerGestureTracking) {
            float deltaX = event.getX() - drawerGestureStartX;
            float deltaY = event.getY() - drawerGestureStartY;
            float horizontalDistance = Math.abs(deltaX);
            float verticalDistance = Math.abs(deltaY);

            if (verticalDistance > dp(14) && verticalDistance > horizontalDistance) {
                drawerGestureTracking = false;
            } else if (horizontalDistance > dp(72)
                    && horizontalDistance > verticalDistance * 1.35f) {
                boolean shouldClose = drawerGestureStartedOpen && deltaX < 0;
                boolean shouldOpen = !drawerGestureStartedOpen && deltaX > 0;
                if (shouldOpen || shouldClose) {
                    drawerGestureTracking = false;
                    MotionEvent cancel = MotionEvent.obtain(event);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                    if (shouldOpen) {
                        openDrawer();
                    } else {
                        closeDrawer(null);
                    }
                    return true;
                }
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            drawerGestureTracking = false;
        }
        return super.dispatchTouchEvent(event);
    }

    @SuppressLint("GestureBackNavigation")
    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (drawerOpen) {
            closeDrawer(null);
        } else {
            super.onBackPressed();
        }
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
        activeKana = null;
        loadedFlashLevel = null;
        preferences.edit().putString(PREF_LEVEL, currentLevel).apply();
        buildInterface();
    }

    private void jumpToKana(String kana) {
        activeKana = kana;
        searchQuery = "";
        if (!"vocabulary".equals(currentMode)) {
            currentMode = "vocabulary";
            preferences.edit().putString(PREF_MODE, currentMode).apply();
        }
        buildInterface();
        if (vocabularyList == null || wordAdapter == null) {
            return;
        }
        vocabularyList.post(() -> {
            int position = kana == null ? 0 : wordAdapter.positionForKana(kana);
            if (position >= 0) {
                vocabularyList.setSelection(position);
            }
        });
    }

    private Set<String> availableKana() {
        Set<String> available = new HashSet<>();
        for (Word word : repository.words(currentLevel)) {
            String kana = normalizedFirstKana(word.furigana);
            if (kana != null) {
                available.add(kana);
            }
        }
        return available;
    }

    private String kanaRomaji(String kana) {
        for (int rowIndex = 0; rowIndex < KANA_ROWS.length; rowIndex++) {
            for (int columnIndex = 0; columnIndex < KANA_ROWS[rowIndex].length; columnIndex++) {
                if (KANA_ROWS[rowIndex][columnIndex].equals(kana)) {
                    return KANA_ROMAJI_ROWS[rowIndex][columnIndex];
                }
            }
        }
        return kana;
    }

    private String normalizedFirstKana(String reading) {
        if (reading == null || reading.trim().isEmpty()) {
            return null;
        }
        char kana = 0;
        String cleaned = reading.trim();
        for (int index = 0; index < cleaned.length(); index++) {
            char candidate = cleaned.charAt(index);
            boolean hiragana = candidate >= 'ぁ' && candidate <= 'ゖ';
            boolean katakana = candidate >= 'ァ' && candidate <= 'ヶ';
            if (hiragana || katakana) {
                kana = candidate;
                break;
            }
        }
        if (kana == 0) {
            return null;
        }
        if (kana >= 'ァ' && kana <= 'ヶ') {
            kana = (char) (kana - 0x60);
        }
        String variants = "ぁぃぅぇぉゃゅょっがぎぐげござじずぜぞだぢづでどばびぶべぼぱぴぷぺぽゔ";
        String normalized = "あいうえおやゆよつかきくけこさしすせそたちつてとはひふへほはひふへほう";
        int variantIndex = variants.indexOf(kana);
        if (variantIndex >= 0) {
            kana = normalized.charAt(variantIndex);
        }
        return String.valueOf(kana);
    }

    private int kanaOrderIndex(String kana) {
        int position = 0;
        for (String[] row : KANA_ROWS) {
            for (String candidate : row) {
                if (candidate.equals(kana)) {
                    return position;
                }
                position++;
            }
        }
        return Integer.MAX_VALUE;
    }

    private void renderVocabulary() {
        List<Word> allWords = repository.words(currentLevel);
        allWords.sort((left, right) -> {
            int kanaOrder = Integer.compare(
                    kanaOrderIndex(normalizedFirstKana(left.furigana)),
                    kanaOrderIndex(normalizedFirstKana(right.furigana))
            );
            return kanaOrder != 0
                    ? kanaOrder
                    : left.furigana.compareToIgnoreCase(right.furigana);
        });

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
            if (selected == null) {
                return true;
            }
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
        vocabularyCount.setText(filtered.isEmpty()
                ? (allWords.isEmpty() ? "Coming soon" : "0 matches")
                : filtered.size() + " words");
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

        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(Math.max(1, available.size()));
        progressBar.setProgress(flashDeck.isEmpty()
                ? 0
                : (flashComplete ? flashDeck.size() : Math.max(1, flashIndex + 1)));
        progressBar.setProgressTintList(ColorStateList.valueOf(accent));
        progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(line));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(7)
        );
        barParams.setMargins(0, 0, 0, dp(10));
        content.addView(progressBar, barParams);

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
            addCardLabel(card, "JLPT " + currentLevel, 34, ink, Typeface.BOLD, 0);
            addCardLabel(card, "Coming soon", 17, muted, Typeface.NORMAL, 10);
            return;
        }
        if (flashDeck.isEmpty()) {
            addCardLabel(card, "JLPT " + currentLevel, 34, ink, Typeface.BOLD, 0);
            addCardLabel(card, "Ready", 18, muted, Typeface.NORMAL, 10);
            addCardLabel(card, available.size() + " cards", 14, muted, Typeface.NORMAL, 5);
            return;
        }
        if (flashComplete) {
            addCardLabel(card, "Deck completed!", 31, ink, Typeface.BOLD, 0);
            addCardLabel(
                    card,
                    "You finished all " + flashDeck.size() + " flashcards.",
                    17,
                    muted,
                    Typeface.NORMAL,
                    12
            );
            return;
        }

        Word word = flashDeck.get(flashIndex);
        String front = word.kanji.isEmpty() ? word.furigana : word.kanji;
        addCardLabel(card, front, 42, ink, Typeface.BOLD, 0);
        if (flashAnswerVisible) {
            if (!word.kanji.isEmpty()) {
                addCardLabel(card, word.furigana, 24, ink, Typeface.NORMAL, 14);
            }
            addCardLabel(card, word.romaji, 18, muted, Typeface.NORMAL, 8);
            addCardLabel(card, word.meaning, 21, ink, Typeface.BOLD, 12);
        } else {
            addCardLabel(card, "Tap to reveal", 15, muted, Typeface.NORMAL, 14);
        }
    }

    private void addCardLabel(
            LinearLayout card,
            String text,
            float size,
            int color,
            int style,
            int topMargin
    ) {
        TextView item = label(text, size, color, style);
        item.setGravity(Gravity.CENTER);
        item.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(topMargin), 0, 0);
        card.addView(item, params);
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
        View decorView = window.getDecorView();
        window.setStatusBarColor(topbar);
        window.setNavigationBarColor(paper);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        darkMode ? 0 : WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
            }
        } else {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (!darkMode) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decorView.setSystemUiVisibility(flags);
        }
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

    private static final int VOCAB_SECTION = 0;
    private static final int VOCAB_WORD = 1;

    private final class VocabularyItem {
        final int kind;
        final Word word;
        final String kana;

        VocabularyItem(int kind, Word word, String kana) {
            this.kind = kind;
            this.word = word;
            this.kana = kana;
        }
    }

    private final class WordAdapter extends BaseAdapter {
        private final List<VocabularyItem> items = new ArrayList<>();

        WordAdapter(List<Word> words) {
            String previousKana = null;
            for (Word word : words) {
                String kana = normalizedFirstKana(word.furigana);
                if (kana != null && !kana.equals(previousKana)) {
                    items.add(new VocabularyItem(VOCAB_SECTION, null, kana));
                    previousKana = kana;
                }
                items.add(new VocabularyItem(VOCAB_WORD, word, kana));
            }
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Word getItem(int position) {
            return items.get(position).word;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).kind;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return items.get(position).kind == VOCAB_WORD;
        }

        int positionForKana(String kana) {
            for (int index = 0; index < items.size(); index++) {
                VocabularyItem item = items.get(index);
                if (item.kind == VOCAB_SECTION && kana.equals(item.kana)) {
                    return index;
                }
            }
            return -1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            VocabularyItem item = items.get(position);
            if (item.kind == VOCAB_SECTION) {
                return buildKanaSectionRow(item.kana, position == 0);
            }
            return buildWordRow(item.word, position);
        }

        private View buildKanaSectionRow(String kana, boolean firstSection) {
            LinearLayout section = new LinearLayout(MainActivity.this);
            section.setOrientation(LinearLayout.VERTICAL);
            section.setPadding(0, firstSection ? dp(12) : dp(52), 0, 0);
            section.setBackgroundColor(paper);

            View marker = new View(MainActivity.this);
            marker.setBackgroundColor(accent);
            LinearLayout.LayoutParams markerParams = new LinearLayout.LayoutParams(dp(48), dp(3));
            markerParams.setMargins(dp(16), 0, 0, dp(13));
            section.addView(marker, markerParams);

            TextView title = label(
                    kana,
                    28,
                    darkMode ? Color.rgb(255, 216, 200) : accent,
                    Typeface.BOLD
            );
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            titleParams.setMargins(dp(16), 0, 0, dp(12));
            section.addView(title, titleParams);

            LinearLayout tableHeader = new LinearLayout(MainActivity.this);
            tableHeader.setOrientation(LinearLayout.HORIZONTAL);
            tableHeader.setGravity(Gravity.CENTER_VERTICAL);
            tableHeader.setBackgroundColor(topbar);
            tableHeader.setMinimumHeight(dp(38));
            addTableCell(tableHeader, "Kanji", 0.85f, Color.rgb(255, 247, 237), Typeface.BOLD, 11);
            addTableCell(tableHeader, "Furigana", 1.05f, Color.rgb(255, 247, 237), Typeface.BOLD, 11);
            addTableCell(tableHeader, "Romaji", 0.85f, Color.rgb(255, 247, 237), Typeface.BOLD, 11);
            addTableCell(tableHeader, "Meaning", 1.65f, Color.rgb(255, 247, 237), Typeface.BOLD, 11);
            section.addView(tableHeader, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return section;
        }

        private View buildWordRow(Word word, int position) {
            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(58));
            row.setBackgroundColor(position % 2 == 0
                    ? panel
                    : (darkMode ? Color.rgb(39, 27, 37) : Color.rgb(255, 247, 239)));

            String kanji = word.kanji.isEmpty() ? "—" : word.kanji;
            int kanjiColor = word.custom ? accent : ink;
            addTableCell(row, kanji, 0.85f, kanjiColor, Typeface.BOLD, 14);
            addTableCell(row, word.furigana, 1.05f, ink, Typeface.NORMAL, 13);
            addTableCell(row, word.romaji, 0.85f, muted, Typeface.NORMAL, 12);
            addTableCell(row, word.meaning, 1.65f, ink, Typeface.NORMAL, 13);
            return row;
        }

        private void addTableCell(
                LinearLayout row,
                String text,
                float weight,
                int color,
                int style,
                float size
        ) {
            if (row.getChildCount() > 0) {
                View divider = new View(MainActivity.this);
                divider.setBackgroundColor(line);
                row.addView(divider, new LinearLayout.LayoutParams(dp(1), ViewGroup.LayoutParams.MATCH_PARENT));
            }
            TextView cell = label(text, size, color, style);
            cell.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            cell.setPadding(dp(8), dp(8), dp(6), dp(8));
            row.addView(cell, new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    weight
            ));
        }
    }
}
