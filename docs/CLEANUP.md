# Cleanup scope

2.0 retains favorites/menu/form mappings across recompositions and avoids creating glow shaders for unfocused settings controls. Earlier fixed-colour/duplicate rim rendering was replaced by the shared artwork-coloured glow. Structural checks keep Android out of core and enforce shared controls and compatibility bridges.

The review did not establish any unused production subsystem or redundant regression test. They were not deleted to reduce line count. Existing cache, account, remote, layout and migration tests still protect live behavior.

[Historical cleanup](docs/archive/CLEANUP-HISTORY.md). Current validation is in [docs/VALIDATION.md](docs/VALIDATION.md).
