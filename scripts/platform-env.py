#!/usr/bin/env python3
"""Generate an ignored development environment without printing secrets."""
from pathlib import Path
import secrets
import os

root = Path(__file__).resolve().parents[1]
target = root / 'docker/platform/.env.dev'
values = {'MYSQL_ROOT_PASSWORD': secrets.token_hex(24), 'JWT_SECRET': secrets.token_hex(48),
          'BOOTSTRAP_ADMIN_USERNAME': 'studio-admin', 'BOOTSTRAP_ADMIN_PASSWORD': secrets.token_hex(20),
          'PLATFORM_PROFILE': 'dev', 'PLATFORM_HTTP_PORT': '18880',
          'PUBLIC_BASE_URL': 'http://localhost:18880'}
for service in ('identity', 'academic', 'teaching', 'portfolio', 'media', 'notification', 'engagement', 'commerce'):
    values[service.upper() + '_DB_PASSWORD'] = secrets.token_hex(24)
    values[service.upper() + '_SERVICE_TOKEN'] = secrets.token_hex(32)
for service in ('identity', 'academic'):
    values[service.upper() + '_REDIS_PASSWORD'] = secrets.token_hex(24)
try:
    descriptor = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
except FileExistsError:
    raise SystemExit('Environment already exists; existing credentials were preserved.')
with os.fdopen(descriptor, 'w') as stream:
    stream.write(''.join(f'{key}={value}\n' for key, value in values.items()))
print(f'Created private development environment: {target}')
