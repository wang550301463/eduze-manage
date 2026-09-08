#!/usr/bin/env python3
"""Export live application contracts over the internal authenticated route, without logging credentials."""
import argparse,json,pathlib,subprocess
parser=argparse.ArgumentParser();parser.add_argument('--env-file',default='docker/platform/.env.dev');args=parser.parse_args()
root=pathlib.Path(__file__).resolve().parents[1];target=root/'packages/contracts/openapi';target.mkdir(parents=True,exist_ok=True)
compose=['docker','compose','--env-file',str(root/args.env_file),'-f',str(root/'docker/platform/compose.yml')]
for name in ('identity','academic','teaching','portfolio','media','notification','engagement','commerce'):
    command=compose+['exec','-T','identity','sh','-c','curl -fsS --connect-timeout 5 --max-time 30 -H "X-Service-Name: identity" -H "X-Service-Token: $IDENTITY_SERVICE_TOKEN" http://'+name+':8080/internal/openapi']
    reply=subprocess.run(command,capture_output=True,check=False)
    if reply.returncode:raise SystemExit(f'Cannot export {name}; check service health and internal credentials.')
    schema=json.loads(reply.stdout);schema.pop('servers',None)
    if not schema.get('paths'):raise SystemExit(f'{name} returned no contract paths')
    (target/(name+'.json')).write_text(json.dumps(schema,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
    print(name+': '+str(len(schema['paths']))+' paths')
