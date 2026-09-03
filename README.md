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


## My Contribution

This project was built by a team of 6 as part of a university coursework assignment simulating a pharmacy POS system for a fictional company ("Cosymed Ltd"). Contribution was equally distributed across the team, with all members collaborating across the full stack of the application — the JDBC-based data access layer (database/), the Swing GUI panels (Sales, Stock, Orders, Customer Accounts, Reporting), the domain model classes, and the JUnit test suite — rather than each person owning a single isolated module. Team members rotated across features and reviewed each other's code throughout development, so the final architecture and implementation reflect joint design decisions rather than individually siloed work.
