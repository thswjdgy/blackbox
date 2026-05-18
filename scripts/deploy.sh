#!/bin/bash
# 프로덕션 배포 스크립트
# 사용법: bash scripts/deploy.sh

set -e

echo "=== Team Blackbox 배포 시작 ==="

# 1. .env 파일 확인
if [ ! -f ".env" ]; then
  echo "❌ .env 파일이 없습니다. .env.example을 복사 후 값을 채워주세요."
  echo "   cp .env.example .env && nano .env"
  exit 1
fi

# 필수 환경변수 검증
source .env
if [ -z "$DB_PASSWORD" ] || [ "$DB_PASSWORD" = "change_me_strong_password" ]; then
  echo "❌ DB_PASSWORD를 설정해주세요 (.env)"
  exit 1
fi
if [ -z "$JWT_SECRET" ] || [ "$JWT_SECRET" = "change_me_at_least_32_chars_long_secret" ]; then
  echo "❌ JWT_SECRET을 설정해주세요 (.env)"
  exit 1
fi

# 2. SSL 인증서 확인/생성
if [ ! -f "./nginx/ssl/server.crt" ]; then
  echo "SSL 인증서가 없습니다. 자체 서명 인증서를 생성합니다..."
  bash scripts/setup-ssl.sh
fi

# 3. 빌드 및 기동
echo "Docker 이미지 빌드 중..."
docker compose -f docker-compose.prod.yml build --no-cache

echo "컨테이너 기동 중..."
docker compose -f docker-compose.prod.yml up -d

echo ""
echo "=== 배포 완료 ==="
echo "상태 확인: docker compose -f docker-compose.prod.yml ps"
echo "로그 확인: docker compose -f docker-compose.prod.yml logs -f backend"
