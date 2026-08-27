# Roadmap

This roadmap is ordered by **testable product slices**, not by subsystem prestige. The first goal is a debug APK that demonstrates `pick folder -> library -> tap EXE -> game runtime` while keeping Advanced Winlator available.

## Milestone 0 — Baseline and reproducible build

- [x] Record upstream repositories and pinned SHAs.
- [x] Document architecture boundaries.
- [ ] Add a reproducible upstream bootstrap script.
- [ ] Add GitHub Actions debug APK build.
- [ ] Preserve/copy upstream license files in the assembled source tree.
- [ ] Produce one unmodified baseline APK before launcher changes.

Acceptance: CI can build the pinned Winlator source without Viros product changes.

## Milestone 1 — Console-library MVP

- [ ] `LibraryActivity` becomes the normal launcher.
- [ ] Existing `MainActivity` remains reachable as Advanced Winlator.
- [ ] First-run folder picker uses `ACTION_OPEN_DOCUMENT_TREE`.
- [ ] Persist URI permission and selected tree URI.
- [ ] Verify selected provider maps to local executable storage.
- [ ] Scan for `.exe` files off the UI thread.
- [ ] Exclude obvious installers/uninstallers/crash handlers/updaters.
- [ ] Ask the user when a folder contains multiple strong executable candidates.
- [ ] Persist game records.
- [ ] Show a responsive portrait/landscape library grid.
- [ ] Create/reuse one automatic shared container.
- [ ] Launch directly through `XServerDisplayActivity` with `container_id` + `exec_path`.
- [ ] Manual Refresh Library action.
- [ ] Friendly launch failure screen with retry/config/log actions.

Acceptance: a locally stored portable Windows game can be discovered and started without opening the Winlator desktop/file manager.

## Milestone 2 — Library quality

- [ ] PE version metadata for better game names.
- [ ] Reuse/extend upstream `PEParser` for executable art.
- [ ] Fallback images: `cover.*`, `icon.*`, `folder.jpg`, `poster.png`.
- [ ] Custom cover picker.
- [ ] Favorites.
- [ ] Recent games.
- [ ] Playtime accounting.
- [ ] Search and sorting.
- [ ] Incremental fingerprints so app startup does not re-scan the full tree.
- [ ] Background scan progress without blocking library use.

Acceptance: hundreds of games can be managed without slow cold starts or technical UI clutter.

## Milestone 3 — Game detection

- [ ] `GameEngineDetector`.
- [ ] Unity signatures.
- [ ] Unreal signatures.
- [ ] Godot signatures.
- [ ] GameMaker signatures.
- [ ] Ren'Py signatures.
- [ ] RPG Maker signatures.
- [ ] Electron signatures.
- [ ] Executable scoring model uses engine evidence.
- [ ] Persist and display detected engine only where useful.

Acceptance: launcher chooses the correct main EXE automatically in common portable-game layouts with an explicit correction path.

## Milestone 4 — DeviceProfile + AutoTune v1

- [ ] `DeviceProfile` collection.
- [ ] GPU/vendor detection.
- [ ] Vulkan version/extensions.
- [ ] RAM/CPU/Android/resolution/refresh data.
- [ ] Data-driven `CompatibilityRuleEngine`.
- [ ] User modes: Automatic / Performance / Balanced / Quality.
- [ ] Rules only reference backends/settings verified in the pinned Winlator source.
- [ ] Per-game override layer.
- [ ] Explainable resolved profile in Advanced mode/logs.

Acceptance: default launch configuration is selected automatically and can always be overridden.

## Milestone 5 — Controls UX

- [ ] Game-oriented input profile selector.
- [ ] Reuse existing Winlator controls profile backend.
- [ ] Touch presets: keyboard+mouse, WASD, platformer, FPS, RPG, rhythm/FNF, gamepad.
- [ ] Simplified entry into the existing fullscreen control editor.
- [ ] Physical controller detection/status.
- [ ] Prefer native gamepad path when the game supports it.
- [ ] Per-game profile binding.

Acceptance: user can start with a sensible layout and edit it entirely by touch.

## Milestone 6 — InputDetectionEngine experimental

- [ ] Static scan of `.ini`, `.cfg`, `.json`, `.xml` and engine-specific input files.
- [ ] Recommendation confidence score.
- [ ] Never apply a detected layout silently.
- [ ] Research Wine-side runtime profiling hooks for Win32 keyboard/raw input/DirectInput/XInput.
- [ ] Keep runtime profiler behind an experimental flag until stable.

Acceptance: launcher can recommend, not force, a useful control layout for selected games.

## Milestone 7 — Per-game isolation and advanced compatibility

- [ ] Shared vs isolated per-game container choice.
- [ ] Migration/clone helper.
- [ ] Friendly per-game resolution/FPS/fullscreen/arguments UI.
- [ ] Advanced screen exposing supported upstream Wine/Box64/graphics/DX/audio/env settings.
- [ ] Per-game logs and reset-to-auto.

Acceptance: advanced users retain Winlator-level control without exposing that complexity to new users.

## Non-goals for the first release

- online store/account system;
- downloading pirated games or bypassing DRM;
- cloud providers as executable game storage;
- automatic destructive deletion of game files;
- fragile Wine input API hooks before a safe profiling design exists.
