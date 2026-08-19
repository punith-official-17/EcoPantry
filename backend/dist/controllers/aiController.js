"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.scanReceipt = scanReceipt;
exports.suggestRecipes = suggestRecipes;
const db_js_1 = require("../db/db.js");
const geminiService_js_1 = require("../services/geminiService.js");
const validation_js_1 = require("../utils/validation.js");
async function scanReceipt(req, res) {
    try {
        const userId = req.user.id;
        let imageBase64 = '';
        let mimeType = 'image/jpeg';
        // Check if file was uploaded via multipart/form-data
        if (req.file) {
            imageBase64 = req.file.buffer.toString('base64');
            mimeType = req.file.mimetype || 'image/jpeg';
        }
        else if (req.body.imageBase64) {
            // Base64 provided in JSON body
            const validation = validation_js_1.ScanReceiptBase64Schema.safeParse(req.body);
            if (!validation.success) {
                return res.status(400).json({
                    success: false,
                    errors: validation.error.format()
                });
            }
            imageBase64 = validation.data.imageBase64.replace(/^data:image\/\w+;base64,/, '');
            mimeType = validation.data.mimeType;
        }
        else {
            return res.status(400).json({
                success: false,
                error: 'Please upload an image file or provide an imageBase64 string in JSON body.'
            });
        }
        // Call Gemini OCR
        const extractedItems = await (0, geminiService_js_1.scanReceiptWithGemini)(imageBase64, mimeType);
        if (!extractedItems || extractedItems.length === 0) {
            return res.status(200).json({
                success: true,
                message: 'No readable grocery items detected in receipt.',
                count: 0,
                data: []
            });
        }
        // Bulk insert items into PostgreSQL
        const insertedItems = [];
        const todayStr = new Date().toISOString().split('T')[0];
        for (const item of extractedItems) {
            const sql = `
                INSERT INTO items (user_id, name, category, quantity, purchase_date, expiry_date, status)
                VALUES ($1, $2, $3, $4, $5, $6, 'active')
                RETURNING *, (expiry_date - CURRENT_DATE) AS days_until_expiry
            `;
            const result = await (0, db_js_1.query)(sql, [
                userId,
                item.name,
                item.category,
                item.quantity || '1',
                todayStr,
                item.expiry_date
            ]);
            insertedItems.push(result.rows[0]);
        }
        return res.status(201).json({
            success: true,
            message: `Successfully extracted and saved ${insertedItems.length} pantry items from receipt.`,
            count: insertedItems.length,
            data: insertedItems
        });
    }
    catch (error) {
        console.error('Receipt scan endpoint error:', error);
        return res.status(500).json({
            success: false,
            error: 'Failed to process receipt image'
        });
    }
}
async function suggestRecipes(req, res) {
    try {
        const userId = req.user.id;
        const validation = validation_js_1.SuggestRecipesSchema.safeParse(req.body || {});
        const days = validation.success ? validation.data.expiringWithinDays : 3;
        // Query user's items expiring in the next N days
        const expiringQuery = `
            SELECT id, name, category, quantity, expiry_date
            FROM items
            WHERE user_id = $1 
              AND status = 'active'
              AND expiry_date <= (CURRENT_DATE + ($2 || ' days')::INTERVAL)
            ORDER BY expiry_date ASC
        `;
        const expiringResult = await (0, db_js_1.query)(expiringQuery, [userId, days.toString()]);
        let expiringItems = expiringResult.rows;
        // If no items are strictly expiring within N days, fetch all active items as fallback
        if (expiringItems.length === 0) {
            const allItemsResult = await (0, db_js_1.query)(`SELECT id, name, category, quantity, expiry_date FROM items WHERE user_id = $1 AND status = 'active' ORDER BY expiry_date ASC LIMIT 5`, [userId]);
            expiringItems = allItemsResult.rows;
        }
        if (expiringItems.length === 0) {
            return res.status(200).json({
                success: true,
                message: 'Your pantry is currently empty! Add items to get personalized recipes.',
                count: 0,
                data: []
            });
        }
        // Fetch other pantry staples
        const allItemsResult = await (0, db_js_1.query)(`SELECT name, quantity FROM items WHERE user_id = $1 AND status = 'active'`, [userId]);
        // Generate recipes via Gemini
        const generatedRecipes = await (0, geminiService_js_1.generateRecipesWithGemini)(expiringItems, allItemsResult.rows);
        // Save generated recipes into the recipes database table
        const savedRecipes = [];
        for (const recipe of generatedRecipes) {
            const insertSql = `
                INSERT INTO recipes (user_id, title, ingredients, instructions, prep_time)
                VALUES ($1, $2, $3, $4, $5)
                RETURNING *
            `;
            const result = await (0, db_js_1.query)(insertSql, [
                userId,
                recipe.title,
                JSON.stringify(recipe.ingredients),
                recipe.instructions,
                recipe.prep_time
            ]);
            savedRecipes.push(result.rows[0]);
        }
        return res.status(200).json({
            success: true,
            message: `Generated ${savedRecipes.length} zero-waste recipes using your expiring ingredients.`,
            count: savedRecipes.length,
            expiring_ingredients_used: expiringItems.map(i => i.name),
            data: savedRecipes
        });
    }
    catch (error) {
        console.error('Recipe suggestion error:', error);
        return res.status(500).json({
            success: false,
            error: 'Failed to generate zero-waste recipe suggestions'
        });
    }
}
