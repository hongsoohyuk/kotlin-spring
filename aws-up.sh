#!/bin/sh
#
# 학습 시작: RDS를 켜고(완료 대기) EC2를 복구한다.
#
# 사용법: sh aws-up.sh
#
set -eu
. "$(dirname "$0")/aws-common.sh"

check_auth
log "올리는 중... (RDS 먼저 시작 → 완료 대기 → EC2 복구)"

ST="$(rds_status)"
if [ "$ST" = "stopped" ]; then
  log "RDS 시작 요청"
  awsx rds start-db-instance --db-instance-identifier "$RDS_ID" >/dev/null
elif [ "$ST" = "available" ]; then
  log "RDS가 이미 available"
else
  warn "RDS 상태가 '$ST' 입니다. available 될 때까지 대기합니다."
fi

log "RDS가 available 될 때까지 대기 중..."
awsx rds wait db-instance-available --db-instance-identifier "$RDS_ID"
log "  → RDS available"

log "EC2(ASG) 용량을 ${DESIRED_UP}로 복구"
awsx autoscaling update-auto-scaling-group \
  --auto-scaling-group-name "$ASG" \
  --min-size "$DESIRED_UP" --max-size "$DESIRED_UP" --desired-capacity "$DESIRED_UP"

log "완료. 인스턴스 부팅 + 앱 기동까지 몇 분 더 걸립니다."
print_status
