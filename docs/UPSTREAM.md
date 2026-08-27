# Upstream baseline

## Repositories

Viros Winlator is based on the Winlator project maintained by `brunodev85`.

- Main Winlator repository: `brunodev85/winlator`
- Android application source: `brunodev85/winlator-app`
- Other upstream submodules include `brunodev85/vortek` and `brunodev85/gladio`.

The main Winlator repository pins the Android application through a Git submodule.

## Initial pinned baseline

Main repository commit:

`5949297d9dc83ad24ce3f5119fe382da7c899a78`

At that baseline, the `app` submodule points to:

`c03f6ab558c6f94cbac6ec0c791b12f3428fbdf6`

The fork should keep these values explicit so upstream updates are deliberate and reviewable instead of silently changing the compatibility backend.

## Licensing

The Winlator app repository includes GNU Lesser General Public License v2.1 text. The project also bundles or integrates components with independent licenses, including Wine, Box86/Box64, Mesa/Turnip/Zink/VirGL, DXVK, VKD3D and other compatibility components.

Rules for this fork:

1. Keep upstream copyright/license notices intact.
2. Do not remove attribution from copied or modified upstream files.
3. Record the upstream commit used by every release.
4. Record added third-party components and their licenses.
5. Preserve source-availability and redistribution obligations for components included in APK/release artifacts.
6. Mark substantial fork-specific modifications clearly.

## Upstream update policy

Do not track upstream `main` directly in release builds.

Preferred process:

1. choose a known-good upstream Winlator commit;
2. record its app/submodule SHAs here;
3. build the unmodified baseline once;
4. apply Viros changes;
5. run launcher/runtime smoke tests;
6. update compatibility rules only after confirming the upstream options still exist;
7. document breaking changes in a migration note.

## Ownership boundaries

### Upstream-derived backend

Keep changes minimal where possible in:

- rootfs/container lifecycle;
- Wine launch/runtime;
- Box64/Box86;
- X server;
- graphics backends;
- DX wrappers;
- audio backends;
- low-level input plumbing.

### Fork-owned product layer

Prefer new isolated packages for:

- game library;
- scanner;
- engine detection;
- launcher orchestration;
- AutoTune rules;
- cover metadata;
- game-oriented UI;
- per-game friendly settings.

This separation should make future upstream rebases easier.
