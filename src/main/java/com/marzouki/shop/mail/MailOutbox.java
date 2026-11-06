package com.marzouki.shop.mail;

/** Delivers a finished email. Implementations must not throw: a mail problem never breaks a purchase. */
public interface MailOutbox {

    void send(Email email);
}