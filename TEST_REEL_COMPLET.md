# 🧪 TEST RÉEL AVEC LA BASE DE DONNÉES - Guide Complet

## ⚠️ PRÉ-REQUIS

Avant de commencer, vérifiez que vous avez:

1. **Java 17+** installé
```bash
java -version
```

2. **MySQL** en cours d'exécution 
```bash
# Vérifier que MySQL démarre (port 3306 par défaut)
```

3. **VS Code** avec terminal PowerShell ou CMD

---

## 🚀 ÉTAPE 1: Configurer JAVA_HOME (Si nécessaire)

### Si Java n'est pas reconnu:

**Méthode 1: Directement dans PowerShell**
```powershell
# Avant de lancer l'application, exécutez:
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"  # Adapter le chemin
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Vérifier:
java -version
```

**Méthode 2: Configuration permanente (Windows)**
1. Clic droit sur "Ordinateur" → Propriétés
2. Variables d'environnement → Nouvelle variable
3. Nom: `JAVA_HOME`
4. Valeur: `C:\Program Files\Java\jdk-17` (adapter votre version)
5. Redémarrer PowerShell

---

## 🚀 ÉTAPE 2: Démarrer l'Application

### Dans VS Code:

1. **Ouvrir le terminal** (Ctrl + `)
2. **Exécuter:**

```powershell
# Aller au dossier Helma
cd c:\Helma

# Démarrer l'application
.\mvnw.cmd spring-boot:run
```

### 🎯 Attendre le message:
```
Started HelmaApplication in X.XXX seconds
Tomcat started on port(s): 8082 (http)
```

**⚠️ Laissez cette fenêtre OUVERTE et ACTIVE**

---

## 🆕 ÉTAPE 3: Ouvrir UN AUTRE Terminal

### Ouvrir un nouvel onglet terminal:
1. Ctrl + Shift + `
2. Ou: Terminal → New Terminal

### Dans ce **NOUVEAU terminal**, nous allons tester:

```powershell
# Vous êtes dans: c:\Helma
```

---

## ✅ VÉRIFICATION 1: L'API Répond-elle?

**Commande:**
```powershell
curl -X GET http://localhost:8082/helma/accounts/get/1
```

**Résultat attendu:**
```json
null  # Car aucun compte n'existe encore
```

✅ Si vous voyez `null` ou `0`, l'API répond! C'est bon!

---

## 💾 TEST RÉEL - CRÉER LE 1ER COMPTE (Ahmed)

**Commande:**
```powershell
$body = @{
    userId = 1
    rib = "12345678901234567890123456"
    accountType = "CHECKING"
    currency = "TND"
} | ConvertTo-Json

curl -X POST http://localhost:8082/helma/accounts/add `
  -H "Content-Type: application/json" `
  -d $body
```

**Résultat attendu:**
```json
{
  "id": 1,
  "userId": 1,
  "rib": "12345678901234567890123456",
  "balance": 0,
  "currency": "TND",
  "accountType": "CHECKING",
  "status": "ACTIVE",
  "createdAt": "2026-02-21T...",
  "updatedAt": null
}
```

✅ **Le compte a été créé dans la base de données!**
✅ **Notez l'ID: `1`**

---

## 💰 CRÉDITER LE COMPTE 1 AVEC 5000 TND

**Commande:**
```powershell
curl -X POST "http://localhost:8082/helma/accounts/credit/1?amount=5000"
```

**Résultat:**
```json
{
  "id": 1,
  "balance": 5000,
  ...
}
```

✅ **Ahmed a maintenant 5000 TND dans la base de données!**

---

## 💾 CRÉER LE 2E COMPTE (Fatima)

**Commande:**
```powershell
$body = @{
    userId = 2
    rib = "98765432109876543210987654"
    accountType = "CHECKING"
    currency = "TND"
} | ConvertTo-Json

curl -X POST http://localhost:8082/helma/accounts/add `
  -H "Content-Type: application/json" `
  -d $body
```

**Résultat:**
```json
{
  "id": 2,
  "userId": 2,
  "rib": "98765432109876543210987654",
  "balance": 0,
  ...
}
```

✅ **Le compte Fatima est créé!**
✅ **Notez l'ID: `2`**

---

## 💰 CRÉDITER LE COMPTE 2 AVEC 2000 TND

**Commande:**
```powershell
curl -X POST "http://localhost:8082/helma/accounts/credit/2?amount=2000"
```

**Résultat:**
```json
{
  "id": 2,
  "balance": 2000,
  ...
}
```

✅ **Fatima a maintenant 2000 TND!**

---

## 📊 VÉRIFIER L'ÉTAT DES COMPTES

**Vérifier le compte Ahmed:**
```powershell
curl -X GET http://localhost:8082/helma/accounts/get/1
```

**Résultat:**
```json
{
  "id": 1,
  "balance": 5000,
  "status": "ACTIVE",
  ...
}
```

**Vérifier le compte Fatima:**
```powershell
curl -X GET http://localhost:8082/helma/accounts/get/2
```

**Résultat:**
```json
{
  "id": 2,
  "balance": 2000,
  "status": "ACTIVE",
  ...
}
```

---

## 💸 TRANSACTION 1: NOW (Immédiate) - Ahmed → Fatima 500 TND

**Commande:**
```powershell
$body = @{
    beneficiaryName = "Fatima Zahra"
    beneficiaryRib = "98765432109876543210987654"
    amount = 500
    type = "EXTERNAL"
    category = "Transfert Personnel"
    description = "Paiement immédiat pour Fatima"
    periodicity = "NOW"
} | ConvertTo-Json

curl -X POST http://localhost:8082/helma/transactions/add/1 `
  -H "Content-Type: application/json" `
  -d $body
```

**Résultat attendu:**
```json
{
  "id": 1,
  "bankAccountId": 1,
  "beneficiaryName": "Fatima Zahra",
  "beneficiaryRib": "98765432109876543210987654",
  "amount": 500,
  "status": "CONFIRMED",
  "periodicity": "NOW",
  "scheduledDate": null,
  "confirmedAt": "2026-02-21T...",
  ...
}
```

✅ **Transaction créée et CONFIRMÉE IMMÉDIATEMENT!**

---

## ✅ VÉRIFIER LES SOLDES APRÈS TRANSACTION 1

**Ahmed doit avoir: 5000 - 500 = 4500 TND**
```powershell
curl -X GET http://localhost:8082/helma/accounts/get/1
```

**Résultat:**
```json
{
  "id": 1,
  "balance": 4500,  ← DÉBITÉ!
  ...
}
```

**Fatima doit avoir: 2000 + 500 = 2500 TND**
```powershell
curl -X GET http://localhost:8082/helma/accounts/get/2
```

**Résultat:**
```json
{
  "id": 2,
  "balance": 2500,  ← CRÉDITÉ!
  ...
}
```

✅ **Les soldes ont CHANGÉ dans la base de données!**
✅ **La transaction NOW a fonctionné!**

---

## 💸 TRANSACTION 2: SCHEDULED (Programmée) - Fatima → Ahmed 1000 TND

**Commande:**
```powershell
$body = @{
    beneficiaryName = "Ahmed Ben Ali"
    beneficiaryRib = "12345678901234567890123456"
    amount = 1000
    type = "EXTERNAL"
    category = "Transfert"
    description = "Paiement programmé pour demain"
    periodicity = "SCHEDULED"
    scheduledDate = "2026-02-22T10:00:00"
} | ConvertTo-Json

curl -X POST http://localhost:8082/helma/transactions/add/2 `
  -H "Content-Type: application/json" `
  -d $body
```

**Résultat attendu:**
```json
{
  "id": 2,
  "bankAccountId": 2,
  "beneficiaryName": "Ahmed Ben Ali",
  "amount": 1000,
  "status": "PENDING",
  "periodicity": "SCHEDULED",
  "scheduledDate": "2026-02-22T10:00:00",
  "confirmedAt": null,
  ...
}
```

✅ **Transaction créée et EN ATTENTE (PENDING)!**
✅ **confirmedAt est `null` (sera confirmée demain)!**

---

## ✅ VÉRIFIER LES SOLDES APRÈS TRANSACTION 2

**Ahmed doit TOUJOURS avoir: 4500 TND (pas changé)**
```powershell
curl -X GET http://localhost:8082/helma/accounts/get/1
```

**Résultat:**
```json
{
  "id": 1,
  "balance": 4500,  ← INCHANGÉ!
  ...
}
```

**Fatima doit TOUJOURS avoir: 2500 TND (pas changé)**
```powershell
curl -X GET http://localhost:8082/helma/accounts/get/2
```

**Résultat:**
```json
{
  "id": 2,
  "balance": 2500,  ← INCHANGÉ!
  ...
}
```

✅ **Les soldes NE ONT PAS CHANGÉ car la transaction est PENDING!**
✅ **Les soldes changeront le 22 février à 10h!**

---

## 📋 LISTER TOUTES LES TRANSACTIONS

**Lister les transactions du compte Ahmed (ID: 1):**
```powershell
curl -X GET http://localhost:8082/helma/transactions/account/1
```

**Résultat:**
```json
[
  {
    "id": 1,
    "bankAccountId": 1,
    "amount": 500,
    "status": "CONFIRMED",
    "periodicity": "NOW",
    "confirmedAt": "2026-02-21T14:40:00"
  },
  {
    "id": 2,
    "bankAccountId": 2,
    "amount": 1000,
    "status": "PENDING",
    "periodicity": "SCHEDULED",
    "scheduledDate": "2026-02-22T10:00:00",
    "confirmedAt": null
  }
]
```

✅ **Les 2 transactions sont dans la base de données!**

---

## 📊 RÉSUMÉ FINAL DU TEST RÉEL

### État Initial (avant tests):
```
Base de données: VIDE
```

### Après création des comptes:
```
Ahmed (ID: 1):  0 TND
Fatima (ID: 2): 0 TND
```

### Après crédit:
```
Ahmed (ID: 1):  5000 TND
Fatima (ID: 2): 2000 TND
```

### Après Transaction 1 (NOW):
```
Ahmed (ID: 1):  4500 TND ✅ DÉBITÉ
Fatima (ID: 2): 2500 TND ✅ CRÉDITÉ
Status: CONFIRMED, confirmedAt: Maintenant
```

### Après Transaction 2 (SCHEDULED):
```
Ahmed (ID: 1):  4500 TND ⏳ NON CHANGÉ
Fatima (ID: 2): 2500 TND ⏳ NON CHANGÉ
Status: PENDING, confirmedAt: null
scheduledDate: 22 février

À la date prévue (22 février):
Ahmed (ID: 1):  5500 TND (crédité de 1000)
Fatima (ID: 2): 1500 TND (débité de 1000)
Status: CONFIRMED, confirmedAt: 22 février
```

---

## 🔍 VÉRIFIER LA BASE DE DONNÉES (MySQL)

### Vous pouvez aussi vérifier directement dans MySQL:

**Connectez-vous à MySQL:**
```bash
mysql -u root -p
```

**Utilisez la base Helma:**
```sql
USE Helma;
```

**Voir tous les comptes:**
```sql
SELECT * FROM bank_account;
```

**Voir toutes les transactions:**
```sql
SELECT id, bank_account_id, amount, type, status, periodicity, scheduled_date FROM transaction;
```

**Voir les soldes:**
```sql
SELECT id, balance, currency FROM bank_account;
```

---

## ✅ CHECKLIST DE TEST RÉEL

- [ ] Application démarrée (message "Started HelmaApplication")
- [ ] Terminal de test ouvert dans un nouvel onglet
- [ ] Compte Ahmed créé (ID: 1)
- [ ] Compte Ahmed crédité (5000 TND)
- [ ] Compte Fatima créé (ID: 2)
- [ ] Compte Fatima crédité (2000 TND)
- [ ] Transaction 1 (NOW) créée
  - [ ] Status = CONFIRMED
  - [ ] Soldes changés immédiatement
- [ ] Transaction 2 (SCHEDULED) créée
  - [ ] Status = PENDING
  - [ ] Soldes NON changés
  - [ ] scheduledDate = 22 février
- [ ] Lister toutes les transactions
- [ ] Vérifier les soldes finaux

---

## 🎓 CE QUE VOUS TESTEZ RÉELLEMENT

✅ **Base de données real MySQL**:
- Les comptes sont enregistrés
- Les soldes sont modifiés
- Les transactions sont enregistrées

✅ **Logique NOW (Immédiate)**:
- La transaction est CONFIRMÉE tout de suite
- Les soldes changent IMMÉDIATEMENT
- confirmedAt est défini

✅ **Logique SCHEDULED (Programmée)**:
- La transaction reste en PENDING
- Les soldes NE changent PAS
- confirmedAt reste null
- scheduledDate est enregistré

✅ **Intégrité des données**:
- Les RIB sont validés
- Les comptes bénéficiaires existent
- Les soldes sont mis à jour correctement

---

## 💡 CONSEILS

1. **Copier-coller les commandes** directement dans PowerShell
2. **Attendre la réponse** de chaque requête avant la suivante
3. **Vérifier les soldes** après chaque transaction
4. **Garder l'application démarrée** pendant tout le test
5. **Si erreur**, vérifier les IDs et les RIB

---

## 🆘 DÉPANNAGE

### "mvnw.cmd not found"
→ Utiliser le chemin complet: `C:\Helma\mvnw.cmd spring-boot:run`

### "JAVA_HOME not configured"
→ Configurer JAVA_HOME comme décrit à l'Étape 1

### "Cannot connect to localhost:8082"
→ Vérifier que l'application est démarrée et ne montre pas d'erreur

### "RIB not found"
→ Assurez-vous que le RIB bénéficiaire est exactement le même que créé

### "Insufficient balance"
→ Le solde n'est pas assez élevé (créditer plus)

---

## 🎉 RÉSULTAT

Une fois tous les tests passés, vous avez:
✅ Comptes réels dans MySQL
✅ Transactions réelles dans MySQL
✅ Soldes mis à jour correctement
✅ NOW qui fonctionne parfaitement
✅ SCHEDULED qui fonctionne parfaitement

**C'EST UN TEST 100% RÉEL! 🎯**

---

**Version: 1.0**  
**Date: 2026-02-21**  
**Statut: ✅ Guide Complet de Test Réel**
