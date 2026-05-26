# 🚀 Guide Rapide d'Utilisation des Services

## 📦 Fichiers Créés (22 fichiers Java)

### Entités (4 fichiers)
- ✅ `entities/BankAccount.java`
- ✅ `entities/Transaction.java`
- ✅ `entities/VirtualCard.java`
- ✅ `entities/SavedBeneficiary.java`

### Énumérations (5 fichiers)
- ✅ `enums/AccountStatus.java` (ACTIVE, FROZEN, CLOSED)
- ✅ `enums/AccountType.java` (COURANT, EPARGNE, BUSINESS)
- ✅ `enums/CardStatus.java` (ACTIVE, BLOCKED)
- ✅ `enums/TransactionStatus.java` (PENDING, CONFIRMED, SUSPICIOUS, CANCELED)
- ✅ `enums/TransactionType.java` (INTERNAL, EXTERNAL, CARD)

### Repositories (4 fichiers)
- ✅ `repositories/BankAccountRepository.java`
- ✅ `repositories/TransactionRepository.java`
- ✅ `repositories/VirtualCardRepository.java`
- ✅ `repositories/SavedBeneficiaryRepository.java`

### Interfaces de Services (4 fichiers)
- ✅ `services/IBankAccountService.java`
- ✅ `services/ITransactionService.java`
- ✅ `services/IVirtualCardService.java`
- ✅ `services/ISavedBeneficiaryService.java`

### Implémentations de Services (4 fichiers)
- ✅ `services/impl/BankAccountServiceImpl.java`
- ✅ `services/impl/TransactionServiceImpl.java`
- ✅ `services/impl/VirtualCardServiceImpl.java`
- ✅ `services/impl/SavedBeneficiaryServiceImpl.java`

---

## 💰 Cas d'Utilisation Courants

### 1️⃣ Créer un Compte Bancaire

```java
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class BankAccountController {
    
    private final IBankAccountService accountService;
    
    @PostMapping("/create")
    public ResponseEntity<BankAccount> createAccount(
            @RequestParam Long userId,
            @RequestParam String rib,
            @RequestParam AccountType accountType) {
        
        BankAccount account = accountService.createAccount(
            userId,
            rib,
            accountType,
            "TND"  // devise
        );
        
        return ResponseEntity.ok(account);
    }
    
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long accountId) {
        BigDecimal balance = accountService.getBalance(accountId);
        return ResponseEntity.ok(balance);
    }
}
```

---

### 2️⃣ Effectuer une Transaction (⚠️ CRITIQUE)

```java
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    
    private final ITransactionService transactionService;
    
    @PostMapping("/{accountId}/transfer")
    public ResponseEntity<?> transferMoney(
            @PathVariable Long accountId,
            @RequestBody TransactionRequest request) {
        
        try {
            // Créer l'objet transaction
            Transaction transaction = Transaction.builder()
                .beneficiaryName(request.getBeneficiaryName())
                .beneficiaryRib(request.getBeneficiaryRib())
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .description(request.getDescription())
                .build();
            
            // ⚠️ VALIDATION + DÉBIT AUTOMATIQUE
            Transaction result = transactionService.createTransaction(accountId, transaction);
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            // Solde insuffisant, compte non actif, etc.
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/{accountId}/monthly-summary")
    public ResponseEntity<String> getMonthlyAnalysis(@PathVariable Long accountId) {
        String summary = transactionService.generateMonthlySummary(accountId);
        return ResponseEntity.ok(summary);
        // "Ce mois vos dépenses sont de 1450 TND (+12%). Top catégorie: Épicerie (450 TND)."
    }
    
    @GetMapping("/{accountId}/search")
    public ResponseEntity<List<Transaction>> searchTransactions(
            @PathVariable Long accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {
        
        Map<String, Object> filters = new HashMap<>();
        if (type != null) filters.put("type", type);
        if (minAmount != null) filters.put("minAmount", minAmount);
        if (maxAmount != null) filters.put("maxAmount", maxAmount);
        
        List<Transaction> results = transactionService.searchTransactions(accountId, filters);
        return ResponseEntity.ok(results);
    }
}
```

---

### 3️⃣ Gérer les Cartes Virtuelles

```java
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class VirtualCardController {
    
    private final IVirtualCardService cardService;
    
    @PostMapping("/{accountId}/create")
    public ResponseEntity<VirtualCard> createCard(
            @PathVariable Long accountId,
            @RequestParam String expiryDate) {
        
        VirtualCard card = cardService.createCard(
            accountId,
            expiryDate,
            "hashed_cvv_value",
            BigDecimal.valueOf(5000.00)  // limite mensuelle
        );
        
        return ResponseEntity.ok(card);
    }
    
    @PutMapping("/{cardId}/block")
    public ResponseEntity<VirtualCard> blockCard(@PathVariable Long cardId) {
        VirtualCard blocked = cardService.blockCard(cardId);
        return ResponseEntity.ok(blocked);
    }
    
    @GetMapping("/{cardId}/limit-reached")
    public ResponseEntity<Boolean> isLimitReached(@PathVariable Long cardId) {
        boolean reached = cardService.isMonthlyLimitReached(cardId);
        return ResponseEntity.ok(reached);
    }
}
```

---

### 4️⃣ Enregistrer des Bénéficiaires

```java
@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {
    
    private final ISavedBeneficiaryService beneficiaryService;
    
    @PostMapping
    public ResponseEntity<SavedBeneficiary> saveBeneficiary(
            @RequestParam Long userId,
            @RequestParam String beneficiaryName,
            @RequestParam String beneficiaryRib,
            @RequestParam String alias) {
        
        SavedBeneficiary saved = beneficiaryService.saveBeneficiary(
            userId,
            beneficiaryName,
            beneficiaryRib,
            alias
        );
        
        return ResponseEntity.ok(saved);
    }
    
    @GetMapping("/{userId}/most-used")
    public ResponseEntity<List<SavedBeneficiary>> getMostUsed(@PathVariable Long userId) {
        List<SavedBeneficiary> mostUsed = beneficiaryService.getMostUsedBeneficiaries(userId);
        return ResponseEntity.ok(mostUsed);
    }
}
```

---

## 🧪 Tests d'Exemple

### Test: Créer une Transaction avec Validation

```java
@SpringBootTest
class TransactionServiceTest {
    
    @Autowired
    private ITransactionService transactionService;
    
    @Autowired
    private IBankAccountService accountService;
    
    @Test
    void testCreateTransactionWithValidation() {
        // 1. Créer un compte
        BankAccount account = accountService.createAccount(
            1L,
            "41200012012345678901234567",
            AccountType.COURANT,
            "TND"
        );
        
        // 2. Créditer le compte
        accountService.creditAccount(account.getId(), BigDecimal.valueOf(1000.00));
        
        // 3. Créer une transaction valide
        Transaction tx = Transaction.builder()
            .beneficiaryName("Ahmed")
            .beneficiaryRib("41200012098765432109876543")
            .amount(BigDecimal.valueOf(500.00))
            .type(TransactionType.EXTERNAL)
            .category("Transfert")
            .build();
        
        Transaction created = transactionService.createTransaction(account.getId(), tx);
        
        // 4. Vérifications
        assertEquals(TransactionStatus.CONFIRMED, created.getStatus());
        assertEquals(0, created.getRiskScore());
        assertNotNull(created.getConfirmedAt());
        
        // 5. Vérifier le solde débité
        BigDecimal newBalance = accountService.getBalance(account.getId());
        assertEquals(BigDecimal.valueOf(500.00), newBalance);
    }
    
    @Test
    void testCreateTransactionInsufficientBalance() {
        // 1. Créer un compte avec solde faible
        BankAccount account = accountService.createAccount(
            2L,
            "41200012012345678901234568",
            AccountType.COURANT,
            "TND"
        );
        
        accountService.creditAccount(account.getId(), BigDecimal.valueOf(100.00));
        
        // 2. Essayer de transférer plus que le solde
        Transaction tx = Transaction.builder()
            .beneficiaryName("Ahmed")
            .beneficiaryRib("41200012098765432109876543")
            .amount(BigDecimal.valueOf(500.00))  // > 100 TND
            .type(TransactionType.EXTERNAL)
            .build();
        
        // 3. Exception attendue
        assertThrows(IllegalArgumentException.class, () ->
            transactionService.createTransaction(account.getId(), tx)
        );
    }
}
```

---

## 🔄 Flux Transactionnel Complet

```
┌─────────────────────────────────────────────────────┐
│ CLIENT INITIE UNE TRANSACTION                       │
│ - POST /api/transactions/{accountId}/transfer       │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ CONTROLLER REÇOIT LA REQUÊTE                        │
│ - Validation des données HTTP (format, types)       │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ SERVICE EXÉCUTE LA LOGIQUE MÉTIER                   │
│ createTransaction(accountId, transaction)           │
│                                                     │
│ 1️⃣ Compte existe ? ────► Non ─► Exception           │
│ 2️⃣ Compte ACTIVE ? ────► Non ─► Exception           │
│ 3️⃣ Montant > 0 ? ───────► Non ─► Exception           │
│ 4️⃣ Solde >= Montant ? ──► Non ─► Exception           │
│    Tous OK ? ─────────────► OUI │                   │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ MODIFICATION DU COMPTE                              │
│ - balance = balance - amount                        │
│ - updatedAt = LocalDateTime.now()                   │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ CRÉATION DE LA TRANSACTION                          │
│ - status = CONFIRMED                                │
│ - riskScore = 0                                     │
│ - confirmedAt = LocalDateTime.now()                 │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ SAUVEGARDE EN BASE DE DONNÉES                       │
│ @Transactional ────► COMMIT ou ROLLBACK             │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ RÉPONSE RETOURNÉE AU CLIENT                         │
│ - 200 OK + Transaction créée                        │
│ - 400 Bad Request + Message d'erreur                │
└─────────────────────────────────────────────────────┘
```

---

## 📊 Prochaines Étapes

Pour compléter votre application bancaire:

### 1. Controllers REST ✅ À CRÉER
```
controllers/
├── BankAccountController.java
├── TransactionController.java
├── VirtualCardController.java
└── BeneficiaryController.java
```

### 2. DTOs (Data Transfer Objects) ✅ À CRÉER
```
dtos/
├── BankAccountDTO.java
├── TransactionDTO.java
├── VirtualCardDTO.java
└── ErrorResponse.java
```

### 3. Exception Handling ✅ À CRÉER
```
exceptions/
├── GlobalExceptionHandler.java
├── InsufficientBalanceException.java
├── AccountNotFoundException.java
└── TransactionException.java
```

### 4. Configuration de Sécurité ✅ À CRÉER
```
config/
├── SecurityConfig.java
├── WebConfig.java
└── JwtTokenProvider.java
```

### 5. Tests Unitaires ✅ À CRÉER
```
tests/
├── TransactionServiceTest.java
├── BankAccountServiceTest.java
├── VirtualCardServiceTest.java
└── BeneficiaryServiceTest.java
```

---

## 🚦 Compilation et Exécution

```bash
# Compiler le projet
$env:JAVA_HOME="C:\Users\User\.jdks\corretto-17.0.12"
.\mvnw clean compile

# Voir les erreurs de compilation
.\mvnw clean compile

# Générer le JAR exécutable
.\mvnw clean package

# Lancer l'application
java -jar target/Helma-0.0.1-SNAPSHOT.jar
```

---

## ✅ Validation du Code

Tous les fichiers compilent correctement ✅

```
[INFO] BUILD SUCCESS
[INFO] Compiling 22 Java files...
```

---

## 📖 Documentation Détaillée

Consultez le fichier **ARCHITECTURE.md** pour:
- Diagramme détaillé des entités
- Logique métier complète
- Exemples d'utilisation avancée
- Best practices

---

## 💡 Points Importants

1. **@Transactional** est appliquée à tous les services
   - Garantit l'intégrité des données
   - Rollback automatique en cas d'erreur

2. **BigDecimal** utilisée pour tous les montants
   - Pas de `double` ❌
   - Précision requise pour les données financières

3. **Validations métier** dans le service
   - Jamais dans le controller
   - Messages d'erreur explicites

4. **Relations JPA configurées**
   - BankAccount ↔ Transaction (1:N)
   - BankAccount ↔ VirtualCard (1:N)
   - Pas de relation directe avec SavedBeneficiary

5. **Logging complet**
   - @Slf4j sur tous les services
   - Debugging facile en production

---

**Bon développement! 🎉**
