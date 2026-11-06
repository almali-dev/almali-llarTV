package io.github.mmarkimark.llartv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/** The app ships the Almali LlarTV launcher icon and Android TV banner from the Almali brand kit. */
public class AppIconTest {
    private static final File MAIN = new File("src/main");
    private static final File RES = new File(MAIN, "res");

    private static String manifest() throws IOException {
        return new String(Files.readAllBytes(new File(MAIN, "AndroidManifest.xml").toPath()), StandardCharsets.UTF_8);
    }

    /** Width and height from a PNG's IHDR chunk. */
    private static int[] pngSize(File png) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(png))) {
            in.skipBytes(16);
            return new int[] {in.readInt(), in.readInt()};
        }
    }

    @Test public void applicationUsesTheLauncherIconAndTheTvBanner() throws IOException {
        String m = manifest();
        assertTrue(m.contains("android:icon=\"@mipmap/ic_launcher\""));
        assertTrue(m.contains("android:banner=\"@drawable/banner\""));
        assertTrue(!m.contains("sym_def_app_icon"));
    }

    @Test public void bannerIsThe320x180AndroidTvSize() throws IOException {
        int[] size = pngSize(new File(RES, "drawable-xhdpi/banner.png"));
        assertEquals(320, size[0]);
        assertEquals(180, size[1]);
    }

    @Test public void launcherIconExistsAtEveryDensity() throws IOException {
        String[] dpis = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        int[] px = {48, 72, 96, 144, 192};
        for (int i = 0; i < dpis.length; i++) {
            int[] icon = pngSize(new File(RES, "mipmap-" + dpis[i] + "/ic_launcher.png"));
            assertEquals(dpis[i], px[i], icon[0]);
            assertEquals(dpis[i], px[i], icon[1]);
            int[] fg = pngSize(new File(RES, "mipmap-" + dpis[i] + "/ic_launcher_foreground.png"));
            assertEquals(dpis[i] + " foreground", Math.round(px[i] * 108f / 48f), fg[0]);
        }
    }

    @Test public void adaptiveIconLayersTheForegroundOnTheBrandInk() throws IOException {
        String adaptive = new String(Files.readAllBytes(new File(RES, "mipmap-anydpi-v26/ic_launcher.xml").toPath()),
                StandardCharsets.UTF_8);
        assertTrue(adaptive.contains("@color/ic_launcher_background"));
        assertTrue(adaptive.contains("@mipmap/ic_launcher_foreground"));
        String colors = new String(Files.readAllBytes(new File(RES, "values/ic_launcher_background.xml").toPath()),
                StandardCharsets.UTF_8);
        assertTrue(colors.contains("<color name=\"ic_launcher_background\">#17191F</color>"));
    }
}
