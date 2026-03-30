# Helma – Micro Finance Platform

## Overview
This project was developed as part of the PIDEV Engineering Program at **Esprit School of Engineering** (Academic Year 2025–2026).

**Helma** is a web-based micro finance platform designed to improve financial inclusion for young people under 25. The platform combines personal financial management tools, responsible financing services, and a selective equity crowdfunding workflow in a unified digital solution.

Helma helps users manage savings goals, track budgets, access micro-loans and micro-leasing services, and participate in a trusted investment ecosystem through verified campaigns and transparent workflows.

## Features
- Secure user registration and login
- User profile management
- KYC document submission and verification
- Savings goals management
- Budget tracking dashboard with alerts
- Financial guidance and coaching support
- Micro-loan application workflow
- Equipment micro-leasing requests
- Installment and repayment tracking
- Founder project proposal submission
- Campaign approval workflow for validated projects
- Approved campaign browsing
- Equity investment subscription
- Investor portfolio and campaign update tracking
- Sponsorship package purchase
- Sponsor reporting dashboard
- Support ticket creation

## Tech Stack

### Frontend
- Angular
- TypeScript
- Responsive web interface

### Backend
- Spring Boot
- Spring Security
- RESTful APIs
- Role-based access control
- Integration with external KYC verification services
- Integration with payment providers

### Database
- MySQL

### DevOps & Tools
- Git
- Docker

## Architecture
Helma follows a web-based client-server architecture:

- **Frontend layer:** built with Angular to provide a modular and responsive user interface
- **Backend layer:** built with Spring Boot to handle business logic, authentication, authorization, and API services
- **Security layer:** implemented with Spring Security for secure access control and role management
- **Database layer:** powered by MySQL for storing users, KYC records, campaigns, investments, payments, and repayment schedules
- **External integrations:** KYC verification services and payment providers

## Contributors
- Aziz Gharbi
- Selima Bellil
- Emine Souissi
- Rihem Zoghlami
- Yassmine Tebib
- Yassine Kamoun

## Academic Context
Developed at **Esprit School of Engineering – Tunisia**

- **Project:** Helma – Micro Finance Platform
- **Program:** PIDEV
- **Class:** 4 INFINI 3
- **Academic Year:** 2025–2026

### Supervisors
- Aymen Selmi
- Ichraf Ayari
- Nadine Maazoune

## Getting Started

### Prerequisites
Make sure you have the following installed:
- Node.js and npm
- Angular CLI
- Java
- Maven
- MySQL
- Docker 

### Frontend Setup
```bash
cd frontend
npm install
ng serve
