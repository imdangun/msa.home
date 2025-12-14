#!/bin/bash

KEYCLOAK_URL="http://localhost:8073"
REALM="msa-realm"
CLIENT_ID="gateway"
CLIENT_SECRET="dw5kxZk5D3psgZFVqiLb1AOMx6jOVojt"
USERNAME="testuser"
PASSWORD="test1234"
GATEWAY_URL="http://localhost:8072"

get_correlation_id() {
  powershell.exe -Command "[guid]::NewGuid().Guid" | tr -d '\r\n'
}

echo "🔐 Token 획득 중..."
TOKEN_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${USERNAME}" \
  -d "password=${PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=${CLIENT_ID}" \
  -d "client_secret=${CLIENT_SECRET}")

ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.access_token')

if [ "$ACCESS_TOKEN" == "null" ]; then
    echo "❌ Token 획득 실패"
    echo $TOKEN_RESPONSE | jq '.'
    exit 1
fi

echo "✅ Token 획득 성공"

# 1. License 목록
echo -e "\n📋 1. License 목록"
curl -H "Authorization: Bearer ${ACCESS_TOKEN}" \
     "${GATEWAY_URL}/license" | jq '.'

# 2. License 단건
CORRELATION_ID=$(get_correlation_id)
echo -e "\n📋 2. License 단건 (Correlation-Id: ${CORRELATION_ID})"
curl -H "Authorization: Bearer ${ACCESS_TOKEN}" \
     -H "Correlation-Id: ${CORRELATION_ID}" \
     "${GATEWAY_URL}/license/1" | jq '.'

# 3. Company 목록
echo -e "\n🏢 3. Company 목록"
curl -s -H "Authorization: Bearer ${ACCESS_TOKEN}" \
     "${GATEWAY_URL}/company" | jq '.'

# 4. Company + Licenses
CORRELATION_ID=$(get_correlation_id)
echo -e "\n🏢 4. Company + Licenses (FeignClient)"
curl -s -H "Authorization: Bearer ${ACCESS_TOKEN}" \
     -H "Correlation-Id: ${CORRELATION_ID}" \
     "${GATEWAY_URL}/company/1/licenses" | jq '.'

echo -e "\n✅ 테스트 완료"