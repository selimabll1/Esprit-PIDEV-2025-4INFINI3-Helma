import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-forbidden',
  templateUrl: './forbidden.component.html'
})
export class ForbiddenComponent {
  constructor(private readonly router: Router) {}

  goBack(): void {
    void this.router.navigate(['/dashboard']);
  }
}
