# 🧪 SCÉNARIO DE TEST COMPLET - 2 Comptes + 2 Transactions

## 📋 Objectif

Créer un scénario réaliste:
1. ✅ Créer 2 comptes bancaires
2. ✅ Faire une transaction immédiate (NOW)
3. ✅ Faire une transaction programmée (SCHEDULED)

---

## 🚀 AVANT DE COMMENCER

**1. Démarrer l'application:**
```bash
mvn spring-boot:run
```

Attendez le message:
```
Started HelmaApplication in X.XXX seconds
```

**2. Ouvrir Swagger UI ou Postman**
```
http://localhost:8082/helma/swagger-ui/index.html
```

---

## 📝 ÉTAPE 1: Créer le Premier Compte (Ahmed)

### 📮 Requête POST:
```
POST http://localhost:8082/helma/accounts/add
Content-Type: application/json
```

### 📤 Body:
```json
{
  "userId": 1,
  "rib": "12345678901234567890123456",
  "accountType": "CHECKING",
  "currency": "TND"
}
```

### ✅ Réponse Attendue:
```json
{
  "id": 1,
  "userId": 1,
  "rib": "12345678901234567890123456",
  "balance": 0,
  "currency": "TND",
  "accountType": "CHECKING",
  "status": "ACTIVE",
  "createdAt": "2026-02-21T14:30:00"
}
```

**⚠️ Note l'ID du compte: `1`**

---

## 💰 ÉTAPE 2: Ajouter du Solde au Premier Compte (Créditer)

### 📮 Requête POST:
```
POST http://localhost:8082/helma/accounts/credit/1?amount=5000
```

### ✅ Réponse:
```json
{
  "id": 1,
  "balance": 5000,
  ...
}
```

**✅ Compte Ahmed a maintenant 5000 TND**

---

## 📝 ÉTAPE 3: Créer le Deuxième Compte (Fatima)

### 📮 Requête POST:
```
POST http://localhost:8082/helma/accounts/add
Content-Type: application/json
```

### 📤 Body:
```json
{
  "userId": 2,
  "rib": "98765432109876543210987654",
  "accountType": "CHECKING",
  "currency": "TND"
}
```

### ✅ Réponse Attendue:
```json
{
  "id": 2,
  "userId": 2,
  "rib": "98765432109876543210987654",
  "balance": 0,
  "currency": "TND",
  "accountType": "CHECKING",
  "status": "ACTIVE",
  "createdAt": "2026-02-21T14:35:00"
}
```

**⚠️ Note l'ID du compte: `2`**

---

## 💰 ÉTAPE 4: Ajouter du Solde au Deuxième Compte (Créditer)

### 📮 Requête POST:
```
POST http://localhost:8082/helma/accounts/credit/2?amount=2000
```

### ✅ Réponse:
```json
{
  "id": 2,
  "balance": 2000,
  ...
}
```

**✅ Compte Fatima a maintenant 2000 TND**

---

## 📊 RECAP - État des Comptes:

```
┌─────────────────────────────────────────┐
│ Ahmed (ID: 1)                           │
├─────────────────────────────────────────┤
│ RIB: 12345678901234567890123456        │
│ Solde: 5000 TND                         │
│ Status: ACTIVE                          │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ Fatima (ID: 2)                          │
├─────────────────────────────────────────┤
│ RIB: 98765432109876543210987654        │
│ Solde: 2000 TND                         │
│ Status: ACTIVE                          │
└─────────────────────────────────────────┘
```

---

## 💸 ÉTAPE 5: Transaction 1 - NOW (Immédiate)

**Scénario:** Ahmed veut envoyer 500 TND à Fatima MAINTENANT

### 📮 Requête POST:
```
POST http://localhost:8082/helma/transactions/add/1
Content-Type: application/json
```

### 📤 Body:
```json
{
  "beneficiaryName": "Fatima Zahra",
  "beneficiaryRib": "98765432109876543210987654",
  "amount": 500,
  "type": "EXTERNAL",
  "category": "Transfert Personnel",
  "description": "Paiement immédiat pour Fatima",
  "periodicity": "NOW"
}
```

### ✅ Réponse Attendue:
```json
{
  "id": 1,
  "bankAccountId": 1,
  "beneficiaryName": "Fatima Zahra",
  "beneficiaryRib": "98765432109876543210987654",
  "amount": 500,
  "type": "EXTERNAL",
  "category": "Transfert Personnel",
  "description": "Paiement immédiat pour Fatima",
  "status": "CONFIRMED",
  "periodicity": "NOW",
  "scheduledDate": null,
  "nextExecutionDate": null,
  "lastExecutionDate": null,
  "riskScore": 0,
  "createdAt": "2026-02-21T14:40:00",
  "confirmedAt": "2026-02-21T14:40:00"
}
```

### ✅ Vérification - Soldes Après Transaction 1:

**Ahmed (ID: 1):**
```
Avant: 5000 TND
Après: 4500 TND (débité de 500)
```

**Fatima (ID: 2):**
```
Avant: 2000 TND
Après: 2500 TND (crédité de 500)
```

---

## ⏰ ÉTAPE 6: Transaction 2 - SCHEDULED (Programmée)

**Scénario:** Fatima veut envoyer 1000 TND à Ahmed DEMAIN à 10h (transaction programmée)

### 📮 Requête POST:
```
POST http://localhost:8082/helma/transactions/add/2
Content-Type: application/json
```

### 📤 Body:
```json
{
  "beneficiaryName": "Ahmed Ben Ali",
  "beneficiaryRib": "12345678901234567890123456",
  "amount": 1000,
  "type": "EXTERNAL",
  "category": "Transfert",
  "description": "Paiement programmé pour demain",
  "periodicity": "SCHEDULED",
  "scheduledDate": "2026-02-22T10:00:00"
}
```

### ✅ Réponse Attendue:
```json
{
  "id": 2,
  "bankAccountId": 2,
  "beneficiaryName": "Ahmed Ben Ali",
  "beneficiaryRib": "12345678901234567890123456",
  "amount": 1000,
  "type": "EXTERNAL",
  "category": "Transfert",
  "description": "Paiement programmé pour demain",
  "status": "PENDING",
  "periodicity": "SCHEDULED",
  "scheduledDate": "2026-02-22T10:00:00",
  "nextExecutionDate": null,
  "lastExecutionDate": null,
  "riskScore": 0,
  "createdAt": "2026-02-21T14:45:00",
  "confirmedAt": null
}
```

### ⚠️ Important - Soldes APRÈS Transaction 2:

**Fatima (ID: 2):**
```
Après Trans1: 2500 TND
Après Trans2: 2500 TND (NON modifié - transaction PENDING)
```

**Ahmed (ID: 1):**
```
Après Trans1: 4500 TND
Restera: 4500 TND (jusqu'au 22 février)
```

**DEMAIN (22 Février à 10h):**
```
Ahmed: 4500 + 1000 = 5500 TND ✅
Fatima: 2500 - 1000 = 1500 TND ✅
```

---

## 📊 RÉSUMÉ COMPLET DU SCÉNARIO

### État Initial:
```
Ahmed:  0 TND
Fatima: 0 TND
```

### Après Crédit (Étape 2 & 4):
```
Ahmed:  5000 TND
Fatima: 2000 TND
```

### Après Transaction 1 (NOW - Immédiate):
```
Ahmed:  4500 TND (débité de 500)
Fatima: 2500 TND (crédité de 500)
Status: ✅ CONFIRMED (immédiatement)
```

### Après Transaction 2 (SCHEDULED - Programmée):
```
Ahmed:  4500 TND (changement demain)
Fatima: 2500 TND (changement demain)
Status: ⏳ PENDING (en attente du 22 février)

Le 22 Février à 10h:
Ahmed:  5500 TND (crédité de 1000)
Fatima: 1500 TND (débité de 1000)
Status: ✅ CONFIRMED
```

---

## 🔍 VÉRIFICATIONS PENDANT LE TEST

### Vérifier le Compte Ahmed:
```
GET http://localhost:8082/helma/accounts/get/1
```

Doit retourner:
```json
{
  "id": 1,
  "balance": 4500,
  ...
}
```

### Vérifier le Compte Fatima:
```
GET http://localhost:8082/helma/accounts/get/2
```

Doit retourner (après Trans1 seulement):
```json
{
  "id": 2,
  "balance": 2500,
  ...
}
```

### Lister toutes les Transactions d'Ahmed:
```
GET http://localhost:8082/helma/transactions/account/1
```

Doit retourner:
```json
[
  {
    "id": 1,
    "status": "CONFIRMED",
    "periodicity": "NOW",
    "amount": 500
  },
  {
    "id": 2,
    "status": "PENDING",
    "periodicity": "SCHEDULED",
    "amount": 1000
  }
]
```

### Lister toutes les Transactions de Fatima:
```
GET http://localhost:8082/helma/transactions/account/2
```

Doit retourner (transaction où elle est émetteur):
```json
[
  {
    "id": 2,
    "status": "PENDING",
    "periodicity": "SCHEDULED",
    "amount": 1000
  }
]
```

---

## ✅ CHECKLIST DE TEST

- [ ] **Compte 1 créé** (Ahmed, ID=1)
- [ ] **Compte 1 crédité** (5000 TND)
- [ ] **Compte 2 créé** (Fatima, ID=2)
- [ ] **Compte 2 crédité** (2000 TND)
- [ ] **Transaction 1 créée** (NOW, 500 TND)
- [ ] **Soldes mis à jour** après Transaction 1
  - [ ] Ahmed: 4500 TND
  - [ ] Fatima: 2500 TND
- [ ] **Transaction 2 créée** (SCHEDULED, 1000 TND)
- [ ] **Soldes NON modifiés** après Transaction 2
  - [ ] Ahmed: 4500 TND (pas changé)
  - [ ] Fatima: 2500 TND (pas changé)
- [ ] **Transaction 1 Status** = CONFIRMED
- [ ] **Transaction 2 Status** = PENDING
- [ ] **Transaction 2 scheduledDate** = 2026-02-22T10:00:00

---

## 🎯 RÉSULTATS ATTENDUS

### ✅ SUCCÈS si:
1. Les comptes sont créés avec les bons soldes
2. Transaction NOW est CONFIRMED immédiatement
3. Soldes débité/crédité après Transaction NOW
4. Transaction SCHEDULED est PENDING
5. Soldes NON modifiés après Transaction SCHEDULED
6. `confirmedAt` est NULL pour SCHEDULED
7. `scheduledDate` est correctement défini

### ❌ PROBLÈMES si:
- Les transactions échouent
- Les validations rejettent les RIB
- Les soldes ne changent pas (ou mal)
- Status incorrect

---

## 📸 SCREENSHOT ATTENDU dans Swagger UI

Après avoir exécuté tous les tests, vous devriez voir:

```
Accounts:
├─ Account 1 (Ahmed)
│  └─ ID: 1, RIB: 123456..., Balance: 4500, Status: ACTIVE
└─ Account 2 (Fatima)
   └─ ID: 2, RIB: 987654..., Balance: 2500, Status: ACTIVE

Transactions:
├─ Transaction 1
│  ├─ ID: 1
│  ├─ Status: CONFIRMED
│  ├─ Periodicity: NOW
│  ├─ Amount: 500
│  └─ confirmedAt: 2026-02-21T14:40:00
└─ Transaction 2
   ├─ ID: 2
   ├─ Status: PENDING
   ├─ Periodicity: SCHEDULED
   ├─ Amount: 1000
   ├─ scheduledDate: 2026-02-22T10:00:00
   └─ confirmedAt: null
```

---

## 🧪 BONUS: Test avec cURL (Alternative à Swagger/Postman)

### Créer Compte Ahmed:
```bash
curl -X POST http://localhost:8082/helma/accounts/add \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "rib": "12345678901234567890123456",
    "accountType": "CHECKING",
    "currency": "TND"
  }'
```

### Créditer Ahmed (5000 TND):
```bash
curl -X POST "http://localhost:8082/helma/accounts/credit/1?amount=5000"
```

### Créer Compte Fatima:
```bash
curl -X POST http://localhost:8082/helma/accounts/add \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 2,
    "rib": "98765432109876543210987654",
    "accountType": "CHECKING",
    "currency": "TND"
  }'
```

### Créditer Fatima (2000 TND):
```bash
curl -X POST "http://localhost:8082/helma/accounts/credit/2?amount=2000"
```

### Transaction 1 - NOW:
```bash
curl -X POST http://localhost:8082/helma/transactions/add/1 \
  -H "Content-Type: application/json" \
  -d '{
    "beneficiaryName": "Fatima Zahra",
    "beneficiaryRib": "98765432109876543210987654",
    "amount": 500,
    "type": "EXTERNAL",
    "category": "Transfert Personnel",
    "description": "Paiement immédiat",
    "periodicity": "NOW"
  }'
```

### Transaction 2 - SCHEDULED:
```bash
curl -X POST http://localhost:8082/helma/transactions/add/2 \
  -H "Content-Type: application/json" \
  -d '{
    "beneficiaryName": "Ahmed Ben Ali",
    "beneficiaryRib": "12345678901234567890123456",
    "amount": 1000,
    "type": "EXTERNAL",
    "category": "Transfert",
    "description": "Paiement programmé",
    "periodicity": "SCHEDULED",
    "scheduledDate": "2026-02-22T10:00:00"
  }'
```

### Vérifier les Comptes:
```bash
curl http://localhost:8082/helma/accounts/get/1
curl http://localhost:8082/helma/accounts/get/2
```

### Lister les Transactions:
```bash
curl http://localhost:8082/helma/transactions/account/1
curl http://localhost:8082/helma/transactions/account/2
```

---

## 🎓 Ce Que Vous Apprenez

✅ Comment créer 2 comptes
✅ Comment créditer des comptes
✅ Comment faire une transaction immédiate (NOW)
✅ Comment faire une transaction programmée (SCHEDULED)
✅ Comment vérifier les soldes
✅ Comment les statuts changent
✅ La différence NOW vs SCHEDULED

---

## 📞 Besoin d'Aide?

Si un test échoue:
1. **Vérifier les RIB** - Ils doivent être identiques
2. **Vérifier le format** - Les JSON doivent être corrects
3. **Vérifier les logs** - L'application affiche les erreurs
4. **Vérifier les IDs** - Utilisez les bons IDs retournés

---

**Version: 1.0**  
**Date: 2026-02-21**  
**Statut: ✅ Scénario Complet Prêt à Tester**
