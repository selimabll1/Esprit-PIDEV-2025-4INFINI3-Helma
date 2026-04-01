# 🎯 Validation Intelligente des Transactions

## Résumé Exécutif

Quand vous créez une transaction, les champs de date (scheduledDate, nextExecutionDate) sont **validés intelligemment selon la périodicité**. 

✅ **Vous n'avez besoin que des champs nécessaires pour chaque mode.**

---

## 3️⃣ Modes de Périodicité

### 1️⃣ NOW (Immédiat) - Transaction Instantanée

**Mode par défaut, exécution immédiate**

| Paramètre | Status | Description |
|-----------|--------|-------------|
| periodicity | ✅ Requis | `"NOW"` |
| scheduledDate | ❌ Ignoré | Sera ignoré/nettoyé par le serveur |
| nextExecutionDate | ❌ Ignoré | Sera ignoré/nettoyé par le serveur |
| amount | ✅ Requis | Montant > 0 |
| beneficiaryRib | ✅ Requis | Compte destinataire |

**Logique Backend:**
```
☑️ Montant débité IMMÉDIATEMENT du compte source
☑️ Montant crédité IMMÉDIATEMENT au bénéficiaire
☑️ Statut = CONFIRMED
☑️ confirmedAt = LocalDateTime.now()
☑️ Toutes dates ignorées = null
```

**Exemple Request JSON ✅ (CORRECT):**
```json
{
  "beneficiaryName": "MAHMOUD",
  "beneficiaryRib": "12233455TNZ",
  "amount": 300,
  "type": "EXTERNAL",
  "category": "MASROUF",
  "description": "Versement salaire",
  "periodicity": "NOW"
}
```

**Exemple cURL ✅ (CORRECT):**
```bash
curl -X POST 'http://localhost:8082/helma/transactions/add/1' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
    "beneficiaryName": "MAHMOUD",
    "beneficiaryRib": "12233455TNZ",
    "amount": 300,
    "type": "EXTERNAL",
    "category": "MASROUF",
    "description": "Versement salaire",
    "periodicity": "NOW"
  }'
```

**Exemple Request JSON ❌ (INUTILE - sera ignoré):**
```json
{
  "beneficiaryName": "MAHMOUD",
  "beneficiaryRib": "12233455TNZ",
  "amount": 300,
  "type": "EXTERNAL",
  "category": "MASROUF",
  "description": "Versement salaire",
  "periodicity": "NOW",
  "scheduledDate": "2026-02-20T23:47:46.575Z",
  "nextExecutionDate": "2026-02-20T23:47:46.575Z"
}
```

⚠️ **Pourquoi éviter:** Les dates seront ignorées par le serveur (nettoyées = null). C'est du bruit inutile.

**Réponse Serveur 200 OK:**
```json
{
  "id": 101,
  "bankAccountId": 1,
  "beneficiaryName": "MAHMOUD",
  "beneficiaryRib": "12233455TNZ",
  "amount": 300,
  "type": "EXTERNAL",
  "category": "MASROUF",
  "description": "Versement salaire",
  "periodicity": "NOW",
  "status": "CONFIRMED",
  "scheduledDate": null,
  "nextExecutionDate": null,
  "lastExecutionDate": null,
  "createdAt": "2026-02-20T15:30:00",
  "confirmedAt": "2026-02-20T15:30:00"
}
```

---

### 2️⃣ SCHEDULED (Programmé) - Transaction Future

**Exécution à une date spécifique dans le FUTUR**

| Paramètre | Status | Description |
|-----------|--------|-------------|
| periodicity | ✅ Requis | `"SCHEDULED"` |
| **scheduledDate** | ✅ **OBLIGATOIRE** | Date d'exécution (ISO 8601, future) |
| nextExecutionDate | ❌ Ignoré | Sera ignoré/nettoyé par le serveur |
| amount | ✅ Requis | Montant > 0 |
| beneficiaryRib | ✅ Requis | Compte destinataire |

**Logique Backend:**
```
☑️ Montant NE PAS débité maintenant
☑️ Transaction reste PENDING
☑️ Montant débité à la date prévue (par scheduler)
☑️ scheduledDate = date d'exécution programmée
☑️ nextExecutionDate ignorée = null
☑️ status = PENDING (jusqu'à la date)
```

**Exemple Request JSON ✅ (CORRECT):**
```json
{
  "beneficiaryName": "Ahmed Bennour",
  "beneficiaryRib": "98765432TNZ",
  "amount": 1000,
  "type": "EXTERNAL",
  "category": "LOYER",
  "description": "Paiement loyer février 2026",
  "periodicity": "SCHEDULED",
  "scheduledDate": "2026-02-22T10:00:00"
}
```

**Exemple cURL ✅ (CORRECT):**
```bash
curl -X POST 'http://localhost:8082/helma/transactions/add/1' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
    "beneficiaryName": "Ahmed Bennour",
    "beneficiaryRib": "98765432TNZ",
    "amount": 1000,
    "type": "EXTERNAL",
    "category": "LOYER",
    "description": "Paiement loyer février 2026",
    "periodicity": "SCHEDULED",
    "scheduledDate": "2026-02-22T10:00:00"
  }'
```

**Erreur - scheduledDate manquante ❌:**
```bash
curl -X POST 'http://localhost:8082/helma/transactions/add/1' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
    "beneficiaryName": "Ahmed Bennour",
    "beneficiaryRib": "98765432TNZ",
    "amount": 1000,
    "type": "EXTERNAL",
    "category": "LOYER",
    "periodicity": "SCHEDULED"
  }'
```

**Réponse Serveur 400 Bad Request:**
```json
{
  "error": "❌ Mode SCHEDULED obligatoire: 'scheduledDate' est REQUISE\nExemple: { \"periodicity\": \"SCHEDULED\", \"scheduledDate\": \"2026-02-22T10:00:00\" }"
}
```

**Réponse Serveur 200 OK:**
```json
{
  "id": 102,
  "bankAccountId": 1,
  "beneficiaryName": "Ahmed Bennour",
  "beneficiaryRib": "98765432TNZ",
  "amount": 1000,
  "type": "EXTERNAL",
  "category": "LOYER",
  "description": "Paiement loyer février 2026",
  "periodicity": "SCHEDULED",
  "status": "PENDING",
  "scheduledDate": "2026-02-22T10:00:00",
  "nextExecutionDate": null,
  "lastExecutionDate": null,
  "createdAt": "2026-02-20T15:32:00",
  "confirmedAt": null
}
```

**Conditions d'exécution:**
- ⏰ Scheduler (à implémenter) vérifiera chaque jour si une transaction PENDING a atteint sa date
- 💰 À la date prévue: montant débité et crédité
- ✅ Status passe à CONFIRMED
- 📅 confirmedAt = date réelle d'exécution

---

### 3️⃣ PERMANENT (Récurrent) - Transaction Mensuelle

**Exécution immédiate + plans pour les exécutions futures mensuelles**

| Paramètre | Status | Description |
|-----------|--------|-------------|
| periodicity | ✅ Requis | `"PERMANENT"` |
| scheduledDate | ❌ Ignoré | Sera ignoré/nettoyé par le serveur |
| nextExecutionDate | 🟡 Optional | Prochaine exécution (défaut +30j) |
| amount | ✅ Requis | Montant > 0 |
| beneficiaryRib | ✅ Requis | Compte destinataire |

**Logique Backend:**
```
☑️ PREMIÈRE EXÉCUTION: Montant débité/crédité IMMÉDIATEMENT
☑️ Status = CONFIRMED
☑️ lastExecutionDate = LocalDateTime.now()
☑️ nextExecutionDate = date d'exécution suivante
  - Si fournie: utilise la valeur fournie
  - Si absente: défaut = LocalDateTime.now().plusDays(30)
☑️ confirmedAt = LocalDateTime.now()
```

**Exemple Request JSON ✅ (CORRECT - avec date):**
```json
{
  "beneficiaryName": "Orange Telecom",
  "beneficiaryRib": "11111111TNZ",
  "amount": 50,
  "type": "EXTERNAL",
  "category": "ABONNEMENT",
  "description": "Abonnement mobile récurrent",
  "periodicity": "PERMANENT",
  "nextExecutionDate": "2026-03-21T00:00:00"
}
```

**Exemple Request JSON ✅ (CORRECT - sans date, défaut auto):**
```json
{
  "beneficiaryName": "Orange Telecom",
  "beneficiaryRib": "11111111TNZ",
  "amount": 50,
  "type": "EXTERNAL",
  "category": "ABONNEMENT",
  "description": "Abonnement mobile récurrent",
  "periodicity": "PERMANENT"
}
```

**Exemple cURL ✅ (CORRECT - avec date):**
```bash
curl -X POST 'http://localhost:8082/helma/transactions/add/1' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
    "beneficiaryName": "Orange Telecom",
    "beneficiaryRib": "11111111TNZ",
    "amount": 50,
    "type": "EXTERNAL",
    "category": "ABONNEMENT",
    "description": "Abonnement mobile récurrent",
    "periodicity": "PERMANENT",
    "nextExecutionDate": "2026-03-21T00:00:00"
  }'
```

**Exemple cURL ✅ (CORRECT - sans date, défaut auto):**
```bash
curl -X POST 'http://localhost:8082/helma/transactions/add/1' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
    "beneficiaryName": "Orange Telecom",
    "beneficiaryRib": "11111111TNZ",
    "amount": 50,
    "type": "EXTERNAL",
    "category": "ABONNEMENT",
    "description": "Abonnement mobile récurrent",
    "periodicity": "PERMANENT"
  }'
```

**Réponse Serveur 200 OK (avec date fournie):**
```json
{
  "id": 103,
  "bankAccountId": 1,
  "beneficiaryName": "Orange Telecom",
  "beneficiaryRib": "11111111TNZ",
  "amount": 50,
  "type": "EXTERNAL",
  "category": "ABONNEMENT",
  "description": "Abonnement mobile récurrent",
  "periodicity": "PERMANENT",
  "status": "CONFIRMED",
  "scheduledDate": null,
  "nextExecutionDate": "2026-03-21T00:00:00",
  "lastExecutionDate": "2026-02-20T15:35:00",
  "createdAt": "2026-02-20T15:35:00",
  "confirmedAt": "2026-02-20T15:35:00"
}
```

**Réponse Serveur 200 OK (sans date, défaut +30j):**
```json
{
  "id": 104,
  "bankAccountId": 1,
  "beneficiaryName": "Orange Telecom",
  "beneficiaryRib": "11111111TNZ",
  "amount": 50,
  "type": "EXTERNAL",
  "category": "ABONNEMENT",
  "description": "Abonnement mobile récurrent",
  "periodicity": "PERMANENT",
  "status": "CONFIRMED",
  "scheduledDate": null,
  "nextExecutionDate": "2026-03-22T15:35:00",
  "lastExecutionDate": "2026-02-20T15:35:00",
  "createdAt": "2026-02-20T15:35:00",
  "confirmedAt": "2026-02-20T15:35:00"
}
```

**Cycle de vie PERMANENT:**
```
Jour 1: Transaction créée
  - lastExecutionDate = 2026-02-20 15:35
  - nextExecutionDate = 2026-03-22 15:35 (calculé)
  
Jour 31 (2026-03-22): Scheduler exécute la 2e fois
  - montant débité/crédité
  - lastExecutionDate = 2026-03-22 15:35
  - nextExecutionDate = 2026-04-21 15:35 (calculé +30j)
  
Jour 61 (2026-04-21): Scheduler exécute la 3e fois
  - ...et ainsi de suite chaque mois
```

---

## 📊 Tableau Comparatif

| Feature | NOW | SCHEDULED | PERMANENT |
|---------|-----|-----------|-----------|
| **Périodicité** | Immédiat | Une fois future | Mensuel |
| **scheduledDate** | ❌ Ignoré | ✅ OBLIGATOIRE | ❌ Ignoré |
| **nextExecutionDate** | ❌ Ignoré | ❌ Ignoré | 🟡 Optionnel (défaut +30j) |
| **Exécution 1ère** | Immédiat | À la date | Immédiat |
| **Statut créé** | CONFIRMED | PENDING | CONFIRMED |
| **confirmedAt** | Maintenant | null | Maintenant |
| **Solde requis** | ✅ Oui | ❌ Non | ✅ Oui |
| **Montant débité** | Tout de suite | À la date programmée | Immédiat + mensuel |

---

## 🔴 Erreurs Courantes

### Erreur 1: scheduledDate manquante en SCHEDULED
```bash
❌ FAUX:
curl -X POST 'http://localhost:8082/helma/transactions/add/1' \
  -d '{"periodicity": "SCHEDULED", ...}'

✅ CORRECT:
curl -X POST 'http://localhost:8082/helma/transactions/add/1' \
  -d '{"periodicity": "SCHEDULED", "scheduledDate": "2026-02-22T10:00:00", ...}'
```

**Message d'erreur:**
```
❌ Mode SCHEDULED obligatoire: 'scheduledDate' est REQUISE
Exemple: { "periodicity": "SCHEDULED", "scheduledDate": "2026-02-22T10:00:00" }
```

### Erreur 2: Date passée en SCHEDULED
```bash
❌ FAUX:
curl -X POST ... -d '{"periodicity": "SCHEDULED", "scheduledDate": "2020-02-20T10:00:00", ...}'

✅ CORRECT:
curl -X POST ... -d '{"periodicity": "SCHEDULED", "scheduledDate": "2026-02-22T10:00:00", ...}'
```

**Message d'erreur:**
```
La date programmée doit être dans le futur
```

### Erreur 3: Inclure des dates inutiles en NOW
```bash
⚠️ INUTILE (mais pas d'erreur):
curl -X POST -d '{
  "periodicity": "NOW",
  "scheduledDate": "2026-02-22T10:00:00",
  "nextExecutionDate": "2026-03-22T10:00:00"
}'

✅ PROPRE:
curl -X POST -d '{
  "periodicity": "NOW"
}'
```

**Comportement:** Les dates seront nettoyées (= null) par le serveur. Pas d'erreur, mais c'est du bruit inutile.

---

## 🎯 Règles de Validation Résumées

1. **NOW:**
   - ✅ Aucune date requise
   - ℹ️ Les dates fournies sont ignorées
   - ⚡ Exécution immédiate garantie

2. **SCHEDULED:**
   - ✅ `scheduledDate` OBLIGATOIRE et future
   - ℹ️ `nextExecutionDate` ignorée
   - 📅 Exécution à la date programmée

3. **PERMANENT:**
   - ✅ `nextExecutionDate` OPTIONNELLE (défaut +30j)
   - ℹ️ `scheduledDate` ignorée
   - 🔄 Exécution immédiate + plans futurs

---

## 🚀 Tester Maintenant

### Test 1: NOW (immédiat)
```bash
curl -X POST 'http://localhost:8082/helma/transactions/add/1' \
  -H 'Content-Type: application/json' \
  -d '{
    "beneficiaryName": "Test NOW",
    "beneficiaryRib": "12233455TNZ",
    "amount": 100,
    "type": "EXTERNAL",
    "periodicity": "NOW"
  }'
```

### Test 2: SCHEDULED (programmé)
```bash
curl -X POST 'http://localhost:8082/helma/transactions/add/1' \
  -H 'Content-Type: application/json' \
  -d '{
    "beneficiaryName": "Test SCHEDULED",
    "beneficiaryRib": "98765432TNZ",
    "amount": 200,
    "type": "EXTERNAL",
    "periodicity": "SCHEDULED",
    "scheduledDate": "2026-02-25T10:00:00"
  }'
```

### Test 3: PERMANENT (récurrent)
```bash
curl -X POST 'http://localhost:8082/helma/transactions/add/1' \
  -H 'Content-Type: application/json' \
  -d '{
    "beneficiaryName": "Test PERMANENT",
    "beneficiaryRib": "11111111TNZ",
    "amount": 50,
    "type": "EXTERNAL",
    "periodicity": "PERMANENT"
  }'
```

---

**Dernière mise à jour:** Février 2026  
**Version API:** 1.0.0  
**Validation Intelligente:** ✅ Activée
