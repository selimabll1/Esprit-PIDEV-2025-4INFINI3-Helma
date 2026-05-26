# 🗄️ Schéma Base de Données MySQL

## Configuration de la Connexion

```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/helma_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.use_sql_comments=true
```

---

## 📊 Schéma des Tables

### 1. Table `bank_account` (Comptes Bancaires)

```sql
CREATE TABLE bank_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT 'ID de l\'utilisateur propriétaire',
    rib VARCHAR(27) UNIQUE NOT NULL COMMENT 'RIB unique du compte',
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0 COMMENT 'Solde actuel',
    currency VARCHAR(3) NOT NULL DEFAULT 'TND' COMMENT 'Devise (TND, EUR, USD)',
    account_type VARCHAR(50) NOT NULL COMMENT 'COURANT, EPARGNE, BUSINESS',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, FROZEN, CLOSED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Dernière modification',
    
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Explications:**
- `id`: Clé primaire auto-incrémentée
- `rib`: Unique (pas deux comptes avec même RIB)
- `balance`: DECIMAL(19,2) = max 99999999999999999.99 TND
- `account_type`: ENUM stocké comme VARCHAR
- `status`: ENUM stocké comme VARCHAR
- Index sur `user_id` pour recherches rapides
- Timestamps automatiques avec `created_at` et `updated_at`

**Données d'exemple:**
```sql
INSERT INTO bank_account 
VALUES (1, 100, '41200012012345678901234567', 5000.00, 'TND', 'COURANT', 'ACTIVE', NOW(), NOW());
```

---

### 2. Table `transaction` (Transactions)

```sql
CREATE TABLE transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bank_account_id BIGINT NOT NULL COMMENT 'Compte source',
    beneficiary_name VARCHAR(100) NOT NULL COMMENT 'Nom du bénéficiaire',
    beneficiary_rib VARCHAR(27) COMMENT 'RIB du bénéficiaire (nullable)',
    amount DECIMAL(19, 2) NOT NULL COMMENT 'Montant de la transaction',
    type VARCHAR(50) NOT NULL COMMENT 'INTERNAL, EXTERNAL, CARD',
    category VARCHAR(50) COMMENT 'Catégorie (Épicerie, Salaire, etc.)',
    description VARCHAR(500) COMMENT 'Description libre',
    scheduled_date TIMESTAMP NULL COMMENT 'Date programmée (transferts futurs)',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, CONFIRMED, SUSPICIOUS, CANCELED',
    risk_score INT NOT NULL DEFAULT 0 COMMENT 'Score de risque 0-100',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création',
    confirmed_at TIMESTAMP NULL COMMENT 'Date de confirmation',
    
    FOREIGN KEY (bank_account_id) REFERENCES bank_account(id) ON DELETE CASCADE,
    INDEX idx_bank_account_id (bank_account_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_amount (amount),
    INDEX idx_type (type),
    INDEX idx_risk_score (risk_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Explications:**
- `bank_account_id`: Clé étrangère vers bank_account
- `amount`: DECIMAL(19,2) pour précision financière
- `status`: Enum pour le cycle de vie
- `risk_score`: Détection de fraude
- `confirmed_at`: NULL jusqu'à confirmation
- Index sur les colonnes fréquemment filtrées

**Données d'exemple:**
```sql
INSERT INTO transaction 
VALUES (1, 1, 'Ahmed Ben Ali', '41200012098765432109876543', 1000.00, 
        'EXTERNAL', 'Transfert Personnel', 'Transfert vers Ahmed', 
        NULL, 'CONFIRMED', 0, NOW(), NOW());
```

---

### 3. Table `virtual_card` (Cartes Virtuelles)

```sql
CREATE TABLE virtual_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bank_account_id BIGINT NOT NULL COMMENT 'Compte propriétaire',
    card_number VARCHAR(16) UNIQUE NOT NULL COMMENT 'Numéro de carte 16 chiffres',
    expiry_date VARCHAR(5) NOT NULL COMMENT 'Format MM/YY',
    cvv_hash VARCHAR(255) NOT NULL COMMENT 'Hash du CVV (jamais stocker en clair)',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, BLOCKED',
    payment_limit DECIMAL(19, 2) NOT NULL DEFAULT 5000.00 COMMENT 'Limite mensuelle',
    monthly_spent DECIMAL(19, 2) NOT NULL DEFAULT 0 COMMENT 'Dépenses du mois',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (bank_account_id) REFERENCES bank_account(id) ON DELETE CASCADE,
    INDEX idx_bank_account_id (bank_account_id),
    INDEX idx_status (status),
    INDEX idx_expiry_date (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Explications:**
- `card_number`: Unique, 16 chiffres (Visa: 4532...)
- `cvv_hash`: JAMAIS stocker le CVV en clair! Toujours hashé
- `payment_limit`: Limite mensuelle configurable
- `monthly_spent`: Réinitialisé chaque mois
- Index sur `expiry_date` pour chercher les cartes expirées

**Données d'exemple:**
```sql
INSERT INTO virtual_card 
VALUES (1, 1, '4532123456789012', '12/26', 
        'hashed_cvv_value_here', 'ACTIVE', 5000.00, 1200.00, NOW(), NOW());
```

---

### 4. Table `saved_beneficiary` (Bénéficiaires Enregistrés)

```sql
CREATE TABLE saved_beneficiary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT 'Utilisateur propriétaire',
    beneficiary_name VARCHAR(100) NOT NULL COMMENT 'Nom complet',
    beneficiary_rib VARCHAR(27) NOT NULL COMMENT 'RIB du bénéficiaire',
    alias VARCHAR(50) COMMENT 'Surnom (ex: "Mère", "Entreprise XYZ")',
    transfer_count INT NOT NULL DEFAULT 0 COMMENT 'Nombre de transferts effectués',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY unique_user_rib (user_id, beneficiary_rib),
    INDEX idx_user_id (user_id),
    INDEX idx_alias (alias),
    INDEX idx_transfer_count (transfer_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Explications:**
- `user_id`: Chaque utilisateur a ses propres bénéficiaires
- `beneficiary_rib`: Unique par utilisateur (UNIQUE KEY)
- `alias`: Surnom personnalisé pour accès rapide
- `transfer_count`: Utile pour tri "les plus utilisés"
- Pas de Foreign Key vers bank_account (bénéficiaire indépendant)

**Données d'exemple:**
```sql
INSERT INTO saved_beneficiary 
VALUES (1, 100, 'Fatima Ben Ali', '41200012087654321098765432', 
        'Mère', 5, NOW(), NOW());
```

---

## 🔄 Diagramme Relationnel

```
┌─────────────────────┐
│   bank_account      │
├─────────────────────┤
│ id (PK)             │
│ user_id             │
│ rib (UNIQUE)        │
│ balance             │
│ currency            │
│ account_type        │
│ status              │
│ created_at          │
│ updated_at          │
└──────────┬──────────┘
           │
           │ OneToMany (1:N)
           │
    ┌──────▼────────────┐         ┌──────────────┐
    │   transaction     │         │ virtual_card │
    ├───────────────────┤         ├──────────────┤
    │ id (PK)           │         │ id (PK)      │
    │ bank_account_id─┬─┼─────────┼─bank_account │
    │ beneficiary_name│ │         │ card_number  │
    │ beneficiary_rib │ │         │ expiry_date  │
    │ amount          │ │         │ cvv_hash     │
    │ type            │ │         │ status       │
    │ category        │ │         │ payment_limit│
    │ status          │ │         │ monthly_spent│
    │ risk_score      │ │         │ created_at   │
    │ created_at      │ │         │ updated_at   │
    │ confirmed_at    │ │         └──────────────┘
    └─────────────────┘ │
    (Foreign Key) ───────┘

┌────────────────────────┐
│  saved_beneficiary     │
├────────────────────────┤
│ id (PK)                │
│ user_id                │
│ beneficiary_name       │
│ beneficiary_rib        │
│ alias                  │
│ transfer_count         │
│ created_at             │
│ updated_at             │
└────────────────────────┘
(INDÉPENDANT - Utilisé dans Transactions)
```

---

## 🗑️ Script MySQL Complet

```sql
-- Créer la base de données
CREATE DATABASE IF NOT EXISTS helma_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE helma_db;

-- Créer table bank_account
CREATE TABLE bank_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    rib VARCHAR(27) UNIQUE NOT NULL,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'TND',
    account_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Créer table transaction
CREATE TABLE transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bank_account_id BIGINT NOT NULL,
    beneficiary_name VARCHAR(100) NOT NULL,
    beneficiary_rib VARCHAR(27),
    amount DECIMAL(19, 2) NOT NULL,
    type VARCHAR(50) NOT NULL,
    category VARCHAR(50),
    description VARCHAR(500),
    scheduled_date TIMESTAMP NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    risk_score INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP NULL,
    FOREIGN KEY (bank_account_id) REFERENCES bank_account(id) ON DELETE CASCADE,
    INDEX idx_bank_account_id (bank_account_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_amount (amount),
    INDEX idx_type (type),
    INDEX idx_risk_score (risk_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Créer table virtual_card
CREATE TABLE virtual_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bank_account_id BIGINT NOT NULL,
    card_number VARCHAR(16) UNIQUE NOT NULL,
    expiry_date VARCHAR(5) NOT NULL,
    cvv_hash VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    payment_limit DECIMAL(19, 2) NOT NULL DEFAULT 5000.00,
    monthly_spent DECIMAL(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (bank_account_id) REFERENCES bank_account(id) ON DELETE CASCADE,
    INDEX idx_bank_account_id (bank_account_id),
    INDEX idx_status (status),
    INDEX idx_expiry_date (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Créer table saved_beneficiary
CREATE TABLE saved_beneficiary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    beneficiary_name VARCHAR(100) NOT NULL,
    beneficiary_rib VARCHAR(27) NOT NULL,
    alias VARCHAR(50),
    transfer_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_rib (user_id, beneficiary_rib),
    INDEX idx_user_id (user_id),
    INDEX idx_alias (alias),
    INDEX idx_transfer_count (transfer_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Vérifier les tables créées
SHOW TABLES;
SHOW FULL COLUMNS FROM bank_account;
```

---

## 💡 Points Importantes pour MySQL

### 1. Sélection du Moteur
```sql
ENGINE=InnoDB  -- Transactions ACID, Foreign Keys
-- PAS MyISAM (pas de transactions)
```

### 2. Encodage Unicode
```sql
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
-- Support complet UTF-8 (emojis, caractères spéciaux)
```

### 3. Types de Données Corrects
```sql
DECIMAL(19, 2)    -- Montants (jamais FLOAT/DOUBLE)
BIGINT            -- IDs (pas INTEGER limité)
VARCHAR(N)        -- Strings de longueur variable
TIMESTAMP         -- Dates avec fuseau horaire
```

### 4. Indexes pour Performance
```sql
-- Recherches fréquentes
INDEX idx_user_id       -- Trouver les comptes d'un user
INDEX idx_status        -- Filtrer par statut
INDEX idx_created_at    -- Trier par date
INDEX idx_amount        -- Recherche par montant

-- Clés uniques
UNIQUE KEY unique_user_rib  -- Pas de doublon
```

### 5. Contraintes d'Intégrité
```sql
FOREIGN KEY (bank_account_id) REFERENCES bank_account(id) ON DELETE CASCADE
-- Supprimer les transactions si le compte est supprimé
```

---

## 🚀 Configuration Hibernate

Hibernate va créer automatiquement les tables si:

```properties
spring.jpa.hibernate.ddl-auto=update
```

**Valeurs possibles:**
- `create`: Crée les tables (supprime les données)
- `create-drop`: Crée et supprime à l'arrêt
- `update`: Met à jour le schéma (RECOMMANDÉ)
- `validate`: Valide sans changer
- `none`: Pas de modification

---

## 🔐 Recommandations de Sécurité

1. **Jamais de Mots de Passe en Clair**
   ```sql
   -- ❌ MAUVAIS
   password VARCHAR(100)
   
   -- ✅ BON
   password_hash VARCHAR(255)  -- SHA256 ou bcrypt
   ```

2. **CVV Hashé**
   ```sql
   -- ✅ Stored as hash, never as plain text
   cvv_hash VARCHAR(255)
   ```

3. **Audit Trail (À ajouter)**
   ```sql
   CREATE TABLE audit_log (
       id BIGINT PRIMARY KEY AUTO_INCREMENT,
       table_name VARCHAR(100),
       record_id BIGINT,
       action VARCHAR(50),  -- CREATE, UPDATE, DELETE
       old_values JSON,
       new_values JSON,
       user_id BIGINT,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   );
   ```

---

## 📊 Exemples de Requêtes

### Récupérer le solde d'un compte
```sql
SELECT balance FROM bank_account WHERE id = 1 AND status = 'ACTIVE';
```

### Transactions du mois courant
```sql
SELECT * FROM transaction 
WHERE bank_account_id = 1 
  AND YEAR(created_at) = YEAR(CURDATE())
  AND MONTH(created_at) = MONTH(CURDATE())
  AND status = 'CONFIRMED'
ORDER BY created_at DESC;
```

### Total dépensé ce mois
```sql
SELECT COALESCE(SUM(amount), 0) as total_spent
FROM transaction
WHERE bank_account_id = 1
  AND YEAR(created_at) = YEAR(CURDATE())
  AND MONTH(created_at) = MONTH(CURDATE())
  AND status = 'CONFIRMED';
```

### Cartes expirées
```sql
SELECT * FROM virtual_card
WHERE bank_account_id = 1
  AND DATE_FORMAT(CURDATE(), '%m/%y') > expiry_date;
```

### Bénéficiaires les plus utilisés
```sql
SELECT * FROM saved_beneficiary
WHERE user_id = 100
ORDER BY transfer_count DESC
LIMIT 10;
```

---

## ✅ Vérification Post-Création

```bash
# Depuis MySQL CLI
mysql -u root -p helma_db

# Vérifier les tables
SHOW TABLES;

# Compter les colonnes
DESCRIBE bank_account;

# Vérifier les indexes
SHOW INDEX FROM bank_account;

# Vérifier les clés étrangères
SELECT CONSTRAINT_NAME, TABLE_NAME, REFERENCED_TABLE_NAME 
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = 'helma_db' AND REFERENCED_TABLE_NAME IS NOT NULL;
```

---

**Base de données prête pour la production! ✅**
