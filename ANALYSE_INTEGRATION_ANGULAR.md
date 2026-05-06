# 📋 ANALYSE COMPLÈTE DU BACKEND LOAN-SERVICE POUR INTÉGRATION ANGULAR

**Date d'analyse :** 15 avril 2026  
**Version du projet :** 0.0.1-SNAPSHOT  
**Framework :** Spring Boot 3.2.5 (Java 17)  
**Base de données :** MySQL

---

## 📑 TABLE DES MATIÈRES

1. [1. ANALYSE GLOBALE DU PROJET](#1-analyse-globale-du-projet)
2. [2. ANALYSE DES ENTITÉS](#2-analyse-des-entités)
3. [3. ANALYSE DES ENDPOINTS API](#3-analyse-des-endpoints-api)
4. [4. SÉCURITÉ & AUTHENTIFICATION](#4-sécurité--authentification)
5. [5. CORS & CONFIGURATION](#5-cors--configuration)
6. [6. INTERFACES TYPESCRIPT POUR ANGULAR](#6-interfaces-typescript-pour-angular)
7. [7. STRUCTURE ANGULAR RECOMMANDÉE](#7-structure-angular-recommandée)
8. [8. SERVICES ANGULAR (HttpClient)](#8-services-angular-httpclient)
9. [9. COMPONENTS ET PAGES CRUD](#9-components-et-pages-crud)
10. [10. ERREURS & AMÉLIORATIONS](#10-erreurs--améliorations)

---

## 1. ANALYSE GLOBALE DU PROJET

### 📊 Vue d'ensemble

C'est un **microservice de prêts** (Loan Service) d'une architecture microservices complète.

**Technologie :**
- ✅ Spring Boot 3.2.5 (dernière version LTS)
- ✅ Spring Data JPA + Hibernate
- ✅ MySQL 8+
- ✅ Spring Security + JWT (JJWT 0.12.3)
- ✅ Swagger/OpenAPI pour documentation
- ✅ Lombok pour réduire le boilerplate

**Stack complète :**
```
Frontend (Angular) ← CORS ← Backend (Spring Boot)
                              ├── Services métier
                              ├── Calculs (Amortization, Risk Scoring, etc.)
                              ├── IA (Groq API)
                              └── DB (MySQL)
```

### 🏗️ Architecture Microservices

- **loan-service** (8081) : Gestion des prêts
- **user-service** : Gestion des utilisateurs (référencé via `userId`)
- **Intégration API Groq** : IA pour décisions d'emprunt

---

## 2. ANALYSE DES ENTITÉS

### 📊 Entités et relations

```
┌─────────────────────────────────────────────────────────────────┐
│ Loan (ENTITÉ PRINCIPALE)                                        │
├─────────────────────────────────────────────────────────────────┤
│ id (PK)                    : Long                              │
│ userId (FK vers user-service) : Long                          │
│ principalAmount            : BigDecimal (montant du prêt)    │
│ interestRate               : BigDecimal (taux d'intérêt %)   │
│ durationMonths             : Integer (durée en mois)         │
│ monthlyPayment             : BigDecimal                      │
│ startDate                  : LocalDate                       │
│ loanType                   : LoanType (PERSONAL, STUDENT, BUSINESS) │
│ riskScore                  : Integer (0-100)                │
│ status                     : LoanStatus (PENDING, ACTIVE, CLOSED, DEFAULTED) │
└─────────────────────────────────────────────────────────────────┘
         ↓
    OneToMany (Loan → RepaymentSchedule)
         ↓
┌─────────────────────────────────────────────────────────────────┐
│ RepaymentSchedule (CALENDRIER D'AMORTISSEMENT)                 │
├─────────────────────────────────────────────────────────────────┤
│ id (PK)                    : Long                              │
│ loanId (FK)                : Long (référence Loan)           │
│ installmentNumber          : Integer (n° d'échéance)        │
│ dueDate                    : LocalDate (date d'échéance)    │
│ expectedAmount             : BigDecimal (montant prévu)    │
│ paidAmount                 : BigDecimal (montant payé)     │
│ status                     : PaymentStatus (PENDING, PAID, OVERDUE) │
└─────────────────────────────────────────────────────────────────┘
         ↑
    OneToMany (Loan → LoanPayment)
         ↑
┌─────────────────────────────────────────────────────────────────┐
│ LoanPayment (ENREGISTREMENTS DE PAIEMENT)                       │
├─────────────────────────────────────────────────────────────────┤
│ id (PK)                    : Long                              │
│ loanId (FK)                : Long                             │
│ scheduleId (FK)            : Long                             │
│ amount                     : BigDecimal (montant payé)       │
│ paidAt                     : LocalDate (date du paiement)   │
│ paymentMethod              : String (CARD, BANK, MOBILE, etc.) │
│ reference                  : String (numéro de transaction)  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ LoanTransaction (JOURNAL DES TRANSACTIONS)                      │
├─────────────────────────────────────────────────────────────────┤
│ id (PK)                    : Long                              │
│ loanId (FK)                : Long                             │
│ type                       : TransactionType (DISBURSEMENT, REPAYMENT) │
│ amount                     : BigDecimal                      │
│ transactionDate            : LocalDate                       │
│ reference                  : String (numéro de transaction)  │
└─────────────────────────────────────────────────────────────────┘
```

### 📌 Énumérations

| Enum | Valeurs |
|------|---------|
| **LoanStatus** | `PENDING`, `ACTIVE`, `CLOSED`, `DEFAULTED` |
| **LoanType** | `PERSONAL`, `STUDENT`, `BUSINESS` |
| **PaymentStatus** | `PENDING`, `PAID`, `OVERDUE` |
| **TransactionType** | `DISBURSEMENT`, `REPAYMENT` |

---

## 3. ANALYSE DES ENDPOINTS API

### 🔌 Tous les endpoints disponibles

#### **A. LoanController** (`/api/loans`)

| Endpoint | Méthode | Auth | Description |
|----------|---------|------|-------------|
| `/api/loans` | POST | ROLE_YOUTH, ROLE_ADMIN | ✅ Créer un prêt |
| `/api/loans` | GET | ROLE_ADMIN, ROLE_COMPLIANCE | 📄 Lister tous les prêts (paginé) |
| `/api/loans/{id}` | GET | ROLE_YOUTH, ROLE_ADMIN, ROLE_COMPLIANCE | 📄 Récupérer un prêt |
| `/api/loans/user/{userId}` | GET | ROLE_YOUTH, ROLE_ADMIN, ROLE_COMPLIANCE | 📄 Prêts d'un utilisateur |
| `/api/loans/{id}/approve` | PUT | ROLE_ADMIN | ✅ Approuver un prêt |
| `/api/loans/{id}/reject` | PUT | ROLE_ADMIN | ✅ Rejeter un prêt |
| `/api/loans/{id}/schedule` | GET | - | 📄 Obtenir l'échéancier |
| `/api/loans/{id}/summary` | GET | - | 📄 Résumé du prêt |
| `/api/loans/{id}/ai-decision` | POST | ROLE_ADMIN | 🤖 Analyse IA |
| `/api/loans/{id}/ml-predict` | POST | ROLE_ADMIN, ROLE_COMPLIANCE | 🤖 Prédiction ML |
| `/api/loans/{id}/markov-predict` | POST | ROLE_ADMIN, ROLE_COMPLIANCE | 🤖 Prédiction Markov |

#### **B. LoanPaymentController** (`/api/loans/payments`)

| Endpoint | Méthode | Auth | Description |
|----------|---------|------|-------------|
| `/api/loans/payments` | POST | ROLE_YOUTH, ROLE_ADMIN | ✅ Effectuer un paiement |
| `/api/loans/payments/{loanId}` | GET | - | 📄 Historique des paiements |

#### **C. LoanSimulationController** (`/api/loans/simulate`)

| Endpoint | Méthode | Auth | Description |
|----------|---------|------|-------------|
| `/api/loans/simulate` | POST | **PUBLIC** | 💡 Simuler un prêt (sans création) |

#### **D. LoanStatisticsController** (`/api/loans/statistics`)

| Endpoint | Méthode | Auth | Description |
|----------|---------|------|-------------|
| `/api/loans/statistics` | GET | ROLE_ADMIN, ROLE_COMPLIANCE, ROLE_INVESTOR | 📊 Stats globales (PAR 30/60/90) |

#### **E. RepaymentScheduleController** (`/api/repayment-schedules`)

| Endpoint | Méthode | Auth | Description |
|----------|---------|------|-------------|
| `/api/repayment-schedules` | POST | - | ✅ Créer un calendrier |
| `/api/repayment-schedules` | GET | - | 📄 Lister tous les calendriers |
| `/api/repayment-schedules/{id}` | GET | - | 📄 Récupérer un calendrier |
| `/api/repayment-schedules/{id}` | DELETE | - | ❌ Supprimer un calendrier |

#### **F. LoanTransactionController** (`/api/loan-transactions`)

| Endpoint | Méthode | Auth | Description |
|----------|---------|------|-------------|
| `/api/loan-transactions` | POST | - | ✅ Créer une transaction |
| `/api/loan-transactions` | GET | - | 📄 Lister toutes les transactions |
| `/api/loan-transactions/{id}` | GET | - | 📄 Récupérer une transaction |
| `/api/loan-transactions/{id}` | DELETE | - | ❌ Supprimer une transaction |

---

### 📝 Détails des requêtes/réponses

#### **1. POST /api/loans** - Créer un prêt

**Request :**
```json
{
  "userId": 1,
  "loanType": "PERSONAL",
  "principalAmount": 10000.00,
  "durationMonths": 24
}
```

**Response (201 Created) :**
```json
{
  "id": 5,
  "userId": 1,
  "principalAmount": 10000.00,
  "interestRate": 8.50,
  "durationMonths": 24,
  "monthlyPayment": 450.25,
  "startDate": null,
  "loanType": "PERSONAL",
  "riskScore": 45,
  "status": "PENDING"
}
```

#### **2. POST /api/loans/simulate** - Simuler un prêt

**Request :**
```json
{
  "principalAmount": 10000.00,
  "interestRate": 8.50,
  "durationMonths": 24
}
```

**Response (200 OK) :**
```json
{
  "principalAmount": 10000.00,
  "interestRate": 8.50,
  "monthlyPayment": 450.25,
  "totalInterest": 810.00,
  "totalRepayment": 10810.00
}
```

#### **3. GET /api/loans/{id}/summary** - Résumé du prêt

**Response (200 OK) :**
```json
{
  "loan": {
    "id": 5,
    "userId": 1,
    "principalAmount": 10000.00,
    "interestRate": 8.50,
    "durationMonths": 24,
    "monthlyPayment": 450.25,
    "startDate": "2026-04-10",
    "loanType": "PERSONAL",
    "riskScore": 45,
    "status": "ACTIVE"
  },
  "totalPaid": 900.50,
  "totalRemaining": 9909.50,
  "nextPayment": {
    "id": 102,
    "loanId": 5,
    "installmentNumber": 2,
    "dueDate": "2026-05-10",
    "expectedAmount": 450.25,
    "paidAmount": 0,
    "status": "PENDING"
  }
}
```

#### **4. GET /api/loans/statistics** - Statistiques globales

**Response (200 OK) :**
```json
{
  "totalLoans": 150,
  "activeLoans": 120,
  "defaultedLoans": 5,
  "par30": 0.02,
  "par60": 0.01,
  "par90": 0.005
}
```

#### **5. POST /api/loans/payments** - Effectuer un paiement

**Request :**
```json
{
  "loanId": 5,
  "scheduleId": 102,
  "amount": 450.25,
  "paymentMethod": "CARD"
}
```

**Response (201 Created) :**
```json
{
  "id": 201,
  "loanId": 5,
  "scheduleId": 102,
  "amount": 450.25,
  "paidAt": "2026-04-15",
  "paymentMethod": "CARD",
  "reference": "PAY-20260415-00201"
}
```

---

## 4. SÉCURITÉ & AUTHENTIFICATION

### 🔐 Configuration JWT

#### Configuration (dans `application.properties`)
```properties
jwt.secret=helma2025SecretKeyForJWTSharedBetweenP1AndP4MicroservicesVeryLongKey
jwt.expiration=86400000  # 24 heures en millisecondes
```

#### Rôles d'autorisation

| Rôle | Permissions |
|------|-------------|
| **ROLE_YOUTH** | Créer prêt, payer, voir ses prêts |
| **ROLE_ADMIN** | Approuver/rejeter, IA, ML, stats |
| **ROLE_COMPLIANCE** | Voir tous les prêts, ML, stats |
| **ROLE_INVESTOR** | Voir stats |
| **PUBLIC** | Simuler un prêt uniquement |

#### Flow d'authentification

```
User (Angular) → Login Service
                        ↓
                   Vérifie credentials
                        ↓
               Génère JWT Token
                        ↓
   Angular stocke token en localStorage
                        ↓
   Chaque requête : Authorization: Bearer <token>
                        ↓
         JwtAuthFilter valide token
                        ↓
   Si valide → Extrait username + roles → Continue
   Si invalide → 401 Unauthorized
```

### 📌 Points importants

- ✅ JWT inclus dans chaque requête via header `Authorization: Bearer <token>`
- ✅ Rôles extraits du token (pas de DB users ici)
- ✅ Sessions stateless (STATELESS_SESSION_POLICY)
- ✅ CSRF désactivé (API REST)
- ✅ Token valide 24h

---

## 5. CORS & CONFIGURATION

### ✅ CORS est déjà activé

```java
@CrossOrigin(origins = "*")  // Sur chaque controller
```

**⚠️ En production, remplacer par :**
```java
@CrossOrigin(origins = "https://your-angular-app.com")
```

### 🌐 Ports

| Service | Port | URL |
|---------|------|-----|
| Backend | 8081 | `http://localhost:8081` |
| Angular (dev) | 4200 | `http://localhost:4200` |

### 📚 Documentation Swagger/OpenAPI

- **URL :** `http://localhost:8081/swagger-ui.html`
- **JSON :** `http://localhost:8081/api-docs`
- **Authentification Bearer activée** ✅

---

## 6. INTERFACES TYPESCRIPT POUR ANGULAR

Voici les interfaces TypeScript à créer dans votre projet Angular :

### 📁 Structure de fichiers recommandée

```
src/
├── app/
│   ├── models/
│   │   ├── loan.model.ts
│   │   ├── payment.model.ts
│   │   ├── enums.model.ts
│   │   └── index.ts (barrel export)
│   ├── services/
│   │   ├── loan.service.ts
│   │   ├── payment.service.ts
│   │   ├── auth.service.ts
│   │   └── api.service.ts (base)
│   └── ...
```

### 📄 Models TypeScript

#### **enums.model.ts**
```typescript
export enum LoanStatus {
  PENDING = 'PENDING',
  ACTIVE = 'ACTIVE',
  CLOSED = 'CLOSED',
  DEFAULTED = 'DEFAULTED'
}

export enum LoanType {
  PERSONAL = 'PERSONAL',
  STUDENT = 'STUDENT',
  BUSINESS = 'BUSINESS'
}

export enum PaymentStatus {
  PENDING = 'PENDING',
  PAID = 'PAID',
  OVERDUE = 'OVERDUE'
}

export enum TransactionType {
  DISBURSEMENT = 'DISBURSEMENT',
  REPAYMENT = 'REPAYMENT'
}
```

#### **loan.model.ts**
```typescript
import { LoanStatus, LoanType } from './enums.model';

export interface Loan {
  id: number;
  userId: number;
  principalAmount: number;
  interestRate: number;
  durationMonths: number;
  monthlyPayment?: number;
  startDate?: string; // ISO 8601 (2026-04-15)
  loanType: LoanType;
  riskScore?: number;
  status: LoanStatus;
}

export interface CreateLoanRequest {
  userId: number;
  loanType: LoanType;
  principalAmount: number;
  durationMonths: number;
}

export interface LoanSummary {
  loan: Loan;
  totalPaid: number;
  totalRemaining: number;
  nextPayment: RepaymentSchedule | null;
}
```

#### **payment.model.ts**
```typescript
import { PaymentStatus, TransactionType } from './enums.model';

export interface RepaymentSchedule {
  id: number;
  loanId: number;
  installmentNumber: number;
  dueDate: string; // ISO 8601
  expectedAmount: number;
  paidAmount: number;
  status: PaymentStatus;
}

export interface LoanPayment {
  id: number;
  loanId: number;
  scheduleId: number;
  amount: number;
  paidAt: string; // ISO 8601
  paymentMethod: string;
  reference: string;
}

export interface CreatePaymentRequest {
  loanId: number;
  scheduleId: number;
  amount: number;
  paymentMethod: string;
}

export interface LoanTransaction {
  id: number;
  loanId: number;
  type: TransactionType;
  amount: number;
  transactionDate: string; // ISO 8601
  reference: string;
}

export interface LoanStatistics {
  totalLoans: number;
  activeLoans: number;
  defaultedLoans: number;
  par30: number;
  par60: number;
  par90: number;
}
```

#### **simulation.model.ts**
```typescript
export interface SimulationRequest {
  principalAmount: number;
  interestRate: number;
  durationMonths: number;
}

export interface SimulationResult {
  principalAmount: number;
  interestRate: number;
  monthlyPayment: number;
  totalInterest: number;
  totalRepayment: number;
}
```

#### **auth.model.ts**
```typescript
export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string; // JWT Token
  username: string;
  roles: string[];
}

export interface AuthToken {
  token: string;
  expiresIn: number; // en ms (86400000)
}
```

---

## 7. STRUCTURE ANGULAR RECOMMANDÉE

### 📂 Architecture complète

```
src/app/
│
├── auth/
│   ├── login/
│   │   ├── login.component.ts
│   │   └── login.component.html
│   ├── auth.service.ts
│   ├── auth.guard.ts (vérifier JWT)
│   └── jwt.interceptor.ts (ajouter token aux headers)
│
├── loans/
│   ├── shared/
│   │   └── loan.service.ts
│   │
│   ├── loan-list/
│   │   ├── loan-list.component.ts
│   │   └── loan-list.component.html
│   │
│   ├── loan-detail/
│   │   ├── loan-detail.component.ts
│   │   └── loan-detail.component.html
│   │
│   ├── loan-create/
│   │   ├── loan-create.component.ts
│   │   └── loan-create.component.html
│   │
│   ├── loan-simulation/
│   │   ├── loan-simulation.component.ts
│   │   └── loan-simulation.component.html
│   │
│   └── loan-routing.module.ts
│
├── payments/
│   ├── payment.service.ts
│   ├── payment-form/
│   │   ├── payment-form.component.ts
│   │   └── payment-form.component.html
│   │
│   ├── payment-history/
│   │   ├── payment-history.component.ts
│   │   └── payment-history.component.html
│   │
│   └── payment-routing.module.ts
│
├── dashboard/
│   ├── dashboard.component.ts
│   ├── dashboard.component.html
│   └── dashboard.service.ts
│
├── shared/
│   ├── models/ (enums.model.ts, loan.model.ts, etc.)
│   ├── services/
│   │   ├── api.service.ts (base HTTP)
│   │   └── auth.service.ts
│   └── interceptors/
│       ├── jwt.interceptor.ts
│       └── error.interceptor.ts
│
├── app.module.ts
├── app-routing.module.ts
└── app.component.ts
```

### 🔧 Modules à installer

```bash
ng add @angular/material  # UI components
npm install axios         # OU HttpClient natif
npm install date-fns      # Date formatting
```

---

## 8. SERVICES ANGULAR (HttpClient)

### 📡 API Service (Base)

#### **api.service.ts**
```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = 'http://localhost:8081';

  constructor(private http: HttpClient) { }

  // ==================== LOANS ====================
  createLoan(loanData: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/loans`, loanData);
  }

  getLoanById(id: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/api/loans/${id}`);
  }

  getLoansByUser(userId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/api/loans/user/${userId}`);
  }

  getAllLoans(page: number = 0, size: number = 10): Observable<any> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    return this.http.get(`${this.baseUrl}/api/loans`, { params });
  }

  approveLoan(id: number): Observable<any> {
    return this.http.put(`${this.baseUrl}/api/loans/${id}/approve`, {});
  }

  rejectLoan(id: number): Observable<any> {
    return this.http.put(`${this.baseUrl}/api/loans/${id}/reject`, {});
  }

  getLoanSchedule(loanId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/api/loans/${loanId}/schedule`);
  }

  getLoanSummary(loanId: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/api/loans/${loanId}/summary`);
  }

  getAIDecision(loanId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/loans/${loanId}/ai-decision`, {});
  }

  getMLPrediction(loanId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/loans/${loanId}/ml-predict`, {});
  }

  getMarkovPrediction(loanId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/loans/${loanId}/markov-predict`, {});
  }

  // ==================== PAYMENTS ====================
  createPayment(paymentData: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/loans/payments`, paymentData);
  }

  getPaymentsByLoan(loanId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/api/loans/payments/${loanId}`);
  }

  // ==================== SIMULATION ====================
  simulateLoan(simulationData: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/loans/simulate`, simulationData);
  }

  // ==================== STATISTICS ====================
  getStatistics(): Observable<any> {
    return this.http.get(`${this.baseUrl}/api/loans/statistics`);
  }

  // ==================== REPAYMENT SCHEDULES ====================
  getRepaymentSchedules(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/api/repayment-schedules`);
  }

  getRepaymentSchedule(id: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/api/repayment-schedules/${id}`);
  }

  createRepaymentSchedule(scheduleData: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/repayment-schedules`, scheduleData);
  }

  deleteRepaymentSchedule(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/api/repayment-schedules/${id}`);
  }

  // ==================== TRANSACTIONS ====================
  createTransaction(transactionData: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/loan-transactions`, transactionData);
  }

  getTransactions(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/api/loan-transactions`);
  }

  getTransaction(id: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/api/loan-transactions/${id}`);
  }

  deleteTransaction(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/api/loan-transactions/${id}`);
  }
}
```

### 🔐 Auth Service

#### **auth.service.ts**
```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private baseUrl = 'http://localhost:8081';
  private tokenKey = 'auth_token';
  private userKey = 'user_info';
  
  private userSubject = new BehaviorSubject<any>(this.getUserFromStorage());
  public user$ = this.userSubject.asObservable();

  constructor(private http: HttpClient) { }

  login(username: string, password: string): Observable<any> {
    // ⚠️ À adapter selon votre microservice d'auth (P1)
    return this.http.post(`${this.baseUrl}/auth/login`, {
      username,
      password
    }).pipe(
      tap(response => {
        this.setToken(response.token);
        this.setUser(response);
        this.userSubject.next(response);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.userSubject.next(null);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  getUser(): any {
    return this.userSubject.value;
  }

  setUser(user: any): void {
    localStorage.setItem(this.userKey, JSON.stringify(user));
    this.userSubject.next(user);
  }

  private getUserFromStorage(): any {
    const user = localStorage.getItem(this.userKey);
    return user ? JSON.parse(user) : null;
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  hasRole(role: string): boolean {
    const user = this.getUser();
    return user && user.roles && user.roles.includes(role);
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this.hasRole(role));
  }
}
```

### 🛡️ JWT Interceptor

#### **jwt.interceptor.ts**
```typescript
import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService) { }

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.authService.getToken();

    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(request);
  }
}
```

### 📱 Error Interceptor

#### **error.interceptor.ts**
```typescript
import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService, private router: Router) { }

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Token expiré ou invalide
          this.authService.logout();
          this.router.navigate(['/login']);
        } else if (error.status === 403) {
          // Accès non autorisé
          console.error('Accès refusé - permissions insuffisantes');
        }
        return throwError(() => error);
      })
    );
  }
}
```

### 🔒 Auth Guard

#### **auth.guard.ts**
```typescript
import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private authService: AuthService, private router: Router) { }

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return false;
    }

    // Vérifier les rôles si requis
    const requiredRoles = route.data['roles'];
    if (requiredRoles && !this.authService.hasAnyRole(requiredRoles)) {
      this.router.navigate(['/forbidden']);
      return false;
    }

    return true;
  }
}
```

### 📊 Loan Service

#### **loan.service.ts**
```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Loan, CreateLoanRequest, LoanSummary } from '../models/loan.model';

@Injectable({
  providedIn: 'root'
})
export class LoanService {
  private baseUrl = 'http://localhost:8081/api/loans';

  constructor(private http: HttpClient) { }

  createLoan(loanData: CreateLoanRequest): Observable<Loan> {
    return this.http.post<Loan>(this.baseUrl, loanData);
  }

  getLoanById(id: number): Observable<Loan> {
    return this.http.get<Loan>(`${this.baseUrl}/${id}`);
  }

  getLoansByUser(userId: number): Observable<Loan[]> {
    return this.http.get<Loan[]>(`${this.baseUrl}/user/${userId}`);
  }

  getAllLoans(page: number = 0, size: number = 10): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get(`${this.baseUrl}`, { params });
  }

  approveLoan(id: number): Observable<Loan> {
    return this.http.put<Loan>(`${this.baseUrl}/${id}/approve`, {});
  }

  rejectLoan(id: number): Observable<Loan> {
    return this.http.put<Loan>(`${this.baseUrl}/${id}/reject`, {});
  }

  getLoanSummary(loanId: number): Observable<LoanSummary> {
    return this.http.get<LoanSummary>(`${this.baseUrl}/${loanId}/summary`);
  }
}
```

---

## 9. COMPONENTS ET PAGES CRUD

### 📑 List Component

#### **loan-list.component.ts**
```typescript
import { Component, OnInit } from '@angular/core';
import { LoanService } from '../shared/loan.service';
import { Loan } from '../../models/loan.model';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-loan-list',
  templateUrl: './loan-list.component.html',
  styleUrls: ['./loan-list.component.css']
})
export class LoanListComponent implements OnInit {
  loans: Loan[] = [];
  loading = false;
  error: string | null = null;
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;

  constructor(
    private loanService: LoanService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.loadLoans();
  }

  loadLoans(): void {
    this.loading = true;
    this.error = null;

    // Si admin/compliance : voir tous les prêts
    // Sinon : voir prêts de l'utilisateur
    if (this.authService.hasAnyRole(['ROLE_ADMIN', 'ROLE_COMPLIANCE'])) {
      this.loanService.getAllLoans(this.currentPage, this.pageSize)
        .subscribe({
          next: (response) => {
            this.loans = response.content;
            this.totalElements = response.totalElements;
            this.loading = false;
          },
          error: (err) => {
            this.error = 'Erreur lors du chargement des prêts';
            this.loading = false;
          }
        });
    } else {
      const userId = this.authService.getUser().id;
      this.loanService.getLoansByUser(userId)
        .subscribe({
          next: (loans) => {
            this.loans = loans;
            this.loading = false;
          },
          error: (err) => {
            this.error = 'Erreur lors du chargement de vos prêts';
            this.loading = false;
          }
        });
    }
  }

  deleteLoan(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce prêt ?')) {
      // Implémenter le delete si disponible
    }
  }

  getTotalLoans(): string {
    return this.totalElements.toString();
  }
}
```

#### **loan-list.component.html**
```html
<div class="container mt-5">
  <h2>Liste des prêts</h2>

  <div *ngIf="error" class="alert alert-danger">{{ error }}</div>
  <div *ngIf="loading" class="spinner-border" role="status">
    <span class="visually-hidden">Chargement...</span>
  </div>

  <table class="table table-striped" *ngIf="!loading && loans.length > 0">
    <thead>
      <tr>
        <th>#</th>
        <th>Montant</th>
        <th>Type</th>
        <th>Taux</th>
        <th>Durée (mois)</th>
        <th>Statut</th>
        <th>Actions</th>
      </tr>
    </thead>
    <tbody>
      <tr *ngFor="let loan of loans">
        <td>{{ loan.id }}</td>
        <td>{{ loan.principalAmount | currency:'TND' }}</td>
        <td>{{ loan.loanType }}</td>
        <td>{{ loan.interestRate }}%</td>
        <td>{{ loan.durationMonths }}</td>
        <td>
          <span [ngClass]="'badge bg-' + (loan.status === 'ACTIVE' ? 'success' : 'warning')">
            {{ loan.status }}
          </span>
        </td>
        <td>
          <a [routerLink]="['/loans', loan.id]" class="btn btn-sm btn-primary">Voir</a>
        </td>
      </tr>
    </tbody>
  </table>

  <div *ngIf="!loading && loans.length === 0" class="alert alert-info">
    Aucun prêt trouvé.
  </div>
</div>
```

### 📋 Detail Component

#### **loan-detail.component.ts**
```typescript
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { LoanService } from '../shared/loan.service';
import { Loan, LoanSummary } from '../../models/loan.model';
import { RepaymentSchedule } from '../../models/payment.model';

@Component({
  selector: 'app-loan-detail',
  templateUrl: './loan-detail.component.html',
  styleUrls: ['./loan-detail.component.css']
})
export class LoanDetailComponent implements OnInit {
  loan: Loan | null = null;
  summary: LoanSummary | null = null;
  schedule: RepaymentSchedule[] = [];
  loading = false;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private loanService: LoanService
  ) { }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const id = +params['id'];
      this.loadLoan(id);
    });
  }

  loadLoan(id: number): void {
    this.loading = true;
    
    this.loanService.getLoanById(id).subscribe({
      next: (loan) => {
        this.loan = loan;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement du prêt';
        this.loading = false;
      }
    });

    this.loanService.getLoanSummary(id).subscribe({
      next: (summary) => {
        this.summary = summary;
        this.schedule = []; // Charger l'échéancier
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement du résumé';
        this.loading = false;
      }
    });
  }
}
```

### ➕ Create Component

#### **loan-create.component.ts**
```typescript
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { LoanService } from '../shared/loan.service';
import { CreateLoanRequest, LoanType } from '../../models/loan.model';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-loan-create',
  templateUrl: './loan-create.component.html',
  styleUrls: ['./loan-create.component.css']
})
export class LoanCreateComponent {
  formData: CreateLoanRequest = {
    userId: 0,
    loanType: LoanType.PERSONAL,
    principalAmount: 0,
    durationMonths: 0
  };
  loanTypes = Object.values(LoanType);
  loading = false;
  error: string | null = null;

  constructor(
    private loanService: LoanService,
    private authService: AuthService,
    private router: Router
  ) {
    // Récupérer l'ID de l'utilisateur courant
    const user = this.authService.getUser();
    if (user) {
      this.formData.userId = user.id;
    }
  }

  createLoan(): void {
    if (!this.validateForm()) {
      this.error = 'Veuillez remplir tous les champs';
      return;
    }

    this.loading = true;
    this.error = null;

    this.loanService.createLoan(this.formData).subscribe({
      next: (loan) => {
        this.router.navigate(['/loans', loan.id]);
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors de la création du prêt';
        this.loading = false;
      }
    });
  }

  private validateForm(): boolean {
    return this.formData.principalAmount > 0 &&
           this.formData.durationMonths > 0;
  }
}
```

### 💳 Payment Component

#### **payment-form.component.ts**
```typescript
import { Component, Input } from '@angular/core';
import { PaymentService } from '../payment.service';
import { RepaymentSchedule } from '../../models/payment.model';

@Component({
  selector: 'app-payment-form',
  templateUrl: './payment-form.component.html',
  styleUrls: ['./payment-form.component.css']
})
export class PaymentFormComponent {
  @Input() loanId!: number;
  @Input() schedule: RepaymentSchedule | null = null;

  paymentData = {
    loanId: 0,
    scheduleId: 0,
    amount: 0,
    paymentMethod: 'CARD'
  };
  loading = false;
  error: string | null = null;
  success = false;

  paymentMethods = ['CARD', 'BANK', 'MOBILE', 'CHECK'];

  constructor(private paymentService: PaymentService) { }

  submitPayment(): void {
    this.paymentData.loanId = this.loanId;
    if (this.schedule) {
      this.paymentData.scheduleId = this.schedule.id;
      this.paymentData.amount = this.schedule.expectedAmount;
    }

    this.loading = true;
    this.error = null;
    this.success = false;

    this.paymentService.createPayment(this.paymentData).subscribe({
      next: (payment) => {
        this.success = true;
        this.loading = false;
        // Réinitialiser le formulaire
        this.paymentData = {
          loanId: 0,
          scheduleId: 0,
          amount: 0,
          paymentMethod: 'CARD'
        };
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du paiement';
        this.loading = false;
      }
    });
  }
}
```

### 📊 Dashboard Component

#### **dashboard.component.ts**
```typescript
import { Component, OnInit } from '@angular/core';
import { DashboardService } from './dashboard.service';
import { LoanStatistics } from '../../models/payment.model';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  statistics: LoanStatistics | null = null;
  loading = false;

  constructor(private dashboardService: DashboardService) { }

  ngOnInit(): void {
    this.loadStatistics();
  }

  loadStatistics(): void {
    this.loading = true;
    this.dashboardService.getStatistics().subscribe({
      next: (stats) => {
        this.statistics = stats;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des stats', err);
        this.loading = false;
      }
    });
  }
}
```

---

## 10. ERREURS & AMÉLIORATIONS

### ⚠️ Problèmes détectés

| # | Problème | Sévérité | Solution |
|---|----------|----------|----------|
| 1 | **Pas de gestion d'erreurs personnalisée** | 🔴 HAUTE | Créer `GlobalExceptionHandler` |
| 2 | **DTOs demandent validation côté Backend** | 🟡 MOYENNE | Ajouter `@Valid` sur tous les endpoints |
| 3 | **Pas de pagination sur todos les endpoints** | 🟡 MOYENNE | Ajouter `Pageable` aux GET list |
| 4 | **Swagger n'est pas accessible sans auth** | 🟢 BASSE | Configuration JWT Bearer faite ✅ |
| 5 | **Microservice pas isolé des dépendances** | 🟡 MOYENNE | Ajouter circuit breaker (Resilience4j) |
| 6 | **Pas de logging structuré** | 🟡 MOYENNE | Utiliser ELK (Elasticsearch) ou DataDog |
| 7 | **Secrets en dur dans `application.properties`** | 🔴 HAUTE | Utiliser `application-prod.properties` ou Vault |
| 8 | **CORS trop permissif** | 🔴 HAUTE | Restreindre `origins = "*"` en prod |
| 9 | **Manque tests unitaires/intégration** | 🟡 MOYENNE | Ajouter JUnit5 + MockMvc |
| 10 | **Pas de rate limiting** | 🟡 MOYENNE | Ajouter Spring Cloud Gateway |

### ✅ Améliorations recommandées

#### **1. Global Exception Handler**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LoanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLoanNotFound(LoanNotFoundException e) {
        ErrorResponse error = new ErrorResponse(
            404,
            "LOAN_NOT_FOUND",
            e.getMessage(),
            System.currentTimeMillis()
        );
        return ResponseEntity.status(404).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors()
            .stream()
            .map(ObjectError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        
        ErrorResponse error = new ErrorResponse(
            400,
            "VALIDATION_ERROR",
            message,
            System.currentTimeMillis()
        );
        return ResponseEntity.status(400).body(error);
    }
}
```

#### **2. Logging structuré**

```java
@Slf4j
@Service
public class LoanService {
    public Loan createLoan(LoanRequestDTO dto, int riskScore, BigDecimal interestRate) {
        log.info("Creating loan for user: {} amount: {} type: {}", 
                 dto.getUserId(), dto.getPrincipalAmount(), dto.getLoanType());
        // ...
        log.debug("Loan created with ID: {} riskScore: {}", loan.getId(), riskScore);
    }
}
```

#### **3. Validation avancée**

```java
@PostMapping
public ResponseEntity<Loan> createLoan(@Valid @RequestBody LoanRequestDTO dto) {
    // Validation personnalisée
    if (dto.getPrincipalAmount().compareTo(BigDecimal.valueOf(100)) < 0) {
        throw new ValidationException("Le montant minimum est 100");
    }
    // ...
}
```

#### **4. Rate Limiting (Bucket4j)**

```java
@Configuration
public class RateLimitConfig {
    @Bean
    public Bandwidth limit() {
        return Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
    }
}
```

#### **5. Circuit Breaker (Resilience4j)**

```java
@Service
@CircuitBreaker(name = "loanService", fallbackMethod = "fallback")
@Retry(name = "loanService")
@Timeout(name = "loanService")
public class LoanService {
    public Loan createLoan(LoanRequestDTO dto) {
        // Appel à user-service qui peut échouer
    }

    public Loan fallback(LoanRequestDTO dto, Exception e) {
        log.warn("Fallback: Service unavailable");
        return Loan.builder().status(LoanStatus.PENDING).build();
    }
}
```

---

## 📱 EXEMPLE COMPLET : Angular Login + Dashboard

### **app-routing.module.ts**
```typescript
const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'loans',
    canActivate: [AuthGuard],
    children: [
      { path: '', component: LoanListComponent },
      { path: 'create', component: LoanCreateComponent },
      { path: ':id', component: LoanDetailComponent },
      { path: ':id/pay', component: PaymentFormComponent }
    ]
  },
  { path: 'forbidden', component: ForbiddenComponent },
  { path: '**', redirectTo: '/dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
```

### **app.module.ts**
```typescript
@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    DashboardComponent,
    LoanListComponent,
    LoanDetailComponent,
    LoanCreateComponent,
    PaymentFormComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    ReactiveFormsModule,
    AppRoutingModule,
    BrowserAnimationsModule
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
```

---

## 🚀 CHECKLIST INTÉGRATION ANGULAR

- [ ] Créer le projet Angular
- [ ] Installer les dépendances (Angular Material, etc.)
- [ ] Créer les interfaces TypeScript dans `models/`
- [ ] Créer les services (`api.service.ts`, `auth.service.ts`, etc.)
- [ ] Configurer les intercepteurs JWT
- [ ] Créer les components (list, detail, create, payment)
- [ ] Implémenter le formulaire de login
- [ ] Tester l'authentification JWT
- [ ] Afficher les données des prêts
- [ ] Implémenter CRUD complet
- [ ] Ajouter la pagination
- [ ] Tester avec Swagger (http://localhost:8081/swagger-ui.html)
- [ ] Déployer en production

---

## 📞 CONTACT & SUPPORT

**Backend Service :** Loan Service v0.0.1  
**Port :** 8081  
**Documentation API :** http://localhost:8081/swagger-ui.html

---

**Analyse complétée le 15/04/2026** ✅

