#!/usr/bin/env python3
"""Back up each owned schema without SQL that can switch into another database."""
import argparse
import datetime
import gzip
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--env-file', default='docker/platform/.env.dev')
parser.add_argument('--quiesce', action='store_true', help='Pause running business services for a consistent multi-schema restore point')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
target = root / 'backups/platform' / stamp
target.mkdir(parents=True, exist_ok=False, mode=0o700)
compose = ['docker', 'compose', '--env-file', str(root / args.env_file), '-f', str(root / 'docker/platform/compose.yml')]
manifest = {'createdAt': stamp, 'schemas': [], 'complete': False, 'consistency': 'quiesced_services' if args.quiesce else 'independent_online_snapshots'}
running = []
if args.quiesce:
    names = subprocess.check_output(compose + ['ps', '--services', '--filter', 'status=running'], text=True).splitlines()
    running = [name for name in names if name in ('identity', 'academic', 'teaching', 'portfolio', 'media', 'notification', 'engagement', 'commerce')]
    if running:
        subprocess.run(compose + ['stop'] + running, check=True, stdout=subprocess.DEVNULL)
try:
    for service in ('identity', 'academic', 'teaching', 'portfolio', 'media', 'notification', 'engagement', 'commerce'):
        database = 'eduze_' + service
        command = compose + ['exec', '-T', 'mysql', 'sh', '-c',
                             'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction '
                             '--no-tablespaces --set-gtid-purged=OFF --routines --triggers ' + database]
        temporary = target / (database + '.sql.gz.partial')
        with subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE) as process:
            with gzip.open(temporary, 'wb') as stream:
                shutil.copyfileobj(process.stdout, stream)
            error = process.stderr.read()
            if process.wait() != 0:
                raise SystemExit(f'Backup failed for {database}; partial output kept, manifest not complete.')
        final = temporary.with_suffix('')
        temporary.rename(final)
        with gzip.open(final, 'rb') as stream:
            while stream.read(1024 * 1024):
                pass
        query = compose + ['exec', '-T', 'mysql', 'sh', '-c',
                           'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --batch --skip-column-names ' + database]
        tables = subprocess.check_output(query, input=b'SHOW TABLES;').decode().splitlines()
        counts = {}
        for table in tables:
            if not table.replace('_', '').isalnum():
                raise SystemExit('Unexpected table identifier in backup metadata.')
            counts[table] = int(subprocess.check_output(query, input=('SELECT COUNT(*) FROM `' + table + '`;').encode()).strip())
        manifest['schemas'].append({'database': database, 'file': final.name,
                                    'sha256': hashlib.sha256(final.read_bytes()).hexdigest(),
                                    'tableCounts': counts if args.quiesce else None})
finally:
    if running:
        subprocess.run(compose + ['up', '-d', '--no-build'] + running, check=True, stdout=subprocess.DEVNULL)
manifest['complete'] = True
(target / 'manifest.json').write_text(json.dumps(manifest, indent=2))
print(f'Backup complete: {target}; restore verification is a separate operation.')
