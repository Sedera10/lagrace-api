ALTER TABLE additional_service
    ADD COLUMN IF NOT EXISTS free_with_caterer BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE additional_service service
SET free_with_caterer = TRUE
FROM service_scope scope
WHERE scope.id_scope = service.scope_id
    AND scope.code = 'SPACE'
    AND service.name IN ('Congélateur', 'Réfrigérateur', 'Marmite', 'Chauffe-plat', 'Carafe');

CREATE UNIQUE INDEX IF NOT EXISTS uq_space_day
    ON booking_space (space_id, event_date) WHERE is_active;

CREATE OR REPLACE FUNCTION enforce_space_deposit_catalog()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_deposit AND NEW.is_active
       AND EXISTS (SELECT 1 FROM service_scope WHERE id_scope = NEW.scope_id AND code = 'SPACE') THEN
        PERFORM 1 FROM service_scope WHERE id_scope = NEW.scope_id FOR UPDATE;
        IF NEW.price <> 100000 THEN
            RAISE check_violation USING CONSTRAINT = 'chk_space_deposit_amount';
        END IF;
        IF EXISTS (
            SELECT 1
            FROM additional_service existing
            JOIN service_scope scope ON scope.id_scope = existing.scope_id
            WHERE scope.code = 'SPACE'
              AND existing.is_deposit
              AND existing.is_active
              AND existing.id_service <> COALESCE(NEW.id_service, 0)
        ) THEN
            RAISE unique_violation USING CONSTRAINT = 'uq_space_deposit_service';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_space_deposit_catalog ON additional_service;
CREATE TRIGGER trg_space_deposit_catalog
BEFORE INSERT OR UPDATE OF scope_id, price, is_deposit, is_active ON additional_service
FOR EACH ROW EXECUTE FUNCTION enforce_space_deposit_catalog();