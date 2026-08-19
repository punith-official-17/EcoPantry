-- Enable UUID generation extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Define Enums
DO $$ BEGIN
    CREATE TYPE item_category AS ENUM ('produce', 'dairy', 'meat', 'bakery', 'pantry', 'other');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE item_status AS ENUM ('active', 'consumed', 'expired');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Items Table (Pantry Inventory)
CREATE TABLE IF NOT EXISTS items (
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

-- 3. Recipes Table (AI-generated zero-waste suggestions)
CREATE TABLE IF NOT EXISTS recipes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    ingredients JSONB NOT NULL DEFAULT '[]'::jsonb,
    instructions TEXT NOT NULL,
    prep_time VARCHAR(100) NOT NULL DEFAULT '20 mins',
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indices for fast lookups
CREATE INDEX IF NOT EXISTS idx_items_user_status ON items(user_id, status);
CREATE INDEX IF NOT EXISTS idx_items_expiry ON items(user_id, expiry_date ASC);
CREATE INDEX IF NOT EXISTS idx_recipes_user ON recipes(user_id, generated_at DESC);
