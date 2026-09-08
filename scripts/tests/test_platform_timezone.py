"""All deployed services share SQL, JDBC and Chinese lesson wall-clock semantics."""
import pathlib
import unittest
import yaml

ROOT = pathlib.Path(__file__).resolve().parents[2]
SERVICES = ('identity', 'academic', 'teaching', 'portfolio', 'media',
            'notification', 'engagement', 'commerce')


class PlatformTimeZoneTest(unittest.TestCase):
    def test_sql_session_and_jvm_use_chinese_lesson_time(self):
        compose = yaml.safe_load((ROOT / 'docker/platform/compose.yml').read_text())
        for name in SERVICES:
            with self.subTest(service=name):
                env = compose['services'][name]['environment']
                self.assertIn('connectionTimeZone=Asia/Shanghai', env['SPRING_DATASOURCE_URL'])
                self.assertIn('forceConnectionTimeZoneToSession=true', env['SPRING_DATASOURCE_URL'])
                self.assertIn('-Duser.timezone=Asia/Shanghai', env['JAVA_TOOL_OPTIONS'])
                local = next(yaml.safe_load_all((ROOT / 'services' / (name + '-service') /
                                       'src/main/resources/application.yml').read_text()))
                self.assertIn('connectionTimeZone=Asia/Shanghai', local['spring']['datasource']['url'])
                self.assertIn('forceConnectionTimeZoneToSession=true', local['spring']['datasource']['url'])


if __name__ == '__main__':
    unittest.main()
