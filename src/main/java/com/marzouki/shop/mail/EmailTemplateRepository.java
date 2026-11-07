package com.marzouki.shop.mail;

import org.springframework.data.jpa.repository.JpaRepository;

interface EmailTemplateRepository extends JpaRepository<EmailTemplate, String> {
}