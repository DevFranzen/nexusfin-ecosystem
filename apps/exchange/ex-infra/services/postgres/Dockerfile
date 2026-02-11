FROM postgres:15-alpine
HEALTHCHECK --interval=5s --retries=5 --timeout=5s \
    CMD pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}


