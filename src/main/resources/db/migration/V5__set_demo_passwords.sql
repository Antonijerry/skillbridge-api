-- V5: demo passwords for the three seeded accounts so a reviewer can log in.
-- Dummy data only. The plaintext password is documented in the README, never here.
-- Only the V3 placeholder hashes are replaced; a password someone has changed is left alone.
UPDATE employees
SET password_hash = '$2a$10$PeHcuMkEZSSdGZ5or3GtG.XrJ1gfcMjarQDKJYxlzN5PNzk4xxIl.'
WHERE email IN ('ada@skillbridge.local', 'tunde@skillbridge.local', 'chidi@skillbridge.local')
  AND password_hash LIKE '$2a$10$dummy.placeholder%';