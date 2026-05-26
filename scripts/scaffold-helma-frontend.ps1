param(
  [string]$AppName = "helma-frontend",
  [string]$BackendUrl = "http://localhost:8080",
  [string]$FrontendRoot = "frontend",
  [string]$AngularCliVersion = "17",
  [switch]$SkipMaterial,
  [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

# ==============================
# Logging helpers
# ==============================
function Write-Section {
  param([string]$Message)
  Write-Host "`n============================================================" -ForegroundColor DarkCyan
  Write-Host "$Message" -ForegroundColor Cyan
  Write-Host "============================================================" -ForegroundColor DarkCyan
}

function Write-Info {
  param([string]$Message)
  Write-Host "[INFO] $Message" -ForegroundColor Gray
}

function Write-Success {
  param([string]$Message)
  Write-Host "[OK]   $Message" -ForegroundColor Green
}

# ==============================
# Validation helpers
# ==============================
function Assert-Command {
  param([string]$Name)
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "La commande '$Name' est introuvable. Installe-la puis relance le script."
  }
}

function Ensure-Directory {
  param([string]$Path)
  if (-not (Test-Path $Path)) {
    New-Item -ItemType Directory -Path $Path | Out-Null
    Write-Info "Dossier cree: $Path"
  }
}

function Write-File {
  param(
    [string]$Path,
    [string]$Content
  )
  $parentDir = Split-Path -Parent $Path
  if ($parentDir -and -not (Test-Path $parentDir)) {
    New-Item -ItemType Directory -Path $parentDir -Force | Out-Null
  }
  Set-Content -Path $Path -Encoding UTF8 -Value $Content
  Write-Info "Fichier mis a jour: $Path"
}

# ==============================
# Angular generation helper
# ==============================
function Invoke-NgGenerate {
  param([string]$Arguments)
  Write-Info "ng g $Arguments"
  $argList = @('ng', 'g') + ($Arguments -split ' ')
  & npx @argList
  if ($LASTEXITCODE -ne 0) {
    throw "Echec de generation Angular: ng g $Arguments"
  }
}

# ==============================
# Script start
# ==============================
Write-Section "HELMA Frontend Scaffold (NgModules)"
Write-Info "AppName          : $AppName"
Write-Info "BackendUrl       : $BackendUrl"
Write-Info "FrontendRoot     : $FrontendRoot"
Write-Info "Angular CLI ver. : $AngularCliVersion"
Write-Info "SkipMaterial     : $SkipMaterial"
Write-Info "SkipBuild        : $SkipBuild"

Write-Section "1) Verification des prerequis"
Assert-Command node
Assert-Command npm
Assert-Command npx
Write-Success "Prerequis valides"

$RepoRoot = Get-Location
$FrontendBasePath = Join-Path $RepoRoot $FrontendRoot
$AppPath = Join-Path $FrontendBasePath $AppName

Ensure-Directory -Path $FrontendBasePath

Write-Section "2) Creation du projet Angular"
if (-not (Test-Path $AppPath)) {
  Push-Location $FrontendBasePath
  npx -y @angular/cli@$AngularCliVersion new $AppName --routing --style=scss --no-standalone --skip-git --package-manager=npm
  if ($LASTEXITCODE -ne 0) {
    throw "Echec de creation du projet Angular"
  }
  Pop-Location
  Write-Success "Projet Angular cree: $AppPath"
} else {
  Write-Info "Projet deja existant, mode incrementiel active"
}

Push-Location $AppPath

Write-Section "3) Installation Angular Material"
if (-not $SkipMaterial) {
  $packageJsonPath = "package.json"
  $materialAlreadyInstalled = $false
  if (Test-Path $packageJsonPath) {
    $packageJsonRaw = Get-Content $packageJsonPath -Raw
    if ($packageJsonRaw -match '"@angular/material"') {
      $materialAlreadyInstalled = $true
    }
  }

  if ($materialAlreadyInstalled) {
    Write-Info "Angular Material deja installe, etape ng add ignoree"
  } else {
    npx ng add @angular/material --defaults --skip-confirmation
    if ($LASTEXITCODE -ne 0) {
      throw "Echec d'installation d'Angular Material"
    }
    Write-Success "Angular Material installe"
  }
} else {
  Write-Info "Installation Material ignoree (option SkipMaterial)"
}

Write-Section "4) Generation de la structure modulaire"
# Core / Shared / Layouts / Pages
Invoke-NgGenerate -Arguments "m core"
Invoke-NgGenerate -Arguments "m shared"
Invoke-NgGenerate -Arguments "m layouts"
Invoke-NgGenerate -Arguments "m pages"

Invoke-NgGenerate -Arguments "c layouts/navbar --module layouts/layouts.module"
Invoke-NgGenerate -Arguments "c layouts/sidebar --module layouts/layouts.module"
Invoke-NgGenerate -Arguments "c layouts/footer --module layouts/layouts.module"

Invoke-NgGenerate -Arguments "c pages/home --module pages/pages.module"
Invoke-NgGenerate -Arguments "c pages/login --module pages/pages.module"
Invoke-NgGenerate -Arguments "c pages/dashboard --module pages/pages.module"

# Shared reusable components
Invoke-NgGenerate -Arguments "c shared/components/app-card --module shared/shared.module"
Invoke-NgGenerate -Arguments "c shared/components/app-button --module shared/shared.module"
Invoke-NgGenerate -Arguments "c shared/components/app-table --module shared/shared.module"

# Features
Invoke-NgGenerate -Arguments "m features/leasing --routing"
Invoke-NgGenerate -Arguments "m features/crowdfunding --routing"
Invoke-NgGenerate -Arguments "m features/loan --routing"
Invoke-NgGenerate -Arguments "m features/transaction --routing"
Invoke-NgGenerate -Arguments "m features/bank-account --routing"
Invoke-NgGenerate -Arguments "m features/virtual-card --routing"

# Optional shell components for team modules
Invoke-NgGenerate -Arguments "c features/leasing/pages/leasing-home --module features/leasing/leasing.module"
Invoke-NgGenerate -Arguments "c features/crowdfunding/pages/crowdfunding-home --module features/crowdfunding/crowdfunding.module"
Invoke-NgGenerate -Arguments "c features/loan/pages/loan-home --module features/loan/loan.module"

# Transaction module components + service
Invoke-NgGenerate -Arguments "c features/transaction/components/transaction-list --module features/transaction/transaction.module"
Invoke-NgGenerate -Arguments "c features/transaction/components/transaction-detail --module features/transaction/transaction.module"
Invoke-NgGenerate -Arguments "c features/transaction/components/transaction-create --module features/transaction/transaction.module"
Invoke-NgGenerate -Arguments "s features/transaction/services/transaction"

# Bank account module components + service
Invoke-NgGenerate -Arguments "c features/bank-account/components/bank-account-list --module features/bank-account/bank-account.module"
Invoke-NgGenerate -Arguments "c features/bank-account/components/bank-account-detail --module features/bank-account/bank-account.module"
Invoke-NgGenerate -Arguments "c features/bank-account/components/bank-account-create --module features/bank-account/bank-account.module"
Invoke-NgGenerate -Arguments "s features/bank-account/services/bank-account"

# Virtual card module components + service
Invoke-NgGenerate -Arguments "c features/virtual-card/components/virtual-card-list --module features/virtual-card/virtual-card.module"
Invoke-NgGenerate -Arguments "c features/virtual-card/components/virtual-card-detail --module features/virtual-card/virtual-card.module"
Invoke-NgGenerate -Arguments "c features/virtual-card/components/virtual-card-create --module features/virtual-card/virtual-card.module"
Invoke-NgGenerate -Arguments "s features/virtual-card/services/virtual-card"

Write-Success "Structure modulaire generee"

Write-Section "5) Preparation des dossiers metier"
$TransactionModelDir = "src/app/features/transaction/models"
Ensure-Directory -Path $TransactionModelDir
$BankAccountModelDir = "src/app/features/bank-account/models"
Ensure-Directory -Path $BankAccountModelDir
$VirtualCardModelDir = "src/app/features/virtual-card/models"
Ensure-Directory -Path $VirtualCardModelDir

Write-Section "6) Ecriture des fichiers de configuration et templates"

Write-File -Path "src/environments/environment.ts" -Content @"
export const environment = {
  production: false,
  apiBaseUrl: '$BackendUrl',
};
"@

Write-File -Path "src/environments/environment.prod.ts" -Content @"
export const environment = {
  production: true,
  apiBaseUrl: '$BackendUrl',
};
"@

Write-File -Path "src/app/app-routing.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./pages/pages.module').then((m) => m.PagesModule),
  },
  {
    path: 'transactions',
    loadChildren: () => import('./features/transaction/transaction.module').then((m) => m.TransactionModule),
  },
  {
    path: 'leasing',
    loadChildren: () => import('./features/leasing/leasing.module').then((m) => m.LeasingModule),
  },
  {
    path: 'crowdfunding',
    loadChildren: () => import('./features/crowdfunding/crowdfunding.module').then((m) => m.CrowdfundingModule),
  },
  {
    path: 'loan',
    loadChildren: () => import('./features/loan/loan.module').then((m) => m.LoanModule),
  },
  {
    path: 'bank-accounts',
    loadChildren: () => import('./features/bank-account/bank-account.module').then((m) => m.BankAccountModule),
  },
  {
    path: 'virtual-cards',
    loadChildren: () => import('./features/virtual-card/virtual-card.module').then((m) => m.VirtualCardModule),
  },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
'@

Write-File -Path "src/app/app.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LayoutsModule } from './layouts/layouts.module';

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    AppRoutingModule,
    LayoutsModule,
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {}
'@

Write-File -Path "src/app/app.component.html" -Content @'
<app-navbar></app-navbar>

<div class="app-shell">
  <aside class="app-sidebar">
    <app-sidebar></app-sidebar>
  </aside>

  <main class="app-content">
    <router-outlet></router-outlet>
  </main>
</div>

<app-footer></app-footer>
'@

Write-File -Path "src/app/app.component.scss" -Content @'
.app-shell {
  display: grid;
  grid-template-columns: 260px 1fr;
  min-height: calc(100vh - 128px);
}

.app-sidebar {
  border-right: 1px solid var(--border-color);
  background: var(--surface-soft);
}

.app-content {
  padding: 1rem;
}

@media (max-width: 992px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .app-sidebar {
    display: none;
  }
}
'@

Write-File -Path "src/app/shared/material.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@NgModule({
  exports: [
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule,
    MatDividerModule,
    MatProgressSpinnerModule,
  ],
})
export class MaterialModule {}
'@

Write-File -Path "src/app/shared/shared.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AppCardComponent } from './components/app-card/app-card.component';
import { AppButtonComponent } from './components/app-button/app-button.component';
import { AppTableComponent } from './components/app-table/app-table.component';
import { MaterialModule } from './material.module';

@NgModule({
  declarations: [AppCardComponent, AppButtonComponent, AppTableComponent],
  imports: [CommonModule, MaterialModule],
  exports: [CommonModule, MaterialModule, AppCardComponent, AppButtonComponent, AppTableComponent],
})
export class SharedModule {}
'@

Write-File -Path "src/app/layouts/layouts.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { NavbarComponent } from './navbar/navbar.component';
import { SidebarComponent } from './sidebar/sidebar.component';
import { FooterComponent } from './footer/footer.component';
import { SharedModule } from '../shared/shared.module';

@NgModule({
  declarations: [NavbarComponent, SidebarComponent, FooterComponent],
  imports: [CommonModule, RouterModule, SharedModule],
  exports: [NavbarComponent, SidebarComponent, FooterComponent],
})
export class LayoutsModule {}
'@

Write-File -Path "src/app/pages/pages-routing.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './login/login.component';
import { DashboardComponent } from './dashboard/dashboard.component';

const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class PagesRoutingModule {}
'@

Write-File -Path "src/app/pages/pages.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { PagesRoutingModule } from './pages-routing.module';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './login/login.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { SharedModule } from '../shared/shared.module';

@NgModule({
  declarations: [HomeComponent, LoginComponent, DashboardComponent],
  imports: [CommonModule, PagesRoutingModule, SharedModule],
})
export class PagesModule {}
'@

# Feature routes with shell screens
Write-File -Path "src/app/features/leasing/leasing-routing.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LeasingHomeComponent } from './pages/leasing-home/leasing-home.component';

const routes: Routes = [
  { path: '', component: LeasingHomeComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class LeasingRoutingModule {}
'@

Write-File -Path "src/app/features/leasing/leasing.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { LeasingRoutingModule } from './leasing-routing.module';
import { LeasingHomeComponent } from './pages/leasing-home/leasing-home.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [LeasingHomeComponent],
  imports: [CommonModule, LeasingRoutingModule, SharedModule],
})
export class LeasingModule {}
'@

Write-File -Path "src/app/features/crowdfunding/crowdfunding-routing.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CrowdfundingHomeComponent } from './pages/crowdfunding-home/crowdfunding-home.component';

const routes: Routes = [
  { path: '', component: CrowdfundingHomeComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class CrowdfundingRoutingModule {}
'@

Write-File -Path "src/app/features/crowdfunding/crowdfunding.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { CrowdfundingRoutingModule } from './crowdfunding-routing.module';
import { CrowdfundingHomeComponent } from './pages/crowdfunding-home/crowdfunding-home.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [CrowdfundingHomeComponent],
  imports: [CommonModule, CrowdfundingRoutingModule, SharedModule],
})
export class CrowdfundingModule {}
'@

Write-File -Path "src/app/features/loan/loan-routing.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoanHomeComponent } from './pages/loan-home/loan-home.component';

const routes: Routes = [
  { path: '', component: LoanHomeComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class LoanRoutingModule {}
'@

Write-File -Path "src/app/features/loan/loan.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { LoanRoutingModule } from './loan-routing.module';
import { LoanHomeComponent } from './pages/loan-home/loan-home.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [LoanHomeComponent],
  imports: [CommonModule, LoanRoutingModule, SharedModule],
})
export class LoanModule {}
'@

# Bank account model + service + routing + module
Write-File -Path "src/app/features/bank-account/models/bank-account.model.ts" -Content @'
export interface BankAccount {
  id: number;
  userId: number;
  rib: string;
  balance: number;
  currency: string;
  accountType: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface BankAccountCreateRequest {
  userId: number;
  rib: string;
  accountType: string;
  currency: string;
}
'@

Write-File -Path "src/app/features/bank-account/services/bank-account.service.ts" -Content @'
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { BankAccount, BankAccountCreateRequest } from '../models/bank-account.model';

@Injectable({ providedIn: 'root' })
export class BankAccountService {
  private readonly baseUrl = `${environment.apiBaseUrl}/accounts`;

  constructor(private readonly http: HttpClient) {}

  createAccount(data: BankAccountCreateRequest): Observable<BankAccount> {
    return this.http.post<BankAccount>(`${this.baseUrl}/add`, data);
  }

  getAccountById(id: number): Observable<BankAccount> {
    return this.http.get<BankAccount>(`${this.baseUrl}/get/${id}`);
  }

  getBalance(id: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/balance/${id}`);
  }

  getUserAccounts(userId: number): Observable<BankAccount[]> {
    return this.http.get<BankAccount[]>(`${this.baseUrl}/user/${userId}`);
  }

  creditAccount(id: number, amount: number): Observable<BankAccount> {
    const params = new HttpParams().set('amount', amount);
    return this.http.post<BankAccount>(`${this.baseUrl}/credit/${id}`, null, { params });
  }

  debitAccount(id: number, amount: number): Observable<BankAccount> {
    const params = new HttpParams().set('amount', amount);
    return this.http.post<BankAccount>(`${this.baseUrl}/debit/${id}`, null, { params });
  }

  freezeAccount(id: number): Observable<BankAccount> {
    return this.http.put<BankAccount>(`${this.baseUrl}/freeze/${id}`, null);
  }

  unfreezeAccount(id: number): Observable<BankAccount> {
    return this.http.put<BankAccount>(`${this.baseUrl}/unfreeze/${id}`, null);
  }

  updateStatus(id: number, status: string): Observable<BankAccount> {
    return this.http.put<BankAccount>(`${this.baseUrl}/update-status`, { id, status });
  }

  deleteAccount(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/delete/${id}`);
  }
}
'@

Write-File -Path "src/app/features/bank-account/bank-account-routing.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BankAccountListComponent } from './components/bank-account-list/bank-account-list.component';
import { BankAccountCreateComponent } from './components/bank-account-create/bank-account-create.component';
import { BankAccountDetailComponent } from './components/bank-account-detail/bank-account-detail.component';

const routes: Routes = [
  { path: '', component: BankAccountListComponent },
  { path: 'create', component: BankAccountCreateComponent },
  { path: ':id', component: BankAccountDetailComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BankAccountRoutingModule {}
'@

Write-File -Path "src/app/features/bank-account/bank-account.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { SharedModule } from '../../shared/shared.module';
import { BankAccountRoutingModule } from './bank-account-routing.module';
import { BankAccountListComponent } from './components/bank-account-list/bank-account-list.component';
import { BankAccountDetailComponent } from './components/bank-account-detail/bank-account-detail.component';
import { BankAccountCreateComponent } from './components/bank-account-create/bank-account-create.component';

@NgModule({
  declarations: [BankAccountListComponent, BankAccountDetailComponent, BankAccountCreateComponent],
  imports: [CommonModule, SharedModule, ReactiveFormsModule, BankAccountRoutingModule],
})
export class BankAccountModule {}
'@

Write-File -Path "src/app/features/bank-account/components/bank-account-list/bank-account-list.component.ts" -Content @'
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, Validators } from '@angular/forms';
import { BankAccountService } from '../../services/bank-account.service';
import { BankAccount } from '../../models/bank-account.model';

@Component({
  selector: 'app-bank-account-list',
  templateUrl: './bank-account-list.component.html',
  styleUrls: ['./bank-account-list.component.scss'],
})
export class BankAccountListComponent {
  accounts: BankAccount[] = [];
  displayedColumns = ['id', 'rib', 'balance', 'currency', 'status', 'actions'];
  loading = false;

  form: ReturnType<FormBuilder['group']>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly accountService: BankAccountService,
    private readonly router: Router
  ) {
    this.form = this.fb.group({
      userId: [null as number | null, [Validators.required]],
    });
  }

  loadAccounts(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.accountService.getUserAccounts(this.form.getRawValue().userId as number).subscribe({
      next: (data) => {
        this.accounts = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  details(id: number): void {
    this.router.navigate(['/bank-accounts', id]);
  }

  create(): void {
    this.router.navigate(['/bank-accounts/create']);
  }
}
'@

Write-File -Path "src/app/features/bank-account/components/bank-account-list/bank-account-list.component.html" -Content @'
<section class="transactions-page">
  <header class="transactions-header">
    <h1>Comptes Bancaires</h1>
    <button mat-raised-button color="primary" (click)="create()">Nouveau compte</button>
  </header>

  <mat-card>
    <form [formGroup]="form" (ngSubmit)="loadAccounts()" class="tx-form">
      <mat-form-field appearance="outline">
        <mat-label>User ID</mat-label>
        <input matInput type="number" formControlName="userId" />
      </mat-form-field>
      <button mat-raised-button color="primary" type="submit">Charger</button>
    </form>
  </mat-card>

  <mat-card>
    <table mat-table [dataSource]="accounts" class="transactions-table" *ngIf="!loading">
      <ng-container matColumnDef="id"><th mat-header-cell *matHeaderCellDef>ID</th><td mat-cell *matCellDef="let a">{{ a.id }}</td></ng-container>
      <ng-container matColumnDef="rib"><th mat-header-cell *matHeaderCellDef>RIB</th><td mat-cell *matCellDef="let a">{{ a.rib }}</td></ng-container>
      <ng-container matColumnDef="balance"><th mat-header-cell *matHeaderCellDef>Solde</th><td mat-cell *matCellDef="let a">{{ a.balance }}</td></ng-container>
      <ng-container matColumnDef="currency"><th mat-header-cell *matHeaderCellDef>Devise</th><td mat-cell *matCellDef="let a">{{ a.currency }}</td></ng-container>
      <ng-container matColumnDef="status"><th mat-header-cell *matHeaderCellDef>Status</th><td mat-cell *matCellDef="let a">{{ a.status }}</td></ng-container>
      <ng-container matColumnDef="actions"><th mat-header-cell *matHeaderCellDef>Actions</th><td mat-cell *matCellDef="let a"><button mat-button color="primary" (click)="details(a.id)">Details</button></td></ng-container>
      <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
      <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
    </table>
  </mat-card>
</section>
'@

Write-File -Path "src/app/features/bank-account/components/bank-account-detail/bank-account-detail.component.ts" -Content @'
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, Validators } from '@angular/forms';
import { BankAccountService } from '../../services/bank-account.service';
import { BankAccount } from '../../models/bank-account.model';

@Component({
  selector: 'app-bank-account-detail',
  templateUrl: './bank-account-detail.component.html',
  styleUrls: ['./bank-account-detail.component.scss'],
})
export class BankAccountDetailComponent implements OnInit {
  account?: BankAccount;
  balance?: number;
  amountForm: ReturnType<FormBuilder['group']>;
  statusForm: ReturnType<FormBuilder['group']>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly accountService: BankAccountService
  ) {
    this.amountForm = this.fb.group({
      amount: [0, [Validators.required, Validators.min(0.01)]],
    });
    this.statusForm = this.fb.group({
      status: ['ACTIVE', [Validators.required]],
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      return;
    }
    this.accountService.getAccountById(id).subscribe((data) => (this.account = data));
    this.accountService.getBalance(id).subscribe((value) => (this.balance = value));
  }

  private currentId(): number | null {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    return id || null;
  }

  refresh(): void {
    const id = this.currentId();
    if (!id) return;
    this.accountService.getAccountById(id).subscribe((data) => (this.account = data));
    this.accountService.getBalance(id).subscribe((value) => (this.balance = value));
  }

  credit(): void {
    const id = this.currentId();
    if (!id || this.amountForm.invalid) return;
    const amount = this.amountForm.getRawValue().amount as number;
    this.accountService.creditAccount(id, amount).subscribe(() => this.refresh());
  }

  debit(): void {
    const id = this.currentId();
    if (!id || this.amountForm.invalid) return;
    const amount = this.amountForm.getRawValue().amount as number;
    this.accountService.debitAccount(id, amount).subscribe(() => this.refresh());
  }

  freeze(): void {
    const id = this.currentId();
    if (!id) return;
    this.accountService.freezeAccount(id).subscribe(() => this.refresh());
  }

  unfreeze(): void {
    const id = this.currentId();
    if (!id) return;
    this.accountService.unfreezeAccount(id).subscribe(() => this.refresh());
  }

  updateStatus(): void {
    const id = this.currentId();
    if (!id || this.statusForm.invalid) return;
    const status = this.statusForm.getRawValue().status as string;
    this.accountService.updateStatus(id, status).subscribe(() => this.refresh());
  }

  delete(): void {
    const id = this.currentId();
    if (!id) return;
    this.accountService.deleteAccount(id).subscribe(() => {
      this.account = undefined;
      this.balance = undefined;
    });
  }
}
'@

Write-File -Path "src/app/features/bank-account/components/bank-account-detail/bank-account-detail.component.html" -Content @'
<mat-card *ngIf="account as acc">
  <h2>Compte #{{ acc.id }}</h2>
  <p><strong>RIB:</strong> {{ acc.rib }}</p>
  <p><strong>User:</strong> {{ acc.userId }}</p>
  <p><strong>Type:</strong> {{ acc.accountType }}</p>
  <p><strong>Status:</strong> {{ acc.status }}</p>
  <p><strong>Devise:</strong> {{ acc.currency }}</p>
  <p><strong>Solde:</strong> {{ balance ?? acc.balance }}</p>

  <div class="actions-cell">
    <form [formGroup]="amountForm" class="tx-form">
      <mat-form-field appearance="outline">
        <mat-label>Amount</mat-label>
        <input matInput type="number" formControlName="amount" />
      </mat-form-field>
      <div class="actions-cell">
        <button mat-stroked-button color="primary" type="button" (click)="credit()">Credit</button>
        <button mat-stroked-button color="accent" type="button" (click)="debit()">Debit</button>
      </div>
    </form>
  </div>

  <form [formGroup]="statusForm" class="tx-form">
    <mat-form-field appearance="outline">
      <mat-label>Status</mat-label>
      <input matInput formControlName="status" />
    </mat-form-field>
    <div class="actions-cell">
      <button mat-stroked-button type="button" (click)="updateStatus()">Update status</button>
      <button mat-stroked-button type="button" (click)="freeze()">Freeze</button>
      <button mat-stroked-button type="button" (click)="unfreeze()">Unfreeze</button>
      <button mat-stroked-button color="warn" type="button" (click)="delete()">Delete</button>
    </div>
  </form>
</mat-card>
'@

Write-File -Path "src/app/features/bank-account/components/bank-account-create/bank-account-create.component.ts" -Content @'
import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { BankAccountService } from '../../services/bank-account.service';

@Component({
  selector: 'app-bank-account-create',
  templateUrl: './bank-account-create.component.html',
  styleUrls: ['./bank-account-create.component.scss'],
})
export class BankAccountCreateComponent {
  form: ReturnType<FormBuilder['group']>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly accountService: BankAccountService,
    private readonly router: Router
  ) {
    this.form = this.fb.group({
      userId: [null as number | null, [Validators.required]],
      rib: ['', [Validators.required]],
      accountType: ['CHECKING', [Validators.required]],
      currency: ['TND', [Validators.required]],
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload = this.form.getRawValue();
    this.accountService.createAccount({
      userId: payload.userId as number,
      rib: payload.rib as string,
      accountType: payload.accountType as string,
      currency: payload.currency as string,
    }).subscribe((created) => this.router.navigate(['/bank-accounts', created.id]));
  }
}
'@

Write-File -Path "src/app/features/bank-account/components/bank-account-create/bank-account-create.component.html" -Content @'
<mat-card>
  <h2>Creer un compte</h2>
  <form [formGroup]="form" (ngSubmit)="submit()" class="tx-form">
    <mat-form-field appearance="outline"><mat-label>User ID</mat-label><input matInput type="number" formControlName="userId" /></mat-form-field>
    <mat-form-field appearance="outline"><mat-label>RIB</mat-label><input matInput formControlName="rib" /></mat-form-field>
    <mat-form-field appearance="outline"><mat-label>Type</mat-label><input matInput formControlName="accountType" /></mat-form-field>
    <mat-form-field appearance="outline"><mat-label>Devise</mat-label><input matInput formControlName="currency" /></mat-form-field>
    <button mat-raised-button color="primary" type="submit">Enregistrer</button>
  </form>
</mat-card>
'@

# Virtual card model + service + routing + module
Write-File -Path "src/app/features/virtual-card/models/virtual-card.model.ts" -Content @'
export interface VirtualCard {
  id: number;
  bankAccountId: number;
  cardNumber: string;
  expiryDate: string;
  paymentLimit: number;
  monthlySpent: number;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}
'@

Write-File -Path "src/app/features/virtual-card/services/virtual-card.service.ts" -Content @'
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { VirtualCard } from '../models/virtual-card.model';

@Injectable({ providedIn: 'root' })
export class VirtualCardService {
  private readonly baseUrl = `${environment.apiBaseUrl}/cards`;

  constructor(private readonly http: HttpClient) {}

  createCard(data: { bankAccountId: number; expiryDate: string; cvvHash: string; paymentLimit: number }): Observable<VirtualCard> {
    const params = new HttpParams()
      .set('bankAccountId', data.bankAccountId)
      .set('expiryDate', data.expiryDate)
      .set('cvvHash', data.cvvHash)
      .set('paymentLimit', data.paymentLimit);
    return this.http.post<VirtualCard>(`${this.baseUrl}/add`, null, { params });
  }

  getCardById(id: number): Observable<VirtualCard> {
    return this.http.get<VirtualCard>(`${this.baseUrl}/get/${id}`);
  }

  getCardByNumber(cardNumber: string): Observable<VirtualCard> {
    return this.http.get<VirtualCard>(`${this.baseUrl}/get-by-number/${cardNumber}`);
  }

  getCardsByBankAccount(bankAccountId: number): Observable<VirtualCard[]> {
    return this.http.get<VirtualCard[]>(`${this.baseUrl}/account/${bankAccountId}`);
  }

  getActiveCardsByBankAccount(bankAccountId: number): Observable<VirtualCard[]> {
    return this.http.get<VirtualCard[]>(`${this.baseUrl}/account/${bankAccountId}/active`);
  }

  blockCard(id: number): Observable<VirtualCard> {
    return this.http.put<VirtualCard>(`${this.baseUrl}/block/${id}`, null);
  }

  unblockCard(id: number): Observable<VirtualCard> {
    return this.http.put<VirtualCard>(`${this.baseUrl}/unblock/${id}`, null);
  }

  updatePaymentLimit(id: number, newLimit: number): Observable<VirtualCard> {
    const params = new HttpParams().set('newLimit', newLimit);
    return this.http.put<VirtualCard>(`${this.baseUrl}/update-limit/${id}`, null, { params });
  }

  updateCardStatus(id: number, newStatus: string): Observable<VirtualCard> {
    const params = new HttpParams().set('newStatus', newStatus);
    return this.http.put<VirtualCard>(`${this.baseUrl}/update-status/${id}`, null, { params });
  }

  resetMonthlySpent(id: number): Observable<VirtualCard> {
    return this.http.put<VirtualCard>(`${this.baseUrl}/reset-monthly-spent/${id}`, null);
  }

  addMonthlySpending(id: number, amount: number): Observable<VirtualCard> {
    const params = new HttpParams().set('amount', amount);
    return this.http.put<VirtualCard>(`${this.baseUrl}/add-monthly-spending/${id}`, null, { params });
  }

  isMonthlyLimitReached(id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/is-limit-reached/${id}`);
  }

  cardNumberExists(cardNumber: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/${cardNumber}`);
  }

  countCardsByBankAccount(bankAccountId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/account/${bankAccountId}/count`);
  }

  deleteCard(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/delete/${id}`);
  }
}
'@

Write-File -Path "src/app/features/virtual-card/virtual-card-routing.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { VirtualCardListComponent } from './components/virtual-card-list/virtual-card-list.component';
import { VirtualCardCreateComponent } from './components/virtual-card-create/virtual-card-create.component';
import { VirtualCardDetailComponent } from './components/virtual-card-detail/virtual-card-detail.component';

const routes: Routes = [
  { path: '', component: VirtualCardListComponent },
  { path: 'create', component: VirtualCardCreateComponent },
  { path: ':id', component: VirtualCardDetailComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class VirtualCardRoutingModule {}
'@

Write-File -Path "src/app/features/virtual-card/virtual-card.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { SharedModule } from '../../shared/shared.module';
import { VirtualCardRoutingModule } from './virtual-card-routing.module';
import { VirtualCardListComponent } from './components/virtual-card-list/virtual-card-list.component';
import { VirtualCardDetailComponent } from './components/virtual-card-detail/virtual-card-detail.component';
import { VirtualCardCreateComponent } from './components/virtual-card-create/virtual-card-create.component';

@NgModule({
  declarations: [VirtualCardListComponent, VirtualCardDetailComponent, VirtualCardCreateComponent],
  imports: [CommonModule, SharedModule, ReactiveFormsModule, VirtualCardRoutingModule],
})
export class VirtualCardModule {}
'@

Write-File -Path "src/app/features/virtual-card/components/virtual-card-list/virtual-card-list.component.ts" -Content @'
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, Validators } from '@angular/forms';
import { VirtualCardService } from '../../services/virtual-card.service';
import { VirtualCard } from '../../models/virtual-card.model';

@Component({
  selector: 'app-virtual-card-list',
  templateUrl: './virtual-card-list.component.html',
  styleUrls: ['./virtual-card-list.component.scss'],
})
export class VirtualCardListComponent {
  cards: VirtualCard[] = [];
  activeCards: VirtualCard[] = [];
  cardByNumber?: VirtualCard;
  existsValue?: boolean;
  countValue?: number;
  displayedColumns = ['id', 'cardNumber', 'paymentLimit', 'monthlySpent', 'status', 'actions'];

  form: ReturnType<FormBuilder['group']>;
  cardNumberForm: ReturnType<FormBuilder['group']>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly cardService: VirtualCardService,
    private readonly router: Router
  ) {
    this.form = this.fb.group({
      bankAccountId: [null as number | null, [Validators.required]],
    });
    this.cardNumberForm = this.fb.group({
      cardNumber: ['', [Validators.required]],
    });
  }

  loadCards(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.cardService.getCardsByBankAccount(this.form.getRawValue().bankAccountId as number).subscribe((data) => {
      this.cards = data;
    });

    this.cardService.getActiveCardsByBankAccount(this.form.getRawValue().bankAccountId as number).subscribe((data) => {
      this.activeCards = data;
    });

    this.cardService.countCardsByBankAccount(this.form.getRawValue().bankAccountId as number).subscribe((value) => {
      this.countValue = value;
    });
  }

  searchByNumber(): void {
    if (this.cardNumberForm.invalid) {
      this.cardNumberForm.markAllAsTouched();
      return;
    }
    const cardNumber = this.cardNumberForm.getRawValue().cardNumber as string;
    this.cardService.getCardByNumber(cardNumber).subscribe((data) => {
      this.cardByNumber = data;
    });
    this.cardService.cardNumberExists(cardNumber).subscribe((value) => {
      this.existsValue = value;
    });
  }

  details(id: number): void {
    this.router.navigate(['/virtual-cards', id]);
  }

  create(): void {
    this.router.navigate(['/virtual-cards/create']);
  }
}
'@

Write-File -Path "src/app/features/virtual-card/components/virtual-card-list/virtual-card-list.component.html" -Content @'
<section class="transactions-page">
  <header class="transactions-header">
    <h1>Cartes Virtuelles</h1>
    <button mat-raised-button color="primary" (click)="create()">Nouvelle carte</button>
  </header>

  <mat-card>
    <form [formGroup]="form" (ngSubmit)="loadCards()" class="tx-form">
      <mat-form-field appearance="outline">
        <mat-label>Bank Account ID</mat-label>
        <input matInput type="number" formControlName="bankAccountId" />
      </mat-form-field>
      <button mat-raised-button color="primary" type="submit">Charger</button>
    </form>
    <p *ngIf="countValue != null"><strong>Nombre de cartes:</strong> {{ countValue }}</p>
  </mat-card>

  <mat-card>
    <form [formGroup]="cardNumberForm" (ngSubmit)="searchByNumber()" class="tx-form">
      <mat-form-field appearance="outline">
        <mat-label>Numero de carte</mat-label>
        <input matInput formControlName="cardNumber" />
      </mat-form-field>
      <button mat-raised-button color="primary" type="submit">Rechercher</button>
    </form>
    <p *ngIf="existsValue != null"><strong>Existe:</strong> {{ existsValue }}</p>
    <pre *ngIf="cardByNumber">{{ cardByNumber | json }}</pre>
  </mat-card>

  <mat-card>
    <h3>Cartes actives</h3>
    <pre>{{ activeCards | json }}</pre>
  </mat-card>

  <mat-card>
    <h3>Toutes les cartes</h3>
    <table mat-table [dataSource]="cards" class="transactions-table">
      <ng-container matColumnDef="id"><th mat-header-cell *matHeaderCellDef>ID</th><td mat-cell *matCellDef="let c">{{ c.id }}</td></ng-container>
      <ng-container matColumnDef="cardNumber"><th mat-header-cell *matHeaderCellDef>Numero</th><td mat-cell *matCellDef="let c">{{ c.cardNumber }}</td></ng-container>
      <ng-container matColumnDef="paymentLimit"><th mat-header-cell *matHeaderCellDef>Limit</th><td mat-cell *matCellDef="let c">{{ c.paymentLimit }}</td></ng-container>
      <ng-container matColumnDef="monthlySpent"><th mat-header-cell *matHeaderCellDef>Spent</th><td mat-cell *matCellDef="let c">{{ c.monthlySpent }}</td></ng-container>
      <ng-container matColumnDef="status"><th mat-header-cell *matHeaderCellDef>Status</th><td mat-cell *matCellDef="let c">{{ c.status }}</td></ng-container>
      <ng-container matColumnDef="actions"><th mat-header-cell *matHeaderCellDef>Actions</th><td mat-cell *matCellDef="let c"><button mat-button color="primary" (click)="details(c.id)">Details</button></td></ng-container>
      <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
      <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
    </table>
  </mat-card>
</section>
'@

Write-File -Path "src/app/features/virtual-card/components/virtual-card-detail/virtual-card-detail.component.ts" -Content @'
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, Validators } from '@angular/forms';
import { VirtualCardService } from '../../services/virtual-card.service';
import { VirtualCard } from '../../models/virtual-card.model';

@Component({
  selector: 'app-virtual-card-detail',
  templateUrl: './virtual-card-detail.component.html',
  styleUrls: ['./virtual-card-detail.component.scss'],
})
export class VirtualCardDetailComponent implements OnInit {
  card?: VirtualCard;
  limitReached?: boolean;
  limitForm: ReturnType<FormBuilder['group']>;
  spendForm: ReturnType<FormBuilder['group']>;
  statusForm: ReturnType<FormBuilder['group']>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly cardService: VirtualCardService
  ) {
    this.limitForm = this.fb.group({
      newLimit: [0, [Validators.required, Validators.min(1)]],
    });
    this.spendForm = this.fb.group({
      amount: [0, [Validators.required, Validators.min(0.01)]],
    });
    this.statusForm = this.fb.group({
      newStatus: ['ACTIVE', [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.refresh();
  }

  private currentId(): number | null {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    return id || null;
  }

  refresh(): void {
    const id = this.currentId();
    if (!id) return;
    this.cardService.getCardById(id).subscribe((data) => (this.card = data));
    this.cardService.isMonthlyLimitReached(id).subscribe((value) => (this.limitReached = value));
  }

  block(): void {
    const id = this.currentId();
    if (!id) return;
    this.cardService.blockCard(id).subscribe(() => this.refresh());
  }

  unblock(): void {
    const id = this.currentId();
    if (!id) return;
    this.cardService.unblockCard(id).subscribe(() => this.refresh());
  }

  updateStatus(): void {
    const id = this.currentId();
    if (!id || this.statusForm.invalid) return;
    const newStatus = this.statusForm.getRawValue().newStatus as string;
    this.cardService.updateCardStatus(id, newStatus).subscribe(() => this.refresh());
  }

  updateLimit(): void {
    const id = this.currentId();
    if (!id || this.limitForm.invalid) return;
    const newLimit = this.limitForm.getRawValue().newLimit as number;
    this.cardService.updatePaymentLimit(id, newLimit).subscribe(() => this.refresh());
  }

  addSpending(): void {
    const id = this.currentId();
    if (!id || this.spendForm.invalid) return;
    const amount = this.spendForm.getRawValue().amount as number;
    this.cardService.addMonthlySpending(id, amount).subscribe(() => this.refresh());
  }

  resetSpent(): void {
    const id = this.currentId();
    if (!id) return;
    this.cardService.resetMonthlySpent(id).subscribe(() => this.refresh());
  }

  delete(): void {
    const id = this.currentId();
    if (!id) return;
    this.cardService.deleteCard(id).subscribe(() => {
      this.card = undefined;
      this.limitReached = undefined;
    });
  }
}
'@

Write-File -Path "src/app/features/virtual-card/components/virtual-card-detail/virtual-card-detail.component.html" -Content @'
<mat-card *ngIf="card as c">
  <h2>Carte #{{ c.id }}</h2>
  <p><strong>Numero:</strong> {{ c.cardNumber }}</p>
  <p><strong>Compte:</strong> {{ c.bankAccountId }}</p>
  <p><strong>Expiration:</strong> {{ c.expiryDate }}</p>
  <p><strong>Limit:</strong> {{ c.paymentLimit }}</p>
  <p><strong>Monthly spent:</strong> {{ c.monthlySpent }}</p>
  <p><strong>Status:</strong> {{ c.status }}</p>
  <p><strong>Limit reached:</strong> {{ limitReached }}</p>

  <div class="actions-cell">
    <button mat-stroked-button color="warn" type="button" (click)="block()">Block</button>
    <button mat-stroked-button type="button" (click)="unblock()">Unblock</button>
    <button mat-stroked-button color="warn" type="button" (click)="delete()">Delete</button>
  </div>

  <form [formGroup]="statusForm" class="tx-form">
    <mat-form-field appearance="outline">
      <mat-label>New status</mat-label>
      <input matInput formControlName="newStatus" />
    </mat-form-field>
    <button mat-stroked-button type="button" (click)="updateStatus()">Update status</button>
  </form>

  <form [formGroup]="limitForm" class="tx-form">
    <mat-form-field appearance="outline">
      <mat-label>New limit</mat-label>
      <input matInput type="number" formControlName="newLimit" />
    </mat-form-field>
    <button mat-stroked-button type="button" (click)="updateLimit()">Update limit</button>
  </form>

  <form [formGroup]="spendForm" class="tx-form">
    <mat-form-field appearance="outline">
      <mat-label>Add monthly spending</mat-label>
      <input matInput type="number" formControlName="amount" />
    </mat-form-field>
    <button mat-stroked-button type="button" (click)="addSpending()">Add spending</button>
    <button mat-stroked-button type="button" (click)="resetSpent()">Reset spent</button>
  </form>
</mat-card>
'@

Write-File -Path "src/app/features/virtual-card/components/virtual-card-create/virtual-card-create.component.ts" -Content @'
import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { VirtualCardService } from '../../services/virtual-card.service';

@Component({
  selector: 'app-virtual-card-create',
  templateUrl: './virtual-card-create.component.html',
  styleUrls: ['./virtual-card-create.component.scss'],
})
export class VirtualCardCreateComponent {
  form: ReturnType<FormBuilder['group']>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly cardService: VirtualCardService,
    private readonly router: Router
  ) {
    this.form = this.fb.group({
      bankAccountId: [null as number | null, [Validators.required]],
      expiryDate: ['', [Validators.required]],
      cvvHash: ['', [Validators.required]],
      paymentLimit: [0, [Validators.required, Validators.min(1)]],
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = this.form.getRawValue();
    this.cardService.createCard({
      bankAccountId: payload.bankAccountId as number,
      expiryDate: payload.expiryDate as string,
      cvvHash: payload.cvvHash as string,
      paymentLimit: payload.paymentLimit as number,
    }).subscribe((created) => this.router.navigate(['/virtual-cards', created.id]));
  }
}
'@

Write-File -Path "src/app/features/virtual-card/components/virtual-card-create/virtual-card-create.component.html" -Content @'
<mat-card>
  <h2>Creer une carte virtuelle</h2>
  <form [formGroup]="form" (ngSubmit)="submit()" class="tx-form">
    <mat-form-field appearance="outline"><mat-label>Bank Account ID</mat-label><input matInput type="number" formControlName="bankAccountId" /></mat-form-field>
    <mat-form-field appearance="outline"><mat-label>Expiry Date</mat-label><input matInput formControlName="expiryDate" /></mat-form-field>
    <mat-form-field appearance="outline"><mat-label>CVV Hash</mat-label><input matInput formControlName="cvvHash" /></mat-form-field>
    <mat-form-field appearance="outline"><mat-label>Payment Limit</mat-label><input matInput type="number" formControlName="paymentLimit" /></mat-form-field>
    <button mat-raised-button color="primary" type="submit">Enregistrer</button>
  </form>
</mat-card>
'@

# Transaction model + service + routing + module
Write-File -Path "src/app/features/transaction/models/transaction.model.ts" -Content @'
export interface Transaction {
  id: number;
  bankAccountId: number;
  beneficiaryName?: string;
  beneficiaryRib?: string;
  amount: number;
  type?: string;
  category?: string;
  description?: string;
  status?: string;
  periodicity?: string;
  scheduledDate?: string;
  nextExecutionDate?: string;
  lastExecutionDate?: string;
  riskScore?: number;
  createdAt?: string;
  confirmedAt?: string;
}

export interface CreateTransactionRequest {
  beneficiaryName?: string;
  beneficiaryRib?: string;
  amount: number;
  type: string;
  category?: string;
  description?: string;
  periodicity?: string;
  scheduledDate?: string;
  nextExecutionDate?: string;
}

export interface TransactionStatistics {
  totalSpentThisMonth: number;
  spendingByCategory: Record<string, number>;
  topBeneficiaries: Array<{
    beneficiaryName: string;
    beneficiaryRib: string;
    transactionCount: number;
    totalAmount: number;
  }>;
  transactionCountByStatus: Record<string, number>;
}

export interface Recommendation {
  type: string;
  priority: string;
  title: string;
  message: string;
  action: string;
  value: string;
}
'@

Write-File -Path "src/app/features/transaction/services/transaction.service.ts" -Content @'
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  CreateTransactionRequest,
  Recommendation,
  Transaction,
  TransactionStatistics,
} from '../models/transaction.model';

@Injectable({
  providedIn: 'root',
})
export class TransactionService {
  private readonly baseUrl = `${environment.apiBaseUrl}/transactions`;

  constructor(private readonly http: HttpClient) {}

  createTransaction(accountId: number, data: CreateTransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.baseUrl}/add/${accountId}`, data);
  }

  getMonthlySummary(accountId: number): Observable<string> {
    return this.http.get(`${this.baseUrl}/${accountId}/monthly-summary`, { responseType: 'text' });
  }

  getMonthlyStatistics(accountId: number): Observable<TransactionStatistics> {
    return this.http.get<TransactionStatistics>(`${this.baseUrl}/${accountId}/statistics/monthly`);
  }

  getRecommendations(accountId: number): Observable<Recommendation[]> {
    return this.http.get<Recommendation[]>(`${this.baseUrl}/${accountId}/recommendations`);
  }

  searchTransactions(accountId: number, filters: {
    type?: string;
    minAmount?: number;
    maxAmount?: number;
    status?: string;
  }): Observable<Transaction[]> {
    let params = new HttpParams();
    if (filters.type) params = params.set('type', filters.type);
    if (filters.minAmount != null) params = params.set('minAmount', filters.minAmount);
    if (filters.maxAmount != null) params = params.set('maxAmount', filters.maxAmount);
    if (filters.status) params = params.set('status', filters.status);
    return this.http.get<Transaction[]>(`${this.baseUrl}/${accountId}/search`, { params });
  }

  getPendingTransactions(accountId: number): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.baseUrl}/${accountId}/pending`);
  }

  getSuspiciousTransactions(accountId: number): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.baseUrl}/${accountId}/suspicious`);
  }

  getConfirmedTransactions(accountId: number): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.baseUrl}/${accountId}/confirmed`);
  }

  getCanceledTransactions(accountId: number): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.baseUrl}/${accountId}/canceled`);
  }

  confirmTransaction(transactionId: number): Observable<Transaction> {
    return this.http.put<Transaction>(`${this.baseUrl}/${transactionId}/confirm`, null);
  }

  rejectTransaction(transactionId: number): Observable<Transaction> {
    return this.http.put<Transaction>(`${this.baseUrl}/${transactionId}/reject`, null);
  }

  approveTransaction(transactionId: number): Observable<Transaction> {
    return this.http.put<Transaction>(`${this.baseUrl}/${transactionId}/approve`, null);
  }
}
'@

Write-File -Path "src/app/features/transaction/transaction-routing.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TransactionListComponent } from './components/transaction-list/transaction-list.component';
import { TransactionDetailComponent } from './components/transaction-detail/transaction-detail.component';
import { TransactionCreateComponent } from './components/transaction-create/transaction-create.component';

const routes: Routes = [
  { path: '', component: TransactionListComponent },
  { path: 'create', component: TransactionCreateComponent },
  { path: ':id', component: TransactionDetailComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class TransactionRoutingModule {}
'@

Write-File -Path "src/app/features/transaction/transaction.module.ts" -Content @'
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { TransactionRoutingModule } from './transaction-routing.module';
import { TransactionListComponent } from './components/transaction-list/transaction-list.component';
import { TransactionDetailComponent } from './components/transaction-detail/transaction-detail.component';
import { TransactionCreateComponent } from './components/transaction-create/transaction-create.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [
    TransactionListComponent,
    TransactionDetailComponent,
    TransactionCreateComponent,
  ],
  imports: [SharedModule, ReactiveFormsModule, TransactionRoutingModule],
})
export class TransactionModule {}
'@

Write-File -Path "src/app/features/transaction/components/transaction-list/transaction-list.component.ts" -Content @'
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, Validators } from '@angular/forms';
import { TransactionService } from '../../services/transaction.service';
import { Transaction } from '../../models/transaction.model';

@Component({
  selector: 'app-transaction-list',
  templateUrl: './transaction-list.component.html',
  styleUrls: ['./transaction-list.component.scss'],
})
export class TransactionListComponent implements OnInit {
  transactions: Transaction[] = [];
  displayedColumns = ['id', 'beneficiaryName', 'type', 'amount', 'status', 'createdAt', 'actions'];
  loading = false;
  form: ReturnType<FormBuilder['group']>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly transactionService: TransactionService,
    private readonly router: Router
  ) {
    this.form = this.fb.group({
      accountId: [null as number | null, [Validators.required]],
      status: ['PENDING'],
      type: [''],
      minAmount: [null as number | null],
      maxAmount: [null as number | null],
    });
  }

  ngOnInit(): void {
    // Le chargement est manuel apres saisie de accountId pour coller aux endpoints backend.
  }

  loadTransactions(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const accountId = this.form.getRawValue().accountId as number;
    const status = this.form.getRawValue().status as string;
    this.loading = true;

    const request = status === 'SEARCH'
      ? this.transactionService.searchTransactions(accountId, {
          type: this.form.getRawValue().type || undefined,
          minAmount: this.form.getRawValue().minAmount ?? undefined,
          maxAmount: this.form.getRawValue().maxAmount ?? undefined,
          status: undefined,
        })
      : status === 'PENDING'
      ? this.transactionService.getPendingTransactions(accountId)
      : status === 'SUSPICIOUS'
      ? this.transactionService.getSuspiciousTransactions(accountId)
      : status === 'CONFIRMED'
      ? this.transactionService.getConfirmedTransactions(accountId)
      : this.transactionService.getCanceledTransactions(accountId);

    request.subscribe({
      next: (data) => {
        this.transactions = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  viewDetails(): void {
    const accountId = this.form.getRawValue().accountId as number;
    if (!accountId) {
      return;
    }
    this.router.navigate(['/transactions', accountId]);
  }

  goToCreate(): void {
    this.router.navigate(['/transactions/create']);
  }

  approve(id: number): void {
    this.transactionService.approveTransaction(id).subscribe(() => this.loadTransactions());
  }

  confirm(id: number): void {
    this.transactionService.confirmTransaction(id).subscribe(() => this.loadTransactions());
  }

  reject(id: number): void {
    this.transactionService.rejectTransaction(id).subscribe(() => this.loadTransactions());
  }
}
'@

Write-File -Path "src/app/features/transaction/components/transaction-list/transaction-list.component.html" -Content @'
<section class="transactions-page">
  <header class="transactions-header">
    <h1>Transactions</h1>
    <button mat-raised-button color="primary" (click)="goToCreate()">
      Nouvelle transaction
    </button>
  </header>

  <mat-card>
    <form [formGroup]="form" (ngSubmit)="loadTransactions()" class="tx-form">
      <mat-form-field appearance="outline">
        <mat-label>Account ID</mat-label>
        <input matInput type="number" formControlName="accountId" />
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Vue</mat-label>
        <mat-select formControlName="status">
          <mat-option value="PENDING">PENDING</mat-option>
          <mat-option value="SUSPICIOUS">SUSPICIOUS</mat-option>
          <mat-option value="CONFIRMED">CONFIRMED</mat-option>
          <mat-option value="CANCELED">CANCELED</mat-option>
          <mat-option value="SEARCH">SEARCH</mat-option>
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Type (search)</mat-label>
        <input matInput formControlName="type" />
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Min amount</mat-label>
        <input matInput type="number" formControlName="minAmount" />
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Max amount</mat-label>
        <input matInput type="number" formControlName="maxAmount" />
      </mat-form-field>

      <div class="actions-cell">
        <button mat-raised-button color="primary" type="submit">Charger</button>
        <button mat-stroked-button type="button" (click)="viewDetails()">Stats & recommandations</button>
      </div>
    </form>
  </mat-card>

  <mat-card>
    <div *ngIf="loading" class="loading-block">
      <mat-spinner diameter="36"></mat-spinner>
    </div>

    <table mat-table [dataSource]="transactions" class="transactions-table" *ngIf="!loading">
      <ng-container matColumnDef="id">
        <th mat-header-cell *matHeaderCellDef>ID</th>
        <td mat-cell *matCellDef="let tx">{{ tx.id }}</td>
      </ng-container>

      <ng-container matColumnDef="beneficiaryName">
        <th mat-header-cell *matHeaderCellDef>Beneficiaire</th>
        <td mat-cell *matCellDef="let tx">{{ tx.beneficiaryName || '-' }}</td>
      </ng-container>

      <ng-container matColumnDef="type">
        <th mat-header-cell *matHeaderCellDef>Type</th>
        <td mat-cell *matCellDef="let tx">{{ tx.type }}</td>
      </ng-container>

      <ng-container matColumnDef="amount">
        <th mat-header-cell *matHeaderCellDef>Montant</th>
        <td mat-cell *matCellDef="let tx">{{ tx.amount | number: '1.2-2' }}</td>
      </ng-container>

      <ng-container matColumnDef="status">
        <th mat-header-cell *matHeaderCellDef>Statut</th>
        <td mat-cell *matCellDef="let tx">{{ tx.status }}</td>
      </ng-container>

      <ng-container matColumnDef="createdAt">
        <th mat-header-cell *matHeaderCellDef>Date</th>
        <td mat-cell *matCellDef="let tx">{{ tx.createdAt | date: 'short' }}</td>
      </ng-container>

      <ng-container matColumnDef="actions">
        <th mat-header-cell *matHeaderCellDef>Actions</th>
        <td mat-cell *matCellDef="let tx" class="actions-cell">
          <button mat-button color="primary" (click)="approve(tx.id)">Approve</button>
          <button mat-button color="accent" (click)="confirm(tx.id)">Confirm</button>
          <button mat-button color="warn" (click)="reject(tx.id)">Reject</button>
        </td>
      </ng-container>

      <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
      <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
    </table>
  </mat-card>
</section>
'@

Write-File -Path "src/app/features/transaction/components/transaction-list/transaction-list.component.scss" -Content @'
.transactions-page {
  display: grid;
  gap: 1rem;
}

.transactions-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.transactions-table {
  width: 100%;
}

.actions-cell {
  display: flex;
  gap: 0.5rem;
}

.loading-block {
  display: grid;
  place-items: center;
  padding: 2rem;
}
'@

Write-File -Path "src/app/features/transaction/components/transaction-detail/transaction-detail.component.ts" -Content @'
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TransactionService } from '../../services/transaction.service';
import { Recommendation, TransactionStatistics } from '../../models/transaction.model';

@Component({
  selector: 'app-transaction-detail',
  templateUrl: './transaction-detail.component.html',
  styleUrls: ['./transaction-detail.component.scss'],
})
export class TransactionDetailComponent implements OnInit {
  summary = '';
  stats?: TransactionStatistics;
  recommendations: Recommendation[] = [];

  constructor(
    private readonly route: ActivatedRoute,
    private readonly transactionService: TransactionService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.transactionService.getMonthlySummary(id).subscribe((txt) => (this.summary = txt));
      this.transactionService.getMonthlyStatistics(id).subscribe((data) => (this.stats = data));
      this.transactionService.getRecommendations(id).subscribe((data) => (this.recommendations = data));
    }
  }
}
'@

Write-File -Path "src/app/features/transaction/components/transaction-detail/transaction-detail.component.html" -Content @'
<mat-card>
  <h2>Resume mensuel</h2>
  <p>{{ summary || 'Chargement...' }}</p>
</mat-card>

<mat-card *ngIf="stats as s">
  <h3>Statistiques</h3>
  <p><strong>Total depense ce mois:</strong> {{ s.totalSpentThisMonth }}</p>
  <p><strong>Categories:</strong></p>
  <pre>{{ s.spendingByCategory | json }}</pre>
  <p><strong>Top beneficiaires:</strong></p>
  <pre>{{ s.topBeneficiaries | json }}</pre>
  <p><strong>Comptage par status:</strong></p>
  <pre>{{ s.transactionCountByStatus | json }}</pre>
</mat-card>

<mat-card>
  <h3>Recommandations</h3>
  <div *ngFor="let r of recommendations" class="recommendation-item">
    <p><strong>{{ r.title }}</strong> ({{ r.priority }})</p>
    <p>{{ r.message }}</p>
    <p><em>Action:</em> {{ r.action }}</p>
  </div>
</mat-card>
'@

Write-File -Path "src/app/features/transaction/components/transaction-create/transaction-create.component.ts" -Content @'
import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TransactionService } from '../../services/transaction.service';
import { CreateTransactionRequest } from '../../models/transaction.model';

@Component({
  selector: 'app-transaction-create',
  templateUrl: './transaction-create.component.html',
  styleUrls: ['./transaction-create.component.scss'],
})
export class TransactionCreateComponent {
  submitting = false;

  form: ReturnType<FormBuilder['group']>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly transactionService: TransactionService,
    private readonly router: Router
  ) {
    this.form = this.fb.group({
      accountId: [null as number | null, [Validators.required]],
      beneficiaryName: [''],
      beneficiaryRib: [''],
      amount: [0, [Validators.required, Validators.min(0.01)]],
      type: ['TRANSFER', [Validators.required]],
      category: [''],
      description: [''],
      periodicity: ['NOW'],
      scheduledDate: [''],
      nextExecutionDate: [''],
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const accountId = raw.accountId as number;
    const payload: CreateTransactionRequest = {
      beneficiaryName: raw.beneficiaryName ?? undefined,
      beneficiaryRib: raw.beneficiaryRib ?? undefined,
      amount: raw.amount ?? 0,
      type: (raw.type ?? 'TRANSFER') as string,
      category: raw.category ?? undefined,
      description: raw.description ?? undefined,
      periodicity: raw.periodicity ?? undefined,
      scheduledDate: raw.scheduledDate ?? undefined,
      nextExecutionDate: raw.nextExecutionDate ?? undefined,
    };

    this.submitting = true;
    this.transactionService.createTransaction(accountId, payload).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigate(['/transactions']);
      },
      error: () => {
        this.submitting = false;
      },
    });
  }
}
'@

Write-File -Path "src/app/features/transaction/components/transaction-create/transaction-create.component.html" -Content @'
<mat-card>
  <h2>Creer une transaction</h2>

  <form [formGroup]="form" (ngSubmit)="submit()" class="tx-form">
    <mat-form-field appearance="outline">
      <mat-label>Account ID</mat-label>
      <input matInput type="number" formControlName="accountId" />
    </mat-form-field>

    <mat-form-field appearance="outline">
      <mat-label>Beneficiaire</mat-label>
      <input matInput formControlName="beneficiaryName" />
    </mat-form-field>

    <mat-form-field appearance="outline">
      <mat-label>RIB beneficiaire</mat-label>
      <input matInput formControlName="beneficiaryRib" />
    </mat-form-field>

    <mat-form-field appearance="outline">
      <mat-label>Montant</mat-label>
      <input matInput type="number" formControlName="amount" />
      <mat-error *ngIf="form.controls['amount'].hasError('required')">Montant requis</mat-error>
      <mat-error *ngIf="form.controls['amount'].hasError('min')">Montant invalide</mat-error>
    </mat-form-field>

    <mat-form-field appearance="outline">
      <mat-label>Type</mat-label>
      <mat-select formControlName="type">
        <mat-option value="DEPOSIT">DEPOSIT</mat-option>
        <mat-option value="WITHDRAWAL">WITHDRAWAL</mat-option>
        <mat-option value="TRANSFER">TRANSFER</mat-option>
        <mat-option value="PAYMENT">PAYMENT</mat-option>
      </mat-select>
    </mat-form-field>

    <mat-form-field appearance="outline">
      <mat-label>Categorie</mat-label>
      <input matInput formControlName="category" />
    </mat-form-field>

    <mat-form-field appearance="outline">
      <mat-label>Description</mat-label>
      <input matInput formControlName="description" />
    </mat-form-field>

    <mat-form-field appearance="outline">
      <mat-label>Periodicite</mat-label>
      <mat-select formControlName="periodicity">
        <mat-option value="NOW">NOW</mat-option>
        <mat-option value="WEEKLY">WEEKLY</mat-option>
        <mat-option value="MONTHLY">MONTHLY</mat-option>
      </mat-select>
    </mat-form-field>

    <mat-form-field appearance="outline">
      <mat-label>Scheduled date</mat-label>
      <input matInput type="datetime-local" formControlName="scheduledDate" />
    </mat-form-field>

    <mat-form-field appearance="outline">
      <mat-label>Next execution</mat-label>
      <input matInput type="datetime-local" formControlName="nextExecutionDate" />
    </mat-form-field>

    <button mat-raised-button color="primary" type="submit" [disabled]="submitting">
      Enregistrer
    </button>
  </form>
</mat-card>
'@

Write-File -Path "src/app/features/transaction/components/transaction-create/transaction-create.component.scss" -Content @'
.tx-form {
  display: grid;
  gap: 1rem;
  max-width: 720px;
}
'@

# Shell components for other modules
Write-File -Path "src/app/features/leasing/pages/leasing-home/leasing-home.component.html" -Content @'
<mat-card>
  <h2>Leasing</h2>
  <p>Zone prete pour l integration du module Leasing.</p>
</mat-card>
'@

Write-File -Path "src/app/features/crowdfunding/pages/crowdfunding-home/crowdfunding-home.component.html" -Content @'
<mat-card>
  <h2>Crowdfunding</h2>
  <p>Zone prete pour l integration du module Crowdfunding.</p>
</mat-card>
'@

Write-File -Path "src/app/features/loan/pages/loan-home/loan-home.component.html" -Content @'
<mat-card>
  <h2>Loan Management</h2>
  <p>Zone prete pour l integration du module Loan.</p>
</mat-card>
'@

# Layout and pages
Write-File -Path "src/app/layouts/navbar/navbar.component.html" -Content @'
<mat-toolbar color="primary" class="main-toolbar">
  <span class="brand">HELMA</span>
  <span class="spacer"></span>
  <a mat-button routerLink="/">Accueil</a>
  <a mat-button routerLink="/dashboard">Dashboard</a>
  <a mat-button routerLink="/transactions">Transactions</a>
  <a mat-button routerLink="/bank-accounts">Comptes</a>
  <a mat-button routerLink="/virtual-cards">Cartes</a>
  <a mat-button routerLink="/leasing">Leasing</a>
  <a mat-button routerLink="/crowdfunding">Crowdfunding</a>
  <a mat-button routerLink="/loan">Loan</a>
  <a mat-button routerLink="/login">Login</a>
</mat-toolbar>
'@

Write-File -Path "src/app/layouts/navbar/navbar.component.scss" -Content @'
.main-toolbar {
  position: sticky;
  top: 0;
  z-index: 100;
}

.brand {
  font-weight: 700;
  letter-spacing: 0.08em;
}

.spacer {
  flex: 1;
}
'@

Write-File -Path "src/app/layouts/sidebar/sidebar.component.html" -Content @'
<nav class="sidebar-nav">
  <a routerLink="/dashboard">Dashboard</a>
  <a routerLink="/transactions">Transactions</a>
  <a routerLink="/bank-accounts">Comptes</a>
  <a routerLink="/virtual-cards">Cartes</a>
  <a routerLink="/leasing">Leasing</a>
  <a routerLink="/crowdfunding">Crowdfunding</a>
  <a routerLink="/loan">Loan</a>
</nav>
'@

Write-File -Path "src/app/layouts/sidebar/sidebar.component.scss" -Content @'
.sidebar-nav {
  display: grid;
  gap: 0.5rem;
  padding: 1rem;
}

.sidebar-nav a {
  text-decoration: none;
  color: #1f3f45;
  padding: 0.6rem 0.8rem;
  border-radius: 0.6rem;
}

.sidebar-nav a:hover {
  background: #d8f2eb;
}
'@

Write-File -Path "src/app/layouts/footer/footer.component.html" -Content @'
<footer class="app-footer">
  <small>HELMA - Digital Micro-finance Platform (2026)</small>
</footer>
'@

Write-File -Path "src/app/layouts/footer/footer.component.scss" -Content @'
.app-footer {
  border-top: 1px solid var(--border-color);
  padding: 0.8rem 1rem;
  text-align: center;
  color: #335a60;
  background: #f8fcfb;
}
'@

Write-File -Path "src/app/pages/home/home.component.html" -Content @'
<section class="hero">
  <h1>Bienvenue sur HELMA</h1>
  <p>Plateforme digitale de micro-finance dediee aux jeunes.</p>
  <a mat-raised-button color="primary" routerLink="/transactions">Voir les transactions</a>
</section>
'@

Write-File -Path "src/app/pages/login/login.component.html" -Content @'
<mat-card>
  <h2>Connexion</h2>
  <p>Page prete pour integration avec ton backend de securite.</p>
</mat-card>
'@

Write-File -Path "src/app/pages/dashboard/dashboard.component.html" -Content @'
<section>
  <h2>Dashboard</h2>
  <p>Vue globale des services: Leasing, Crowdfunding, Loan, Transaction.</p>
</section>
'@

# Shared reusable components
Write-File -Path "src/app/shared/components/app-card/app-card.component.ts" -Content @'
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-card',
  templateUrl: './app-card.component.html',
  styleUrls: ['./app-card.component.scss'],
})
export class AppCardComponent {
  @Input() title = '';
}
'@

Write-File -Path "src/app/shared/components/app-card/app-card.component.html" -Content @'
<mat-card>
  <h3 *ngIf="title">{{ title }}</h3>
  <ng-content></ng-content>
</mat-card>
'@

Write-File -Path "src/app/shared/components/app-button/app-button.component.ts" -Content @'
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-button',
  templateUrl: './app-button.component.html',
  styleUrls: ['./app-button.component.scss'],
})
export class AppButtonComponent {
  @Input() label = 'Action';
  @Input() color: 'primary' | 'accent' | 'warn' = 'primary';
  @Input() disabled = false;
  @Output() clicked = new EventEmitter<void>();
}
'@

Write-File -Path "src/app/shared/components/app-button/app-button.component.html" -Content @'
<button mat-raised-button [color]="color" [disabled]="disabled" (click)="clicked.emit()">
  {{ label }}
</button>
'@

Write-File -Path "src/app/shared/components/app-table/app-table.component.ts" -Content @'
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-table',
  templateUrl: './app-table.component.html',
  styleUrls: ['./app-table.component.scss'],
})
export class AppTableComponent {
  @Input() columns: string[] = [];
  @Input() data: Record<string, unknown>[] = [];
}
'@

Write-File -Path "src/app/shared/components/app-table/app-table.component.html" -Content @'
<div class="table-shell">
  <table>
    <thead>
      <tr>
        <th *ngFor="let c of columns">{{ c }}</th>
      </tr>
    </thead>
    <tbody>
      <tr *ngFor="let row of data">
        <td *ngFor="let c of columns">{{ row[c] }}</td>
      </tr>
    </tbody>
  </table>
</div>
'@

Write-File -Path "src/styles.scss" -Content @'
:root {
  --surface-soft: #f0fbf8;
  --border-color: #d4e4e4;
}

* {
  box-sizing: border-box;
}

html,
body {
  margin: 0;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background: linear-gradient(180deg, #f7fcff 0%, #eefaf6 100%);
  color: #173a3f;
}
'@

Write-Section "7) Documentation de demarrage"
Write-File -Path "README.SCAFFOLD.md" -Content @"
HELMA Frontend Scaffold

Commande de base:
  powershell -ExecutionPolicy Bypass -File .\\scripts\\scaffold-helma-frontend.ps1

Options utiles:
  -BackendUrl "http://localhost:8080"
  -AppName "helma-frontend"
  -FrontendRoot "frontend"
  -SkipMaterial
  -SkipBuild

Resultat:
- Architecture NgModules complete
- Module Transaction pret a connecter avec backend Spring
- Ecrans shell pour Leasing, Crowdfunding, Loan
- UI Angular Material responsive
"@

if (-not $SkipBuild) {
  Write-Section "8) Build de verification"
  npm run build
  if ($LASTEXITCODE -ne 0) {
    throw "Le build Angular a echoue. Corrige les erreurs puis relance."
  }
  Write-Success "Build termine avec succes"
} else {
  Write-Info "Build ignore (option SkipBuild)"
}

Write-Section "Termine"
Write-Success "Frontend Angular pret dans: $AppPath"
Write-Info "Lancer l'app: cd $AppPath ; npm start"

Pop-Location
