UPDATE user_account
SET password = '$2a$10$UwEvbGKeLxC3gIN2kNNU3OoIiLdlU2yrSXrhwxZZZg0JixiLEZz0m'
WHERE email = 'admin@example.com'
  AND password = 'admin123';

UPDATE user_account
SET password = '$2a$10$StleQ2zvLJA3rQGDT98bBO4pQ.tTxX3IQj8VoMHv8.dqD8y5y5EaC'
WHERE email IN ('alice@example.com', 'bob@example.com')
  AND password = 'user123';
