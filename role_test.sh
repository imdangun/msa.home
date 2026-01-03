#!/bin/bash

# Keycloak 설정
KEYCLOAK_URL="http://localhost:8073"
REALM="msa-realm"
CLIENT_ID="gateway"
CLIENT_SECRET="dw5kxZk5D3psgZFVqiLb1AOMx6jOVojt"
LICENSE_ID="11"

# Gateway URL
GATEWAY_URL="http://localhost:8072"

# 일반 사용자 토큰 (USER 권한)
USER_TOKEN=$(curl -s -X POST \
  "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=testuser" \
  -d "password=test1234" \
  | jq -r .access_token)

# 관리자 토큰 (ADMIN 권한)
ADMIN_TOKEN=$(curl -s -X POST \
  "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=adminuser" \
  -d "password=admin1234" \
  | jq -r .access_token)

echo "=== GET /license (USER 권한) ==="
curl -v -H "Authorization: Bearer $USER_TOKEN" \
  $GATEWAY_URL/license/1

echo -e "\n\n=== POST /license (ADMIN 권한 필요) ==="
curl -v -X POST \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"New License"}' \
  $GATEWAY_URL/license

echo -e "\n\n=== PUT /license (ADMIN 권한 필요) ==="
curl -v -X PUT \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Updated License"}' \
  $GATEWAY_URL/license/$LICENSE_ID

echo -e "\n\n=== DELETE /license (ADMIN 권한 필요) ==="
curl -v -X DELETE \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  $GATEWAY_URL/license/$LICENSE_ID

echo -e "\n\n=== USER가 POST 시도 (403 예상) ==="
curl -v -X POST \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Unauthorized"}' \
  $GATEWAY_URL/license