#!/usr/bin/env python3
"""Bounded development load using two-campus smoke fixtures, never production traffic."""
import base64, concurrent.futures, datetime, hashlib, json, pathlib, random, statistics, struct, subprocess, time, urllib.error, urllib.parse, urllib.request, zlib
root=pathlib.Path(__file__).resolve().parents[1]
env=dict(line.split('=',1) for line in (root/'docker/platform/.env.dev').read_text().splitlines() if line and not line.startswith('#'))
base=env['PUBLIC_BASE_URL'].rstrip('/')
if env.get('PLATFORM_PROFILE')!='dev' or urllib.parse.urlparse(base).hostname not in ('localhost','127.0.0.1'):raise SystemExit('Loopback development load only.')
fixtures=json.loads((root/'target/platform-smoke.json').read_text())
if len(fixtures['campuses'])<2:raise SystemExit('Run successful two-campus smoke first.')
def http(method,path,body=None,token=None):
    headers={'Content-Type':'application/json'}
    if token:headers['Authorization']='Bearer '+token
    with urllib.request.urlopen(urllib.request.Request(base+path,data=None if body is None else json.dumps(body).encode(),headers=headers,method=method),timeout=30) as response:
        result=json.load(response)
    if result.get('code')!=0:raise ValueError('non-success envelope')
    return result['data']
token=http('POST','/api/auth/login',{'username':env['BOOTSTRAP_ADMIN_USERNAME'],'password':env['BOOTSTRAP_ADMIN_PASSWORD']})['accessToken']
# Valid incompressible RGB PNG fixture approximates a 3 MB student artwork; no user photos are used.
def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
rng=random.Random(20260908);width=1024;height=1024
pixels=b''.join(b'\0'+rng.randbytes(width*3) for _ in range(height))
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',width,height,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(pixels))+chunk(b'IEND',b'')
def measure(kind,index):
    started=time.perf_counter()
    try:
        campus=fixtures['campuses'][index%2]
        if kind=='read':
            http('GET','/api/v1/portfolio/records?themeId='+campus['themeId'],token=token)
        else:
            media=http('POST','/api/v1/media/uploads',{'branchId':campus['branchId'],'purpose':'ARTWORK','fileName':'load.png','contentType':'image/png','size':len(png)},token)
            with urllib.request.urlopen(urllib.request.Request(media['uploadUrl'],data=png,headers={'Content-Type':'image/png',**media.get('headers',{})},method=media['method']),timeout=30) as response:
                if response.status not in (200,204):raise ValueError('upload status')
            http('POST','/api/v1/media/uploads/'+media['id']+'/complete',{},token)
        return {'kind':kind,'ms':round((time.perf_counter()-started)*1000,2),'ok':True}
    except Exception as error:return {'kind':kind,'ms':round((time.perf_counter()-started)*1000,2),'ok':False,'errorType':type(error).__name__}
started=time.perf_counter()
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
    rows=list(executor.map(lambda n:measure('upload' if n<16 else 'read',n),range(216)))
result={'at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'concurrency':8,'durationSeconds':round(time.perf_counter()-started,2),'fixtureBytes':len(png),'campuses':2,'classes':2,'scope':'local Docker Desktop, synthetic PNG; not a production capacity claim','kinds':{}}
for kind in ('read','upload'):
    samples=[r for r in rows if r['kind']==kind];values=sorted(r['ms'] for r in samples)
    result['kinds'][kind]={'count':len(samples),'errors':sum(not r['ok'] for r in samples),'p50Ms':statistics.median(values),'p95Ms':values[int(len(values)*.95)-1],'maxMs':max(values)}
compose=['docker','compose','--env-file',str(root/'docker/platform/.env.dev'),'-f',str(root/'docker/platform/compose.yml')]
containers=subprocess.check_output(compose+['ps','-q'],text=True).splitlines()
result['containers']=[]
if containers:
    stats=subprocess.check_output(['docker','stats','--no-stream','--format','{{json .}}']+containers,text=True)
    result['containers']=[json.loads(line) for line in stats.splitlines()]
result['metrics']={}
for service in ('identity','academic','teaching','portfolio','media','notification','engagement','commerce'):
    command=compose+['exec','-T',service,'curl','-fsS','--max-time','10','--config','-']
    # Curl reads the header from stdin; the token never appears in process arguments or reports.
    metrics=subprocess.run(command,input='header = "Authorization: Bearer '+token+'"\nurl = "http://localhost:8080/actuator/prometheus"\n',capture_output=True,text=True)
    prefixes=('hikaricp_connections_active','hikaricp_connections_idle','hikaricp_connections_max','eduze_outbox_events','eduze_outbox_oldest_seconds','jvm_memory_used_bytes')
    result['metrics'][service]=[line for line in metrics.stdout.splitlines() if line.startswith(prefixes)] if metrics.returncode==0 else ['METRICS_UNAVAILABLE']
(root/'target/platform-load.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
print(json.dumps({k:result[k] for k in ('concurrency','durationSeconds','fixtureBytes','kinds')},ensure_ascii=False))
if any(not r['ok'] for r in rows):raise SystemExit('Load failed; see target/platform-load.json for safe error counts.')
