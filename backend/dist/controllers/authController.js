"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.register = register;
exports.login = login;
exports.getProfile = getProfile;
const bcryptjs_1 = __importDefault(require("bcryptjs"));
const jsonwebtoken_1 = __importDefault(require("jsonwebtoken"));
const db_js_1 = require("../db/db.js");
const validation_js_1 = require("../utils/validation.js");
async function register(req, res) {
    try {
        const validation = validation_js_1.RegisterSchema.safeParse(req.body);
        if (!validation.success) {
            return res.status(400).json({
                success: false,
                errors: validation.error.format()
            });
        }
        const { email, password, name } = validation.data;
        // Check if user already exists
        const existing = await (0, db_js_1.query)('SELECT id FROM users WHERE email = $1', [email.toLowerCase()]);
        if (existing.rows.length > 0) {
            return res.status(409).json({
                success: false,
                error: 'A user with this email address already exists'
            });
        }
        // Hash password
        const salt = await bcryptjs_1.default.genSalt(10);
        const passwordHash = await bcryptjs_1.default.hash(password, salt);
        // Insert user
        const result = await (0, db_js_1.query)('INSERT INTO users (email, password_hash, name) VALUES ($1, $2, $3) RETURNING id, email, name, created_at', [email.toLowerCase(), passwordHash, name]);
        const user = result.rows[0];
        // Generate JWT
        const secret = process.env.JWT_SECRET || 'super_secret_jwt_key_smart_pantry_2026';
        const token = jsonwebtoken_1.default.sign({ id: user.id, email: user.email, name: user.name }, secret, { expiresIn: (process.env.JWT_EXPIRES_IN || '7d') });
        return res.status(201).json({
            success: true,
            data: {
                user,
                token
            }
        });
    }
    catch (error) {
        console.error('Registration error:', error);
        return res.status(500).json({
            success: false,
            error: 'Internal server error during user registration'
        });
    }
}
async function login(req, res) {
    try {
        const validation = validation_js_1.LoginSchema.safeParse(req.body);
        if (!validation.success) {
            return res.status(400).json({
                success: false,
                errors: validation.error.format()
            });
        }
        const { email, password } = validation.data;
        const result = await (0, db_js_1.query)('SELECT id, email, password_hash, name, created_at FROM users WHERE email = $1', [email.toLowerCase()]);
        if (result.rows.length === 0) {
            return res.status(401).json({
                success: false,
                error: 'Invalid email or password'
            });
        }
        const user = result.rows[0];
        const isMatch = await bcryptjs_1.default.compare(password, user.password_hash);
        if (!isMatch) {
            return res.status(401).json({
                success: false,
                error: 'Invalid email or password'
            });
        }
        const secret = process.env.JWT_SECRET || 'super_secret_jwt_key_smart_pantry_2026';
        const token = jsonwebtoken_1.default.sign({ id: user.id, email: user.email, name: user.name }, secret, { expiresIn: (process.env.JWT_EXPIRES_IN || '7d') });
        return res.status(200).json({
            success: true,
            data: {
                user: {
                    id: user.id,
                    email: user.email,
                    name: user.name,
                    created_at: user.created_at
                },
                token
            }
        });
    }
    catch (error) {
        console.error('Login error:', error);
        return res.status(500).json({
            success: false,
            error: 'Internal server error during user login'
        });
    }
}
async function getProfile(req, res) {
    try {
        if (!req.user) {
            return res.status(401).json({ success: false, error: 'Unauthorized' });
        }
        const result = await (0, db_js_1.query)('SELECT id, email, name, created_at FROM users WHERE id = $1', [req.user.id]);
        if (result.rows.length === 0) {
            return res.status(404).json({ success: false, error: 'User not found' });
        }
        return res.status(200).json({
            success: true,
            data: result.rows[0]
        });
    }
    catch (error) {
        console.error('Profile fetch error:', error);
        return res.status(500).json({
            success: false,
            error: 'Failed to retrieve profile'
        });
    }
}
