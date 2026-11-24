ALTER TABLE photo
    DROP CONSTRAINT IF EXISTS chk_photo_item_type;

ALTER TABLE photo
    ADD CONSTRAINT chk_photo_item_type
    CHECK (item_type IN ('ROOM', 'ROOM_TYPE', 'SPACE', 'POOL', 'EMPLOYEE', 'USER'));
