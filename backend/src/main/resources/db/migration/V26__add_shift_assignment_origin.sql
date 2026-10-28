-- Who put each day of the grid: the automatic rotation (GENERATED) or the administrator (MANUAL).
-- The rotation only rewrites empty or generated days, so a change made by hand is never overwritten by mistake.
-- Every existing row becomes MANUAL: nobody can tell today which rows came from the old rotation, and treating them as
-- manual is the safe choice.

ALTER TABLE shift_assignment
  ADD COLUMN origin VARCHAR(10) NOT NULL DEFAULT 'MANUAL'
    CONSTRAINT ck_shift_assignment_origin CHECK (origin IN ('GENERATED', 'MANUAL'));
