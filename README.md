#  Easybox Food Locker Platform

A cloud-hosted, secure, and scalable smart locker system designed for bakery order deliveries. This platform enables clients to reserve temperature-compatible compartments in advance, bakeries to manage deliveries flexibly, and administrators to supervise everything from a centralized dashboard.

---

## What It Does

- Allows customers to pick up bakery orders from smart lockers ("Easyboxes") at precise times.
- Supports secure, temperature-verified reservation of locker compartments.
- Ensures hygiene with automatic flagging and cleaning workflows.
- Enables bakeries to confirm orders, place deliveries, and report locker issues.
- Provides a responsive admin dashboard for overseeing devices, orders, and issues.
- Uses QR codes for pickup, with real-time backend-device communication via MQTT.

---

## System Architecture

### Modular Monolith Backend (Spring Boot)

- Built with **Spring WebFlux** for reactive, non-blocking behavior.
- Structured into logical modules: `Reservations`, `Users`, `Devices`, `Security`, `Admin`.
- Communicates over **HTTPS** with web and mobile clients.
- Communicates over **MQTT** with Easybox devices using signed JWTs.
- Hosted on **Render**, deployed via Docker.

### Client Interfaces

- **Mobile App (Expo Router)**  
  - Supports `Client`, `Bakery`, and `Admin` roles.
  - Built using **React Native + Expo**.
  - Secure login, order tracking, issue reporting, QR code scanning.

- **Admin Dashboard (React + Netlify)**  
  - View Easybox status, manage bakeries and reservations.
  - Secure RS256 JWT authentication.
  - Deployed to a custom domain via **Netlify + Namecheap**.

- **Checkout Widget (React iFrame Plugin)**  
  - Embeds in bakery sites.
  - Allows clients to pick delivery time/location.
  - Displays real-time Easybox availability and handles pre-checkout reservation.

### Raspberry Pi Easybox Device

- Spring Boot app on Raspberry Pi.
- Registers via REST + fallback HS256 JWT, then connects over **MQTT with per-device JWT**.
- Subscribes to commands (`open-locker`, `confirm-placement`) via **HiveMQ Cloud Broker**.
- Handles QR scanning → verification → lock opening → user confirmation → state update.
- Loads compartment config from local JSON.

---

## Design Principles and Patterns

- **Vertical Slice Architecture**: Features are implemented end-to-end across all layers.
- **Modular Monolith**: Faster development, easier debugging, better cohesion.
- **Command Pattern**: Used in device command dispatch via MQTT.
- **MVC**: Applied to both device and backend layers.
- **SRP & SoC**: Each service/module does one thing well.
- **Security by Design**: JWT auth + TLS for all communication.
- **Immutable DTOs**: Ensures safe and consistent data exchange.

---

## Security Architecture

- **Frontend**: RS256 JWTs, signed with private key, verified via public key.
- **Devices**: HS256 JWTs, signed with per-device secret.
- **Transport**: All traffic encrypted via TLS (HTTPS, MQTT over TLS).
- **JWT Verification**:
  - Web/Mobile clients → Backend: RS256.
  - Devices → Backend (MQTT): HS256.

---

## Reservation Lifecycle

1. **Widget** sends availability request (address, time, temp, size).
2. **Backend** checks geolocation, Easybox availability, compatibility.
3. **Soft Lock**: Pending reservation is created after user selection.
4. **Hard Confirmation**: Order confirmed, QR generated.
5. **Device** receives and validates QR scan.
6. **Compartment opened**, user confirms placement or pickup.
7. **Reservation/Compartment status updated** accordingly.

---

## Compartment States

- `FREE` → `BUSY` (after order placed)
- `BUSY` → `FREE` (after pickup)
- `EXPIRED` → `DIRTY`
- Manual reports: `BROKEN` or `DIRTY`
- Admin/Cleaner resolves and resets state

---

## Deployment & Infrastructure

- **Backend**: Spring Boot + WebFlux, Dockerized, hosted on **Render**
- **Frontend** (Dashboard & Widget): React apps deployed on **Netlify**
- **Mobile App**: React Native + Expo, dynamic OTA bundles
- **Devices**: Spring Boot apps on **Raspberry Pi**, connected to **HiveMQ Cloud**
- **Database**: PostgreSQL on Render
- **MQTT Broker**: HiveMQ Cloud (AWS)

---

## Key Screens and Diagrams

- Architecture Diagram
![image](https://github.com/user-attachments/assets/7093c6e4-2908-4b6e-807d-eb16ece89ae8)

![image](https://github.com/user-attachments/assets/bdc5af93-ce7b-4f81-95f2-cf60ce75f607)

- Reservation Status
![image](https://github.com/user-attachments/assets/1f402323-4013-4a3e-b11f-b1ff99f000c7)

- Security Architecture
![image](https://github.com/user-attachments/assets/ff1cd619-cb06-45d7-accc-1f896f3b46df)

- QR Scan Flow
  
![image](https://github.com/user-attachments/assets/c7d03400-950a-4b54-9317-a1247c180ea9)


---

## 🛠 Technologies Used

- **Backend**: Spring Boot, WebFlux, R2DBC, PostgreSQL, Docker
- **MQTT Broker**: HiveMQ Cloud
- **Frontend**: React, Netlify, JWT Auth
- **Mobile App**: React Native + Expo Router v2
- **Device**: Spring Boot (on Raspberry Pi), JWT + MQTT
- **DevOps**: GitHub, Render, Netlify, Expo, Namecheap (DNS)

---

## Features Summary

| Feature                             | Supported |
|-------------------------------------|-----------|
| Advanced reservation                | yes       |
| Size & temperature compatibility    | yes        |
| QR-based secure pickup              | yes        |
| Cleaning workflows                  | yes        |
| Mobile + Web + Device integration   | yes        |
| Real-time locker state management   | yes        |
| Role-based access and tokens        | yes        |
| TLS + JWT security                  | yes        |

---

## Contact

This system was developed as part of a thesis project to modernize and digitize food delivery for local bakeries using cloud infrastructure, IoT devices, and secure mobile/web interfaces.

For more information or collaboration, contact the project author.
