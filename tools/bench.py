#!/usr/bin/env python3
"""Scripted D-pad bench for Matrix Launcher on the user's TV, over adb.

Runs a fixed tour (wake, cold start, 20 focus moves, 6 row changes, menu
open/close, Home press, 30 s idle) and collects frame, memory and cold-start
numbers. Release builds only; 3-run medians; the first cold-start run is
dropped from the cold-start median. Compares medians against a previous
run's JSON with --baseline and reports pass/fail per gate, always printing
the raw numbers alongside the verdicts.

Cannot run on this VM: the user's TV is not reachable from here. Run it
from a machine on the same network as the TV (see the tv-bench skill).

Usage:
    python3 tools/bench.py --serial <TV-IP>:5555 [--runs 3] [--baseline bench-2026-11-14.json] [--output bench.json]
    python3 tools/bench.py --serial <TV-IP>:5555 --runs 1 --allow-debug   # smoke-test the script only

Gates:
    janky frames per key        <= 1.5
    p95 frame time              <= 1.15x the reference in the same session
    key-latency p95             < 50 ms (only when the in-app recorder is on;
                                 otherwise "not measured")
    idle frame rate             <= 1 idle frame/s
    Total PSS                   < 182 MB
    cold first frame (TotalTime) <= 1.15x reference
"""

import argparse
import json
import re
import statistics
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

PACKAGE = "com.matrix.launcher"
# Launcher alias (see AndroidManifest.xml): the exported HOME/LEANBACK entry.
ACTIVITY = "com.gathin.tvhome.ComposeHomeActivity"
FOCUS_MOVES = 20
ROW_CHANGES = 6
IDLE_SECONDS = 30
FRAME_BUDGET_MS = 16.7
GATE_JANKY_PER_KEY = 1.5
GATE_P95_RATIO = 1.15
GATE_KEY_LATENCY_P95_MS = 50.0
GATE_IDLE_FPS = 1.0
GATE_PSS_MB = 182.0
GATE_COLD_RATIO = 1.15


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", default="adb", help="adb binary (default: adb)")
    parser.add_argument("--serial", required=True, help="TV serial, e.g. 192.168.1.50:5555")
    parser.add_argument("--package", default=PACKAGE)
    parser.add_argument("--activity", default=ACTIVITY,
                        help="launcher activity component after the package slash")
    parser.add_argument("--runs", type=int, default=3)
    parser.add_argument("--baseline", type=Path, default=None,
                        help="path to a previous run's JSON; medians are compared against it")
    parser.add_argument("--output", type=Path, default=None,
                        help="write the full run JSON here (default: bench-<timestamp>.json)")
    parser.add_argument("--key-latency", action="store_true",
                        help="pass --ez matrix_key_latency true on cold start so the in-app "
                             "recorder measures key->frame latency (off by default)")
    parser.add_argument("--allow-debug", action="store_true",
                        help="allow a debuggable build (default: refuse; bench release only)")
    return parser.parse_args()


class Adb:
    def __init__(self, adb, serial):
        self.adb = adb
        self.serial = serial

    def run(self, *command, timeout=60, check=True):
        result = subprocess.run([self.adb, "-s", self.serial, *command],
                                capture_output=True, text=True, timeout=timeout)
        if check and result.returncode != 0:
            raise RuntimeError(
                f"adb {' '.join(command)} failed ({result.returncode}): "
                f"{result.stderr.strip() or result.stdout.strip()}")
        return result.stdout

    def shell(self, *command, timeout=60, check=True):
        return self.run("shell", *command, timeout=timeout, check=check)

    def key(self, keycode):
        self.shell("input", "keyevent", keycode)


def parse_total_time(am_start_output):
    """TotalTime from `am start -W` output, in ms, or None."""
    for line in am_start_output.splitlines():
        line = line.strip()
        if line.startswith("TotalTime:"):
            try:
                return int(line.split(":", 1)[1].strip())
            except ValueError:
                return None
    return None


def parse_framestats(text):
    """Parse `dumpsys gfxinfo <pkg> framestats` into frame durations in ms.

    Rows after the ---PROFILEDATA--- marker are comma-separated integers;
    total frame time is FrameCompleted - IntendedVsync (columns 12 and 1,
    0-based), in nanoseconds. Returns a list of durations in milliseconds.
    """
    durations = []
    in_data = False
    for line in text.splitlines():
        line = line.strip()
        if line.startswith("---PROFILEDATA---"):
            in_data = True
            continue
        if not in_data or not line or "," not in line:
            continue
        parts = line.split(",")
        try:
            values = [int(p) for p in parts]
        except ValueError:
            continue
        if len(values) < 13:
            continue
        intended_vsync, frame_completed = values[1], values[12]
        if frame_completed <= intended_vsync:
            continue
        durations.append((frame_completed - intended_vsync) / 1_000_000.0)
    return durations


def framestat_frame_count(text):
    return len(parse_framestats(text))


def percentile(values, pct):
    if not values:
        return None
    ordered = sorted(values)
    index = (len(ordered) - 1) * pct / 100.0
    lower, upper = int(index), min(int(index) + 1, len(ordered) - 1)
    frac = index - lower
    return ordered[lower] * (1 - frac) + ordered[upper] * frac


def parse_pss_mb(meminfo_text):
    """Total PSS in MB from `dumpsys meminfo <pkg>` (TOTAL line, KB)."""
    for line in meminfo_text.splitlines():
        stripped = line.strip()
        if stripped.startswith("TOTAL"):
            parts = stripped.split()
            if len(parts) >= 2:
                try:
                    return int(parts[1]) / 1024.0
                except ValueError:
                    return None
    return None


def parse_key_latency(dump_text):
    """p50/p95 key->frame latency in ms from the activity dump, or None.

    The activity prints `KeyLatency p50=.. p95=.. n=..` when the recorder
    was enabled (see ComposeHomeActivity.dump); absent means not measured.
    """
    match = re.search(r"KeyLatency p50=([\d.]+) p95=([\d.]+) n=(\d+)", dump_text)
    if not match:
        return None
    return {"p50_ms": float(match.group(1)), "p95_ms": float(match.group(2)),
            "n": int(match.group(3))}


def check_release_build(adb, package, allow_debug):
    out = adb.shell("dumpsys", "package", package, timeout=30)
    if "DEBUGGABLE" in out:
        if not allow_debug:
            raise RuntimeError(
                "package is debuggable; bench release builds only "
                "(pass --allow-debug to measure a debug build anyway)")
        print("WARNING: debuggable build (--allow-debug given); numbers are not release numbers",
              file=sys.stderr)


def cold_start(adb, package, activity, key_latency):
    adb.shell("am", "force-stop", package)
    time.sleep(1.0)
    command = ["am", "start", "-W", "-n", f"{package}/{activity}",
               "-a", "android.intent.action.MAIN",
               "-c", "android.intent.category.HOME"]
    if key_latency:
        command += ["--ez", "matrix_key_latency", "true"]
    out = adb.shell(*command, timeout=60)
    total = parse_total_time(out)
    if total is None:
        raise RuntimeError(f"cold start did not report TotalTime:\n{out}")
    return total


def tour(adb):
    """The fixed D-pad tour. Returns the number of key events sent."""
    keys = 0
    for _ in range(FOCUS_MOVES):
        adb.key("KEYCODE_DPAD_RIGHT")
        keys += 1
        time.sleep(0.35)
    for _ in range(ROW_CHANGES):
        adb.key("KEYCODE_DPAD_DOWN")
        keys += 1
        time.sleep(0.35)
    for _ in range(ROW_CHANGES):
        adb.key("KEYCODE_DPAD_UP")
        keys += 1
        time.sleep(0.35)
    adb.key("KEYCODE_MENU")  # menu open
    keys += 1
    time.sleep(1.0)
    adb.key("KEYCODE_BACK")  # menu close
    keys += 1
    time.sleep(0.5)
    adb.key("KEYCODE_HOME")  # Home press (already home: a no-op, then idle at rest)
    keys += 1
    return keys


def one_run(adb, args, run_index):
    print(f"--- run {run_index + 1}/{args.runs} ---")
    adb.key("KEYCODE_WAKEUP")
    time.sleep(0.5)
    adb.shell("wm", "dismiss-keyguard", check=False)
    total_time_ms = cold_start(adb, args.package, args.activity, args.key_latency)
    print(f"cold start TotalTime: {total_time_ms} ms")
    time.sleep(3.0)  # wait for Home to settle
    adb.shell("dumpsys", "gfxinfo", args.package, "reset", check=False)
    frames_before = framestat_frame_count(
        adb.shell("dumpsys", "gfxinfo", args.package, "framestats"))
    keys_sent = tour(adb)
    time.sleep(1.0)
    tour_frames = parse_framestats(
        adb.shell("dumpsys", "gfxinfo", args.package, "framestats"))
    frames_after_tour = len(tour_frames)
    idle_before = framestat_frame_count(
        adb.shell("dumpsys", "gfxinfo", args.package, "framestats"))
    time.sleep(IDLE_SECONDS)
    idle_after = framestat_frame_count(
        adb.shell("dumpsys", "gfxinfo", args.package, "framestats"))
    idle_fps = (idle_after - idle_before) / IDLE_SECONDS
    meminfo = adb.shell("dumpsys", "meminfo", args.package)
    pss_mb = parse_pss_mb(meminfo)
    dump = adb.shell("dumpsys", "activity", args.package, "--matrix-ui", check=False)
    latency = parse_key_latency(dump)
    janky = sum(1 for d in tour_frames if d > FRAME_BUDGET_MS)
    run = {
        "cold_total_time_ms": total_time_ms,
        "keys_sent": keys_sent,
        "tour_frames": frames_after_tour - frames_before,
        "janky_frames": janky,
        "janky_per_key": janky / keys_sent if keys_sent else None,
        "p95_frame_ms": percentile(tour_frames, 95),
        "median_frame_ms": percentile(tour_frames, 50),
        "idle_fps": idle_fps,
        "pss_mb": pss_mb,
        "key_latency_p95_ms": latency["p95_ms"] if latency else None,
        "key_latency_p50_ms": latency["p50_ms"] if latency else None,
        "key_latency_n": latency["n"] if latency else None,
    }
    print(f"tour: {run['tour_frames']} frames, {janky} janky, "
          f"{run['janky_per_key']:.2f} janky/key, p95 {run['p95_frame_ms']:.1f} ms")
    print(f"idle: {idle_fps:.2f} frames/s over {IDLE_SECONDS} s"
          + (f"; PSS: {pss_mb:.1f} MB" if pss_mb is not None else "; PSS not measured"))
    print("key latency p95: "
          f"{latency['p95_ms']:.1f} ms (n={latency['n']})" if latency else
          "key latency: not measured (run with --key-latency)")
    return run


def median_of(runs, key, drop_first=False):
    values = [r[key] for r in runs if r.get(key) is not None]
    if drop_first and len(values) > 1:
        values = values[1:]
    return statistics.median(values) if values else None


def verdict(passed, detail):
    return ("PASS" if passed else "FAIL") + f" ({detail})"


def ratio_gate(ratio_value, limit, detail, skip_text):
    """Verdict for a ratio-vs-baseline gate, or SKIP when there is no baseline."""
    if ratio_value is None:
        return skip_text
    return verdict(ratio_value <= limit, f"{detail}, ratio {ratio_value:.2f}x vs baseline")


def main():
    args = parse_args()
    if args.runs < 1:
        raise SystemExit("--runs must be at least 1")
    adb = Adb(args.adb, args.serial)
    check_release_build(adb, args.package, args.allow_debug)
    baseline = None
    if args.baseline:
        baseline = json.loads(args.baseline.read_text(encoding="utf-8"))
        print(f"baseline: {args.baseline} "
              f"(medians {baseline.get('medians', {})})")

    runs = [one_run(adb, args, i) for i in range(args.runs)]
    medians = {
        "cold_total_time_ms": median_of(runs, "cold_total_time_ms", drop_first=True),
        "janky_per_key": median_of(runs, "janky_per_key"),
        "p95_frame_ms": median_of(runs, "p95_frame_ms"),
        "idle_fps": median_of(runs, "idle_fps"),
        "pss_mb": median_of(runs, "pss_mb"),
        "key_latency_p95_ms": median_of(runs, "key_latency_p95_ms"),
    }
    base_medians = (baseline or {}).get("medians", {}) if baseline else {}

    def ratio(key):
        ref = base_medians.get(key)
        value = medians.get(key)
        if ref is None or value is None:
            return None
        return value / ref if ref else None

    print("\n=== medians (3-run) ===")
    for key, value in medians.items():
        print(f"{key}: {value if value is None else round(value, 2)}"
              + (f"  (baseline {base_medians.get(key)}, "
                 f"ratio {ratio(key):.2f}x)" if key in base_medians else ""))

    print("\n=== gates ===")
    gates = {}
    gates["janky_per_key<=1.5"] = verdict(
        medians["janky_per_key"] is not None and medians["janky_per_key"] <= GATE_JANKY_PER_KEY,
        f"median {medians['janky_per_key']:.2f} janky/key")
    p95_ratio = ratio("p95_frame_ms")
    gates["p95_frame<=1.15x_reference"] = ratio_gate(
        p95_ratio, GATE_P95_RATIO,
        f"median p95 {medians['p95_frame_ms']:.1f} ms",
        "SKIP (no baseline p95_frame_ms to compare; median p95 "
        f"{medians['p95_frame_ms']:.1f} ms)")
    if medians["key_latency_p95_ms"] is None:
        gates["key_latency_p95<50ms"] = "not measured (run with --key-latency)"
    else:
        gates["key_latency_p95<50ms"] = verdict(
            medians["key_latency_p95_ms"] < GATE_KEY_LATENCY_P95_MS,
            f"median p95 {medians['key_latency_p95_ms']:.1f} ms")
    gates["idle<=1_frame/s"] = verdict(
        medians["idle_fps"] is not None and medians["idle_fps"] <= GATE_IDLE_FPS,
        f"median {medians['idle_fps']:.2f} frames/s")
    gates["pss<182MB"] = verdict(
        medians["pss_mb"] is not None and medians["pss_mb"] < GATE_PSS_MB,
        f"median {medians['pss_mb']:.1f} MB")
    cold_ratio = ratio("cold_total_time_ms")
    gates["cold_first_frame<=1.15x_reference"] = ratio_gate(
        cold_ratio, GATE_COLD_RATIO,
        f"median TotalTime {medians['cold_total_time_ms']:.0f} ms (first run dropped)",
        "SKIP (no baseline cold_total_time_ms to compare; median TotalTime "
        f"{medians['cold_total_time_ms']:.0f} ms, first run dropped)")
    for name, result in gates.items():
        print(f"{name}: {result}")

    result = {
        "package": args.package,
        "activity": args.activity,
        "runs_requested": args.runs,
        "started_at": datetime.now(timezone.utc).isoformat(),
        "tour": {"focus_moves": FOCUS_MOVES, "row_changes": ROW_CHANGES,
                 "idle_seconds": IDLE_SECONDS},
        "per_run": runs,
        "medians": medians,
        "baseline": str(args.baseline) if args.baseline else None,
        "gates": gates,
    }
    output = args.output or Path(
        f"bench-{datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S')}.json")
    output.write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(f"\nfull JSON written to {output}")
    failed = [name for name, value in gates.items() if value.startswith("FAIL")]
    if failed:
        print(f"FAILED gates: {', '.join(failed)}", file=sys.stderr)
        raise SystemExit(1)
    print("all measured gates passed (skipped gates need a baseline or --key-latency)")


if __name__ == "__main__":
    main()
