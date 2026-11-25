-- Operational observations per daily journal (SR-08 / SCRUM-118).
-- Editable while the journal is OPEN; locked in read-only mode once CLOSED.

ALTER TABLE journals
    ADD COLUMN IF NOT EXISTS observations TEXT;
