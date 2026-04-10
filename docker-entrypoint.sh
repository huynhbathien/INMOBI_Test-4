#!/bin/sh
# Load Docker secrets into environment variables before starting the JVM.
# Pattern: if FOO_FILE is set, read the file content into FOO.

file_env() {
  local var="$1"
  local fileVar="${var}_FILE"
  local val=""
  if [ -n "$(eval echo \$${fileVar})" ]; then
    val=$(cat "$(eval echo \$${fileVar})")
    export "${var}=${val}"
    unset "${fileVar}"
  fi
}

file_env SPRING_DATASOURCE_USERNAME
file_env SPRING_DATASOURCE_PASSWORD
file_env SPRING_REDIS_PASSWORD
file_env SSL_KEY_STORE_PASSWORD

# SSL_KEY_STORE is already a path pointing directly to the secret file
# No _FILE indirection needed for binary files

exec java ${JAVA_OPTS} -jar app.jar
