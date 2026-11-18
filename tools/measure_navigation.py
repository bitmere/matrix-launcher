"""Repeatable, playback-free warm Home-row navigation; keeps evidence in ignored artifacts."""
import argparse
import os
from pathlib import Path
import subprocess
import sys
import time

parser = argparse.ArgumentParser()
parser.add_argument('--adb', required=True)
parser.add_argument('--serial', required=True)
parser.add_argument('--output', required=True)
parser.add_argument('--trace-helper')
parser.add_argument('--screenshot', action='store_true', help='Optional warm-frame visual check; off by default')
args = parser.parse_args()
prefix = Path(args.output).resolve()
prefix.parent.mkdir(parents=True, exist_ok=True)
package = 'com.matrix.launcher'
def adb(*cmd):
    return subprocess.run([args.adb, '-s', args.serial, *cmd], check=True, capture_output=True, text=True, timeout=35).stdout

# Never interrupt another app merely to establish a measurement starting point.
initial_window = adb('shell', 'dumpsys', 'window')
if not any(package in line for line in initial_window.splitlines() if 'mCurrentFocus=' in line):
    raise RuntimeError('Matrix must already be foreground; no input sent')
adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
for _ in range(2):
    adb('shell', 'am', 'start', '-n', package + '/com.matrix.launcher.ComposeHomeActivity',
        '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME',
        '--activity-clear-top', '--activity-single-top')
    time.sleep(.6)
def require_foreground():
    focus = [line for line in adb('shell', 'dumpsys', 'window').splitlines() if 'mCurrentFocus=' in line]
    if not any(package in line for line in focus):
        raise RuntimeError('Candidate is not focused; stopping input and measurement')
    return focus
focus = require_foreground()
adb('shell', 'input', 'keyevent', 'KEYCODE_DPAD_DOWN', 'KEYCODE_DPAD_RIGHT', 'KEYCODE_DPAD_LEFT')
time.sleep(.6)
require_foreground()
Path(str(prefix) + '-window.txt').write_text('\n'.join(focus), encoding='utf-8')
if args.screenshot:
    adb('shell', 'screencap', '-p', '/sdcard/matrix-profile-warm.png')
    adb('pull', '/sdcard/matrix-profile-warm.png', str(prefix) + '-warm.png')
    adb('shell', 'rm', '-f', '/sdcard/matrix-profile-warm.png')
adb('shell', 'dumpsys', 'gfxinfo', package, 'reset')
trace = None
if args.trace_helper:
    env = dict(os.environ)
    env['PATH'] = str(Path(args.adb).parent) + os.pathsep + env.get('PATH', '')
    trace = subprocess.Popen([sys.executable, args.trace_helper, '--serial', args.serial,
        '-t', '18s', '-b', '16mb', '-a', package, '-n', '-o', str(prefix) + '.perfetto-trace', 'gfx', 'view', 'sched'],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, env=env)
    time.sleep(1)
for key in (['KEYCODE_DPAD_RIGHT'] * 3 + ['KEYCODE_DPAD_LEFT'] * 3) * 4:
    require_foreground()
    adb('shell', 'input', 'keyevent', key)
    time.sleep(.22)
if trace:
    output, _ = trace.communicate(timeout=40)
    Path(str(prefix) + '-trace.log').write_text(output, encoding='utf-8')
    print('trace exit', trace.returncode)
report = adb('shell', 'dumpsys', 'gfxinfo', package)
Path(str(prefix) + '-gfx.txt').write_text(report, encoding='utf-8')
for line in report.splitlines():
    if any(label in line for label in ('Total frames', 'Janky frames', 'percentile', 'Number Missed', 'Number Slow')):
        print(line.strip())
