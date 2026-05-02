#!/bin/bash

# Example: MySQL Backup Script
# Usage: ./backup-mysql.sh [database_name]

set -e

# Configuration
DB_TYPE="mysql"
DB_HOST="localhost"
DB_PORT="3306"
DB_USER="backup_user"
DB_NAME="${1:-mydb}"
BACKUP_TYPE="FULL"
COMPRESS="true"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo "Starting MySQL backup for database: $DB_NAME"

# Run backup
if java -jar ../target/cloudcli-0.0.1-SNAPSHOT.jar backup \
    --type "$DB_TYPE" \
    --host "$DB_HOST" \
    --port "$DB_PORT" \
    --database "$DB_NAME" \
    --username "$DB_USER" \
    --password \
    --backup-type "$BACKUP_TYPE" \
    --compress; then
    
    echo -e "${GREEN}✓ Backup completed successfully${NC}"
    exit 0
else
    echo -e "${RED}✗ Backup failed${NC}"
    exit 1
fi
