CREATE OR REPLACE VIEW v_booking_balance AS
SELECT b.id_booking,
       lt.total                       AS lines_total,
       ct.total                       AS charges_total,
       lt.total + ct.total + ot.total AS total,
       ot.total                       AS others_total,
       pt.paid                        AS paid,
       lt.total + ct.total + ot.total - pt.paid AS balance,
       ct.deposit_due                 AS deposit_due,
       pt.deposit_in - pt.deposit_out AS deposit_held
FROM booking b
CROSS JOIN LATERAL (
    SELECT COALESCE(SUM(l.amount), 0) AS total
    FROM v_booking_line l
    WHERE l.booking_id = b.id_booking AND l.is_active
) lt
CROSS JOIN LATERAL (
    SELECT COALESCE(SUM(c.amount) FILTER (WHERE NOT c.is_deposit), 0) AS total,
           COALESCE(SUM(c.amount) FILTER (WHERE c.is_deposit), 0)     AS deposit_due
    FROM booking_charge c
    WHERE c.booking_id = b.id_booking AND c.voided_at IS NULL
) ct
CROSS JOIN LATERAL (
    SELECT COALESCE(SUM(o.amount), 0) AS total
    FROM booking_other o
    WHERE o.booking_id = b.id_booking AND o.voided_at IS NULL
) ot
CROSS JOIN LATERAL (
    SELECT COALESCE(SUM(CASE WHEN k.code IN ('PAYMENT', 'ADVANCE') THEN p.amount
                             WHEN k.code = 'REFUND' THEN -p.amount ELSE 0 END), 0) AS paid,
           COALESCE(SUM(p.amount) FILTER (WHERE k.code = 'DEPOSIT'), 0)           AS deposit_in,
           COALESCE(SUM(p.amount) FILTER (WHERE k.code = 'DEPOSIT_REFUND'), 0)    AS deposit_out
    FROM payment p
    JOIN payment_kind k ON k.id_kind = p.kind_id
    WHERE p.booking_id = b.id_booking
) pt;
