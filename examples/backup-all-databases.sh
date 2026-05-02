#!/bin/bash

# Example: Backup Multiple Databases
# This script backs up multiple databases in sequence

set -e

# Configuration
CLOUDCLI_JAR="../target/cloudcli-0.0.1-SNAPSHOT.jar"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Database configurations
declare -A DATABASES=(
    ["mysql:production_db"]="mysql:localhost:3306:production_db:backup_user"
    ["mysql:analytics_db"]="mysql:localhost:3306:analytics_db:backup_user"
    ["postgres:app_db"]="postgres:localhost:5432:app_db:postgres"
    ["mongodb:logs_db"]="mongodb:localhost:27017:logs_db:admin"
)

# Counters
SUCCESS_COUNT=0
FAIL_COUNT=0

echo "================================================"
echo "Multi-Database Backup Script"
echo "================================================"
echo ""

# Backup each database
for key in "${!DATABASES[@]}"; do
    IFS=':' read -r db_type db_name <<< "$key"
    IFS=':' read -r _ host port database username <<< "${DATABASES[$key]}"
    
    echo -e "${YELLOW}Backing up $db_type database: $db_name${NC}"
    
    if java -jar "$CLOUDCLI_JAR" backup \
        --type "$db_type" \
        --host "$host" \
        --port "$port" \
        --database "$database" \
        --username "$username" \
        --password \
        --backup-type FULL \
        --compress; then
        
        echo -e "${GREEN}✓ $db_name backup successful${NC}"
        ((SUCCESS_COUNT++))
    else
        echo -e "${RED}✗ $db_name backup failed${NC}"
        ((FAIL_COUNT++))
    fi
    
    echo ""
done

# Summary
echo "================================================"
echo "Backup Summary"
echo "================================================"
echo -e "Successful: ${GREEN}$SUCCESS_COUNT${NC}"
echo -e "Failed: ${RED}$FAIL_COUNT${NC}"
echo ""

if [ $FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}All backups completed successfully!${NC}"
    exit 0
else
    echo -e "${RED}Some backups failed. Check logs for details.${NC}"
    exit 1
fi
