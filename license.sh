#!/bin/bash

BASE_URL="http://localhost:8080/license"

licenses=(
    "Windows Server 2022 Standard"
    "Microsoft Office 365 Enterprise"
    "Oracle Database Enterprise Edition"
    "Red Hat Enterprise Linux"
    "VMware vSphere Enterprise Plus"
    "Adobe Creative Cloud All Apps"
    "Atlassian Jira Software"
    "JetBrains IntelliJ IDEA Ultimate"
    "Splunk Enterprise"
    "Tableau Desktop Professional"
)

for license in "${licenses[@]}"; do
  curl -X POST ${BASE_URL} \
       -H 'Content-Type: application/json' \
       -d "{\"licenseName\": \"${license}\"}"
  echo "Created: ${license}"
done

echo ""
echo "라이선스 10건 생성했습니다."