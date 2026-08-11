# JLPT Kotoba

JLPT Kotoba is an offline JLPT vocabulary, flashcard, grammar, and kanji study app for Windows and Android.

## Included study lists

- Vocabulary: N5 (644), N4 (571), N3 (192), and N2 (99)
- Grammar: N5 (84), N4 (132), N3 (182), N2 (197), and N1 (253)
- Kanji: N5 (80), N4 (167), N3 (370), N2 (374), and N1 (1,504)

The bundled lists are reproduced from JLPT Sensei and work offline after installation. JLPT Sensei notes that these are study lists, not official JLPT specifications. Vocabulary is intentionally unavailable for N1 because the source does not publish an N1 vocabulary list.

## Downloads

- `JLPT Kotoba.exe` — Windows desktop build
- `JLPT Kotoba.apk` — installable Android build

Both apps work locally. No account, internet permission, analytics, or cloud storage is used.

## Windows features

- Compact APK-style toolbar, animated slide-out study menu, and right-side Settings panel
- Responsive split-screen layout down to a 600×480 window
- Automatic active-kana tracking with smooth jumps to each vocabulary section
- Clearly separated, aligned per-hiragana vocabulary tables
- Full-text search across vocabulary, grammar, and kanji; CSV importing and vocabulary editing
- Centered flashcards with left/right card navigation, center reveal, Restart-to-ready, progress, and saved sessions
- Context-aware N5–N1 level selector, light and dark themes, and keyboard navigation

## Android features

- Vocabulary, grammar, and kanji browsing with full-text search
- Clearly separated and aligned per-hiragana vocabulary tables
- Finger-tracked vocabulary controls that collapse and reveal with the list scroll
- Finger-tracked left study menu and right Settings menu, including outside-panel closing gestures
- Fast, natural ease-out scrolling when selecting a kana section
- Context-aware N5–N1 level selector
- Centered flashcard decks with left/right card navigation, a visual progress bar, Restart-to-ready, and automatic saving
- Settings panel for theme, vocabulary editing, and CSV import
- A transient fast scrollbar while browsing long lists
- Long-press deletion for imported custom words
- Phone and tablet layouts

Android stores custom vocabulary, settings, and flashcard progress in the app's private storage.

## CSV format

Use UTF-8 CSV files with this header:

```csv
word,reading,romaji,type,meaning
勉強,べんきょう,benkyou,Noun,study
```

Word, reading, and meaning are required. Romaji and type are optional.

## Project structure

```text
desktop/   Windows CustomTkinter source and packaging files
android/   Native Android source and Gradle build
tools/     Reproducible JLPT Sensei list synchronizer
```

The root `custom_vocabulary.json` and `flashcard_progress.json` files are local Windows user data and are intentionally excluded from Git.

## Development

### Windows

From `desktop/`:

```powershell
python -m pip install -r requirements.txt
python app.py
```

Run `desktop/build_exe.bat` to create a new Windows executable. Windows Smart App Control may block newly built unsigned executables.

### Android

Android requires JDK 17 and Android SDK 36:

```powershell
cd android
.\gradlew.bat assembleDebug
```

The generated APK is located at:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```
