-- Apply once to the existing SmartPark database after taking a backup.
-- Existing reservations remain linked to their users; only guest reservations use NULL user_id.
-- guest_name and guest_phone already exist in the inspected SmartPark schema.
ALTER TABLE reservations
    MODIFY COLUMN user_id BIGINT(20) UNSIGNED NULL;
