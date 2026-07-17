-- Only configuration values explicitly confirmed by the LTSS architecture are seeded.
-- Guest is an unauthenticated state and therefore is not persisted as a role.
-- Permission codes, badges, and engagement event types remain empty until their
-- official catalogs are approved; no account or domain sample data is created.

INSERT INTO roles (role_code, role_name, description, is_active)
VALUES
    ('TOURIST', 'Tourist', NULL, TRUE),
    ('BUSINESS_OWNER', 'Business Owner', NULL, TRUE),
    ('RELIC_MANAGER', 'Relic Manager', NULL, TRUE),
    ('MODERATOR', 'Moderator', NULL, TRUE),
    ('ADMINISTRATOR', 'Administrator', NULL, TRUE);

INSERT INTO role_inheritances (role_id, inherited_role_id)
SELECT child_role.id, parent_role.id
FROM roles child_role
JOIN roles parent_role ON parent_role.role_code = 'TOURIST'
WHERE child_role.role_code = 'BUSINESS_OWNER';

INSERT INTO role_inheritances (role_id, inherited_role_id)
SELECT child_role.id, parent_role.id
FROM roles child_role
JOIN roles parent_role ON parent_role.role_code = 'MODERATOR'
WHERE child_role.role_code = 'ADMINISTRATOR';
