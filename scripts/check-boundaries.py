#!/usr/bin/env python3
"""Fail build for cross-service implementation imports and shared business persistence."""
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1]
sources = list((root / 'services').glob('*/src/main/java/**/*.java'))
owners = {}
for source in sources:
    match = re.search(r'^package\s+([\w.]+);', source.read_text(), re.M)
    if match:
        owners[match[1] + '.' + source.stem] = source.relative_to(root / 'services').parts[0]
errors = []
for source in sources:
    owner = source.relative_to(root / 'services').parts[0]
    for imported in re.findall(r'^import\s+(?:static\s+)?([\w.*]+);', source.read_text(), re.M):
        targets = {other for name, other in owners.items()
                   if name == imported or imported.startswith(name + '.') or
                   (imported.endswith('.*') and name.startswith(imported[:-1]))}
        if targets - {owner}:
            errors.append(f'{source.relative_to(root)}: cross-service import {imported}')
for source in (root / 'libraries/platform-runtime/src/main/java').rglob('*.java'):
    if '@TableName' in source.read_text() or 'com.eduze.manage.' in source.read_text():
        errors.append(f'{source.relative_to(root)}: business persistence in technical runtime')
for source in sources:
    content = source.read_text()
    if re.search(r'(?i)\b(?:FROM|JOIN|UPDATE|INTO)\s+eduze_\w+\.', content):
        errors.append(f'{source.relative_to(root)}: cross-schema SQL')
if errors:
    print('\n'.join(errors), file=sys.stderr)
    raise SystemExit(1)
print(f'Boundary check passed: {len(sources)} service sources, no cross-service implementation imports.')
