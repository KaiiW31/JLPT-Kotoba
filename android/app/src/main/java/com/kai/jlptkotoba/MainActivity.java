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
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.Display;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.BaseAdapter;
import android.widget.AbsListView;
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
import java.util.Locale;
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
    private ScrollView drawerScroll;
    private LinearLayout drawerMenuContent;
    private boolean drawerOpen;
    private boolean drawerGestureTracking;
    private boolean drawerGestureDragging;
    private boolean drawerGestureStartedOpen;
    private float drawerGestureStartX;
    private float drawerGestureStartY;
    private int systemGestureInsetLeft;
    private int systemGestureInsetRight;
    private VelocityTracker drawerVelocityTracker;
    private View settingsScrim;
    private LinearLayout settingsDrawer;
    private boolean settingsOpen;
    private boolean settingsGestureTracking;
    private boolean settingsGestureDragging;
    private boolean settingsGestureStartedOpen;
    private float settingsGestureStartX;
    private float settingsGestureStartY;
    private final Map<String, LinearLayout> drawerGroupChildren = new HashMap<>();
    private final Map<String, ImageView> drawerGroupChevrons = new HashMap<>();
    private final Map<String, Button> drawerKanaButtons = new HashMap<>();
    private final Set<String> expandedKanaGroups = new HashSet<>();
    private String activeKana;
    private Word selectedVocabularyWord;
    private TextView vocabularyCount;
    private LinearLayout vocabularyControls;
    private ListView vocabularyList;
    private WordAdapter wordAdapter;
    private StudyListAdapter studyAdapter;
    private boolean vocabularyControlsHidden;
    private int vocabularyControlsHeight;
    private int vocabularyScrollPreviousFirst = -1;
    private int vocabularyScrollPreviousTop;
    private final SparseIntArray vocabularyVisibleItemTops = new SparseIntArray();
    private int vocabularyControlsLastDirection;
    private int vocabularyScrollGeneration;
    private long vocabularyScrollLastFrameNanos;
    private boolean vocabularyKanaScrollRunning;

    private List<Word> flashDeck = new ArrayList<>();
    private int flashIndex = -1;
    private boolean flashAnswerVisible;
    private boolean flashComplete;
    private String loadedFlashLevel;

    private final List<StudyRow> quizDeck = new ArrayList<>();
    private final List<StudyRow> quizMistakes = new ArrayList<>();
    private final Set<String> quizMistakeKeys = new HashSet<>();
    private int quizIndex = -1;
    private int quizMainCorrect;
    private String quizStage = "ready";
    private boolean quizAnswered;
    private TextView quizFeedback;
    private Button quizNextButton;
    private LinearLayout quizShakeTarget;

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
        if (!"vocabulary".equals(currentMode)
                && !"flashcards".equals(currentMode)
                && !"grammar".equals(currentMode)
                && !"grammar_quiz".equals(currentMode)
                && !"kanji".equals(currentMode)
                && !"kanji_flashcards".equals(currentMode)) {
            currentMode = "vocabulary";
        }
        if ("N1".equals(currentLevel)
                && ("vocabulary".equals(currentMode) || "flashcards".equals(currentMode))) {
            currentLevel = "N2";
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
        drawerOpen = false;
        settingsOpen = false;
        drawerGestureTracking = false;
        drawerGestureDragging = false;
        settingsGestureTracking = false;
        settingsGestureDragging = false;
        applyPalette();

        root = new FrameLayout(this);
        root.setBackgroundColor(paper);
        setContentView(root);
        configureWindow();
        requestHighRefreshRate();

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

        renderCurrentScreen();

        buildDrawerLayer();
        buildSettingsLayer();
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

        Button settings = button("⚙", false, false);
        settings.setTextSize(20);
        settings.setContentDescription("Open settings");
        settings.setOnClickListener(view -> openSettings());
        toolbar.addView(settings, new LinearLayout.LayoutParams(dp(52), dp(44)));
        return toolbar;
    }

    private void buildDrawerLayer() {
        drawerGroupChildren.clear();
        drawerGroupChevrons.clear();
        drawerKanaButtons.clear();

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
        drawer.setClickable(true);

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
        drawerScroll = scroll;
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        drawerMenuContent = new LinearLayout(this);
        drawerMenuContent.setOrientation(LinearLayout.VERTICAL);
        drawerMenuContent.setPadding(dp(14), dp(4), dp(14), dp(24));
        scroll.addView(drawerMenuContent, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        drawer.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        populateDrawerMenuContent();

        drawer.setTranslationX(-drawerWidth);
        drawer.setVisibility(View.INVISIBLE);
    }

    private void populateDrawerMenuContent() {
        if (drawerMenuContent == null) {
            return;
        }
        drawerMenuContent.removeAllViews();
        drawerGroupChildren.clear();
        drawerGroupChevrons.clear();
        drawerKanaButtons.clear();

        drawerMenuContent.addView(drawerSectionLabel("STUDY"));
        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        String baseMode = baseStudyMode(currentMode);
        Button vocabulary = button("Vocabulary", "vocabulary".equals(baseMode), false);
        vocabulary.setOnClickListener(view -> selectBaseModeKeepingDrawerOpen("vocabulary"));
        modes.addView(vocabulary, weightedButtonParams(0, dp(4)));
        Button grammar = button("Grammar", "grammar".equals(baseMode), false);
        grammar.setOnClickListener(view -> selectBaseModeKeepingDrawerOpen("grammar"));
        modes.addView(grammar, weightedButtonParams(dp(4), dp(4)));
        Button kanji = button("Kanji", "kanji".equals(baseMode), false);
        kanji.setOnClickListener(view -> selectBaseModeKeepingDrawerOpen("kanji"));
        modes.addView(kanji, weightedButtonParams(dp(4), 0));
        drawerMenuContent.addView(modes);

        String studyLabel = "grammar".equals(baseMode) ? "Quiz" : "Flashcards";
        Button study = button(studyLabel, isStudyMode(currentMode), false);
        study.setOnClickListener(view -> closeDrawer(this::selectContextStudy));
        drawerMenuContent.addView(study, drawerItemParams(dp(8)));

        LinearLayout levels = new LinearLayout(this);
        levels.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams levelsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
        );
        levelsParams.setMargins(0, dp(8), 0, 0);
        drawerMenuContent.addView(levels, levelsParams);
        for (String level : LEVELS) {
            Button item = button(level, level.equals(currentLevel), false);
            boolean levelAvailable = !"N1".equals(level)
                    || "grammar".equals(baseMode)
                    || "kanji".equals(baseMode);
            item.setEnabled(levelAvailable);
            item.setAlpha(levelAvailable ? 1f : 0.42f);
            item.setOnClickListener(view -> closeDrawer(() -> selectLevel(level)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1);
            params.setMargins(dp(2), 0, dp(2), 0);
            levels.addView(item, params);
        }

        if (!isStudyMode(currentMode)) {
            drawerMenuContent.addView(drawerSectionLabel("BROWSE BY KANA"));

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
                drawerMenuContent.addView(group, drawerItemParams(dp(6)));

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
                    drawerKanaButtons.put(kana, child);
                    children.addView(child, drawerItemParams(dp(4)));
                }
                children.setVisibility(expandedKanaGroups.contains(groupKana) ? View.VISIBLE : View.GONE);
                drawerMenuContent.addView(children);
                drawerGroupChildren.put(groupKana, children);
                drawerGroupChevrons.put(groupKana, chevron);

                boolean finalRowAvailable = rowAvailable;
                group.setOnClickListener(view -> {
                    if (finalRowAvailable) {
                        toggleKanaGroup(groupKana, chevron, children);
                    }
                });
            }
        }
    }

    private void buildSettingsLayer() {
        settingsScrim = new View(this);
        settingsScrim.setBackgroundColor(Color.argb(150, 0, 0, 0));
        settingsScrim.setAlpha(0f);
        settingsScrim.setVisibility(View.GONE);
        settingsScrim.setOnClickListener(view -> closeSettings(null));
        root.addView(settingsScrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        settingsDrawer = new LinearLayout(this);
        settingsDrawer.setOrientation(LinearLayout.VERTICAL);
        settingsDrawer.setBackgroundColor(panel);
        settingsDrawer.setElevation(dp(14));
        settingsDrawer.setClickable(true);
        int width = Math.min(dp(340), Math.round(getResources().getDisplayMetrics().widthPixels * 0.86f));
        root.addView(settingsDrawer, new FrameLayout.LayoutParams(
                width,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END
        ));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(12), dp(12), dp(8));
        header.addView(label("Settings", 21, ink, Typeface.BOLD), new LinearLayout.LayoutParams(0, dp(48), 1));
        Button close = button("×", false, false);
        close.setTextSize(23);
        close.setContentDescription("Close settings");
        close.setOnClickListener(view -> closeSettings(null));
        header.addView(close, new LinearLayout.LayoutParams(dp(46), dp(44)));
        settingsDrawer.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(4), dp(16), dp(24));
        body.addView(drawerSectionLabel("APPEARANCE"));
        Button theme = button(darkMode ? "Use light theme" : "Use dark theme", false, false);
        theme.setOnClickListener(view -> {
            darkMode = !darkMode;
            preferences.edit().putBoolean(PREF_DARK_MODE, darkMode).apply();
            buildInterface();
            showSettingsLayer();
            settingsDrawer.setTranslationX(0f);
            settingsScrim.setAlpha(1f);
            settingsOpen = true;
        });
        body.addView(theme, drawerItemParams(0));
        scroll.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        settingsDrawer.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));
        settingsDrawer.setTranslationX(width);
        settingsDrawer.setVisibility(View.INVISIBLE);
    }

    private void showSettingsLayer() {
        settingsDrawer.animate().cancel();
        settingsScrim.animate().cancel();
        settingsScrim.setVisibility(View.VISIBLE);
        settingsDrawer.setVisibility(View.VISIBLE);
        settingsScrim.bringToFront();
        settingsDrawer.bringToFront();
    }

    private void setSettingsTranslation(float translation) {
        float width = Math.max(1f, settingsDrawer.getWidth());
        float clamped = Math.max(0f, Math.min(width, translation));
        settingsDrawer.setTranslationX(clamped);
        settingsScrim.setAlpha(1f - clamped / width);
    }

    private void openSettings() {
        if (settingsOpen || settingsDrawer == null) {
            return;
        }
        hideKeyboard();
        showSettingsLayer();
        animateSettingsTo(true, null);
    }

    private void animateSettingsTo(boolean open, Runnable afterAnimation) {
        showSettingsLayer();
        float width = Math.max(1f, settingsDrawer.getWidth());
        float target = open ? 0f : width;
        float distanceFraction = Math.abs(target - settingsDrawer.getTranslationX()) / width;
        long duration = Math.max(120L, Math.round(260f * distanceFraction));
        settingsOpen = open;
        settingsScrim.animate()
                .alpha(open ? 1f : 0f)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .withLayer()
                .start();
        settingsDrawer.animate()
                .translationX(target)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .withLayer()
                .withEndAction(() -> {
                    if (!open) {
                        settingsDrawer.setVisibility(View.INVISIBLE);
                        settingsScrim.setVisibility(View.GONE);
                    }
                    if (afterAnimation != null) {
                        afterAnimation.run();
                    }
                })
                .start();
    }

    private void closeSettings(Runnable afterClose) {
        if (settingsDrawer == null) {
            if (afterClose != null) {
                afterClose.run();
            }
            return;
        }
        if (!settingsOpen && settingsDrawer.getVisibility() != View.VISIBLE) {
            if (afterClose != null) {
                afterClose.run();
            }
            return;
        }
        animateSettingsTo(false, afterClose);
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

    private String kanaGroupKey(String kana) {
        if (kana == null) {
            return null;
        }
        for (String[] row : KANA_ROWS) {
            for (String candidate : row) {
                if (candidate.equals(kana)) {
                    return row[0];
                }
            }
        }
        return null;
    }

    private void syncDrawerToActiveKana() {
        if (activeKana == null || drawerScroll == null) {
            return;
        }
        String activeGroup = kanaGroupKey(activeKana);
        if (activeGroup == null) {
            return;
        }

        expandedKanaGroups.clear();
        expandedKanaGroups.add(activeGroup);
        for (Map.Entry<String, LinearLayout> entry : drawerGroupChildren.entrySet()) {
            boolean expanded = entry.getKey().equals(activeGroup);
            LinearLayout children = entry.getValue();
            ViewGroup.LayoutParams params = children.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            children.setLayoutParams(params);
            children.setAlpha(1f);
            children.setVisibility(expanded ? View.VISIBLE : View.GONE);

            ImageView chevron = drawerGroupChevrons.get(entry.getKey());
            if (chevron != null) {
                chevron.animate().cancel();
                chevron.setRotation(expanded ? 180f : 0f);
            }
        }

        for (Map.Entry<String, Button> entry : drawerKanaButtons.entrySet()) {
            boolean selected = entry.getKey().equals(activeKana);
            Button item = entry.getValue();
            item.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
            item.setTextColor(selected ? Color.rgb(255, 247, 237) : ink);
            item.setBackground(selected
                    ? roundedBackground(accent, accent, 9)
                    : roundedBackground(panel, line, 9));
        }

        Button activeButton = drawerKanaButtons.get(activeKana);
        if (activeButton != null) {
            boolean keepStudyVisible = KANA_ROWS[0][0].equals(activeKana);
            drawerScroll.post(() -> {
                if (keepStudyVisible) {
                    drawerScroll.smoothScrollTo(0, 0);
                    return;
                }
                Rect target = new Rect();
                activeButton.getDrawingRect(target);
                drawerScroll.offsetDescendantRectToMyCoords(activeButton, target);
                drawerScroll.smoothScrollTo(0, Math.max(0, target.top - dp(88)));
            });
        }
    }

    private void openDrawer() {
        if (drawerOpen || drawer == null) {
            return;
        }
        hideKeyboard();
        syncDrawerToActiveKana();
        showDrawerLayer();
        animateDrawerTo(true, null);
    }

    private void showDrawerLayer() {
        drawer.animate().cancel();
        drawerScrim.animate().cancel();
        drawerScrim.setVisibility(View.VISIBLE);
        drawer.setVisibility(View.VISIBLE);
        drawerScrim.bringToFront();
        drawer.bringToFront();
    }

    private void setDrawerTranslation(float translation) {
        float width = Math.max(1f, drawer.getWidth());
        float clamped = Math.max(-width, Math.min(0f, translation));
        drawer.setTranslationX(clamped);
        drawerScrim.setAlpha(1f + clamped / width);
    }

    private void animateDrawerTo(boolean open, Runnable afterAnimation) {
        showDrawerLayer();
        float width = Math.max(1f, drawer.getWidth());
        float target = open ? 0f : -width;
        float distanceFraction = Math.abs(target - drawer.getTranslationX()) / width;
        long duration = Math.max(120L, Math.round(260f * distanceFraction));
        drawerOpen = open;

        drawerScrim.animate()
                .alpha(open ? 1f : 0f)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .withLayer()
                .start();
        drawer.animate()
                .translationX(target)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .withLayer()
                .withEndAction(() -> {
                    if (!open) {
                        drawer.setVisibility(View.INVISIBLE);
                        drawerScrim.setVisibility(View.GONE);
                    }
                    if (afterAnimation != null) {
                        afterAnimation.run();
                    }
                })
                .start();
    }

    private void closeDrawer(Runnable afterClose) {
        if (drawer == null) {
            if (afterClose != null) {
                afterClose.run();
            }
            return;
        }
        if (!drawerOpen && drawer.getVisibility() != View.VISIBLE) {
            if (afterClose != null) {
                afterClose.run();
            }
            return;
        }
        animateDrawerTo(false, afterClose);
    }

    private void installSystemBarInsets() {
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset;
            int bottomInset;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                android.graphics.Insets gestures = insets.getInsets(WindowInsets.Type.systemGestures());
                topInset = bars.top;
                bottomInset = bars.bottom;
                systemGestureInsetLeft = gestures.left;
                systemGestureInsetRight = gestures.right;
            } else {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    android.graphics.Insets gestures = insets.getSystemGestureInsets();
                    systemGestureInsetLeft = gestures.left;
                    systemGestureInsetRight = gestures.right;
                }
            }
            topChrome.setPadding(0, topInset, 0, 0);
            mainColumn.setPadding(0, 0, 0, bottomInset);
            drawer.setPadding(0, topInset, 0, bottomInset);
            settingsDrawer.setPadding(0, topInset, 0, bottomInset);
            return insets;
        });
        root.requestApplyInsets();
    }

    private void registerBackHandler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    () -> {
                        if (settingsOpen) {
                            closeSettings(null);
                        } else if (drawerOpen) {
                            closeDrawer(null);
                        } else if (isStudyMode(currentMode)) {
                            selectMode(baseStudyMode(currentMode));
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
            vocabularyScrollGeneration++;
            vocabularyKanaScrollRunning = false;
            recycleDrawerVelocityTracker();
            drawerVelocityTracker = VelocityTracker.obtain();
            drawerVelocityTracker.addMovement(event);
            drawerGestureStartX = event.getX();
            drawerGestureStartY = event.getY();
            drawerGestureStartedOpen = drawerOpen;
            drawerGestureDragging = false;
            settingsGestureStartX = event.getX();
            settingsGestureStartY = event.getY();
            settingsGestureStartedOpen = settingsOpen;
            settingsGestureDragging = false;

            boolean insideOpenDrawer = drawerOpen && drawer != null;
            int safeLeft = Math.max(dp(32), systemGestureInsetLeft + dp(8));
            int safeRight = Math.max(dp(32), systemGestureInsetRight + dp(8));
            boolean insideGestureSafeContent = !drawerOpen && !settingsOpen
                    && root != null
                    && drawerGestureStartX >= safeLeft
                    && drawerGestureStartX <= root.getWidth() - safeRight;
            drawerGestureTracking = insideOpenDrawer || insideGestureSafeContent;
            boolean insideOpenSettings = settingsOpen && settingsDrawer != null;
            settingsGestureTracking = insideOpenSettings || insideGestureSafeContent;
        } else if (drawerVelocityTracker != null) {
            drawerVelocityTracker.addMovement(event);
        }

        if (action == MotionEvent.ACTION_MOVE && drawerGestureTracking) {
            float deltaX = event.getX() - drawerGestureStartX;
            float deltaY = event.getY() - drawerGestureStartY;
            float horizontalDistance = Math.abs(deltaX);
            float verticalDistance = Math.abs(deltaY);
            int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

            if (!drawerGestureDragging) {
                if (verticalDistance > touchSlop && verticalDistance > horizontalDistance) {
                    drawerGestureTracking = false;
                } else if (horizontalDistance > touchSlop) {
                    boolean correctDirection = drawerGestureStartedOpen ? deltaX < 0 : deltaX > 0;
                    if (!correctDirection) {
                        drawerGestureTracking = false;
                    } else {
                        drawerGestureDragging = true;
                        hideKeyboard();
                        if (!drawerGestureStartedOpen) {
                            syncDrawerToActiveKana();
                        }
                        showDrawerLayer();
                        MotionEvent cancel = MotionEvent.obtain(event);
                        cancel.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(cancel);
                        cancel.recycle();
                    }
                }
            }

            if (drawerGestureDragging) {
                float width = Math.max(1f, drawer.getWidth());
                float startTranslation = drawerGestureStartedOpen ? 0f : -width;
                setDrawerTranslation(startTranslation + deltaX);
                return true;
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (drawerGestureDragging) {
                boolean open;
                if (action == MotionEvent.ACTION_CANCEL) {
                    open = drawerGestureStartedOpen;
                } else {
                    drawerVelocityTracker.computeCurrentVelocity(1000);
                    float velocityX = drawerVelocityTracker.getXVelocity();
                    float minimumFling = Math.max(
                            ViewConfiguration.get(this).getScaledMinimumFlingVelocity(),
                            dp(450)
                    );
                    float width = Math.max(1f, drawer.getWidth());
                    float openFraction = 1f + drawer.getTranslationX() / width;
                    open = Math.abs(velocityX) >= minimumFling
                            ? velocityX > 0
                            : openFraction >= 0.5f;
                }
                drawerGestureTracking = false;
                drawerGestureDragging = false;
                recycleDrawerVelocityTracker();
                animateDrawerTo(open, null);
                return true;
            }
        }

        if (action == MotionEvent.ACTION_MOVE && settingsGestureTracking) {
            float deltaX = event.getX() - settingsGestureStartX;
            float deltaY = event.getY() - settingsGestureStartY;
            float horizontalDistance = Math.abs(deltaX);
            float verticalDistance = Math.abs(deltaY);
            int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

            if (!settingsGestureDragging) {
                if (verticalDistance > touchSlop && verticalDistance > horizontalDistance) {
                    settingsGestureTracking = false;
                } else if (horizontalDistance > touchSlop) {
                    boolean correctDirection = settingsGestureStartedOpen ? deltaX > 0 : deltaX < 0;
                    if (!correctDirection) {
                        settingsGestureTracking = false;
                    } else {
                        settingsGestureDragging = true;
                        hideKeyboard();
                        showSettingsLayer();
                        MotionEvent cancel = MotionEvent.obtain(event);
                        cancel.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(cancel);
                        cancel.recycle();
                    }
                }
            }

            if (settingsGestureDragging) {
                float width = Math.max(1f, settingsDrawer.getWidth());
                float startTranslation = settingsGestureStartedOpen ? 0f : width;
                setSettingsTranslation(startTranslation + deltaX);
                return true;
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (settingsGestureDragging) {
                boolean open;
                if (action == MotionEvent.ACTION_CANCEL) {
                    open = settingsGestureStartedOpen;
                } else {
                    drawerVelocityTracker.computeCurrentVelocity(1000);
                    float velocityX = drawerVelocityTracker.getXVelocity();
                    float minimumFling = Math.max(
                            ViewConfiguration.get(this).getScaledMinimumFlingVelocity(),
                            dp(450)
                    );
                    float width = Math.max(1f, settingsDrawer.getWidth());
                    float openFraction = 1f - settingsDrawer.getTranslationX() / width;
                    open = Math.abs(velocityX) >= minimumFling
                            ? velocityX < 0
                            : openFraction >= 0.5f;
                }
                drawerGestureTracking = false;
                drawerGestureDragging = false;
                settingsGestureTracking = false;
                settingsGestureDragging = false;
                recycleDrawerVelocityTracker();
                animateSettingsTo(open, null);
                return true;
            }
            drawerGestureTracking = false;
            drawerGestureDragging = false;
            settingsGestureTracking = false;
            settingsGestureDragging = false;
            recycleDrawerVelocityTracker();
        }
        return super.dispatchTouchEvent(event);
    }

    private void recycleDrawerVelocityTracker() {
        if (drawerVelocityTracker != null) {
            drawerVelocityTracker.recycle();
            drawerVelocityTracker = null;
        }
    }

    @SuppressLint("GestureBackNavigation")
    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (settingsOpen) {
            closeSettings(null);
        } else if (drawerOpen) {
            closeDrawer(null);
        } else if (isStudyMode(currentMode)) {
            selectMode(baseStudyMode(currentMode));
        } else {
            super.onBackPressed();
        }
    }

    private String baseStudyMode(String mode) {
        if ("grammar_quiz".equals(mode)) {
            return "grammar";
        }
        if ("kanji_flashcards".equals(mode)) {
            return "kanji";
        }
        if ("flashcards".equals(mode)) {
            return "vocabulary";
        }
        return mode;
    }

    private boolean isStudyMode(String mode) {
        return "flashcards".equals(mode)
                || "grammar_quiz".equals(mode)
                || "kanji_flashcards".equals(mode);
    }

    private boolean isFlashMode(String mode) {
        return "flashcards".equals(mode) || "kanji_flashcards".equals(mode);
    }

    private void selectContextStudy() {
        if (isStudyMode(currentMode)) {
            return;
        }
        String baseMode = baseStudyMode(currentMode);
        if ("grammar".equals(baseMode)) {
            resetGrammarQuizReady();
            selectMode("grammar_quiz");
        } else if ("kanji".equals(baseMode)) {
            loadedFlashLevel = null;
            selectMode("kanji_flashcards");
        } else {
            loadedFlashLevel = null;
            selectMode("flashcards");
        }
    }

    private void selectBaseModeKeepingDrawerOpen(String mode) {
        if (mode.equals(currentMode)) {
            return;
        }
        selectMode(mode);
    }

    private void selectMode(String mode) {
        if (mode.equals(currentMode)) {
            return;
        }
        hideKeyboard();
        String previousBaseMode = baseStudyMode(currentMode);
        currentMode = mode;
        if (!previousBaseMode.equals(baseStudyMode(currentMode))) {
            activeKana = null;
            expandedKanaGroups.clear();
        }
        searchQuery = "";
        selectedVocabularyWord = null;
        if ("N1".equals(currentLevel)
                && "vocabulary".equals(baseStudyMode(mode))) {
            currentLevel = "N2";
            preferences.edit().putString(PREF_LEVEL, currentLevel).apply();
        }
        if (isFlashMode(mode)) {
            loadedFlashLevel = null;
        }
        if ("grammar_quiz".equals(mode) && !"main".equals(quizStage)
                && !"review".equals(quizStage)
                && !"review_ready".equals(quizStage)
                && !"complete".equals(quizStage)) {
            resetGrammarQuizReady();
        }
        preferences.edit().putString(PREF_MODE, currentMode).apply();
        renderCurrentScreen();
        populateDrawerMenuContent();
    }

    private void selectLevel(String level) {
        if ("N1".equals(level)
                && "vocabulary".equals(baseStudyMode(currentMode))) {
            return;
        }
        if (level.equals(currentLevel)) {
            return;
        }
        hideKeyboard();
        currentLevel = level;
        activeKana = null;
        searchQuery = "";
        selectedVocabularyWord = null;
        loadedFlashLevel = null;
        if ("grammar_quiz".equals(currentMode)) {
            resetGrammarQuizReady();
        }
        preferences.edit().putString(PREF_LEVEL, currentLevel).apply();
        renderCurrentScreen();
        populateDrawerMenuContent();
    }

    private void jumpToKana(String kana) {
        if (isStudyMode(currentMode)) {
            return;
        }
        activeKana = kana;
        boolean clearingSearch = !searchQuery.isEmpty();
        searchQuery = "";
        if (clearingSearch) {
            renderCurrentScreen();
        }
        animateVocabularyToKana(kana);
    }

    private void animateVocabularyToKana(String kana) {
        if (vocabularyList == null || !hasKanaAdapter()) {
            return;
        }
        vocabularyKanaScrollRunning = true;
        animateVocabularyControls(true);
        int animationGeneration = ++vocabularyScrollGeneration;
        vocabularyList.post(() -> {
            if (animationGeneration != vocabularyScrollGeneration
                    || vocabularyList == null
                    || !hasKanaAdapter()) {
                if (animationGeneration == vocabularyScrollGeneration) {
                    vocabularyKanaScrollRunning = false;
                }
                return;
            }
            int position = kana == null ? 0 : positionForKana(kana);
            if (position >= 0) {
                vocabularyScrollLastFrameNanos = 0L;
                runVocabularyEaseOutFrame(position, animationGeneration);
            } else {
                vocabularyKanaScrollRunning = false;
            }
        });
    }

    private void runVocabularyEaseOutFrame(int targetPosition, int animationGeneration) {
        if (animationGeneration != vocabularyScrollGeneration
                || vocabularyList == null
                || !hasKanaAdapter()
                || vocabularyList.getChildCount() == 0) {
            if (animationGeneration == vocabularyScrollGeneration) {
                vocabularyKanaScrollRunning = false;
            }
            return;
        }

        int firstVisible = vocabularyList.getFirstVisiblePosition();
        int lastVisible = vocabularyList.getLastVisiblePosition();
        int desiredTop = dp(4);
        boolean targetVisible = targetPosition >= firstVisible && targetPosition <= lastVisible;
        float remainingPixels;

        if (targetVisible) {
            View targetView = vocabularyList.getChildAt(targetPosition - firstVisible);
            if (targetView == null) {
                vocabularyKanaScrollRunning = false;
                return;
            }
            remainingPixels = targetView.getTop() - desiredTop;
        } else {
            View firstChild = vocabularyList.getChildAt(0);
            View lastChild = vocabularyList.getChildAt(vocabularyList.getChildCount() - 1);
            float visibleSpan = Math.max(
                    dp(58),
                    lastChild.getBottom() - firstChild.getTop()
            );
            float averageRowHeight = visibleSpan / vocabularyList.getChildCount();
            if (targetPosition > lastVisible) {
                int rowsAway = targetPosition - lastVisible;
                remainingPixels = Math.max(dp(1), lastChild.getTop() - desiredTop)
                        + rowsAway * averageRowHeight;
            } else {
                int rowsAway = firstVisible - targetPosition;
                remainingPixels = -(
                        Math.max(dp(1), desiredTop - firstChild.getTop())
                                + rowsAway * averageRowHeight
                );
            }
        }

        float remainingDistance = Math.abs(remainingPixels);
        if (targetVisible && remainingDistance <= 1f) {
            vocabularyList.setSelectionFromTop(targetPosition, desiredTop);
            activeKana = kanaAtPosition(targetPosition);
            vocabularyKanaScrollRunning = false;
            return;
        }

        long frameNanos = System.nanoTime();
        float frameSeconds = vocabularyScrollLastFrameNanos == 0L
                ? 1f / 60f
                : Math.min(1f / 30f, (frameNanos - vocabularyScrollLastFrameNanos) / 1_000_000_000f);
        vocabularyScrollLastFrameNanos = frameNanos;

        // A braking-distance curve: fast while far away, then progressively slower near the target.
        float braking = dp(50_000);
        float maximumVelocity = dp(26_000);
        float minimumVelocity = dp(240);
        float velocity = (float) Math.sqrt(2f * braking * remainingDistance);
        velocity = Math.max(minimumVelocity, Math.min(maximumVelocity, velocity));
        int frameDistance = Math.max(1, Math.round(velocity * frameSeconds));
        if (targetVisible) {
            frameDistance = Math.min(frameDistance, Math.max(1, Math.round(remainingDistance)));
        }

        vocabularyList.scrollListBy(remainingPixels > 0f ? frameDistance : -frameDistance);
        vocabularyList.postOnAnimation(
                () -> runVocabularyEaseOutFrame(targetPosition, animationGeneration)
        );
    }

    private Set<String> availableKana() {
        Set<String> available = new HashSet<>();
        if ("vocabulary".equals(currentMode)) {
            for (Word word : repository.words(currentLevel)) {
                String kana = normalizedFirstKana(word.furigana);
                if (kana != null) {
                    available.add(kana);
                }
            }
            return available;
        }
        for (StudyRow row : repository.studyRows(currentMode, currentLevel)) {
            String kana = studyRowKana(currentMode, row);
            if (kana != null) {
                available.add(kana);
            }
        }
        return available;
    }

    private boolean hasKanaAdapter() {
        return wordAdapter != null || studyAdapter != null;
    }

    private int positionForKana(String kana) {
        return wordAdapter != null
                ? wordAdapter.positionForKana(kana)
                : (studyAdapter == null ? -1 : studyAdapter.positionForKana(kana));
    }

    private String kanaAtPosition(int position) {
        return wordAdapter != null
                ? wordAdapter.kanaAtPosition(position)
                : (studyAdapter == null ? null : studyAdapter.kanaAtPosition(position));
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

    private String kanaForRomaji(String romaji) {
        for (int rowIndex = 0; rowIndex < KANA_ROMAJI_ROWS.length; rowIndex++) {
            for (int columnIndex = 0; columnIndex < KANA_ROMAJI_ROWS[rowIndex].length; columnIndex++) {
                if (KANA_ROMAJI_ROWS[rowIndex][columnIndex].equals(romaji)) {
                    return KANA_ROWS[rowIndex][columnIndex];
                }
            }
        }
        return null;
    }

    private String kanaFromRomaji(String text) {
        if (text == null) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        int start = -1;
        int end = -1;
        for (int index = 0; index < lower.length(); index++) {
            char candidate = lower.charAt(index);
            boolean letter = candidate >= 'a' && candidate <= 'z';
            if (letter && start < 0) {
                start = index;
            } else if (!letter && start >= 0) {
                end = index;
                break;
            }
        }
        if (start < 0) {
            return null;
        }
        String token = lower.substring(start, end < 0 ? lower.length() : end);
        String[][] aliases = {
                {"kya", "ki"}, {"kyu", "ki"}, {"kyo", "ki"},
                {"gya", "ki"}, {"gyu", "ki"}, {"gyo", "ki"},
                {"sha", "shi"}, {"shu", "shi"}, {"sho", "shi"},
                {"sya", "shi"}, {"syu", "shi"}, {"syo", "shi"},
                {"jya", "shi"}, {"jyu", "shi"}, {"jyo", "shi"},
                {"cha", "chi"}, {"chu", "chi"}, {"cho", "chi"},
                {"cya", "chi"}, {"cyu", "chi"}, {"cyo", "chi"},
                {"nya", "ni"}, {"nyu", "ni"}, {"nyo", "ni"},
                {"hya", "hi"}, {"hyu", "hi"}, {"hyo", "hi"},
                {"bya", "hi"}, {"byu", "hi"}, {"byo", "hi"},
                {"pya", "hi"}, {"pyu", "hi"}, {"pyo", "hi"},
                {"mya", "mi"}, {"myu", "mi"}, {"myo", "mi"},
                {"rya", "ri"}, {"ryu", "ri"}, {"ryo", "ri"},
                {"tsu", "tsu"}, {"shi", "shi"}, {"chi", "chi"},
                {"ja", "shi"}, {"ju", "shi"}, {"jo", "shi"}, {"ji", "shi"},
                {"ga", "ka"}, {"gi", "ki"}, {"gu", "ku"}, {"ge", "ke"}, {"go", "ko"},
                {"za", "sa"}, {"zi", "shi"}, {"zu", "su"}, {"ze", "se"}, {"zo", "so"},
                {"da", "ta"}, {"di", "chi"}, {"du", "tsu"}, {"de", "te"}, {"do", "to"},
                {"ba", "ha"}, {"bi", "hi"}, {"bu", "fu"}, {"be", "he"}, {"bo", "ho"},
                {"pa", "ha"}, {"pi", "hi"}, {"pu", "fu"}, {"pe", "he"}, {"po", "ho"},
                {"fa", "fu"}, {"fi", "fu"}, {"fe", "fu"}, {"fo", "fu"}
        };
        for (String[] alias : aliases) {
            if (token.startsWith(alias[0])) {
                return kanaForRomaji(alias[1]);
            }
        }
        String bestKana = null;
        int bestLength = -1;
        for (int rowIndex = 0; rowIndex < KANA_ROMAJI_ROWS.length; rowIndex++) {
            for (int columnIndex = 0; columnIndex < KANA_ROMAJI_ROWS[rowIndex].length; columnIndex++) {
                String romaji = KANA_ROMAJI_ROWS[rowIndex][columnIndex];
                if (token.startsWith(romaji) && romaji.length() > bestLength) {
                    bestKana = KANA_ROWS[rowIndex][columnIndex];
                    bestLength = romaji.length();
                }
            }
        }
        return bestKana;
    }

    private String studyRowKana(String section, StudyRow row) {
        if (row == null || row.values.length == 0) {
            return null;
        }
        if ("grammar".equals(section)) {
            String romaji = row.values.length > 1 ? row.values[1] : "";
            String pattern = row.values[0];
            String kana = kanaFromRomaji(romaji);
            return kana != null ? kana : normalizedFirstKana(pattern);
        }
        String onyomi = row.values.length > 1 ? row.values[1] : "";
        String kunyomi = row.values.length > 2 ? row.values[2] : "";
        String kana = normalizedFirstKana(onyomi);
        if (kana == null) kana = normalizedFirstKana(kunyomi);
        if (kana == null) kana = kanaFromRomaji(onyomi);
        if (kana == null) kana = kanaFromRomaji(kunyomi);
        return kana;
    }

    private void configureTransientScrollbar(ListView list) {
        list.setVerticalScrollBarEnabled(true);
        list.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        list.setScrollbarFadingEnabled(true);
        list.setScrollBarFadeDuration(320);
        list.setScrollBarDefaultDelayBeforeFade(700);
        list.setSmoothScrollbarEnabled(true);
        list.setFastScrollEnabled(true);
        list.setFastScrollAlwaysVisible(false);
    }

    private void renderVocabulary() {
        if (vocabularyControls != null) {
            vocabularyControls.animate().cancel();
        }
        vocabularyControlsHidden = false;
        vocabularyControlsHeight = 0;
        vocabularyScrollPreviousFirst = -1;
        vocabularyScrollPreviousTop = 0;
        vocabularyVisibleItemTops.clear();
        vocabularyControlsLastDirection = 0;
        vocabularyKanaScrollRunning = false;

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

        vocabularyControls = new LinearLayout(this);
        vocabularyControls.setOrientation(LinearLayout.VERTICAL);
        vocabularyControls.setClipChildren(true);
        vocabularyControls.setBackgroundColor(paper);
        vocabularyControls.setElevation(dp(2));

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
        vocabularyControls.addView(heading);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setText(searchQuery);
        search.setSelection(search.getText().length());
        search.setHint("Search word, reading, romaji, type, or meaning");
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
        vocabularyControls.addView(search, searchParams);

        vocabularyCount = label("", 13, muted, Typeface.BOLD);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        countParams.setMargins(dp(2), 0, 0, dp(7));
        vocabularyControls.addView(vocabularyCount, countParams);

        FrameLayout vocabularyViewport = new FrameLayout(this);
        content.addView(vocabularyViewport, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        vocabularyList = new ListView(this);
        vocabularyList.setDivider(new ColorDrawable(line));
        vocabularyList.setDividerHeight(1);
        vocabularyList.setBackgroundColor(paper);
        vocabularyList.setClipToOutline(false);
        configureTransientScrollbar(vocabularyList);
        vocabularyViewport.addView(vocabularyList, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        vocabularyViewport.addView(vocabularyControls, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
        ));

        updateVocabularyFilter(allWords, searchQuery);
        LinearLayout measuredControls = vocabularyControls;
        ListView measuredList = vocabularyList;
        measuredControls.post(() -> {
            if (measuredControls != vocabularyControls || measuredList != vocabularyList) {
                return;
            }
            int measuredHeight = measuredControls.getHeight();
            if (measuredHeight > 0 && measuredHeight != vocabularyControlsHeight) {
                vocabularyControlsHeight = measuredHeight;
                if (wordAdapter != null) {
                    vocabularyVisibleItemTops.clear();
                    vocabularyScrollPreviousFirst = -1;
                    wordAdapter.notifyDataSetChanged();
                    measuredList.setSelection(0);
                }
            }
        });
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
        vocabularyList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    vocabularyControlsLastDirection = 0;
                    if (vocabularyControls != null) {
                        vocabularyControls.animate().cancel();
                    }
                }
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    settleVocabularyControls();
                }
            }

            @Override
            public void onScroll(
                    AbsListView view,
                    int firstVisibleItem,
                    int visibleItemCount,
                    int totalItemCount
            ) {
                if (wordAdapter == null || totalItemCount == 0) {
                    return;
                }
                View lastChild = view.getChildAt(view.getChildCount() - 1);
                boolean atBottom = lastChild != null
                        && view.getLastVisiblePosition() == totalItemCount - 1
                        && lastChild.getBottom() <= view.getHeight() - view.getPaddingBottom() + dp(1);
                String visibleKana = atBottom
                        ? wordAdapter.lastKana()
                        : wordAdapter.kanaAtOrAfterPosition(firstVisibleItem);
                if (visibleKana != null) {
                    activeKana = visibleKana;
                }
                updateVocabularyControlsForScroll(view, firstVisibleItem);
            }
        });
        String initialKana = wordAdapter.firstKana();
        if (initialKana != null) {
            activeKana = initialKana;
        }
    }

    private void updateVocabularyControlsForScroll(AbsListView view, int firstVisibleItem) {
        if (vocabularyControls == null || view.getChildCount() == 0) {
            return;
        }

        int firstTop = view.getChildAt(0).getTop();
        if (vocabularyScrollPreviousFirst < 0) {
            if (firstVisibleItem == 0 && firstTop >= 0) {
                showVocabularyControlsImmediately();
            }
            rememberVocabularyItemTops(view, firstVisibleItem);
            return;
        }

        int pixelDelta = calculateVocabularyScrollDelta(view, firstVisibleItem);
        if (firstVisibleItem == 0 && firstTop >= 0) {
            showVocabularyControlsImmediately();
        } else if (vocabularyKanaScrollRunning) {
            animateVocabularyControls(true);
        } else if (pixelDelta != 0) {
            moveVocabularyControlsBy(pixelDelta);
            vocabularyControlsLastDirection = pixelDelta > 0 ? 1 : -1;
        }

        rememberVocabularyItemTops(view, firstVisibleItem);
    }

    private int calculateVocabularyScrollDelta(AbsListView view, int firstVisibleItem) {
        for (int childIndex = 0; childIndex < view.getChildCount(); childIndex++) {
            int adapterPosition = firstVisibleItem + childIndex;
            int previousTop = vocabularyVisibleItemTops.get(adapterPosition, Integer.MIN_VALUE);
            if (previousTop != Integer.MIN_VALUE) {
                return previousTop - view.getChildAt(childIndex).getTop();
            }
        }

        int firstTop = view.getChildAt(0).getTop();
        int itemDelta = firstVisibleItem - vocabularyScrollPreviousFirst;
        View lastChild = view.getChildAt(view.getChildCount() - 1);
        float averageRowHeight = Math.max(
                dp(1),
                (lastChild.getBottom() - firstTop) / (float) view.getChildCount()
        );
        return Math.round(itemDelta * averageRowHeight + vocabularyScrollPreviousTop - firstTop);
    }

    private void rememberVocabularyItemTops(AbsListView view, int firstVisibleItem) {
        vocabularyVisibleItemTops.clear();
        for (int childIndex = 0; childIndex < view.getChildCount(); childIndex++) {
            vocabularyVisibleItemTops.put(
                    firstVisibleItem + childIndex,
                    view.getChildAt(childIndex).getTop()
            );
        }
        vocabularyScrollPreviousFirst = firstVisibleItem;
        vocabularyScrollPreviousTop = view.getChildAt(0).getTop();
    }

    private void animateVocabularyControls(boolean hidden) {
        if (vocabularyControls == null) {
            return;
        }
        int controlsHeight = vocabularyControlsHeight > 0
                ? vocabularyControlsHeight
                : vocabularyControls.getHeight();
        if (controlsHeight <= 0) {
            return;
        }
        float targetTranslation = hidden ? -controlsHeight : 0f;
        float currentTranslation = vocabularyControls.getTranslationY();
        if (Math.abs(currentTranslation - targetTranslation) <= 1f) {
            vocabularyControls.setTranslationY(targetTranslation);
            vocabularyControls.setAlpha(1f);
            vocabularyControlsHidden = hidden;
            return;
        }
        vocabularyControlsHidden = hidden;
        vocabularyControls.animate().cancel();
        float remainingFraction = Math.min(
                1f,
                Math.abs(currentTranslation - targetTranslation) / controlsHeight
        );
        vocabularyControls.animate()
                .translationY(targetTranslation)
                .alpha(1f)
                .setDuration(Math.round(90 + 130 * remainingFraction))
                .setInterpolator(new DecelerateInterpolator(2f))
                .withLayer()
                .start();
    }

    private void moveVocabularyControlsBy(int scrollDelta) {
        if (vocabularyControls == null || vocabularyControlsHeight <= 0) {
            return;
        }
        float currentOffset = Math.max(0f, -vocabularyControls.getTranslationY());
        float targetOffset = Math.max(
                0f,
                Math.min(vocabularyControlsHeight, currentOffset + scrollDelta)
        );
        vocabularyControls.animate().cancel();
        vocabularyControls.setTranslationY(-targetOffset);
        vocabularyControls.setAlpha(1f);
        vocabularyControlsHidden = targetOffset >= vocabularyControlsHeight - 1f;
    }

    private void settleVocabularyControls() {
        if (vocabularyControls == null || vocabularyControlsHeight <= 0) {
            return;
        }
        float currentOffset = Math.max(0f, -vocabularyControls.getTranslationY());
        if (currentOffset <= 1f || currentOffset >= vocabularyControlsHeight - 1f) {
            vocabularyControlsLastDirection = 0;
            return;
        }
        boolean hide = currentOffset >= vocabularyControlsHeight / 2f;
        if (vocabularyList != null && vocabularyList.getFirstVisiblePosition() == 0) {
            int coupledDistance = hide
                    ? Math.round(vocabularyControlsHeight - currentOffset)
                    : -Math.round(currentOffset);
            if (Math.abs(coupledDistance) > 1) {
                vocabularyControlsLastDirection = hide ? 1 : -1;
                int duration = Math.round(
                        90 + 130 * Math.min(1f, Math.abs(coupledDistance) / (float) vocabularyControlsHeight)
                );
                vocabularyList.smoothScrollBy(coupledDistance, duration);
                return;
            }
        }
        animateVocabularyControls(hide);
        vocabularyControlsLastDirection = 0;
    }

    private void showVocabularyControlsImmediately() {
        if (vocabularyControls == null) {
            return;
        }
        vocabularyControls.animate().cancel();
        vocabularyControls.setTranslationY(0f);
        vocabularyControls.setAlpha(1f);
        vocabularyControlsHidden = false;
        vocabularyControlsLastDirection = 0;
    }

    private void updateVocabularyFilter(List<Word> allWords, String query) {
        List<Word> filtered = new ArrayList<>();
        for (Word word : allWords) {
            if (word.matches(query)) {
                filtered.add(word);
            }
        }
        wordAdapter = new WordAdapter(filtered);
        vocabularyVisibleItemTops.clear();
        vocabularyScrollPreviousFirst = -1;
        vocabularyList.setAdapter(wordAdapter);
        showVocabularyControlsImmediately();
        vocabularyCount.setText(filtered.isEmpty()
                ? (allWords.isEmpty() ? "Coming soon" : "0 matches")
                : filtered.size() + " words");
    }

    private void renderReferenceList(String section) {
        if (vocabularyControls != null) {
            vocabularyControls.animate().cancel();
        }
        vocabularyControlsHidden = false;
        vocabularyControlsHeight = 0;
        vocabularyScrollPreviousFirst = -1;
        vocabularyScrollPreviousTop = 0;
        vocabularyVisibleItemTops.clear();
        vocabularyControlsLastDirection = 0;
        vocabularyKanaScrollRunning = false;
        wordAdapter = null;

        List<StudyRow> allRows = repository.studyRows(section, currentLevel);
        allRows.sort((left, right) -> {
            String leftKana = studyRowKana(section, left);
            String rightKana = studyRowKana(section, right);
            int kanaOrder = Integer.compare(kanaOrderIndex(leftKana), kanaOrderIndex(rightKana));
            if (kanaOrder != 0) {
                return kanaOrder;
            }
            String leftReading = left.values.length > 1 ? left.values[1] : "";
            String rightReading = right.values.length > 1 ? right.values[1] : "";
            return leftReading.compareToIgnoreCase(rightReading);
        });
        vocabularyControls = new LinearLayout(this);
        vocabularyControls.setOrientation(LinearLayout.VERTICAL);
        vocabularyControls.setClipChildren(true);
        vocabularyControls.setBackgroundColor(paper);
        vocabularyControls.setElevation(dp(2));

        String title = "grammar".equals(section) ? "Grammar" : "Kanji";
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.addView(label("JLPT " + currentLevel + " " + title, 25, ink, Typeface.BOLD));
        heading.addView(label("Search or browse the complete " + title.toLowerCase(Locale.ROOT) + " list.", 14, muted, Typeface.NORMAL));
        vocabularyControls.addView(heading);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setText(searchQuery);
        search.setSelection(search.getText().length());
        search.setHint("grammar".equals(section)
                ? "Search grammar, romaji, or meaning"
                : "Search kanji, readings, or meaning");
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
        vocabularyControls.addView(search, searchParams);

        vocabularyCount = label("", 13, muted, Typeface.BOLD);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        countParams.setMargins(dp(2), 0, 0, dp(7));
        vocabularyControls.addView(vocabularyCount, countParams);

        FrameLayout viewport = new FrameLayout(this);
        content.addView(viewport, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));
        vocabularyList = new ListView(this);
        vocabularyList.setDivider(new ColorDrawable(line));
        vocabularyList.setDividerHeight(1);
        vocabularyList.setBackgroundColor(paper);
        configureTransientScrollbar(vocabularyList);
        viewport.addView(vocabularyList, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        viewport.addView(vocabularyControls, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
        ));

        updateReferenceFilter(section, allRows, searchQuery);
        LinearLayout measuredControls = vocabularyControls;
        ListView measuredList = vocabularyList;
        measuredControls.post(() -> {
            if (measuredControls != vocabularyControls || measuredList != vocabularyList) {
                return;
            }
            int measuredHeight = measuredControls.getHeight();
            if (measuredHeight > 0 && measuredHeight != vocabularyControlsHeight) {
                vocabularyControlsHeight = measuredHeight;
                vocabularyScrollPreviousFirst = -1;
                vocabularyVisibleItemTops.clear();
                if (studyAdapter != null) {
                    studyAdapter.notifyDataSetChanged();
                    measuredList.setSelection(0);
                }
            }
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable value) { }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                searchQuery = value.toString();
                updateReferenceFilter(section, allRows, searchQuery);
            }
        });

        vocabularyList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    vocabularyControlsLastDirection = 0;
                    if (vocabularyControls != null) {
                        vocabularyControls.animate().cancel();
                    }
                } else if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    settleVocabularyControls();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (studyAdapter == null || totalItemCount == 0) {
                    return;
                }
                View lastChild = view.getChildAt(view.getChildCount() - 1);
                boolean atBottom = lastChild != null
                        && view.getLastVisiblePosition() == totalItemCount - 1
                        && lastChild.getBottom() <= view.getHeight() - view.getPaddingBottom() + dp(1);
                String visibleKana = atBottom
                        ? studyAdapter.lastKana()
                        : studyAdapter.kanaAtOrAfterPosition(firstVisibleItem);
                if (visibleKana != null) {
                    activeKana = visibleKana;
                }
                updateVocabularyControlsForScroll(view, firstVisibleItem);
            }
        });
        String initialKana = studyAdapter == null ? null : studyAdapter.firstKana();
        if (initialKana != null) {
            activeKana = initialKana;
        }
    }

    private void updateReferenceFilter(String section, List<StudyRow> allRows, String query) {
        List<StudyRow> filtered = new ArrayList<>();
        for (StudyRow row : allRows) {
            if (row.matches(query)) {
                filtered.add(row);
            }
        }
        studyAdapter = new StudyListAdapter(section, filtered);
        vocabularyVisibleItemTops.clear();
        vocabularyScrollPreviousFirst = -1;
        vocabularyList.setAdapter(studyAdapter);
        showVocabularyControlsImmediately();
        String initialKana = studyAdapter.firstKana();
        if (initialKana != null) {
            activeKana = initialKana;
        }
        String unit = "grammar".equals(section) ? " grammar points" : " kanji";
        vocabularyCount.setText(String.format(Locale.ROOT, "%d%s", filtered.size(), unit));
    }

    private String flashSourceLabel() {
        return "kanji_flashcards".equals(currentMode) ? "Kanji" : "Vocabulary";
    }

    private String flashStateKey() {
        return "kanji_flashcards".equals(currentMode) ? "kanji_" + currentLevel : currentLevel;
    }

    private List<Word> flashAvailableItems() {
        if (!"kanji_flashcards".equals(currentMode)) {
            return repository.words(currentLevel);
        }
        List<Word> cards = new ArrayList<>();
        for (StudyRow row : repository.studyRows("kanji", currentLevel)) {
            String kanji = row.values.length > 0 ? row.values[0] : "";
            String onyomi = row.values.length > 1 ? row.values[1] : "";
            String kunyomi = row.values.length > 2 ? row.values[2] : "";
            String meaning = row.values.length > 3 ? row.values[3] : "";
            cards.add(new Word(
                    kanji,
                    "On'yomi: " + (onyomi.isEmpty() ? "—" : onyomi),
                    "Kun'yomi: " + (kunyomi.isEmpty() ? "—" : kunyomi),
                    "Kanji",
                    meaning,
                    false
            ));
        }
        return cards;
    }

    private void renderFlashcards() {
        ensureFlashState();
        List<Word> available = flashAvailableItems();

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.addView(label(
                "JLPT " + currentLevel + " " + flashSourceLabel() + " Flashcards",
                25,
                ink,
                Typeface.BOLD
        ));
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

        FlashCardLayout card = new FlashCardLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(24), dp(26), dp(24), dp(26));
        card.setBackground(roundedBackground(panel, line, 18));
        card.setOnClickListener(view -> handleCardTap(available));
        final float[] cardTouchStart = new float[2];
        card.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                cardTouchStart[0] = event.getX();
                cardTouchStart[1] = event.getY();
                return true;
            }
            if (action != MotionEvent.ACTION_UP) {
                return true;
            }
            float movement = Math.max(
                    Math.abs(event.getX() - cardTouchStart[0]),
                    Math.abs(event.getY() - cardTouchStart[1])
            );
            if (movement > ViewConfiguration.get(this).getScaledTouchSlop()) {
                return true;
            }
            float fraction = event.getX() / Math.max(1f, view.getWidth());
            if (fraction < 0.34f) {
                previousCard();
            } else if (fraction > 0.66f) {
                if (flashDeck.isEmpty()) {
                    startDeck(available);
                } else {
                    nextCard();
                }
            } else {
                view.performClick();
            }
            return true;
        });
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
        loadedFlashLevel = flashStateKey();
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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCustomTitle(dialogTitle("Restart this deck?"))
                .setMessage("Return to the ready screen for JLPT " + currentLevel + "? Your saved position will be cleared.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Restart", (ignored, which) -> resetFlashDeckToReady())
                .create();
        showStyledDialog(dialog);
    }

    private void resetFlashDeckToReady() {
        String stateKey = flashStateKey();
        preferences.edit()
                .remove("flash_deck_" + stateKey)
                .remove("flash_index_" + stateKey)
                .remove("flash_revealed_" + stateKey)
                .remove("flash_complete_" + stateKey)
                .apply();
        flashDeck = new ArrayList<>();
        flashIndex = -1;
        flashAnswerVisible = false;
        flashComplete = false;
        loadedFlashLevel = stateKey;
        renderCurrentScreen();
    }

    private void ensureFlashState() {
        String stateKey = flashStateKey();
        if (stateKey.equals(loadedFlashLevel)) {
            return;
        }
        flashDeck = new ArrayList<>();
        flashIndex = -1;
        flashAnswerVisible = false;
        flashComplete = false;
        loadedFlashLevel = stateKey;

        List<Word> available = flashAvailableItems();
        Map<String, Word> byId = new HashMap<>();
        for (Word word : available) {
            byId.put(word.id(), word);
        }

        String rawDeck = preferences.getString("flash_deck_" + stateKey, "[]");
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
                    Math.max(preferences.getInt("flash_index_" + stateKey, 0), 0),
                    flashDeck.size() - 1
            );
            flashAnswerVisible = preferences.getBoolean("flash_revealed_" + stateKey, false);
            flashComplete = preferences.getBoolean("flash_complete_" + stateKey, false);
        }
    }

    private void saveFlashState() {
        String stateKey = flashStateKey();
        JSONArray ids = new JSONArray();
        for (Word word : flashDeck) {
            ids.put(word.id());
        }
        preferences.edit()
                .putString("flash_deck_" + stateKey, ids.toString())
                .putInt("flash_index_" + stateKey, Math.max(0, flashIndex))
                .putBoolean("flash_revealed_" + stateKey, flashAnswerVisible)
                .putBoolean("flash_complete_" + stateKey, flashComplete)
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

    private void resetGrammarQuizReady() {
        quizStage = "ready";
        quizDeck.clear();
        quizMistakes.clear();
        quizMistakeKeys.clear();
        quizIndex = -1;
        quizMainCorrect = 0;
        quizAnswered = false;
        quizFeedback = null;
        quizNextButton = null;
        quizShakeTarget = null;
    }

    private void renderGrammarQuizHeader(String subtitle, String progressText, int progress, int maximum) {
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.addView(label("JLPT " + currentLevel + " Grammar Quiz", 25, ink, Typeface.BOLD));
        heading.addView(label(subtitle, 14, muted, Typeface.NORMAL));
        content.addView(heading);

        TextView progressLabel = label(progressText, 13, muted, Typeface.BOLD);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        progressParams.setMargins(0, dp(12), 0, dp(8));
        content.addView(progressLabel, progressParams);

        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(Math.max(1, maximum));
        progressBar.setProgress(Math.max(0, Math.min(progress, maximum)));
        progressBar.setProgressTintList(ColorStateList.valueOf(accent));
        progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(line));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(7)
        );
        barParams.setMargins(0, 0, 0, dp(10));
        content.addView(progressBar, barParams);
    }

    private LinearLayout grammarQuizCard() {
        FlashCardLayout card = new FlashCardLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(roundedBackground(panel, line, 18));
        content.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        card.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        inner.setPadding(dp(6), dp(8), dp(6), dp(8));
        scroll.addView(inner, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        scroll.post(() -> inner.setMinimumHeight(Math.max(0, scroll.getHeight())));
        scroll.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> {
            int viewportHeight = Math.max(0, bottom - top);
            if (inner.getMinimumHeight() != viewportHeight) {
                inner.setMinimumHeight(viewportHeight);
            }
        });
        quizShakeTarget = card;
        return inner;
    }

    private LinearLayout grammarQuizActions() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
        );
        actionsParams.setMargins(0, dp(10), 0, 0);
        content.addView(actions, actionsParams);
        return actions;
    }

    private Button addQuizAction(
            LinearLayout actions,
            String text,
            boolean primary,
            boolean enabled,
            int leftMargin,
            int rightMargin,
            View.OnClickListener listener
    ) {
        Button action = button(text, primary, false);
        action.setEnabled(enabled);
        action.setAlpha(enabled ? 1f : 0.5f);
        action.setOnClickListener(listener);
        actions.addView(action, weightedButtonParams(leftMargin, rightMargin));
        return action;
    }

    private void renderGrammarQuiz() {
        if ("main".equals(quizStage) || "review".equals(quizStage)) {
            renderGrammarQuizQuestion();
        } else if ("review_ready".equals(quizStage)) {
            renderMistakeReviewReady();
        } else if ("complete".equals(quizStage)) {
            renderGrammarQuizComplete();
        } else {
            renderGrammarQuizReady();
        }
    }

    private void renderGrammarQuizReady() {
        List<StudyRow> grammar = repository.studyRows("grammar", currentLevel);
        renderGrammarQuizHeader(
                grammar.isEmpty() ? "This quiz is coming soon." : "Choose the correct meaning and continue.",
                grammar.size() + " questions • randomized each session",
                0,
                grammar.size()
        );
        LinearLayout card = grammarQuizCard();
        TextView title = label("JLPT " + currentLevel + " Grammar", 34, ink, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        card.addView(title, matchWrapParams(0, 0, 0, 10));
        TextView ready = label("Ready", 18, muted, Typeface.NORMAL);
        ready.setGravity(Gravity.CENTER);
        card.addView(ready, matchWrapParams(0, 0, 0, 5));
        TextView count = label(grammar.size() + " questions • 4 choices each", 14, muted, Typeface.BOLD);
        count.setGravity(Gravity.CENTER);
        count.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        card.addView(count, matchWrapParams(0, 0, 0, 8));
        TextView note = label(
                "Wrong answers return in a randomized Mistake Review before the quiz can be completed.",
                14,
                muted,
                Typeface.NORMAL
        );
        note.setGravity(Gravity.CENTER);
        note.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        card.addView(note, matchWrapParams(0, 0, 0, 0));

        LinearLayout actions = grammarQuizActions();
        addQuizAction(actions, "Back", false, true, 0, dp(4), view -> selectMode("grammar"));
        addQuizAction(
                actions,
                grammar.isEmpty() ? "No Quiz" : "Start Quiz",
                true,
                !grammar.isEmpty(),
                dp(4),
                dp(4),
                view -> startGrammarQuiz()
        );
        addQuizAction(actions, "Restart", false, false, dp(4), 0, view -> resetGrammarQuizReady());
    }

    private void startGrammarQuiz() {
        quizDeck.clear();
        quizDeck.addAll(repository.studyRows("grammar", currentLevel));
        Collections.shuffle(quizDeck);
        quizMistakes.clear();
        quizMistakeKeys.clear();
        quizIndex = 0;
        quizMainCorrect = 0;
        quizAnswered = false;
        quizStage = "main";
        renderCurrentScreen();
    }

    private String grammarQuestionKey(StudyRow question) {
        return String.join("\u001f", question.values);
    }

    private List<String> randomizedGrammarOptions(StudyRow question) {
        String correct = question.values.length > 2 ? question.values[2] : "";
        Set<String> seen = new HashSet<>();
        seen.add(correct);
        List<String> distractors = new ArrayList<>();
        for (StudyRow candidate : repository.studyRows("grammar", currentLevel)) {
            String meaning = candidate.values.length > 2 ? candidate.values[2] : "";
            if (!meaning.isEmpty() && seen.add(meaning)) {
                distractors.add(meaning);
            }
        }
        Collections.shuffle(distractors);
        List<String> options = new ArrayList<>();
        options.addAll(distractors.subList(0, Math.min(3, distractors.size())));
        options.add(correct);
        Collections.shuffle(options);
        return options;
    }

    private void renderGrammarQuizQuestion() {
        if (quizDeck.isEmpty() || quizIndex < 0 || quizIndex >= quizDeck.size()) {
            resetGrammarQuizReady();
            renderGrammarQuizReady();
            return;
        }
        quizAnswered = false;
        StudyRow question = quizDeck.get(quizIndex);
        String correct = question.values.length > 2 ? question.values[2] : "";
        String section = "review".equals(quizStage) ? "MISTAKE REVIEW" : "GRAMMAR QUIZ";
        renderGrammarQuizHeader(
                "Choose the correct meaning and continue.",
                section + "  •  " + (quizIndex + 1) + " / " + quizDeck.size(),
                quizIndex + 1,
                quizDeck.size()
        );
        LinearLayout card = grammarQuizCard();

        TextView pattern = label(question.values.length > 0 ? question.values[0] : "", 31, ink, Typeface.BOLD);
        pattern.setGravity(Gravity.CENTER);
        pattern.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        card.addView(pattern, matchWrapParams(0, 0, 0, 8));
        TextView romaji = label(question.values.length > 1 ? question.values[1] : "", 16, muted, Typeface.NORMAL);
        romaji.setGravity(Gravity.CENTER);
        romaji.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        card.addView(romaji, matchWrapParams(0, 0, 0, 8));
        TextView instruction = label("Choose the correct meaning", 14, muted, Typeface.BOLD);
        instruction.setGravity(Gravity.CENTER);
        card.addView(instruction, matchWrapParams(0, 0, 0, 16));

        List<String> options = randomizedGrammarOptions(question);
        List<Button> optionButtons = new ArrayList<>();
        for (int index = 0; index < options.size(); index++) {
            String option = options.get(index);
            Button choice = button(option, false, false);
            choice.setGravity(Gravity.CENTER);
            choice.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            int selectedIndex = index;
            choice.setOnClickListener(view -> answerGrammarQuiz(
                    option,
                    selectedIndex,
                    correct,
                    question,
                    options,
                    optionButtons
            ));
            optionButtons.add(choice);
            card.addView(choice, matchFixedHeightParams(dp(64), index == 0 ? 0 : 8));
        }

        quizFeedback = label("", 15, muted, Typeface.BOLD);
        quizFeedback.setGravity(Gravity.CENTER);
        quizFeedback.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        card.addView(quizFeedback, matchWrapParams(0, 16, 0, 0));

        LinearLayout actions = grammarQuizActions();
        addQuizAction(actions, "Exit", false, true, 0, dp(4), view -> selectMode("grammar"));
        quizNextButton = addQuizAction(
                actions,
                "Next",
                true,
                false,
                dp(4),
                dp(4),
                view -> advanceGrammarQuiz()
        );
        addQuizAction(actions, "Restart", false, true, dp(4), 0, view -> {
            resetGrammarQuizReady();
            renderCurrentScreen();
        });
    }

    private void answerGrammarQuiz(
            String selected,
            int selectedIndex,
            String correct,
            StudyRow question,
            List<String> options,
            List<Button> optionButtons
    ) {
        if (quizAnswered) {
            return;
        }
        quizAnswered = true;
        boolean isCorrect = selected.equals(correct);
        if (isCorrect && "main".equals(quizStage)) {
            quizMainCorrect++;
        } else if (!isCorrect && "main".equals(quizStage)) {
            String key = grammarQuestionKey(question);
            if (quizMistakeKeys.add(key)) {
                quizMistakes.add(question);
            }
        }

        int successBackground = darkMode ? Color.rgb(23, 53, 34) : Color.rgb(220, 252, 231);
        int successStroke = Color.rgb(34, 197, 94);
        int successText = darkMode ? Color.rgb(187, 247, 208) : Color.rgb(22, 101, 52);
        int errorBackground = darkMode ? Color.rgb(59, 23, 32) : Color.rgb(255, 228, 230);
        int errorStroke = Color.rgb(225, 29, 72);
        int errorText = darkMode ? Color.rgb(255, 228, 230) : Color.rgb(159, 18, 57);
        for (int index = 0; index < optionButtons.size(); index++) {
            Button choice = optionButtons.get(index);
            String option = options.get(index);
            if (option.equals(correct)) {
                choice.setBackground(roundedBackground(successBackground, successStroke, 9));
                choice.setTextColor(successText);
            } else if (index == selectedIndex) {
                choice.setBackground(roundedBackground(errorBackground, errorStroke, 9));
                choice.setTextColor(errorText);
            }
            choice.setEnabled(false);
        }

        if (isCorrect) {
            quizFeedback.setText("Correct!");
            quizFeedback.setTextColor(successText);
        } else {
            quizFeedback.setText("review".equals(quizStage)
                    ? "Not quite — check the correct answer above."
                    : "Not quite — added to Mistake Review.");
            quizFeedback.setTextColor(errorText);
            shakeGrammarQuiz();
        }
        quizNextButton.setEnabled(true);
        quizNextButton.setAlpha(1f);
    }

    private void shakeGrammarQuiz() {
        if (quizShakeTarget == null) {
            return;
        }
        LinearLayout target = quizShakeTarget;
        ValueAnimator shake = ValueAnimator.ofFloat(
                0f,
                -dp(12),
                dp(10),
                -dp(8),
                dp(6),
                -dp(4),
                dp(2),
                0f
        );
        shake.setDuration(260L);
        shake.addUpdateListener(animation -> target.setTranslationX((float) animation.getAnimatedValue()));
        shake.start();
    }

    private void advanceGrammarQuiz() {
        if (!quizAnswered) {
            return;
        }
        quizIndex++;
        if (quizIndex < quizDeck.size()) {
            renderCurrentScreen();
            return;
        }
        if ("main".equals(quizStage) && !quizMistakes.isEmpty()) {
            quizStage = "review_ready";
        } else {
            quizStage = "complete";
        }
        renderCurrentScreen();
    }

    private void renderMistakeReviewReady() {
        renderGrammarQuizHeader(
                "Review the questions you missed before completing the quiz.",
                quizMistakes.size() + (quizMistakes.size() == 1 ? " mistake" : " mistakes") + " to review",
                0,
                quizMistakes.size()
        );
        LinearLayout card = grammarQuizCard();
        TextView title = label("Mistake Review", 34, ink, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        card.addView(title, matchWrapParams(0, 0, 0, 10));
        TextView ready = label("Ready", 18, muted, Typeface.NORMAL);
        ready.setGravity(Gravity.CENTER);
        card.addView(ready, matchWrapParams(0, 0, 0, 8));
        TextView count = label(
                quizMistakes.size() + (quizMistakes.size() == 1 ? " question" : " questions") + " to review",
                16,
                muted,
                Typeface.BOLD
        );
        count.setGravity(Gravity.CENTER);
        card.addView(count, matchWrapParams(0, 0, 0, 8));
        TextView note = label("Questions and answer positions will be shuffled again.", 14, muted, Typeface.NORMAL);
        note.setGravity(Gravity.CENTER);
        note.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        card.addView(note, matchWrapParams(0, 0, 0, 0));
        LinearLayout actions = grammarQuizActions();
        addQuizAction(actions, "Exit", false, true, 0, dp(4), view -> selectMode("grammar"));
        addQuizAction(actions, "Start Review", true, true, dp(4), dp(4), view -> startMistakeReview());
        addQuizAction(actions, "Restart", false, true, dp(4), 0, view -> {
            resetGrammarQuizReady();
            renderCurrentScreen();
        });
    }

    private void startMistakeReview() {
        quizDeck.clear();
        quizDeck.addAll(quizMistakes);
        Collections.shuffle(quizDeck);
        quizIndex = 0;
        quizAnswered = false;
        quizStage = "review";
        renderCurrentScreen();
    }

    private void renderGrammarQuizComplete() {
        int total = repository.studyRows("grammar", currentLevel).size();
        renderGrammarQuizHeader(
                "Your session is complete.",
                total + " / " + total + " completed",
                total,
                total
        );
        LinearLayout card = grammarQuizCard();
        TextView title = label("Quiz completed!", 34, ink, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        card.addView(title, matchWrapParams(0, 0, 0, 10));
        TextView score = label(quizMainCorrect + " / " + total + " correct on the first pass", 17, muted, Typeface.BOLD);
        score.setGravity(Gravity.CENTER);
        score.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        card.addView(score, matchWrapParams(0, 0, 0, 8));
        String reviewText = quizMistakes.isEmpty()
                ? "Perfect run — no Mistake Review needed."
                : "Reviewed " + quizMistakes.size() + (quizMistakes.size() == 1 ? " mistake." : " mistakes.");
        TextView review = label(reviewText, 14, muted, Typeface.NORMAL);
        review.setGravity(Gravity.CENTER);
        review.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        card.addView(review, matchWrapParams(0, 0, 0, 0));
        LinearLayout actions = grammarQuizActions();
        addQuizAction(actions, "Back", false, true, 0, dp(4), view -> selectMode("grammar"));
        addQuizAction(actions, "Complete", true, false, dp(4), dp(4), view -> selectMode("grammar"));
        addQuizAction(actions, "Restart", false, true, dp(4), 0, view -> startGrammarQuiz());
    }

    private LinearLayout.LayoutParams matchWrapParams(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private LinearLayout.LayoutParams matchFixedHeightParams(int height, int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
        );
        params.setMargins(0, dp(topMargin), 0, 0);
        return params;
    }

    private void renderCurrentScreen() {
        hideKeyboard();
        content.removeAllViews();
        if (isFlashMode(currentMode)) {
            renderFlashcards();
        } else if ("grammar_quiz".equals(currentMode)) {
            renderGrammarQuiz();
        } else if ("grammar".equals(currentMode) || "kanji".equals(currentMode)) {
            renderReferenceList(currentMode);
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

    private void openVocabularyEditor(Word target) {
        if (target == null) {
            Toast.makeText(this, "Tap a vocabulary row first, then choose Edit.", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(22), dp(6), dp(22), dp(8));
        EditText word = editorField("Word", target.kanji);
        EditText reading = editorField("Reading", target.furigana);
        EditText romaji = editorField("Romaji", target.romaji);
        EditText type = editorField("Type", target.type);
        EditText meaning = editorField("Meaning", target.meaning);
        meaning.setSingleLine(false);
        meaning.setMinLines(2);
        for (EditText field : new EditText[]{word, reading, romaji, type, meaning}) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    field == meaning ? dp(76) : dp(52)
            );
            params.setMargins(0, dp(7), 0, dp(7));
            form.addView(field, params);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCustomTitle(dialogTitle("Edit vocabulary"))
                .setView(scroll)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            styleDialog(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String wordValue = word.getText().toString().trim();
                String readingValue = reading.getText().toString().trim();
                String romajiValue = romaji.getText().toString().trim();
                String typeValue = type.getText().toString().trim();
                String meaningValue = meaning.getText().toString().trim();
                if (wordValue.isEmpty() || readingValue.isEmpty() || meaningValue.isEmpty()) {
                    Toast.makeText(this, "Word, reading, and meaning are required.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Word replacement = new Word(
                        wordValue,
                        readingValue,
                        romajiValue,
                        typeValue,
                        meaningValue,
                        true
                );
                repository.replaceWord(currentLevel, target, replacement);
                selectedVocabularyWord = replacement;
                loadedFlashLevel = null;
                searchQuery = "";
                dialog.dismiss();
                renderCurrentScreen();
                Toast.makeText(this, "Vocabulary updated.", Toast.LENGTH_SHORT).show();
            });
        });
        dialog.show();
    }

    private EditText editorField(String hint, String value) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setText(value);
        field.setHintTextColor(muted);
        field.setTextColor(ink);
        field.setTextSize(15);
        field.setPadding(dp(14), 0, dp(14), 0);
        field.setBackground(roundedBackground(panel, line, 10));
        field.setSingleLine(true);
        return field;
    }

    private void showStyledDialog(AlertDialog dialog) {
        dialog.setOnShowListener(ignored -> styleDialog(dialog));
        dialog.show();
    }

    private void styleDialog(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(roundedBackground(panel, line, 14));
        }
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positive != null) {
            positive.setTextColor(accent);
        }
        if (negative != null) {
            negative.setTextColor(muted);
        }
        TextView message = dialog.findViewById(android.R.id.message);
        if (message != null) {
            message.setTextColor(muted);
        }
    }

    private TextView dialogTitle(String text) {
        TextView title = label(text, 20, ink, Typeface.BOLD);
        title.setPadding(dp(24), dp(20), dp(24), dp(8));
        return title;
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

    private void requestHighRefreshRate() {
        Display display = getWindowManager().getDefaultDisplay();
        float highestRefreshRate = display.getRefreshRate();
        for (Display.Mode mode : display.getSupportedModes()) {
            highestRefreshRate = Math.max(highestRefreshRate, mode.getRefreshRate());
        }
        if (highestRefreshRate <= 0f) {
            return;
        }
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.preferredRefreshRate = highestRefreshRate;
        getWindow().setAttributes(attributes);
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

    private static final int VOCAB_SPACER = 0;
    private static final int VOCAB_SECTION = 1;
    private static final int VOCAB_WORD = 2;

    private static final class FlashCardLayout extends LinearLayout {
        FlashCardLayout(Context context) {
            super(context);
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }
    }

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
            items.add(new VocabularyItem(VOCAB_SPACER, null, null));
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
            return 3;
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

        String kanaAtPosition(int position) {
            if (position < 0 || position >= items.size()) {
                return null;
            }
            return items.get(position).kana;
        }

        String kanaAtOrAfterPosition(int position) {
            for (int index = Math.max(0, position); index < items.size(); index++) {
                String kana = items.get(index).kana;
                if (kana != null) {
                    return kana;
                }
            }
            return null;
        }

        String firstKana() {
            return kanaAtOrAfterPosition(0);
        }

        String lastKana() {
            for (int index = items.size() - 1; index >= 0; index--) {
                String kana = items.get(index).kana;
                if (kana != null) {
                    return kana;
                }
            }
            return null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            VocabularyItem item = items.get(position);
            if (item.kind == VOCAB_SPACER) {
                View spacer = new View(MainActivity.this);
                spacer.setBackgroundColor(paper);
                spacer.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Math.max(1, vocabularyControlsHeight)
                ));
                return spacer;
            }
            if (item.kind == VOCAB_SECTION) {
                return buildKanaSectionRow(item.kana, position == 1);
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
            addTableCell(tableHeader, "Word", 0.9f, Color.rgb(255, 247, 237), Typeface.BOLD, 10);
            addTableCell(tableHeader, "Reading", 1.0f, Color.rgb(255, 247, 237), Typeface.BOLD, 10);
            addTableCell(tableHeader, "Romaji", 0.8f, Color.rgb(255, 247, 237), Typeface.BOLD, 10);
            addTableCell(tableHeader, "Type", 1.25f, Color.rgb(255, 247, 237), Typeface.BOLD, 10);
            addTableCell(tableHeader, "Meaning", 1.65f, Color.rgb(255, 247, 237), Typeface.BOLD, 10);
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
            addTableCell(row, kanji, 0.9f, kanjiColor, Typeface.BOLD, 13);
            addTableCell(row, word.furigana, 1.0f, ink, Typeface.NORMAL, 12);
            addTableCell(row, word.romaji, 0.8f, muted, Typeface.NORMAL, 11);
            addTableCell(row, word.type, 1.25f, muted, Typeface.NORMAL, 10);
            addTableCell(row, word.meaning, 1.65f, ink, Typeface.NORMAL, 12);
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

    private final class StudyListAdapter extends BaseAdapter {
        private static final int SPACER = 0;
        private static final int SECTION = 1;
        private static final int ROW = 2;

        private final String section;
        private final List<StudyItem> items = new ArrayList<>();
        private final String[] labels;
        private final float[] weights;

        StudyListAdapter(String section, List<StudyRow> rows) {
            this.section = section;
            if ("grammar".equals(section)) {
                labels = new String[]{"Grammar", "Romaji", "Meaning"};
                weights = new float[]{1.45f, 1.1f, 2.45f};
            } else {
                labels = new String[]{"Kanji", "On'yomi", "Kun'yomi", "Meaning"};
                weights = new float[]{0.65f, 1.65f, 1.65f, 2.05f};
            }
            items.add(new StudyItem(SPACER, null, null));
            String previousKana = null;
            for (StudyRow row : rows) {
                String kana = studyRowKana(section, row);
                if (kana != null && !kana.equals(previousKana)) {
                    items.add(new StudyItem(SECTION, null, kana));
                    previousKana = kana;
                }
                items.add(new StudyItem(ROW, row, kana));
            }
        }

        @Override public int getCount() { return items.size(); }
        @Override public StudyRow getItem(int position) {
            return position >= 0 && position < items.size() ? items.get(position).row : null;
        }
        @Override public long getItemId(int position) { return position; }
        @Override public int getViewTypeCount() { return 3; }
        @Override public int getItemViewType(int position) {
            return items.get(position).kind;
        }
        @Override public boolean areAllItemsEnabled() { return false; }
        @Override public boolean isEnabled(int position) { return false; }

        int positionForKana(String kana) {
            for (int index = 0; index < items.size(); index++) {
                StudyItem item = items.get(index);
                if (item.kind == SECTION && kana.equals(item.kana)) {
                    return index;
                }
            }
            return -1;
        }

        String kanaAtPosition(int position) {
            if (position < 0 || position >= items.size()) {
                return null;
            }
            return items.get(position).kana;
        }

        String kanaAtOrAfterPosition(int position) {
            for (int index = Math.max(0, position); index < items.size(); index++) {
                String kana = items.get(index).kana;
                if (kana != null) {
                    return kana;
                }
            }
            return null;
        }

        String firstKana() {
            return kanaAtOrAfterPosition(0);
        }

        String lastKana() {
            for (int index = items.size() - 1; index >= 0; index--) {
                String kana = items.get(index).kana;
                if (kana != null) {
                    return kana;
                }
            }
            return null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            StudyItem adapterItem = items.get(position);
            if (adapterItem.kind == SPACER) {
                View spacer = new View(MainActivity.this);
                spacer.setBackgroundColor(paper);
                spacer.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Math.max(1, vocabularyControlsHeight)
                ));
                return spacer;
            }
            if (adapterItem.kind == SECTION) {
                return buildStudySectionRow(adapterItem.kana, position == 1);
            }

            StudyRow item = adapterItem.row;
            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp("grammar".equals(section) ? 66 : 62));
            row.setBackgroundColor(position % 2 == 0
                    ? panel
                    : (darkMode ? Color.rgb(39, 27, 37) : Color.rgb(255, 247, 239)));
            for (int index = 0; index < item.values.length; index++) {
                int color = index == 0 ? (darkMode ? Color.rgb(255, 216, 200) : accent) : (index == item.values.length - 1 ? ink : muted);
                int style = index == 0 ? Typeface.BOLD : Typeface.NORMAL;
                addStudyCell(row, item.values[index], weights[index], color, style, index == 0 ? 13 : 11);
            }
            return row;
        }

        private View buildStudySectionRow(String kana, boolean firstSection) {
            LinearLayout sectionView = new LinearLayout(MainActivity.this);
            sectionView.setOrientation(LinearLayout.VERTICAL);
            sectionView.setPadding(0, firstSection ? dp(12) : dp(52), 0, 0);
            sectionView.setBackgroundColor(paper);

            View marker = new View(MainActivity.this);
            marker.setBackgroundColor(accent);
            LinearLayout.LayoutParams markerParams = new LinearLayout.LayoutParams(dp(48), dp(3));
            markerParams.setMargins(dp(16), 0, 0, dp(13));
            sectionView.addView(marker, markerParams);

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
            sectionView.addView(title, titleParams);

            LinearLayout header = new LinearLayout(MainActivity.this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setBackgroundColor(topbar);
            header.setMinimumHeight(dp(42));
            for (int index = 0; index < labels.length; index++) {
                addStudyCell(header, labels[index], weights[index], Color.rgb(255, 247, 237), Typeface.BOLD, 10);
            }
            sectionView.addView(header, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return sectionView;
        }

        private void addStudyCell(
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

    private static final class StudyItem {
        final int kind;
        final StudyRow row;
        final String kana;

        StudyItem(int kind, StudyRow row, String kana) {
            this.kind = kind;
            this.row = row;
            this.kana = kana;
        }
    }
}
