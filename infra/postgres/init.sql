-- =============================================================================
-- TABLA PRINCIPAL: orders
-- =============================================================================
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    encrypted_card_data TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_orders_product_name_not_blank CHECK (length(trim(product_name)) > 0),
    CONSTRAINT chk_orders_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_orders_amount_non_negative CHECK (amount >= 0),
    CONSTRAINT chk_orders_status_allowed CHECK (status IN ('PENDIENTE', 'PAGADO', 'FALLO_PAGO'))
);

-- Índice funcional para búsquedas parciales sin distinguir mayúsculas.
CREATE INDEX IF NOT EXISTS idx_orders_product_name
    ON orders (lower(product_name));

-- Índice para filtrar las órdenes por estado.
CREATE INDEX IF NOT EXISTS idx_orders_status
    ON orders (status);

-- Índice para consultas ordenadas por fecha de creación descendente.
CREATE INDEX IF NOT EXISTS idx_orders_created_at_desc
    ON orders (created_at DESC);

-- Función que actualiza automáticamente la fecha de modificación.
CREATE OR REPLACE FUNCTION set_orders_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger que actualiza updated_at antes de modificar una orden.
DROP TRIGGER IF EXISTS trg_orders_updated_at ON orders;

CREATE TRIGGER trg_orders_updated_at
BEFORE UPDATE ON orders
FOR EACH ROW
EXECUTE FUNCTION set_orders_updated_at();
