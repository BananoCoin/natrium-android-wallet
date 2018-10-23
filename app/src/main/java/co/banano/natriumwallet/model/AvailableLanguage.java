package co.banano.natriumwallet.model;

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
    GREEK("GREEK"),
    HINDI("HINDI"),
    HEBREW("HEBREW"),
    HUNGARIAN("HUNGARIAN"),
    INDONESIAN("INDONESIAN"),
    KOREAN("KOREAN"),
    ITALIAN("ITALIAN"),
    MALAY("MALAY"),
    PORTUGUESE("PORTUGUESE"),
    ROMANIAN("ROMANIAN"),
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
            case "GREEK":
                return "el";
            case "HUNGARIAN":
                return "hu";
            case "HINDI":
                return "hi";
            case "HEBREW":
                return "iw";
            case "INDONESIAN":
                return "in";
            case "KOREAN":
                return "ko";
            case "ITALIAN":
                return "it";
            case "DUTCH":
                return "nl";
            case "PORTUGUESE":
                return "pt";
            case "ROMANIAN":
                return "ro";
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
                return "English (en)";
            case "FRENCH":
                return "Français (fr)";
            case "GERMAN":
                return "Deutsch (de)";
            case "SPANISH":
                return "Español (es)";
            case "GREEK":
                return "ελληνικά (el)";
            case "HUNGARIAN":
                return "Magyar (hu)";
            case "HINDI":
                return "हिन्दी (hi)";
            case "HEBREW":
                return "Hebrew (he)";
            case "INDONESIAN":
                return "bahasa Indonesia (in)";
            case "KOREAN":
                return "한국어 (ko)";
            case "ITALIAN":
                return "Italiano (it)";
            case "DUTCH":
                return "Nederlands (nl)";
            case "PORTUGUESE":
                return "Português (pt)";
            case "ROMANIAN":
                return "Romanian (ro)";
            case "RUSSIAN":
                return "Русский язык (ru)";
            case "SWEDISH":
                return "Svenska (sv)";
            case "TAGALOG":
                return "Tagalog (tl)";
            case "TURKISH":
                return "Türkçe (tr)";
            case "VIETNAMESE":
                return "Tiếng Việt (vi)";
            case "CHINESE_SIMPLIFIED":
                return "簡體字 (zh-Hans)";
            case "CHINESE_TRADITIONAL":
                return "繁體字 (zh-Hant)";
            case "MALAY":
                return "Bahasa Melayu (ms)";
            default:
                return "DEFAULT";
        }
    }
}