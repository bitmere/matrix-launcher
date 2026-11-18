"""Read-only settled Home row checks; no screenshots, UiAutomation or account data."""
import argparse
import re
import subprocess
from matrix_ui import resumed_ui

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--adb', required=True)
parser.add_argument('--serial', required=True)
args = parser.parse_args()
def read(*command):
    return subprocess.run([args.adb, '-s', args.serial, 'shell', *command], check=True,
        capture_output=True, text=True, timeout=20).stdout
dump = resumed_ui(read('dumpsys', 'activity', 'com.matrix.launcher', '--matrix-ui'))
window = read('dumpsys', 'window')
if not any('com.matrix.launcher' in line for line in window.splitlines() if 'mCurrentFocus=' in line):
    raise SystemExit('FAIL: Matrix is not the focused window')
if 'MatrixUi page=home ' not in dump or 'lifecycle=RESUMED' not in dump:
    raise SystemExit('FAIL: Home must be active and resumed')
rows = [line.strip() for line in dump.splitlines() if 'RowLayout ' in line]
if not rows:
    raise SystemExit('FAIL: no measured Home rows')
for row in rows:
    if 'scrolling=false' not in row or not re.search(r'partial=0\b', row):
        raise SystemExit('FAIL: row is moving or contains partial slots: ' + row)
print(f'PASS: {len(rows)} settled rows contain whole visible slots')
print('Layout bounds do not verify painted borders, artwork or animation smoothness.')
