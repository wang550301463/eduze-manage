#!/bin/sh
set -eu
umask 077
for password in "$IDENTITY_REDIS_PASSWORD" "$ACADEMIC_REDIS_PASSWORD"; do
  case "$password" in *[!a-zA-Z0-9]*|'') echo "Redis credentials must be alphanumeric" >&2; exit 1;; esac
  [ "${#password}" -ge 24 ] || exit 1
done
cat > /tmp/users.acl <<ACL
user default off
user identity on >${IDENTITY_REDIS_PASSWORD} ~identity:* +@read +@write +@connection +info -keys -scan -randomkey -dbsize -flushdb -flushall
user academic on >${ACADEMIC_REDIS_PASSWORD} ~academic:* +@read +@write +@connection +info -keys -scan -randomkey -dbsize -flushdb -flushall
ACL
exec redis-server --appendonly yes --aclfile /tmp/users.acl
