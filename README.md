# Viros Winlator

Viros Winlator is an experimental Android gaming-focused fork/rework of Winlator. The goal is to preserve Winlator's mature Windows compatibility stack while replacing the normal container/desktop-first workflow with a console-like local game library:

`open app -> choose game -> play`

## Project status

Early architecture/bootstrap phase. The upstream Winlator repositories are being studied before invasive changes are made.

## Upstream

- Main project: https://github.com/brunodev85/winlator
- Android app source: https://github.com/brunodev85/winlator-app
- Upstream main commit selected for the initial baseline: `5949297d9dc83ad24ce3f5119fe382da7c899a78`
- App source is a submodule in upstream Winlator.

Winlator currently exposes the compatibility/runtime pieces we want to reuse: Wine, Box86/Box64, container management, DXVK/VKD3D/WineD3D, Turnip/VirGL/Vortek graphics backends, X server integration and input controls.

## Product direction

The default UI will become a touch-first game library. Containers, Wine configuration, graphics backends and compatibility settings will remain available internally and through an opt-in Advanced mode, but they will not dominate the normal user flow.

Planned core layers:

- game library + incremental scanner;
- Android Storage Access Framework folder persistence;
- game records and per-game settings;
- direct EXE launch through the Winlator runtime;
- automatic container/profile preparation;
- device profiling + AutoTune rules;
- PE icon extraction and custom covers;
- touch/gamepad profiles and a visual control editor;
- advanced compatibility settings as an escape hatch.

See `docs/ARCHITECTURE.md`, `docs/UPSTREAM.md` and `docs/ROADMAP.md` as they land.

## Licensing

This project is derived from and interoperates with Winlator and third-party components with their own licenses. Upstream copyright notices and license obligations must be preserved. No upstream attribution should be removed.
