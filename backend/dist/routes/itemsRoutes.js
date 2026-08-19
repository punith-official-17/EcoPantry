"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const itemsController_js_1 = require("../controllers/itemsController.js");
const authMiddleware_js_1 = require("../middleware/authMiddleware.js");
const router = (0, express_1.Router)();
// All item endpoints require authentication
router.use(authMiddleware_js_1.authenticateToken);
router.get('/', itemsController_js_1.getItems);
router.post('/', itemsController_js_1.createItem);
router.put('/:id', itemsController_js_1.updateItem);
router.delete('/:id', itemsController_js_1.deleteItem);
exports.default = router;
