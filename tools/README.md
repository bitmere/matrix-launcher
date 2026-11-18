# Tool index

Run helpers from the repository root. Existing scripts stay in place to preserve
entry points and relative artifact paths. Device tools require an explicit device
or the configuration documented in the script; they are not local unit tests.
Keep device evidence and build outputs in ignored artifacts.

| Helper | Purpose |
| --- | --- |
| [build.ps1](../scripts/build.ps1) | Keyless Preview/Check/Test modes; optimized release checks/signing require GitHub Actions secrets. See [testing](../docs/TESTING.md). |
| [install-compose-preview.ps1](../scripts/install-compose-preview.ps1) | Despite its name, installs the measured release artifact and verifies the installed hash. |
| [optimize-app.ps1](../scripts/optimize-app.ps1) | App-only ART compilation or reset on the selected TV. |
| [enter-tv-text.ps1](../scripts/enter-tv-text.ps1) | Guarded text entry into a focused Matrix field. |
| [adb_tv.ps1](adb_tv.ps1) | Windows ADB wrapper; reads TV_SERIAL, otherwise uses its built-in device address. |
| [audit_structure.py](audit_structure.py) | Local structural and component contract audit. |
| [release_gate.py](release_gate.py), [test_release_gate.py](test_release_gate.py) | Actions release metadata, signer and immutable publication gates; offline boundary tests run with the tool suite below. Requires the full approved public `MATRIX_RELEASE_CERT_SHA256` repository variable before release dispatch. |
| [test_audit_structure.py](test_audit_structure.py), [test_matrix_ui.py](test_matrix_ui.py) | Local Python helper regression tests. |
| [compose_smoke.py](compose_smoke.py) | Device XML and D-pad smoke checks. |
| [check_home_navigation.py](check_home_navigation.py) | Bounded device navigation checks. |
| [check_row_layout.py](check_row_layout.py), [check_icon_layout.py](check_icon_layout.py) | Settled row geometry and icon frame offsets from device diagnostics. |
| [measure_navigation.py](measure_navigation.py) | Warm Home navigation measurements with optional trace capture. |
| [bench.py](bench.py) | Scripted device tour with frame, memory and cold-start measurements. |
| [matrix_ui.py](matrix_ui.py) | Shared parser selecting the resumed Matrix activity. |
| [parse_ui_xml.py](parse_ui_xml.py) | Prints text from the fixed artifacts/tv_ui_inspected.xml input. |

Local checks without Gradle or a device:

```sh
python3 tools/audit_structure.py
python3 -m unittest discover -s tools -p 'test_*.py'
```

Device scripts have different input/output contracts; read the script or its
argument help before use. Physical remote and account integration still require
appropriate device checks.
