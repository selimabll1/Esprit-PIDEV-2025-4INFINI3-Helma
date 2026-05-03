import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DepositHistoryAdminComponentComponent } from './deposit-history-admin-component.component';

describe('DepositHistoryAdminComponentComponent', () => {
  let component: DepositHistoryAdminComponentComponent;
  let fixture: ComponentFixture<DepositHistoryAdminComponentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DepositHistoryAdminComponentComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DepositHistoryAdminComponentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
