import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
  import { CommonModule } from '@angular/common';
@Component({
  selector: 'app-layout-admin',
  imports: [CommonModule, RouterOutlet,RouterLink ,RouterLinkActive ],
  templateUrl: './layout-admin.component.html',
  styleUrl: './layout-admin.component.css'
})
export class LayoutAdminComponent {

}
