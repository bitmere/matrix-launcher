"""Explicit-device, XML-first smoke check. No account, source or favorite changes are made."""
import argparse
import json
from pathlib import Path
import subprocess
import time
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--serial', required=True)
    parser.add_argument('--adb', required=True)
    parser.add_argument('--package', default='com.matrix.launcher', choices=['com.matrix.launcher', 'com.matrix.launcher.preview'])
    parser.add_argument('--extended', action='store_true')
    parser.add_argument('--fresh-task', action='store_true', help='Reset the candidate activity task; use on the emulator, not with a live TV Home interceptor')
    parser.add_argument('--output', default='artifacts/compose-smoke')
    args = parser.parse_args()
    output = Path(args.output).resolve() / str(time.time_ns())
    output.mkdir(parents=True)
    results = []
    package = args.package

    def adb(*values):
        if values[:2] in (('shell', 'input'), ('shell', 'am')):
            window = subprocess.run([args.adb, '-s', args.serial, 'shell', 'dumpsys', 'window'],
                                    capture_output=True, text=True, check=True, timeout=35).stdout
            if not any(package in line for line in window.splitlines() if 'mCurrentFocus=' in line):
                raise RuntimeError('Matrix is not foreground; stopping before input')
        result = subprocess.run([args.adb, '-s', args.serial, *values], capture_output=True, text=True, timeout=35)
        if result.returncode:
            raise RuntimeError(result.stderr.strip() or result.stdout.strip())
        return result.stdout

    def keys(*codes):
        adb('shell', 'input', 'keyevent', *codes)

    def inspect(name, expected):
        remote = '/sdcard/matrix-smoke-' + str(time.time_ns()) + '.xml'
        dumped = adb('shell', 'uiautomator', 'dump', remote)
        if 'dumped' not in dumped.lower():
            raise RuntimeError('Hierarchy unavailable: ' + dumped.strip())
        local = output / (name + '.xml')
        adb('pull', remote, str(local))
        adb('shell', 'rm', '-f', remote)
        root = ET.parse(local)
        nodes = list(root.iter('node'))
        if package not in {n.get('package') for n in nodes}:
            raise AssertionError('Candidate is absent from the current hierarchy')
        texts = {value for n in nodes for value in (n.get('text'), n.get('content-desc'))}
        for value in expected:
            if value not in texts:
                raise AssertionError(f'{name}: expected text absent: {value}')
        results.append(dict(page=name, expected=list(expected), nodes=len(nodes)))
        return nodes


    def choose(label):
        # Restore-aware: reset spatial focus to the first row, then inspect each D-pad step.
        keys(*(['KEYCODE_DPAD_UP'] * 16))
        for step in range(16):
            remote = '/sdcard/matrix-focus-' + str(time.time_ns()) + '.xml'
            adb('shell', 'uiautomator', 'dump', remote)
            local = output / 'focus.xml'
            adb('pull', remote, str(local)); adb('shell', 'rm', '-f', remote)
            nodes = ET.parse(local)
            focused = [n for n in nodes.iter('node') if n.get('focused') == 'true']
            if any(label in {c.get('text') for c in n.iter('node')} for n in focused):
                keys('KEYCODE_DPAD_CENTER'); return
            keys('KEYCODE_DPAD_DOWN')
        raise AssertionError('D-pad could not focus ' + label)

    try:
        adb('shell', 'am', 'start', '-W', *(['-f', '0x10008000'] if args.fresh_task else []), '-n', package + '/com.matrix.launcher.ComposeHomeActivity', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')
        inspect('home', ['WELCOME HOME'])
        # Explicit public request also verifies remote settings routing. D-pad handles all menu interactions.
        adb('shell', 'am', 'start', '-W', '-n', package + '/com.matrix.launcher.ComposeHomeActivity', '-a', 'com.matrix.launcher.action.LAUNCHER_SETTINGS')
        inspect('settings', ['Home settings', 'Home channels'])
        choose('Home channels'); inspect('channels', ['Home channels', 'Choose apps', 'Rows & order'])
        choose('Choose apps'); inspect('sources', ['App channels on Home', 'Save'])
        keys('KEYCODE_BACK'); inspect('back-to-channels', ['Home channels'])
        keys('KEYCODE_BACK'); inspect('back-to-settings', ['Home settings'])
        choose('Edit favorites'); inspect('favorites', ['Edit favorites', 'Save', 'Save & reorder'])
        keys('KEYCODE_BACK'); inspect('favorites-cancel', ['Home settings'])
        if args.extended:
            choose('Appearance'); inspect('appearance', ['Appearance', 'Channel & list card size'])
            choose('Idle clock · Off'); inspect('idle-clock', ['Idle clock', 'Off'])
            keys('KEYCODE_BACK'); keys('KEYCODE_BACK')
            choose('Wallpapers'); inspect('wallpapers', ['Wallpapers', 'Import pictures'])
            choose('Wallpaper credits'); inspect('wallpaper-credits', ['Wallpaper credits'])
            keys('KEYCODE_BACK'); keys('KEYCODE_BACK')
            choose('Integrations'); inspect('integrations', ['Integrations', 'SIMKL', 'TMDB'])
            choose('SIMKL'); nodes = inspect('simkl-optional', [])
            if any(n.get('text') == 'Public Client ID' for n in nodes):
                choose('Public Client ID'); inspect('simkl-client', ['SIMKL application', 'Public Client ID', 'Save', 'Cancel'])
                keys('KEYCODE_BACK')
            else:
                choose('API usage & sync'); inspect('simkl-usage', ['SIMKL · sync', 'Connected Apps'])
                keys('KEYCODE_BACK')
            keys('KEYCODE_BACK'); keys('KEYCODE_BACK')
            choose('Home & remote'); inspect('home-control', ['Matrix Home setup', 'Home redirect options'])
            choose('Home redirect options'); inspect('redirect', ['Home redirect', 'Window-only redirect', 'Capture Home key'])
            keys('KEYCODE_BACK'); keys('KEYCODE_BACK')
            choose('About'); inspect('about', ['Matrix Launcher', 'Licenses'])
            choose('Licenses'); inspect('licenses', ['Licenses', 'Back'])
            keys('KEYCODE_BACK'); keys('KEYCODE_BACK')
        keys('KEYCODE_BACK'); inspect('final-home', ['WELCOME HOME'])
        pid = adb('shell', 'pidof', package).strip()
        if not pid:
            raise AssertionError('Candidate process exited')
        report = dict(status='passed', device=args.serial, package=package, checks=results, output=str(output))
    except Exception as error:
        report = dict(status='inconclusive' if 'Hierarchy unavailable' in str(error) or 'failed to stat remote object' in str(error) else 'failed', device=args.serial, checks=results, error=str(error), output=str(output))
    (output / 'result.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report, indent=2))
    return 0 if report['status'] == 'passed' else 2 if report['status'] == 'inconclusive' else 1


if __name__ == '__main__':
    raise SystemExit(main())
