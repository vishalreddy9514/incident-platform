-- V11: seed default incident categories.
--
-- This is reference/configuration data the application needs to be usable
-- out of the box (ADMIN can add more via FR-21) — not demo/fake data.
-- Deliberately no seeded users, teams, or incidents here: those are test
-- fixtures/demo data, which belong in test setup (Phase 9) or a separate,
-- clearly-labelled optional seed script — not in production migrations,
-- where they'd risk being mistaken for real accounts in a real deployment.

INSERT INTO incident_categories (name, description) VALUES
    ('Hardware', 'Physical equipment issues: laptops, monitors, peripherals'),
    ('Software', 'Application bugs, installation issues, licensing'),
    ('Network', 'Connectivity, VPN, Wi-Fi, DNS issues'),
    ('Access & Permissions', 'Account access, permission requests, SSO issues'),
    ('Database', 'Database performance, connectivity, or data issues'),
    ('Security', 'Security incidents, suspicious activity, policy violations'),
    ('Other', 'Anything not covered by the above categories');
