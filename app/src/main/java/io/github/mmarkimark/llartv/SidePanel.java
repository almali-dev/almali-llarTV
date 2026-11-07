package io.github.mmarkimark.llartv;

/** The geometry of the settings side panel, kept free of Android so the JVM tests can pin it. */
final class SidePanel {
    private SidePanel() {}

    /** The Android TV settings panel takes the right 3/8 of the screen (720 of 1920 px on the reference TV). */
    static int width(int screenWidth) { return Math.round(screenWidth * 3f / 8f); }

    /** The settings row whose value list is a list of apps to unhide, not a list of values. */
    static final String HIDDEN_KEY = "hidden";

    /** The settings panel that lists the cities of a search: places to choose from, not the values of a row. */
    static final String CITIES_KEY = "cities";

    /**
     * The option a value list marks as the saved one, or -1 when the list has no saved value. The focus moves
     * while the list is open, so without this mark nothing on screen says which value is in use (#94).
     *
     * @param key    the home.conf key of the row, null for a row that writes no value
     * @param action whether the row is an action (its choices do something instead of being a saved value)
     * @param selected the option the row starts on
     */
    static int markedOption(String key, boolean action, int selected) {
        if (action) return -1;                   // the reset confirmation starts on Cancel: a choice, not a value
        if (HIDDEN_KEY.equals(key)) return -1;   // the hidden apps: every entry is an app to unhide
        if (CITIES_KEY.equals(key)) return -1;   // the cities of a search: every entry is a place to choose
        return selected;
    }

    /**
     * The scroll position that shows a row between top and bottom inside a view of the given height scrolled to
     * scrollY: unchanged when the row is already visible, otherwise the smallest move that shows it, top first.
     */
    static int scrollFor(int top, int bottom, int scrollY, int height) {
        if (top < scrollY || bottom - top > height) return top;
        if (bottom > scrollY + height) return bottom - height;
        return scrollY;
    }
}
