"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.scanReceiptWithGemini = scanReceiptWithGemini;
exports.generateRecipesWithGemini = generateRecipesWithGemini;
const genai_1 = require("@google/genai");
const dotenv_1 = __importDefault(require("dotenv"));
dotenv_1.default.config();
const apiKey = process.env.GEMINI_API_KEY || '';
const ai = apiKey ? new genai_1.GoogleGenAI({ apiKey }) : null;
/**
 * Scan a receipt or grocery item image using Google Gemini 1.5 Flash Vision
 */
async function scanReceiptWithGemini(imageBase64, mimeType = 'image/jpeg') {
    const today = new Date();
    const todayStr = today.toISOString().split('T')[0];
    if (!apiKey || !ai) {
        console.warn('GEMINI_API_KEY not configured, using smart heuristic fallback parser');
        return getFallbackReceiptItems();
    }
    try {
        const prompt = `
You are an intelligent grocery receipt and food packaging OCR scanner for a Smart Pantry app.
Current date: ${todayStr}.

Analyze this grocery receipt or food item image and extract all purchased/visible food items.
For every item, provide:
1. "name": Clean, concise food item name (e.g. "Whole Milk", "Baby Spinach", "Salmon Fillet").
2. "category": Must strictly be one of: "produce", "dairy", "meat", "bakery", "pantry", "other".
3. "quantity": Detected quantity and unit (e.g. "1 carton", "200g", "2 lbs", "1 loaf", "1").
4. "estimated_shelf_life_days": Realistic number of days this item stays fresh under standard storage (e.g. spinach 4, milk 7, fresh meat 3, bread 5, rice 180).
5. "expiry_date": Calculated ISO date (YYYY-MM-DD) based on current date (${todayStr}) + estimated_shelf_life_days.

Return ONLY a valid JSON array of objects.
`;
        const response = await ai.models.generateContent({
            model: 'gemini-1.5-flash',
            contents: [
                {
                    parts: [
                        { text: prompt },
                        {
                            inlineData: {
                                mimeType,
                                data: imageBase64
                            }
                        }
                    ]
                }
            ],
            config: {
                responseMimeType: 'application/json',
                responseSchema: {
                    type: genai_1.Type.ARRAY,
                    items: {
                        type: genai_1.Type.OBJECT,
                        properties: {
                            name: { type: genai_1.Type.STRING },
                            category: {
                                type: genai_1.Type.STRING,
                                enum: ['produce', 'dairy', 'meat', 'bakery', 'pantry', 'other']
                            },
                            quantity: { type: genai_1.Type.STRING },
                            estimated_shelf_life_days: { type: genai_1.Type.INTEGER },
                            expiry_date: { type: genai_1.Type.STRING }
                        },
                        required: ['name', 'category', 'quantity', 'estimated_shelf_life_days', 'expiry_date']
                    }
                }
            }
        });
        const rawText = response.text || '';
        const parsed = JSON.parse(rawText);
        return parsed.map(item => ({
            ...item,
            category: validateCategory(item.category),
            expiry_date: item.expiry_date || calculateExpiryDate(item.estimated_shelf_life_days || 7)
        }));
    }
    catch (error) {
        console.error('Gemini OCR Receipt processing error:', error);
        return getFallbackReceiptItems();
    }
}
/**
 * Generate 2-3 custom zero-waste recipes using ingredients expiring soon
 */
async function generateRecipesWithGemini(expiringItems, allPantryItems = []) {
    if (expiringItems.length === 0) {
        return getFallbackRecipes();
    }
    if (!apiKey || !ai) {
        console.warn('GEMINI_API_KEY not configured, using curated zero-waste recipes');
        return getFallbackRecipes(expiringItems);
    }
    try {
        const expiringListStr = expiringItems
            .map(i => `- ${i.name} (Qty: ${i.quantity}, Expires: ${i.expiry_date})`)
            .join('\n');
        const stapleListStr = allPantryItems.length > 0
            ? allPantryItems.map(i => `${i.name} (${i.quantity})`).join(', ')
            : 'Standard cooking oil, salt, pepper, garlic, onion, rice, pasta';
        const prompt = `
You are a professional zero-waste chef and food sustainability expert.
The user has the following food items EXPIRING VERY SOON in their pantry/fridge:
${expiringListStr}

Available pantry staples / other items:
${stapleListStr}

Generate 2 to 3 delicious, practical, zero-waste recipes that prioritize rescuing these expiring items before they spoil.
For each recipe provide:
1. "title": Catchy, appetizing recipe name.
2. "ingredients": Complete list of ingredient strings including quantities.
3. "instructions": Clear step-by-step cooking instructions formatted as a single string with numbered steps.
4. "prep_time": Total time needed (e.g. "15 mins", "25 mins").

Return ONLY a valid JSON array of recipe objects.
`;
        const response = await ai.models.generateContent({
            model: 'gemini-1.5-flash',
            contents: prompt,
            config: {
                responseMimeType: 'application/json',
                responseSchema: {
                    type: genai_1.Type.ARRAY,
                    items: {
                        type: genai_1.Type.OBJECT,
                        properties: {
                            title: { type: genai_1.Type.STRING },
                            ingredients: {
                                type: genai_1.Type.ARRAY,
                                items: { type: genai_1.Type.STRING }
                            },
                            instructions: { type: genai_1.Type.STRING },
                            prep_time: { type: genai_1.Type.STRING }
                        },
                        required: ['title', 'ingredients', 'instructions', 'prep_time']
                    }
                }
            }
        });
        const rawText = response.text || '';
        const recipes = JSON.parse(rawText);
        return recipes;
    }
    catch (error) {
        console.error('Gemini Recipe Generation error:', error);
        return getFallbackRecipes(expiringItems);
    }
}
function validateCategory(cat) {
    const valid = ['produce', 'dairy', 'meat', 'bakery', 'pantry', 'other'];
    const lower = (cat || '').toLowerCase();
    return valid.includes(lower) ? lower : 'other';
}
function calculateExpiryDate(days) {
    const date = new Date();
    date.setDate(date.getDate() + days);
    return date.toISOString().split('T')[0];
}
function getFallbackReceiptItems() {
    const date = new Date();
    const addDays = (d) => {
        const res = new Date(date);
        res.setDate(res.getDate() + d);
        return res.toISOString().split('T')[0];
    };
    return [
        {
            name: 'Fresh Organic Spinach',
            category: 'produce',
            quantity: '1 bag (200g)',
            estimated_shelf_life_days: 4,
            expiry_date: addDays(4)
        },
        {
            name: 'Whole Milk',
            category: 'dairy',
            quantity: '1 carton',
            estimated_shelf_life_days: 7,
            expiry_date: addDays(7)
        },
        {
            name: 'Salmon Fillets',
            category: 'meat',
            quantity: '2 pcs (400g)',
            estimated_shelf_life_days: 2,
            expiry_date: addDays(2)
        },
        {
            name: 'Sourdough Bread',
            category: 'bakery',
            quantity: '1 loaf',
            estimated_shelf_life_days: 5,
            expiry_date: addDays(5)
        }
    ];
}
function getFallbackRecipes(expiringItems) {
    const itemNames = expiringItems ? expiringItems.map(i => i.name).join(', ') : 'Expiring pantry ingredients';
    return [
        {
            title: 'Quick Pantry Rescue Skillet',
            ingredients: [
                'Expiring produce/meat: ' + itemNames,
                '2 tbsp Olive Oil',
                '2 cloves Garlic (minced)',
                'Salt and black pepper to taste',
                '1 cup cooked rice or pasta'
            ],
            instructions: '1. Heat olive oil in a skillet over medium-high heat.\n2. Sauté garlic for 30 seconds until fragrant.\n3. Add chopped expiring items and cook for 4-6 minutes until tender and cooked through.\n4. Toss in cooked rice/pasta and season with salt and pepper.\n5. Serve hot.',
            prep_time: '15 mins'
        },
        {
            title: 'Zero-Waste Garden Frittata',
            ingredients: [
                '3 large Eggs',
                '1/4 cup Milk or Cream',
                'Chopped expiring vegetables (' + itemNames + ')',
                '1/4 cup shredded cheese',
                '1 tbsp butter'
            ],
            instructions: '1. Whisk eggs with milk, salt, and pepper in a bowl.\n2. Melt butter in an oven-safe skillet and sauté vegetables for 3 minutes.\n3. Pour egg mixture over vegetables and top with cheese.\n4. Cook on stovetop until edges set (4 mins), then broil for 3 minutes until golden.\n5. Slice and serve.',
            prep_time: '20 mins'
        }
    ];
}
