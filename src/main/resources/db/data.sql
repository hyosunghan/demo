INSERT INTO users(id,username, password, phone_number) VALUES(100000000,'admin', '$2a$10$Adms66B41iS.4NFVlsMsJu6tebFlqpnIoCN77CVtIHT4jAbKR34Ve', '4839cd3a710b59fc75c0cacee0fa76ba');

INSERT INTO role(id,role_name) VALUES(1,'admin');

INSERT INTO users_role(users_id,role_id) VALUES(100000000,1);
