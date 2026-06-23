INSERT INTO users(id, username, password, phone_number, status) VALUES(1,'admin', '$2a$10$Y2Cgsuie2spIjKHfwi8VL.EaflQaxBP5c9yQyok4pC.jD45zn6a7e', '4839cd3a710b59fc75c0cacee0fa76ba', 1);
INSERT INTO users(id, username, password, phone_number, status) VALUES(2,'user', '$2a$10$Y2Cgsuie2spIjKHfwi8VL.5aQgyQwRYt04eel4.vzROkuNFQxYXDa', '4839cd3a710b59fc75c0cacee0fa76ba', 1);

INSERT INTO role(id, role_name) VALUES(1,'admin');
INSERT INTO role(id, role_name) VALUES(2,'user');

INSERT INTO users_role(users_id, role_id) VALUES(1,1);
INSERT INTO users_role(users_id, role_id) VALUES(2,2);

INSERT INTO permission(id, permission_name, url) VALUES (1, 'user:user', '/user');
INSERT INTO permission(id, permission_name, url) VALUES (2, 'user:add', '/user/add');

INSERT INTO role_permission(role_id, permission_id) VALUES(1,1);
INSERT INTO role_permission(role_id, permission_id) VALUES(1,2);
INSERT INTO role_permission(role_id, permission_id) VALUES(2,1);