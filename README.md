# Easybox Food Locker Platform

## Project Overview

Easybox is a cloud-hosted, secure, and scalable smart locker system designed for bakery order deliveries. It enables clients to reserve temperature-compatible compartments in advance, bakeries to manage deliveries, and administrators to supervise the system from a centralized dashboard.

This project was developed as part of a Bachelor's thesis and is hosted publicly at:

Repository: https://github.com/anisiaap/Easybox

Compiled binaries are excluded from the repository.

---

## Application Components

### Backend (Spring Boot + WebFlux)

Implements a reactive API for managing users, reservations, compartments, devices, and security. It uses PostgreSQL for persistence, R2DBC for non-blocking database access, and Docker for deployment. The backend is deployed to Render at:

https://api.easybox-food.xyz

### Admin Dashboard

A React-based single-page application allowing administrators to view and manage Easyboxes, orders, users, and locker status. It uses RS256 JWT authentication and is deployed on Netlify at:

https://admin.easybox-food.xyz

### Widget

A React application embedded via iframe in bakery checkout sites. It allows clients to choose a compatible Easybox based on delivery preferences. The widget receives bakery identity via JWT and communicates reservation data using `postMessage`.

Deployed at: https://widget.easybox-food.xyz

### Mobile Application

Built with Expo Router v2 (React Native), the mobile app supports three roles: Client, Bakery, and Maintenance. Each user accesses a role-specific interface. The app is currently run locally using Expo:

```

npx expo run

```

### Raspberry Pi Device Application

A Spring Boot application that runs on Raspberry Pi. It communicates with the backend over MQTT via HiveMQ Cloud. After registration, the device subscribes to MQTT topics for commands such as `request-compartments` and QR scan verification. It opens compartments based on the scanned QR and handles cleaning or pickup confirmation.

---

## Build and Installation Steps

To build and run each component locally:

### Backend

```

cd backend
mvn clean install
mvn spring-boot\:run

```

### Admin Dashboard

```

cd admin-dashboard
npm install
npm run build

```

### Widget

```

cd widget
npm install
npm run build

```

### Mobile App

```

cd mobile
npx expo install
npx expo run

```

### Device App (on Raspberry Pi)

```

cd ondevice
mvn clean install
mvn spring-boot\:run -Dspring-boot.run.arguments="--spring.main.headless=true"

````

The device must have GPIO pins, camera, and display properly configured. Compartments and GPIO pins are loaded from local configuration files.

---

## Device Configuration Files

Example content for required JSON configuration files:

### `device-config.json`

```json
{
  "compartments": [
    { "id": 1, "size": 10, "temperature": 4, "status": "free", "condition": "good" },
    { "id": 2, "size": 15, "temperature": 8, "status": "free", "condition": "good" },
    { "id": 3, "size": 20, "temperature": 12, "status": "free", "condition": "good" }
  ]
}
````

### `pin-mapping.json`

```json
{
  "mappings": [
    { "compartmentId": 1, "pin": 17 },
    { "compartmentId": 2, "pin": 27 },
    { "compartmentId": 3, "pin": 22 }
  ]
}
```

---

## Device Application Properties

Example `application.properties` content:

```
server.port=8081
server.address=0.0.0.0
spring.lifecycle.timeout-per-shutdown-phase=30s

central.backend.url=https://api.easybox-food.xyz/api/
device.address=Strada Alion, nr. 1, Drobeta Turnu Severin

mqtt.broker-url=ssl://7f5c04cad0974691b00ef4fb124be7a1.s1.eu.hivemq.cloud:8883
mqtt.username=deviceUser
mqtt.password=deviceUser1
mqtt.client-id=easybox-190
mqtt.topic-prefix=easybox
device.id=easybox-190

jwt.device-secret=keykeykeykeykeykeykeykeykeykeykeykeykey
spring.main.headless=false
```

---

## Bakery Site Integration (Widget)

A widget can be embedded in any HTML-based bakery checkout page. A working example is provided in the `bakery-test` folder.

To use the widget:

1. Open `bakery-test/index.html` in a browser.
2. Register as a bakery in the mobile app and copy the generated JWT token.
3. Replace the value of `window.env.JWT_TOKEN` in the HTML with the bakery token.
4. Select delivery details and use the widget to reserve an Easybox.
5. Confirm the reservation by pressing "Place Order".

The widget communicates the reservation status to the backend and generates a QR code accessible via the client account.

---

## Application Flow Summary

1. The bakery registers and receives a token.
2. The token is injected into the widget for identity verification.
3. A client places an order using the bakery site.
4. The widget makes a reservation and receives a QR code.
5. The client uses the QR to access the locker on the Raspberry Pi device.
6. The compartment opens, and the status is updated after confirmation.
7. The admin can monitor and manage all activity from the dashboard.

---

## Admin Login Credentials

To access the admin dashboard:

* Username: `admin`
* Password: `Admin123`

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

## Technologies Used

- **Backend**: Spring Boot, WebFlux, R2DBC, PostgreSQL, Docker
- **MQTT Broker**: HiveMQ Cloud
- **Frontend**: React, Netlify, JWT Auth
- **Mobile App**: React Native + Expo Router v2
- **Device**: Spring Boot (on Raspberry Pi), JWT + MQTT
- **DevOps**: GitHub, Render, Netlify, Expo, Namecheap (DNS)

---

## Contact

This system was developed as part of a thesis project to modernize and digitize food delivery for local bakeries using cloud infrastructure, IoT devices, and secure mobile/web interfaces.

For more information or collaboration, contact the project author.
