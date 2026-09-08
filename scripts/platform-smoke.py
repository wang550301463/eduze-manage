#!/usr/bin/env python3
"""Real HTTP, eight-service development smoke. Creates labelled fixtures; never uses production adapters."""
import argparse, base64, concurrent.futures, datetime, json, pathlib, secrets, statistics, time, urllib.error, urllib.parse, urllib.request
parser=argparse.ArgumentParser();parser.add_argument('--env-file',default='docker/platform/.env.dev');parser.add_argument('--students-per-class',type=int,default=3);args=parser.parse_args()
root=pathlib.Path(__file__).resolve().parents[1]
env=dict(line.split('=',1) for line in (root/args.env_file).read_text().splitlines() if line and not line.startswith('#'))
base=env['PUBLIC_BASE_URL'].rstrip('/')
if env.get('PLATFORM_PROFILE')!='dev' or urllib.parse.urlparse(base).hostname not in ('localhost','127.0.0.1'):
    raise SystemExit('Smoke fixtures require a loopback development environment.')
if not 1<=args.students_per_class<=20:raise SystemExit('Use 1–20 students per class.')
timings=[]; token=None; stamp=str(int(time.time())); fixture={'createdAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'campuses':[]}
def request(method,path,body=None,auth=True,expected=200,headers=None):
    started=time.perf_counter();h={'Content-Type':'application/json',**(headers or {})}
    if auth and token:h['Authorization']='Bearer '+token
    data=json.dumps(body).encode() if body is not None else None
    try:
        with urllib.request.urlopen(urllib.request.Request(base+path,data=data,headers=h,method=method),timeout=30) as response: status=response.status;raw=response.read()
    except urllib.error.HTTPError as error:status=error.code;raw=error.read()
    timings.append((time.perf_counter()-started)*1000)
    if status!=expected:
        try:message=json.loads(raw).get('message','')
        except (ValueError,AttributeError):message='non-JSON response'
        raise AssertionError(f'{method} {path}: expected {expected}, got {status}: {message}')
    if status!=200:return None
    payload=json.loads(raw)
    if payload.get('code')!=0:raise AssertionError(f'{method} {path}: non-success envelope')
    return payload.get('data')
def create_user(name):
    password=secrets.token_hex(20)
    user=request('POST','/api/users',{'username':name+stamp,'password':password,'name':'验收 '+name})
    return str(user['id']),password
login=request('POST','/api/auth/login',{'username':env['BOOTSTRAP_ADMIN_USERNAME'],'password':env['BOOTSTRAP_ADMIN_PASSWORD']},False)
token=login['accessToken'];admin_token=token
parent_id,parent_password=create_user('parent')
parent_login=request('POST','/api/auth/login',{'username':'parent'+stamp,'password':parent_password},False)
parent_token=parent_login['accessToken']
today=datetime.date.today();first_day=today+datetime.timedelta(days=7)
course=request('POST','/api/courses',{'name':'E2E多课次绘画 '+stamp,'lessonMinutes':90,'ageMin':5,'ageMax':9})
course_id=str(course['id'])
png=base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII=')
def upload(branch):
    item=request('POST','/api/v1/media/uploads',{'branchId':branch,'purpose':'ARTWORK','fileName':'smoke.png','contentType':'image/png','size':len(png)})
    with urllib.request.urlopen(urllib.request.Request(item['uploadUrl'],data=png,headers={'Content-Type':'image/png',**item.get('headers',{})},method=item['method']),timeout=30) as response:
        if response.status not in (200,204):raise AssertionError('Upload failed')
    request('POST','/api/v1/media/uploads/'+item['id']+'/complete',{})
    return item['id']
for index in range(2):
    branch=request('POST','/api/branches',{'name':'E2E验收校区 '+stamp+'-'+str(index),'code':'E'+stamp+str(index),'status':1});branch_id=str(branch['id'])
    teacher_id,_=create_user('teacher'+str(index))
    request('POST','/api/users/'+teacher_id+'/roles',{'roleIds':['4']})
    request('POST','/api/users/'+teacher_id+'/branches',{'branchIds':[branch_id]})
    students=[]
    for number in range(args.students_per_class):
        child=request('POST','/api/students',{'branchId':branch_id,'enrollNo':'E'+stamp+str(index)+str(number),'name':'验收小画家 '+str(index)+'-'+str(number),'mentorTeacherId':teacher_id,'status':1})
        child_id=str(child['id']);students.append(child_id)
        request('POST','/api/students/'+child_id+'/packages',{'totalLessons':20,'remainingLessons':20,'courseId':course_id})
    invite=request('POST','/api/v1/academic/family/invites',{'studentId':students[0]});token=parent_token
    binding=request('POST','/api/v1/academic/family/bindings',{'code':invite['code']});token=admin_token
    request('POST','/api/v1/academic/family/bindings/'+binding['id']+'/approve',{})
    group=request('POST','/api/class-groups',{'branchId':branch_id,'name':'E2E固定班 '+stamp+str(index),'courseId':course_id,'headTeacherId':teacher_id,'nestedAvailability':{'teacherId':teacher_id,'branchId':branch_id,'dayOfWeek':first_day.isoweekday(),'startMinute':540,'endMinute':630,'capacity':30,'validFrom':today.isoformat(),'status':1}})
    group_id=str(group['id']);request('POST','/api/class-groups/'+group_id+'/members',{'studentIds':students})
    lessons=[]
    for week in range(4):
        day=first_day+datetime.timedelta(days=7*week)
        lesson=request('POST','/api/lessons',{'branchId':branch_id,'classGroupId':group_id,'teacherId':teacher_id,'startAt':day.isoformat()+'T09:00:00','endAt':day.isoformat()+'T10:30:00','source':2})
        lessons.append(str(lesson['id']))
    template=request('POST','/api/v1/teaching/templates',{'title':'三课次主题 '+stamp+str(index),'ageMin':5,'ageMax':9,'goals':'观察、构图、完成创作','materials':'画纸和彩笔','expectedLessons':3,'steps':[{'title':'第'+str(i+1)+'课','content':'观察并记录过程'} for i in range(3)],'tags':['验收'],'mediaIds':[]})
    published=request('POST','/api/v1/teaching/templates/'+template['id']+'/publish',{'version':template['version']})
    theme_body={'branchId':branch_id,'groupId':group_id,'templateVersionId':published['id'],'title':'班级三课次主题','lessonIds':lessons[:3],'handoffNote':'验收代课交接'}
    theme=request('POST','/api/v1/teaching/themes',theme_body)
    request('POST','/api/v1/teaching/plans',{'branchId':branch_id,'name':'验收教学计划','startsOn':today.isoformat(),'endsOn':(today+datetime.timedelta(days=180)).isoformat(),'themeIds':[theme['id']]})
    record_ids=[]
    for child_id in students:
        media=upload(branch_id)
        body={'themeId':theme['id'],'studentId':child_id,'progress':'IN_PROGRESS','classroomNote':'三节课共同主题','comment':'初次课效点评','artworks':[{'id':secrets.token_hex(8),'title':'多课次作品','kind':'FINAL','mediaIds':[media],'participantIds':[child_id],'story':'过程到成品'}],'audioMediaIds':[]}
        record=request('POST','/api/v1/portfolio/records',body);record_ids.append(record['id'])
        for lesson_id in lessons[:3]:
            entry=request('POST','/api/v1/portfolio/records/'+record['id']+'/entries',{'lessonId':lesson_id,'occurredAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'notes':'本课观察与创作记录','mediaIds':[media],'idempotencyKey':secrets.token_hex(16)})
            request('POST','/api/v1/portfolio/records/'+record['id']+'/entries/'+entry['id']+'/publish',{'idempotencyKey':secrets.token_hex(16)})
        final_body={**body,'version':record['version'],'progress':'COMPLETED'}
        record=request('PUT','/api/v1/portfolio/records/'+record['id'],final_body)
        command={'version':record['version'],'idempotencyKey':secrets.token_hex(16)}
        publication=request('POST','/api/v1/portfolio/records/'+record['id']+'/publish',command)
        replay=request('POST','/api/v1/portfolio/records/'+record['id']+'/publish',command)
        assert publication['id']==replay['id'],'Publication replay created another version'
        balances=request('GET','/api/students/'+child_id+'/packages');assert int(balances[0]['remainingLessons'])==20,'Portfolio publication consumed lessons'
        if child_id==students[0]:
            latest=request('GET','/api/v1/portfolio/records/'+record['id']);request('PUT','/api/v1/portfolio/records/'+record['id'],{**body,'version':latest['version'],'comment':'未发布的新草稿'})
            token=parent_token;household=request('GET','/api/v1/portfolio/records/'+record['id']);assert household['content']['comment']=='初次课效点评','Draft leaked to household'
            request('GET','/api/v1/academic/family/children/'+child_id+'/balance')
            links=request('POST','/api/v1/portfolio/records/'+record['id']+'/media-access',{'mediaIds':[media]})
            with urllib.request.urlopen(links['items'][0]['url'],timeout=10) as image:
                assert image.read()==png,'Authorized media content mismatch'
            deadline=time.monotonic()+45
            while True:
                messages=request('GET','/api/v1/notifications')
                if (any(message['businessId']==record['id'] for message in messages)
                        and any(message['businessId'] in lessons for message in messages)):break
                if time.monotonic()>deadline:raise AssertionError('Portfolio and lesson events did not both reach household inbox within 45 seconds')
                time.sleep(1)

            token=admin_token
            request('DELETE','/api/v1/academic/family/bindings/'+binding['id'])
            token=parent_token;request('GET','/api/v1/portfolio/records/'+record['id'],expected=403);token=admin_token
    # Extending this class cannot mutate the immutable shared template version.
    current=request('GET','/api/v1/teaching/themes/'+theme['id'])
    request('PUT','/api/v1/teaching/themes/'+theme['id'],{**theme_body,'version':current['version'],'lessonIds':lessons})
    assert request('GET','/api/v1/teaching/templates/'+template['id']+'/versions')[0]['content']['expectedLessons']==3
    fixture['campuses'].append({'branchId':branch_id,'groupId':group_id,'studentIds':students,'lessonIds':lessons,'themeId':theme['id'],'recordIds':record_ids})
    print('Verified campus '+str(index+1)+': '+str(len(students))+' students, 3 classroom updates, immutable publications, family authorization')
request('GET','/internal/openapi',auth=False,expected=404)
request('GET','/api/v1/media/uploads/unknown',auth=False,expected=401)
request('GET','/api/v1/engagement/public/studios',auth=False)
request('GET','/api/v1/commerce/public/products?branchId='+fixture['campuses'][0]['branchId'],auth=False)
fixture['http']={'requests':len(timings),'p50Ms':round(statistics.median(timings),2),'p95Ms':round(sorted(timings)[int(len(timings)*.95)-1],2),'errors':0,'scope':'sequential development smoke, not production capacity'}
output=root/'target/platform-smoke.json';output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(fixture,ensure_ascii=False,indent=2));print('Smoke passed; fixture IDs and measured timings: '+str(output))
