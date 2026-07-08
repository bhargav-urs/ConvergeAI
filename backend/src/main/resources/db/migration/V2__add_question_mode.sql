-- Debate mode: NORMAL (3 rounds) or FAST (1 round + consensus, concise outputs).
ALTER TABLE questions
    ADD COLUMN mode VARCHAR(20) NOT NULL DEFAULT 'NORMAL';
