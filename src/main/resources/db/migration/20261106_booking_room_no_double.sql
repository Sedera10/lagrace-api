CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE booking_room
    ADD CONSTRAINT no_double_room
    EXCLUDE USING gist (
        room_id WITH =,
        daterange(check_in, check_out, '[)'::text) WITH &&
    ) WHERE (is_active = true);