## 📱 Guide des Transactions avec Périodicité

Ce document décrit comment utiliser les trois types de transactions avec la nouvelle fonctionnalité de **Périodicité**.

---

## 🎯 Types de Transactions

### 1️⃣ **NOW (Maintenant)** - Transaction Immédiate

**Description**: Les fonds sont transférés **IMMÉDIATEMENT**. La transaction est confirmée tout de suite.

**Exemple de Requête**:
```json
POST /transactions/add/1
Content-Type: application/json

{
  "beneficiaryName": "Ahmed Ben Ali",
  "beneficiaryRib": "12345678901234567890123456",
  "amount": 500,
  "type": "EXTERNAL",
  "category": "Transfert",
  "description": "Paiement facture",
  "periodicity": "NOW"
}
```

**Statut Retourné**: `CONFIRMED`
**confirmed_at**: La date/heure actuelle
**Soldes**: Débité et crédité **IMMÉDIATEMENT**

**Cas d'usage**: 
- Transferts urgents
- Paiements immédiats
- Virements instantanés

---

### 2️⃣ **SCHEDULED (Programmé)** - Transaction Future

**Description**: Les fonds seront transférés à une **date spécifique dans le futur**. La transaction reste en PENDING jusqu'à cette date.

**Exemple de Requête**:
```json
POST /transactions/add/1
Content-Type: application/json

{
  "beneficiaryName": "Fatima Zahra",
  "beneficiaryRib": "98765432109876543210987654",
  "amount": 1000,
  "type": "EXTERNAL",
  "category": "Facture",
  "description": "Paiement loyer février",
  "periodicity": "SCHEDULED",
  "scheduledDate": "2026-03-15T10:30:00"
}
```

**Statut Retourné**: `PENDING`
**confirmed_at**: `null` (sera mis à jour à la date programmée)
**Soldes**: **NON débité/crédité maintenant** (le sera à la date prévue)
**scheduled_date**: 2026-03-15T10:30:00

**Cas d'usage**:
- Paiement de salaire à date future
- Paiement de facture à échéance
- Virements programmés

---

### 3️⃣ **PERMANENT (Permanent)** - Transaction Récurrente

**Description**: La transaction est **exécutée immédiatement**, puis **récurrente** à chaque intervalle (défaut: 30 jours).

**Exemple de Requête**:
```json
POST /transactions/add/1
Content-Type: application/json

{
  "beneficiaryName": "Orange Tunisie",
  "beneficiaryRib": "11111111111111111111111111",
  "amount": 50,
  "type": "EXTERNAL",
  "category": "Abonnement",
  "description": "Abonnement Internet mensuel",
  "periodicity": "PERMANENT",
  "nextExecutionDate": "2026-03-21T00:00:00"
}
```

**Statut Retourné**: `CONFIRMED`
**confirmed_at**: La date/heure actuelle (première exécution)
**lastExecutionDate**: La date de la dernière exécution (première exécution = maintenant)
**nextExecutionDate**: 2026-03-21T00:00:00 (prochaine exécution)
**Soldes**: Débité et crédité **IMMÉDIATEMENT** (première exécution)

**Cas d'usage**:
- Abonnements mensuels
- Virements automatiques
- Paiements récurrents
- Allocations familiales

---

## 📊 Comparaison des Trois Types

| Feature | NOW | SCHEDULED | PERMANENT |
|---------|-----|-----------|-----------|
| **Exécution** | Immédiate | À la date prévue | Immédiate + Récurrent |
| **Statut Initial** | CONFIRMED | PENDING | CONFIRMED |
| **Soldes Affectés** | ✅ Maintenant | ❌ À la date future | ✅ Maintenant |
| **confirmed_at** | Maintenant | Futur | Maintenant |
| **scheduledDate** | ❌ Ignoré | ✅ Obligatoire | ❌ Ignoré |
| **nextExecutionDate** | ❌ Ignoré | ❌ N/A | ✅ Optionnel (défaut: +30j) |
| **lastExecutionDate** | ❌ Ignoré | ❌ N/A | ✅ Mis à jour |

---

## 🔄 Cycle de Vie d'une Transaction

### NOW (Immédiate)
```
Créée (NOW)
  ↓
✅ CONFIRMED (maintenant)
  ↓
Soldes mis à jour (immédiatement)
```

### SCHEDULED (Programmée)
```
Créée (SCHEDULED)
  ↓
⏳ PENDING (en attente de date)
  ↓
[À la date prévue...]
  ↓
✅ CONFIRMED (+ soldes mis à jour)
```

### PERMANENT (Permanente)
```
Créée (PERMANENT)
  ↓
✅ CONFIRMED (première exécution)
  ↓
Soldes mis à jour (immédiatement)
  ↓
⏳ En attente (prochaine exécution)
  ↓
[À chaque nextExecutionDate...]
  ↓
✅ RE-EXÉCUTÉE + soldes remis à jour
```

---

## 📋 Réponses Typiques

### Réponse NOW:
```json
{
  "id": 1,
  "bankAccountId": 1,
  "beneficiaryName": "Ahmed Ben Ali",
  "beneficiaryRib": "12345678901234567890123456",
  "amount": 500,
  "type": "EXTERNAL",
  "category": "Transfert",
  "description": "Paiement facture",
  "status": "CONFIRMED",
  "periodicity": "NOW",
  "scheduledDate": null,
  "nextExecutionDate": null,
  "lastExecutionDate": null,
  "riskScore": 0,
  "createdAt": "2026-02-21T14:30:00",
  "confirmedAt": "2026-02-21T14:30:00"
}
```

### Réponse SCHEDULED:
```json
{
  "id": 2,
  "bankAccountId": 1,
  "beneficiaryName": "Fatima Zahra",
  "beneficiaryRib": "98765432109876543210987654",
  "amount": 1000,
  "type": "EXTERNAL",
  "category": "Facture",
  "description": "Paiement loyer février",
  "status": "PENDING",
  "periodicity": "SCHEDULED",
  "scheduledDate": "2026-03-15T10:30:00",
  "nextExecutionDate": null,
  "lastExecutionDate": null,
  "riskScore": 0,
  "createdAt": "2026-02-21T14:30:00",
  "confirmedAt": null
}
```

### Réponse PERMANENT:
```json
{
  "id": 3,
  "bankAccountId": 1,
  "beneficiaryName": "Orange Tunisie",
  "beneficiaryRib": "11111111111111111111111111",
  "amount": 50,
  "type": "EXTERNAL",
  "category": "Abonnement",
  "description": "Abonnement Internet mensuel",
  "status": "CONFIRMED",
  "periodicity": "PERMANENT",
  "scheduledDate": null,
  "nextExecutionDate": "2026-03-21T00:00:00",
  "lastExecutionDate": "2026-02-21T14:30:00",
  "riskScore": 0,
  "createdAt": "2026-02-21T14:30:00",
  "confirmedAt": "2026-02-21T14:30:00"
}
```

---

## ⚙️ Configuration par Défaut

| Champ | Défaut | Cas |
|-------|--------|-----|
| `periodicity` | NOW | Pas spécifié |
| `scheduledDate` | null | Non utilisé pour NOW/PERMANENT |
| `nextExecutionDate` | +30 jours | PERMANENT sans valeur |
| `lastExecutionDate` | null | Mis à jour après première exécution |

---

## ❌ Erreurs Possibles

### 1. Transaction SCHEDULED sans date
```json
{
  "error": "La date programmée est obligatoire pour une transaction SCHEDULED",
  "status": 400
}
```

### 2. Solde insuffisant (NOW ou PERMANENT)
```json
{
  "error": "Solde insuffisant",
  "status": 400
}
```

### 3. Compte non actif
```json
{
  "error": "Le compte n'est pas actif",
  "status": 400
}
```

### 4. RIB bénéficiaire invalide
```json
{
  "error": "Le compte bénéficiaire n'existe pas",
  "status": 400
}
```

---

## 🧪 Commandes CURL pour Tester

### NOW - Transaction Immédiate:
```bash
curl -X POST http://localhost:8080/transactions/add/1 \
  -H "Content-Type: application/json" \
  -d '{
    "beneficiaryName": "Ahmed",
    "beneficiaryRib": "12345678901234567890123456",
    "amount": 500,
    "type": "EXTERNAL",
    "periodicity": "NOW"
  }'
```

### SCHEDULED - Transaction Programmée:
```bash
curl -X POST http://localhost:8080/transactions/add/1 \
  -H "Content-Type: application/json" \
  -d '{
    "beneficiaryName": "Fatima",
    "beneficiaryRib": "98765432109876543210987654",
    "amount": 1000,
    "type": "EXTERNAL",
    "periodicity": "SCHEDULED",
    "scheduledDate": "2026-03-15T10:30:00"
  }'
```

### PERMANENT - Transaction Permanente:
```bash
curl -X POST http://localhost:8080/transactions/add/1 \
  -H "Content-Type: application/json" \
  -d '{
    "beneficiaryName": "Orange",
    "beneficiaryRib": "11111111111111111111111111",
    "amount": 50,
    "type": "EXTERNAL",
    "periodicity": "PERMANENT",
    "nextExecutionDate": "2026-03-21T00:00:00"
  }'
```

---

## 📖 Notes Importantes

✅ **Validations appliquées**:
- Le compte source doit exister et être ACTIVE
- Le compte bénéficiaire doit exister et être ACTIVE  
- Le montant doit être > 0
- Pour NOW et PERMANENT: le solde doit être suffisant **maintenant**
- Pour SCHEDULED: le solde sera vérifié à la date programmée
- Impossible de virer vers son propre compte

✅ **Logging détaillé**: Chaque transaction génère des logs pour tracer l'exécution

✅ **Transactions atomiques**: Utilisation de `@Transactional` pour l'intégrité des données

---

## 🎓 Exemple Complet: Abonnement Internet

**Scénario**: Un client veut s'abonner à un abonnement Internet mensuel.

**Requête**:
```json
POST /transactions/add/5

{
  "beneficiaryName": "Orange Tunisie - Abonnement",
  "beneficiaryRib": "11111111111111111111111111",
  "amount": 49.99,
  "type": "EXTERNAL",
  "category": "Abonnement",
  "description": "Abonnement Internet Fibre - 100Mbps",
  "periodicity": "PERMANENT",
  "nextExecutionDate": "2026-03-21T00:00:00"
}
```

**Résultat**:
- ✅ Transaction créée avec ID = 10
- ✅ 49,99 TND débité du compte "Ahmed" tout de suite
- ✅ 49,99 TND crédité au compte Orange
- ✅ Status = CONFIRMED (première exécution réussie)
- ✅ lastExecutionDate = 21/02/2026 14:30:00
- ✅ nextExecutionDate = 21/03/2026 00:00:00
- ✅ À partir du 21 mars, la transaction sera exécutée automatiquement chaque mois

---

Version: 1.0  
Date: 2026-02-21  
Auteur: Système Bancaire Helma
