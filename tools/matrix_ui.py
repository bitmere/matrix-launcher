"""Select the live Matrix instance; stopped Home/standard tasks are not current UI."""
import re


def resumed_ui(text):
    blocks = re.split(r'(?m)^\s*ACTIVITY ', text) if re.search(r'(?m)^\s*ACTIVITY ', text) else [text]
    live = [block for block in blocks if 'MatrixUi ' in block and re.search(r'\blifecycle=RESUMED\b', block)]
    if len(live) != 1:
        raise ValueError(f'Expected one resumed Matrix instance, found {len(live)}')
    return live[0]
