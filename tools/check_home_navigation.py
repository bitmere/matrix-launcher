"""Bounded, playback-free Home stress checks; dumps only, no screenshots or preference writes.

Bursts and synthetic longpress exercise fast input but cannot certify real remote repeats.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess
import time
from matrix_ui import resumed_ui

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--adb', required=True)
parser.add_argument('--serial', required=True)
parser.add_argument('--output', required=True)
parser.add_argument('--home-key-reset', action='store_true', help='Exercise synthetic HOME; not a physical-remote verdict')
args = parser.parse_args()
output = Path(args.output)
output.mkdir(parents=True, exist_ok=True)
package = 'com.matrix.launcher'
history = []


def adb(*command):
    return subprocess.run([args.adb, '-s', args.serial, *command], capture_output=True,
                          text=True, check=True, timeout=35).stdout


def checkpoint(name):
    window = adb('shell', 'dumpsys', 'window')
    if not any(package in line for line in window.splitlines() if 'mCurrentFocus=' in line):
        raise RuntimeError('Matrix lost foreground; stopping input')
    live = resumed_ui(adb('shell', 'dumpsys', 'activity', package, '--matrix-ui'))
    if 'MatrixUi page=home ' not in live:
        raise RuntimeError('Not on Home; stopping input')
    (output / (name + '.txt')).write_text(live, encoding='utf-8')
    match = re.search(r'homeRow=(-?\d+) homeCard=(-?\d+)', live)
    rows = re.search(r' rows=(\d+) ', live)
    result = {'stage': name, 'row': int(match[1]), 'card': int(match[2]), 'rows': int(rows[1])}
    history.append(result)
    print(json.dumps(result), flush=True)
    return result


def reset():
    for _ in range(2):
        if args.home_key_reset:
            adb('shell', 'input', 'keyevent', 'HOME')
        else:
            adb('shell', 'am', 'start', '-n', package + '/com.matrix.launcher.ComposeHomeActivity',
                '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME',
                '--activity-clear-top', '--activity-single-top')
        time.sleep(.65)
    if checkpoint('reset')['row'] != 0:
        raise RuntimeError('Home did not reset to favorites; refusing invalid stress run')


reset()
checkpoint('initial')
adb('shell', 'dumpsys', 'gfxinfo', package, 'reset')
adb('shell', 'input', 'keyevent', *(['DPAD_DOWN'] * 12))
time.sleep(1)
checkpoint('down-burst')
for _ in range(10):
    adb('shell', 'input', 'keyevent', 'DPAD_DOWN')
    time.sleep(.25)
time.sleep(.5)
bottom = checkpoint('paced-bottom')
if bottom['rows'] and bottom['row'] != bottom['rows']:
    raise RuntimeError('Paced Down failed to reach final channel')
adb('shell', 'input', 'keyevent', *(['DPAD_UP'] * 12))
time.sleep(1)
checkpoint('up-burst')
reset()
for _ in range(5):
    adb('shell', 'input', 'keyevent', '--longpress', 'DPAD_DOWN')
time.sleep(1)
checkpoint('synthetic-hold')
(output / 'gfx.txt').write_text(adb('shell', 'dumpsys', 'gfxinfo', package), encoding='utf-8')
reset()
(output / 'summary.json').write_text(json.dumps(history, indent=2), encoding='utf-8')
