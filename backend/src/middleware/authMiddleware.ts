import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';

export interface AuthUser {
    id: string;
    email: string;
    name: string;
}

declare global {
    namespace Express {
        interface Request {
            user?: AuthUser;
        }
    }
}

export const authenticateToken = (req: Request, res: Response, next: NextFunction) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.split(' ')[1] : null;

    if (!token) {
        return res.status(401).json({
            success: false,
            error: 'Authentication token missing. Please provide Authorization: Bearer <token>'
        });
    }

    const secret = process.env.JWT_SECRET || 'super_secret_jwt_key_smart_pantry_2026';

    try {
        const decoded = jwt.verify(token, secret) as AuthUser;
        req.user = decoded;
        next();
    } catch (err) {
        return res.status(403).json({
            success: false,
            error: 'Invalid or expired authentication token'
        });
    }
};
