-- Database-per-bounded-context: each service owns its schema and credentials.
-- No service can read another context's tables; they integrate via REST or Kafka only.
CREATE USER orders_user WITH PASSWORD 'orders_pw';
CREATE DATABASE orders OWNER orders_user;

CREATE USER inventory_user WITH PASSWORD 'inventory_pw';
CREATE DATABASE inventory OWNER inventory_user;

CREATE USER payments_user WITH PASSWORD 'payments_pw';
CREATE DATABASE payments OWNER payments_user;

CREATE USER sagas_user WITH PASSWORD 'sagas_pw';
CREATE DATABASE sagas OWNER sagas_user;
