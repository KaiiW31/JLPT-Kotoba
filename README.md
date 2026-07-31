# JLPT Kotoba

JLPT Kotoba is an offline Japanese vocabulary and flashcard app for Windows and Android.

## Included vocabulary

- N5: 675 words
- N4: 680 words
- N3, N2, and N1: placeholders ready for future imports

## Downloads

- `JLPT Kotoba.exe` — existing Windows desktop build
- `JLPT Kotoba.apk` — installable Android build

Both apps work locally. No account, internet permission, analytics, or cloud storage is used.

## Android features

- Vocabulary browsing and full-text search
- Clearly separated per-hiragana vocabulary tables
- Animated slide-out menu with expandable あ–ん navigation
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
