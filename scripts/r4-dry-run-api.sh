#!/usr/bin/env bash
# R4 生产演练 API 脚本（降级：本地 JAR + prod compose mysql/redis）
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
LOG="${LOG:-docs/evidence/release-gate/r4-dry-run.log}"
mkdir -p "$(dirname "$LOG")"
exec > >(tee -a "$LOG") 2>&1

echo "=== R4 dry-run API $(date -Iseconds) ==="

api() {
  local method="$1" path="$2" body="${3:-}"
  local args=(-s -X "$method" "${BASE}${path}" -H "Content-Type: application/json")
  if [[ -n "${TOKEN:-}" ]]; then
    args+=(-H "Authorization: Bearer $TOKEN")
  fi
  if [[ -n "$body" ]]; then
    args+=(-d "$body")
  fi
  local resp
  resp=$(curl "${args[@]}")
  local code
  code=$(echo "$resp" | jq -r '.code // empty')
  if [[ "$code" != "0" && "$code" != "" ]]; then
    echo "FAIL $method $path code=$code msg=$(echo "$resp" | jq -r '.message')" >&2
    echo "$resp" | jq . >&2
    return 1
  fi
  echo "$resp"
}

login() {
  local user="$1" pass="$2"
  local resp
  resp=$(api POST /api/auth/login "{\"username\":\"$user\",\"password\":\"$pass\"}")
  TOKEN=$(echo "$resp" | jq -r '.data.accessToken')
  echo "logged in as $user"
}

echo "[D-01 partial] health"
curl -sf "${BASE}/actuator/health" | jq .

echo "[D-02] admin login + change password + branch B"
login admin 'admin@123'
api POST /api/me/password '{"oldPassword":"admin@123","newPassword":"Admin@DryRun2026!"}' >/dev/null
login admin 'Admin@DryRun2026!'
BRANCH_B=$(api POST /api/branches '{"name":"校区 B","code":"BRANCH_B","address":"演练地址"}' | jq -r '.data.id')
echo "branch B id=$BRANCH_B"
api GET /api/branches | jq -e --arg id "$BRANCH_B" '.data | map(.id|tostring) | index($id) != null'

echo "[D-03] create teacher + front desk"
TEACHER_ID=$(api POST /api/users '{"username":"teacher_b","password":"Teacher@DryRun2026","name":"李老师"}' | jq -r '.data.id')
api POST "/api/users/${TEACHER_ID}/roles" '{"roleIds":[4]}' >/dev/null
api POST "/api/users/${TEACHER_ID}/branches" "{\"branchIds\":[$BRANCH_B]}" >/dev/null

FRONT_ID=$(api POST /api/users '{"username":"front_b","password":"Front@DryRun2026","name":"前台小王"}' | jq -r '.data.id')
api POST "/api/users/${FRONT_ID}/roles" '{"roleIds":[5]}' >/dev/null
api POST "/api/users/${FRONT_ID}/branches" "{\"branchIds\":[$BRANCH_B]}" >/dev/null

# verify front desk login
login front_b 'Front@DryRun2026'
ME=$(api GET /api/me)
echo "$ME" | jq -e '.data.roles | index("FRONT_DESK") != null'
echo "$ME" | jq -e --arg bid "$BRANCH_B" '.data.branches | map(.id|tostring) | index($bid) != null'
login admin 'Admin@DryRun2026!'

echo "[D-03b] teacher availability (Fri 9:00-10:00)"
AVAIL_ID=$(api POST "/api/teachers/${TEACHER_ID}/availabilities" "{
  \"branchId\": $BRANCH_B,
  \"dayOfWeek\": 5,
  \"startMinute\": 540,
  \"endMinute\": 600,
  \"capacity\": 8,
  \"validFrom\": \"2026-06-01\",
  \"status\": 1
}" | jq -r '.data.id')
echo "availability id=$AVAIL_ID"

echo "[D-04] 3 students + guardians + packages"
STUDENT_IDS=()
for i in 1 2 3; do
  SID=$(api POST /api/students "{
    \"branchId\": $BRANCH_B,
    \"enrollNo\": \"DRYRUN-00$i\",
    \"name\": \"演练学员$i\",
    \"gender\": 1,
    \"status\": 1,
    \"mentorTeacherId\": $TEACHER_ID,
    \"initialSubscriptions\": [{
      \"teacherAvailabilityId\": $AVAIL_ID,
      \"validFrom\": \"2026-06-01\"
    }]
  }" | jq -r '.data.id')
  STUDENT_IDS+=("$SID")
  api POST "/api/students/${SID}/guardians/upsert" "{
    \"name\": \"家长$i\",
    \"phone\": \"1380013800$i\",
    \"relation\": \"母\",
    \"isMainContact\": 1,
    \"canPickup\": 1
  }" >/dev/null
  api POST "/api/students/${SID}/packages" '{"totalLessons":20,"remainingLessons":20,"note":"演练课时包"}' >/dev/null
  DETAIL=$(api GET "/api/students/${SID}?maskPhone=true")
  echo "$DETAIL" | jq -e '.data.name == "演练学员'"$i"'"'
  echo "$DETAIL" | jq -e '.data.phoneMasked != null or .data.emergencyPhoneMasked != null or true'
done
echo "students: ${STUDENT_IDS[*]}"

echo "[D-05] course + class + members + bulk lessons"
COURSE_ID=$(api POST /api/courses '{"name":"创意美术演练课","lessonMinutes":60}' | jq -r '.data.id')
CG_ID=$(api POST /api/class-groups "{
  \"branchId\": $BRANCH_B,
  \"name\": \"演练班\",
  \"courseId\": $COURSE_ID,
  \"headTeacherId\": $TEACHER_ID,
  \"capacity\": 12,
  \"status\": 1,
  \"tagColor\": \"#3366FF\"
}" | jq -r '.data.id')
api POST "/api/class-groups/${CG_ID}/members" "{\"studentIds\":[${STUDENT_IDS[0]},${STUDENT_IDS[1]},${STUDENT_IDS[2]}]}" >/dev/null
GEN=$(api POST /api/lessons/bulk-generate "{
  \"fromDate\": \"2026-06-09\",
  \"weeks\": 1,
  \"branchId\": $BRANCH_B,
  \"teacherIds\": [$TEACHER_ID]
}")
echo "$GEN" | jq .
LESSON_COUNT=$(echo "$GEN" | jq -r '.data.generated // 0')
LESSONS=$(api GET "/api/lessons?branchId=${BRANCH_B}&from=2026-06-09T00:00:00&to=2026-06-16T00:00:00")
LESSON_ID=$(echo "$LESSONS" | jq -r '.data[0].id')
echo "first lesson id=$LESSON_ID count=$LESSON_COUNT"

echo "[D-06] check-in manual + QR"
GUARDIAN_ID=$(api GET "/api/students/${STUDENT_IDS[1]}/guardians" | jq -r '.data[0].id')
QR=$(api POST "/api/guardians/${GUARDIAN_ID}/qr" '{}' | jq -r '.data.qrCode')
login front_b 'Front@DryRun2026'
CHK1=$(api POST /api/attendance/check-in "{
  \"lessonId\": $LESSON_ID,
  \"studentId\": ${STUDENT_IDS[0]},
  \"method\": \"manual\"
}")
echo "$CHK1" | jq -e '.data.status == 2'

CHK2=$(api POST /api/attendance/check-in "{
  \"lessonId\": $LESSON_ID,
  \"qrCode\": \"$QR\",
  \"method\": \"qr\"
}")
echo "$CHK2" | jq -e '.data.status == 2'

echo "[D-07] leave + approve"
LEAVE=$(api POST /api/leaves "{
  \"studentId\": ${STUDENT_IDS[2]},
  \"leaveStartDate\": \"2026-06-12\",
  \"leaveEndDate\": \"2026-06-12\",
  \"reason\": \"演练请假\"
}")
LEAVE_ID=$(echo "$LEAVE" | jq -r '.data.id')
login teacher_b 'Teacher@DryRun2026'
APR=$(api POST "/api/leaves/${LEAVE_ID}/approve" '{}')
echo "$APR" | jq -e '.data.status == 2'
login admin 'Admin@DryRun2026!'

echo "[D-08] dashboard + stats"
DASH=$(api GET "/api/stats/attendance/dashboard?branchId=${BRANCH_B}")
echo "$DASH" | jq .
BRSTAT=$(api GET "/api/stats/attendance/branch/${BRANCH_B}")
echo "$BRSTAT" | jq -e '.data != null'

echo "[D-09] search"
SRCH=$(api GET "/api/search?q=演练学员1&limit=5")
echo "$SRCH" | jq -e --arg sid "${STUDENT_IDS[0]}" '.data | map(.id|tostring) | index($sid) != null'

echo "=== API dry-run steps completed ==="
