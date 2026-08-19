import { Request, Response } from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { query } from '../db/db.js';
import { RegisterSchema, LoginSchema } from '../utils/validation.js';

export async function register(req: Request, res: Response) {
    try {
        const validation = RegisterSchema.safeParse(req.body);
        if (!validation.success) {
            return res.status(400).json({
                success: false,
                errors: validation.error.format()
            });
        }

        const { email, password, name } = validation.data;

        // Check if user already exists
        const existing = await query('SELECT id FROM users WHERE email = $1', [email.toLowerCase()]);
        if (existing.rows.length > 0) {
            return res.status(409).json({
                success: false,
                error: 'A user with this email address already exists'
            });
        }

        // Hash password
        const salt = await bcrypt.genSalt(10);
        const passwordHash = await bcrypt.hash(password, salt);

        // Insert user
        const result = await query(
            'INSERT INTO users (email, password_hash, name) VALUES ($1, $2, $3) RETURNING id, email, name, created_at',
            [email.toLowerCase(), passwordHash, name]
        );

        const user = result.rows[0];

        // Generate JWT
        const secret = process.env.JWT_SECRET || 'super_secret_jwt_key_smart_pantry_2026';
        const token = jwt.sign(
            { id: user.id, email: user.email, name: user.name },
            secret,
            { expiresIn: (process.env.JWT_EXPIRES_IN || '7d') as any }
        );

        return res.status(201).json({
            success: true,
            data: {
                user,
                token
            }
        });
    } catch (error) {
        console.error('Registration error:', error);
        return res.status(500).json({
            success: false,
            error: 'Internal server error during user registration'
        });
    }
}

export async function login(req: Request, res: Response) {
    try {
        const validation = LoginSchema.safeParse(req.body);
        if (!validation.success) {
            return res.status(400).json({
                success: false,
                errors: validation.error.format()
            });
        }

        const { email, password } = validation.data;

        const result = await query(
            'SELECT id, email, password_hash, name, created_at FROM users WHERE email = $1',
            [email.toLowerCase()]
        );

        if (result.rows.length === 0) {
            return res.status(401).json({
                success: false,
                error: 'Invalid email or password'
            });
        }

        const user = result.rows[0];
        const isMatch = await bcrypt.compare(password, user.password_hash);
        if (!isMatch) {
            return res.status(401).json({
                success: false,
                error: 'Invalid email or password'
            });
        }

        const secret = process.env.JWT_SECRET || 'super_secret_jwt_key_smart_pantry_2026';
        const token = jwt.sign(
            { id: user.id, email: user.email, name: user.name },
            secret,
            { expiresIn: (process.env.JWT_EXPIRES_IN || '7d') as any }
        );

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
    } catch (error) {
        console.error('Login error:', error);
        return res.status(500).json({
            success: false,
            error: 'Internal server error during user login'
        });
    }
}

export async function getProfile(req: Request, res: Response) {
    try {
        if (!req.user) {
            return res.status(401).json({ success: false, error: 'Unauthorized' });
        }

        const result = await query(
            'SELECT id, email, name, created_at FROM users WHERE id = $1',
            [req.user.id]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({ success: false, error: 'User not found' });
        }

        return res.status(200).json({
            success: true,
            data: result.rows[0]
        });
    } catch (error) {
        console.error('Profile fetch error:', error);
        return res.status(500).json({
            success: false,
            error: 'Failed to retrieve profile'
        });
    }
}
