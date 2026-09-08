"""Deployment invariants: private databases, isolated credentials, no old volume reuse."""
import pathlib
import unittest
import yaml

ROOT = pathlib.Path(__file__).resolve().parents[2]


class PlatformTopologyTest(unittest.TestCase):
    def setUp(self):
        self.compose = yaml.safe_load((ROOT / 'docker/platform/compose.yml').read_text())

    def test_services_have_independent_database_and_credentials(self):
        databases = set()
        for name in ('identity', 'academic', 'teaching', 'portfolio', 'media', 'notification', 'engagement', 'commerce'):
            service = self.compose['services'][name]
            env = service['environment']
            databases.add(env['SPRING_DATASOURCE_URL'])
            self.assertEqual(env['SPRING_DATASOURCE_USERNAME'], 'eduze_' + name)
            self.assertIn(name.upper() + '_DB_PASSWORD', env['SPRING_DATASOURCE_PASSWORD'])
            self.assertNotIn('ports', service)
        self.assertEqual(len(databases), 8)

    def test_databases_and_internal_services_are_not_exposed(self):
        for name in ('mysql', 'redis', 'identity', 'academic', 'teaching', 'portfolio', 'media', 'notification', 'engagement', 'commerce'):
            self.assertNotIn('ports', self.compose['services'][name])

    def test_old_mysql_and_storage_volumes_are_not_reused(self):
        volumes = self.compose['volumes']
        self.assertNotIn('mysql_data', volumes)
        self.assertNotIn('../storage', str(self.compose))
        self.assertNotIn('container_name', str(self.compose))

    def test_gateway_refreshes_service_dns_after_independent_replacement(self):
        for filename in ('nginx.conf', 'nginx.prod.conf'):
            config = (ROOT / 'docker/platform' / filename).read_text()
            self.assertIn('resolver 127.0.0.11', config)
            for service in ('identity', 'academic', 'teaching', 'portfolio', 'media', 'notification', 'engagement', 'commerce', 'web'):
                self.assertIn('server ' + service + ':8080 resolve;', config)
                self.assertNotIn('proxy_pass http://' + service + ':8080;', config)


if __name__ == '__main__':
    unittest.main()
