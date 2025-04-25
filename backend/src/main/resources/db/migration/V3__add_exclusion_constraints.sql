ALTER TABLE reservation
    ADD CONSTRAINT compartment_no_overlap
    EXCLUDE USING gist (
    compartment_id WITH =,
    tsrange(reservation_start, reservation_end, '[]') WITH &&
  );

ALTER TABLE reservation
    ADD CONSTRAINT no_overlapping_reservations
    EXCLUDE USING gist (
    compartment_id WITH =,
    tsrange(reservation_start, reservation_end) WITH &&
  );
