#!/bin/sh
#
# fc-board AWS 학습 환경 제어 - 공통 설정/함수
#
# 단독 실행용이 아니라 aws-up.sh / aws-down.sh / aws-status.sh 에서 `.` 으로 불러 씀.
# POSIX sh 호환 (bash 전용 문법 사용 안 함).
#
# 설정값은 아래 기본값을 쓰되, 환경변수로 덮어쓸 수 있다.

PROFILE="${AWS_PROFILE_OVERRIDE:-personal}"
REGION="${AWS_REGION_OVERRIDE:-ap-northeast-2}"
EB_ENV="${EB_ENV:-Fc-board-env}"
ASG="${ASG:-awseb-e-ktqy8wyr8t-stack-AWSEBAutoScalingGroup-sGFwARYs0Ngs}"
RDS_ID="${RDS_ID:-awseb-e-ktqy8wyr8t-stack-awsebrdsdatabase-zuohwxi1mqel}"
DESIRED_UP="${DESIRED_UP:-1}"   # up 시 EC2 대수

# aws 호출 래퍼 (프로필/리전 고정)
awsx() { aws --profile "$PROFILE" --region "$REGION" "$@"; }

log()  { printf '\033[1;34m[%s]\033[0m %s\n' "$(date '+%H:%M:%S')" "$*"; }
warn() { printf '\033[1;33m[warn]\033[0m %s\n' "$*"; }
err()  { printf '\033[1;31m[error]\033[0m %s\n' "$*" >&2; }

rds_status() {
  awsx rds describe-db-instances --db-instance-identifier "$RDS_ID" \
    --query 'DBInstances[0].DBInstanceStatus' --output text 2>/dev/null || echo "unknown"
}

asg_capacity() {
  awsx autoscaling describe-auto-scaling-groups --auto-scaling-group-names "$ASG" \
    --query 'AutoScalingGroups[0].{Min:MinSize,Max:MaxSize,Desired:DesiredCapacity,Running:length(Instances)}' \
    --output json 2>/dev/null || echo '{}'
}

check_auth() {
  if ! awsx sts get-caller-identity >/dev/null 2>&1; then
    err "AWS 자격증명이 유효하지 않습니다 (profile=$PROFILE). 키를 갱신한 뒤 다시 실행하세요."
    exit 1
  fi
}

print_status() {
  log "프로필=$PROFILE  리전=$REGION"
  log "EC2(ASG) 용량: $(asg_capacity)"
  log "RDS 상태: $(rds_status)"
}
