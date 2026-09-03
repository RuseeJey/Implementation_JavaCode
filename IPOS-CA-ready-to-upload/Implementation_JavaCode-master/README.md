# IPOS-CA — Pharmacy Point-of-Sale & Business Management System

A desktop Point-of-Sale (POS) and business management application built in Java, developed as a coursework/team project for a fictional pharmacy retailer ("Cosymed Ltd"). It supports role-based logins for different staff types and covers sales processing, stock control, supplier orders, customer accounts, and reporting in a single Swing-based desktop application backed by a MySQL database.

## Overview

Independent pharmacies typically juggle several disconnected tools for sales, inventory, and customer management. IPOS-CA consolidates these into one application with a role-based interface, so administrators, managers, accountants, and clerks each see the functionality relevant to their job. The system is built around a layered architecture that separates the database access layer, business/domain logic, and the Swing GUI, with automated unit tests covering the core domain classes.

## Features

- **Role-based authentication** — a login screen authenticates against the database and grants access to different tabs depending on the user's role (Administrator, Manager, Senior Accountant, Accountant/Clerk).
- **Sales processing** — record customer sales through a dedicated Sales panel.
- **Stock management** — track inventory levels, with transactional stock deduction when orders are fulfilled (batched, all-or-nothing updates).
- **Supplier orders** — create and manage purchase orders, order items, and order status.
- **Customer accounts** — manage customer account records, with an automatic status engine that re-evaluates every customer account's status on each login.
- **Company accounts & user management** — administer company account details and manage system users and their roles.
- **Templates** — configure business/document settings (e.g. pharmacy identity details used across the app).
- **Reminders** — track and surface reminders relevant to day-to-day operations.
- **Reports** — generate reports on sales/orders/stock activity.
- **Auto-suggest input fields** — custom-built autocomplete/autosuggest support for text fields to speed up data entry across the app.

## Tech Stack

- **Language:** Java (Swing for the GUI)
- **Database:** MySQL, accessed via JDBC (`mysql-connector-j`)
- **UI components:** [SwingX](https://github.com/homebeaver/SwingX) for enhanced Swing widgets
- **Testing:** JUnit 5
- **IDE:** IntelliJ IDEA (project files included)

## Architecture

The codebase is organized into three layers plus a set of domain-model packages:

```
src/
├── database/          # Data access layer (one class per domain area)
│   ├── DatabaseManager.java
│   ├── SalesDB.java, OrdersDB.java, CustomerAccountDB.java
│   ├── MerchantDB.java, UserDB.java, TemplatesDB.java
│   └── RemindersDB.java, ReportsDB.java, LocalStockItemDB.java
├── gui/                # Presentation layer
│   ├── auth/           # LoginFrame
│   ├── main/            # MainFrame (role-aware tab shell)
│   ├── panels/          # One panel per feature area (Sales, Stock, Orders, ...)
│   ├── dialogs/          # Modal dialogs (e.g. flexible discounts)
│   └── util/              # AutoSuggestSupport, ReportPDFExporter
├── IPOS_CA_STOCK/      # Stock domain model + StockManager
├── IPOS_CA_ORD/        # Order domain model (OrderItem, SupplierOrder) + tests
├── IPOS_CA_CUST/       # Customer account domain model + tests
├── IPOS_CA_USER/       # System user domain model
└── Main.java           # Application entry point
```

This separation keeps the domain model (e.g. `OrderItem`, `CustomerAccount`) independent of both the database layer and the UI, which is what makes the domain classes straightforward to unit test in isolation.

## Getting Started

### Prerequisites

- JDK 17+ (or whichever version this was built/run against — update this if different)
- A running MySQL server
- IntelliJ IDEA (recommended, since project config files are included) or any Java IDE

### Setup

1. **Clone the repository**
   ```
   git clone https://github.com/<your-username>/<repo-name>.git
   ```
2. **Create the MySQL database.** Create a schema (default expected name: `ipos_local`). Some tables (e.g. users, merchants, templates) are created automatically on first run; you may need to create the remaining tables manually — see `database/*.java` for the exact schema each DAO class expects.
3. **Configure your database credentials.** Do **not** hardcode credentials — set them as environment variables (or a local, git-ignored `config.properties`) and read them in `DatabaseManager`, for example:
   ```
   DB_URL=jdbc:mysql://localhost:3306/ipos_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   DB_USER=your_username
   DB_PASSWORD=your_password
   ```
4. **Add the required libraries** (`lib/mysql-connector-j-9.6.0.jar`, `lib/swingx-all-1.6.5-1.jar`) to your project's classpath — in IntelliJ: *File → Project Structure → Modules → Dependencies*.
5. **Run** `src/Main.java`.

### Running Tests

Unit tests (JUnit 5) cover the core domain classes, e.g. `OrderItemTest` and `CustomerAccountTest`. Run them from your IDE's built-in test runner.

## My Contribution

This project was built by a team of 6 as part of a university coursework assignment. Work was shared equally across the team throughout the codebase — including the data access layer, Swing GUI panels, domain-model design, and unit testing — rather than being split into isolated individual modules, so all members contributed jointly to the architecture and implementation described above.

## Known Limitations / Future Improvements

- PDF export for reports is scaffolded (`ReportPDFExporter`) but not yet implemented.
- No Maven/Gradle build — dependencies are currently managed as checked-in JARs; migrating to a build tool would simplify setup.
- Database schema setup is currently partly manual; a bundled `schema.sql` would make first-time setup smoother.
