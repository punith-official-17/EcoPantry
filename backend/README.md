# Smart Pantry & Food Waste Reducer — Backend API

Production-ready backend service for the Smart Pantry and Food Waste Reducer application. Built with **Node.js, Express, TypeScript, PostgreSQL**, and powered by **Google Gemini 1.5 Flash** for intelligent receipt OCR and zero-waste recipe synthesis.

---

## 🛠 Tech Stack

- **Runtime & Language**: Node.js & TypeScript
- **Web Framework**: Express.js
- **Database**: PostgreSQL (or Supabase / Neon / Cloud SQL) with connection pooling (`pg`)
- **AI Engine**: Google Gemini 1.5 Flash (`@google/genai`) for structured Vision OCR and recipe generation
- **Authentication**: JWT (`jsonwebtoken`) with `bcryptjs` password hashing
- **Validation**: Zod for type-safe schema verification
- **Scheduler**: `node-cron` for daily 08:00 AM expiration alerts

---

## 📋 Database Schema & Relational Model

```sql
-- 1. Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Items (Pantry Inventory)
CREATE TYPE item_category AS ENUM ('produce', 'dairy', 'meat', 'bakery', 'pantry', 'other');
CREATE TYPE item_status AS ENUM ('active', 'consumed', 'expired');

CREATE TABLE items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    category item_category NOT NULL DEFAULT 'other',
    quantity VARCHAR(100) NOT NULL DEFAULT '1',
    purchase_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expiry_date DATE NOT NULL,
    status item_status NOT NULL DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Recipes (AI Zero-Waste Recipes)
CREATE TABLE recipes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    ingredients JSONB NOT NULL DEFAULT '[]'::jsonb,
    instructions TEXT NOT NULL,
    prep_time VARCHAR(100) NOT NULL DEFAULT '20 mins',
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🚀 Quickstart & Setup

### 1. Configure Environment Variables
Copy `.env.example` to `.env` and fill in your PostgreSQL URL and Gemini API Key:

```bash
PORT=5000
DATABASE_URL=postgresql://postgres:password@localhost:5432/smart_pantry
JWT_SECRET=your_secure_jwt_secret_key_here
GEMINI_API_KEY=AIzaSy...
```

### 2. Install Dependencies & Initialize Database
```bash
npm install
npm run db:init
```

### 3. Run in Development Mode
```bash
npm run dev
```

---

## 📡 API Reference & Endpoints

### 🔐 1. Authentication
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register new user with email, name, and password | No |
| `POST` | `/api/auth/login` | Login and receive Bearer JWT token | No |
| `GET` | `/api/auth/me` | Fetch authenticated user profile | Yes (Bearer) |

#### Register Request
```json
POST /api/auth/register
{
  "email": "sarah@example.com",
  "password": "SecurePassword123!",
  "name": "Sarah Jenkins"
}
```

#### Response
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "c71a39f6-189f-431e-9271-bf0eb9827a58",
      "email": "sarah@example.com",
      "name": "Sarah Jenkins",
      "created_at": "2026-08-18T23:30:00.000Z"
    },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

---

### 📦 2. Inventory Management
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/items` | List user's active pantry items (sorted by `expiry_date ASC`) | Yes |
| `POST` | `/api/items` | Add a single pantry item manually | Yes |
| `PUT` | `/api/items/:id` | Update quantity, status, expiry date, or category | Yes |
| `DELETE` | `/api/items/:id` | Soft-delete (mark `consumed`) or hard-delete (`?permanent=true`) | Yes |

#### Add Item Request
```json
POST /api/items
Authorization: Bearer <TOKEN>
{
  "name": "Greek Yogurt",
  "category": "dairy",
  "quantity": "500g tub",
  "expiry_date": "2026-08-25"
}
```

---

### ✨ 3. AI Receipt & Image Processing
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/ai/scan-receipt` | Upload image (`multipart/form-data` or base64 JSON), extracts items via Gemini 1.5 Flash, estimates shelf life, and bulk saves to items table | Yes |

#### Request via Base64 JSON
```json
POST /api/ai/scan-receipt
Authorization: Bearer <TOKEN>
{
  "imageBase64": "/9j/4AAQSkZJRgABAQEASABIAAD...",
  "mimeType": "image/jpeg"
}
```

#### Response
```json
{
  "success": true,
  "message": "Successfully extracted and saved 3 pantry items from receipt.",
  "count": 3,
  "data": [
    {
      "id": "e9bf6d91-4467-4e68-98e3-479cfda1adcb",
      "name": "Fresh Baby Spinach",
      "category": "produce",
      "quantity": "1 bag (200g)",
      "purchase_date": "2026-08-18",
      "expiry_date": "2026-08-22",
      "days_until_expiry": 4,
      "status": "active"
    }
  ]
}
```

---

### 🍳 4. AI Zero-Waste Recipe Suggestion
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/ai/suggest-recipes` | Queries items expiring in <=3 days, prompts Gemini 1.5 Flash to formulate 2-3 custom rescue recipes, saves to database, and returns recipes | Yes |

#### Request
```json
POST /api/ai/suggest-recipes
Authorization: Bearer <TOKEN>
{
  "expiringWithinDays": 3
}
```

#### Response
```json
{
  "success": true,
  "message": "Generated 2 zero-waste recipes using your expiring ingredients.",
  "count": 2,
  "expiring_ingredients_used": ["Fresh Baby Spinach", "Salmon Fillet"],
  "data": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "title": "Pan-Seared Salmon with Wilted Garlic Spinach",
      "ingredients": [
        "2 Salmon Fillets (Expiring soon)",
        "200g Fresh Baby Spinach (Expiring soon)",
        "2 cloves Garlic (minced)",
        "1 tbsp Olive Oil",
        "Salt & Lemon Juice to taste"
      ],
      "instructions": "1. Season salmon fillets with salt and pepper.\n2. Heat olive oil in a skillet over medium-high heat and sear salmon for 4 minutes per side.\n3. Add garlic and spinach to the remaining pan juices, tossing for 90 seconds until wilted.\n4. Plate salmon over spinach and drizzle with lemon juice.",
      "prep_time": "15 mins",
      "generated_at": "2026-08-18T23:30:00.000Z"
    }
  ]
}
```

---

### ⏰ 5. Expiry Notifications & Scheduled Cron
- **Cron Pattern**: `0 8 * * *` (Fires every morning at 08:00 AM server time).
- **Function**: Automatically finds active items expiring within the next 24 to 48 hours and triggers consolidated user alerts.
- **Manual Trigger Endpoint**: `POST /api/notifications/run-cron` (or `GET /api/notifications/alerts` for currently logged-in user).

---

## 🐳 Docker Deployment

A `Dockerfile` is included for instant container deployment to Google Cloud Run, AWS ECS, Render, or Railway:

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json tsconfig.json ./
RUN npm ci
COPY src/ ./src/
RUN npm run build

FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY package*.json ./
RUN npm ci --only=production
COPY --from=builder /app/dist ./dist
COPY src/db/schema.sql ./dist/db/schema.sql
EXPOSE 5000
CMD ["node", "dist/server.js"]
```
