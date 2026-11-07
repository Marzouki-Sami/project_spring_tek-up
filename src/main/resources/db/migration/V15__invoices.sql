-- One invoice per order. The seller details and VAT rate are copied in when the invoice is issued, so
-- changing the settings later never alters an invoice that was already sent.
CREATE TABLE invoices (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id          BIGINT        NOT NULL,
    invoice_number    VARCHAR(30)   NOT NULL,
    invoice_year      INT           NOT NULL,
    sequence_number   INT           NOT NULL,
    issued_at         TIMESTAMP     NOT NULL,
    seller_name       VARCHAR(80)   NOT NULL,
    seller_address    VARCHAR(300)  NULL,
    seller_vat_number VARCHAR(40)   NULL,
    vat_percent       DECIMAL(4, 1) NOT NULL,
    note              VARCHAR(300)  NULL,
    CONSTRAINT fk_invoices_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT uq_invoices_order UNIQUE (order_id),
    CONSTRAINT uq_invoices_number UNIQUE (invoice_number),
    -- numbers run 1, 2, 3... within a year with no repeats; a lost race rolls back and takes the next one
    CONSTRAINT uq_invoices_sequence UNIQUE (invoice_year, sequence_number)
);