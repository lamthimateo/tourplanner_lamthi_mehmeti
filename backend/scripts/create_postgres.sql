-- Run as a superuser (e.g. postgres) to create the DB/user.

CREATE
USER tourplanner WITH PASSWORD 'tourplanner';
CREATE
DATABASE tourplanner OWNER tourplanner;
GRANT ALL PRIVILEGES ON DATABASE
tourplanner TO tourplanner;
