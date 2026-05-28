# Subscription Analytics

Simple description

This project is a full-stack web application that collects app-usage data, maps usage to subscriptions, generates billing records, and provides basic recommendations to help users optimize subscriptions.

## Features
- Usage tracking and aggregation
- Subscription CRUD and mapping by package name
- Billing record generation
- Recommendation engine (KEEP / CONSIDER / CANCEL)

## Architecture

Frontend (React + Vite) ↔ Backend (Spring Boot) ↔ MySQL

## Tools & Tech Stack
- Frontend: React, Vite, Tailwind/CSS, axios
- Backend: Java 17, Spring Boot, Spring Data JPA, Spring Security, JWT, Flyway
- Database: MySQL (development can use H2)
- Storage/Uploads: Cloudinary (optional)
- Build & tooling: Maven, Node.js, npm


**Subscription Analytics** is a full-stack web application designed to help users manage multiple subscriptions efficiently. It provides:

- **Subscription Management**: Create, update, and track all your subscriptions in one place
- **Billing & Payment Tracking**: Monitor upcoming payments and payment history
- **Usage Analytics**: Track service usage patterns and get insights
- **Smart Recommendations**: AI-powered suggestions to optimize your subscriptions
- **CSV Import**: Bulk upload subscriptions via CSV files
- **Browser Extension**: Real-time usage tracking via browser extension

The platform consists of a **Spring Boot REST API backend**, a **React frontend**, and a **Chrome browser extension** for seamless tracking.

---

## ✨ Features

### Core Features

- ✅ **User Authentication**
  - Secure registration and login
  - JWT-based token authentication
  - Password encryption (BCrypt)
  - Persistent sessions with token refresh

- ✅ **Subscription Management**
  - Create, read, update, delete subscriptions
  - Track subscription categories (Streaming, Music, Cloud, Productivity, Gaming, Fitness, News)
  - Monitor renewal dates and billing cycles
  - Subscription status tracking (Active, Paused, Inactive, Expired)

- ✅ **Billing & Payments**
  - Automatic billing record creation
  - Payment tracking by multiple methods (Credit Card, Debit Card, PayPal, Bank Transfer)
  - Upcoming payment alerts (30-day forecast)
  - Billing history and payment records

- ✅ **Usage Tracking**
  - Real-time service usage monitoring via browser extension
  - Monthly usage aggregation and trends
  - Supported services configuration
  - Idempotency for duplicate prevention

- ✅ **Recommendations Engine**
  - Intelligent subscription suggestions based on usage patterns
  - Recommendation types: Upgrade, Downgrade, Cancel
  - Confidence scoring for recommendations
  - User acceptance/rejection tracking

- ✅ **Analytics & Dashboard**
  - Real-time subscription summary
  - Monthly spending overview
  - Usage metrics and trends
  - Upcoming payments widget
  - Recommendation alerts

- ✅ **Data Import/Export**
  - CSV file upload for bulk subscription import
  - File storage via Cloudinary
  - CSV parsing and validation

---

## 🏗️ Architecture

### Layered Architecture

```
┌─────────────────────────────────────────────────┐
│         Frontend (React + Vite)                 │
│    - React Components & Functional Hooks        │
│    - Context API for State Management           │
│    - Route-based Navigation                     │
│    - Axios HTTP Client                          │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│    REST API Layer (Spring Boot Controllers)     │
│    - REST endpoints with validation             │
│    - Request/Response DTOs                      │
│    - HTTP status code handling                  │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│   Business Logic Layer (Services)               │
│    - SubscriptionService                        │
│    - BillingService                             │
│    - UsageTrackingService                       │
│    - RecommendationService                      │
│    - AuthService                                │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│   Data Access Layer (Repositories)              │
│    - Spring Data JPA                            │
│    - Custom Query Methods                       │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│   Database Layer (MySQL)                        │
│    - Flyway Migrations                          │
│    - Users, Subscriptions, BillingRecords       │
│    - UsageTracking, Recommendations             │
└─────────────────────────────────────────────────┘
```

### Design Patterns Used

| Pattern | Implementation | Benefit |
|---------|----------------|---------|
| **MVC** | Controllers → Services → Repositories | Clear separation of concerns |
| **Repository** | Spring Data JPA interfaces | Data access abstraction |
| **Dependency Injection** | Constructor-based injection | Loose coupling & testability |
| **DTO** | Request/Response objects | API contract definition |
| **Singleton** | Spring beans default scope | Single instance per app |
| **Builder** | Lombok @AllArgsConstructor | Flexible object creation |
| **Factory** | CloudinaryStorageService | Encapsulated object creation |
| **Strategy** | Storage service abstraction | Swappable implementations |
| **JWT Token** | Stateless authentication | Scalable and secure |

---

## 🛠️ Tech Stack

### Backend
- **Framework**: Spring Boot 3.x
- **ORM**: Spring Data JPA / Hibernate
- **Database**: MySQL 8.0
- **Authentication**: Spring Security + JWT
- **File Storage**: Cloudinary
- **Build Tool**: Maven
- **Language**: Java 17+

### Frontend
- **Framework**: React 18.x
- **Build Tool**: Vite
- **HTTP Client**: Axios
- **State Management**: Context API + React Hooks
- **Routing**: React Router v6
- **Styling**: CSS3
- **Node Version**: 18+

### Browser Extension
- **Type**: Chrome Extension (Manifest v3)
- **Communication**: Background Script + Content Script
- **Storage**: Chrome Storage API

### DevOps
- **Version Control**: Git

---

