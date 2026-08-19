import { Request, Response } from 'express';
import { query } from '../db/db.js';
import { CreateItemSchema, UpdateItemSchema } from '../utils/validation.js';

export async function getItems(req: Request, res: Response) {
    try {
        const userId = req.user!.id;
        const { status = 'active', category, sort = 'expiry_asc' } = req.query;

        let sql = `
            SELECT 
                id, 
                user_id, 
                name, 
                category, 
                quantity, 
                purchase_date, 
                expiry_date, 
                status, 
                created_at,
                (expiry_date - CURRENT_DATE) AS days_until_expiry,
                CASE 
                    WHEN expiry_date < CURRENT_DATE THEN 'expired'
                    WHEN (expiry_date - CURRENT_DATE) <= 3 THEN 'expiring_soon'
                    ELSE 'fresh'
                END AS freshness_status
            FROM items
            WHERE user_id = $1
        `;

        const params: any[] = [userId];

        if (status !== 'all') {
            params.push(status);
            sql += ` AND status = $${params.length}`;
        }

        if (category) {
            params.push(category);
            sql += ` AND category = $${params.length}`;
        }

        // Sorting
        switch (sort) {
            case 'expiry_desc':
                sql += ' ORDER BY expiry_date DESC, name ASC';
                break;
            case 'name_asc':
                sql += ' ORDER BY name ASC';
                break;
            case 'created_desc':
                sql += ' ORDER BY created_at DESC';
                break;
            case 'expiry_asc':
            default:
                sql += ' ORDER BY expiry_date ASC, name ASC';
                break;
        }

        const result = await query(sql, params);

        return res.status(200).json({
            success: true,
            count: result.rows.length,
            data: result.rows
        });
    } catch (error) {
        console.error('Fetch items error:', error);
        return res.status(500).json({
            success: false,
            error: 'Failed to fetch inventory items'
        });
    }
}

export async function createItem(req: Request, res: Response) {
    try {
        const userId = req.user!.id;
        const validation = CreateItemSchema.safeParse(req.body);

        if (!validation.success) {
            return res.status(400).json({
                success: false,
                errors: validation.error.format()
            });
        }

        const { name, category, quantity, purchase_date, expiry_date, status } = validation.data;
        const purchaseDateValue = purchase_date || new Date().toISOString().split('T')[0];

        const sql = `
            INSERT INTO items (user_id, name, category, quantity, purchase_date, expiry_date, status)
            VALUES ($1, $2, $3, $4, $5, $6, $7)
            RETURNING *, (expiry_date - CURRENT_DATE) AS days_until_expiry
        `;

        const result = await query(sql, [
            userId,
            name,
            category,
            quantity,
            purchaseDateValue,
            expiry_date,
            status
        ]);

        return res.status(201).json({
            success: true,
            data: result.rows[0]
        });
    } catch (error) {
        console.error('Create item error:', error);
        return res.status(500).json({
            success: false,
            error: 'Failed to add item to inventory'
        });
    }
}

export async function updateItem(req: Request, res: Response) {
    try {
        const userId = req.user!.id;
        const itemId = req.params.id;

        const validation = UpdateItemSchema.safeParse(req.body);
        if (!validation.success) {
            return res.status(400).json({
                success: false,
                errors: validation.error.format()
            });
        }

        // Verify ownership
        const check = await query('SELECT id FROM items WHERE id = $1 AND user_id = $2', [itemId, userId]);
        if (check.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Item not found or you do not have permission to modify it'
            });
        }

        const fields: string[] = [];
        const values: any[] = [];

        Object.entries(validation.data).forEach(([key, val]) => {
            if (val !== undefined) {
                values.push(val);
                fields.push(`${key} = $${values.length}`);
            }
        });

        if (fields.length === 0) {
            return res.status(400).json({
                success: false,
                error: 'No valid update fields provided'
            });
        }

        values.push(itemId, userId);
        const sql = `
            UPDATE items
            SET ${fields.join(', ')}
            WHERE id = $${values.length - 1} AND user_id = $${values.length}
            RETURNING *, (expiry_date - CURRENT_DATE) AS days_until_expiry
        `;

        const result = await query(sql, values);

        return res.status(200).json({
            success: true,
            data: result.rows[0]
        });
    } catch (error) {
        console.error('Update item error:', error);
        return res.status(500).json({
            success: false,
            error: 'Failed to update item'
        });
    }
}

export async function deleteItem(req: Request, res: Response) {
    try {
        const userId = req.user!.id;
        const itemId = req.params.id;
        const permanent = req.query.permanent === 'true';

        // Check ownership
        const check = await query('SELECT id FROM items WHERE id = $1 AND user_id = $2', [itemId, userId]);
        if (check.rows.length === 0) {
            return res.status(404).json({
                success: false,
                error: 'Item not found'
            });
        }

        if (permanent) {
            await query('DELETE FROM items WHERE id = $1 AND user_id = $2', [itemId, userId]);
            return res.status(200).json({
                success: true,
                message: 'Item permanently deleted'
            });
        } else {
            // Soft delete: mark as consumed
            const result = await query(
                "UPDATE items SET status = 'consumed' WHERE id = $1 AND user_id = $2 RETURNING *",
                [itemId, userId]
            );
            return res.status(200).json({
                success: true,
                message: 'Item marked as consumed',
                data: result.rows[0]
            });
        }
    } catch (error) {
        console.error('Delete item error:', error);
        return res.status(500).json({
            success: false,
            error: 'Failed to delete item'
        });
    }
}
