#!/usr/bin/env python3
"""Prove each application credential can read its own schema and cannot read another."""
import argparse
import json
from pathlib import Path
import subprocess
import secrets

parser = argparse.ArgumentParser()
parser.add_argument('--env-file', default='docker/platform/.env.dev')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
compose = ['docker', 'compose', '--env-file', str(root / args.env_file), '-f', str(root / 'docker/platform/compose.yml')]
names = ['identity', 'academic', 'teaching', 'portfolio', 'media', 'notification', 'engagement', 'commerce']
results = []
for index, name in enumerate(names):
    other = names[(index + 1) % len(names)]
    prefix = 'MYSQL_PWD="$' + name.upper() + '_DB_PASSWORD" exec mysql -ueduze_' + name + ' --batch --skip-column-names -e '
    own = subprocess.run(compose + ['exec', '-T', 'mysql', 'sh', '-c', prefix + "'SELECT COUNT(*) FROM eduze_" + name + ".flyway_schema_history'"], capture_output=True)
    foreign = subprocess.run(compose + ['exec', '-T', 'mysql', 'sh', '-c', prefix + "'SELECT COUNT(*) FROM eduze_" + other + ".flyway_schema_history'"], capture_output=True)
    if own.returncode or foreign.returncode == 0 or b'1142' not in foreign.stderr:
        raise SystemExit(name + ': database isolation verification failed')
    results.append({'service': name, 'ownSchemaReadable': True, 'foreignSchemaDenied': other})
    print(name + ': own schema allowed; foreign schema denied')
for name, other in [('identity', 'academic'), ('academic', 'identity')]:
    key = name + ':isolation-check:' + secrets.token_hex(8)
    base = 'REDISCLI_AUTH="$' + name.upper() + '_REDIS_PASSWORD" redis-cli --user ' + name + ' --raw '
    def redis(command):
        return subprocess.check_output(compose + ['exec', '-T', 'redis', 'sh', '-c', base + command], text=True).strip()
    if redis('SET ' + key + ' 1 EX 10') != 'OK':
        raise SystemExit(name + ': own Redis key write failed.')
    if 'NOPERM' not in redis('GET ' + other + ':isolation-check') or 'NOPERM' not in redis('SCAN 0'):
        raise SystemExit(name + ': Redis key isolation failed.')
    print(name + ': own Redis prefix allowed; foreign prefix and global enumeration denied')
    results.append({'redisUser': name, 'ownPrefixWritable': True, 'foreignPrefixDenied': True, 'globalEnumerationDenied': True})
output = root / 'target/database-isolation.json'
output.parent.mkdir(exist_ok=True)
output.write_text(json.dumps(results, indent=2) + '\n')
