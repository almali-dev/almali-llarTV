package io.github.mmarkimark.llartv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Public installer probes run only copied scripts against mock adb, curl and sleep commands. */
public class InstallScriptTest {
    private static final String PKG = "io.github.mmarkimark.llartv";
    private static final String HOME = PKG + "/.HomeActivity";
    private static final String DREAM = PKG + "/.BingDream";
    private static final String RELEASE = "https://github.com/almali-dev/almali-llartv/releases/latest/download/LlarTV.apk";

    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test public void defaultInstallDownloadsTheCanonicalArtifactAndKeepsAndroidIdentifiers() throws Exception {
        Fixture fixture = fixture("default");
        Result result = fixture.run("emulator-default");

        assertEquals(diagnostics(result), 0, result.exit);
        assertEquals(diagnostics(result), "", result.error);
        assertTrue(result.output, result.output.contains("Downloading the latest Almali LlarTV"));
        assertTrue(result.output, result.output.contains("Done. Press Home on the remote"));
        assertTrue(result.calls, result.calls.contains("adb -s emulator-default get-state"));
        assertTrue(result.calls, result.calls.contains("curl -fsSL -o " + fixture.apk + " " + RELEASE));
        assertTrue(result.calls, result.calls.contains("adb -s emulator-default install -r " + fixture.apk));
        assertTrue(result.calls, result.calls.contains("adb -s emulator-default shell am start -W -n " + HOME));
        assertTrue(result.calls, result.calls.contains("adb -s emulator-default shell cmd package set-home-activity " + HOME));
        assertTrue(result.calls, result.calls.contains("adb -s emulator-default shell settings put secure screensaver_components " + DREAM));
        assertTrue("downloaded artifact", fixture.apk.isFile());
        assertTrue("isolated state", new File(fixture.root, ".state/emulator-default/previous-home").isFile());
    }

    @Test public void suppliedApkSkipsCurlAndNoScreensaverSkipsOnlyScreensaverSetup() throws Exception {
        Fixture fixture = fixture("custom");
        File apk = new File(fixture.root, "already-built.apk");
        Files.write(apk.toPath(), "apk".getBytes(StandardCharsets.UTF_8));
        Result result = fixture.run("emulator-custom", "--apk", apk.getAbsolutePath(), "--no-screensaver");

        assertEquals(diagnostics(result), 0, result.exit);
        assertEquals(diagnostics(result), "", result.error);
        assertFalse(result.output, result.output.contains("Downloading the latest"));
        assertFalse(result.calls, result.calls.contains("curl "));
        assertTrue(result.calls, result.calls.contains("adb -s emulator-custom install -r " + apk));
        assertTrue(result.calls, result.calls.contains("adb -s emulator-custom shell cmd package set-home-activity " + HOME));
        assertFalse(result.calls, result.calls.contains("screensaver_components " + DREAM));
        assertTrue("isolated state", new File(fixture.root, ".state/emulator-custom/previous-home").isFile());
    }

    private Fixture fixture(String name) throws Exception {
        File root = tmp.newFolder(name);
        File scripts = new File(root, "scripts");
        File bin = new File(root, "bin");
        assertTrue(scripts.mkdir());
        assertTrue(bin.mkdir());
        Files.copy(projectFile("scripts/install.sh").toPath(), new File(scripts, "install.sh").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(projectFile("scripts/lib.sh").toPath(), new File(scripts, "lib.sh").toPath(), StandardCopyOption.REPLACE_EXISTING);
        File calls = new File(root, "calls");
        write(new File(bin, "adb"), "#!/bin/bash\n{ printf 'adb'; printf ' %s' \"$@\"; printf '\\n'; } >> \"$CALLS\"\ncase \" $* \" in\n  *' get-state'*) echo device ;;\n  *' shell getprop ro.product.model'*) echo MockTV ;;\n  *' shell getprop ro.build.version.release'*) echo 11 ;;\n  *' shell getprop ro.build.version.sdk'*) echo 30 ;;\n  *' shell pm path '*) echo package:$PKG ;;\n  *' install -r '*) echo Success ;;\n  *' shell dumpsys activity activities'*) echo 'mResumedActivity " + HOME + "' ;;\n  *' shell cmd package resolve-activity '*) echo '" + HOME + "' ;;\nesac\n");
        write(new File(bin, "curl"), "#!/bin/bash\n{ printf 'curl'; printf ' %s' \"$@\"; printf '\\n'; } >> \"$CALLS\"\nout=\nwhile [ $# -gt 0 ]; do\n  if [ \"$1\" = -o ]; then out=\"$2\"; shift 2; else shift; fi\ndone\nprintf apk > \"$out\"\n");
        write(new File(bin, "sleep"), "#!/bin/bash\n{ printf 'sleep'; printf ' %s' \"$@\"; printf '\\n'; } >> \"$CALLS\"\n");
        return new Fixture(root, bin, calls);
    }

    private static String diagnostics(Result result) {
        return result.output + "\n-- stderr --\n" + result.error + "\n-- calls --\n" + result.calls;
    }

    private static File projectFile(String path) {
        for (File candidate : new File[]{new File(path), new File("..", path), new File("app/..", path)}) {
            if (candidate.isFile()) return candidate;
        }
        throw new AssertionError(path + " not found");
    }

    private static void write(File file, String content) throws Exception {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        assertTrue(file.setExecutable(true));
    }

    private static String read(InputStream stream) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int count; (count = stream.read(buffer)) >= 0;) bytes.write(buffer, 0, count);
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    private static final class Fixture {
        final File root, bin, calls, apk;

        Fixture(File root, File bin, File calls) {
            this.root = root;
            this.bin = bin;
            this.calls = calls;
            this.apk = new File(root, ".state/emulator-default/LlarTV.apk");
        }

        Result run(String... arguments) throws Exception {
            ProcessBuilder process = new ProcessBuilder();
            process.command().add("bash");
            process.command().add("scripts/install.sh");
            for (String argument : arguments) process.command().add(argument);
            process.directory(root);
            process.environment().put("CALLS", calls.getAbsolutePath());
            process.environment().put("PATH", bin.getAbsolutePath() + File.pathSeparator + process.environment().get("PATH"));
            Process child = process.start();
            String output = read(child.getInputStream());
            String error = read(child.getErrorStream());
            return new Result(child.waitFor(), output, error, calls.isFile()
                    ? new String(Files.readAllBytes(calls.toPath()), StandardCharsets.UTF_8) : "");
        }
    }

    private static final class Result {
        final int exit;
        final String output, error, calls;

        Result(int exit, String output, String error, String calls) {
            this.exit = exit;
            this.output = output;
            this.error = error;
            this.calls = calls;
        }
    }
}
