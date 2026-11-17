-- Une ligne CHECK_OUT ne réserve plus la chambre, mais son montant reste dû et
-- doit donc continuer d'être agrégé dans v_booking_balance.
UPDATE booking_room br
SET is_active = false
FROM booking_line_status status
WHERE br.status_id = status.id_booking_line_status
  AND status.code = 'CHECK_OUT'
  AND br.is_active = true;

-- Répare aussi l'état opérationnel des chambres déjà libérées à la clôture.
-- Les statuts de maintenance et hors service sont volontairement préservés.
UPDATE room room
SET status_id = available.id_status
FROM room_status available
WHERE UPPER(TRIM(available.name)) = 'DISPONIBLE'
  AND UPPER(TRIM((SELECT occupied.name FROM room_status occupied WHERE occupied.id_status = room.status_id)))
      IN ('OCCUPEE', 'OCCUPÉE')
  AND EXISTS (
      SELECT 1
      FROM booking_room br
      JOIN booking booking ON booking.id_booking = br.booking_id
      JOIN booking_line_status line_status ON line_status.id_booking_line_status = br.status_id
      WHERE br.room_id = room.id_room
        AND booking.status_id = (SELECT id_booking_status FROM booking_status WHERE code = 'CLOSED')
        AND line_status.code = 'CHECK_OUT'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM booking_room active_line
      JOIN booking active_booking ON active_booking.id_booking = active_line.booking_id
      WHERE active_line.room_id = room.id_room
        AND active_line.is_active = true
        AND active_booking.status_id NOT IN (
            SELECT id_booking_status FROM booking_status WHERE code IN ('CLOSED', 'CANCELED')
        )
  );

CREATE OR REPLACE VIEW v_booking_balance AS
SELECT b.id_booking, lt.total AS lines_total, ct.total AS charges_total, ot.total AS others_total,
       lt.total + ct.total + ot.total AS total, pt.paid,
       lt.total + ct.total + ot.total - pt.paid AS balance,
       ct.deposit_due, pt.deposit_in - pt.deposit_out AS deposit_held
FROM booking b
CROSS JOIN LATERAL (
    SELECT COALESCE(SUM(l.amount), 0) AS total
    FROM v_booking_line l
    WHERE l.booking_id = b.id_booking
      AND (l.is_active OR l.status_code = 'CHECK_OUT')
) lt
CROSS JOIN LATERAL (SELECT COALESCE(SUM(c.amount) FILTER (WHERE NOT c.is_deposit), 0) AS total,
                           COALESCE(SUM(c.amount) FILTER (WHERE c.is_deposit), 0) AS deposit_due
                    FROM booking_charge c WHERE c.booking_id = b.id_booking AND c.voided_at IS NULL) ct
CROSS JOIN LATERAL (SELECT COALESCE(SUM(o.amount), 0) AS total FROM booking_other o WHERE o.booking_id = b.id_booking AND o.voided_at IS NULL) ot
CROSS JOIN LATERAL (SELECT COALESCE(SUM(CASE WHEN k.code IN ('PAYMENT','ADVANCE') THEN p.amount WHEN k.code = 'REFUND' THEN -p.amount ELSE 0 END), 0) AS paid,
                           COALESCE(SUM(p.amount) FILTER (WHERE k.code = 'DEPOSIT'), 0) AS deposit_in,
                           COALESCE(SUM(p.amount) FILTER (WHERE k.code = 'DEPOSIT_REFUND'), 0) AS deposit_out
                    FROM payment p JOIN payment_kind k ON k.id_kind = p.kind_id WHERE p.booking_id = b.id_booking) pt;
