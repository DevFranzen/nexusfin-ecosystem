#!/bin/bash
set -e

echo "Initializing PostgreSQL database with provided environment variables..."

if [ -n "$KC_DB_NAME" ]; then
    echo "Creating user and database '$KC_DB_NAME' for Keycloak..."
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE USER $KC_DB_USERNAME WITH PASSWORD '$KC_DB_PASSWORD';
        CREATE DATABASE $KC_DB_NAME OWNER $KC_DB_USERNAME;
        GRANT ALL PRIVILEGES ON DATABASE $KC_DB_NAME TO $KC_DB_USERNAME;
EOSQL
fi