insert into tb_role (authority)
values
    ('ROLE_ADMIN'),
    ('ROLE_MANAGER'),
    ('ROLE_DOORMAN'),
    ('ROLE_RESIDENT');

insert into tb_feature (code, name, description)
values
    (
        'PACKAGES',
        'Packages',
        'Package management'
    ),
    (
        'NOTIFICATIONS',
        'Notifications',
        'User notifications'
    ),
    (
        'ANNOUNCEMENTS',
        'Announcements',
        'Condominium announcements'
    ),
    (
        'RESERVATIONS',
        'Reservations',
        'Common area reservations'
    ),
    (
        'REPORTS',
        'Reports',
        'Administrative reports'
    ),
    (
        'MARKETPLACE_INTEGRATION',
        'Marketplace Integration',
        'Marketplace integration capabilities'
    );