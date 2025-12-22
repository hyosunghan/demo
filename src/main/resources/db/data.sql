INSERT INTO users(id, username, password, phone_number, status) VALUES(1,'admin', '13bfb9844ba36ac930ba142d8c3db629', '4839cd3a710b59fc75c0cacee0fa76ba', 1);
INSERT INTO users(id, username, password, phone_number, status) VALUES(2,'user', '6b45279d6b7ee560c7a67c6bdcea87e0', '4839cd3a710b59fc75c0cacee0fa76ba', 1);

INSERT INTO role(id, role_name) VALUES(1,'admin');
INSERT INTO role(id, role_name) VALUES(2,'user');

INSERT INTO users_role(users_id, role_id) VALUES(1,1);
INSERT INTO users_role(users_id, role_id) VALUES(2,2);

INSERT INTO permission(id, permission_name, url) VALUES (1, 'user:user', '/user');
INSERT INTO permission(id, permission_name, url) VALUES (2, 'user:add', '/user/add');
INSERT INTO permission(id, permission_name, url) VALUES (3, 'user:delete', '/user/delete');

INSERT INTO role_permission(role_id, permission_id) VALUES(1,1);
INSERT INTO role_permission(role_id, permission_id) VALUES(1,2);
INSERT INTO role_permission(role_id, permission_id) VALUES(1,3);
INSERT INTO role_permission(role_id, permission_id) VALUES(2,1);