package com.banano.kaliumwallet.model;

/**
 * Enum that maps locales to language strings for runtime language changes
 */
public enum AvailableLanguage {
    DEFAULT("DEFAULT"),
    ENGLISH("ENGLISH"),
    CHINESE_SIMPLIFIED("CHINESE_SIMPLIFIED"),
    CHINESE_TRADITIONAL("CHINESE_TRADITIONAL"),
    DUTCH("DUTCH"),
    FRENCH("FRENCH"),
    GERMAN("GERMAN"),
    HEBREW("HEBREW"),
    HINDI("HINDI"),
    HUNGARIAN("HUNGARIAN"),
    MALAY("MALAY"),
    PORTUGUESE("PORTUGUESE"),
    RUSSIAN("RUSSIAN"),
    SPANISH("SPANISH"),
    SWEDISH("SWEDISH"),
    TAGALOG("TAGALOG"),
    TURKISH("TURKISH"),
    VIETNAMESE("VIETNAMESE");


    private String name;

    AvailableLanguage(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getLocaleString() {
        switch (name) {
            case "ENGLISH":
                return "en";
            case "FRENCH":
                return "fr";
            case "GERMAN":
                return "de";
            case "SPANISH":
                return "es";
            case "HINDI":
                return "hi";
            case "HUNGARIAN":
                return "hu";
            case "HEBREW":
                return "iw";
            case "DUTCH":
                return "nl";
            case "PORTUGUESE":
                return "pt";
            case "RUSSIAN":
                return "ru";
            case "SWEDISH":
                return "sv";
            case "TAGALOG":
                return "tl";
            case "TURKISH":
                return "tr";
            case "VIETNAMESE":
                return "vi";
            case "CHINESE_SIMPLIFIED":
                return "zh";
            case "CHINESE_TRADITIONAL":
                return "zh-tw";
            case "MALAY":
                return "ms";
            default:
                return "DEFAULT";
        }
    }

    public String getDisplayName() {
        switch (name) {
            case "ENGLISH":
                return "English (EN)";
            case "FRENCH":
                return "Français (FR)";
            case "GERMAN":
                return "Deutsch (DE)";
            case "SPANISH":
                return "Español (ES)";
            case "HINDI":
                return "हिन्दी (HI)";
            case "HUNGARIAN":
                return "Magyar (HU)";
            case "HEBREW":
                return "Hebrew (HE)";
            case "DUTCH":
                return "Nederlands (NL)";
            case "PORTUGUESE":
                return "Português (PT)";
            case "RUSSIAN":
                return "Русский язык (RU)";
            case "SWEDISH":
                return "Svenska (SV)";
            case "TAGALOG":
                return "Tagalog (TL)";
            case "TURKISH":
                return "Türkçe (TR)";
            case "VIETNAMESE":
                return "Tiếng Việt (VI)";
            case "CHINESE_SIMPLIFIED":
                return "简化字 (ZH)";
            case "CHINESE_TRADITIONAL":
                return "正體字 (ZH-TW)";
            case "MALAY":
                return "Bahasa Melayu (MS)";
            default:
                return "DEFAULT";
        }
    }
}