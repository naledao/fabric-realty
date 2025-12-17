#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

BASE_COMPOSE="${REPO_ROOT}/application/docker-compose.yml"
SPRINGBOOT_OVERRIDE="${REPO_ROOT}/application/server/springbootserver/docker-compose.springboot.override.yml"

DOCKER_COMPOSE_CMD=""
if docker compose version &>/dev/null; then
  DOCKER_COMPOSE_CMD="docker compose"
elif command -v docker-compose &>/dev/null; then
  DOCKER_COMPOSE_CMD="docker-compose"
else
  echo "未找到 docker compose 或 docker-compose" >&2
  exit 1
fi

usage() {
  cat <<EOF
Usage:
  $(basename "$0") springboot   # 切到 Spring Boot 后端（build + up）
  $(basename "$0") go          # 切回 Go 后端（使用 application/docker-compose.yml 的镜像）
  $(basename "$0") ps          # 查看服务状态
EOF
}

action="${1:-}"
case "${action}" in
  springboot)
    cd "${REPO_ROOT}"
    ${DOCKER_COMPOSE_CMD} -f "${BASE_COMPOSE}" -f "${SPRINGBOOT_OVERRIDE}" up -d --build
    ;;
  go)
    cd "${REPO_ROOT}"
    ${DOCKER_COMPOSE_CMD} -f "${BASE_COMPOSE}" up -d
    ;;
  ps)
    cd "${REPO_ROOT}"
    ${DOCKER_COMPOSE_CMD} -f "${BASE_COMPOSE}" ps
    ;;
  *)
    usage
    exit 1
    ;;
esac

