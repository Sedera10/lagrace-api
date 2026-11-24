DROP VIEW IF EXISTS v_room_detail;

CREATE VIEW v_room_detail AS
SELECT r.id_room,
       r.ref_room,
       r.floor,
       r.zone,
       rt.description,
       rs.id_status AS status_id,
       rs.name AS status,
       rs.color_code AS color,
       rt.id_type AS type_id,
       rt.name AS type,
       rt.default_capacity,
       rt.is_air_conditioned,
       rc.id_category AS category_id,
       rc.name AS category,
       cover.id_photo AS cover_photo_id,
       cover.url AS cover
FROM room r
JOIN room_status rs ON rs.id_status = r.status_id
JOIN room_type rt ON rt.id_type = r.type_id
JOIN room_category rc ON rc.id_category = rt.category_id
LEFT JOIN LATERAL (
    SELECT p.id_photo, p.url
    FROM photo p
    WHERE p.is_cover
      AND (
          (p.item_type = 'ROOM' AND p.item_id = r.id_room)
          OR (p.item_type = 'ROOM_TYPE' AND p.item_id = rt.id_type)
      )
    ORDER BY (p.item_type = 'ROOM') DESC, p.id_photo
    LIMIT 1
) cover ON TRUE
WHERE NOT r.is_deleted;
