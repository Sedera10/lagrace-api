-- Les dates de réservation sont des DATE, sans heure. Une conversion directe
-- DATE::timestamptz dépend du TimeZone de la session PostgreSQL et peut les
-- décaler d'un jour dans l'API. UTC sert ici de représentation stable pour la
-- vue technique : les règles métier restent portées par LocalDate.
CREATE OR REPLACE VIEW v_booking_line AS
SELECT u.line_type, u.line_id, u.ref, u.booking_id, u.resource_id,
       u.starts_at, u.ends_at, u.quantity, u.unit_price, u.included, u.amount,
       u.status_id, status.status_code, u.is_active, u.version
FROM (
    SELECT 'ROOM'::text AS line_type, id_booking_room AS line_id, ref, booking_id, room_id AS resource_id,
           check_in::timestamp AT TIME ZONE 'UTC' AS starts_at,
           check_out::timestamp AT TIME ZONE 'UTC' AS ends_at,
           nights AS quantity, unit_price, included, amount, status_id, version, is_active
    FROM booking_room
    UNION ALL
    SELECT 'POOL'::text, id_booking_pool, ref, booking_id, pool_id, starts_at, ends_at,
           quantity, unit_price, included, amount, status_id, version, is_active FROM booking_pool
    UNION ALL
    SELECT 'MEETING_ROOM'::text, id_booking_meeting_room, ref, booking_id, meeting_room_id,
           service_date::timestamp AT TIME ZONE 'UTC', NULL::timestamptz, quantity, unit_price, included, amount,
           status_id, version, is_active FROM booking_meeting_room
    UNION ALL
    SELECT 'SPACE'::text, id_booking_space, ref, booking_id, space_id,
           event_date::timestamp AT TIME ZONE 'UTC', NULL::timestamptz, quantity, unit_price, included, amount,
           status_id, version, is_active FROM booking_space
) u
JOIN LATERAL (SELECT s.code AS status_code FROM booking_line_status s WHERE s.id_booking_line_status = u.status_id) status ON true;
