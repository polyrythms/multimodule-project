--liquibase formatted sql
--changeset Denis:002-insert-admin-data

INSERT INTO admin_users (user_id, username, role, created_by)
VALUES ( '${TELEGRAM_ADMIN_USER_ID}', 'owner', 'OWNER', '${TELEGRAM_ADMIN_USER_ID}');