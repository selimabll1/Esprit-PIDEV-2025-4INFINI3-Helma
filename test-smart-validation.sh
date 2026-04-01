#!/bin/bash

# Helma Bank API - Smart Validation Tests (Bash)
# Exécutez ce script pour tester les 3 modes de périodicité
# Usage: chmod +x test-smart-validation.sh && ./test-smart-validation.sh

BASE_URL="http://localhost:8082/helma/transactions/add/1"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║ Helma Bank API - Smart Validation Tests                       ║${NC}"
echo -e "${CYAN}║ Test des 3 modes de periodicity: NOW, SCHEDULED, PERMANENT    ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════════╝${NC}"

# ─────────────────────────────────────────────────────────────────────
# TEST 1: NOW (IMMÉDIAT)
# ─────────────────────────────────────────────────────────────────────

echo -e "\n${CYAN}$(printf '=%.0s' {1..70})${NC}"
echo -e "${GREEN}TEST 1️⃣: NOW (Immédiat - aucune date requise)${NC}"
echo -e "${CYAN}$(printf '=%.0s' {1..70})${NC}"

PAYLOAD1='{
  "beneficiaryName": "Test NOW",
  "beneficiaryRib": "12233455TNZ",
  "amount": 100,
  "type": "EXTERNAL",
  "category": "TEST",
  "description": "Transaction immédiate",
  "periodicity": "NOW"
}'

echo -e "\n${YELLOW}📤 Request:${NC}"
echo -e "${PAYLOAD1}" | jq '.' || echo "$PAYLOAD1"

echo -e "\n${CYAN}⏳ Envoi...${NC}"
RESPONSE1=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: */*" \
  -d "$PAYLOAD1")

echo -e "\n${GREEN}✅ Response:${NC}"
echo "$RESPONSE1" | jq '.' 2>/dev/null || echo "$RESPONSE1"

# ─────────────────────────────────────────────────────────────────────
# TEST 2: SCHEDULED (PROGRAMMÉ)
# ─────────────────────────────────────────────────────────────────────

echo -e "\n${CYAN}$(printf '=%.0s' {1..70})${NC}"
echo -e "${GREEN}TEST 2️⃣: SCHEDULED (Programmé - date REQUISE)${NC}"
echo -e "${CYAN}$(printf '=%.0s' {1..70})${NC}"

FUTURE_DATE=$(date -d "+5 days" +"%Y-%m-%dT%H:%M:%S" 2>/dev/null || date -v+5d +"%Y-%m-%dT%H:%M:%S")

PAYLOAD2=$(cat <<EOF
{
  "beneficiaryName": "Test SCHEDULED",
  "beneficiaryRib": "98765432TNZ",
  "amount": 200,
  "type": "EXTERNAL",
  "category": "TEST",
  "description": "Transaction programmée",
  "periodicity": "SCHEDULED",
  "scheduledDate": "$FUTURE_DATE"
}
EOF
)

echo -e "\n${YELLOW}📤 Request (date future: $FUTURE_DATE):${NC}"
echo -e "${PAYLOAD2}" | jq '.' || echo "$PAYLOAD2"

echo -e "\n${CYAN}⏳ Envoi...${NC}"
RESPONSE2=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: */*" \
  -d "$PAYLOAD2")

echo -e "\n${GREEN}✅ Response:${NC}"
echo "$RESPONSE2" | jq '.' 2>/dev/null || echo "$RESPONSE2"

# ─────────────────────────────────────────────────────────────────────
# TEST 3: SCHEDULED SANS DATE (ERREUR ATTENDUE)
# ─────────────────────────────────────────────────────────────────────

echo -e "\n${CYAN}$(printf '=%.0s' {1..70})${NC}"
echo -e "${RED}TEST 3️⃣: SCHEDULED SANS DATE (Erreur attendue 400)${NC}"
echo -e "${CYAN}$(printf '=%.0s' {1..70})${NC}"

PAYLOAD3='{
  "beneficiaryName": "Test ERROR",
  "beneficiaryRib": "98765432TNZ",
  "amount": 200,
  "type": "EXTERNAL",
  "periodicity": "SCHEDULED"
}'

echo -e "\n${YELLOW}📤 Request (SANS scheduledDate):${NC}"
echo -e "${PAYLOAD3}" | jq '.' || echo "$PAYLOAD3"

echo -e "\n${CYAN}⏳ Envoi...${NC}"
RESPONSE3=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: */*" \
  -d "$PAYLOAD3")

echo -e "\n${RED}❌ Expected Error (400):${NC}"
echo "$RESPONSE3" | jq '.' 2>/dev/null || echo "$RESPONSE3"

# ─────────────────────────────────────────────────────────────────────
# TEST 4: PERMANENT (OPTIONNEL)
# ─────────────────────────────────────────────────────────────────────

echo -e "\n${CYAN}$(printf '=%.0s' {1..70})${NC}"
echo -e "${GREEN}TEST 4️⃣: PERMANENT (Récurrent - date OPTIONNELLE)${NC}"
echo -e "${CYAN}$(printf '=%.0s' {1..70})${NC}"

PAYLOAD4='{
  "beneficiaryName": "Test PERMANENT",
  "beneficiaryRib": "11111111TNZ",
  "amount": 50,
  "type": "EXTERNAL",
  "category": "TEST",
  "description": "Transaction permanente (sans date)",
  "periodicity": "PERMANENT"
}'

echo -e "\n${YELLOW}📤 Request (SANS nextExecutionDate):${NC}"
echo -e "${PAYLOAD4}" | jq '.' || echo "$PAYLOAD4"

echo -e "\n${CYAN}⏳ Envoi...${NC}"
RESPONSE4=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: */*" \
  -d "$PAYLOAD4")

echo -e "\n${GREEN}✅ Response:${NC}"
echo "$RESPONSE4" | jq '.' 2>/dev/null || echo "$RESPONSE4"

# ─────────────────────────────────────────────────────────────────────
# TEST 5: PERMANENT AVEC DATE
# ─────────────────────────────────────────────────────────────────────

echo -e "\n${CYAN}$(printf '=%.0s' {1..70})${NC}"
echo -e "${GREEN}TEST 5️⃣: PERMANENT AVEC DATE (Récurrent - date fournie)${NC}"
echo -e "${CYAN}$(printf '=%.0s' {1..70})${NC}"

FUTURE_DATE_PERM=$(date -d "+15 days" +"%Y-%m-%dT%H:%M:%S" 2>/dev/null || date -v+15d +"%Y-%m-%dT%H:%M:%S")

PAYLOAD5=$(cat <<EOF
{
  "beneficiaryName": "Test PERMANENT Custom",
  "beneficiaryRib": "22222222TNZ",
  "amount": 75,
  "type": "EXTERNAL",
  "category": "TEST",
  "description": "Transaction permanente (avec date)",
  "periodicity": "PERMANENT",
  "nextExecutionDate": "$FUTURE_DATE_PERM"
}
EOF
)

echo -e "\n${YELLOW}📤 Request (date future: $FUTURE_DATE_PERM):${NC}"
echo -e "${PAYLOAD5}" | jq '.' || echo "$PAYLOAD5"

echo -e "\n${CYAN}⏳ Envoi...${NC}"
RESPONSE5=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Accept: */*" \
  -d "$PAYLOAD5")

echo -e "\n${GREEN}✅ Response:${NC}"
echo "$RESPONSE5" | jq '.' 2>/dev/null || echo "$RESPONSE5"

# ─────────────────────────────────────────────────────────────────────
# RÉSUMÉ
# ─────────────────────────────────────────────────────────────────────

echo -e "\n${CYAN}$(printf '=%.0s' {1..70})${NC}"
echo -e "${CYAN}RÉSUMÉ DES TESTS${NC}"
echo -e "${CYAN}$(printf '=%.0s' {1..70})${NC}"
echo -e "\n${GREEN}✅ Tous les tests doivent montrer:${NC}"
echo -e "  1. NOW: status=CONFIRMED, pas de dates"
echo -e "  2. SCHEDULED: status=PENDING, scheduledDate présente"
echo -e "  3. SCHEDULED sans date: error 400 avec message"
echo -e "  4. PERMANENT: status=CONFIRMED, nextExecutionDate auto-calculée"
echo -e "  5. PERMANENT custom: status=CONFIRMED, nextExecutionDate fournie"

echo -e "\n${YELLOW}🛠️ En cas de problème:${NC}"
echo -e "  1. Vérifiez que l'app est en cours: http://localhost:8082"
echo -e "  2. Vérifiez que les comptes existent (IDs 1 et RIBs)"
echo -e "  3. Consultez les logs de l'app pour plus de détails"
echo -e "  4. Vérifiez le document API_SMART_VALIDATION.md"

echo -e "\n${CYAN}$(printf '=%.0s' {1..70})${NC}\n"
