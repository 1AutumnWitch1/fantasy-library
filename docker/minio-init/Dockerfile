FROM quay.io/minio/aistor/mc:RELEASE.2026-09-19T15-24-59Z
COPY --chmod=0755 init.sh /usr/local/bin/minio-init
ENTRYPOINT ["/usr/local/bin/minio-init"]
