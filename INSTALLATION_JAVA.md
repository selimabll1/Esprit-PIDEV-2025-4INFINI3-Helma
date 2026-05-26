# 🚀 INSTALLATION JAVA 17 + DÉMARRAGE HELMA BANK

## ⚠️ PROBLÈME ACTUEL

```
ERREUR: "The JAVA_HOME environment variable is not defined correctly"
CAUSE: Java 17 (JDK) n'est pas installé sur votre système
```

---

## 📥 ÉTAPE 1: INSTALLER JAVA 17

### Option 1: Oracle JDK 17 (Officiel)

1. **Accédez au lien de téléchargement:**
```
https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
```

2. **Choisir votre système:**
   - Windows x64: `jdk-17.x.x_windows-x64_bin.exe`

3. **Télécharger et installer:**
   - Exécuter le fichier `.exe`
   - Accepter les conditions
   - **Installation par défaut** (garder le chemin: `C:\Program Files\Java\jdk-17...`)

4. **Garder le chemin** suggéré (ex: `C:\Program Files\Java\jdk-17.0.x`)

---

### Option 2: OpenJDK 17 (Gratuit, même fonctionnalité)

1. **Télécharger depuis:**
```
https://adoptium.net/temurin/releases/?version=17
```

2. **Installer le fichier MSI** (Windows Installer)

3. **Installation par défaut**

---

## ✅ ÉTAPE 2: VÉRIFIER L'INSTALLATION DE JAVA

**Ouvrir PowerShell et vérifier:**

```powershell
java -version
```

**Résultat attendu:**
```
java version "17.x.x"
Java(TM) SE Runtime Environment
```

✅ Si vous le voyez, Java est bien installé!

---

## 🔧 ÉTAPE 3: CONFIGURER JAVA_HOME (Si besoin)

**Si `java -version` donne une erreur:**

1. **Trouver le chemin exact de Java:**
   ```powershell
   Get-ChildItem "C:\Program Files\Java" | Select-Object Name
   ```
   
   Résultat exemple:
   ```
   jdk-17.0.5
   ```

2. **Configurer JAVA_HOME temporairement** (dans PowerShell):
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.5"
   $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
   ```

3. **Ou configurer de façon permanente** (Windows):
   - Clic droit sur "Ordinateur" → "Propriétés"
   - Clic sur "Variables d'environnement"
   - Bouton "Nouvelle..." (Variables utilisateur)
   - Nom: `JAVA_HOME`
   - Valeur: `C:\Program Files\Java\jdk-17.0.5` (adapter votre numéro de version)
   - Clic OK → OK → Redémarrer PowerShell

4. **Vérifier:**
   ```powershell
   java -version
   ```

---

## 🚀 ÉTAPE 4: DÉMARRER HELMA BANK

### Option A: Utiliser le Script (Recommandé)

```powershell
cd C:\Helma
.\start.bat
```

Ce script:
- ✅ Cherche Java automatiquement
- ✅ Configure JAVA_HOME
- ✅ Démarre l'application

### Option B: Manuel (PowerShell)

```powershell
# Aller au dossier
cd C:\Helma

# Configuration temporaire de Java (si besoin)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.5"

# Démarrer l'application
.\mvnw.cmd spring-boot:run
```

---

## 🎯 ATTENDUS LORS DU DÉMARRAGE

L'application devrait afficher:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_|\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.4.12)

...

[INFO] Tomcat started on port(s): 8082 (http) with context path '/helma'
[INFO] Started HelmaApplication in X.XXX seconds
```

✅ **L'application est démarrée!**

---

## 📱 ÉTAPE 5: ACCÉDER À L'APPLICATION

### Swagger UI:
```
http://localhost:8082/helma/swagger-ui/index.html
```

### Ou directement:
```
http://localhost:8082/helma/accounts/get/1
```

---

## 🧪 MAINTENANT: FAIRE LES TESTS RÉELS

Une fois que l'application démarre avec succès:

1. **Garder le terminal de démarrage OUVERT**
2. **Ouvrir UN NOUVEAU PowerShell** (Ctrl + Shift + `)
3. **Suivre le guide: TEST_REEL_COMPLET.md**

```powershell
# Dans le NOUVEAU terminal:
cd C:\Helma

# Test 1: Créer un compte
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

---

## ✅ CHECKLIST

- [ ] Java 17 installé
- [ ] `java -version` fonctionne
- [ ] Application démarre (message "Started HelmaApplication")
- [ ] Tomcat écoute sur le port 8082
- [ ] Swagger UI accessible
- [ ] Nouveau terminal ouvert pour les tests
- [ ] Comptes créés dans la base de données
- [ ] Transactions exécutées

---

## 🆘 SI CA NE FONCTIONNE TOUJOURS PAS

### "Cannot find java"
```powershell
# Cherchez le dossier Java
Get-ChildItem "C:\Program Files\Java"
```
Puis adapter le chemin Java dans le script.

### "Port 8082 already in use"
```powershell
# Chercher le processus
Get-NetTCPConnection -LocalPort 8082 | Select-Object OwningProcess

# Tuer le processus (adapter le PID)
Stop-Process -Id <PID> -Force
```

### "Cannot connect to database Helma"
Assurez-vous que **MySQL est en cours d'exécution** sur le port 3306

### "Module not found"
```powershell
# Nettoyer et reconstruire
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

---

## 📝 COMMANDE RAPIDE (Après installation de Java)

```powershell
cd C:\Helma
.\start.bat
```

Puis dans un nouveau terminal:
```powershell
# Créer compte Ahmed
curl -X POST http://localhost:8082/helma/accounts/add -H "Content-Type: application/json" -d '{"userId":1,"rib":"12345678901234567890123456","accountType":"CHECKING","currency":"TND"}'

# Créditer 5000 TND
curl -X POST "http://localhost:8082/helma/accounts/credit/1?amount=5000"

# Vérifier solde
curl -X GET http://localhost:8082/helma/accounts/get/1
```

---

## 🎉 UNE FOIS JAVA INSTALLÉ

Vous pourrez:
✅ Lancer l'application normalement
✅ Créer des comptes réels
✅ Faire des transactions réelles
✅ Voir la base de données se remplir
✅ Tester NOW et SCHEDULED

---

**La clé: Java 17 DOIT être installé!**

Installez-le d'abord, puis revenez à ce guide.

---

Version: 1.0
Date: 2026-02-21
