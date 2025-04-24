-- V2__AddExclusionConstraint.sql

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservation
  ALTER COLUMN compartment_id SET NOT NULL;

ALTER TABLE reservation
  ADD CONSTRAINT compartment_no_overlap
  EXCLUDE USING gist (
    compartment_id WITH =,
    tsrange(reservation_start, reservation_end, '[]') WITH &&
  );
