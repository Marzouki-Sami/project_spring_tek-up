-- A variant is identified by the set of option values it combines. Storing that set as a key and
-- making it unique per product means two variants can never describe the same combination,
-- even when two managers generate variants at the same moment.
ALTER TABLE product_variants ADD COLUMN option_key VARCHAR(255) NOT NULL DEFAULT '';

UPDATE product_variants
SET option_key = COALESCE((SELECT GROUP_CONCAT(x.option_value_id ORDER BY x.option_value_id SEPARATOR '-')
                           FROM variant_option_values x
                           WHERE x.variant_id = product_variants.id), '');

ALTER TABLE product_variants ADD CONSTRAINT uq_variant_combination UNIQUE (product_id, option_key);