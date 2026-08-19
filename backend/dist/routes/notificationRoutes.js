"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const notificationController_js_1 = require("../controllers/notificationController.js");
const authMiddleware_js_1 = require("../middleware/authMiddleware.js");
const router = (0, express_1.Router)();
router.get('/alerts', authMiddleware_js_1.authenticateToken, notificationController_js_1.getUserExpiryAlerts);
router.post('/run-cron', notificationController_js_1.triggerDailyCronJob);
exports.default = router;
