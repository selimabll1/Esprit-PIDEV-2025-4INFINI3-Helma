# 🏦 Application Bancaire Helma - Architecture Backend

## 📋 Vue d'ensemble

Cette application bancaire est développée avec **Spring Boot**, **JPA/Hibernate** et **MySQL**. L'architecture suit strictement le pattern **Layered Architecture** avec une séparation nette des responsabilités.

### Architecture Générale
```
Controller → ServiceInterface → ServiceImpl → Repository → Database
```

---

## 📁 Structure du Projet

```
src/main/java/tn/esprit/helma/
├── controllers/           # 🚀 Endpoints REST (À créer)
├── entities/              # 📊 Modèles JPA
│   ├── BankAccount.java
│   ├── Transaction.java
│   ├── VirtualCard.java
│   └── SavedBeneficiary.java
├── enums/                 # 🔐 Types énumérés
│   ├── AccountStatus.java
│   ├── AccountType.java
│   ├── CardStatus.java
│   ├── TransactionStatus.java
│   └── TransactionType.java
├── repositories/          # 🗄️ Accès aux données
│   ├── BankAccountRepository.java
│   ├── TransactionRepository.java
│   ├── VirtualCardRepository.java
│   └── SavedBeneficiaryRepository.java
├── services/              # 🔧 Interfaces métier
│   ├── IBankAccountService.java
│   ├── ITransactionService.java
│   ├── IVirtualCardService.java
│   └── ISavedBeneficiaryService.java
└── services/impl/         # 💼 Implémentations
    ├── BankAccountServiceImpl.java
    ├── TransactionServiceImpl.java
    ├── VirtualCardServiceImpl.java
    └── SavedBeneficiaryServiceImpl.java
```

---

## 🗄️ Modèle de Données

### 1. **BankAccount** 🏦
Représente un compte bancaire de l'utilisateur.

**Champs:**
- `id`: Identifiant unique (Long)
- `userId`: ID de l'utilisateur propriétaire
- `rib`: RIB unique du compte (27 caractères)
- `balance`: Solde actuel (BigDecimal)
- `currency`: Devise (TND, EUR, USD, etc.)
- `accountType`: Type de compte (COURANT, EPARGNE, BUSINESS)
- `status`: Statut (ACTIVE, FROZEN, CLOSED)
- `createdAt`: Date de création
- `updatedAt`: Date de modification

**Relations:**
- ✅ OneToMany avec **Transaction** (Un compte → Many transactions)
- ✅ OneToMany avec **VirtualCard** (Un compte → Many cartes)

**Exemple d'utilisation:**
```java
BankAccount account = BankAccount.builder()
    .userId(1L)
    .rib("41200012012345678901234567")
    .balance(BigDecimal.valueOf(5000.00))
    .currency("TND")
    .accountType(AccountType.COURANT)
    .status(AccountStatus.ACTIVE)
    .build();
```

---

### 2. **Transaction** 💸
Enregistre toutes les transactions bancaires.

**Champs:**
- `id`: Identifiant unique
- `bankAccount`: Référence au compte (ManyToOne)
- `beneficiaryName`: Nom du bénéficiaire
- `beneficiaryRib`: RIB du bénéficiaire
- `amount`: Montant (BigDecimal)
- `type`: Type (INTERNAL, EXTERNAL, CARD)
- `category`: Catégorie (ex: "Épicerie", "Salaire")
- `description`: Description libre
- `scheduledDate`: Date programmée (transactions futures)
- `status`: Statut (PENDING, CONFIRMED, SUSPICIOUS, CANCELED)
- `riskScore`: Score de risque (0-100)
- `createdAt`: Date de création
- `confirmedAt`: Date de confirmation

**Logique Métier - `createTransaction()` ⚠️ CRITIQUE:**

```
VALIDATIONS:
1. Le compte existe ?
2. Le compte est ACTIVE ?
3. Montant > 0 ?
4. Solde >= Montant ?

SI VALIDE:
└─ Déduit montant du solde
└─ Statut = CONFIRMED
└─ riskScore = 0
└─ confirmedAt = LocalDateTime.now()
└─ Sauvegarde la transaction et le compte

SINON:
└─ Lance IllegalArgumentException avec message détaillé
```

**Exemple:**
```java
Transaction transaction = Transaction.builder()
    .beneficiaryName("Ahmed Ben Ali")
    .beneficiaryRib("41200012098765432109876543")
    .amount(BigDecimal.valueOf(500.00))
    .type(TransactionType.EXTERNAL)
    .category("Transfert Personnel")
    .description("Transfert vers Ahmed")
    .status(TransactionStatus.CONFIRMED)
    .riskScore(0)
    .build();

// Le service crée et confirme automatiquement
Transaction created = transactionService.createTransaction(accountId, transaction);
```

---

### 3. **VirtualCard** 💳
Représente une carte virtuelle pour les paiements en ligne.

**Champs:**
- `id`: Identifiant unique
- `bankAccount`: Référence au compte (ManyToOne)
- `cardNumber`: Numéro de carte (16 chiffres, généré automatiquement)
- `expiryDate`: Date d'expiration (MM/YY)
- `cvvHash`: Hash du CVV (pour sécurité)
- `status`: Statut (ACTIVE, BLOCKED)
- `paymentLimit`: Limite de paiement mensuel
- `monthlySpent`: Dépenses du mois courant
- `createdAt`: Date de création
- `updatedAt`: Date de modification

**Exemple:**
```java
VirtualCard card = virtualCardService.createCard(
    accountId,
    "12/26",         // expiryDate
    "hashed_cvv",    // cvvHash
    BigDecimal.valueOf(2000.00) // paymentLimit
);
```

---

### 4. **SavedBeneficiary** 👥
Enregistre les bénéficiaires fréquents pour transferts rapides.

**Champs:**
- `id`: Identifiant unique
- `userId`: ID de l'utilisateur propriétaire
- `beneficiaryName`: Nom complet
- `beneficiaryRib`: RIB du bénéficiaire
- `alias`: Surnom personnalisé (ex: "Mère", "Entreprise XYZ")
- `transferCount`: Nombre de transferts effectués
- `createdAt`: Date de création
- `updatedAt`: Date de modification

**Exemple:**
```java
SavedBeneficiary beneficiary = savedBeneficiaryService.saveBeneficiary(
    userId,
    "Fatima Ben Ali",
    "41200012087654321098765432",
    "Mère"
);
```

---

## 🔐 Enumerations

### AccountStatus
```java
ACTIVE  - Compte actif et utilisable
FROZEN  - Compte gelé temporairement
CLOSED  - Compte fermé définitivement
```

### AccountType
```java
COURANT  - Compte chèques classique
EPARGNE  - Compte d'épargne
BUSINESS - Compte professionnel
```

### CardStatus
```java
ACTIVE  - Carte active et utilisable
BLOCKED - Carte bloquée
```

### TransactionStatus
```java
PENDING    - Transaction en attente
CONFIRMED  - Transaction confirmée et exécutée
SUSPICIOUS - Transaction détectée comme suspecte
CANCELED   - Transaction annulée
```

### TransactionType
```java
INTERNAL - Transfert entre comptes du même utilisateur
EXTERNAL - Transfert vers un compte tiers
CARD     - Paiement par carte bancaire
```

---

## 💼 Services et Logique Métier

### 1. **IBankAccountService** 💰
Gestion complète des comptes bancaires.

```java
// CRUD
BankAccount createAccount(Long userId, String rib, AccountType type, String currency);
Optional<BankAccount> getAccountById(Long accountId);
Optional<BankAccount> getAccountByRib(String rib);
List<BankAccount> getUserAccounts(Long userId);
void deleteAccount(Long accountId);

// Opérations financières
BankAccount creditAccount(Long accountId, BigDecimal amount);  // Ajoute au solde
BankAccount debitAccount(Long accountId, BigDecimal amount);   // Soustrait du solde
BigDecimal getBalance(Long accountId);

// Gestion du statut
BankAccount freezeAccount(Long accountId);      // Gèle le compte
BankAccount unfreezeAccount(Long accountId);    // Dégèle le compte
BankAccount closeAccount(Long accountId);       // Ferme le compte

// Validations
boolean isAccountActive(Long accountId);
boolean isRibAvailable(String rib);
```

---

### 2. **ITransactionService** ⚡ PRINCIPAL
Cœur de la logique métier avec validations strictes.

#### **Créer une Transaction** (Méthode Critique)
```java
Transaction createTransaction(Long accountId, Transaction transaction)
```

**Validations strictes:**
1. ✅ Compte existe
2. ✅ Compte est ACTIVE
3. ✅ Montant > 0
4. ✅ Solde >= Montant

**Si valide:** Débite le compte, confirme la transaction
**Sinon:** Lance exception avec message détaillé

```java
// Utilisation:
try {
    Transaction tx = Transaction.builder()
        .beneficiaryName("Ahmed")
        .beneficiaryRib("41200012098765432109876543")
        .amount(BigDecimal.valueOf(1000.00))
        .type(TransactionType.EXTERNAL)
        .category("Transfert Personnel")
        .build();
    
    Transaction created = transactionService.createTransaction(accountId, tx);
    // ✅ Compte débité, transaction confirmée
    
} catch (IllegalArgumentException e) {
    // ❌ "Solde insuffisant", "Compte non actif", etc.
    System.out.println(e.getMessage());
}
```

#### **Recherche Avancée**
```java
// Par type
List<Transaction> getTr ansactionsByType(Long accountId, TransactionType type);

// Par statut
List<Transaction> getTransactionsByStatus(Long accountId, TransactionStatus status);

// Par montant
List<Transaction> getTransactionsByAmountRange(Long accountId, BigDecimal min, BigDecimal max);

// Par plage de dates
List<Transaction> getTransactionsBetweenDates(Long accountId, LocalDateTime start, LocalDateTime end);

// Recherche avec filtres multiples
Map<String, Object> filters = new HashMap<>();
filters.put("type", TransactionType.EXTERNAL);
filters.put("minAmount", BigDecimal.valueOf(500));
filters.put("status", TransactionStatus.CONFIRMED);
List<Transaction> results = transactionService.searchTransactions(accountId, filters);
```

#### **Analyses Mensuelles** 📊
```java
// Total dépensé ce mois
BigDecimal spent = transactionService.getTotalSpentThisMonth(accountId);
// Ex: 1450.50

// Évolution mensuelle (12 derniers mois)
Map<String, BigDecimal> evolution = transactionService.getMonthlyEvolution(accountId);
// Ex: {"2025-02": 1450.50, "2025-01": 1290.00, ...}

// Résumé complet avec analyse
String summary = transactionService.generateMonthlySummary(accountId);
// "Ce mois vos dépenses sont de 1450 TND (+12%). Top catégorie: Épicerie (450 TND)."
```

---

### 3. **IVirtualCardService** 💳
Gestion des cartes virtuelles et des limites.

```java
// Création
VirtualCard createCard(Long bankAccountId, String expiryDate, String cvvHash, BigDecimal limit);

// Gestion du statut
VirtualCard blockCard(Long cardId);      // Bloque la carte
VirtualCard unblockCard(Long cardId);    // Débloque la carte

// Gestion des dépenses
VirtualCard addMonthlySpending(Long cardId, BigDecimal amount);
VirtualCard resetMonthlySpent(Long cardId);  // Réinit en début de mois
boolean isMonthlyLimitReached(Long cardId);

// Limite de paiement
VirtualCard updatePaymentLimit(Long cardId, BigDecimal newLimit);

// Récupération
List<VirtualCard> getCardsByBankAccount(Long bankAccountId);
List<VirtualCard> getActiveCardsByBankAccount(Long bankAccountId);
```

---

### 4. **ISavedBeneficiaryService** 👥
Gestion des bénéficiaires enregistrés.

```java
// Enregistrement
SavedBeneficiary saveBeneficiary(Long userId, String name, String rib, String alias);

// Recherche
Optional<SavedBeneficiary> getBeneficiaryByRib(Long userId, String rib);
Optional<SavedBeneficiary> getBeneficiaryByAlias(Long userId, String alias);
List<SavedBeneficiary> getMostUsedBeneficiaries(Long userId);  // Triés par usage

// Gestion
SavedBeneficiary updateBeneficiary(Long id, String newName, String newAlias);
SavedBeneficiary incrementTransferCount(Long id);  // Après un transfert
void deleteBeneficiary(Long id);

// Vérifications
boolean beneficiaryExists(Long userId, String rib);
long countUserBeneficiaries(Long userId);
```

---

## 🗄️ Repositories

Tous les repositories héritent de `JpaRepository` et fournissent:

### BankAccountRepository
```java
Optional<BankAccount> findByRib(String rib);
List<BankAccount> findByUserId(Long userId);
List<BankAccount> findActiveAccountsByUserId(Long userId, AccountStatus status);
boolean existsByRib(String rib);
long countByUserId(Long userId);
```

### TransactionRepository (Requêtes complexes)
```java
// Recherches basiques
List<Transaction> findByBankAccountId(Long accountId);
List<Transaction> findByBankAccountIdAndType(Long accountId, TransactionType type);

// Recherches avancées
List<Transaction> findByBankAccountIdAndDateBetween(Long accountId, LocalDateTime start, LocalDateTime end);
List<Transaction> findByBankAccountIdAndAmountBetween(Long accountId, BigDecimal min, BigDecimal max);

// Calculs d'agrégation
BigDecimal calculateMonthlySpent(Long accountId);
BigDecimal calculateMonthlySpentForMonth(Long accountId, Integer year, Integer month);

// Analyses
List<Transaction> findByBankAccountIdAndStatusAndRiskScoreGreaterThan(Long accountId, TransactionStatus status, Integer riskScore);
```

### VirtualCardRepository
```java
Optional<VirtualCard> findByCardNumber(String cardNumber);
List<VirtualCard> findByBankAccountId(Long bankAccountId);
List<VirtualCard> findActiveCardsByBankAccountId(Long bankAccountId, CardStatus status);
```

### SavedBeneficiaryRepository
```java
List<SavedBeneficiary> findByUserId(Long userId);
Optional<SavedBeneficiary> findByUserIdAndBeneficiaryRib(Long userId, String rib);
Optional<SavedBeneficiary> findByUserIdAndAlias(Long userId, String alias);
List<SavedBeneficiary> findMostUsedBeneficiaries(Long userId);
```

---

## 🚀 Exemple d'Utilisation Complet

```java
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final ITransactionService transactionService;
    private final IBankAccountService accountService;

    @PostMapping("/{accountId}/create")
    public ResponseEntity<Transaction> createTransaction(
            @PathVariable Long accountId,
            @RequestBody Transaction transaction) {
        
        try {
            Transaction created = transactionService.createTransaction(accountId, transaction);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{accountId}/summary")
    public ResponseEntity<String> getMonthlyPummary(@PathVariable Long accountId) {
        String summary = transactionService.generateMonthlySummary(accountId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{accountId}/monthly-evolution")
    public ResponseEntity<Map<String, BigDecimal>> getEvolution(@PathVariable Long accountId) {
        Map<String, BigDecimal> evolution = transactionService.getMonthlyEvolution(accountId);
        return ResponseEntity.ok(evolution);
    }
}
```

---

## 🔒 Annotations et Best Practices

### @Transactional
- Utilisée sur tous les services d'implémentation
- Garantit l'atomicité des opérations
- Gère automatiquement les rollback en cas d'erreur

### @Slf4j (Lombok)
- Logs automatiques pour debugging
- Exemple: `log.info("Création d'un compte")`, `log.error(...)`

### @RequiredArgsConstructor
- Injection de dépendances automatique
- Évite le code boilerplate

### BigDecimal pour les montants
- ✅ Précision requise pour les données financières
- ❌ Jamais de `double` ou `float`

---

## 📝 Configuration MySQL

```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/helma_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

---

## ✅ Checklist d'Implémentation

Pour continuer le développement:

- [ ] Créer les **Controllers** REST
- [ ] Ajouter les **DTOs** (Data Transfer Objects)
- [ ] Implémenter la **gestion des erreurs** (GlobalExceptionHandler)
- [ ] Ajouter la **sécurité** (Spring Security, JWT)
- [ ] Implémenter les **tests unitaires** (JUnit 5, Mockito)
- [ ] Ajouter la **documentation** (Swagger/OpenAPI)
- [ ] Configurer **CI/CD** (Jenkins, GitLab CI)

---

## 🎯 Points Clés à Retenir

1. **Architecture Propre** ✅
   - Controllers → Services → Repositories
   - Séparation 100% des responsabilités

2. **Validations Métier** ✅
   - Tous les contrôles dans le service
   - Messages d'erreur explicites

3. **Transactions ACID** ✅
   - @Transactional garantit l'intégrité
   - Rollback automatique en cas d'erreur

4. **Sécurité Financière** ✅
   - BigDecimal pour les montants
   - Vérification du solde avant débit
   - Historique complet des transactions

5. **Logging Complet** ✅
   - Debugging facile avec @Slf4j
   - Traçabilité des opérations

---

## 📞 Support

Pour toute question sur l'architecture ou la logique métier, consultez les commentaires détaillés dans le code source.

**Bon développement! 🚀**
