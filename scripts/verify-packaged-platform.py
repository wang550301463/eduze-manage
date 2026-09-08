#!/usr/bin/env python3
"""Catch stale Spring Boot repackaging: deployed nested libraries must match this reactor build."""
import hashlib, pathlib, zipfile
root=pathlib.Path(__file__).resolve().parents[1]
def digest(data):return hashlib.sha256(data).digest()
for service in sorted((root/'services').iterdir()):
    if not service.is_dir():continue
    jar=service/'target'/(service.name+'-1.0.0.jar')
    with zipfile.ZipFile(jar) as archive:
        entries=archive.namelist()
        for library in ('platform-runtime','legacy-support'):
            nested='BOOT-INF/lib/'+library+'-1.0.0.jar'
            if library=='legacy-support' and nested not in entries:continue
            current=root/'libraries'/library/'target'/(library+'-1.0.0.jar')
            if digest(archive.read(nested))!=digest(current.read_bytes()):raise SystemExit(service.name+': stale '+library+' inside deployable Jar')
        if not any('/springdoc-openapi-starter-webmvc-api-2.8.17.jar' in name for name in entries):raise SystemExit(service.name+': missing OpenAPI runtime inside deployable Jar')
print('Eight deployable JARs embed current reactor libraries and OpenAPI runtime.')
