package io.github.mmarkimark.llartv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Public identity surfaces must agree on the Almali name and the canonical repository. */
public class BrandingTest {
    private static final String BRAND = "Almali LlarTV";
    private static final String REPO = "almali-dev/almali-llartv";

    @Test public void visibleAndroidNameAndLocalizedSettingsUseTheAlmaliBrand() throws Exception {
        String catalan = text("src/main/res/values/strings.xml");
        String spanish = text("src/main/res/values-es/strings.xml");
        String english = text("src/main/res/values-en/strings.xml");

        assertTrue(catalan.contains("<string name=\"app_name\" translatable=\"false\">" + BRAND + "</string>"));
        for (String localized : new String[]{catalan, spanish, english}) {
            assertResourceContains(localized, "settings_title", BRAND);
            assertResourceContains(localized, "screen_not_selected", BRAND);
            assertResourceContains(localized, "reset_scope", BRAND);
        }
    }

    @Test public void installerAndRuntimeReleaseAssetsUseTheCanonicalRepository() throws Exception {
        assertTrue(text("scripts/lib.sh").contains("REPO=" + REPO));
        assertEquals("https://github.com/" + REPO + "/releases/download/v1.4/music-macleod-chill-wave-v1.mp3",
                MusicTrack.at(1).url);
        assertEquals("https://github.com/" + REPO + "/releases/download/v1.4/music-macleod-dream-culture-v1.mp3",
                MusicTrack.at(2).url);
        assertEquals(MusicCache.URL, MusicTrack.at(0).url);
    }

    private static void assertResourceContains(String xml, String name, String expected) {
        int start = xml.indexOf("<string name=\"" + name + "\"");
        int end = xml.indexOf("</string>", start);
        assertTrue(name + " is missing " + BRAND, start >= 0 && end >= start && xml.substring(start, end).contains(expected));
    }

    private static String text(String path) throws Exception {
        File file = new File(path);
        if (!file.isFile()) file = new File("app", path);
        if (!file.isFile()) file = new File("..", path);
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
