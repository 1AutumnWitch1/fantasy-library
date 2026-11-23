#!/bin/sh
set -eu
export MC_NO_COLOR=1

case "$MINIO_BUCKET" in
  ''|[.-]*|*[.-]|*[!a-z0-9.-]*)
    echo 'Invalid MinIO bucket name.' >&2
    exit 1
    ;;
esac
if [ "${#MINIO_BUCKET}" -lt 3 ] || [ "${#MINIO_BUCKET}" -gt 63 ]; then
  echo 'Invalid MinIO bucket name length.' >&2
  exit 1
fi

attempt=0
until mc alias set library http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null 2>&1 &&
      mc admin info library >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 60 ]; then
    echo 'MinIO did not become ready. Check the service logs and local credentials.' >&2
    exit 1
  fi
  sleep 2
done

mc mb --ignore-existing "library/$MINIO_BUCKET" >/dev/null

printf '%s\n' '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetBucketLocation", "s3:ListBucket", "s3:ListBucketMultipartUploads"],
      "Resource": ["arn:aws:s3:::'"$MINIO_BUCKET"'"]
    },
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", "s3:AbortMultipartUpload", "s3:ListMultipartUploadParts"],
      "Resource": ["arn:aws:s3:::'"$MINIO_BUCKET"'/*"]
    }
  ]
}' > /tmp/fantasy-library-policy.json

if ! mc admin accesskey info library "$MINIO_ACCESS_KEY" >/dev/null 2>&1; then
  if ! mc admin accesskey create library --access-key "$MINIO_ACCESS_KEY" --secret-key "$MINIO_SECRET_KEY" \
      --policy /tmp/fantasy-library-policy.json --name fantasy-library-backend >/dev/null 2>&1; then
    echo 'Unable to create the scoped MinIO access key.' >&2
    exit 1
  fi
fi

if ! mc alias set app http://minio:9000 "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" >/dev/null 2>&1 ||
   ! mc ls "app/$MINIO_BUCKET" >/dev/null 2>&1; then
  echo 'The MinIO application key cannot access the configured bucket.' >&2
  exit 1
fi
echo 'MinIO bucket and scoped application access are ready.'
