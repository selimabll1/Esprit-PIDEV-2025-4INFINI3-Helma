import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="hero">
      <div class="container hero-grid">
        <div class="hero-copy">
          <span class="hero-kicker">Financial youth inclusion</span>
          <h1>Build your dream with inclusive finance</h1>
          <p>
            Helma empowers youth to save better, access responsible financing,
            and grow ideas into real opportunities through donation and equity crowdfunding.
          </p>

          <div class="hero-actions">
            <a class="btn btn-primary" routerLink="/auth/login">Start Your Journey</a>
            <a class="btn btn-secondary" routerLink="/discover">Explore Opportunities</a>
          </div>

          <div class="hero-trust">
            <span>Trusted funding pathways</span>
            <span>Youth-first guidance</span>
            <span>Transparent growth</span>
          </div>
        </div>

        <div class="hero-visual card">
          <div class="visual-blob"></div>
          <div class="visual-content">
            <div class="growth-card">
              <span class="growth-label">Growth Score</span>
              <strong>87%</strong>
              <small>Stronger youth opportunity index</small>
            </div>

            <div class="coin-stack">
              <span></span><span></span><span></span>
            </div>

            <img src="logo/helma-logo.png" alt="Helma" class="hero-logo" />
          </div>
        </div>
      </div>
    </section>

    <section class="stats">
      <div class="container stats-grid">
        <article class="card stat-card">
          <span class="stat-icon">◎</span>
          <div>
            <p class="stat-label">Youth Supported</p>
            <h3>12,448</h3>
            <small>Budgeting, savings, and guidance journeys</small>
          </div>
        </article>

        <article class="card stat-card">
          <span class="stat-icon">◔</span>
          <div>
            <p class="stat-label">Projects Funded</p>
            <h3>732</h3>
            <small>Validated ideas launched with community support</small>
          </div>
        </article>

        <article class="card stat-card">
          <span class="stat-icon">◈</span>
          <div>
            <p class="stat-label">Funding Raised</p>
            <h3>$5.9M</h3>
            <small>Donation and equity opportunities combined</small>
          </div>
        </article>

        <article class="card stat-card">
          <span class="stat-icon">✓</span>
          <div>
            <p class="stat-label">Inclusion Index</p>
            <h3>92%</h3>
            <small>Designed for clarity, trust, and accessibility</small>
          </div>
        </article>
      </div>
    </section>

    <section class="features-section">
      <div class="container">
        <div class="section-head">
          <span class="eyebrow">What Helma offers</span>
          <h2>One platform, multiple growth paths</h2>
          <p>
            A unified experience for youth finance, responsible financing,
            startup fundraising, investor participation, and sponsor visibility.
          </p>
        </div>

        <div class="features-grid">
          <article class="card feature-card">
            <span class="feature-badge">Youth Tools</span>
            <h3>Savings, budgeting, and AI guidance</h3>
            <p>
              Build better habits through dashboards, alerts, and coaching-oriented guidance.
            </p>
            <a routerLink="/youth-tools">Explore youth tools</a>
          </article>

          <article class="card feature-card">
            <span class="feature-badge">Financing</span>
            <h3>Micro-loans and equipment micro-leasing</h3>
            <p>
              Access responsible financing and productivity tools through structured installment flows.
            </p>
            <a routerLink="/financing">See financing options</a>
          </article>

          <article class="card feature-card">
            <span class="feature-badge">Donation</span>
            <h3>Donation crowdfunding for impact-led ideas</h3>
            <p>
              Support social, creative, and community-driven initiatives with transparent progress.
            </p>
            <a routerLink="/crowdfunding/donation">View donation model</a>
          </article>

          <article class="card feature-card">
            <span class="feature-badge">Equity</span>
            <h3>Selective equity crowdfunding for vetted ventures</h3>
            <p>
              Founders submit proposals, campaigns are reviewed, and investors follow structured updates.
            </p>
            <a routerLink="/crowdfunding/equity">View equity model</a>
          </article>

          <article class="card feature-card">
            <span class="feature-badge">Investors</span>
            <h3>Portfolio visibility and trusted campaign access</h3>
            <p>
              Investors browse approved campaigns, subscribe, and follow portfolio performance.
            </p>
            <a routerLink="/discover">Discover campaigns</a>
          </article>

          <article class="card feature-card">
            <span class="feature-badge">Sponsors</span>
            <h3>Sponsorship packages and impact reporting</h3>
            <p>
              Partners gain structured visibility while supporting youth inclusion and innovation.
            </p>
            <a routerLink="/sponsors">See sponsor opportunities</a>
          </article>
        </div>
      </div>
    </section>

    <section class="crowdfunding-highlight">
      <div class="container split-grid">
        <div class="card split-card">
          <span class="eyebrow">Crowdfunding</span>
          <h2>Two campaign models inside one platform</h2>
          <p>
            Helma supports both donation-based fundraising and selective equity crowdfunding,
            so you can match the funding model to the project mission and maturity.
          </p>

          <div class="dual-cards">
            <div class="mini-panel">
              <h4>Donation</h4>
              <p>Best for impact, community, and early support.</p>
              <a routerLink="/crowdfunding/donation">Explore donation</a>
            </div>

            <div class="mini-panel">
              <h4>Equity</h4>
              <p>Best for vetted ventures seeking investor participation.</p>
              <a routerLink="/crowdfunding/equity">Explore equity</a>
            </div>
          </div>
        </div>

        <div class="card insights-card">
          <span class="eyebrow">Today’s insights</span>
          <h3>What makes Helma different</h3>

          <ul>
            <li>Youth journey first</li>
            <li>Micro-leasing and micro-loans in the same ecosystem</li>
            <li>Selective campaign approval before equity launch</li>
            <li>Investor tracking and sponsor reporting</li>
          </ul>

          <a class="btn btn-secondary" routerLink="/resources">Read more</a>
        </div>
      </div>
    </section>

    <section class="cta-band">
      <div class="container cta-band-inner">
        <div>
          <span class="eyebrow">Start with Helma</span>
          <h2>Empower your next step</h2>
          <p>
            Whether you are building financial stability, launching a project,
            investing in vetted opportunities, or supporting impact, Helma gives you one place to begin.
          </p>
        </div>

        <div class="cta-actions">
          <a class="btn btn-primary" routerLink="/auth/login">Sign in</a>
          <a class="btn btn-secondary" routerLink="/discover">Explore the platform</a>
        </div>
      </div>
    </section>

    <footer class="site-footer">
      <div class="container footer-grid">
        <div>
          <div class="footer-brand">
            <img src="logo/helma-logo.png" alt="Helma logo" />
            <div>
              <strong>Helma</strong>
              <p>Empower. Innovate. Growth.</p>
            </div>
          </div>
        </div>

        <div>
          <h4>Platform</h4>
          <a routerLink="/">Home</a>
          <a routerLink="/discover">Discover</a>
          <a routerLink="/crowdfunding/donation">Donation</a>
          <a routerLink="/crowdfunding/equity">Equity</a>
        </div>

        <div>
          <h4>Features</h4>
          <a routerLink="/youth-tools">Youth Tools</a>
          <a routerLink="/financing">Financing</a>
          <a routerLink="/learning">Learning</a>
          <a routerLink="/sponsors">Sponsors</a>
        </div>

        <div>
          <h4>Resources</h4>
          <a routerLink="/resources">Guides</a>
          <a routerLink="/auth/login">Sign in</a>
        </div>
      </div>
    </footer>
  `,
  styles: [`
    :host {
      display: block;
    }

    .hero,
    .stats,
    .features-section,
    .crowdfunding-highlight,
    .cta-band {
      padding: 32px 0 0;
    }

    .hero {
      background:
        radial-gradient(circle at top right, rgba(15,107,104,0.10), transparent 28%),
        linear-gradient(180deg, var(--helma-ivory), #fff 60%);
      padding-top: 40px;
    }

    .hero-grid,
    .split-grid {
      display: grid;
      grid-template-columns: 1.1fr 0.9fr;
      gap: 24px;
      align-items: stretch;
    }

    .hero-copy {
      padding: 24px 0;
    }

    .hero-kicker,
    .eyebrow {
      display: inline-flex;
      align-items: center;
      padding: 8px 12px;
      border-radius: 999px;
      background: var(--helma-mint);
      color: var(--helma-teal);
      font-weight: 800;
      font-size: 12px;
      margin-bottom: 16px;
    }

    h1 {
      max-width: 640px;
      margin-bottom: 16px;
    }

    .hero-copy p {
      max-width: 620px;
      font-size: 16px;
    }

    .hero-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      margin-top: 24px;
    }

    .hero-trust {
      margin-top: 24px;
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
    }

    .hero-trust span {
      padding: 8px 12px;
      border-radius: 999px;
      border: 1px solid var(--color-border);
      background: #fff;
      font-size: 12px;
      font-weight: 700;
      color: var(--color-text-muted);
    }

    .hero-visual {
      position: relative;
      overflow: hidden;
      min-height: 440px;
      background: linear-gradient(180deg, #fff, var(--helma-ivory));
    }

    .visual-blob {
      position: absolute;
      inset: auto -50px -70px auto;
      width: 260px;
      height: 260px;
      background: radial-gradient(circle, rgba(212,166,42,0.25), rgba(15,107,104,0.08), transparent 70%);
      border-radius: 50%;
    }

    .visual-content {
      position: relative;
      height: 100%;
      display: grid;
      place-items: center;
      gap: 20px;
      padding: 24px;
    }

    .hero-logo {
      width: 240px;
      max-width: 100%;
      object-fit: contain;
      filter: drop-shadow(0 10px 30px rgba(15, 23, 42, 0.08));
    }

    .growth-card {
      align-self: start;
      justify-self: end;
      background: #fff;
      border: 1px solid var(--color-border);
      border-radius: 18px;
      padding: 16px 18px;
      box-shadow: var(--shadow-sm);
      text-align: right;
    }

    .growth-card strong {
      display: block;
      font-size: 36px;
      line-height: 1;
      color: var(--helma-teal);
      margin: 8px 0;
    }

    .growth-label {
      font-size: 12px;
      font-weight: 800;
      color: var(--color-text-muted);
    }

    .growth-card small {
      color: var(--color-text-muted);
    }

    .coin-stack {
      display: flex;
      gap: 10px;
      align-self: end;
      justify-self: start;
    }

    .coin-stack span {
      width: 58px;
      height: 58px;
      border-radius: 50%;
      background: radial-gradient(circle at 30% 30%, #ffe08a, var(--helma-gold));
      box-shadow: var(--shadow-sm);
      border: 4px solid rgba(255,255,255,0.8);
    }

    .stats-grid,
    .features-grid {
      display: grid;
      gap: 20px;
    }

    .stats-grid {
      grid-template-columns: repeat(4, 1fr);
    }

    .stat-card {
      display: flex;
      gap: 14px;
      align-items: flex-start;
    }

    .stat-card h3 {
      margin: 6px 0;
    }

    .stat-icon {
      width: 44px;
      height: 44px;
      display: grid;
      place-items: center;
      border-radius: 14px;
      background: var(--helma-teal-soft);
      color: var(--helma-teal);
      font-weight: 800;
      flex: 0 0 auto;
    }

    .stat-label {
      margin: 0;
      color: var(--color-text-muted);
      font-weight: 700;
    }

    .section-head {
      max-width: 760px;
      margin-bottom: 24px;
    }

    .features-grid {
      grid-template-columns: repeat(3, 1fr);
    }

    .feature-card {
      display: flex;
      flex-direction: column;
      gap: 12px;
      min-height: 250px;
    }

    .feature-card h3 {
      margin-bottom: 0;
    }

    .feature-card p {
      flex: 1;
      margin-bottom: 0;
    }

    .feature-card a,
    .mini-panel a {
      font-weight: 800;
      color: var(--helma-teal);
      text-decoration: none;
    }

    .feature-badge {
      display: inline-flex;
      width: fit-content;
      padding: 8px 12px;
      border-radius: 999px;
      background: #fff7e0;
      color: #8f6b12;
      font-size: 12px;
      font-weight: 800;
    }

    .split-card,
    .insights-card {
      min-height: 100%;
    }

    .dual-cards {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 14px;
      margin-top: 20px;
    }

    .mini-panel {
      padding: 18px;
      border-radius: 16px;
      background: var(--color-surface-soft);
      border: 1px solid var(--color-border);
    }

    .mini-panel h4 {
      margin: 0 0 8px;
    }

    .insights-card ul {
      margin: 18px 0 22px;
      padding-left: 18px;
      color: var(--color-text-muted);
      line-height: 1.8;
    }

    .cta-band {
      padding-bottom: 32px;
    }

    .cta-band-inner {
      display: flex;
      justify-content: space-between;
      gap: 24px;
      align-items: center;
      background: linear-gradient(135deg, rgba(15,107,104,0.08), rgba(212,166,42,0.10));
      border: 1px solid var(--color-border);
      border-radius: 24px;
      padding: 32px;
    }

    .cta-actions {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
      justify-content: flex-end;
    }

    .site-footer {
      background: var(--helma-teal);
      color: white;
      margin-top: 12px;
      padding: 32px 0;
    }

    .footer-grid {
      display: grid;
      grid-template-columns: 1.4fr 1fr 1fr 1fr;
      gap: 24px;
    }

    .footer-brand {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .footer-brand img {
      width: 62px;
      height: 62px;
      object-fit: contain;
    }

    .footer-brand p {
      margin: 4px 0 0;
      color: rgba(255,255,255,0.75);
    }

    .site-footer h4 {
      margin-bottom: 12px;
      color: white;
    }

    .site-footer a {
      display: block;
      margin-bottom: 10px;
      color: rgba(255,255,255,0.82);
      text-decoration: none;
    }

    .site-footer a:hover {
      color: #fff;
    }

    @media (max-width: 1100px) {
      .stats-grid {
        grid-template-columns: repeat(2, 1fr);
      }

      .features-grid {
        grid-template-columns: repeat(2, 1fr);
      }

      .hero-grid,
      .split-grid,
      .footer-grid {
        grid-template-columns: 1fr;
      }
    }

    @media (max-width: 720px) {
      .stats-grid,
      .features-grid,
      .dual-cards {
        grid-template-columns: 1fr;
      }

      .cta-band-inner {
        flex-direction: column;
        align-items: flex-start;
      }

      h1 {
        font-size: 34px;
      }
    }
  `]
})
export class HomePageComponent {}