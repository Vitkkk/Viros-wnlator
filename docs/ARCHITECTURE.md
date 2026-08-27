# Architecture

## Goal

Viros Winlator keeps Winlator's compatibility/runtime backend but changes the product model from **container-first** to **game-first**.

Default user flow:

`First setup -> choose local games folder -> scan -> library -> tap game -> run`

Advanced flow remains available for users who want the traditional Winlator controls.

## Upstream architecture observed

The initial baseline is Winlator 11.2-era source pinned by the main Winlator repository.

### Android frontend

The upstream Android application is Java + XML resources using AppCompat/Material components. The normal launcher activity is `com.winlator.MainActivity`. It currently switches between `ContainersFragment`, `ShortcutsFragment`, `InputControlsFragment` and `SettingsFragment` through a navigation drawer.

### Runtime launch path

`XServerDisplayActivity` is the important reusable execution boundary. It already accepts:

- `container_id`
- `shortcut_path` for a desktop shortcut
- `exec_path` for direct execution of a file

When an executable is launched through the upstream file manager, the file manager starts `XServerDisplayActivity` with `container_id` + `exec_path`. This means the new library does **not** need to automate the Windows desktop or Explorer. The library can resolve a game to a local executable and enter the existing runtime directly.

### Containers

`ContainerManager` stores containers under the Winlator rootfs home directory and activates one by switching the active user symlink. Container creation expands the existing container pattern and persists configuration into `.container`.

The existing `Container` model already exposes the compatibility settings the new product needs to drive automatically, including:

- screen size;
- environment variables;
- graphics driver/config;
- DX wrapper/config;
- audio driver/config;
- Win components;
- drive mappings;
- Wine version;
- Box64 preset;
- CPU lists;
- startup selection.

The fork should treat these as backend implementation details, not duplicate them in a second compatibility stack.

### PE metadata/icons

Upstream already contains `com.winlator.win32.PEParser` and uses it from `ContainerFileManagerFragment` to extract executable icons. The new library should reuse/extend this parser instead of introducing an unrelated PE library unless SAF/InputStream support requires it.

### Input

Upstream already contains the input-control manager, controls profiles, external controller handling, fullscreen controls editor and X-server input plumbing. The fork should add game-oriented profile selection and a friendlier editing entry point around these systems before considering replacement.

## Fork layers

The code should be separated into these layers.

### 1. Compatibility backend

Existing Winlator runtime code. Owns Wine/Box64/Box86, graphics backends, DX wrappers, X server, rootfs, containers and low-level input.

This layer should receive configuration from the launcher but should not depend on library UI classes.

### 2. Library domain

New package proposal:

`com.winlator.library`

Responsibilities:

- selected game tree URI;
- local-path capability check;
- scan state;
- game records;
- executable selection;
- custom names/covers;
- favorites/recent/play time;
- detected engine;
- selected input profile;
- selected performance profile;
- per-game overrides.

Suggested entities:

```text
Game
- id
- name
- folderUri
- executableUri
- executableRelativePath
- resolvedLocalPath
- customCoverUri
- detectedEngine
- containerId
- performanceMode
- inputProfileId
- favorite
- lastPlayed
- totalPlayTimeMs
- customArguments
- environmentVariables
```

Do not store the entire container configuration twice. Store only the launcher-level choice/override and let a profile resolver translate it into Winlator's existing container/shortcut fields.

### 3. Storage Access Framework

New package proposal:

`com.winlator.library.storage`

Responsibilities:

- launch `ACTION_OPEN_DOCUMENT_TREE`;
- call `takePersistableUriPermission`;
- persist the selected tree URI;
- enumerate documents without broad storage permissions where possible;
- resolve a local filesystem path when the provider is backed by local shared storage;
- reject or clearly mark non-local/cloud providers that cannot be executed safely by Wine.

Important: SAF URI persistence and Wine filesystem access are separate problems. The app may use SAF for user consent/discovery while the runtime still needs a stable local path or a deliberate mapping layer.

### 4. Scanner

New package proposal:

`com.winlator.library.scan`

Pipeline:

1. enumerate candidate directories and `.exe` files;
2. exclude obvious helpers/installers;
3. detect engine signatures;
4. score executable candidates;
5. choose automatically only above a confidence threshold;
6. ask the user when multiple candidates remain;
7. persist a fingerprint so later scans are incremental.

Initial negative-name rules:

- `unins*.exe`
- `uninstall*.exe`
- `setup*.exe`
- `installer*.exe`
- `crashhandler*.exe`
- `UnityCrashHandler*.exe`
- `vc_redist*.exe`
- `dxsetup*.exe`
- `updater*.exe`

Scoring signals should include file size, directory depth, folder-name similarity, neighboring engine files and previous user choice.

### 5. Engine detector

New package proposal:

`com.winlator.library.detect`

File-signature based detection. Initial signatures:

- Unity: `UnityPlayer.dll`, `*_Data/`
- Unreal: `Engine/`, `Binaries/`, `Content/`
- Godot: `.pck`
- GameMaker: `data.win`
- Ren'Py: `renpy/`, `.rpa`, common launcher layout
- RPG Maker: engine-specific runtime files
- Electron: `resources/app.asar`, Electron runtime files
- generic Windows executable fallback

Detector results are advisory, not truth.

### 6. Launcher/runtime bridge

New package proposal:

`com.winlator.launcher`

Main class concept:

`GameLaunchCoordinator`

Responsibilities:

1. ensure RootFS is installed;
2. load/create the automatic container;
3. resolve the effective compatibility profile;
4. ensure the selected game folder is mapped/visible;
5. apply per-game overrides;
6. select the input profile;
7. start `XServerDisplayActivity` with `container_id` and `exec_path`;
8. update recent/playtime state.

The normal path must not open the desktop or file manager.

### 7. AutoTune

New package proposal:

`com.winlator.autotune`

Objects:

```text
DeviceProfile
HardwareRule
PerformanceProfile
CompatibilityRuleEngine
ResolvedRuntimeProfile
```

`DeviceProfile` should collect SoC/CPU/GPU/RAM/Android/Vulkan/OpenGL/resolution/refresh-rate facts. Rules must be data-driven and versioned. Do not invent unsupported driver combinations; every rule should map to graphics/DX/Wine/Box64 options that exist in the pinned upstream.

User-facing modes:

- Automatic
- Performance
- Balanced
- Quality

### 8. UI

Proposed launcher activity:

`LibraryActivity`

The existing `MainActivity` should remain reachable as **Advanced Winlator** during early development instead of being deleted.

Primary screens:

- FirstSetup
- Library
- GameDetails
- GameSettings
- ControllerEditor entry point
- Settings
- Advanced Settings

Home should prioritize large cover cards, recent/favorite sections and fast launch. Technical compatibility vocabulary stays out of the default home.

## Container strategy

For the first working version, use one automatically managed shared container unless upstream behavior or game isolation tests show a blocker. This minimizes prefix creation time and storage use.

The data model should already allow `containerId` per game so a later **isolated per-game container** option can be added without migration pain.

Never silently destroy a container when a game is removed from the library.

## First implementation slice

The first code slice should prove the product loop before AutoTune becomes complex:

1. Library launcher activity.
2. SAF tree picker + persisted permission.
3. Local tree path resolution.
4. Incremental scanner with executable filtering.
5. Simple persistent game database/store.
6. Grid library with fallback art.
7. Automatic shared container creation.
8. Direct launch through `XServerDisplayActivity(container_id, exec_path)`.
9. "Open Advanced Winlator" escape hatch.
10. CI-built debug APK.

After this works on-device, add PE covers, engine detection, performance rules, advanced per-game overrides and input recommendation logic.
