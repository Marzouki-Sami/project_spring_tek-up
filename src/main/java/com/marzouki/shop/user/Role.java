package com.marzouki.shop.user;

/** What a signed-in user is allowed to do. */
public enum Role {
    /** Shops and manages their own account and orders. */
    CUSTOMER,
    /** Runs the store day to day: catalog, stock, suppliers and orders. */
    MANAGER,
    /** Everything a manager can do, plus users and roles. */
    ADMIN;

    /** Managers and admins run the shop and are held to stricter sign-in rules. */
    public boolean isStaff() {
        return this != CUSTOMER;
    }
}