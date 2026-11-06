CREATE SEQUENCE IF NOT EXISTS seq_ref_other;

CREATE TABLE IF NOT EXISTS booking_other (
    id_booking_other INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ref VARCHAR(20) NOT NULL UNIQUE,
    booking_id INTEGER NOT NULL REFERENCES booking(id_booking),
    label VARCHAR(150) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    voided_at TIMESTAMPTZ,
    void_reason VARCHAR(255),
    created_by_user_id INTEGER REFERENCES users(id_user),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_other_label CHECK (btrim(label) <> ''),
    CONSTRAINT chk_other_amount CHECK (amount > 0),
    CONSTRAINT chk_other_void CHECK (voided_at IS NOT NULL OR void_reason IS NULL)
);

CREATE INDEX IF NOT EXISTS idx_other_booking ON booking_other (booking_id);
CREATE INDEX IF NOT EXISTS idx_other_created ON booking_other (created_at DESC);

CREATE TRIGGER trg_01_ref BEFORE INSERT ON booking_other
    FOR EACH ROW EXECUTE FUNCTION fn_set_ref('OT', 'seq_ref_other');

CREATE TRIGGER trg_02_touch BEFORE UPDATE ON booking_other
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
