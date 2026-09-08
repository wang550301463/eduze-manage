#!/usr/bin/env python3
"""Verify a backup in a disposable MySQL container; never connect to the live database."""
import argparse
import gzip
import hashlib
import json
import os
from pathlib import Path
import secrets
import subprocess
import time

parser = argparse.ArgumentParser()
parser.add_argument('backup_directory', type=Path)
args = parser.parse_args()
directory = args.backup_directory.resolve()
manifest = json.loads((directory / 'manifest.json').read_text())
if not manifest.get('complete'):
    raise SystemExit('Refusing an incomplete backup.')
schemas = manifest['schemas']
if len(schemas) != 8 or len({item['database'] for item in schemas}) != 8:
    raise SystemExit('Platform backup must contain exactly eight distinct service schemas.')
allowed = {'eduze_' + item for item in ('identity', 'academic', 'teaching', 'portfolio', 'media', 'notification', 'engagement', 'commerce')}
for item in schemas:
    source = (directory / item['file']).resolve()
    if source.parent != directory or item['database'] not in allowed:
        raise SystemExit('Invalid backup manifest.')
    if hashlib.sha256(source.read_bytes()).hexdigest() != item['sha256']:
        raise SystemExit('Backup checksum mismatch.')
name = 'eduze-restore-check-' + secrets.token_hex(5)
environment = dict(os.environ, MYSQL_ROOT_PASSWORD=secrets.token_hex(24))
subprocess.run(['docker', 'run', '--rm', '-d', '--name', name, '-e', 'MYSQL_ROOT_PASSWORD', 'mysql:8.4'],
               env=environment, check=True, stdout=subprocess.DEVNULL)
def mysql(sql=None, database=None):
    command = ['docker', 'exec', '-i', name, 'sh', '-c',
               'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --batch --skip-column-names' + (' ' + database if database else '')]
    return subprocess.run(command, input=sql, capture_output=True)
try:
    for attempt in range(90):
        if mysql(b'SELECT 1;').returncode == 0:
            break
        time.sleep(1)
    else:
        raise SystemExit('Disposable MySQL did not become ready.')
    result = []
    for item in schemas:
        database = item['database']
        if mysql(f'CREATE DATABASE {database} CHARACTER SET utf8mb4;'.encode()).returncode:
            raise SystemExit('Could not initialize restore database.')
        # Dump was made without --databases, so there are no USE or CREATE DATABASE directives.
        with gzip.open(directory / item['file'], 'rb') as stream:
            restored = mysql(stream.read(), database)
        if restored.returncode:
            raise SystemExit(f'Restore failed for {database}; no live database was touched.')
        count = mysql(f"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='{database}';".encode())
        expected_counts = item.get('tableCounts')
        if expected_counts is not None:
            for table, expected in expected_counts.items():
                if not table.replace('_', '').isalnum():
                    raise SystemExit('Invalid table identifier in manifest.')
                actual = mysql(('SELECT COUNT(*) FROM `' + table + '`;').encode(), database)
                if actual.returncode or int(actual.stdout.strip()) != expected:
                    raise SystemExit(database + ': restored row count mismatch.')
        result.append({'database': database, 'tables': int(count.stdout.strip()),
                       'tableRowCountsMatched': expected_counts is not None})
    output = directory / 'restore-check.json'
    output.write_text(json.dumps({'verifiedAt': time.time(), 'schemas': result,
                                  'scope': 'checksums, schema/data import and quiesced table row counts; business consistency checked separately'}, indent=2))
    print(f'Disposable restore verification complete: {output}')
finally:
    subprocess.run(['docker', 'rm', '-f', name], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
