# RDS trust bundle

Before an AWS release, run `scripts/fetch-rds-ca.sh` through a network allowing the official AWS endpoint. Review and commit the resulting public certificate bundle and its checksum, then rebuild the image. The bundle is not secret. AWS connections use `sslmode=verify-full`; local PostgreSQL connections do not require this file. Never downgrade TLS verification to bypass a missing bundle.
