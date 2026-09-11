<<<<<<< HEAD
# 🍔 Campus Canteen - Food Pre-ordering System

A modern, responsive full-stack web application built in **Java (Spring Boot 3 + Thymeleaf)** that solves the problem of long, crowded college canteen queues during break hours.

Students can browse menus, customize dishes, choose their recess pickup slots, and receive **digital pickup tokens (e.g. `#TK-101`)** with live tracking. Canteen chefs and counter operators get a **Live Kitchen Display System (KDS)** to prepare orders on time and manage real-time item availability.

---

## 🚀 Quick Start (Run Locally)

The application uses an embedded **H2 database** with pre-seeded campus dishes and test accounts—**no external database installation is required**!

### 1. Open Terminal in the project directory:
```powershell
cd "c:\Users\MANEESH MOHANDAS\Documents\campus-canteen"
```

### 2. Start the Spring Boot Application:
Using the included Maven Wrapper:
```powershell
.\mvnw.cmd spring-boot:run
```
*(Or if you have Maven installed globally: `mvn spring-boot:run`)*

### 3. Open in Browser:
- **Student Canteen Portal**: [http://localhost:8080](http://localhost:8080)
- **Food Menu**: [http://localhost:8080/menu](http://localhost:8080/menu)
- **Kitchen & Staff Portal**: [http://localhost:8080/admin/orders](http://localhost:8080/admin/orders)
- **H2 Database Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:canteen_db`, User: `sa`, Password: *(leave blank)*)

---

## 🔑 Demo Accounts (Pre-seeded)

| Role | Username / ID | Password | Notes |
|---|---|---|---|
| **Student** | `CS101` | `password123` | Alex Morgan (CSE) - Pre-loaded with ₹500 wallet balance |
| **Student** | `EC202` | `password123` | Sneha Patel (ECE) - Pre-loaded with ₹350 wallet balance |
| **Kitchen Staff / Admin** | `admin` | `admin123` | Access to Kitchen Display Board & Menu Stock Manager |

*(You can also click the **"Fill Demo Student"** or **"Fill Demo Staff"** magic buttons on the login page for instant 1-click access!)*

---

## ✨ Features & Architecture

### 1. 🎓 Student Pre-ordering Portal
- **Menu & Discovery**: Filter by category (*Breakfast*, *Lunch*, *Snacks*, *Beverages*, *Specials*), Veg-Only filter, live dish search, preparation time estimates, and stock indicators.
- **Cart / Tray**: Adjust item quantities, view item subtotals, clear tray.
- **Recess Pickup Slot Scheduling**: Select exact break times (*10:45 AM Recess*, *1:15 PM Lunch*, *3:30 PM Tea Break*, or *Next 15 mins*) to skip queues.
- **Digital Token & Live Tracker**: Every order generates an express digital token (e.g. `#TK-101`) with live step-by-step progress tracking:
  $$\text{Received} \longrightarrow \text{Cooking in Kitchen} \longrightarrow \text{Ready for Pickup} \longrightarrow \text{Collected}$$
- **Student Wallet & Payment Options**: Pay at counter on pickup, UPI online mock, or instant campus wallet payment.
- **Order History**: Review previous meals and receipts anytime.

### 2. 👨‍🍳 Canteen Staff / Kitchen Display System (KDS)
- **Live Order Board**: Real-time Kanban board showing pending, preparing, and ready orders with 15-second auto-refresh.
- **One-Click Status Updates**:
  - `Start Cooking` ➔ changes status to **PREPARING** (student's phone updates live)
  - `Mark Ready for Counter Pickup` ➔ changes status to **READY_FOR_PICKUP**
  - `Student Picked Up` ➔ marks order **COMPLETED**
- **Live Stock Switch**: Instantly toggle dishes between **"In Stock"** and **"Sold Out"** with one click.
- **Add New Dishes**: Add new breakfast/lunch specials with prices and descriptions on the fly.
- **Kitchen Metrics**: Real-time counter of new orders, active cooking, ready items, and daily revenue.

---

## 🛠 Tech Stack

- **Backend**: Java 17+, Spring Boot 3.3.5
- **Modules**: Spring Web MVC, Spring Data JPA, Spring Validation
- **View Layer**: Thymeleaf, Bootstrap 5.3, Bootstrap Icons
- **Database**: H2 (Embedded In-Memory, default) / MySQL (Optional configurable in `application.properties`)
- **Build Tool**: Apache Maven (with `mvnw.cmd` wrapper)

---

## 🗄 Switching to MySQL (Optional)

If you prefer to use a local MySQL server instead of the embedded H2 database:
1. Open `src/main/resources/application.properties`.
2. Comment out the H2 lines.
3. Uncomment the MySQL lines and configure your MySQL username and password:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/campus_canteen?createDatabaseIfNotExist=true
   spring.datasource.username=root
   spring.datasource.password=your_password
   spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
   ```
4. Restart the application. Spring Boot will automatically create all tables and seed the initial dishes!
=======
# CampusCanteen
java project
>>>>>>> 111224e768659c32f1b4a538f0cb06a127b0f9b1
