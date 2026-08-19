import { z } from 'zod';

export const CategoryEnum = z.enum(['produce', 'dairy', 'meat', 'bakery', 'pantry', 'other']);
export const StatusEnum = z.enum(['active', 'consumed', 'expired']);

export const RegisterSchema = z.object({
    email: z.string().email('Invalid email address'),
    password: z.string().min(6, 'Password must be at least 6 characters long'),
    name: z.string().min(2, 'Name must be at least 2 characters long'),
});

export const LoginSchema = z.object({
    email: z.string().email('Invalid email address'),
    password: z.string().min(1, 'Password is required'),
});

export const CreateItemSchema = z.object({
    name: z.string().min(1, 'Item name is required'),
    category: CategoryEnum.default('other'),
    quantity: z.string().default('1'),
    purchase_date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Purchase date must be YYYY-MM-DD').optional(),
    expiry_date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Expiry date must be YYYY-MM-DD'),
    status: StatusEnum.default('active'),
});

export const UpdateItemSchema = z.object({
    name: z.string().min(1).optional(),
    category: CategoryEnum.optional(),
    quantity: z.string().optional(),
    purchase_date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional(),
    expiry_date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional(),
    status: StatusEnum.optional(),
});

export const ScanReceiptBase64Schema = z.object({
    imageBase64: z.string().min(10, 'Base64 image content is required'),
    mimeType: z.string().default('image/jpeg'),
});

export const SuggestRecipesSchema = z.object({
    expiringWithinDays: z.number().int().positive().default(3),
});

export type RegisterInput = z.infer<typeof RegisterSchema>;
export type LoginInput = z.infer<typeof LoginSchema>;
export type CreateItemInput = z.infer<typeof CreateItemSchema>;
export type UpdateItemInput = z.infer<typeof UpdateItemSchema>;
export type ItemCategory = z.infer<typeof CategoryEnum>;
export type ItemStatus = z.infer<typeof StatusEnum>;
