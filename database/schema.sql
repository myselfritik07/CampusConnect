CREATE DATABASE IF NOT EXISTS campus_connect;
USE campus_connect;

CREATE TABLE IF NOT EXISTS events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(40) NOT NULL,
    venue VARCHAR(120) NOT NULL,
    start_time DATETIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_events_start_time (start_time),
    INDEX idx_events_category (category)
);

INSERT INTO events (title, description, category, venue, start_time)
SELECT 'Creative Coding Night', 'Build a tiny visual project with fellow makers.', 'Workshop', 'Innovation Lab', '2027-03-18 17:30:00'
WHERE NOT EXISTS (SELECT 1 FROM events WHERE title = 'Creative Coding Night');

INSERT INTO events (title, description, category, venue, start_time)
SELECT 'Research Made Clear', 'A friendly introduction to presenting your research.', 'Seminar', 'Lecture Hall B', '2027-03-22 13:00:00'
WHERE NOT EXISTS (SELECT 1 FROM events WHERE title = 'Research Made Clear');

INSERT INTO events (title, description, category, venue, start_time)
SELECT 'Spring Courtyard Social', 'Meet classmates over music and refreshments.', 'Social', 'Central Courtyard', '2027-03-27 16:00:00'
WHERE NOT EXISTS (SELECT 1 FROM events WHERE title = 'Spring Courtyard Social');
