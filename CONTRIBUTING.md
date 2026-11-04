# Contributing to Almali LlarTV

Almali LlarTV is a small personal project. These guidelines keep issues and changes easy to follow.

## Reporting issues

1. Search the [existing issues](https://github.com/almali-dev/almali-llartv/issues?q=is%3Aissue), open and closed,
   before opening a new one.
2. Open a new issue with one of the forms:
   - **Bug report** for something that does not work as expected.
   - **Feature request** for a new feature or an improvement.
3. Keep one problem or proposal per issue, and use a Conventional Commit style title (`fix: ...`, `feat: ...`).
4. For bugs, always include the Almali LlarTV version, the device model, the Android version and the steps to
   reproduce. For crashes, attach the output of:

   ```sh
   adb logcat -d | grep -i llartv
   ```

Do not include personal data (Wi-Fi names, IP addresses, account details) in issues, screenshots or logs.

## Making changes

- Branch from `main` with a descriptive name, for example `feat/weather-forecast` or `fix/clock-clipping`.
- Use [Conventional Commits](https://www.conventionalcommits.org/) for commit messages
  (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`).
- Keep commits focused, and include tests and documentation with the behaviour they describe.
- Reference the issue in the pull request description (`Closes #12`).

## Development checks

Before opening a pull request, run:

```sh
./gradlew testDebugUnitTest assembleDebug
shellcheck -x -P SCRIPTDIR/.. scripts/*.sh   # when you change the scripts
```

CI runs the same checks on every push and pull request. The workflow pins its actions to commit SHAs, and
Dependabot opens the pull requests that move them forward.

When a change affects what is shown on screen, test it on an Android TV device or emulator and attach a
screenshot to the pull request.

## Releases

Releases are tagged `vMAJOR.MINOR` on `main`, recorded in [CHANGELOG.md](CHANGELOG.md) and published on the
[Releases](https://github.com/almali-dev/almali-llartv/releases) page with the built APK attached. Update
`versionName` and `versionCode` in `app/build.gradle` for each release.

Since v1.9 the published APK is shrunk with R8 (#104). Build the release in a clean checkout of the tagged commit:
AGP's incremental packaging can leave megabytes of unused space in an APK built over an older one, so a size is
only meaningful from a clean build. Attach two assets: `LlarTV.apk` and its `LlarTV-mapping.txt`
(`app/build/outputs/mapping/debug/mapping.txt` from the same build), which turns an obfuscated stack trace back
into class and method names. The JVM tests run on unminified classes and cannot see an R8 breakage, so check the
installer, `scripts/config.sh` and the screensaver on a device before each release.
