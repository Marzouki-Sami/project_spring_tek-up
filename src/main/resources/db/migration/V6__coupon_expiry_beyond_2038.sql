-- TIMESTAMP stops at 2038 on MySQL; coupons may legitimately expire later.
ALTER TABLE coupons MODIFY expires_at DATETIME(6) NULL;