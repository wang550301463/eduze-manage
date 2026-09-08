#!/usr/bin/env python3
"""Inspect dead events, or explicitly retry one after fixing its cause. Never changes the immutable envelope."""
import argparse, datetime, json, os, pathlib, re, subprocess
names=('identity','academic','teaching','portfolio','media','notification','engagement','commerce')
parser=argparse.ArgumentParser();parser.add_argument('service',choices=names);parser.add_argument('--env-file',default='docker/platform/.env.dev');parser.add_argument('--retry');parser.add_argument('--reason');args=parser.parse_args()
root=pathlib.Path(__file__).resolve().parents[1]
if args.retry and (not re.fullmatch('[a-zA-Z0-9-]{1,36}',args.retry) or not args.reason or not args.reason.strip()):raise SystemExit('Retry requires an exact event ID and a written reason after the root cause has been fixed.')
compose=['docker','compose','--env-file',str(root/args.env_file),'-f',str(root/'docker/platform/compose.yml')]
command=compose+['exec','-T','mysql','sh','-c','MYSQL_PWD="$'+args.service.upper()+'_DB_PASSWORD" exec mysql -ueduze_'+args.service+' eduze_'+args.service+' --batch --skip-column-names']
if args.retry:
    sql="UPDATE platform_outbox SET status='PENDING',attempts=0,next_attempt_at=CURRENT_TIMESTAMP,lease_until=NULL WHERE event_id='"+args.retry+"' AND status='DEAD'; SELECT ROW_COUNT();"
else:sql="SELECT event_id,target,status,attempts,created_at FROM platform_outbox WHERE status='DEAD' ORDER BY created_at LIMIT 100;"
reply=subprocess.run(command,input=sql,text=True,capture_output=True)
if reply.returncode:raise SystemExit('Outbox maintenance failed; no credentials or event payloads were printed.')
if args.retry:
    output=root/'logs/platform-maintenance.jsonl';output.parent.mkdir(parents=True,exist_ok=True,mode=0o700)
    record={'at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'operator':os.environ.get('USER','unknown'),'service':args.service,'eventId':args.retry,'action':'REQUEUE_DEAD','changed':reply.stdout.strip()=='1','reason':args.reason.strip()[:500]}
    fd=os.open(output,os.O_WRONLY|os.O_CREAT|os.O_APPEND,0o600)
    with os.fdopen(fd,'a') as stream:stream.write(json.dumps(record,ensure_ascii=False)+'\n')
    print('Event requeued with original event ID.' if record['changed'] else 'No change: event is missing or is not DEAD.')
else:print(reply.stdout,end='')
