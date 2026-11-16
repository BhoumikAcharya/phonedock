# Handover for coding agents

Read this first if you are an AI coding agent (Claude Code, Codex, Cursor,
Copilot or similar) picking up this project in a new session. It records the
state of the project, how to work on it, and the traps that cost time before.
The human-facing guide is [README.md](README.md); do not duplicate it here.

## 1. What this project is

A Xiaomi Redmi Note 5 / Redmi 5 Plus (codename `vince`) was reflashed from MIUI
to Evolution X 10.9 (Android 15) on 2026-11-17 and now runs **Dock**, a custom
launcher in [dock/](dock), as its home screen. The phone lives plugged in on a
desk in landscape as a clock, pomodoro timer, YouTube Music controller and
app tile grid.

Current state: everything described in the README works and is installed on
the phone. The ROM flashing is finished and must not be redone.

## 2. The phone

| Fact | Value |
| --- | --- |
| Codename | `vince`. Never flash anything built for `whyred`. |
| ROM | Evolution X 10.9, Android 15, build `lineage_vince-userdebug 15 BP1A.250505.005` |
| Bootloader | Unlocked. **Never run `fastboot oem lock` or `fastboot flashing lock`.** |
| RAM | 4 GB, memory is tight. The low-memory killer is aggressive on this ROM. |
| Screen | 1080 x 2160, density 400, so about 864 x 432 dp in landscape |
| USB debugging | On. If `adb devices` says `unauthorized`, the user must accept the prompt on the phone. |
| Lock screen | Swipe only, no PIN, unless the user has changed that since |
| Known broken | Fingerprint. The vendor fingerprint HAL segfaults on session open. ROM bug, user chose to leave it. |

Tools on the laptop (Windows, Git Bash):

| Tool | Path |
| --- | --- |
| adb / fastboot | `C:\platform-tools` |
| JDK 17 | `C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot` |
| Android SDK | `C:\android-sdk` (platforms;android-35, build-tools 34.0.0 and 35.0.0) |
| Gradle 8.9 | `C:\gradle\gradle-8.9` |
| Working copy of the app | `C:\Users\DELL\Documents\Redmi\dock` is the original; this repo's `dock/` is a copy. Treat **this repo** as the source of truth from now on and tell the user if the two have drifted. |

## 3. Build, install, run

```bash
export JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.21.21-hotspot"
export PATH="$JAVA_HOME/bin:/c/gradle/gradle-8.9/bin:$PATH"
export ANDROID_HOME=/c/android-sdk
cd dock
gradle assembleDebug --no-daemon -q
/c/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
/c/platform-tools/adb shell am start -n app.vince.dock/.MainActivity
```

A build takes about a minute. `dock/local.properties` is gitignored; copy
`local.properties.example` if it is missing. There is no Gradle wrapper on
purpose, Gradle is run from its install directory.

Permissions already granted on the phone and not needed again unless the app
is uninstalled (an `install -r` keeps them):

```bash
adb shell cmd package set-home-activity app.vince.dock/.MainActivity
adb shell cmd role add-role-holder android.app.role.HOME app.vince.dock
adb shell cmd notification allow_listener app.vince.dock/.MediaListener
adb shell dpm set-active-admin app.vince.dock/.DockAdmin
```

## 4. Code map

Package `app.vince.dock`, Kotlin, classic Views (no Compose), no DI, no tests.
Dependencies are only AndroidX core, appcompat, constraintlayout, viewpager2,
recyclerview and fragment.

| File | Role |
| --- | --- |
| `MainActivity.kt` | Single activity. ViewPager2 with three fragments: `PAGE_POMODORO` (0), `PAGE_HOME` (1), `PAGE_MUSIC` (2). Hosts the full-screen flip clock overlay, keeps the screen on, hides system bars, handles the Lock button via `DevicePolicyManager.lockNow()`. Back and Home always return to the home page. |
| `HomeFragment.kt` | Time, date, grid of pinned tiles (5 columns, monochrome), All apps button. Tapping the time calls `MainActivity.showFlipClock()`. |
| `FlipDigit.kt` | Custom view for one flip-clock digit. Animates with `rotationX`. |
| `PomodoroFragment.kt` / `PomodoroTimer.kt` | UI and a process-wide singleton timer so state survives page swipes. Focus and break lengths are persisted in SharedPreferences `dock` under `focus_min` / `break_min`. |
| `MusicFragment.kt` / `MediaHub.kt` / `MediaListener.kt` | `MediaHub` is a singleton over `MediaSessionManager`, prefers the YouTube Music session, exposes controller, metadata and playback state. `MediaListener` is an empty `NotificationListenerService` that exists only to hold notification access. The fragment ticks once a second to update progress and the play/pause glyph. |
| `Apps.kt` | `AppEntry` (an app or a pinned shortcut), `Apps` (launchable apps, pin list, launching) and `AppAdapter` (RecyclerView adapter with optional monochrome filter and large layout). Pins are stored in SharedPreferences `dock` under key `pins` as a comma-separated list. An app pin is its package name; a shortcut pin is `sc:<package>/<shortcutId>`. |
| `AppsDialog.kt` | All-apps grid dialog. Tap launches, long press toggles pin. |
| `ConfirmPinActivity.kt` | Handles `android.content.pm.action.CONFIRM_PIN_SHORTCUT`. Its presence is what makes Chrome offer "Install app" for PWAs. |
| `DockAdmin.kt` | Empty `DeviceAdminReceiver` for `lockNow()`. Policy XML in `res/xml/device_admin.xml`. |
| `res/layout/page_*.xml` | One layout per page. `item_app.xml` (dialog) and `item_app_large.xml` (home) are the tiles. |
| `res/values/colors.xml`, `themes.xml` | Dark palette: `bg`, `card`, `fg`, `muted`, `accent`. Button style `Dock.Button`, glyph variant `Dock.Button.Glyph`. |

## 5. Conventions and preferences the user has expressed

- Landscape only. Big, quiet, dark UI. The user said the music page "looks
  great", so keep its look unless asked.
- Home shows only pinned apps as large black-and-white tiles, All apps bottom
  right, time and date top left. Tapping the time opens the flip clock.
- Pomodoro has editable focus and break lengths.
- Play and pause are text glyphs, not emoji. The pause glyph is `\u275a\u275a`
  because `\u23f8` renders as a colour emoji on this ROM.
- The user did not want the ROM's face-unlock service (`co.aospa.sense`) or
  the Pixel Launcher disabled. Do not disable system packages without asking.
- Before any command that writes to or erases the phone beyond an
  `adb install`, show the command and wait for confirmation. Never relock the
  bootloader, never flash partitions, never use unofficial Xiaomi tools.
- The user cannot be assumed to be watching the phone. When a step needs a
  physical button or a tap on the phone, say exactly what to press and wait.

## 6. Gotchas that cost time before

- **Screenshots over adb are black** when the screen is off or the keyguard is
  up. Always run `adb shell input keyevent KEYCODE_WAKEUP` then
  `adb shell wm dismiss-keyguard` before `adb exec-out screencap -p`. The lock
  screen times out after 10 seconds, so capture promptly.
- **The phone may be in landscape or portrait** depending on how it is lying.
  Blind taps by coordinate often hit the wrong thing. Prefer
  `adb exec-out uiautomator dump /dev/tty` to read on-screen text and bounds,
  then tap the centre of the bounds. `adb shell input keyevent KEYCODE_MENU`
  opens Chrome's menu without needing coordinates.
- **Chrome caches whether the launcher supports pinned shortcuts** for the
  life of its process. After changing anything about the HOME app, run
  `adb shell am force-stop com.android.chrome` before testing PWA install.
- **YouTube Music releases its media session** a while after being paused.
  "Nothing is playing" on the music page is then correct, not a bug.
- **Memory pressure looks like crashes.** Check
  `adb logcat -d -s lowmemorykiller:*` and `adb shell dumpsys dropbox` before
  assuming an app crashed. There were no real Chrome or Play Store crashes.
- **`sdkmanager.bat` under Git Bash** mangles `--sdk_root=C:\android-sdk` and
  installs into a nested folder. Run it from PowerShell or cmd, or move the
  result up afterwards.
- **Bash heredocs with large XML** tripped on quoting once. The Write tool is
  more reliable for multi-file scaffolding.
- `MediaHub.start()` once recursed into `refresh()` and crashed the app on
  first launch. The structure now is `start()` -> `refresh()` ->
  `ensureStarted()`; keep it that way.
- The dock's pin list lives in app-private storage. To edit it without the UI:
  `adb shell run-as app.vince.dock cat shared_prefs/dock.xml`.

## 7. Ideas the user may ask for next

Nothing is committed to, these are natural follow-ups:

- A night mode or scheduled dimming (the screen currently stays at full
  brightness while the dock is showing).
- Weather or calendar on the home page.
- Reordering pinned tiles by drag.
- Volume controls on the music page.
- A release build with a signing key instead of the debug APK.
- A licence file for the repo (the user has not chosen one).

## 8. Checklist for a new session

1. `adb devices` shows the phone as `device`. If not, ask the user to plug it
   in and accept the prompt.
2. Build once with the commands in section 3 to confirm the toolchain still
   works.
3. Make changes in this repo's `dock/`, not in the old working copy.
4. After installing, wake the phone, dismiss the keyguard, and screenshot each
   affected page to verify. Check `adb logcat -d -s AndroidRuntime:E` for
   crashes.
5. Commit with a short message. Push only when the user asks.
