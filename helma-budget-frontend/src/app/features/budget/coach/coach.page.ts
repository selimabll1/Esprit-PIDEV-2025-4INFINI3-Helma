import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BudgetApiService } from '../../../core/services/budget-api.service';
import { AuthStorageService } from '../../../core/services/auth-storage.service';
import { CoachMessage } from '../../../core/models/budget.models';

@Component({
  selector: 'app-coach-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <header class="page-header">
      <div>
        <span class="eyebrow">AI Coach</span>
        <h1>Financial Guidance</h1>
        <p class="subtitle">Ask your AI coach for budgeting tips, savings advice, or financial analysis.</p>
      </div>
    </header>

    <div class="chat-container card">
      <div class="chat-messages" #chatBox>
        <div class="welcome" *ngIf="!messages().length && !loading()">
          <span class="welcome-icon">✦</span>
          <h3>Your Financial Coach</h3>
          <p>I can help with budgeting strategies, savings plans, spending analysis, and more. Ask me anything!</p>
          <div class="suggestions">
            <button class="suggestion" (click)="sendQuick('How can I reduce my expenses this month?')">Reduce expenses</button>
            <button class="suggestion" (click)="sendQuick('Create a savings plan for 1000 TND in 3 months')">Savings plan</button>
            <button class="suggestion" (click)="sendQuick('Analyze my spending patterns and give advice')">Spending analysis</button>
          </div>
        </div>

        <div *ngFor="let msg of messages()"
             class="msg"
             [class.msg-user]="msg.role === 'USER'"
             [class.msg-ai]="msg.role === 'ASSISTANT'">
          <span class="msg-avatar" *ngIf="msg.role === 'ASSISTANT'">✦</span>
          <div class="msg-bubble">
            <p>{{ msg.content }}</p>
            <span class="msg-time">{{ msg.createdAt | date:'shortTime' }}</span>
          </div>
          <span class="msg-avatar user-avatar" *ngIf="msg.role === 'USER'">
            {{ userInitial }}
          </span>
        </div>

        <div class="msg msg-ai" *ngIf="sending()">
          <span class="msg-avatar">✦</span>
          <div class="msg-bubble typing">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <div class="chat-input-row">
        <input class="input chat-input" type="text"
               [(ngModel)]="inputText"
               (keydown.enter)="send()"
               placeholder="Ask your coach…"
               [disabled]="sending()" />
        <button class="btn btn-primary" (click)="send()" [disabled]="sending() || !inputText.trim()">
          Send
        </button>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .page-header { margin-bottom: 20px; }
    .page-header h1 { margin-bottom: 4px; }
    .eyebrow { display: inline-flex; padding: 6px 10px; border-radius: 999px; background: var(--helma-mint); color: var(--helma-teal); font-size: 12px; font-weight: 800; margin-bottom: 8px; }
    .subtitle { color: var(--color-text-muted); margin: 0; }

    .chat-container { display: flex; flex-direction: column; height: calc(100vh - 220px); padding: 0; overflow: hidden; }

    .chat-messages { flex: 1; overflow-y: auto; padding: 24px; display: flex; flex-direction: column; gap: 16px; }

    .welcome { text-align: center; padding: 40px 20px; }
    .welcome-icon { font-size: 40px; display: block; margin-bottom: 12px; }
    .welcome h3 { margin-bottom: 8px; }
    .welcome p { max-width: 420px; margin: 0 auto 20px; color: var(--color-text-muted); }
    .suggestions { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
    .suggestion { padding: 8px 16px; border-radius: 999px; border: 1px solid var(--color-border); background: #fff; font-size: 13px; font-weight: 600; cursor: pointer; transition: 0.15s; }
    .suggestion:hover { border-color: var(--helma-teal); color: var(--helma-teal); background: var(--helma-teal-soft); }

    .msg { display: flex; gap: 10px; align-items: flex-end; }
    .msg-user { justify-content: flex-end; }
    .msg-avatar { width: 32px; height: 32px; display: grid; place-items: center; border-radius: 50%; background: var(--helma-teal); color: #fff; font-size: 14px; font-weight: 800; flex: 0 0 auto; }
    .user-avatar { background: var(--helma-gold); color: #1f2937; }

    .msg-bubble { max-width: 70%; padding: 14px 18px; border-radius: 18px; }
    .msg-ai .msg-bubble { background: var(--color-surface-soft); border: 1px solid var(--color-border); border-bottom-left-radius: 4px; }
    .msg-user .msg-bubble { background: var(--helma-teal); color: #fff; border-bottom-right-radius: 4px; }
    .msg-bubble p { margin: 0; font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
    .msg-time { display: block; font-size: 10px; margin-top: 6px; opacity: 0.6; }

    .typing { display: flex; gap: 6px; padding: 16px 20px; }
    .typing span { width: 8px; height: 8px; border-radius: 50%; background: var(--color-text-muted); animation: bounce 1.2s infinite; }
    .typing span:nth-child(2) { animation-delay: 0.15s; }
    .typing span:nth-child(3) { animation-delay: 0.3s; }
    @keyframes bounce { 0%, 80%, 100% { transform: translateY(0); } 40% { transform: translateY(-6px); } }

    .chat-input-row { display: flex; gap: 10px; padding: 16px 20px; border-top: 1px solid var(--color-border); }
    .chat-input { flex: 1; border-radius: 999px; padding: 12px 20px; }
  `]
})
export class CoachPage implements OnInit, AfterViewChecked {
  private readonly api = inject(BudgetApiService);
  private readonly auth = inject(AuthStorageService);

  @ViewChild('chatBox') chatBox!: ElementRef;

  messages = signal<CoachMessage[]>([]);
  sending = signal(false);
  loading = signal(false);
  inputText = '';
  userInitial = '';

  ngOnInit(): void {
    const user = this.auth.getUser();
    this.userInitial = user?.email?.charAt(0).toUpperCase() ?? 'U';
    this.loadHistory();
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  loadHistory(): void {
    const userId = this.auth.getUser()?.userId;
    if (!userId) return;
    this.loading.set(true);
    this.api.getCoachHistory(userId).subscribe({
      next: (msgs) => { this.messages.set(msgs); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  send(): void {
    if (!this.inputText.trim() || this.sending()) return;
    const userId = this.auth.getUser()?.userId;
    if (!userId) return;

    const text = this.inputText.trim();
    this.inputText = '';

    // Optimistic user message
    this.messages.update(msgs => [...msgs, { messageId: 0, role: 'USER', content: text, createdAt: new Date().toISOString() }]);

    this.sending.set(true);
    this.api.sendCoachMessage({ userId, message: text }).subscribe({
      next: (res) => {
        this.messages.update(msgs => [...msgs, { messageId: res.messageId, role: 'ASSISTANT', content: res.content, createdAt: res.createdAt }]);
        this.sending.set(false);
      },
      error: () => {
        this.messages.update(msgs => [...msgs, { messageId: 0, role: 'ASSISTANT', content: 'Sorry, I encountered an error. Please try again.', createdAt: new Date().toISOString() }]);
        this.sending.set(false);
      }
    });
  }

  sendQuick(text: string): void {
    this.inputText = text;
    this.send();
  }

  private scrollToBottom(): void {
    if (this.chatBox) {
      this.chatBox.nativeElement.scrollTop = this.chatBox.nativeElement.scrollHeight;
    }
  }
}
