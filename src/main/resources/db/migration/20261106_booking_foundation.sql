-- À exécuter après Migration_Booking_update.sql et la création de booking_other.
ALTER TABLE booking_status_history ADD COLUMN IF NOT EXISTS reason VARCHAR(255);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE booking_room ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE booking_pool ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE booking_meeting_room ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE booking_space ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE booking_charge ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE booking_other ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;

DROP VIEW IF EXISTS v_booking_balance;
DROP VIEW IF EXISTS v_booking_line;
CREATE VIEW v_booking_line AS
SELECT u.line_type, u.line_id, u.ref, u.booking_id, u.resource_id,
       u.starts_at, u.ends_at, u.quantity, u.unit_price, u.included, u.amount,
       u.status_id, status.status_code, u.is_active, u.version
FROM (
    SELECT 'ROOM'::text AS line_type, id_booking_room AS line_id, ref, booking_id, room_id AS resource_id,
           check_in::timestamptz AS starts_at, check_out::timestamptz AS ends_at,
           nights AS quantity, unit_price, included, amount, status_id, version, is_active
    FROM booking_room
    UNION ALL
    SELECT 'POOL'::text, id_booking_pool, ref, booking_id, pool_id, starts_at, ends_at,
           quantity, unit_price, included, amount, status_id, version, is_active FROM booking_pool
    UNION ALL
    SELECT 'MEETING_ROOM'::text, id_booking_meeting_room, ref, booking_id, meeting_room_id,
           service_date::timestamptz, NULL::timestamptz, quantity, unit_price, included, amount,
           status_id, version, is_active FROM booking_meeting_room
    UNION ALL
    SELECT 'SPACE'::text, id_booking_space, ref, booking_id, space_id,
           event_date::timestamptz, NULL::timestamptz, quantity, unit_price, included, amount,
           status_id, version, is_active FROM booking_space
) u
JOIN LATERAL (SELECT s.code AS status_code FROM booking_line_status s WHERE s.id_booking_line_status = u.status_id) status ON true;

CREATE VIEW v_booking_balance AS
SELECT b.id_booking, lt.total AS lines_total, ct.total AS charges_total, ot.total AS others_total,
       lt.total + ct.total + ot.total AS total, pt.paid,
       lt.total + ct.total + ot.total - pt.paid AS balance,
       ct.deposit_due, pt.deposit_in - pt.deposit_out AS deposit_held
FROM booking b
CROSS JOIN LATERAL (SELECT COALESCE(SUM(l.amount), 0) AS total FROM v_booking_line l WHERE l.booking_id = b.id_booking AND l.is_active) lt
CROSS JOIN LATERAL (SELECT COALESCE(SUM(c.amount) FILTER (WHERE NOT c.is_deposit), 0) AS total,
                           COALESCE(SUM(c.amount) FILTER (WHERE c.is_deposit), 0) AS deposit_due
                    FROM booking_charge c WHERE c.booking_id = b.id_booking AND c.voided_at IS NULL) ct
CROSS JOIN LATERAL (SELECT COALESCE(SUM(o.amount), 0) AS total FROM booking_other o WHERE o.booking_id = b.id_booking AND o.voided_at IS NULL) ot
CROSS JOIN LATERAL (SELECT COALESCE(SUM(CASE WHEN k.code IN ('PAYMENT','ADVANCE') THEN p.amount WHEN k.code = 'REFUND' THEN -p.amount ELSE 0 END), 0) AS paid,
                           COALESCE(SUM(p.amount) FILTER (WHERE k.code = 'DEPOSIT'), 0) AS deposit_in,
                           COALESCE(SUM(p.amount) FILTER (WHERE k.code = 'DEPOSIT_REFUND'), 0) AS deposit_out
                    FROM payment p JOIN payment_kind k ON k.id_kind = p.kind_id WHERE p.booking_id = b.id_booking) pt;
