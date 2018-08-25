FROM mysql:5.7

# By default script / sqls files under the directory "docker-entrypoint-initdb.d" will be executed while setting up the db.
# If you want to execute it again, enable this flag and create a new image.
ENV IMPORT_FROM_VOLUME=true

COPY customscripts/*.sql /docker-entrypoint-initdb.d/