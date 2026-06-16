#!/bin/sh
#
# 학습 종료: EC2를 0대로 줄이고 RDS를 정지한다 (terminate 아님, 데이터 보존).
#
# 사용법: sh aws-down.sh
#
set -eu
. "$(dirname "$0")/aws-common.sh"

check_auth
log "내리는 중... (EC2 먼저 축소 → RDS 정지)"

log "EC2(ASG) 용량을 0으로 축소"
awsx autoscaling update-auto-scaling-group \
  --auto-scaling-group-name "$ASG" \
  --min-size 0 --max-size 0 --desired-capacity 0
log "  → EC2 종료 진행 중 (인스턴스가 사라지기까지 몇 분 소요)"

ST="$(rds_status)"
if [ "$ST" = "available" ]; then
  log "RDS 정지 요청"
  awsx rds stop-db-instance --db-instance-identifier "$RDS_ID" >/dev/null
  log "  → RDS 정지 진행 중 (stopped 까지 몇 분 소요)"
else
  warn "RDS가 'available'이 아니라 정지를 건너뜀 (현재 상태: $ST)"
fi

log "완료. 현재 상태:"
print_status
warn "RDS는 최대 7일 후 AWS가 자동으로 다시 켭니다. 오래 안 쓰면 가끔 상태를 확인하세요."
