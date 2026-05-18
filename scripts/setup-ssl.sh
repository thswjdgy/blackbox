#!/bin/bash
# SSL 인증서 설정 스크립트
# 사용법: bash scripts/setup-ssl.sh [--domain your-domain.com]

DOMAIN=${2:-localhost}
SSL_DIR="./nginx/ssl"

mkdir -p "$SSL_DIR"

echo "=== SSL 인증서 생성 ==="
echo "도메인: $DOMAIN"

# 자체 서명 인증서 생성 (개발/데모용)
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout "$SSL_DIR/server.key" \
  -out    "$SSL_DIR/server.crt" \
  -subj "/C=KR/ST=Seoul/L=Seoul/O=Blackbox/CN=$DOMAIN" \
  2>/dev/null

echo "✅ 자체 서명 인증서 생성 완료: $SSL_DIR/"
echo ""
echo "실제 도메인 배포 시 Let's Encrypt 사용 권장:"
echo "  1. certbot 설치: apt install certbot"
echo "  2. 인증서 발급:  certbot certonly --standalone -d $DOMAIN"
echo "  3. 인증서 복사:  cp /etc/letsencrypt/live/$DOMAIN/fullchain.pem $SSL_DIR/server.crt"
echo "                   cp /etc/letsencrypt/live/$DOMAIN/privkey.pem   $SSL_DIR/server.key"
echo "  4. 스택 재시작:  docker compose -f docker-compose.prod.yml restart nginx"
