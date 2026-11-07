-- One row of shop-wide settings: branding for emails and invoices, company details and VAT.
CREATE TABLE shop_settings (
    id              INT           PRIMARY KEY,
    shop_name       VARCHAR(80)   NOT NULL,
    accent_color    VARCHAR(7)    NOT NULL,
    footer_text     VARCHAR(300)  NOT NULL,
    support_email   VARCHAR(254)  NULL,
    company_address VARCHAR(300)  NULL,
    vat_number      VARCHAR(40)   NULL,
    vat_percent     DECIMAL(4, 1) NOT NULL,
    invoice_note    VARCHAR(300)  NULL,
    updated_at      TIMESTAMP     NOT NULL
);
INSERT INTO shop_settings (id, shop_name, accent_color, footer_text, vat_percent, updated_at)
VALUES (1, 'Shop', '#4f46e5', 'You received this email because of activity on your Shop account.', 20.0, CURRENT_TIMESTAMP);

-- Only edited emails are stored; an email with no row uses its built-in text.
CREATE TABLE email_templates (
    template_key VARCHAR(40)   PRIMARY KEY,
    subject      VARCHAR(200)  NOT NULL,
    heading      VARCHAR(120)  NOT NULL,
    body         VARCHAR(2000) NOT NULL,
    button_label VARCHAR(40)   NULL,
    updated_at   TIMESTAMP     NOT NULL,
    updated_by   BIGINT        NULL
);

ALTER TABLE users ADD COLUMN low_stock_alerts BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE products ADD COLUMN low_stock_threshold INT NULL;