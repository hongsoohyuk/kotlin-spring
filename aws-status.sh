#!/bin/sh
#
# 현재 EC2/RDS 상태 확인.
#
# 사용법: sh aws-status.sh
#
set -eu
. "$(dirname "$0")/aws-common.sh"

check_auth
print_status
