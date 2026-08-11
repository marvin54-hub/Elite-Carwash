# 🚗 Witbank Elite Car Wash — Management Platform

Full-stack car wash booking and management system.
**Spring Boot 3 · Thymeleaf · Spring Security · H2 / PostgreSQL**

---

## ⚡ Quick Start (Local)

### Requirements
- Java 17+ → [Download](https://adoptium.net/temurin/releases/?version=17)
- Maven 3.8+ → or use the included `./mvnw`

```bash
git clone https://github.com/YOUR_USERNAME/witbank-elite-carwash.git
cd witbank-elite-carwash
mvn spring-boot:run
```
Open → **http://localhost:8080**

### Default Credentials
| Role | URL | Username | Password |
|------|-----|----------|----------|
| Admin | `/staff/login` | `admin` | `admin123` |
| Staff | `/staff/login` | `staff1` | `staff123` |
| Customer | `/customer/register` | — | Register new |

> ⚠️ **Change passwords immediately** after first login via the Employees tab.

---

## 🐙 Deploy to GitHub

### 1. Create a repository
```bash
# Inside the project folder:
git init
git add .
git commit -m "Initial commit — Witbank Elite Car Wash"
```

### 2. Push to GitHub
```bash
git remote add origin https://github.com/YOUR_USERNAME/witbank-elite-carwash.git
git branch -M main
git push -u origin main
```

GitHub Actions will automatically **build and test** every push to `main`.

---

## 🚀 Deploy to Vercel

> Vercel hosts the app as a **Docker container**.
> A `Dockerfile` and `vercel.json` are already included.

### Step 1 — Install Vercel CLI
```bash
npm install -g vercel
```

### Step 2 — Login
```bash
vercel login
```

### Step 3 — Deploy
```bash
vercel --prod
```

### Step 4 — Set Environment Variables on Vercel
Go to **Vercel Dashboard → Your Project → Settings → Environment Variables** and add:

| Variable | Value | Required |
|----------|-------|----------|
| `PORT` | `8080` | ✅ |
| `BASE_URL` | `https://your-app.vercel.app` | ✅ |
| `YOCO_SECRET_KEY` | `sk_test_...` | Optional |
| `YOCO_WEBHOOK_SECRET` | `whsec_...` | Optional |
| `OPENWEATHER_API_KEY` | your key | Optional |
| `DATABASE_URL` | `jdbc:postgresql://...` | Optional (defaults to H2) |

### Step 5 — Redeploy after setting env vars
```bash
vercel --prod
```

---

## 🚂 Deploy to Railway (Recommended Alternative)

Railway natively supports Spring Boot with **persistent storage** — better than Vercel for apps with a file-based database.

### Steps
1. Go to [railway.app](https://railway.app) → **New Project → Deploy from GitHub Repo**
2. Select your repository
3. Railway auto-detects the `Dockerfile` and builds it
4. Add a **PostgreSQL** plugin from the Railway dashboard
5. Set environment variables (same table as above, plus Railway auto-sets `DATABASE_URL`)
6. Done — Railway gives you a public URL

---

## 🎨 Deploy to Render (Another Alternative)

1. Go to [render.com](https://render.com) → **New → Web Service**
2. Connect your GitHub repo
3. Set:
   - **Environment**: Docker
   - **Build Command**: *(auto from Dockerfile)*
   - **Start Command**: *(auto from Dockerfile)*
4. Add environment variables
5. Click **Deploy**

---

## ⚙️ Environment Variables Reference

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8080` | HTTP port |
| `BASE_URL` | `http://localhost:8080` | Public URL (for Yoco redirects) |
| `DATABASE_URL` | H2 file | JDBC URL — set to PostgreSQL for production |
| `DB_DRIVER` | `org.h2.Driver` | `org.postgresql.Driver` for PostgreSQL |
| `DB_USERNAME` | `sa` | Database username |
| `DB_PASSWORD` | *(blank)* | Database password |
| `DB_DIALECT` | H2Dialect | `PostgreSQLDialect` for PostgreSQL |
| `BAYS` | `2` | Concurrent wash bays |
| `YOCO_SECRET_KEY` | *(blank)* | Yoco payment key |
| `YOCO_WEBHOOK_SECRET` | *(blank)* | Yoco webhook secret |
| `OPENWEATHER_API_KEY` | *(blank)* | Weather widget key |
| `OPENWEATHER_CITY` | `Witbank,ZA` | City for weather |

---

## 🐳 Docker (Local)

```bash
# Build
docker build -t witbank-elite-carwash .

# Run
docker run -p 8080:8080 \
  -e BASE_URL=http://localhost:8080 \
  witbank-elite-carwash

# Open
http://localhost:8080
```

---

## 🗂️ Project Structure

```
src/main/java/com/witbank/carwash/
├── config/         SecurityConfig
├── controller/     Admin, Staff, Customer, Booking,
│                   Payment, Report, SqlConsole, Auth
├── dto/            BookingForm (validation)
├── model/          Booking, Customer, Staff, Vehicle,
│                   Feedback, Payment, ServicePackage,
│                   InventoryItem, NotificationLog, StaffSchedule
├── repository/     JPA repositories (10)
└── service/        BookingService, CustomerService, StaffService,
                    NotificationService, ReminderService,
                    QrCodeService, YocoService, WeatherService

src/main/resources/
├── templates/      13 Thymeleaf HTML templates
├── static/         CSS + JS
└── application.properties
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.2, Spring MVC, Spring Security |
| Database | H2 (dev) / PostgreSQL (production) |
| Frontend | Thymeleaf, HTML/CSS, Vanilla JS, Chart.js |
| Auth | BCrypt + HTTP Session + CSRF |
| Payments | Yoco Online Checkout |
| Email | EmailJS (client-side) |
| SMS | Simulated / Outbox Logged |
| QR Code | ZXing |
| Reports | OpenPDF + Apache POI (Excel) |
| Weather | Open-Meteo (no key needed on homepage) |
| Maps | Google Maps embed |
| Build | Maven 3.8+ / Docker |

---

## 🔧 Troubleshooting

| Error | Fix |
|-------|-----|
| `jakarta.persistence does not exist` | Install Java 17+ |
| `Port 8080 in use` | Add `server.port=8081` to `application.properties` |
| 500 on admin dashboard | Delete `./data/carwashdb.mv.db` to reset DB |
| Yoco button not showing | Set `YOCO_SECRET_KEY` env variable |
| Weather widget missing | Set `OPENWEATHER_API_KEY` env variable |

---

## 📄 Licence
MIT — free to use, modify, and distribute.
