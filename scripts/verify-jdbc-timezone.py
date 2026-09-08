#!/usr/bin/env python3
"""Check actual app JDBC credentials, driver and JVM timezone using connection-local tables."""
import argparse
from pathlib import Path
import subprocess
import tempfile
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--env-file', default='docker/platform/.env.dev')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
compose = ['docker', 'compose', '--env-file', str(root / args.env_file), '-f', str(root / 'docker/platform/compose.yml')]
with tempfile.TemporaryDirectory(prefix='eduze-timezone-') as folder:
    directory = Path(folder)
    directory.chmod(0o755)
    subprocess.run(['javac', '--release', '17', '-d', folder, str(root / 'scripts/tests/JdbcSessionTimezoneCheck.java')], check=True)
    with zipfile.ZipFile(root / 'services/academic-service/target/academic-service-1.0.0.jar') as jar:
        drivers = [item for item in jar.namelist() if item.startswith('BOOT-INF/lib/mysql-connector-j-')]
        if len(drivers) != 1:
            raise SystemExit('Expected one pinned Connector/J dependency.')
        (directory / 'driver.jar').write_bytes(jar.read(drivers[0]))
    for service in ('identity', 'academic', 'teaching', 'portfolio', 'media', 'notification', 'engagement', 'commerce'):
        container = subprocess.check_output(compose + ['ps', '-q', service], text=True).strip()
        if not container:
            raise SystemExit(service + ': service is not running.')
        remote = '/tmp/' + directory.name
        subprocess.run(['docker', 'cp', folder, container + ':' + remote], check=True, stdout=subprocess.DEVNULL)
        try:
            command = 'java -cp ' + remote + ':' + remote + '/driver.jar JdbcSessionTimezoneCheck "${SPRING_DATASOURCE_URL#*\\?}"'
            result = subprocess.run(compose + ['exec', '-T', service, 'sh', '-c', command], capture_output=True, text=True, timeout=30)
            if result.returncode:
                raise SystemExit(service + ': runtime JDBC timezone verification failed.')
            print(service + ': ' + result.stdout.strip())
        finally:
            subprocess.run(compose + ['exec', '-T', '--user', '0', service, 'rm', '-rf', remote], check=True, stdout=subprocess.DEVNULL)
