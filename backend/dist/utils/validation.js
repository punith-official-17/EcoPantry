"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SuggestRecipesSchema = exports.ScanReceiptBase64Schema = exports.UpdateItemSchema = exports.CreateItemSchema = exports.LoginSchema = exports.RegisterSchema = exports.StatusEnum = exports.CategoryEnum = void 0;
const zod_1 = require("zod");
exports.CategoryEnum = zod_1.z.enum(['produce', 'dairy', 'meat', 'bakery', 'pantry', 'other']);
exports.StatusEnum = zod_1.z.enum(['active', 'consumed', 'expired']);
exports.RegisterSchema = zod_1.z.object({
    email: zod_1.z.string().email('Invalid email address'),
    password: zod_1.z.string().min(6, 'Password must be at least 6 characters long'),
    name: zod_1.z.string().min(2, 'Name must be at least 2 characters long'),
});
exports.LoginSchema = zod_1.z.object({
    email: zod_1.z.string().email('Invalid email address'),
    password: zod_1.z.string().min(1, 'Password is required'),
});
exports.CreateItemSchema = zod_1.z.object({
    name: zod_1.z.string().min(1, 'Item name is required'),
    category: exports.CategoryEnum.default('other'),
    quantity: zod_1.z.string().default('1'),
    purchase_date: zod_1.z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Purchase date must be YYYY-MM-DD').optional(),
    expiry_date: zod_1.z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Expiry date must be YYYY-MM-DD'),
    status: exports.StatusEnum.default('active'),
});
exports.UpdateItemSchema = zod_1.z.object({
    name: zod_1.z.string().min(1).optional(),
    category: exports.CategoryEnum.optional(),
    quantity: zod_1.z.string().optional(),
    purchase_date: zod_1.z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional(),
    expiry_date: zod_1.z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional(),
    status: exports.StatusEnum.optional(),
});
exports.ScanReceiptBase64Schema = zod_1.z.object({
    imageBase64: zod_1.z.string().min(10, 'Base64 image content is required'),
    mimeType: zod_1.z.string().default('image/jpeg'),
});
exports.SuggestRecipesSchema = zod_1.z.object({
    expiringWithinDays: zod_1.z.number().int().positive().default(3),
});
