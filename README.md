# JLPT Kotoba

JLPT Kotoba is an offline Japanese vocabulary and flashcard app for Windows and Android.

## Included vocabulary

- N5: 675 words
- N4: 680 words
- N3, N2, and N1: placeholders ready for future imports

## Downloads

- `JLPT Kotoba.exe` — Windows desktop build
- `JLPT Kotoba.apk` — installable Android build

Both apps work locally. No account, internet permission, analytics, or cloud storage is used.

## Windows features

- Compact APK-style toolbar and animated slide-out study menu
- Automatic active-kana tracking with smooth jumps to each vocabulary section
- Clearly separated, aligned per-hiragana vocabulary tables
- Full-text search, CSV importing, and editing for custom vocabulary
- Centered flashcards with Back, Reveal/Next, Restart, progress, and saved sessions
- N5–N1 level selector, light and dark themes, and keyboard navigation

## Android features

- Vocabulary browsing and full-text search
- Clearly separated and aligned per-hiragana vocabulary tables
- Finger-tracked vocabulary controls that collapse and reveal with the list scroll
- Finger-tracked slide-out menu with automatic visible-kana highlighting
- Fast, natural ease-out scrolling when selecting a kana section
- N5–N1 level selector
- Centered flashcard decks with a visual progress bar and automatic saving
- Light and dark themes
- CSV vocabulary importing
- Long-press deletion for imported custom words
- Phone and tablet layouts

Android stores custom vocabulary, settings, and flashcard progress in the app's private storage.

## CSV format

Use UTF-8 CSV files with this header:

```csv
kanji,furigana,romaji,meaning
勉強,べんきょう,benkyou,study
```

Kanji can be blank. Furigana, romaji, and meaning are required.

## Project structure

```text
desktop/   Windows CustomTkinter source and packaging files
android/   Native Android source and Gradle build
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
