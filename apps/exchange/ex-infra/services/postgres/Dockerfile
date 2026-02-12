FROM postgres:15-alpine
COPY init_dbs.sh /docker-entrypoint-initdb.d/init_dbs.sh
HEALTHCHECK --interval=5s --retries=5 --timeout=5s \
    CMD pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}


