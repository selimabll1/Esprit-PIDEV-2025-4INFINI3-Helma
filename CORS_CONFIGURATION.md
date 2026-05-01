# Configuration CORS - Backend Spring Boot

**Date:** 29 Avril 2026  
**Objectif:** Autoriser les requêtes depuis le frontend Angular (port 4200)

---

## Fichiers Créés

### 1. **CorsConfig.java** (Recommandé)
**Emplacement:** `src/main/java/com/helma/helmabackend/config/CorsConfig.java`

Configuration CORS utilisant un `CorsFilter` bean.

**Avantages:**
- ✅ Plus de contrôle sur la configuration
- ✅ Fonctionne avec tous les types de requêtes
- ✅ Gère mieux les requêtes OPTIONS (preflight)

### 2. **WebConfig.java** (Alternative)
**Emplacement:** `src/main/java/com/helma/helmabackend/config/WebConfig.java`

Configuration CORS utilisant `WebMvcConfigurer`.

**Avantages:**
- ✅ Plus simple et concis
- ✅ Intégration native avec Spring MVC

---

## ⚠️ Important: Choisir UNE Configuration

**Vous devez utiliser SOIT `CorsConfig.java` SOIT `WebConfig.java`, mais PAS les deux!**

### Option 1: Utiliser CorsConfig.java (Recommandé)
```bash
# Garder: CorsConfig.java
# Supprimer ou commenter: WebConfig.java
```

### Option 2: Utiliser WebConfig.java
```bash
# Supprimer ou commenter: CorsConfig.java
# Garder: WebConfig.java
```

---

## Configuration Actuelle

### Origines Autorisées
```java
"http://localhost:4200"      // Frontend Angular principal
"http://localhost:4201"      // Port alternatif
"http://127.0.0.1:4200"      // Variante avec 127.0.0.1
```

### Méthodes HTTP Autorisées
```java
GET, POST, PUT, DELETE, PATCH, OPTIONS
```

### Headers Autorisés
```java
Origin
Content-Type
Accept
Authorization
Access-Control-Request-Method
Access-Control-Request-Headers
X-Requested-With
```

### Headers Exposés
```java
Access-Control-Allow-Origin
Access-Control-Allow-Credentials
Authorization
```

### Credentials
```java
allowCredentials = true  // Autorise les cookies et headers d'authentification
```

### Cache
```java
maxAge = 3600  // 1 heure (en secondes)
```

---

## Test de la Configuration

### 1. Redémarrer le Backend
```bash
cd helmabackend
mvn clean spring-boot:run
```

### 2. Vérifier les Logs
Vous devriez voir:
```
Started HelmabackendApplication in X.XXX seconds
```

### 3. Tester avec cURL
```bash
# Test OPTIONS (preflight)
curl -X OPTIONS http://localhost:8082/helma/api/equipements \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: GET" \
  -v

# Test GET
curl -X GET http://localhost:8082/helma/api/equipements \
  -H "Origin: http://localhost:4200" \
  -v
```

**Réponse attendue:**
```
< Access-Control-Allow-Origin: http://localhost:4200
< Access-Control-Allow-Credentials: true
< Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
```

### 4. Tester depuis le Frontend
```bash
cd Esprit-PIDEV-2025-4INFINI3-Helma
npm start
```

Ouvrez la console du navigateur (F12) et vérifiez qu'il n'y a pas d'erreurs CORS.

---

## Problèmes Courants et Solutions

### ❌ Erreur: "CORS policy: No 'Access-Control-Allow-Origin' header"

**Cause:** La configuration CORS n'est pas chargée

**Solutions:**
1. Vérifier que le fichier est dans le bon package: `com.helma.helmabackend.config`
2. Vérifier l'annotation `@Configuration`
3. Redémarrer le backend
4. Vérifier les logs pour des erreurs de démarrage

---

### ❌ Erreur: "CORS policy: Credentials flag is 'true'"

**Cause:** Le frontend envoie des credentials mais le backend ne les autorise pas

**Solution:**
Vérifier que `allowCredentials(true)` est bien configuré

---

### ❌ Erreur: "CORS policy: Method not allowed"

**Cause:** La méthode HTTP n'est pas dans la liste des méthodes autorisées

**Solution:**
Ajouter la méthode dans `allowedMethods`:
```java
.allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
```

---

### ❌ Erreur: "CORS policy: Request header not allowed"

**Cause:** Un header personnalisé n'est pas autorisé

**Solution:**
Ajouter le header dans `allowedHeaders`:
```java
.allowedHeaders("*")  // Autorise tous les headers
// OU
.allowedHeaders("Origin", "Content-Type", "Accept", "Authorization", "X-Custom-Header")
```

---

## Configuration pour Production

Pour la production, modifiez les origines autorisées:

```java
// CorsConfig.java
corsConfiguration.setAllowedOrigins(Arrays.asList(
    "https://votre-domaine.com",
    "https://www.votre-domaine.com"
));
```

**⚠️ Ne JAMAIS utiliser `*` avec `allowCredentials(true)` en production!**

---

## Configuration Avancée

### Autoriser Plusieurs Domaines Dynamiquement

```java
@Bean
public CorsFilter corsFilter() {
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    
    // Lire depuis application.properties
    corsConfiguration.setAllowedOriginPatterns(Arrays.asList(
        "http://localhost:*",
        "https://*.votre-domaine.com"
    ));
    
    // ... reste de la configuration
}
```

### Configuration par Endpoint

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    // Configuration pour les APIs publiques
    registry.addMapping("/api/public/**")
            .allowedOrigins("*")
            .allowedMethods("GET")
            .allowCredentials(false);
    
    // Configuration pour les APIs privées
    registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:4200")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowCredentials(true);
}
```

---

## Annotations @CrossOrigin (Alternative)

Vous pouvez aussi utiliser l'annotation `@CrossOrigin` directement sur les controllers:

```java
@RestController
@RequestMapping("/api/equipements")
@CrossOrigin(
    origins = "http://localhost:4200",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE},
    allowCredentials = "true"
)
public class EquipementController {
    // ...
}
```

**⚠️ Attention:** Cette approche nécessite d'ajouter l'annotation sur TOUS les controllers.

---

## Vérification de la Configuration

### Checklist
- [ ] Un seul fichier de configuration CORS actif (CorsConfig OU WebConfig)
- [ ] Annotation `@Configuration` présente
- [ ] Package correct: `com.helma.helmabackend.config`
- [ ] Port 4200 dans les origines autorisées
- [ ] Méthodes HTTP nécessaires autorisées
- [ ] `allowCredentials(true)` si vous utilisez l'authentification
- [ ] Backend redémarré après les modifications
- [ ] Pas d'erreurs dans les logs du backend
- [ ] Pas d'erreurs CORS dans la console du navigateur

---

## Logs de Débogage

Pour activer les logs CORS détaillés, ajoutez dans `application.properties`:

```properties
# Logs CORS
logging.level.org.springframework.web.cors=DEBUG
logging.level.org.springframework.web.filter.CorsFilter=DEBUG
```

---

## Résumé

✅ **Configuration CORS créée** pour autoriser le frontend Angular  
✅ **Port 4200 autorisé** avec credentials  
✅ **Toutes les méthodes HTTP** autorisées  
✅ **Headers nécessaires** configurés  
✅ **Deux approches disponibles** (CorsConfig ou WebConfig)  

**Choisissez UNE configuration et redémarrez le backend!** 🚀
