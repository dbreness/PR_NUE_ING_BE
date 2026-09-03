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

-- Indice funcional: Optimiza busquedas en barra de texto convirtiendo todo a minusculas.
-- Permite busquedas rapidas case-insensitive en la UI.
CREATE INDEX IF NOT EXISTS idx_orders_product_name
    ON orders (lower(product_name));

-- Indice de estado: Acelera drasticamente el filtrado del Dashboard
-- cuando el usuario consulte unicamente pedidos 'PAGADO' o 'FALLO_PAGO' 
CREATE INDEX IF NOT EXISTS idx_orders_status
    ON orders (status);

-- Indice de paginacion: Ordena fisicamente los accesos en orden descendente.
CREATE INDEX IF NOT EXISTS idx_orders_created_at_desc
    ON orders (created_at DESC);

-- Funcion que actualiza automaticamente la mmodificacion.
CREATE OR REPLACE FUNCTION set_orders_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger que intercepta la fila justo antes de realizar un UPDATE.
-- cada transicion de estado asIncrona registra el segundo exacto del cambio.
DROP TRIGGER IF EXISTS trg_orders_updated_at ON orders;

CREATE TRIGGER trg_orders_updated_at
BEFORE UPDATE ON orders
FOR EACH ROW
EXECUTE FUNCTION set_orders_updated_at();