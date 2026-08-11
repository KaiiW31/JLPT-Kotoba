import bisect
import csv
import ctypes
import json
import math
import random
import sys
import time
from pathlib import Path


def enable_windows_dpi_awareness():
    if sys.platform != "win32":
        return
    try:
        ctypes.windll.user32.SetProcessDpiAwarenessContext.argtypes = [ctypes.c_void_p]
        ctypes.windll.user32.SetProcessDpiAwarenessContext.restype = ctypes.c_bool
        if ctypes.windll.user32.SetProcessDpiAwarenessContext(ctypes.c_void_p(-4)):
            return
    except Exception:
        pass
    try:
        ctypes.windll.shcore.SetProcessDpiAwareness(2)
        return
    except Exception:
        pass
    try:
        ctypes.windll.user32.SetProcessDPIAware()
    except Exception:
        pass


enable_windows_dpi_awareness()


import tkinter as tk
from tkinter import ttk, filedialog

import customtkinter as ctk
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageGrab, ImageTk


SOURCE_DIR = Path(__file__).resolve().parent
APP_DIR = Path(sys.executable).resolve().parent if getattr(sys, "frozen", False) else SOURCE_DIR.parent
BUNDLE_DIR = Path(getattr(sys, "_MEIPASS", SOURCE_DIR))
ICON_PATH = BUNDLE_DIR / "assets" / "app_icon.ico"
DATA_PATH = BUNDLE_DIR / "data" / "study_data.json"
PROGRESS_PATH = APP_DIR / "flashcard_progress.json"
CUSTOM_VOCAB_PATH = APP_DIR / "custom_vocabulary.json"

LEVELS = ["N5", "N4", "N3", "N2", "N1"]
STUDY_VIEWS = ["Vocabulary", "Flashcards", "Grammar", "Kanji"]
ACCENT = "#B91C1C"
ACCENT_DARK = "#A11D26"
ACCENT_HOVER = "#991B1B"
BUTTON_PRIMARY_BORDER = ("#DC2626", "#F87171")
BUTTON_SECONDARY_BG = ("#FFF1F2", "#32151D")
BUTTON_SECONDARY_HOVER = ("#FFE4E6", "#421923")
BUTTON_SECONDARY_TEXT = ("#7F1D1D", "#FFE4E6")
BUTTON_TOP_BG = ("#6F1418", "#34131B")
BUTTON_TOP_HOVER = ("#991B1B", "#5F1111")
BUTTON_DISABLED_BG = ("#D8C9C2", "#3A3034")
BUTTON_DISABLED_TEXT = ("#8F817A", "#918188")
PAPER = ("#FFF9F1", "#120D13")
PANEL = ("#FFFDF8", "#211720")
INK = ("#24191D", "#FFF3E8")
MUTED = ("#765E64", "#D4B7BA")
LINE = ("#E8CEC2", "#6A3A47")
CONTENT_BORDER = ("#E8CEC2", "#F3E6DB")
KANA_BUTTON_BG = BUTTON_SECONDARY_BG
KANA_CHILD_BG = BUTTON_SECONDARY_BG
KANA_BUTTON_HOVER = BUTTON_SECONDARY_HOVER
TABLE_ROW_HOVER = ("#FFF1E6", "#2F202A")
TABLE_ROW_ALT = ("#FFF4EA", "#261A23")
TABLE_GRID = ("#9F9188", "#9A818B")
UI_SCALE = 1.0

KANA_ROWS = [
    ["あ", "い", "う", "え", "お"],
    ["か", "き", "く", "け", "こ"],
    ["さ", "し", "す", "せ", "そ"],
    ["た", "ち", "つ", "て", "と"],
    ["な", "に", "ぬ", "ね", "の"],
    ["は", "ひ", "ふ", "へ", "ほ"],
    ["ま", "み", "む", "め", "も"],
    ["や", "ゆ", "よ"],
    ["ら", "り", "る", "れ", "ろ"],
    ["わ", "を", "ん"],
]
KANA_ORDER = [kana for row in KANA_ROWS for kana in row]
KANA_INDEX = {kana: index for index, kana in enumerate(KANA_ORDER)}
KANA_GROUP_MEMBERS = {row[0]: row for row in KANA_ROWS}
KANA_GROUP_BY_MEMBER = {kana: row[0] for row in KANA_ROWS for kana in row}
KANA_ROMAJI = {
    "あ": "a", "い": "i", "う": "u", "え": "e", "お": "o",
    "か": "ka", "き": "ki", "く": "ku", "け": "ke", "こ": "ko",
    "さ": "sa", "し": "shi", "す": "su", "せ": "se", "そ": "so",
    "た": "ta", "ち": "chi", "つ": "tsu", "て": "te", "と": "to",
    "な": "na", "に": "ni", "ぬ": "nu", "ね": "ne", "の": "no",
    "は": "ha", "ひ": "hi", "ふ": "fu", "へ": "he", "ほ": "ho",
    "ま": "ma", "み": "mi", "む": "mu", "め": "me", "も": "mo",
    "や": "ya", "ゆ": "yu", "よ": "yo",
    "ら": "ra", "り": "ri", "る": "ru", "れ": "re", "ろ": "ro",
    "わ": "wa", "を": "wo", "ん": "n",
}
KANA_NORMALIZE = str.maketrans(
    {
        "ぁ": "あ", "ぃ": "い", "ぅ": "う", "ぇ": "え", "ぉ": "お",
        "ゃ": "や", "ゅ": "ゆ", "ょ": "よ", "っ": "つ",
        "が": "か", "ぎ": "き", "ぐ": "く", "げ": "け", "ご": "こ",
        "ざ": "さ", "じ": "し", "ず": "す", "ぜ": "せ", "ぞ": "そ",
        "だ": "た", "ぢ": "ち", "づ": "つ", "で": "て", "ど": "と",
        "ば": "は", "び": "ひ", "ぶ": "ふ", "べ": "へ", "ぼ": "ほ",
        "ぱ": "は", "ぴ": "ひ", "ぷ": "ふ", "ぺ": "へ", "ぽ": "ほ",
        "ゔ": "う",
    }
)


class JLPTStudyApp(ctk.CTk):
    def __init__(self):
        super().__init__()
        ctk.set_appearance_mode("System")
        ctk.set_default_color_theme("blue")
        self.display_scale = detect_display_scale(self)
        self.reading_scale = 1.16
        ctk.set_widget_scaling(UI_SCALE)
        self.title("JLPT Kotoba")
        self.geometry("1280x780")
        self.minsize(600, 480)
        self.configure(fg_color=PAPER)
        if ICON_PATH.exists():
            self.iconbitmap(ICON_PATH)

        self.data = load_data()
        self.words_cache = {}
        self.current_level = "N5"
        self.current_view = "Vocabulary"
        self.active_nav_button = None
        self.active_level_button = None
        self.flash_rail_selected_level = None
        self.flashcards_ready = False
        self.flash_deck = []
        self.flash_index = -1
        self.card_answer_visible = False
        self.flash_complete = False
        self.completion_animation_after_id = None
        self.completion_canvas_active = False
        self.completion_canvas_ready = False
        self.completion_show_buttons = False
        self.completion_button_bounds = {}
        self.completion_hover_action = None
        self.completion_style_key = None
        self.completion_panel_color = None
        self.confetti_after_id = None
        self.confetti_particles = []
        self.restart_dialog = None
        self.restart_dialog_image = None
        self.restart_overlay_cache = None
        self.restart_overlay_cache_key = None
        self.vocab_editor_overlay = None
        self.vocab_delete_overlay = None
        self.vocab_import_overlay = None
        self.flash_progress = self.load_flash_progress()
        self.custom_vocab = self.load_custom_vocabulary()
        self.search_after_id = None
        self.root_configure_after_id = None
        self.responsive_compact = None
        self.last_vocab_render_key = None
        self.tree_items_by_kana = {}
        self.expanded_kana_groups = set()
        self.hovered_tree_item = None
        self.active_kana = None
        self.pencil_icon_image = None
        self.import_icon_image = None
        self.drawer_open = False
        self.drawer_animation_after_id = None
        self.drawer_x = 0
        self.drawer_target_x = 0
        self.drawer_animation_started_at = None
        self.drawer_animation_start_x = 0.0
        self.drawer_animation_duration = 0.26
        self.drawer_backdrop_image = None
        self.drawer_backdrop_cache = None
        self.drawer_backdrop_cache_key = None
        self.drawer_close_callback = None
        self.settings_open = False
        self.settings_animation_after_id = None
        self.settings_x = 0.0
        self.settings_target_x = 0.0
        self.settings_animation_start_x = 0.0
        self.settings_animation_started_at = None
        self.settings_animation_duration = 0.26

        self.search_var = ctk.StringVar()
        self.flash_level_var = ctk.StringVar(value="N5")
        self.card_front_var = ctk.StringVar(value="Choose a level and start.")
        self.card_back_var = ctk.StringVar(value="")
        self.card_hint_var = ctk.StringVar(value="")
        self.count_var = ctk.StringVar()
        self.flash_save_status_var = ctk.StringVar(value="Not saved")
        self.flash_save_progress_var = ctk.StringVar(value="0/0")
        self.save_toast_var = ctk.StringVar(value="")
        self.save_toast_after_id = None
        self.save_toast_animation_after_id = None
        self.app_toast_var = ctk.StringVar(value="")
        self.app_toast_animation_after_id = None

        self._build_ui()
        self.bind_keyboard_shortcuts()
        self.reset_flashcards()
        self.show_vocabulary("N5")

    def _build_ui(self):
        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(1, weight=1)

        self.topbar = ctk.CTkFrame(self, height=72, corner_radius=0, fg_color=("#8F1717", "#4A0B14"))
        self.topbar.grid(row=0, column=0, sticky="ew")
        self.topbar.grid_propagate(False)
        self.topbar.grid_columnconfigure(1, weight=1)

        self.menu_button = themed_button(
            self.topbar,
            variant="topbar",
            text="☰",
            width=48,
            height=46,
            corner_radius=10,
            font=ctk.CTkFont(size=25),
            command=self.open_drawer,
        )
        self.menu_button.grid(row=0, column=0, sticky="w", padx=(22, 14), pady=13)
        self.menu_button.bind("<Enter>", self.prepare_drawer_backdrop_cache, add="+")

        self.brand_font = ctk.CTkFont(size=25, weight="bold")
        self.brand_label = ctk.CTkLabel(
            self.topbar,
            text="JLPT Kotoba",
            font=self.brand_font,
            text_color="#FFF7ED",
        )
        self.brand_label.grid(row=0, column=1, sticky="w")

        self.settings_button = themed_button(
            self.topbar,
            variant="topbar_active",
            text="Settings",
            width=96,
            height=42,
            corner_radius=10,
            font=ctk.CTkFont(size=14),
            command=self.open_settings,
        )
        self.settings_button.grid(row=0, column=2, sticky="e", padx=22, pady=15)

        self.shell = ctk.CTkFrame(self, fg_color="transparent")
        self.shell.grid(row=1, column=0, sticky="nsew", padx=28, pady=(24, 26))
        self.shell.grid_columnconfigure(0, weight=1)
        self.shell.grid_rowconfigure(1, weight=1)

        self.header = ctk.CTkFrame(self.shell, fg_color="transparent")
        self.header.grid(row=0, column=0, sticky="ew", pady=(0, 16))
        self.header.grid_columnconfigure(0, weight=1)
        self.title_font = ctk.CTkFont(size=31, weight="bold")
        self.title_label = ctk.CTkLabel(self.header, text="", font=self.title_font, text_color=INK)
        self.title_label.grid(row=0, column=0, sticky="w")
        self.vocab_import_button = secondary_button(
            self.header,
            text="Import CSV",
            width=122,
            height=44,
            image=None,
            compound="left",
            font=ctk.CTkFont(size=13, weight="bold"),
            command=self.open_vocab_import_file,
        )
        self.vocab_import_button.grid(row=0, column=1, rowspan=2, sticky="e")
        self.draw_import_icon()
        self.flash_header_actions = ctk.CTkFrame(self.header, fg_color="transparent")
        self.flash_header_actions.grid(row=0, column=1, rowspan=2, sticky="e")
        self.flash_header_actions.grid_columnconfigure((0, 1), weight=0)
        self.flash_save_status_panel = ctk.CTkFrame(
            self.flash_header_actions,
            width=158,
            height=44,
            fg_color=PANEL,
            border_width=1,
            border_color=LINE,
            corner_radius=8,
        )
        self.flash_save_status_panel.grid(row=0, column=0, sticky="e", padx=(0, 8))
        self.flash_save_status_panel.grid_propagate(False)
        self.flash_save_status_panel.grid_columnconfigure(0, weight=1)
        self.flash_save_status_label = ctk.CTkLabel(
            self.flash_save_status_panel,
            textvariable=self.flash_save_status_var,
            text_color=MUTED,
            font=ctk.CTkFont(size=12, weight="bold"),
            anchor="w",
            width=82,
            height=16,
        )
        self.flash_save_status_label.place(x=10, y=5)
        self.flash_save_progress_label = ctk.CTkLabel(
            self.flash_save_status_panel,
            textvariable=self.flash_save_progress_var,
            text_color=MUTED,
            font=ctk.CTkFont(size=11, weight="bold"),
            anchor="e",
            width=54,
            height=16,
        )
        self.flash_save_progress_label.place(x=94, y=5)
        self.flash_save_bar = ctk.CTkProgressBar(
            self.flash_save_status_panel,
            width=136,
            height=8,
            corner_radius=8,
            fg_color=("#E8DAD2", "#3A3034"),
            progress_color=("#F59E0B", "#FBBF24"),
        )
        self.flash_save_bar.place(x=10, y=28)
        self.flash_save_bar.set(0.0)
        self.flash_save_button = red_button(
            self.flash_header_actions,
            text="Save Progress",
            width=118,
            height=44,
            corner_radius=8,
            font=ctk.CTkFont(size=13, weight="bold"),
            command=self.save_flashcards,
        )
        self.flash_save_button.grid(row=0, column=1, sticky="e")
        self.flash_header_actions.grid_remove()
        self.subtitle_label = ctk.CTkLabel(self.header, text="", text_color=MUTED, anchor="w", font=ctk.CTkFont(size=14))
        self.subtitle_label.grid(row=1, column=0, sticky="ew", pady=(3, 0))

        self.content = ctk.CTkFrame(self.shell, fg_color="transparent")
        self.content.grid(row=1, column=0, sticky="nsew")
        self.content.grid_columnconfigure(0, weight=1)
        self.content.grid_rowconfigure(0, weight=1)

        self.app_toast = ctk.CTkFrame(
            self,
            fg_color=PANEL,
            border_width=1,
            border_color=LINE,
            corner_radius=9,
        )
        self.app_toast_label = ctk.CTkLabel(
            self.app_toast,
            textvariable=self.app_toast_var,
            text_color=INK,
            font=ctk.CTkFont(size=13, weight="bold"),
        )
        self.app_toast_label.pack(padx=18, pady=9)
        self.app_toast.place_forget()

        self.vocab_view = ctk.CTkFrame(self.content, fg_color="transparent")
        self.flash_view = ctk.CTkFrame(self.content, fg_color="transparent")
        self.vocab_view.grid_columnconfigure(0, weight=1)
        self.vocab_view.grid_columnconfigure(1, weight=0)
        self.vocab_view.grid_columnconfigure(2, weight=1)
        self.vocab_view.grid_rowconfigure(1, weight=1)
        self.flash_view.grid_columnconfigure(0, weight=1)
        self.flash_view.grid_rowconfigure(0, weight=1)
        self.flash_view.grid_rowconfigure(1, weight=0)

        self.vocab_sheet = ctk.CTkFrame(self.vocab_view, fg_color="transparent", width=1320)
        self.vocab_sheet.grid(row=0, column=1, rowspan=2, sticky="ns")
        self.vocab_sheet.grid_propagate(False)
        self.vocab_sheet.grid_columnconfigure(0, weight=1)
        self.vocab_sheet.grid_rowconfigure(1, weight=1)
        self.vocab_view.bind("<Configure>", self.resize_vocab_sheet)

        self.vocab_controls = ctk.CTkFrame(self.vocab_sheet, fg_color="transparent")
        self.vocab_controls.grid(row=0, column=0, sticky="ew", pady=(0, 14))
        self.vocab_controls.grid_columnconfigure(0, weight=1)

        self.search_wrap = ctk.CTkFrame(
            self.vocab_controls,
            height=50,
            fg_color=PANEL,
            border_width=1,
            border_color=LINE,
            corner_radius=6,
        )
        self.search_wrap.grid(row=0, column=0, sticky="ew")
        self.search_wrap.grid_columnconfigure(1, weight=1)

        self.search_icon = ctk.CTkLabel(self.search_wrap, text="", width=28, height=28)
        self.search_icon.grid(row=0, column=0, padx=(15, 6), pady=9)
        self.search_entry = ctk.CTkEntry(
            self.search_wrap,
            textvariable=self.search_var,
            height=42,
            placeholder_text="Search kanji, kana, romaji, or meaning...",
            placeholder_text_color=MUTED,
            text_color=INK,
            font=ctk.CTkFont(size=15),
            fg_color=PANEL,
            border_width=0,
        )
        self.search_entry.grid(row=0, column=1, sticky="ew", padx=(0, 12), pady=4)
        self.search_wrap.bind("<Button-1>", lambda _event: self.search_entry.focus_set())
        self.search_icon.bind("<Button-1>", lambda _event: self.search_entry.focus_set())
        self.draw_search_icon()
        self.search_var.trace_add("write", lambda *_: self.schedule_vocab_render())

        self.vocab_edit_actions = ctk.CTkFrame(self.vocab_controls, fg_color="transparent")
        self.vocab_edit_actions.grid(row=0, column=1, sticky="e", padx=(10, 0))
        self.vocab_edit_button = secondary_button(
            self.vocab_edit_actions,
            text="",
            width=54,
            height=50,
            image=None,
            command=lambda: self.open_vocab_editor("edit"),
        )
        self.vocab_edit_button.grid(row=0, column=0)
        self.vocab_edit_actions.grid_remove()
        self.update_vocab_edit_buttons()

        self.count_label = ctk.CTkLabel(
            self.vocab_controls,
            textvariable=self.count_var,
            text_color=MUTED,
            font=ctk.CTkFont(size=14, weight="bold"),
            anchor="w",
        )
        self.count_label.grid(row=1, column=0, sticky="w", pady=(12, 0))

        table_wrap = ctk.CTkFrame(self.vocab_sheet, fg_color=PANEL, border_width=1, border_color=LINE, corner_radius=14)
        table_wrap.grid(row=1, column=0, sticky="nsew")
        table_wrap.grid_columnconfigure(0, weight=1)
        table_wrap.grid_rowconfigure(0, weight=1)

        self.vocab_table = VocabGrid(table_wrap, scale=self.reading_scale, active_kana_callback=self.sync_kana_from_table, selection_callback=self.update_vocab_edit_buttons)
        self.vocab_table.grid(row=0, column=0, sticky="nsew", padx=8, pady=8)
        self.bind_all("<Button-1>", self.clear_vocab_selection_on_outside_click, add="+")

        self._build_flashcards()
        self._build_drawer()
        self._build_settings_drawer()
        self.flash_view.grid(row=0, column=0, sticky="nsew")
        self.vocab_view.grid(row=0, column=0, sticky="nsew")
        self.vocab_view.tkraise()
        self.bind("<Configure>", self.schedule_root_layout_refresh, add="+")
        self.after_idle(self.apply_responsive_layout)
        self.after(420, self.prepare_drawer_backdrop_cache)

    def _build_drawer(self):
        self.drawer_width = 344
        self.drawer_scrim = tk.Frame(self, bg=theme_color(("#2A1117", "#080508")), bd=0, highlightthickness=0)
        self.drawer_backdrop_label = tk.Label(
            self.drawer_scrim,
            bg=theme_color(("#2A1117", "#080508")),
            bd=0,
            highlightthickness=0,
        )
        self.drawer_backdrop_label.place(relx=0, rely=0, relwidth=1, relheight=1)
        self.drawer_backdrop_label.bind("<Button-1>", lambda _event: self.close_drawer())

        self.drawer_panel = ctk.CTkFrame(
            self.drawer_scrim,
            width=self.drawer_width,
            corner_radius=0,
            fg_color=PANEL,
            border_width=1,
            border_color=LINE,
        )
        self.drawer_panel.place(x=-self.drawer_width, y=0, relheight=1)
        self.drawer_panel.grid_propagate(False)
        self.drawer_panel.grid_columnconfigure(0, weight=1)
        self.drawer_panel.grid_rowconfigure(6, weight=1)

        drawer_header = ctk.CTkFrame(self.drawer_panel, height=72, corner_radius=0, fg_color=("#8F1717", "#4A0B14"))
        drawer_header.grid(row=0, column=0, sticky="ew")
        drawer_header.grid_propagate(False)
        drawer_header.grid_columnconfigure(0, weight=1)
        ctk.CTkLabel(
            drawer_header,
            text="JLPT Kotoba",
            text_color="#FFF7ED",
            font=ctk.CTkFont(size=23, weight="bold"),
        ).grid(row=0, column=0, sticky="w", padx=22, pady=20)
        themed_button(
            drawer_header,
            variant="topbar",
            text="×",
            width=42,
            height=42,
            corner_radius=9,
            font=ctk.CTkFont(size=24),
            command=self.close_drawer,
        ).grid(row=0, column=1, sticky="e", padx=16, pady=15)

        ctk.CTkLabel(
            self.drawer_panel,
            text="Study",
            text_color=MUTED,
            font=ctk.CTkFont(size=12, weight="bold"),
            anchor="w",
        ).grid(row=1, column=0, sticky="ew", padx=20, pady=(18, 7))

        nav = ctk.CTkFrame(self.drawer_panel, fg_color="transparent")
        nav.grid(row=2, column=0, sticky="ew", padx=16)
        nav.grid_columnconfigure((0, 1), weight=1)
        self.nav_buttons = {}
        for index, label in enumerate(STUDY_VIEWS):
            button = themed_button(
                nav,
                variant="secondary",
                text=label,
                height=44,
                corner_radius=9,
                font=ctk.CTkFont(size=14, weight="bold"),
                command=lambda value=label: self.select_drawer_view(value),
            )
            row, column = divmod(index, 2)
            button.grid(
                row=row,
                column=column,
                sticky="ew",
                padx=(0, 5) if column == 0 else (5, 0),
                pady=(0, 5) if row == 0 else (5, 0),
            )
            self.nav_buttons[label] = button

        ctk.CTkLabel(
            self.drawer_panel,
            text="JLPT Level",
            text_color=MUTED,
            font=ctk.CTkFont(size=12, weight="bold"),
            anchor="w",
        ).grid(row=3, column=0, sticky="ew", padx=20, pady=(18, 7))

        levels = ctk.CTkFrame(self.drawer_panel, fg_color="transparent")
        levels.grid(row=4, column=0, sticky="ew", padx=16)
        levels.grid_columnconfigure(tuple(range(len(LEVELS))), weight=1)
        self.level_buttons = {}
        for index, level in enumerate(LEVELS):
            button = themed_button(
                levels,
                variant="secondary",
                text=level,
                width=52,
                height=40,
                corner_radius=9,
                font=ctk.CTkFont(size=13, weight="bold"),
                command=lambda value=level: self.select_drawer_level(value),
            )
            button.grid(row=0, column=index, sticky="ew", padx=3)
            self.level_buttons[level] = button
        self.flash_rail_buttons = self.level_buttons

        self.browse_kana_label = ctk.CTkLabel(
            self.drawer_panel,
            text="Browse by Kana",
            text_color=MUTED,
            font=ctk.CTkFont(size=12, weight="bold"),
            anchor="w",
        )
        self.browse_kana_label.grid(row=5, column=0, sticky="ew", padx=20, pady=(18, 7))

        self.kana_rail = ctk.CTkFrame(
            self.drawer_panel,
            fg_color=PANEL,
            border_width=1,
            border_color=LINE,
            corner_radius=12,
        )
        self.kana_rail.grid(row=6, column=0, sticky="nsew", padx=16, pady=(0, 16))
        self.kana_rail.grid_columnconfigure(0, weight=1)
        self.kana_rail.grid_rowconfigure(0, weight=1)
        self.kana_rail_widget = KanaRail(
            self.kana_rail,
            self.toggle_kana_group,
            self.jump_to_kana_from_drawer,
            scale=self.kana_rail_scale(),
        )
        self.kana_rail_widget.grid(row=0, column=0, sticky="nsew", padx=6, pady=8)
        self.drawer_scrim.bind("<Escape>", lambda _event: self.close_drawer())
        self.drawer_x = -self.drawer_width
        self.drawer_target_x = self.drawer_x
        self.drawer_scrim.place_forget()

    def _build_settings_drawer(self):
        self.settings_width = 344
        self.settings_scrim = tk.Frame(
            self,
            bg=theme_color(("#2A1117", "#080508")),
            bd=0,
            highlightthickness=0,
        )
        settings_backdrop = tk.Frame(
            self.settings_scrim,
            bg=theme_color(("#2A1117", "#080508")),
            bd=0,
            highlightthickness=0,
        )
        settings_backdrop.place(relx=0, rely=0, relwidth=1, relheight=1)
        settings_backdrop.bind("<Button-1>", lambda _event: self.close_settings())

        self.settings_panel = ctk.CTkFrame(
            self.settings_scrim,
            width=self.settings_width,
            corner_radius=0,
            fg_color=PANEL,
            border_width=1,
            border_color=LINE,
        )
        self.settings_panel.place(x=self.winfo_width(), y=0, relheight=1)
        self.settings_panel.grid_propagate(False)
        self.settings_panel.grid_columnconfigure(0, weight=1)

        header = ctk.CTkFrame(self.settings_panel, height=72, corner_radius=0, fg_color=("#8F1717", "#4A0B14"))
        header.grid(row=0, column=0, sticky="ew")
        header.grid_propagate(False)
        header.grid_columnconfigure(0, weight=1)
        ctk.CTkLabel(
            header,
            text="Settings",
            text_color="#FFF7ED",
            font=ctk.CTkFont(size=23, weight="bold"),
        ).grid(row=0, column=0, sticky="w", padx=22, pady=20)
        themed_button(
            header,
            variant="topbar",
            text="×",
            width=42,
            height=42,
            corner_radius=9,
            font=ctk.CTkFont(size=24),
            command=self.close_settings,
        ).grid(row=0, column=1, sticky="e", padx=16, pady=15)

        body = ctk.CTkFrame(self.settings_panel, fg_color="transparent")
        body.grid(row=1, column=0, sticky="nsew", padx=18, pady=18)
        body.grid_columnconfigure(0, weight=1)
        ctk.CTkLabel(body, text="APPEARANCE", text_color=MUTED, font=ctk.CTkFont(size=11, weight="bold"), anchor="w").grid(row=0, column=0, sticky="ew", pady=(0, 8))
        self.settings_theme_button = secondary_button(
            body,
            text=self.theme_action_text(),
            height=46,
            command=self.toggle_theme,
        )
        self.settings_theme_button.grid(row=1, column=0, sticky="ew")

        ctk.CTkLabel(body, text="VOCABULARY", text_color=MUTED, font=ctk.CTkFont(size=11, weight="bold"), anchor="w").grid(row=2, column=0, sticky="ew", pady=(22, 8))
        self.settings_edit_button = secondary_button(
            body,
            text="Edit selected word",
            height=46,
            command=lambda: self.close_settings_then(lambda: self.open_vocab_editor("edit")),
        )
        self.settings_edit_button.grid(row=3, column=0, sticky="ew")
        self.settings_import_button = secondary_button(
            body,
            text="Import CSV",
            height=46,
            command=lambda: self.close_settings_then(self.open_vocab_import_file),
        )
        self.settings_import_button.grid(row=4, column=0, sticky="ew", pady=(10, 0))
        ctk.CTkLabel(
            body,
            text="Study lists: JLPT Sensei\nAvailable offline after installation.",
            text_color=MUTED,
            justify="left",
            anchor="w",
            font=ctk.CTkFont(size=12),
        ).grid(row=5, column=0, sticky="ew", pady=(26, 0))
        self.settings_scrim.bind("<Escape>", lambda _event: self.close_settings())
        self.settings_scrim.place_forget()
        self.update_vocab_edit_buttons()

    def theme_action_text(self):
        return "Use light theme" if ctk.get_appearance_mode() == "Dark" else "Use dark theme"

    def open_settings(self):
        if self.settings_open and self.settings_animation_after_id is None:
            return
        if not self.settings_scrim.place_info():
            self.settings_scrim.place(relx=0, rely=0, relwidth=1, relheight=1)
            self.settings_scrim.lift()
            self.settings_x = float(self.winfo_width())
            self.settings_target_x = self.settings_x
            self.settings_panel.place_configure(x=round(self.settings_x))
        self.settings_theme_button.configure(text=self.theme_action_text())
        self.update_vocab_edit_buttons()
        self.settings_scrim.focus_set()
        self.settings_open = True
        self.start_settings_animation(max(0, self.winfo_width() - self.settings_width))

    def close_settings(self):
        if not self.settings_scrim.place_info():
            return
        self.settings_open = False
        self.start_settings_animation(self.winfo_width())

    def close_settings_then(self, callback):
        if not self.settings_scrim.place_info():
            callback()
            return
        self.close_settings()
        self.after(280, callback)

    def start_settings_animation(self, target_x):
        self.settings_target_x = float(target_x)
        self.settings_animation_start_x = self.settings_x
        distance_fraction = abs(self.settings_target_x - self.settings_x) / max(1, self.settings_width)
        self.settings_animation_duration = max(0.12, 0.26 * distance_fraction)
        self.settings_animation_started_at = time.perf_counter()
        if self.settings_animation_after_id is None:
            self.settings_animation_after_id = self.after(0, self.animate_settings)

    def animate_settings(self):
        elapsed = time.perf_counter() - (self.settings_animation_started_at or time.perf_counter())
        progress = min(1.0, elapsed / max(0.01, self.settings_animation_duration))
        eased = 1.0 - ((1.0 - progress) ** 2)
        self.settings_x = self.settings_animation_start_x + ((self.settings_target_x - self.settings_animation_start_x) * eased)
        self.settings_panel.place_configure(x=round(self.settings_x))
        if progress < 1.0:
            self.settings_animation_after_id = self.after(15, self.animate_settings)
            return
        self.settings_animation_after_id = None
        self.settings_x = self.settings_target_x
        self.settings_panel.place_configure(x=round(self.settings_x))
        if not self.settings_open:
            self.settings_scrim.place_forget()

    def sync_drawer_content(self):
        vocabulary_mode = self.current_view == "Vocabulary"
        if vocabulary_mode:
            self.browse_kana_label.grid()
            self.kana_rail.grid()
        else:
            self.browse_kana_label.grid_remove()
            self.kana_rail.grid_remove()
        n1_enabled = self.current_view in {"Grammar", "Kanji"}
        self.level_buttons["N1"].configure(state="normal" if n1_enabled else "disabled")

    def drawer_backdrop_key(self):
        return (
            ctk.get_appearance_mode(),
            self.winfo_width(),
            self.winfo_height(),
            self.current_view,
            self.current_level,
            self.flash_level_var.get(),
        )

    def prepare_drawer_backdrop_cache(self, _event=None):
        if self.drawer_open or self.drawer_animation_after_id is not None:
            return
        cache_key = self.drawer_backdrop_key()
        if self.drawer_backdrop_cache_key == cache_key and self.drawer_backdrop_cache is not None:
            return
        self.drawer_backdrop_cache = self.create_drawer_backdrop_image()
        self.drawer_backdrop_cache_key = cache_key if self.drawer_backdrop_cache is not None else None

    def create_drawer_backdrop_image(self):
        try:
            self.update_idletasks()
            x = self.winfo_rootx()
            y = self.winfo_rooty()
            width = max(1, self.winfo_width())
            height = max(1, self.winfo_height())
            image = ImageGrab.grab(bbox=(x, y, x + width, y + height)).convert("RGB")
            image = ImageEnhance.Brightness(image).enhance(0.55)
            tint = Image.new("RGB", image.size, theme_color(("#3A151B", "#080508")))
            image = Image.blend(image, tint, 0.16)
            return ImageTk.PhotoImage(image)
        except Exception:
            return None

    def open_drawer(self):
        if not hasattr(self, "drawer_scrim"):
            return
        if self.drawer_open and self.drawer_animation_after_id is None:
            return
        if not self.drawer_scrim.place_info():
            cache_key = self.drawer_backdrop_key()
            if self.drawer_backdrop_cache_key == cache_key and self.drawer_backdrop_cache is not None:
                self.drawer_backdrop_image = self.drawer_backdrop_cache
            else:
                self.drawer_backdrop_image = self.create_drawer_backdrop_image()
            if self.drawer_backdrop_image is not None:
                self.drawer_backdrop_label.configure(image=self.drawer_backdrop_image)
            else:
                self.drawer_backdrop_label.configure(
                    image="",
                    bg=theme_color(("#2A1117", "#080508")),
                )
            self.drawer_scrim.place(relx=0, rely=0, relwidth=1, relheight=1)
            self.drawer_scrim.lift()
            self.drawer_x = -self.drawer_width
            self.drawer_target_x = self.drawer_x
            self.drawer_panel.place_configure(x=self.drawer_x)
            try:
                self.drawer_scrim.grab_set()
            except tk.TclError:
                pass
        self.set_nav_buttons(self.current_view, force=True)
        self.sync_drawer_content()
        active_level = self.flash_level_var.get() if self.current_view == "Flashcards" else self.current_level
        self.set_level_buttons(active_level, force=True)
        self.drawer_scrim.focus_set()
        self.drawer_open = True
        self.start_drawer_animation(0)
        self.after(280, self.reveal_active_kana_in_drawer)

    def close_drawer(self, after=None):
        if after is not None:
            self.drawer_close_callback = after
        if not hasattr(self, "drawer_scrim") or not self.drawer_scrim.place_info():
            callback = self.drawer_close_callback
            self.drawer_close_callback = None
            if callback is not None:
                callback()
            return
        self.drawer_open = False
        self.start_drawer_animation(-self.drawer_width)

    def start_drawer_animation(self, target_x):
        self.drawer_target_x = float(target_x)
        self.drawer_animation_start_x = self.drawer_x
        distance_fraction = abs(self.drawer_target_x - self.drawer_x) / max(
            1,
            self.drawer_width,
        )
        self.drawer_animation_duration = max(0.12, 0.26 * distance_fraction)
        self.drawer_animation_started_at = time.perf_counter()
        if self.drawer_animation_after_id is None:
            self.drawer_animation_after_id = self.after(0, self.animate_drawer)

    def animate_drawer(self):
        now = time.perf_counter()
        elapsed = now - (self.drawer_animation_started_at or now)
        progress = min(1.0, elapsed / max(0.01, self.drawer_animation_duration))
        eased_progress = 1.0 - ((1.0 - progress) ** 2)
        distance = self.drawer_target_x - self.drawer_animation_start_x
        self.drawer_x = self.drawer_animation_start_x + (distance * eased_progress)
        finished = progress >= 1.0
        try:
            self.drawer_panel.place_configure(x=round(self.drawer_x))
        except tk.TclError:
            self.drawer_animation_after_id = None
            return

        if not finished:
            self.drawer_animation_after_id = self.after(15, self.animate_drawer)
            return
        self.drawer_animation_after_id = None
        self.drawer_x = self.drawer_target_x
        self.drawer_animation_started_at = None
        self.drawer_panel.place_configure(x=round(self.drawer_x))
        if self.drawer_x <= -self.drawer_width:
            try:
                self.drawer_scrim.grab_release()
            except tk.TclError:
                pass
            self.drawer_scrim.place_forget()
            self.drawer_backdrop_label.configure(image="")
            self.drawer_backdrop_image = None
            self.drawer_backdrop_cache = None
            self.drawer_backdrop_cache_key = None
            self.after(220, self.prepare_drawer_backdrop_cache)
            callback = self.drawer_close_callback
            self.drawer_close_callback = None
            if callback is not None:
                self.after_idle(callback)

    def select_drawer_view(self, view):
        if view != self.current_view:
            self.search_var.set("")
        self.switch_view(view)
        self.close_drawer()

    def select_drawer_level(self, level):
        active_level = self.flash_level_var.get() if self.current_view == "Flashcards" else self.current_level
        if level != active_level:
            self.search_var.set("")
        if self.current_view == "Flashcards":
            self.set_flash_level(level)
        elif self.current_view in {"Grammar", "Kanji"}:
            self.show_reference(self.current_view, level)
        else:
            self.show_vocabulary(level)
        self.close_drawer()

    def jump_to_kana_from_drawer(self, kana):
        self.close_drawer(after=lambda value=kana: self.jump_to_kana(value))

    def reveal_active_kana_in_drawer(self):
        if self.drawer_open and hasattr(self, "kana_rail_widget"):
            self.kana_rail_widget.reveal_active()

    def clear_vocab_selection_on_outside_click(self, event):
        if not hasattr(self, "vocab_table") or event.widget is self.vocab_table.body:
            return
        if self.widget_is_descendant(event.widget, getattr(self, "vocab_edit_actions", None)):
            return
        if self.widget_is_descendant(event.widget, getattr(self, "vocab_editor_overlay", None)):
            return
        if self.widget_is_descendant(event.widget, getattr(self, "vocab_delete_overlay", None)):
            return
        if self.widget_is_descendant(event.widget, getattr(self, "vocab_import_overlay", None)):
            return
        self.vocab_table.clear_selection()

    def widget_is_descendant(self, widget, parent):
        if widget is None or parent is None:
            return False
        while widget is not None:
            if widget is parent:
                return True
            widget = getattr(widget, "master", None)
        return False

    def update_vocab_edit_buttons(self, *_):
        if "vocab_edit_button" not in self.__dict__:
            return
        selected = self.selected_vocab_word() is not None
        state = "normal" if selected else "disabled"
        self.draw_pencil_icon(enabled=selected)
        self.vocab_edit_button.configure(
            state=state,
            image=self.pencil_icon_image,
            fg_color=BUTTON_SECONDARY_BG if selected else BUTTON_DISABLED_BG,
            hover_color=BUTTON_SECONDARY_HOVER if selected else BUTTON_DISABLED_BG,
            border_color=BUTTON_PRIMARY_BORDER if selected else theme_color(LINE),
            text_color=BUTTON_SECONDARY_TEXT if selected else BUTTON_DISABLED_TEXT,
            text_color_disabled=BUTTON_DISABLED_TEXT,
        )
        if "settings_edit_button" in self.__dict__:
            self.settings_edit_button.configure(state=state)
        if "settings_import_button" in self.__dict__:
            self.settings_import_button.configure(state="normal" if self.current_view == "Vocabulary" else "disabled")

    def selected_vocab_word(self):
        if self.current_view != "Vocabulary" or "vocab_table" not in self.__dict__:
            return None
        index = self.vocab_table.selected_index
        if index is None or index < 0 or index >= len(self.vocab_table.rows):
            return None
        return self.vocab_table.rows[index]

    def open_vocab_editor(self, mode):
        selected = self.selected_vocab_word()
        if mode == "edit" and selected is None:
            return
        self.close_vocab_editor()
        overlay = self.create_modal_overlay()
        self.vocab_editor_overlay = overlay
        title = "Add vocabulary" if mode == "add" else "Edit vocabulary"
        panel = ctk.CTkFrame(overlay, width=500, height=486, fg_color=PANEL, border_width=1, border_color=LINE, corner_radius=12)
        panel.place(relx=0.5, rely=0.5, anchor="center")
        panel.pack_propagate(False)
        ctk.CTkLabel(panel, text=title, text_color=INK, font=ctk.CTkFont(size=22, weight="bold")).pack(pady=(18, 10))

        fields_frame = ctk.CTkFrame(panel, fg_color="transparent")
        fields_frame.pack(fill="x", padx=28)
        entries = {}
        source = selected or {}
        for row, (key, label) in enumerate((("kanji", "Word"), ("furigana", "Reading"), ("romaji", "Romaji"), ("type", "Type"), ("meaning", "Meaning"))):
            ctk.CTkLabel(fields_frame, text=label, text_color=MUTED, anchor="w", font=ctk.CTkFont(size=12, weight="bold")).grid(row=row * 2, column=0, sticky="ew", pady=(0 if row == 0 else 7, 2))
            entry = ctk.CTkEntry(
                fields_frame,
                height=36,
                fg_color=PANEL,
                border_width=1,
                border_color=LINE,
                text_color=INK,
                font=ctk.CTkFont(size=14),
            )
            entry.grid(row=row * 2 + 1, column=0, sticky="ew")
            entry.insert(0, source.get(key, ""))
            entries[key] = entry
        fields_frame.grid_columnconfigure(0, weight=1)

        error_var = ctk.StringVar(value="")
        ctk.CTkLabel(panel, textvariable=error_var, text_color=("#B91C1C", "#FCA5A5"), height=20, font=ctk.CTkFont(size=12, weight="bold")).pack(pady=(8, 0))

        actions = ctk.CTkFrame(panel, fg_color="transparent")
        actions.pack(fill="x", padx=28, pady=(6, 18))
        actions.grid_columnconfigure((0, 1), weight=1)

        def close():
            self.close_vocab_editor()

        def save():
            word = {key: entries[key].get().strip() for key in ("kanji", "furigana", "romaji", "type", "meaning")}
            if not word["kanji"] or not word["furigana"] or not word["meaning"]:
                error_var.set("Word, reading, and meaning are required.")
                return
            if mode == "add":
                self.add_custom_vocab_word(self.current_level, word)
            else:
                self.edit_vocab_word(self.current_level, selected, word)
            self.close_vocab_editor()
            self.refresh_vocabulary_after_edit()

        secondary_button(actions, text="Cancel", height=38, command=close).grid(row=0, column=0, sticky="ew", padx=(0, 6))
        red_button(actions, text="Save", height=38, command=save).grid(row=0, column=1, sticky="ew", padx=(6, 0))
        overlay.bind("<Escape>", lambda _event: close())
        overlay.bind("<Return>", lambda _event: save())
        overlay.grab_set()
        entries["furigana" if mode == "add" else "kanji"].focus_set()

    def close_vocab_editor(self):
        if self.vocab_editor_overlay is not None and self.vocab_editor_overlay.winfo_exists():
            try:
                self.vocab_editor_overlay.grab_release()
            except tk.TclError:
                pass
            self.vocab_editor_overlay.destroy()
        self.vocab_editor_overlay = None

    def confirm_delete_vocab_word(self):
        selected = self.selected_vocab_word()
        if selected is None:
            return
        self.close_vocab_delete_overlay()
        overlay = self.create_modal_overlay()
        self.vocab_delete_overlay = overlay
        panel = ctk.CTkFrame(overlay, width=430, height=210, fg_color=PANEL, border_width=1, border_color=LINE, corner_radius=12)
        panel.place(relx=0.5, rely=0.5, anchor="center")
        panel.pack_propagate(False)
        ctk.CTkLabel(panel, text="Delete this word?", text_color=INK, font=ctk.CTkFont(size=22, weight="bold")).pack(pady=(22, 8))
        label = selected.get("kanji") or selected.get("furigana") or "Selected word"
        ctk.CTkLabel(panel, text=label, text_color=MUTED, font=ctk.CTkFont(size=15), wraplength=340).pack(pady=(0, 18))
        actions = ctk.CTkFrame(panel, fg_color="transparent")
        actions.pack(fill="x", padx=28, pady=(0, 22))
        actions.grid_columnconfigure((0, 1), weight=1)

        def close():
            self.close_vocab_delete_overlay()

        def delete():
            self.delete_vocab_word(self.current_level, selected)
            self.close_vocab_delete_overlay()
            self.refresh_vocabulary_after_edit()

        secondary_button(actions, text="No", height=38, command=close).grid(row=0, column=0, sticky="ew", padx=(0, 6))
        red_button(actions, text="Delete", height=38, command=delete).grid(row=0, column=1, sticky="ew", padx=(6, 0))
        overlay.bind("<Escape>", lambda _event: close())
        overlay.grab_set()
        overlay.focus_set()

    def close_vocab_delete_overlay(self):
        if self.vocab_delete_overlay is not None and self.vocab_delete_overlay.winfo_exists():
            try:
                self.vocab_delete_overlay.grab_release()
            except tk.TclError:
                pass
            self.vocab_delete_overlay.destroy()
        self.vocab_delete_overlay = None

    def create_modal_overlay(self):
        overlay = tk.Frame(self, bg=theme_color(("#1A0B10", "#090507")), bd=0, highlightthickness=0)
        overlay.place(relx=0, rely=0, relwidth=1, relheight=1)
        overlay.lift()
        return overlay

    def open_vocab_import_file(self):
        path = filedialog.askopenfilename(
            parent=self,
            title=f"Import vocabulary CSV for JLPT {self.current_level}",
            filetypes=(("CSV files", "*.csv"), ("Text files", "*.txt"), ("All files", "*.*")),
        )
        if not path:
            return
        try:
            words = load_vocabulary_csv(Path(path))
        except ValueError as error:
            self.show_app_toast(str(error), success=False)
            return
        new_words, skipped = self.preview_import_words(self.current_level, words)
        if not new_words:
            self.show_app_toast(f"JLPT {self.current_level} already has those words", success=False)
            return
        self.confirm_vocab_import(Path(path), new_words, skipped)

    def preview_import_words(self, level, words):
        existing_keys = {vocab_word_key(word) for word in self.words(level)}
        seen_keys = set()
        new_words = []
        skipped = 0
        for word in words:
            cleaned = clean_vocab_word(word)
            key = vocab_word_key(cleaned)
            if key in existing_keys or key in seen_keys:
                skipped += 1
                continue
            seen_keys.add(key)
            new_words.append(cleaned)
        return new_words, skipped

    def confirm_vocab_import(self, path, words, skipped):
        self.close_vocab_import_overlay()
        overlay = self.create_modal_overlay()
        self.vocab_import_overlay = overlay
        level = self.current_level
        panel = ctk.CTkFrame(overlay, width=500, height=226, fg_color=PANEL, border_width=1, border_color=LINE, corner_radius=12)
        panel.place(relx=0.5, rely=0.5, anchor="center")
        panel.pack_propagate(False)
        ctk.CTkLabel(panel, text=f"Import to JLPT {level}?", text_color=INK, font=ctk.CTkFont(size=22, weight="bold")).pack(pady=(22, 8))
        ctk.CTkLabel(
            panel,
            text=path.name,
            text_color=MUTED,
            font=ctk.CTkFont(size=13, weight="bold"),
            wraplength=390,
            justify="center",
        ).pack(pady=(0, 10))
        summary = f"{len(words)} new words will be added."
        if skipped:
            summary += f" {skipped} duplicate rows will be skipped."
        ctk.CTkLabel(
            panel,
            text=summary,
            text_color=MUTED,
            font=ctk.CTkFont(size=14),
            wraplength=390,
            justify="center",
        ).pack(pady=(0, 24))
        actions = ctk.CTkFrame(panel, fg_color="transparent")
        actions.pack(fill="x", padx=28, pady=(0, 22))
        actions.grid_columnconfigure((0, 1), weight=1)

        def close():
            self.close_vocab_import_overlay()

        def import_words():
            added = self.add_imported_vocab_words(level, words)
            self.close_vocab_import_overlay()
            if added:
                self.show_vocabulary(level)
                self.show_app_toast(f"Imported {added} words to JLPT {level}", success=True)
            else:
                self.show_app_toast("Import failed", success=False)

        secondary_button(actions, text="Cancel", height=38, command=close).grid(row=0, column=0, sticky="ew", padx=(0, 6))
        red_button(actions, text="Import", height=38, command=import_words).grid(row=0, column=1, sticky="ew", padx=(6, 0))
        overlay.bind("<Escape>", lambda _event: close())
        overlay.bind("<Return>", lambda _event: import_words())
        overlay.grab_set()
        overlay.focus_set()

    def close_vocab_import_overlay(self):
        if self.vocab_import_overlay is not None and self.vocab_import_overlay.winfo_exists():
            try:
                self.vocab_import_overlay.grab_release()
            except tk.TclError:
                pass
            self.vocab_import_overlay.destroy()
        self.vocab_import_overlay = None

    def add_imported_vocab_words(self, level, words):
        level_data = self.custom_level_data(level)
        start_index = len(level_data["custom"])
        for word in words:
            level_data["custom"].append({**word, "id": self.new_custom_word_id()})
        if not self.write_custom_vocabulary():
            del level_data["custom"][start_index:]
            return 0
        self.refresh_vocabulary_after_edit()
        return len(words)

    def show_app_toast(self, message, success=True):
        if "app_toast" not in self.__dict__:
            return
        self.cancel_app_toast_animation()
        self.app_toast_var.set(message)
        self.app_toast_success = success
        self.app_toast_started_at = time.perf_counter()
        self.app_toast.place(relx=0.5, y=18, anchor="n")
        self.app_toast.lift()
        self.animate_app_toast()

    def cancel_app_toast_animation(self):
        after_id = self.__dict__.get("app_toast_animation_after_id")
        if after_id:
            try:
                self.after_cancel(after_id)
            except tk.TclError:
                pass
        self.app_toast_animation_after_id = None

    def animate_app_toast(self):
        if "app_toast" not in self.__dict__:
            return
        fade_in = 0.18
        hold = 1.15
        fade_out = 0.52
        total = fade_in + hold + fade_out
        elapsed = time.perf_counter() - self.__dict__.get("app_toast_started_at", time.perf_counter())
        fade_in_progress = 1.0
        fade_out_progress = 0.0
        if elapsed < fade_in:
            fade_in_progress = elapsed / fade_in
            alpha = ease_out_cubic(fade_in_progress)
        elif elapsed < fade_in + hold:
            alpha = 1.0
        elif elapsed < total:
            fade_out_progress = (elapsed - fade_in - hold) / fade_out
            alpha = 1.0 - ease_out_cubic(fade_out_progress)
            if alpha <= 0.16:
                self.app_toast.place_forget()
                self.app_toast_animation_after_id = None
                return
        else:
            self.app_toast.place_forget()
            self.app_toast_animation_after_id = None
            return

        base_color = theme_color(PAPER)
        if self.__dict__.get("app_toast_success", True):
            target_bg = theme_color(("#ECFDF3", "#1E3328"))
            target_border = theme_color(("#86EFAC", "#4ADE80"))
            target_text = theme_color(("#166534", "#BBF7D0"))
        else:
            target_bg = theme_color(("#FFF1F2", "#3B1821"))
            target_border = theme_color(("#FDA4AF", "#FB7185"))
            target_text = theme_color(("#9F1239", "#FFE4E6"))
        self.app_toast.configure(
            fg_color=blend_hex(base_color, target_bg, alpha),
            border_color=blend_hex(base_color, target_border, alpha),
        )
        self.app_toast_label.configure(text_color=blend_hex(base_color, target_text, alpha))
        y_offset = round((1 - ease_out_cubic(fade_in_progress)) * 8) if elapsed < fade_in else round(ease_out_cubic(fade_out_progress) * 3)
        self.app_toast.place_configure(y=18 + y_offset)
        self.app_toast_animation_after_id = self.after(16, self.animate_app_toast)

    def refresh_vocabulary_after_edit(self):
        self.words_cache.pop(self.current_level, None)
        self.last_vocab_render_key = None
        self.render_vocab_rows()
        self.update_flash_rail_buttons(force=True)
        if self.current_view == "Flashcards":
            self.reset_flashcards()

    def add_custom_vocab_word(self, level, word):
        level_data = self.custom_level_data(level)
        custom_word = {**word, "id": self.new_custom_word_id()}
        level_data["custom"].append(custom_word)
        self.write_custom_vocabulary()

    def edit_vocab_word(self, level, original, word):
        level_data = self.custom_level_data(level)
        if original.get("_source") == "custom":
            target_id = original.get("_id")
            for item in level_data["custom"]:
                if item.get("id") == target_id:
                    item.update(word)
                    break
        else:
            original_key = original.get("_key") or vocab_word_key(original)
            if original_key not in level_data["removed"]:
                level_data["removed"].append(original_key)
            level_data["custom"].append({**word, "id": self.new_custom_word_id()})
        self.write_custom_vocabulary()

    def delete_vocab_word(self, level, word):
        level_data = self.custom_level_data(level)
        if word.get("_source") == "custom":
            target_id = word.get("_id")
            level_data["custom"] = [item for item in level_data["custom"] if item.get("id") != target_id]
        else:
            original_key = word.get("_key") or vocab_word_key(word)
            if original_key not in level_data["removed"]:
                level_data["removed"].append(original_key)
        self.write_custom_vocabulary()

    def resize_vocab_sheet(self, event):
        visible_width = max(360, event.width)
        for measured_width in (self.vocab_view.winfo_width(), self.content.winfo_width()):
            if measured_width > 100:
                visible_width = min(visible_width, measured_width)
        available = max(360, visible_width - 12)
        width = min(1480, available)
        self.vocab_sheet.configure(width=max(360, round(width / self.widget_scale())))
        if hasattr(self, "vocab_table"):
            self.reading_scale = self.reading_scale_for_size(width, event.height)
            self.vocab_table.set_scale(self.reading_scale)
            self.draw_search_icon()

    def schedule_root_layout_refresh(self, _event=None):
        if self.root_configure_after_id is not None:
            return
        self.root_configure_after_id = self.after(80, self.refresh_root_layout)

    def refresh_root_layout(self):
        self.root_configure_after_id = None
        self.display_scale = detect_display_scale(self)
        self.apply_responsive_layout()
        if hasattr(self, "kana_rail_widget"):
            self.kana_rail_widget.set_scale(self.kana_rail_scale())
        self.sync_current_view_layout()

    def apply_responsive_layout(self):
        if not hasattr(self, "header"):
            return
        logical_width = self.winfo_width() / max(1.0, getattr(self, "display_scale", 1.0))
        compact = logical_width < 820
        if compact == self.responsive_compact:
            return
        self.responsive_compact = compact

        if compact:
            self.shell.grid_configure(padx=16, pady=(16, 18))
            self.header.grid_configure(pady=(0, 12))
            self.menu_button.configure(width=44, height=42)
            self.menu_button.grid_configure(padx=(14, 10), pady=15)
            self.brand_font.configure(size=22)
            self.title_font.configure(size=26)
            self.settings_button.configure(width=82, height=40, text="Settings")
            self.settings_button.grid_configure(padx=14, pady=16)
            self.vocab_import_button.configure(width=108, height=40)
            self.vocab_import_button.grid_configure(row=2, column=0, rowspan=1, sticky="e", pady=(10, 0))
            self.flash_header_actions.grid_configure(row=2, column=0, rowspan=1, sticky="e", pady=(10, 0))
            self.search_entry.configure(placeholder_text=self.search_placeholder(compact=True))
        else:
            self.shell.grid_configure(padx=28, pady=(24, 26))
            self.header.grid_configure(pady=(0, 16))
            self.menu_button.configure(width=48, height=46)
            self.menu_button.grid_configure(padx=(22, 14), pady=13)
            self.brand_font.configure(size=25)
            self.title_font.configure(size=31)
            self.settings_button.configure(width=96, height=42, text="Settings")
            self.settings_button.grid_configure(padx=22, pady=15)
            self.vocab_import_button.configure(width=122, height=44)
            self.vocab_import_button.grid_configure(row=0, column=1, rowspan=2, sticky="e", pady=0)
            self.flash_header_actions.grid_configure(row=0, column=1, rowspan=2, sticky="e", pady=0)
            self.search_entry.configure(placeholder_text=self.search_placeholder(compact=False))

        if self.current_view == "Flashcards":
            self.show_flash_header_actions()
        else:
            self.show_vocab_header_status()

    def search_placeholder(self, compact=False):
        if self.current_view == "Grammar":
            return "Search grammar..." if compact else "Search grammar, romaji, or meaning..."
        if self.current_view == "Kanji":
            return "Search kanji..." if compact else "Search kanji, readings, or meaning..."
        return "Search vocabulary..." if compact else "Search word, reading, romaji, type, or meaning..."

    def kana_rail_scale(self):
        return max(1.0, min(1.35, getattr(self, "display_scale", 1.0)))

    def widget_scale(self):
        try:
            return max(1.0, ctk.ScalingTracker.get_widget_scaling(self))
        except Exception:
            return 1.0

    def reading_scale_for_size(self, width, height):
        if width < 1150 or height < 760:
            scale = 1.08
        elif width < 1350:
            scale = 1.12
        elif width < 1500:
            scale = 1.16
        else:
            scale = 1.2
        if self.widget_scale() >= 1.25:
            scale = max(scale, 1.23)
        return scale

    def draw_search_icon(self):
        if not hasattr(self, "search_icon"):
            return
        icon_size = round(22 * min(1.25, getattr(self, "reading_scale", 1.0)))
        self.search_icon_image = ctk.CTkImage(
            light_image=create_search_icon_image("#765E64"),
            dark_image=create_search_icon_image("#D4B7BA"),
            size=(icon_size, icon_size),
        )
        self.search_icon.configure(image=self.search_icon_image)

    def draw_pencil_icon(self, enabled=True):
        icon_color = theme_color(BUTTON_SECONDARY_TEXT if enabled else BUTTON_DISABLED_TEXT)
        self.pencil_icon_image = ctk.CTkImage(
            light_image=create_pencil_icon_image(icon_color),
            dark_image=create_pencil_icon_image(icon_color),
            size=(21, 21),
        )

    def draw_import_icon(self):
        if not hasattr(self, "vocab_import_button"):
            return
        icon_color = theme_color(BUTTON_SECONDARY_TEXT)
        self.import_icon_image = ctk.CTkImage(
            light_image=create_import_icon_image(icon_color),
            dark_image=create_import_icon_image(icon_color),
            size=(19, 19),
        )
        self.vocab_import_button.configure(image=self.import_icon_image)

    def _build_flashcards(self):
        self.flash_card = ctk.CTkFrame(self.flash_view, fg_color=PANEL, border_width=1, border_color=LINE, corner_radius=18)
        self.flash_card.grid(row=0, column=0, sticky="nsew")
        self.flash_card.grid_columnconfigure(0, weight=1)
        self.flash_card.grid_rowconfigure(0, weight=1)
        self.flash_inner = ctk.CTkFrame(self.flash_card, fg_color="transparent")
        self.flash_inner.grid(row=0, column=0)
        self.confetti_canvas = tk.Canvas(
            self.flash_card,
            bg=theme_color(PANEL),
            bd=0,
            highlightthickness=0,
            takefocus=0,
        )
        self.confetti_canvas.place(relx=0, rely=0, relwidth=1, relheight=1)
        self.lower_completion_canvas()
        self.save_toast = ctk.CTkFrame(
            self.flash_card,
            fg_color=PANEL,
            border_width=1,
            border_color=LINE,
            corner_radius=9,
        )
        self.save_toast_label = ctk.CTkLabel(
            self.save_toast,
            textvariable=self.save_toast_var,
            text_color=INK,
            font=ctk.CTkFont(size=13, weight="bold"),
        )
        self.save_toast_label.pack(padx=18, pady=9)
        self.save_toast.place_forget()
        self.set_flash_save_status(self.flash_save_status_var.get())
        self.card_front_font = ctk.CTkFont(size=46, weight="bold")
        self.card_back_font = ctk.CTkFont(size=22)
        self.card_hint_font = ctk.CTkFont(size=13, weight="bold")
        self.card_front_label = ctk.CTkLabel(
            self.flash_inner,
            textvariable=self.card_front_var,
            font=self.card_front_font,
            text_color=INK,
            wraplength=720,
            justify="center",
        )
        self.card_front_label.pack(padx=28, pady=(20, 16))
        self.card_back_label = ctk.CTkLabel(
            self.flash_inner,
            textvariable=self.card_back_var,
            font=self.card_back_font,
            text_color=MUTED,
            wraplength=720,
            justify="center",
        )
        self.card_back_label.pack(padx=28, pady=(0, 12))
        self.card_hint_label = ctk.CTkLabel(
            self.flash_inner,
            textvariable=self.card_hint_var,
            font=self.card_hint_font,
            text_color=MUTED,
            wraplength=720,
            justify="center",
        )
        self.card_hint_label.pack(padx=28, pady=(0, 20))
        self.completion_actions = ctk.CTkFrame(self.flash_inner, fg_color="transparent")
        self.completion_actions.grid_columnconfigure((0, 1), weight=1)
        red_button(
            self.completion_actions,
            text="Yes, Restart",
            height=38,
            command=self.restart_flashcards_to_ready,
        ).grid(row=0, column=0, sticky="ew", padx=(0, 6))
        secondary_button(
            self.completion_actions,
            text="Not Now",
            height=38,
            command=self.keep_completed_flashcards,
        ).grid(row=0, column=1, sticky="ew", padx=(6, 0))
        self.bind_flash_card_clicks()

        actions = ctk.CTkFrame(self.flash_view, fg_color="transparent")
        actions.grid(row=1, column=0, sticky="ew", pady=(18, 0))
        actions.grid_columnconfigure((0, 1, 2), weight=1)
        self.back_button = secondary_button(actions, text="Back", height=46, command=self.previous_card)
        self.back_button.grid(row=0, column=0, sticky="ew", padx=(0, 6))
        self.reveal_button = red_button(
            actions,
            text="Start",
            height=46,
            font=ctk.CTkFont(size=14, weight="bold"),
            command=self.handle_flash_card_click,
        )
        self.reveal_button.grid(row=0, column=1, sticky="ew", padx=6)
        self.restart_button = secondary_button(
            actions,
            text="Restart",
            height=46,
            command=self.confirm_restart_flashcards,
        )
        self.restart_button.grid(row=0, column=2, sticky="ew", padx=(6, 0))
        self.restart_button.bind("<Enter>", self.prepare_restart_overlay_cache)

    def _configure_tree_style(self):
        dark = ctk.get_appearance_mode() == "Dark"
        bg = "#211720" if dark else "#FFFDF8"
        fg = "#FFF3E8" if dark else "#24191D"
        muted_bg = "#421923" if dark else "#F8E8DD"
        selected = "#A11D26" if dark else "#E8CEC2"
        style = ttk.Style(self)
        style.theme_use("clam")
        style.configure(
            "JLPT.Treeview",
            background=bg,
            fieldbackground=bg,
            foreground=fg,
            borderwidth=0,
            rowheight=34,
            font=("Segoe UI", 11),
        )
        style.layout("JLPT.Treeview", [("Treeview.treearea", {"sticky": "nswe"})])
        style.configure(
            "JLPT.Treeview.Heading",
            background=muted_bg,
            foreground=fg,
            relief="flat",
            borderwidth=0,
            font=("Segoe UI", 10, "bold"),
            padding=(12, 8),
        )
        style.map(
            "JLPT.Treeview",
            background=[("selected", selected)],
            foreground=[("selected", fg)],
        )
        style.map(
            "JLPT.Treeview.Heading",
            background=[("active", muted_bg), ("pressed", muted_bg), ("!active", muted_bg)],
            foreground=[("active", fg), ("pressed", fg), ("!active", fg)],
            relief=[("active", "flat"), ("pressed", "flat")],
        )
        if hasattr(self, "vocab_tree"):
            self.configure_tree_tags()

    def configure_tree_tags(self):
        dark = ctk.get_appearance_mode() == "Dark"
        self.vocab_tree.tag_configure("hover", background=TABLE_ROW_HOVER[1] if dark else TABLE_ROW_HOVER[0])

    def on_tree_motion(self, event):
        item_id = self.vocab_tree.identify_row(event.y)
        if item_id == self.hovered_tree_item:
            return
        self.clear_tree_hover()
        if item_id:
            self.vocab_tree.item(item_id, tags=("hover",))
            self.hovered_tree_item = item_id

    def on_tree_leave(self, _event):
        self.clear_tree_hover()

    def clear_tree_hover(self):
        if self.hovered_tree_item and self.vocab_tree.exists(self.hovered_tree_item):
            self.vocab_tree.item(self.hovered_tree_item, tags=())
        self.hovered_tree_item = None

    def bind_keyboard_shortcuts(self):
        self.bind_all("<space>", self.handle_space_shortcut)
        self.bind_all("<Left>", self.handle_left_shortcut)
        self.bind_all("<Right>", self.handle_right_shortcut)
        self.bind_all("<Up>", self.handle_up_shortcut)
        self.bind_all("<Down>", self.handle_down_shortcut)

    def focused_text_input(self, event):
        widget = event.widget
        return isinstance(widget, (tk.Entry, tk.Text, ttk.Entry)) or widget.winfo_class() in {"Entry", "TEntry"}

    def restart_dialog_is_open(self):
        return self.restart_dialog is not None and self.restart_dialog.winfo_exists()

    def handle_space_shortcut(self, event):
        if self.restart_dialog_is_open():
            return "break"
        if self.current_view != "Flashcards":
            return None
        self.handle_flash_card_click()
        return "break"

    def handle_left_shortcut(self, event):
        if self.restart_dialog_is_open():
            return "break"
        if self.current_view != "Flashcards":
            return None
        self.previous_card()
        return "break"

    def handle_right_shortcut(self, event):
        if self.restart_dialog_is_open():
            return "break"
        if self.current_view != "Flashcards":
            return None
        if self.flash_deck:
            self.next_card()
        else:
            self.start_flashcards()
        return "break"

    def handle_up_shortcut(self, event):
        if self.restart_dialog_is_open():
            return "break"
        if self.current_view != "Vocabulary":
            return None
        self.vocab_table.smooth_scroll_by(-1)
        return "break"

    def handle_down_shortcut(self, event):
        if self.restart_dialog_is_open():
            return "break"
        if self.current_view != "Vocabulary":
            return None
        self.vocab_table.smooth_scroll_by(1)
        return "break"

    def toggle_theme(self):
        next_mode = "Light" if ctk.get_appearance_mode() == "Dark" else "Dark"
        ctk.set_appearance_mode(next_mode)
        if hasattr(self, "settings_theme_button"):
            self.settings_theme_button.configure(text=self.theme_action_text())
        self.draw_search_icon()
        self.draw_import_icon()
        self.update_vocab_edit_buttons()
        self._configure_tree_style()
        if hasattr(self, "kana_rail_widget"):
            self.kana_rail_widget.refresh_theme()
        self.update_flash_rail_buttons(force=True)
        self.apply_flashcard_style()
        self.set_flash_save_status(self.flash_save_status_var.get())
        if hasattr(self, "vocab_table"):
            self.vocab_table.refresh_theme()

    def switch_view(self, view):
        if view == self.current_view:
            self.sync_current_view_layout()
            return
        paint_locked = self.lock_window_paint()
        try:
            self.current_view = view
            self.set_nav_buttons(view)
            if view == "Vocabulary":
                if self.current_level == "N1":
                    self.current_level = "N2"
                self.show_vocabulary(self.current_level)
            elif view in {"Grammar", "Kanji"}:
                self.show_reference(view, self.current_level)
            else:
                if self.flash_level_var.get() == "N1":
                    self.flash_level_var.set("N2")
                self.prepare_vocabulary_for_view_switch()
                self.show_flash_header_actions()
                level = self.flash_level_var.get()
                self.title_label.configure(text=f"JLPT {level} Flashcards")
                self.subtitle_label.configure(text="Tap the card to reveal and continue.")
                self.count_var.set("")
                if self.flashcards_ready:
                    self.update_flash_rail_buttons(force=True)
                else:
                    self.reset_flashcards()
                self.flash_view.tkraise()
            if paint_locked:
                self.update_idletasks()
        finally:
            self.unlock_window_paint(paint_locked)

    def lock_window_paint(self):
        if sys.platform != "win32":
            return False
        try:
            return bool(ctypes.windll.user32.LockWindowUpdate(ctypes.c_void_p(self.winfo_id())))
        except Exception:
            return False

    def unlock_window_paint(self, locked):
        if not locked:
            return
        try:
            ctypes.windll.user32.LockWindowUpdate(ctypes.c_void_p(0))
        except Exception:
            pass

    def show_vocabulary(self, level):
        if level == "N1":
            level = "N2"
        self.current_level = level
        self.current_view = "Vocabulary"
        if hasattr(self, "vocab_table"):
            self.vocab_table.set_rendering_enabled(True)
        self.show_vocab_header_status()
        self.set_nav_buttons("Vocabulary")
        self.set_level_buttons(level)
        self.title_label.configure(text=f"JLPT {level} Vocabulary")
        self.subtitle_label.configure(text="Search or browse the complete list.")
        self.search_entry.configure(textvariable=self.search_var, placeholder_text=self.search_placeholder(self.responsive_compact is True))
        self.last_vocab_render_key = None
        self.render_vocab_rows()
        self.vocab_view.tkraise()
        self.show_vocab_header_status()

    def show_reference(self, section, level):
        self.current_level = level
        self.current_view = section
        if hasattr(self, "vocab_table"):
            self.vocab_table.set_rendering_enabled(True)
        self.show_vocab_header_status()
        self.set_nav_buttons(section)
        self.set_level_buttons(level)
        self.title_label.configure(text=f"JLPT {level} {section}")
        self.subtitle_label.configure(text=f"Search or browse the complete {section.lower()} list.")
        self.search_entry.configure(textvariable=self.search_var, placeholder_text=self.search_placeholder(self.responsive_compact is True))
        self.last_vocab_render_key = None
        self.render_vocab_rows()
        self.vocab_view.tkraise()

    def prepare_vocabulary_for_view_switch(self):
        if hasattr(self, "vocab_table"):
            self.vocab_table.cancel_scroll_animation()
            self.vocab_table.cancel_selection_animation()
            self.vocab_table.set_rendering_enabled(False)
        if hasattr(self, "kana_rail_widget"):
            self.kana_rail_widget.cancel_animation()

    def show_flash_header_actions(self):
        if self.current_view != "Flashcards":
            self.show_vocab_header_status()
            return
        if "count_label" in self.__dict__:
            self.count_label.grid_remove()
        if "vocab_import_button" in self.__dict__:
            self.vocab_import_button.grid_remove()
        if "flash_header_actions" in self.__dict__:
            self.flash_header_actions.grid()

    def show_vocab_header_status(self):
        if "flash_header_actions" in self.__dict__:
            self.flash_header_actions.grid_remove()
        if "count_label" in self.__dict__:
            self.count_label.grid()
        if "vocab_import_button" in self.__dict__:
            self.vocab_import_button.grid_remove()

    def sync_current_view_layout(self):
        if not hasattr(self, "vocab_view"):
            return
        if self.current_view == "Flashcards":
            self.show_flash_header_actions()
            self.flash_view.tkraise()
        else:
            if hasattr(self, "vocab_table"):
                self.vocab_table.set_rendering_enabled(True)
            self.show_vocab_header_status()
            self.vocab_view.tkraise()

    def set_nav_buttons(self, active, force=False):
        if not hasattr(self, "nav_buttons"):
            return
        if not force and self.active_nav_button == active:
            return
        for name, button in self.nav_buttons.items():
            apply_button_style(button, "primary" if name == active else "secondary")
        self.active_nav_button = active

    def set_level_buttons(self, active, force=False):
        if not hasattr(self, "level_buttons"):
            return
        if not force and self.active_level_button == active:
            return
        for item_level, button in self.level_buttons.items():
            apply_button_style(button, "primary" if item_level == active else "secondary")
        self.active_level_button = active

    def schedule_vocab_render(self):
        if self.search_after_id is not None:
            self.after_cancel(self.search_after_id)
        self.search_after_id = self.after(120, self.render_vocab_rows)

    def render_vocab_rows(self):
        self.search_after_id = None
        if self.current_view == "Vocabulary":
            words = self.words(self.current_level)
            searchable_keys = ("kanji", "furigana", "romaji", "type", "meaning")
            unit = "words"
        else:
            words = [dict(item) for item in self.data[self.current_view.lower()].get(self.current_level, [])]
            searchable_keys = tuple(key for key in words[0] if key != "source_url") if words else ()
            unit = "grammar points" if self.current_view == "Grammar" else "kanji"
        query = self.search_var.get().strip().lower()
        render_key = (self.current_view, self.current_level, query)
        if render_key == self.last_vocab_render_key:
            self.update_kana_buttons()
            return
        self.last_vocab_render_key = render_key
        if query:
            words = [word for word in words if query in " ".join(str(word.get(key, "")) for key in searchable_keys).lower()]

        self.count_var.set(f"{len(words)} {unit}" if words else "0 matches")
        self.tree_items_by_kana = {}

        if self.current_view == "Vocabulary":
            for row_index, word in enumerate(words):
                kana = normalized_first_kana(word["furigana"])
                self.tree_items_by_kana.setdefault(kana, row_index)

        self.vocab_table.configure_mode(self.current_view)
        self.vocab_table.set_rows(words)
        self.update_kana_buttons()
        self.update_vocab_edit_buttons()

    def kana_group_marker(self, kana):
        return "▴" if kana in self.expanded_kana_groups else "▾"

    def toggle_kana_group(self, kana):
        if kana in self.expanded_kana_groups:
            self.expanded_kana_groups.remove(kana)
        else:
            self.expanded_kana_groups.add(kana)
        self.update_kana_buttons(animated=True)

    def open_kana_group(self, kana, close_others=False, animated=True):
        group_kana = KANA_GROUP_BY_MEMBER.get(kana)
        if not group_kana:
            return False
        changed = False
        if close_others:
            for expanded_kana in list(self.expanded_kana_groups):
                if expanded_kana != group_kana:
                    self.expanded_kana_groups.remove(expanded_kana)
                    changed = True
        if group_kana not in self.expanded_kana_groups:
            self.expanded_kana_groups.add(group_kana)
            changed = True
        if changed:
            self.update_kana_buttons(animated=animated)
        return changed

    def sync_kana_from_table(self, kana):
        if self.current_view != "Vocabulary" or not kana:
            return
        changed = self.open_kana_group(kana, close_others=True, animated=True)
        if kana == self.active_kana:
            return
        self.active_kana = kana
        self.update_kana_buttons(animated=changed)

    def update_kana_buttons(self, animated=False):
        if self.current_view != "Vocabulary":
            self.kana_rail_widget.set_state(set(), None, set(), animated=False)
            return
        available = set(self.tree_items_by_kana)
        self.kana_rail_widget.set_state(available, self.active_kana, self.expanded_kana_groups, animated=animated)

    def jump_to_kana(self, kana):
        row_index = self.tree_items_by_kana.get(kana)
        target_kana = kana
        if row_index is None and kana in KANA_GROUP_MEMBERS:
            for member in KANA_GROUP_MEMBERS[kana]:
                row_index = self.tree_items_by_kana.get(member)
                if row_index is not None:
                    target_kana = member
                    break
        if row_index is None:
            return
        self.open_kana_group(target_kana, close_others=True, animated=True)
        self.vocab_table.see(row_index, animated=True)
        self.vocab_table.select_index(None)
        self.active_kana = target_kana
        self.update_kana_buttons(animated=True)

    def reset_flashcards(self, restore_saved=True, clear_saved=False):
        self.flashcards_ready = True
        level = self.flash_level_var.get()
        self.update_flash_rail_buttons()
        self.stop_completion_animation()
        self.stop_confetti()
        self.hide_completion_prompt()
        if clear_saved:
            self.clear_saved_flashcards(level)
        self.set_flash_save_status("Not saved")
        if restore_saved and self.restore_flashcards(level):
            return

        self.flash_deck = []
        self.flash_index = -1
        self.card_answer_visible = False
        self.flash_complete = False
        self.count_var.set("")
        self.set_flash_save_status("Not saved")
        if not self.words(level):
            self.card_front_var.set(f"JLPT {level}")
            self.card_back_var.set("Coming soon")
            self.card_hint_var.set("")
            self.apply_flashcard_style()
            return
        self.card_front_var.set(f"JLPT {level}")
        self.card_back_var.set(f"{len(self.words(level))} cards")
        self.card_hint_var.set("Tap the center or Start to begin.")
        self.apply_flashcard_style()

    def clear_saved_flashcards(self, level):
        levels = self.flash_progress.get("levels")
        if not isinstance(levels, dict) or level not in levels:
            return
        del levels[level]
        self.write_flash_progress()

    def restart_flashcards_to_ready(self):
        self.reset_flashcards(restore_saved=False, clear_saved=True)
        self.after(30, lambda: self.show_save_toast("Deck restarted", success=True))

    def confirm_restart_flashcards(self):
        if self.restart_dialog is not None and self.restart_dialog.winfo_exists():
            self.restart_dialog.lift()
            self.restart_dialog.focus_set()
            return

        self.update_idletasks()
        cache_key = self.restart_overlay_key()
        self.restart_dialog_image = self.restart_overlay_cache if self.restart_overlay_cache_key == cache_key else None
        overlay = tk.Frame(self, bg=theme_color(PAPER), bd=0, highlightthickness=0)
        self.restart_dialog = overlay
        overlay.place(relx=0, rely=0, relwidth=1, relheight=1)
        overlay.lift()

        canvas = tk.Canvas(overlay, bd=0, highlightthickness=0)
        canvas.pack(fill="both", expand=True)
        if self.restart_dialog_image:
            canvas.create_image(0, 0, image=self.restart_dialog_image, anchor="nw")
        else:
            canvas.create_rectangle(
                0,
                0,
                max(1, self.winfo_width()),
                max(1, self.winfo_height()),
                fill=theme_color(("#2A1117", "#0C070B")),
                outline="",
            )

        panel = ctk.CTkFrame(overlay, width=410, height=178, fg_color=PANEL, border_width=1, border_color=LINE, corner_radius=12)
        panel.place(relx=0.5, rely=0.5, anchor="center")
        panel.pack_propagate(False)
        ctk.CTkLabel(
            panel,
            text="Restart this deck?",
            text_color=INK,
            font=ctk.CTkFont(size=21, weight="bold"),
        ).pack(pady=(20, 6))
        ctk.CTkLabel(
            panel,
            text=f"This will return JLPT {self.flash_level_var.get()} to the Ready screen.",
            text_color=MUTED,
            font=ctk.CTkFont(size=13),
            wraplength=320,
            justify="center",
        ).pack(pady=(0, 18))

        actions = ctk.CTkFrame(panel, fg_color="transparent")
        actions.pack(fill="x", padx=28, pady=(0, 20))
        actions.grid_columnconfigure((0, 1), weight=1)

        def close_dialog():
            if overlay.winfo_exists():
                try:
                    overlay.grab_release()
                except tk.TclError:
                    pass
                overlay.destroy()
            self.restart_dialog = None
            self.restart_dialog_image = None

        def proceed_restart():
            close_dialog()
            self.restart_flashcards_to_ready()

        secondary_button(actions, text="No", height=38, command=close_dialog).grid(row=0, column=0, sticky="ew", padx=(0, 6))
        red_button(actions, text="Yes, Restart", height=38, command=proceed_restart).grid(row=0, column=1, sticky="ew", padx=(6, 0))

        overlay.bind("<Escape>", lambda _event: close_dialog())
        overlay.bind("<Return>", lambda _event: proceed_restart())
        canvas.bind("<Button-1>", lambda _event: "break")
        panel.lift()
        overlay.grab_set()
        overlay.focus_set()

    def restart_overlay_key(self):
        return (ctk.get_appearance_mode(), self.winfo_width(), self.winfo_height())

    def prepare_restart_overlay_cache(self, _event=None):
        if self.restart_dialog_is_open():
            return
        cache_key = self.restart_overlay_key()
        if self.restart_overlay_cache_key == cache_key and self.restart_overlay_cache is not None:
            return
        self.update_idletasks()
        self.restart_overlay_cache = self.create_restart_overlay_image()
        self.restart_overlay_cache_key = cache_key if self.restart_overlay_cache else None

    def create_restart_overlay_image(self):
        try:
            x = self.winfo_rootx()
            y = self.winfo_rooty()
            width = max(1, self.winfo_width())
            height = max(1, self.winfo_height())
            image = ImageGrab.grab(bbox=(x, y, x + width, y + height))
            small_width = max(160, width // 5)
            small_height = max(100, height // 5)
            image = image.convert("RGB")
            blurred = image.resize((small_width, small_height), Image.Resampling.BILINEAR)
            blurred = blurred.filter(ImageFilter.GaussianBlur(radius=2.4))
            image = blurred.resize(image.size, Image.Resampling.BILINEAR)
            image = ImageEnhance.Brightness(image).enhance(0.48)
            tint = Image.new("RGB", image.size, theme_color(("#2A1117", "#0C070B")))
            image = Image.blend(image, tint, 0.32)
            return ImageTk.PhotoImage(image)
        except Exception:
            return None

    def set_flash_level(self, level):
        if level == "N1":
            return
        self.flash_level_var.set(level)
        self.set_level_buttons(level, force=True)
        if self.current_view == "Flashcards" and hasattr(self, "title_label"):
            self.title_label.configure(text=f"JLPT {level} Flashcards")
        self.reset_flashcards()

    def update_flash_rail_buttons(self, force=False):
        if not hasattr(self, "flash_rail_buttons"):
            return
        level = self.flash_level_var.get()
        if not force and self.flash_rail_selected_level == level:
            return
        if self.current_view == "Flashcards":
            self.set_level_buttons(level, force=True)
        self.flash_rail_selected_level = level

    def bind_flash_card_clicks(self):
        for widget in (self.flash_card, self.flash_inner, self.card_front_label, self.card_back_label, self.card_hint_label):
            widget.bind("<Button-1>", self.handle_flash_card_click)
        self.confetti_canvas.bind("<Button-1>", self.handle_completion_canvas_click)
        self.confetti_canvas.bind("<Motion>", self.handle_completion_canvas_motion)
        self.confetti_canvas.bind("<Leave>", self.handle_completion_canvas_leave)
        self.confetti_canvas.bind("<Configure>", self.handle_completion_canvas_configure)

    def handle_flash_card_click(self, event=None):
        if event is not None and "flash_card" in self.__dict__:
            card_width = max(1, self.flash_card.winfo_width())
            pointer_x = event.x_root - self.flash_card.winfo_rootx()
            if pointer_x < card_width * 0.34:
                if self.flash_deck:
                    self.previous_card()
                return
            if pointer_x > card_width * 0.66:
                if self.flash_deck:
                    self.next_card()
                else:
                    self.start_flashcards()
                return
        if not self.flash_deck:
            self.start_flashcards()
            return
        if self.flash_complete:
            return
        if self.card_answer_visible:
            self.next_card()
        else:
            self.show_answer()

    def apply_flashcard_style(self):
        if "flash_card" not in self.__dict__:
            return
        self.update_flashcard_action_buttons()
        if self.flash_complete:
            self.show_completion_reveal_frame(1.0)
            return
        self.clear_completion_canvas()
        self.completion_style_key = None
        self.completion_panel_color = None
        self.flash_card.configure(fg_color=PANEL, border_color=LINE)
        self.flash_card.configure(border_width=1)
        self.flash_inner.grid_configure(pady=0)
        self.card_front_font.configure(size=46)
        self.card_back_font.configure(size=22)
        self.card_hint_font.configure(size=13)
        self.update_confetti_canvas_bg(theme_color(PANEL))
        self.card_front_label.configure(text_color=INK)
        self.card_back_label.configure(text_color=MUTED)
        self.card_hint_label.configure(text_color=MUTED)

    def start_flashcards(self):
        self.stop_completion_animation()
        self.stop_confetti()
        self.hide_completion_prompt()
        self.clear_completion_canvas()
        level = self.flash_level_var.get()
        self.flash_deck = self.words(level).copy()
        if not self.flash_deck:
            self.reset_flashcards()
            return
        random.shuffle(self.flash_deck)
        self.flash_index = 0
        self.card_answer_visible = False
        self.flash_complete = False
        self.count_var.set("")
        self.show_current_card()

    def show_current_card(self, reveal_answer=False):
        if not self.flash_deck or self.flash_index < 0:
            return
        self.stop_completion_animation()
        self.stop_confetti()
        self.hide_completion_prompt()
        self.clear_completion_canvas()
        self.flash_complete = False
        word = self.flash_deck[self.flash_index]
        self.card_front_var.set(word["kanji"] or word["furigana"])
        self.card_answer_visible = reveal_answer
        if reveal_answer:
            self.card_back_var.set(f"{word['furigana']}  |  {word['romaji']}  |  {word['meaning']}")
        else:
            self.card_back_var.set(f"Card {self.flash_index + 1} / {len(self.flash_deck)}")
        self.card_hint_var.set("")
        self.mark_flashcards_not_saved()
        self.apply_flashcard_style()

    def previous_card(self):
        if not self.flash_deck:
            self.reset_flashcards()
            return
        if self.flash_complete:
            self.flash_index = len(self.flash_deck) - 1
            self.card_answer_visible = False
            self.show_current_card()
            return
        self.flash_index = max(0, self.flash_index - 1)
        self.card_answer_visible = False
        self.show_current_card()

    def next_card(self):
        if not self.flash_deck:
            self.reset_flashcards()
            return
        if self.flash_complete:
            return
        if self.flash_index >= len(self.flash_deck) - 1:
            self.show_flashcards_complete()
            return
        self.flash_index = (self.flash_index + 1) % len(self.flash_deck)
        self.card_answer_visible = False
        self.show_current_card()

    def show_answer(self):
        if not self.flash_deck:
            return
        if self.flash_complete:
            return
        self.show_current_card(reveal_answer=True)

    def show_flashcards_complete(self, animate=True):
        if not self.flash_deck:
            return
        self.flash_complete = True
        self.card_answer_visible = True
        total = len(self.flash_deck)
        self.card_front_var.set("Deck Completed!")
        self.card_back_var.set(f"You finished all {total} flashcards.")
        self.card_hint_var.set("Restart this deck?")
        self.count_var.set("Finished")
        self.completion_show_buttons = True
        self.mark_flashcards_not_saved()
        self.update_flashcard_action_buttons()
        if animate and "flash_card" in self.__dict__:
            self.start_completion_animation()
        else:
            self.completion_canvas_active = True
            self.completion_canvas_ready = True
            self.show_completion_reveal_frame(1.0)

    def show_completion_prompt(self):
        self.completion_show_buttons = True
        if self.flash_complete and self.completion_canvas_active:
            self.render_completion_canvas(1.0)

    def update_flashcard_action_buttons(self):
        if "back_button" not in self.__dict__:
            return
        if "reveal_button" in self.__dict__:
            if self.flash_complete:
                self.reveal_button.configure(
                    text="Complete",
                    state="disabled",
                    fg_color=BUTTON_DISABLED_BG,
                    hover_color=BUTTON_DISABLED_BG,
                    border_color=theme_color(LINE),
                    text_color=BUTTON_DISABLED_TEXT,
                    text_color_disabled=BUTTON_DISABLED_TEXT,
                )
            else:
                reveal_text = "Start" if not self.flash_deck else ("Next" if self.card_answer_visible else "Reveal")
                self.reveal_button.configure(
                    text=reveal_text,
                    state="normal",
                    **button_style("primary"),
                    text_color_disabled=BUTTON_DISABLED_TEXT,
                )
        disabled = not self.flash_deck or (not self.flash_complete and self.flash_index <= 0)
        if disabled:
            self.back_button.configure(
                state="disabled",
                fg_color=BUTTON_DISABLED_BG,
                hover_color=BUTTON_DISABLED_BG,
                border_color=theme_color(LINE),
                text_color=BUTTON_DISABLED_TEXT,
                text_color_disabled=BUTTON_DISABLED_TEXT,
            )
            return
        self.back_button.configure(
            state="normal",
            **button_style("secondary"),
            text_color_disabled=BUTTON_DISABLED_TEXT,
        )

    def set_flash_save_status(self, status):
        status = status or "Not saved"
        self.flash_save_status_var.set(status)
        if "flash_save_status_panel" not in self.__dict__:
            return
        status_key = status.lower()
        if status_key == "saved":
            label_color = theme_color(("#166534", "#BBF7D0"))
            panel_color = theme_color(("#F0FDF4", "#172C22"))
            border_color = theme_color(("#86EFAC", "#4ADE80"))
            bar_color = theme_color(("#22C55E", "#4ADE80"))
        elif "fail" in status_key:
            label_color = theme_color(("#9F1239", "#FFE4E6"))
            panel_color = theme_color(("#FFF1F2", "#351620"))
            border_color = theme_color(("#FDA4AF", "#FB7185"))
            bar_color = theme_color(("#E11D48", "#FB7185"))
        else:
            label_color = theme_color(("#92400E", "#FDE68A"))
            panel_color = theme_color(("#FFFBEB", "#332615"))
            border_color = theme_color(("#FCD34D", "#FBBF24"))
            bar_color = theme_color(("#F59E0B", "#FBBF24"))
        self.flash_save_status_panel.configure(fg_color=panel_color, border_color=border_color)
        self.flash_save_status_label.configure(text_color=label_color)
        self.flash_save_progress_label.configure(text_color=label_color)
        self.flash_save_bar.configure(progress_color=bar_color, fg_color=theme_color(("#E8DAD2", "#3A3034")))
        self.update_flash_progress_bar()

    def update_flash_progress_bar(self):
        if "flash_save_bar" not in self.__dict__:
            return
        total = len(self.flash_deck)
        if total:
            done = total if self.flash_complete else min(max(self.flash_index + 1, 0), total)
        else:
            total = len(self.words(self.flash_level_var.get()))
            done = 0
        value = done / total if total else 0.0
        self.flash_save_progress_var.set(f"{done}/{total}" if total else "0/0")
        self.flash_save_bar.set(min(max(value, 0.0), 1.0))

    def mark_flashcards_not_saved(self):
        if self.flash_deck:
            self.set_flash_save_status("Not saved")

    def show_save_toast(self, message, success=True):
        if "save_toast" not in self.__dict__:
            return
        self.cancel_save_toast_animation()
        self.save_toast_var.set(message)
        self.save_toast_success = success
        self.save_toast_started_at = time.perf_counter()
        self.save_toast.place(relx=0.5, y=18, anchor="n")
        self.save_toast.lift()
        self.animate_save_toast()

    def cancel_save_toast_animation(self):
        for attr_name in ("save_toast_after_id", "save_toast_animation_after_id"):
            after_id = self.__dict__.get(attr_name)
            if after_id:
                try:
                    self.after_cancel(after_id)
                except tk.TclError:
                    pass
                setattr(self, attr_name, None)

    def animate_save_toast(self):
        if "save_toast" not in self.__dict__:
            return
        fade_in = 0.2
        hold = 0.95
        fade_out = 0.58
        total = fade_in + hold + fade_out
        elapsed = time.perf_counter() - self.__dict__.get("save_toast_started_at", time.perf_counter())
        fade_in_progress = 1.0
        fade_out_progress = 0.0
        if elapsed < fade_in:
            fade_in_progress = elapsed / fade_in
            alpha = ease_out_cubic(fade_in_progress)
        elif elapsed < fade_in + hold:
            alpha = 1.0
        elif elapsed < total:
            fade_out_progress = (elapsed - fade_in - hold) / fade_out
            alpha = 1.0 - ease_out_cubic(fade_out_progress)
            if alpha <= 0.16:
                self.save_toast.place_forget()
                self.save_toast_animation_after_id = None
                return
        else:
            self.save_toast.place_forget()
            self.save_toast_animation_after_id = None
            return

        base_color = self.completion_panel_color if self.flash_complete and self.completion_panel_color else theme_color(PANEL)
        if self.__dict__.get("save_toast_success", True):
            target_bg = theme_color(("#ECFDF3", "#1E3328"))
            target_border = theme_color(("#86EFAC", "#4ADE80"))
            target_text = theme_color(("#166534", "#BBF7D0"))
        else:
            target_bg = theme_color(("#FFF1F2", "#3B1821"))
            target_border = theme_color(("#FDA4AF", "#FB7185"))
            target_text = theme_color(("#9F1239", "#FFE4E6"))
        self.save_toast.configure(
            fg_color=blend_hex(base_color, target_bg, alpha),
            border_color=blend_hex(base_color, target_border, alpha),
        )
        self.save_toast_label.configure(text_color=blend_hex(base_color, target_text, alpha))
        y_offset = round((1 - ease_out_cubic(fade_in_progress)) * 8) if elapsed < fade_in else round(ease_out_cubic(fade_out_progress) * 3)
        self.save_toast.place_configure(y=18 + y_offset)
        self.save_toast_animation_after_id = self.after(16, self.animate_save_toast)

    def hide_completion_prompt(self):
        if "completion_actions" in self.__dict__ and self.completion_actions.winfo_manager():
            self.completion_actions.pack_forget()

    def keep_completed_flashcards(self):
        self.hide_completion_prompt()
        self.card_hint_var.set("Finished. Restart when you are ready.")
        self.count_var.set("Finished")
        self.completion_show_buttons = False
        self.completion_button_bounds = {}
        self.completion_hover_action = None
        self.render_completion_canvas(1.0)

    def start_completion_animation(self):
        self.stop_completion_animation()
        self.hide_completion_prompt()
        self.completion_canvas_active = True
        self.completion_canvas_ready = False
        self.completion_hover_action = None
        self.show_completion_reveal_frame(0.0)
        self.start_confetti_burst()
        self.completion_animation_started_at = time.perf_counter()
        self.animate_completion_reveal()

    def animate_completion_reveal(self):
        if not self.flash_complete:
            return
        duration = 0.58
        elapsed = time.perf_counter() - self.__dict__.get("completion_animation_started_at", time.perf_counter())
        progress = max(0.0, min(1.0, elapsed / duration))
        self.show_completion_reveal_frame(progress)
        if progress < 1.0:
            self.completion_animation_after_id = self.after(1, self.animate_completion_reveal)
        else:
            self.completion_animation_after_id = None
            self.completion_canvas_ready = True
            self.show_completion_reveal_frame(1.0)

    def stop_completion_animation(self):
        after_id = self.__dict__.get("completion_animation_after_id")
        if after_id:
            try:
                self.after_cancel(after_id)
            except tk.TclError:
                pass
        self.completion_animation_after_id = None

    def show_completion_reveal_frame(self, amount):
        if "flash_card" not in self.__dict__:
            return
        amount = max(0.0, min(1.0, amount))
        if self.completion_canvas_active:
            panel_color = self.set_completion_style(0.72)
            self.render_completion_canvas(amount, panel_color)
            return

        card_amount = ease_out_cubic(amount)
        panel_color = self.set_completion_style(0.72 * card_amount)
        self.flash_inner.grid_configure(pady=0)
        self.card_front_font.configure(size=46)
        self.card_back_font.configure(size=22)
        self.card_hint_font.configure(size=13)
        self.card_front_label.configure(text_color=theme_color(("#9A3412", "#FFE0B8")))
        self.card_back_label.configure(text_color=theme_color(INK))
        self.card_hint_label.configure(text_color=theme_color(MUTED))

    def raise_completion_canvas(self):
        if "confetti_canvas" in self.__dict__ and "flash_inner" in self.__dict__:
            self.confetti_canvas.tk.call("raise", self.confetti_canvas._w, self.flash_inner._w)

    def lower_completion_canvas(self):
        if "confetti_canvas" in self.__dict__ and "flash_inner" in self.__dict__:
            self.confetti_canvas.tk.call("lower", self.confetti_canvas._w, self.flash_inner._w)

    def clear_completion_canvas(self):
        self.completion_canvas_active = False
        self.completion_canvas_ready = False
        self.completion_button_bounds = {}
        self.completion_hover_action = None
        self.completion_style_key = None
        self.completion_panel_color = None
        if "confetti_canvas" in self.__dict__:
            self.confetti_canvas.configure(cursor="")
            self.confetti_canvas.delete("completion")
            self.lower_completion_canvas()

    def render_completion_canvas(self, amount, panel_color=None):
        if "confetti_canvas" not in self.__dict__:
            return
        amount = max(0.0, min(1.0, amount))
        panel_color = panel_color or self.set_completion_style(0.72)
        canvas = self.confetti_canvas
        canvas.configure(bg=panel_color)
        self.raise_completion_canvas()
        if canvas.winfo_width() <= 1 or canvas.winfo_height() <= 1:
            canvas.update_idletasks()
        width = max(canvas.winfo_width(), 720)
        height = max(canvas.winfo_height(), 420)
        canvas.delete("completion")
        self.completion_button_bounds = {}
        self.render_completion_vector_canvas(canvas, width, height, amount, panel_color)

    def render_completion_vector_canvas(self, canvas, width, height, amount, panel_color):
        entrance = ease_out_cubic(amount)
        opacity = ease_out_cubic(min(1.0, amount * 1.1))
        spread = 0.96 + (0.04 * entrance)
        lift = (1 - entrance) * 12
        center_x = width / 2
        center_y = (height / 2) + lift
        title_target = theme_color(("#9A3412", "#FFE0B8"))
        detail_target = theme_color(INK)
        hint_target = theme_color(MUTED)

        if opacity > 0.01:
            canvas.create_text(
                center_x,
                center_y + (-72 * spread),
                text=self.card_front_var.get(),
                fill=blend_hex(panel_color, title_target, opacity),
                font=("Segoe UI", 46, "bold"),
                tags=("completion",),
            )
            canvas.create_text(
                center_x,
                center_y + (-18 * spread),
                text=self.card_back_var.get(),
                fill=blend_hex(panel_color, detail_target, opacity),
                font=("Segoe UI", 22),
                tags=("completion",),
            )
            canvas.create_text(
                center_x,
                center_y + (24 * spread),
                text=self.card_hint_var.get(),
                fill=blend_hex(panel_color, hint_target, opacity),
                font=("Segoe UI", 13, "bold"),
                tags=("completion",),
            )

        if not self.completion_show_buttons:
            return
        button_alpha = ease_out_cubic(max(0.0, min(1.0, (amount - 0.26) / 0.74)))
        if button_alpha <= 0.01:
            return
        button_scale = 0.98 + (0.02 * button_alpha)
        total_width = min(460, max(320, width - 180)) * button_scale
        gap = 12 * button_scale
        button_width = (total_width - gap) / 2
        button_height = 42 * button_scale
        y1 = center_y + (64 * button_scale) + ((1 - button_alpha) * 10)
        y2 = y1 + button_height
        restart_x1 = center_x - total_width / 2
        restart_x2 = restart_x1 + button_width
        keep_x1 = restart_x2 + gap
        keep_x2 = keep_x1 + button_width
        self.draw_completion_button(canvas, restart_x1, y1, restart_x2, y2, "Yes, Restart", "restart", button_alpha, panel_color)
        self.draw_completion_button(canvas, keep_x1, y1, keep_x2, y2, "Not Now", "keep", button_alpha, panel_color)
        if self.completion_canvas_ready:
            self.completion_button_bounds = {
                "restart": (restart_x1, y1, restart_x2, y2),
                "keep": (keep_x1, y1, keep_x2, y2),
            }

    def draw_completion_button(self, canvas, x1, y1, x2, y2, text, action, alpha, panel_color):
        hovered = self.completion_hover_action == action
        if action == "restart":
            fill_target = theme_color((ACCENT_HOVER if hovered else ACCENT, "#5F1111" if hovered else ACCENT_DARK))
            text_target = "#FFF7ED"
            outline_target = theme_color(BUTTON_PRIMARY_BORDER)
        else:
            fill_target = theme_color(BUTTON_SECONDARY_HOVER if hovered else BUTTON_SECONDARY_BG)
            text_target = theme_color(BUTTON_SECONDARY_TEXT)
            outline_target = theme_color(BUTTON_PRIMARY_BORDER)
        fill = blend_hex(panel_color, fill_target, alpha)
        outline = blend_hex(panel_color, outline_target, alpha)
        text_color = blend_hex(panel_color, text_target, alpha)
        draw_round_rect(canvas, x1, y1, x2, y2, 12, fill=fill, outline=outline, width=1, tags=("completion",))
        canvas.create_text(
            (x1 + x2) / 2,
            (y1 + y2) / 2,
            text=text,
            fill=text_color,
            font=("Segoe UI", max(10, round(13 * (0.92 + 0.08 * alpha))), "bold"),
            tags=("completion",),
        )

    def handle_completion_canvas_click(self, event):
        if self.flash_complete and self.completion_canvas_active:
            action = self.completion_action_at(event.x, event.y)
            if action == "restart":
                self.restart_flashcards_to_ready()
            elif action == "keep":
                self.keep_completed_flashcards()
            return "break"
        return self.handle_flash_card_click(event)

    def handle_completion_canvas_motion(self, event):
        if not self.flash_complete or not self.completion_canvas_active:
            self.confetti_canvas.configure(cursor="")
            return
        action = self.completion_action_at(event.x, event.y)
        self.confetti_canvas.configure(cursor="hand2" if action else "")
        if action != self.completion_hover_action:
            self.completion_hover_action = action
            self.render_completion_canvas(1.0)

    def handle_completion_canvas_leave(self, _event):
        if "confetti_canvas" in self.__dict__:
            self.confetti_canvas.configure(cursor="")
        if self.completion_hover_action is not None:
            self.completion_hover_action = None
            if self.flash_complete and self.completion_canvas_active:
                self.render_completion_canvas(1.0)

    def handle_completion_canvas_configure(self, _event):
        if self.flash_complete and self.completion_canvas_active:
            self.render_completion_canvas(1.0)

    def completion_action_at(self, x, y):
        if not self.completion_canvas_ready:
            return None
        for action, bounds in self.completion_button_bounds.items():
            x1, y1, x2, y2 = bounds
            if x1 <= x <= x2 and y1 <= y <= y2:
                return action
        return None

    def update_confetti_canvas_bg(self, color):
        if "confetti_canvas" in self.__dict__:
            self.confetti_canvas.configure(bg=color)

    def start_confetti_burst(self):
        if "confetti_canvas" not in self.__dict__:
            return
        self.stop_confetti()
        self.confetti_canvas.update_idletasks()
        width = max(self.confetti_canvas.winfo_width(), 720)
        height = max(self.confetti_canvas.winfo_height(), 420)
        panel_color = self.completion_panel_color or self.confetti_canvas.cget("bg")
        colors = ["#F97316", "#F59E0B", "#EF4444", "#22C55E", "#38BDF8", "#A855F7", "#F43F5E"]
        self.confetti_particles = []
        for _ in range(160):
            angle = random.uniform(math.radians(205), math.radians(335))
            speed = random.uniform(9.5, 19.5)
            length = random.uniform(14, 30)
            x = random.uniform(width * 0.08, width * 0.92)
            y = random.uniform(height * 0.12, height * 0.32)
            color = random.choice(colors)
            particle = {
                "x": x,
                "y": y,
                "vx": math.cos(angle) * speed,
                "vy": math.sin(angle) * speed,
                "angle": random.uniform(0, math.tau),
                "spin": random.uniform(-0.34, 0.34),
                "length": length,
                "life": 0,
                "ttl": random.randint(38, 66),
                "fade_in": random.randint(5, 10),
                "color": color,
                "item": self.confetti_canvas.create_line(x, y, x + length, y, fill=panel_color, width=random.choice([3, 4]), tags="confetti"),
            }
            self.confetti_particles.append(particle)
        self.confetti_canvas.tag_lower("confetti")
        self.animate_confetti()

    def animate_confetti(self):
        if "confetti_canvas" not in self.__dict__:
            return
        width = max(self.confetti_canvas.winfo_width(), 720)
        height = max(self.confetti_canvas.winfo_height(), 420)
        panel_color = self.completion_panel_color or self.confetti_canvas.cget("bg")
        remaining = []
        for particle in self.confetti_particles:
            particle["life"] += 1
            particle["vy"] += 0.62
            particle["vx"] *= 0.982
            particle["vy"] *= 0.994
            particle["x"] += particle["vx"]
            particle["y"] += particle["vy"]
            particle["angle"] += particle["spin"]
            half = particle["length"] / 2
            dx = math.cos(particle["angle"]) * half
            dy = math.sin(particle["angle"]) * half
            self.confetti_canvas.coords(
                particle["item"],
                particle["x"] - dx,
                particle["y"] - dy,
                particle["x"] + dx,
                particle["y"] + dy,
            )
            fade_in = min(1.0, particle["life"] / particle["fade_in"])
            fade_out = min(1.0, max(0.0, (particle["ttl"] - particle["life"]) / 12))
            self.confetti_canvas.itemconfigure(
                particle["item"],
                fill=blend_hex(panel_color, particle["color"], fade_in * fade_out),
            )
            on_screen = -40 <= particle["x"] <= width + 40 and particle["y"] <= height + 40
            if particle["life"] < particle["ttl"] and on_screen:
                remaining.append(particle)
            else:
                self.confetti_canvas.delete(particle["item"])
        self.confetti_particles = remaining
        if remaining:
            self.confetti_after_id = self.after(24, self.animate_confetti)
        else:
            self.confetti_after_id = None

    def stop_confetti(self):
        after_id = self.__dict__.get("confetti_after_id")
        if after_id:
            try:
                self.after_cancel(after_id)
            except tk.TclError:
                pass
        self.confetti_after_id = None
        self.confetti_particles = []
        if "confetti_canvas" in self.__dict__:
            self.confetti_canvas.delete("confetti")

    def set_completion_style(self, amount):
        if "flash_card" not in self.__dict__:
            return
        style_key = (ctk.get_appearance_mode(), round(amount, 3))
        if self.completion_style_key == style_key and self.completion_panel_color:
            return self.completion_panel_color
        panel_target = "#FFE7B5" if ctk.get_appearance_mode() == "Light" else "#3A1624"
        line_target = "#F59E0B" if ctk.get_appearance_mode() == "Light" else "#F97373"
        panel_color = blend_hex(theme_color(PANEL), panel_target, amount)
        self.flash_card.configure(
            fg_color=panel_color,
            border_color=blend_hex(theme_color(LINE), line_target, amount),
            border_width=2,
        )
        self.update_confetti_canvas_bg(panel_color)
        self.completion_style_key = style_key
        self.completion_panel_color = panel_color
        return panel_color

    def save_flashcards(self):
        if not self.flash_deck:
            self.set_flash_save_status("Not saved")
            self.show_save_toast("Start a deck first", success=False)
            return
        levels = self.flash_progress.setdefault("levels", {})
        levels[self.flash_level_var.get()] = {
            "deck": [self.flash_word_key(word) for word in self.flash_deck],
            "index": self.flash_index,
            "answer_visible": self.card_answer_visible,
            "complete": self.flash_complete,
        }
        self.flash_progress["version"] = 1
        if self.write_flash_progress():
            self.set_flash_save_status("Saved")
            self.show_save_toast("Progress saved", success=True)
        else:
            self.set_flash_save_status("Save failed")
            self.show_save_toast("Save failed", success=False)

    def restore_flashcards(self, level):
        saved = self.flash_progress.get("levels", {}).get(level)
        if not isinstance(saved, dict):
            return False
        available_words = {self.flash_word_key(word): word for word in self.words(level)}
        deck = [available_words[key] for key in saved.get("deck", []) if key in available_words]
        if not deck:
            return False
        try:
            saved_index = int(saved.get("index", 0))
        except (TypeError, ValueError):
            saved_index = 0
        self.flash_deck = deck
        self.flash_index = min(max(saved_index, 0), len(deck) - 1)
        self.card_answer_visible = bool(saved.get("answer_visible", False))
        self.flash_complete = bool(saved.get("complete", False))
        if self.flash_complete:
            self.show_flashcards_complete(animate=False)
            self.set_flash_save_status("Saved")
            return True
        self.show_current_card(reveal_answer=self.card_answer_visible)
        self.set_flash_save_status("Saved")
        self.apply_flashcard_style()
        return True

    def flash_word_key(self, word):
        return json.dumps(
            [
                word.get("kanji", ""),
                word.get("furigana", ""),
                word.get("romaji", ""),
                word.get("meaning", ""),
            ],
            ensure_ascii=False,
        )

    def load_flash_progress(self):
        if not PROGRESS_PATH.exists():
            return {"version": 1, "levels": {}}
        try:
            with PROGRESS_PATH.open("r", encoding="utf-8") as progress_file:
                progress = json.load(progress_file)
        except (OSError, json.JSONDecodeError):
            return {"version": 1, "levels": {}}
        if not isinstance(progress, dict):
            return {"version": 1, "levels": {}}
        progress.setdefault("version", 1)
        if not isinstance(progress.get("levels"), dict):
            progress["levels"] = {}
        return progress

    def write_flash_progress(self):
        try:
            PROGRESS_PATH.parent.mkdir(parents=True, exist_ok=True)
            with PROGRESS_PATH.open("w", encoding="utf-8") as progress_file:
                json.dump(self.flash_progress, progress_file, ensure_ascii=False, indent=2)
        except OSError:
            return False
        return True

    def load_custom_vocabulary(self):
        if not CUSTOM_VOCAB_PATH.exists():
            return {"version": 1, "levels": {}}
        try:
            with CUSTOM_VOCAB_PATH.open("r", encoding="utf-8") as custom_file:
                data = json.load(custom_file)
        except (OSError, json.JSONDecodeError):
            return {"version": 1, "levels": {}}
        if not isinstance(data, dict):
            return {"version": 1, "levels": {}}
        data.setdefault("version", 1)
        if not isinstance(data.get("levels"), dict):
            data["levels"] = {}
        for level, level_data in list(data["levels"].items()):
            if not isinstance(level_data, dict):
                data["levels"][level] = {"removed": [], "custom": []}
                continue
            if not isinstance(level_data.get("removed"), list):
                level_data["removed"] = []
            if not isinstance(level_data.get("custom"), list):
                level_data["custom"] = []
            for item in level_data["custom"]:
                if isinstance(item, dict) and not item.get("id"):
                    item["id"] = self.new_custom_word_id()
        return data

    def write_custom_vocabulary(self):
        try:
            CUSTOM_VOCAB_PATH.parent.mkdir(parents=True, exist_ok=True)
            with CUSTOM_VOCAB_PATH.open("w", encoding="utf-8") as custom_file:
                json.dump(self.custom_vocab, custom_file, ensure_ascii=False, indent=2)
        except OSError:
            return False
        return True

    def custom_level_data(self, level):
        levels = self.custom_vocab.setdefault("levels", {})
        level_data = levels.setdefault(level, {"removed": [], "custom": []})
        level_data.setdefault("removed", [])
        level_data.setdefault("custom", [])
        return level_data

    def new_custom_word_id(self):
        return f"custom-{int(time.time() * 1000)}-{random.randint(1000, 9999)}"

    def words(self, level):
        if level not in self.words_cache:
            level_data = self.custom_level_data(level)
            removed = set(level_data.get("removed", []))
            words = []
            for base_word in self.data["vocabulary"].get(level, []):
                key = vocab_word_key(base_word)
                if key in removed:
                    continue
                words.append({**clean_vocab_word(base_word), "_source": "base", "_key": key})
            for custom_word in level_data.get("custom", []):
                cleaned = clean_vocab_word(custom_word)
                if not cleaned["furigana"]:
                    continue
                words.append({**cleaned, "_source": "custom", "_id": custom_word.get("id") or self.new_custom_word_id()})
            self.words_cache[level] = sorted(words, key=kana_sort_key)
        return self.words_cache[level]


def theme_color(color_pair):
    return color_pair[1] if ctk.get_appearance_mode() == "Dark" else color_pair[0]


def ease_out_cubic(amount):
    amount = max(0, min(1, amount))
    return 1 - pow(1 - amount, 3)


def hex_to_rgb(color):
    color = color.lstrip("#")
    return tuple(int(color[index:index + 2], 16) for index in (0, 2, 4))


def blend_hex(start, end, amount):
    amount = max(0, min(1, amount))
    start_rgb = hex_to_rgb(start)
    end_rgb = hex_to_rgb(end)
    mixed = tuple(round(start_rgb[index] + (end_rgb[index] - start_rgb[index]) * amount) for index in range(3))
    return "#{:02X}{:02X}{:02X}".format(*mixed)


def clean_vocab_word(word):
    return {
        "kanji": str(word.get("word", word.get("kanji", ""))).strip(),
        "furigana": str(word.get("reading", word.get("furigana", ""))).strip(),
        "romaji": str(word.get("romaji", "")).strip(),
        "type": str(word.get("type", "")).strip(),
        "meaning": str(word.get("meaning", "")).strip(),
    }


def vocab_word_key(word):
    cleaned = clean_vocab_word(word)
    return json.dumps(
        [cleaned["kanji"], cleaned["furigana"], cleaned["romaji"], cleaned["type"], cleaned["meaning"]],
        ensure_ascii=False,
    )


def draw_round_rect(canvas, x1, y1, x2, y2, radius, **kwargs):
    radius = min(radius, abs(x2 - x1) / 2, abs(y2 - y1) / 2)
    points = [
        x1 + radius, y1,
        x2 - radius, y1,
        x2, y1,
        x2, y1 + radius,
        x2, y2 - radius,
        x2, y2,
        x2 - radius, y2,
        x1 + radius, y2,
        x1, y2,
        x1, y2 - radius,
        x1, y1 + radius,
        x1, y1,
    ]
    return canvas.create_polygon(points, smooth=True, splinesteps=16, **kwargs)


class KanaRail(tk.Frame):
    def __init__(self, parent, group_command, kana_command, scale=1.0):
        super().__init__(parent, bg=theme_color(PANEL), bd=0, highlightthickness=0)
        self.group_command = group_command
        self.kana_command = kana_command
        self.scale = None
        self.available = set()
        self.active_kana = None
        self.expanded_groups = set()
        self.progress = {row[0]: 0.0 for row in KANA_ROWS}
        self.hover_item = None
        self.hitboxes = []
        self.animation_job = None
        self.animation_start = None
        self.animation_duration = 0.18
        self.animation_from = {}
        self.animation_to = {}
        self.apply_scale(scale)

        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(0, weight=1)
        self.canvas = tk.Canvas(self, bd=0, highlightthickness=0, yscrollincrement=16)
        self.canvas.grid(row=0, column=0, sticky="nsew")
        self.scrollbar = ctk.CTkScrollbar(self, width=12, command=self.canvas.yview)
        self.scrollbar.grid(row=0, column=1, sticky="ns", padx=(4, 0))
        self.canvas.configure(yscrollcommand=self.scrollbar.set)

        self.canvas.bind("<Configure>", lambda _event: self.draw())
        self.canvas.bind("<Button-1>", self.on_click)
        self.canvas.bind("<Motion>", self.on_motion)
        self.canvas.bind("<Leave>", self.on_leave)
        self.canvas.bind("<MouseWheel>", self.on_mousewheel)
        self.refresh_theme()

    def apply_scale(self, scale):
        self.scale = max(1.0, min(1.35, scale))
        font_scale = max(1.0, min(1.22, self.scale))
        self.outer_pad = round(8 * self.scale)
        self.title_y = round(13 * self.scale)
        self.title_gap = round(48 * self.scale)
        self.group_height = round(50 * self.scale)
        self.group_inner_gap = round(6 * self.scale)
        self.group_gap = round(8 * self.scale)
        self.child_height = round(30 * self.scale)
        self.child_gap = round(4 * self.scale)
        self.child_indent = round(16 * self.scale)
        self.title_font = ("Segoe UI", round(18 * font_scale), "bold")
        self.group_font = ("Segoe UI", round(20 * font_scale), "bold")
        self.marker_font = ("Segoe UI", round(12 * font_scale), "bold")
        self.child_kana_font = ("Segoe UI", round(13 * font_scale), "bold")
        self.child_romaji_font = ("Segoe UI", round(12 * font_scale))

    def set_scale(self, scale):
        scale = max(1.0, min(1.35, scale))
        if self.scale is not None and abs(self.scale - scale) < 0.03:
            return
        self.apply_scale(scale)
        self.draw()

    def refresh_theme(self):
        self.panel = theme_color(PANEL)
        self.line = theme_color(LINE)
        self.ink = theme_color(INK)
        self.muted = theme_color(MUTED)
        self.group_bg = theme_color(KANA_BUTTON_BG)
        self.group_hover = theme_color(KANA_BUTTON_HOVER)
        self.child_bg = theme_color(KANA_CHILD_BG)
        self.child_hover = theme_color(KANA_BUTTON_HOVER)
        self.button_line = theme_color(BUTTON_PRIMARY_BORDER)
        self.button_text = theme_color(BUTTON_SECONDARY_TEXT)
        self.active_bg = theme_color((ACCENT, ACCENT_DARK))
        self.selected_text = "#FFF7ED"
        self.configure(bg=self.panel)
        self.canvas.configure(bg=self.panel)
        self.draw()

    def set_state(self, available, active_kana, expanded_groups, animated=False):
        next_available = set(available)
        next_expanded = set(expanded_groups)
        if (
            not animated
            and next_available == self.available
            and active_kana == self.active_kana
            and next_expanded == self.expanded_groups
        ):
            return
        self.available = next_available
        self.active_kana = active_kana
        self.expanded_groups = next_expanded
        targets = {row[0]: (1.0 if row[0] in self.expanded_groups else 0.0) for row in KANA_ROWS}
        if animated:
            self.start_animation(targets)
        else:
            self.cancel_animation()
            self.progress.update(targets)
            self.draw()

    def reveal_active(self):
        if not self.active_kana:
            return
        self.draw()
        target = None
        group_kana = KANA_GROUP_BY_MEMBER.get(self.active_kana)
        for x1, y1, x2, y2, item in self.hitboxes:
            if item == ("kana", self.active_kana):
                target = (y1, y2)
                break
            if target is None and item == ("group", group_kana):
                target = (y1, y2)
        if target is None:
            return
        visible_top = self.canvas.canvasy(0)
        visible_height = max(1, self.canvas.winfo_height())
        visible_bottom = visible_top + visible_height
        padding = round(14 * self.scale)
        target_top, target_bottom = target
        if target_top >= visible_top + padding and target_bottom <= visible_bottom - padding:
            return
        bbox = self.canvas.bbox("all")
        content_height = max(visible_height, bbox[3] if bbox else visible_height)
        if target_top < visible_top + padding:
            next_top = max(0, target_top - padding)
        else:
            next_top = min(content_height - visible_height, target_bottom - visible_height + padding)
        self.canvas.yview_moveto(next_top / max(1, content_height))

    def start_animation(self, targets):
        self.cancel_animation()
        self.animation_start = time.perf_counter()
        self.animation_from = self.progress.copy()
        self.animation_to = targets
        self.run_animation()

    def cancel_animation(self):
        if self.animation_job is not None:
            try:
                self.after_cancel(self.animation_job)
            except tk.TclError:
                pass
            self.animation_job = None

    def run_animation(self):
        try:
            if not self.winfo_exists() or not self.canvas.winfo_exists():
                self.animation_job = None
                return
        except tk.TclError:
            self.animation_job = None
            return
        amount = ease_out_cubic((time.perf_counter() - self.animation_start) / self.animation_duration)
        for kana, target in self.animation_to.items():
            start = self.animation_from.get(kana, 0.0)
            self.progress[kana] = start + (target - start) * amount
        try:
            self.draw()
        except tk.TclError:
            self.animation_job = None
            return
        if amount >= 1:
            self.progress.update(self.animation_to)
            self.animation_job = None
            try:
                self.draw()
            except tk.TclError:
                pass
            return
        self.animation_job = self.after(16, self.run_animation)

    def row_is_available(self, row):
        return any(kana in self.available for kana in row)

    def draw(self):
        self.canvas.delete("all")
        self.hitboxes = []
        width = max(110, self.canvas.winfo_width())
        x1 = self.outer_pad
        x2 = max(92, width - self.outer_pad)
        y = round(16 * self.scale)

        self.canvas.create_text(
            x1 + 4,
            y + self.title_y,
            text="Kana",
            fill=self.ink,
            anchor="w",
            font=self.title_font,
        )
        y += self.title_gap

        for row in KANA_ROWS:
            group_kana = row[0]
            active_group = self.row_is_available(row)
            hovering = self.hover_item == ("group", group_kana)
            fill = self.group_hover if hovering and active_group else self.group_bg
            text_color = self.button_text if active_group else self.muted
            draw_round_rect(self.canvas, x1, y, x2, y + self.group_height, round(10 * self.scale), fill=fill, outline=self.button_line, width=1)
            self.canvas.create_text((x1 + x2) / 2, y + self.group_height / 2, text=group_kana, fill=text_color, font=self.group_font)
            marker = "▴" if self.progress[group_kana] > 0.5 else "▾"
            self.canvas.create_text(x2 - round(16 * self.scale), y + self.group_height / 2, text=marker, fill=text_color, font=self.marker_font)
            self.hitboxes.append((x1, y, x2, y + self.group_height, ("group", group_kana)))
            y += self.group_height + self.group_inner_gap

            visible_children = self.progress[group_kana]
            child_area_height = len(row) * (self.child_height + self.child_gap)
            child_limit = y + child_area_height * visible_children
            for child_kana in row:
                child_y = y
                visible_fraction = max(0, min(1, (child_limit - child_y) / self.child_height))
                if visible_fraction > 0:
                    child_active = child_kana in self.available
                    selected = child_active and child_kana == self.active_kana
                    child_hover = self.hover_item == ("kana", child_kana)
                    if selected:
                        child_fill = self.active_bg
                        child_text = self.selected_text
                    else:
                        child_fill = self.child_hover if child_hover and child_active else self.child_bg
                        child_text = self.button_text if child_active else self.muted
                    visible_bottom = min(child_y + self.child_height, child_limit)
                    draw_round_rect(self.canvas, x1 + self.child_indent, child_y, x2, visible_bottom, round(8 * self.scale), fill=child_fill, outline=self.button_line, width=1)
                    if visible_fraction > 0.55:
                        self.canvas.create_text(x1 + round(28 * self.scale), child_y + self.child_height / 2, text=child_kana, fill=child_text, anchor="w", font=self.child_kana_font)
                        self.canvas.create_text(x1 + round(56 * self.scale), child_y + self.child_height / 2, text=KANA_ROMAJI[child_kana], fill=child_text, anchor="w", font=self.child_romaji_font)
                    if visible_fraction > 0.85:
                        self.hitboxes.append((x1 + self.child_indent, child_y, x2, child_y + self.child_height, ("kana", child_kana)))
                y += self.child_height + self.child_gap

            y -= child_area_height * (1 - visible_children)
            y += self.group_gap

        self.canvas.configure(scrollregion=(0, 0, width, max(y + 8, self.canvas.winfo_height())))

    def item_at(self, event):
        y = self.canvas.canvasy(event.y)
        x = self.canvas.canvasx(event.x)
        for x1, y1, x2, y2, item in self.hitboxes:
            if x1 <= x <= x2 and y1 <= y <= y2:
                return item
        return None

    def on_click(self, event):
        item = self.item_at(event)
        if not item:
            return
        kind, value = item
        if kind == "group":
            if self.row_is_available(KANA_GROUP_MEMBERS[value]):
                self.group_command(value)
        elif value in self.available:
            self.kana_command(value)

    def on_motion(self, event):
        item = self.item_at(event)
        if item != self.hover_item:
            self.hover_item = item
            self.draw()

    def on_leave(self, _event):
        if self.hover_item is not None:
            self.hover_item = None
            self.draw()

    def on_mousewheel(self, event):
        steps = int(-1 * (event.delta / 120))
        if steps:
            self.canvas.yview_scroll(steps * 3, "units")


def create_search_icon_image(color):
    scale = 4
    size = 28 * scale
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    line_width = 2 * scale
    draw.ellipse((7 * scale, 6 * scale, 18 * scale, 17 * scale), outline=color, width=line_width)
    draw.line((16 * scale, 16 * scale, 23 * scale, 23 * scale), fill=color, width=line_width)
    return image


def create_pencil_icon_image(color):
    scale = 4
    size = 28 * scale
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    body = [
        (8 * scale, 21 * scale),
        (6 * scale, 19 * scale),
        (18 * scale, 7 * scale),
        (21 * scale, 10 * scale),
    ]
    tip = [
        (18 * scale, 7 * scale),
        (20 * scale, 5 * scale),
        (23 * scale, 8 * scale),
        (21 * scale, 10 * scale),
    ]
    edge_width = 2 * scale
    draw.line((8 * scale, 21 * scale, 21 * scale, 8 * scale), fill=color, width=4 * scale)
    draw.line((6 * scale, 19 * scale, 9 * scale, 22 * scale), fill=color, width=4 * scale)
    draw.polygon(body, fill=color)
    draw.polygon(tip, fill=color)
    draw.line((5 * scale, 23 * scale, 10 * scale, 22 * scale), fill=color, width=edge_width)
    return image


def create_import_icon_image(color):
    scale = 4
    size = 28 * scale
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    line_width = 2 * scale
    draw.line((14 * scale, 5 * scale, 14 * scale, 18 * scale), fill=color, width=line_width)
    draw.line((9 * scale, 13 * scale, 14 * scale, 18 * scale), fill=color, width=line_width)
    draw.line((19 * scale, 13 * scale, 14 * scale, 18 * scale), fill=color, width=line_width)
    draw.line((7 * scale, 17 * scale, 7 * scale, 22 * scale), fill=color, width=line_width)
    draw.line((21 * scale, 17 * scale, 21 * scale, 22 * scale), fill=color, width=line_width)
    draw.line((7 * scale, 22 * scale, 21 * scale, 22 * scale), fill=color, width=line_width)
    return image


CSV_HEADER_ALIASES = {
    "kanji": ("kanji", "word", "vocab", "vocabulary", "term", "japanese"),
    "furigana": ("furigana", "kana", "hiragana", "reading", "yomikata"),
    "romaji": ("romaji", "romanji", "roumaji", "romaji reading", "romanized"),
    "type": ("type", "word type", "part of speech", "pos"),
    "meaning": ("meaning", "english", "definition", "translation", "gloss"),
}


def normalize_csv_header(value):
    return "".join(character for character in str(value).lower() if character.isalnum())


def find_csv_column(fieldnames, aliases):
    normalized = {normalize_csv_header(name): name for name in fieldnames}
    for alias in aliases:
        key = normalize_csv_header(alias)
        if key in normalized:
            return normalized[key]
    return None


def load_vocabulary_csv(path):
    try:
        with path.open("r", encoding="utf-8-sig", newline="") as csv_file:
            sample = csv_file.read(4096)
            csv_file.seek(0)
            try:
                dialect = csv.Sniffer().sniff(sample, delimiters=",;\t")
            except csv.Error:
                dialect = csv.excel
            reader = csv.DictReader(csv_file, dialect=dialect)
            if not reader.fieldnames:
                raise ValueError("CSV needs a header row.")
            columns = {key: find_csv_column(reader.fieldnames, aliases) for key, aliases in CSV_HEADER_ALIASES.items()}
            missing = [key for key in ("kanji", "furigana", "meaning") if columns[key] is None]
            if missing:
                expected = "word, reading, romaji, type (optional), meaning"
                raise ValueError(f"CSV is missing {', '.join(missing)}. Expected columns: {expected}.")

            words = []
            invalid_rows = []
            for row_number, row in enumerate(reader, start=2):
                word = {
                    key: str((row.get(column_name, "") if column_name else "") or "").strip()
                    for key, column_name in columns.items()
                }
                if not any(word.values()):
                    continue
                if not word["kanji"] or not word["furigana"] or not word["meaning"]:
                    invalid_rows.append(row_number)
                    continue
                words.append(clean_vocab_word(word))
    except (OSError, UnicodeError):
        raise ValueError("Could not read that CSV file.")

    if invalid_rows:
        first_bad_row = invalid_rows[0]
        raise ValueError(f"{len(invalid_rows)} row(s) are missing word, reading, or meaning. First bad row: {first_bad_row}.")
    if not words:
        raise ValueError("No vocabulary rows found in that CSV.")
    return words


def detect_display_scale(window):
    try:
        dpi = ctypes.windll.user32.GetDpiForWindow(window.winfo_id())
        if dpi:
            return max(1.0, min(1.6, dpi / 96))
    except Exception:
        pass
    try:
        dpi = window.winfo_fpixels("1i")
        if dpi:
            return max(1.0, min(1.6, dpi / 96))
    except Exception:
        pass
    return 1.0


class VocabGrid(tk.Frame):
    def __init__(self, parent, scale=1.0, active_kana_callback=None, selection_callback=None):
        super().__init__(parent, bd=0, highlightthickness=1)
        self.scale = None
        self.active_kana_callback = active_kana_callback
        self.selection_callback = selection_callback
        self.synced_kana = None
        self.mode = "Vocabulary"
        self.group_by_kana = True
        self.columns = []
        self.configure_mode(self.mode)
        self.rows = []
        self.items = []
        self.item_tops = []
        self.item_bottoms = []
        self.row_tops = {}
        self.row_section_tops = {}
        self.section_tops = {}
        self.hover_index = None
        self.selected_index = None
        self.kana_sync_mode = "scroll"
        self.rendering_enabled = True
        if self.selection_callback:
            self.selection_callback(None)
        self.selection_alpha = 1
        self.selection_animation_job = None
        self.scroll_animation_job = None
        self.scrollbar_hide_job = None
        self.apply_scale(scale)
        self.header_height = 0

        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(0, weight=1)

        self.header = tk.Canvas(self, height=self.header_height, bd=0, highlightthickness=0)

        self.body = tk.Canvas(self, bd=0, highlightthickness=0, yscrollincrement=1)
        self.body.grid(row=0, column=0, sticky="nsew")
        self.scrollbar = ctk.CTkScrollbar(self, command=self.on_scrollbar)
        self.body.configure(yscrollcommand=self.set_scrollbar)
        self.scrollbar.bind("<ButtonPress-1>", lambda _event: self.show_transient_scrollbar(), add="+")
        self.scrollbar.bind("<B1-Motion>", lambda _event: self.show_transient_scrollbar(), add="+")

        self.header.bind("<Configure>", lambda _event: self.draw_header())
        self.body.bind("<Configure>", lambda _event: self.draw_body())
        self.body.bind("<Motion>", self.on_motion)
        self.body.bind("<Leave>", self.on_leave)
        self.body.bind("<Button-1>", self.on_click)
        self.body.bind("<MouseWheel>", self.on_mousewheel)
        self.header.bind("<MouseWheel>", self.on_mousewheel)
        self.refresh_theme()

    def configure_mode(self, mode):
        self.mode = mode
        self.group_by_kana = mode == "Vocabulary"
        if mode == "Vocabulary":
            self.columns = [
                ("kanji", "Word", 1.15),
                ("furigana", "Reading", 1.15),
                ("romaji", "Romaji", 0.95),
                ("type", "Type", 1.35),
                ("meaning", "Meaning", 2.2),
            ]
        elif mode == "Grammar":
            self.columns = [
                ("pattern", "Grammar", 1.5),
                ("romaji", "Romaji", 1.15),
                ("meaning", "Meaning", 2.75),
            ]
        else:
            self.columns = [
                ("kanji", "Kanji", 0.65),
                ("onyomi", "On'yomi", 1.8),
                ("kunyomi", "Kun'yomi", 1.8),
                ("meaning", "Meaning", 2.25),
            ]
        if hasattr(self, "body"):
            # Rows from the previous mode use a different schema; render_vocab_rows
            # immediately supplies the new mode's rows after this reconfiguration.
            self.rows = []
            self.build_items()
            self.body.configure(scrollregion=(0, 0, 0, max(1, self.content_height())))
            self.draw_body()

    def apply_scale(self, scale):
        self.scale = scale
        self.cell_pad = round(10 * scale)
        self.row_height = round(54 * scale)
        self.table_header_height = round(36 * scale)
        self.section_height = round(60 * scale)
        self.section_gap = round(42 * scale)
        self.bottom_padding = round(54 * scale)
        self.header_font = ("Segoe UI", round(12 * scale), "bold")
        self.row_font = ("Segoe UI", round(13 * scale))
        self.section_font = ("Segoe UI", round(14 * scale), "bold")

    def set_scale(self, scale):
        if self.scale is not None and abs(self.scale - scale) < 0.01:
            return
        self.apply_scale(scale)
        self.recalculate_positions()
        self.body.configure(scrollregion=(0, 0, 0, max(1, self.content_height())))
        self.draw_header()
        self.draw_body()

    def set_rendering_enabled(self, enabled):
        enabled = bool(enabled)
        if self.rendering_enabled == enabled:
            return
        self.rendering_enabled = enabled
        if enabled:
            self.draw_header()
            self.draw_body()

    def refresh_theme(self):
        self.panel = theme_color(PANEL)
        self.ink = theme_color(INK)
        self.muted = theme_color(MUTED)
        self.grid_line = theme_color(TABLE_GRID)
        self.header_bg = "#5B0D18" if ctk.get_appearance_mode() == "Dark" else "#8F1717"
        self.header_text = "#FFF7ED"
        self.section_bg = "#2A1721" if ctk.get_appearance_mode() == "Dark" else "#FFF9F1"
        self.row_alt_bg = theme_color(TABLE_ROW_ALT)
        self.hover_bg = theme_color(TABLE_ROW_HOVER)
        self.selected_bg = ACCENT_DARK if ctk.get_appearance_mode() == "Dark" else ACCENT
        self.selected_text = "#FFF7ED"
        self.configure(bg=self.panel, highlightbackground=self.grid_line, highlightcolor=self.grid_line)
        self.header.configure(bg=self.header_bg)
        self.body.configure(bg=self.panel)
        self.draw_header()
        self.draw_body()

    def set_rows(self, rows):
        self.rows = rows
        self.build_items()
        self.hover_index = None
        self.selected_index = None
        self.kana_sync_mode = "scroll"
        self.selection_alpha = 1
        self.cancel_scroll_animation()
        self.cancel_selection_animation()
        self.synced_kana = None
        self.body.configure(scrollregion=(0, 0, 0, max(1, self.content_height())))
        self.draw_body()
        self.after_idle(self.sync_active_kana)

    def build_items(self):
        self.items = []
        if not self.group_by_kana:
            if self.rows:
                self.items.append({"kind": "table_header"})
            for row_index, row in enumerate(self.rows):
                self.items.append({"kind": "word", "word": row, "row_index": row_index})
            self.recalculate_positions()
            return
        last_kana = None
        for row_index, word in enumerate(self.rows):
            kana = normalized_first_kana(word["furigana"])
            if kana and kana != last_kana:
                if self.items:
                    self.items.append({"kind": "gap"})
                self.items.append({"kind": "section", "kana": kana})
                self.items.append({"kind": "table_header"})
                last_kana = kana
            self.items.append({"kind": "word", "word": word, "row_index": row_index})
        self.recalculate_positions()

    def recalculate_positions(self):
        self.item_tops = []
        self.item_bottoms = []
        self.row_tops = {}
        self.row_section_tops = {}
        self.section_tops = {}
        top = 0
        current_section_top = 0
        for item_index, item in enumerate(self.items):
            self.item_tops.append(top)
            if item["kind"] == "section":
                current_section_top = top
                self.section_tops[item["kana"]] = top
                top += self.section_height
            elif item["kind"] == "gap":
                top += self.section_gap
            elif item["kind"] == "table_header":
                top += self.table_header_height
            else:
                self.row_tops[item["row_index"]] = top
                self.row_section_tops[item["row_index"]] = current_section_top
                top += self.row_height
            self.item_bottoms.append(top)
        self.total_height = top

    def content_height(self):
        return getattr(self, "total_height", 0) + getattr(self, "bottom_padding", 0)

    def column_edges(self, width):
        table_width = self.table_width(width)
        total_weight = sum(column[2] for column in self.columns)
        widths = [round(table_width * column[2] / total_weight) for column in self.columns[:-1]]
        widths.append(table_width - sum(widths))
        edges = [self.table_left(width)]
        for column_width in widths:
            edges.append(edges[-1] + column_width)
        return edges

    def table_left(self, width):
        return max(16, int((width - self.table_width(width)) / 2))

    def table_width(self, width):
        return min(1500, max(480, width - 32))

    def draw_header(self):
        if not self.rendering_enabled:
            return
        self.header.delete("all")
        width = max(1, self.header.winfo_width())
        edges = self.column_edges(width)
        full_width = max(width, edges[-1])
        self.header.create_rectangle(0, 0, full_width, self.header_height, fill=self.header_bg, outline=self.grid_line)
        for index in range(len(self.columns)):
            self.header.create_rectangle(
                edges[index],
                0,
                edges[index + 1],
                self.header_height,
                fill=self.header_bg,
                outline=self.grid_line,
                width=1,
            )
        for index, (_key, label, _width) in enumerate(self.columns):
            self.header.create_text(
                edges[index] + self.cell_pad,
                self.header_height / 2,
                text=label,
                fill=self.header_text,
                anchor="w",
                font=self.header_font,
            )

    def draw_body(self):
        if not self.rendering_enabled:
            return
        self.body.delete("all")
        width = max(1, self.body.winfo_width())
        height = max(1, self.body.winfo_height())
        content_height = max(1, self.content_height())
        edges = self.column_edges(width)
        self.body.configure(scrollregion=(0, 0, max(width, edges[-1]), content_height))
        full_width = max(width, edges[-1])
        table_right = edges[-1]
        visible_top = self.body.canvasy(0)
        visible_bottom = self.body.canvasy(height)
        buffer = max(self.row_height, self.section_height, self.section_gap) * 3
        start_y = max(0, visible_top - buffer)
        end_y = min(content_height, visible_bottom + buffer)
        start_index = max(0, bisect.bisect_left(self.item_bottoms, start_y))
        for item_index in range(start_index, len(self.items)):
            y0 = self.item_tops[item_index]
            if y0 > end_y:
                break
            item = self.items[item_index]
            y0 = self.item_tops[item_index]
            if item["kind"] == "gap":
                y1 = y0 + self.section_gap
                self.body.create_rectangle(0, y0, full_width, y1, fill=self.panel, outline="")
                self.body.create_line(edges[0], y0, table_right, y0, fill=self.grid_line, width=1)
                continue

            if item["kind"] == "section":
                y1 = y0 + self.section_height
                self.body.create_rectangle(0, y0, full_width, y1, fill=self.panel, outline="")
                self.body.create_text(
                    edges[0],
                    y0 + self.section_height - 26,
                    text=item["kana"],
                    fill=ACCENT if ctk.get_appearance_mode() == "Light" else "#FFD8C8",
                    anchor="w",
                    font=self.section_font,
                )
                self.body.create_line(edges[0], y1 - 1, table_right, y1 - 1, fill=self.grid_line, width=2)
                continue

            if item["kind"] == "table_header":
                y1 = y0 + self.table_header_height
                for column_index, (_key, label, _width) in enumerate(self.columns):
                    self.body.create_rectangle(
                        edges[column_index],
                        y0,
                        edges[column_index + 1],
                        y1,
                        fill=self.header_bg,
                        outline=self.grid_line,
                        width=1,
                    )
                    self.body.create_text(
                        edges[column_index] + self.cell_pad,
                        y0 + self.table_header_height / 2,
                        text=label,
                        fill=self.header_text,
                        anchor="w",
                        font=self.header_font,
                    )
                continue

            row_index = item["row_index"]
            word = item["word"]
            y1 = y0 + self.row_height
            row_fill = self.row_alt_bg if row_index % 2 else self.panel
            selected = row_index == self.selected_index
            if row_index == self.selected_index:
                row_fill = blend_hex(row_fill, self.selected_bg, self.selection_alpha)
            elif row_index == self.hover_index:
                row_fill = self.hover_bg
            values = [str(word.get(key, "")) for key, _label, _weight in self.columns]
            for column_index, value in enumerate(values):
                self.body.create_rectangle(
                    edges[column_index],
                    y0,
                    edges[column_index + 1],
                    y1,
                    fill=row_fill,
                    outline=self.grid_line,
                    width=1,
                )
                self.body.create_text(
                    edges[column_index] + self.cell_pad,
                    y0 + self.row_height / 2,
                    text=value,
                    fill=self.selected_text if selected else ((ACCENT if ctk.get_appearance_mode() == "Light" else "#FFD1C1") if column_index == 0 and value else self.ink),
                    anchor="w",
                    font=self.row_font,
                    width=max(24, edges[column_index + 1] - edges[column_index] - (self.cell_pad * 2)),
                )
            next_item = self.items[item_index + 1] if item_index + 1 < len(self.items) else None
            if next_item is None or next_item.get("kind") != "word":
                self.body.create_line(edges[0], y1 - 1, table_right, y1 - 1, fill=self.grid_line, width=2)

    def on_click(self, event):
        row_index = self.row_at_y(self.body.canvasy(event.y))
        self.select_index(None if row_index == self.selected_index else row_index, animated=True)

    def on_motion(self, event):
        row_index = self.row_at_y(self.body.canvasy(event.y))
        if row_index != self.hover_index:
            self.hover_index = row_index
            self.draw_body()

    def row_at_y(self, y):
        item_index = bisect.bisect_right(self.item_tops, y) - 1
        if 0 <= item_index < len(self.items) and y < self.item_bottoms[item_index]:
            item = self.items[item_index]
            return item.get("row_index") if item["kind"] == "word" else None
        return None

    def on_leave(self, _event):
        if self.hover_index is not None:
            self.hover_index = None
            self.draw_body()

    def on_mousewheel(self, event):
        self.cancel_scroll_animation()
        self.show_transient_scrollbar()
        steps = int(-1 * (event.delta / 120))
        if steps:
            self.kana_sync_mode = "scroll"
            self.body.yview_scroll(steps * max(72, int(self.row_height * 1.5)), "units")
            self.draw_body()
            self.sync_active_kana()

    def set_scrollbar(self, first, last):
        self.scrollbar.set(first, last)

    def on_scrollbar(self, *args):
        self.cancel_scroll_animation()
        self.show_transient_scrollbar()
        self.kana_sync_mode = "scroll"
        self.body.yview(*args)
        self.draw_body()
        self.sync_active_kana()

    def show_transient_scrollbar(self):
        self.scrollbar.place(relx=1.0, rely=0, relheight=1.0, anchor="ne")
        self.scrollbar.lift()
        if self.scrollbar_hide_job is not None:
            self.after_cancel(self.scrollbar_hide_job)
        self.scrollbar_hide_job = self.after(1200, self.hide_transient_scrollbar)

    def hide_transient_scrollbar(self):
        self.scrollbar_hide_job = None
        self.scrollbar.place_forget()

    def see(self, index, animated=False):
        if not self.rows:
            return
        content_height = max(1, self.content_height())
        view_height = max(1, self.body.winfo_height())
        max_top = max(0, content_height - view_height)
        top = min(self.row_section_tops.get(index, self.row_tops.get(index, 0)), max_top)
        if animated:
            self.smooth_scroll_to(top)
            return
        self.body.yview_moveto(top / content_height)
        self.draw_body()
        self.sync_active_kana()

    def select_index(self, index, animated=False):
        self.cancel_selection_animation()
        self.selected_index = index
        self.kana_sync_mode = "selection" if index is not None else "scroll"
        if self.selection_callback:
            self.selection_callback(index)
        self.sync_active_kana()
        if animated and index is not None:
            self.selection_alpha = 0
            self.animate_selection(0)
            return
        self.selection_alpha = 1
        self.draw_body()

    def clear_selection(self):
        if self.selected_index is not None:
            self.select_index(None)

    def cancel_selection_animation(self):
        if self.selection_animation_job is not None:
            self.after_cancel(self.selection_animation_job)
            self.selection_animation_job = None

    def animate_selection(self, step):
        steps = 6
        self.selection_alpha = ease_out_cubic(min(1, step / steps))
        self.draw_body()
        if step >= steps:
            self.selection_alpha = 1
            self.selection_animation_job = None
            self.draw_body()
            return
        self.selection_animation_job = self.after(18, lambda: self.animate_selection(step + 1))

    def current_scroll_top(self):
        content_height = max(1, self.content_height())
        return self.body.yview()[0] * content_height

    def cancel_scroll_animation(self):
        if self.scroll_animation_job is not None:
            self.after_cancel(self.scroll_animation_job)
            self.scroll_animation_job = None

    def smooth_scroll_to(self, target_top):
        self.cancel_scroll_animation()
        content_height = max(1, self.content_height())
        view_height = max(1, self.body.winfo_height())
        max_top = max(0, content_height - view_height)
        start_top = self.current_scroll_top()
        target_top = max(0, min(target_top, max_top))
        if abs(target_top - start_top) < 2:
            self.body.yview_moveto(target_top / content_height)
            self.draw_body()
            self.sync_active_kana()
            return
        distance = abs(target_top - start_top)
        duration = max(0.18, min(0.32, distance / max(3600, self.row_height * 80)))
        self.animate_scroll(start_top, target_top, time.perf_counter(), duration)

    def smooth_scroll_by(self, direction):
        self.kana_sync_mode = "scroll"
        step = max(64, int(self.row_height * 1.45))
        self.smooth_scroll_to(self.current_scroll_top() + (direction * step))

    def animate_scroll(self, start_top, target_top, start_time, duration):
        self.show_transient_scrollbar()
        content_height = max(1, self.content_height())
        amount = ease_out_cubic((time.perf_counter() - start_time) / duration)
        top = start_top + (target_top - start_top) * amount
        self.body.yview_moveto(top / content_height)
        self.draw_body()

        if amount >= 1:
            self.body.yview_moveto(target_top / content_height)
            self.draw_body()
            self.sync_active_kana()
            self.scroll_animation_job = None
            return

        self.scroll_animation_job = self.after(17, lambda: self.animate_scroll(start_top, target_top, start_time, duration))

    def visible_kana(self):
        if not self.group_by_kana or not self.items:
            return None
        focus_y = self.body.canvasy(0) + min(140, max(24, self.body.winfo_height() * 0.18))
        item_index = min(len(self.items) - 1, max(0, bisect.bisect_right(self.item_tops, focus_y) - 1))
        for index in range(item_index, -1, -1):
            item = self.items[index]
            if item["kind"] == "section":
                return item["kana"]
            if item["kind"] == "word":
                return normalized_first_kana(item["word"]["furigana"])
        return None

    def selected_kana(self):
        if not self.group_by_kana or self.selected_index is None:
            return None
        if 0 <= self.selected_index < len(self.rows):
            return normalized_first_kana(self.rows[self.selected_index]["furigana"])
        return None

    def sync_active_kana(self):
        if not self.group_by_kana:
            return
        kana = self.selected_kana() if self.kana_sync_mode == "selection" else None
        kana = kana or self.visible_kana()
        if kana and kana != self.synced_kana:
            self.synced_kana = kana
            if self.active_kana_callback:
                self.active_kana_callback(kana)


def to_hiragana(text):
    converted = []
    for char in text:
        code = ord(char)
        if 0x30A1 <= code <= 0x30F6:
            converted.append(chr(code - 0x60))
        else:
            converted.append(char)
    return "".join(converted).translate(KANA_NORMALIZE)


def normalized_first_kana(text):
    for char in to_hiragana(text):
        if char in KANA_INDEX:
            return char
    return ""


def sortable_kana_reading(text):
    return "".join(char for char in to_hiragana(text) if char in KANA_INDEX)


def kana_sort_key(word):
    raw_reading = to_hiragana(word.get("furigana", ""))
    reading = sortable_kana_reading(raw_reading)
    order = tuple(KANA_INDEX.get(char, 999 + ord(char)) for char in reading) or (999,)
    return order, reading, raw_reading, word.get("romaji", "")


def button_style(variant="primary"):
    styles = {
        "primary": {
            "fg_color": (ACCENT, ACCENT_DARK),
            "hover_color": (ACCENT_HOVER, "#5F1111"),
            "border_width": 1,
            "border_color": BUTTON_PRIMARY_BORDER,
            "text_color": "#FFF7ED",
        },
        "secondary": {
            "fg_color": BUTTON_SECONDARY_BG,
            "hover_color": BUTTON_SECONDARY_HOVER,
            "border_width": 1,
            "border_color": BUTTON_PRIMARY_BORDER,
            "text_color": BUTTON_SECONDARY_TEXT,
        },
        "topbar": {
            "fg_color": BUTTON_TOP_BG,
            "hover_color": BUTTON_TOP_HOVER,
            "border_width": 1,
            "border_color": BUTTON_PRIMARY_BORDER,
            "text_color": "#FFF7ED",
        },
        "topbar_active": {
            "fg_color": (ACCENT, ACCENT_DARK),
            "hover_color": (ACCENT_HOVER, "#5F1111"),
            "border_width": 1,
            "border_color": BUTTON_PRIMARY_BORDER,
            "text_color": "#FFF7ED",
        },
    }
    return styles.get(variant, styles["primary"]).copy()


def themed_button(parent, variant="primary", **kwargs):
    options = button_style(variant)
    options.update(kwargs)
    return ctk.CTkButton(parent, **options)


def red_button(parent, **kwargs):
    return themed_button(parent, "primary", **kwargs)


def secondary_button(parent, **kwargs):
    return themed_button(parent, "secondary", **kwargs)


def apply_button_style(button, variant="primary"):
    button.configure(**button_style(variant))


def red_option_menu(parent, **kwargs):
    return ctk.CTkOptionMenu(
        parent,
        fg_color=(ACCENT, ACCENT_DARK),
        button_color=(ACCENT_DARK, ACCENT_DARK),
        button_hover_color=(ACCENT_HOVER, "#5F1111"),
        dropdown_hover_color=(ACCENT_HOVER, "#5F1111"),
        **kwargs,
    )


def load_data():
    with DATA_PATH.open("r", encoding="utf-8") as data_file:
        return json.load(data_file)


if __name__ == "__main__":
    JLPTStudyApp().mainloop()
