"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const multer_1 = __importDefault(require("multer"));
const aiController_js_1 = require("../controllers/aiController.js");
const authMiddleware_js_1 = require("../middleware/authMiddleware.js");
const upload = (0, multer_1.default)({
    storage: multer_1.default.memoryStorage(),
    limits: { fileSize: 10 * 1024 * 1024 } // 10MB limit
});
const router = (0, express_1.Router)();
// All AI endpoints require authentication
router.use(authMiddleware_js_1.authenticateToken);
router.post('/scan-receipt', upload.single('image'), aiController_js_1.scanReceipt);
router.post('/suggest-recipes', aiController_js_1.suggestRecipes);
exports.default = router;
