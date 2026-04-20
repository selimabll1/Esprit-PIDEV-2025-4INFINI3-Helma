import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TopNavbarComponent } from '../top-navbar/top-navbar.component';

@Component({
  selector: 'app-public-shell',
  standalone: true,
  imports: [RouterOutlet, TopNavbarComponent],
  template: `
    <app-top-navbar></app-top-navbar>

    <main class="public-main">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .public-main {
      min-height: calc(100vh - var(--navbar-height));
    }
  `]
})
export class PublicShellComponent {}