<div align="center">
  <img src="./app/src/main/res/drawable/wpc_logo.png" width="160" alt="WPC Logo">
</div>

# WPC Broadsheet Manager — Android App

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3+-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-2026+-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material3](https://img.shields.io/badge/Material_3-Modern-757575?logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![Room](https://img.shields.io/badge/Room_DB-Local_Persistence-3DDC84?logo=android&logoColor=white)](https://developer.android.com/training/data-storage/room)

A cutting-edge Android application designed to replace the legacy Excel-based broadsheet system for **Western Province Caterers**. Built with a focus on speed, reliability, and a premium user experience.

---

## Features

- **Real-time Billing**: Instant calculation of meal counts, VAT, and final totals as you type, mirroring Excel broadsheet formulas exactly.
- **Multi-Site Management**: Effortlessly switch between different catering units with per-site context.
- **Resident Management**: Full CRUD operations for managing residents and their meal plans, with a complete audit trail.
- **Advanced Reporting**: Visual dashboards, per-site and consolidated reports, and monthly history tracking.
- **Export Engine**: Export reports to CSV (Excel-compatible) and PDF, with direct email sharing to unit managers. Supports both per-site and consolidated all-sites exports.
- **Offline First**: Robust local storage using **Room Database** (v5 schema) with background sync capabilities.
- **User & Role Management**: Admin panel for managing users and sites. Role-based access (Admin, Operations Manager, Unit Manager).
- **Profile & Avatar**: Full profile editing with avatar capture and crop screen.
- **Theming**: Premium dark-mode first design using **Syne** and **DM Sans** typography, with a user-configurable appearance screen.
- **Smart Reminders**: Integrated **WorkManager** and **BootReceiver** for capture reminders and background tasks.

---

## Tech Stack

- **UI**: Jetpack Compose (100% Declarative UI)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Navigation**: Compose Navigation with Type-Safe Routes
- **Database**: Room Persistence Library (v5, with full migration history)
- **Local Storage**: Preferences DataStore (User Settings, Theme, Notifications)
- **Serialization**: Gson (meal count JSON in Room)
- **Networking**: Retrofit 3.0 + OkHttp 5.0 (scaffolded and ready for API integration)
- **Dependency Management**: Gradle Version Catalog (`libs.versions.toml`)
- **Image Loading**: Coil-Compose
- **Background Tasks**: WorkManager + BootReceiver

---

## Project Architecture

```
WPCBroadsheet/
├── app/src/main/java/com/hildebrandtdigital/wpcbroadsheet/
│   ├── data/
│   │   ├── db/              ← Room Database, DAOs & Entities (v5 schema)
│   │   ├── model/           ← Type-safe Data Classes & Enums
│   │   ├── network/         ← Retrofit API interfaces, OkHttp client, AuthTokenStore
│   │   └── repository/      ← Single source of truth (Meal, Resident, Site, User repos)
│   │                           BillingCalculator, ExportManager, AvatarManager,
│   │                           AppSession, ThemePreferences, NotificationPreferences
│   ├── navigation/          ← NavHost & Route definitions
│   ├── ui/
│   │   ├── components/      ← Reusable UI components (BottomNav, ScreenHeader, Avatar)
│   │   ├── screens/         ← Full-page Composable screens (see list below)
│   │   └── theme/           ← Material3 Design Tokens (Colors, Typography, Theme)
│   └── workers/             ← CaptureReminderWorker, BootReceiver
```

### Screens

| Screen | Description |
| :--- | :--- |
| `SplashScreen` | Animated launch screen |
| `LoginScreen` | Email + password auth against local Room users |
| `DashboardScreen` | Overview of all sites with summary metrics |
| `CaptureScreen` | Daily meal count entry per resident |
| `ReportsScreen` | Monthly billing report with export (CSV / PDF / Email) |
| `ResidentManagementScreen` | Add / edit / deactivate residents per site |
| `SiteManagementScreen` | Add / edit catering sites |
| `MonthlyHistoryScreen` | Browse historical monthly reports |
| `PricingConfigScreen` | Configure per-site meal prices and VAT |
| `ProfileScreen` | User profile hub with settings navigation |
| `EditProfileScreen` | Edit name, email, phone |
| `AvatarCropScreen` | Crop and set profile avatar |
| `AppearanceScreen` | Theme and display preferences |
| `NotificationsScreen` | Capture reminder preferences |
| `AdminScreen` | Admin-only user and site management panel |

---

## Getting Started

### Prerequisites
- Android Studio Meerkat (or newer)
- JDK 21
- Android SDK 36 (Compile/Target), min SDK 26

### Installation
1. Clone the repository.
2. Open in Android Studio.
3. Add the required fonts to `res/font/` (Syne & DM Sans).
4. Build and run on an emulator or device (API 26+).

### Default Seed Accounts

The database is seeded automatically on first launch. Default password for all accounts is `wpc2026`.

| Email | Role |
| :--- | :--- |
| `admin@wpc.co.za` | Admin |
| `chernay@wpc.co.za` | Operations Manager (all sites) |
| `vanrooyen@wpc.co.za` | Unit Manager (Lizane Village) |
| `nothnagel@wpc.co.za` | Unit Manager (Bakkies Estate) |

---

## Design Tokens

| Token | Color | Usage |
| :--- | :--- | :--- |
| `BgDeep` | `#0B0F1A` | Primary Background |
| `Primary` | `#4F8EF7` | Actionable Elements (Blue) |
| `Secondary` | `#F7A84F` | Highlights & Revenue (Gold) |
| `Accent` | `#4FF7C8` | Success & Totals (Teal) |
| `Danger` | `#F74F6B` | Errors & Deductions (Red) |

---

## Roadmap

- [x] Room Database Implementation (v5 schema with full migrations)
- [x] Multi-site Navigation
- [x] Resident Management UI (full CRUD + audit trail)
- [x] Profile & Avatar Customization (with crop screen)
- [x] Billing Calculator (exact Excel formula parity)
- [x] CSV & PDF Export Engine (per-site and consolidated)
- [x] Email Report Sharing
- [x] Admin Panel (user & site management)
- [x] WorkManager Capture Reminders
- [ ] Firebase / Supabase Cloud Sync (Retrofit layer scaffolded)
- [ ] Push Notifications Integration

---

---

<div align="center">
  <img src="./mrhdigital-logo.png" width="200" alt="Mr. H Digital Logo">
  <br>
  <em>Crafted with love by <strong>Mr. H Digital</strong> — <a href="https://mrhdigital.co.za">mrhdigital.co.za</a></em>
</div>
