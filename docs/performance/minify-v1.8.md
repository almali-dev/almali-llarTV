# LlarTV v1.8 minified with R8 (reference TV, paired)

**Status: evidence for the owner's decision on #104, not a decision.** A debug build of the v1.8 tree with R8 and
resource shrinking is 82.5% smaller, starts 155 ms faster (−11.7%) and holds 3.5 MB less memory (−6.5%) than the
published v1.8 on the reference TV, with Home return unchanged. The ADB surface works end to end on the TV. The
published artifact stays the unminified debug build until the owner decides (#104, step 3); adopting the minified
build would require re-baselining the budgets in [baseline-v1.0.md](baseline-v1.0.md#regression-budgets) in the same
change, because those budgets are relative to the same build variant.

## Setup

- Target: the reference TV, Xiaomi MiTV-MSSP3, Android 11/API30, armeabi-v7a, on 2026-11-04, with the owner's
  go-ahead and the owner's own `home.conf` (11 lines), written again after every install and byte-identical at the
  end.
- Baseline: [published v1.8 APK](https://github.com/almali-dev/almali-llartv/releases/download/v1.8/LlarTV.apk),
  4,231,049 bytes, SHA-256 `15a2a0d5ff5a60e83a660176f9d745594921f4c06600b7d1a7394a5aa74bf251`.
- Candidate: debug build of `main` `c0c176f` with `minifyEnabled`, `shrinkResources` and
  `proguard-android-optimize.txt` in `buildTypes.debug` and no extra rules (branch `perf/minify-measurement`, commit
  `47ba905`), clean build, 739,322 bytes, SHA-256
  `06aa68825139128c873419992c000bb139c9d2402c4e29caa4190132bed29e08`. Still a debug build: same signing certificate,
  installed in place with `install -r -d`, debuggable like every baseline since v1.0, so the pair isolates R8 and
  nothing else.
- Procedure, unchanged since v1.6: `scripts/measure.sh --serial <tv> --apk <apk> --runs 7` (one discarded warm-up,
  seven cold starts, seven returns from another app, three PSS samples) after a 20 s settle following each install;
  every round reinstalls its APK. Five pairs in an order-balanced sequence: min-a, v18-a, v18-b, min-b, min-c,
  v18-c, v18-d, min-d, min-e, v18-e. Script: `.state/minify-tv/run.sh`.

## Results

Paired comparisons use the mean of each version's five round medians, as in [v1.8](v1.8.md).

| Metric | v1.8 rounds a · b · c · d · e | Minified rounds a · b · c · d · e | Difference (mean of the round medians) | Paired allowance | Result |
| --- | --- | --- | --- | --- | --- |
| Cold start, median ms | 1526 · 1272 · 1268 · 1269 · 1282 | 1109 · 1170 · 1182 · 1187 · 1194 | −155 ms (−11.7%) | +150 ms and +15% | Inside, and faster |
| Home return, median ms | 196 · 187 · 192 · 180 · 202 | 206 · 185 · 189 · 180 · 174 | −5 ms (−2.4%) | +40 ms and +40% | Inside, noise |
| RAM, TOTAL PSS, median KB | 54,477 · 54,889 · 54,563 · 54,574 · 54,490 | 50,994 · 51,146 · 51,086 · 51,031 · 51,094 | −3,528 KB (−6.5%) | +4,096 KB and +10% | Inside, and smaller |
| APK bytes | 4,231,049 | 739,322 | −3,491,727 (−82.5%) | +5% | Inside, and smaller |

Pooled medians: cold start 1,285 ms against 1,180 ms (−105 ms, −8.2%) over 35 samples each, Home return 192 ms
against 184 ms (−8 ms, −4.2%) over 35 samples each, RAM 54,502 KB against 51,085 KB (−3,417 KB, −6.3%) over 15 samples each.

### The cold start

v18-a (1,541, 1,526, 1,519, 1,505, 1,656, 1,553, 1,364) is the first v1.8 round, right after the first switch from
the candidate back to v1.8, and the only round of either version with a median above 1,300 ms. Without it, v1.8's four round
medians average 1,273 ms and the difference is −104 ms (−8.2%), the same as the pooled reading, so that round
makes the paired figure larger but does not create the result. The worst minified round median (1,194 ms) is still
faster than the best v1.8 one (1,268 ms): the two versions do not overlap round by round. Sample by sample, 34 of
v1.8's 35 cold starts are at 1,250 ms or above; 33 of the candidate's 35 are at 1,240 ms or below. The other two
are 1,266 ms (min-d) and one 3,279 ms run in min-b, a single outlier that does not move that round's median
(1,170 ms).

### Home return

The difference is inside the roughly 30 ms of run-to-run noise in both readings. The first return of every round,
of either version, is the slow one (479–750 ms). One sample in min-a is 0 ms (704, 301,
206, 251, 191, **0**, 166): below the fastest of the other 69 samples (160 ms), so it is a measurement artifact,
not a return. It sits below that round's median, so the median of 206 ms holds for any real value up to 206 ms;
dropping the sample would raise it to 228.5 ms and make the paired difference −0.1 ms. Either way, no change.

### The RAM reading

Every minified round median is lower than every v1.8 one (51,146 KB worst against 54,477 KB best). The post-run
`dumpsys meminfo` of each round puts the whole difference in the code mapped from the APK: `.apk mmap` averages
4,348 KB for v1.8 and 978 KB for the candidate, and the App Summary's *Code* 6,286 KB against 2,939 KB, while the
Java heap, the native heap and the graphics stay within a few dozen KB of each other.

The absolute values differ between days: v1.8 holds about 54.5 MB here and about 70 MB in the
[v1.8 measurement](v1.8.md) (the night before, around midnight). Only the paired comparison, both versions on the same TV on the same day,
counts.

### Size and dex

Clean builds of the same tree (`.state/minify/`): the unminified base is 4,231,053 bytes and the candidate 739,322
bytes (−3,491,731, −82.5%). The dex goes from 3,981,248 to 557,212 bytes (one dex instead of three), the classes
from 2,281 to 444, the `Lkotlin/` references from 290 to 10, and `resources.arsc` from 87,960 to 33,340 bytes. The
fonts and the quote corpora are unchanged. `app/src/main` uses no reflection, and the four manifest components get
AGP's generated keep rules, so no hand-written rule was needed.

A trap for any future size figure: the first, incremental candidate build was 4,133,414 bytes, because AGP's
incremental debug packaging reused the old APK and left 3.4 MB of holes where the removed dex entries had been (its
entries add up to only 732,875 bytes). Every size figure must come from a clean build.

## The ADB surface on the TV

Checked on 2026-11-04, after the measurement:

- **Installer.** `scripts/install.sh 192.168.1.52 --apk <candidate>` installed it in place (pulled back,
  byte-identical), kept LlarTV as the Home and `.BingDream` as the screensaver. The secure screensaver settings, the
  HOME resolution and the disabled-package list are identical before and after (`sys.before`, `sys.final`).
- **`config.sh` write and read.** It wrote `clock=12` and Home showed "5:44 p. m."
  (`tv-1-home-clock12.png`); the value read back through SettingsReceiver. The owner's `home.conf` was restored
  byte-identical.
- **Screensaver.** SystemUI's Somnambulator started the dream: `mCurrentDreamName` =
  `io.github.mmarkimark.llartv.BingDream`, with the photo, its caption and the weather line (`tv-3-dream.png`).
- **Network code under R8.** Home shows the weather card, so the forecast was downloaded and parsed by the minified
  code.
- No LlarTV entry in the crash buffer, during the measurement or the check.

Not exercised on the TV: the screensaver music, the football card, and the settings panel (it was opened with the
D-pad on the `tv11` emulator only, see `odd/tasks/minify-measurement.md` L5).

The TV was left on the published v1.8 (pulled back, byte-identical), with the owner's `home.conf` byte-identical
and the system settings as before.

Evidence: `.state/minify-tv/` (`check.log`, `perf-*.md`, `meminfo-*.txt`, `sys.*`, the captures) and
`.state/minify/` (builds and dex listings).

## What the decision involves

Facts for #104, step 3, with no recommendation:

- The JVM tests run on unminified classes, so they cannot catch a breakage introduced by R8; only a check of the
  minified APK on a device can.
- Any future reflective code would need its own keep rules.
- Stack traces from users would be obfuscated and need the mapping file of that exact build. The mapping of this
  candidate (`.state/minify/mapping.txt`) is not kept anywhere durable.
- The artifact stays a debug build either way: minifying does not make it a release build, and it stays
  debuggable.
- Adopting it changes the build variant every budget is measured against, so the budgets in
  [baseline-v1.0.md](baseline-v1.0.md#regression-budgets) would be re-baselined in the same change.

## Still open

- The owner's decision on #104, step 3. Until then the published artifact is the unminified debug build.
- If the minified build is adopted: the screensaver music, the football card and the settings panel on the TV.

## Release

The owner chose the minified build as the release artifact (#104), and it shipped as v1.9. The published APK is
the debug build of `main` `6611da2` (tag `v1.9`), built in a clean detached worktree: 739,322 bytes, SHA-256
`b1a394f554d11ef2a78fcec7a3438b3936aa6ac778894e3e433f0700253320ec`, versionCode 10, versionName 1.9, with the
same certificate as v1.8. Its R8 mapping is published next to it as `LlarTV-mapping.txt` (1,488,629 bytes, SHA-256
`22285ecb27c50cd9470dee8b7a614d478ef8e9fa6f79c0578336f771a7963ca7`). Both were downloaded again from the public
URLs (`releases/download/v1.9/…`, and `releases/latest/download/LlarTV.apk` for the APK), with HTTP 200, and are
byte-identical to the built files. The budgets were re-baselined in the same change
([baseline](baseline-v1.0.md#rebaseline-for-the-minified-build-v19)).

The reference TV was then updated with `scripts/install.sh 192.168.1.52`. Without `--apk`, that is the download
path users take. The TV now runs the published v1.9 (pulled back, byte-identical), the owner's `home.conf` is
byte-identical, the screensaver settings and the Home resolution are unchanged, Home shows the weather card, the
screensaver starts, and the crash buffer has no LlarTV entry.
