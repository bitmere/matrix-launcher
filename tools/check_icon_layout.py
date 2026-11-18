"""Read-only ADB check of shared icon frame centering; no UiAutomation or screenshots."""
import argparse
import re
import subprocess
from matrix_ui import resumed_ui

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--adb', required=True)
parser.add_argument('--serial', required=True)
parser.add_argument('--min-icons', type=int, default=5)
parser.add_argument('--tolerance', type=float, default=.5, help='Maximum frame-center offset in physical pixels')
args = parser.parse_args()
result = subprocess.run([args.adb, '-s', args.serial, 'shell', 'dumpsys', 'activity',
    'com.matrix.launcher', '--matrix-ui'], check=True, capture_output=True, text=True, timeout=20)
window = subprocess.run([args.adb, '-s', args.serial, 'shell', 'dumpsys', 'window'],
    check=True, capture_output=True, text=True, timeout=20).stdout
focused = [line for line in window.splitlines() if 'mCurrentFocus=' in line]
if not any('com.matrix.launcher' in line for line in focused):
    raise SystemExit('FAIL: Matrix is not the focused window')
live = resumed_ui(result.stdout)
lines = [line.strip() for line in live.splitlines() if 'IconLayout ' in line]
if len(lines) < args.min_icons:
    raise SystemExit('FAIL: expected a settled foreground Matrix screen and enough icon controls')
for line in lines:
    match = re.search(r'offset=(-?[\d.]+),(-?[\d.]+)$', line)
    if not match or any(abs(float(value)) > args.tolerance for value in match.groups()):
        raise SystemExit('FAIL: ' + line)
print(f'PASS: {len(lines)} icon frames centered within {args.tolerance} physical pixels')
print('Frame bounds do not prove vector artwork, colors or optical alignment; inspect final captures too.')
