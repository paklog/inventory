#!/bin/bash

# Stock Initialization Runner Script
# This script executes the k6 stock initialization simulation

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
PRODUCT_CATALOG_URL="${PRODUCT_CATALOG_URL:-http://localhost:8082}"
INVENTORY_URL="${INVENTORY_URL:-http://localhost:8085}"
MIN_STOCK="${MIN_STOCK:-500}"
MAX_STOCK="${MAX_STOCK:-2000}"

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║         PAKLOG INVENTORY - STOCK INITIALIZATION           ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if k6 is installed
if ! command -v k6 &> /dev/null; then
    echo -e "${RED}❌ k6 is not installed!${NC}"
    echo ""
    echo "Please install k6:"
    echo "  macOS:   brew install k6"
    echo "  Linux:   sudo apt-get install k6"
    echo "  Windows: choco install k6"
    echo ""
    echo "Or download from: https://k6.io/docs/get-started/installation/"
    exit 1
fi

echo -e "${GREEN}✅ k6 found: $(k6 version | head -1)${NC}"
echo ""

# Display configuration
echo -e "${YELLOW}Configuration:${NC}"
echo "  Product Catalog: $PRODUCT_CATALOG_URL"
echo "  Inventory API:   $INVENTORY_URL"
echo "  Stock Range:     $MIN_STOCK - $MAX_STOCK units"
echo ""

# Check if services are running
echo -e "${YELLOW}Checking service availability...${NC}"

if curl -s -f "$PRODUCT_CATALOG_URL/products?limit=1" > /dev/null; then
    echo -e "${GREEN}✅ Product Catalog service is available${NC}"
else
    echo -e "${RED}❌ Product Catalog service is not available at $PRODUCT_CATALOG_URL${NC}"
    echo -e "${YELLOW}   Please start the Product Catalog service first${NC}"
    exit 1
fi

if curl -s -f "$INVENTORY_URL/inventory/inventory_health_metrics" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Inventory service is available${NC}"
else
    echo -e "${YELLOW}⚠️  Inventory service check returned non-200 (this might be normal)${NC}"
fi

echo ""
echo -e "${BLUE}Starting stock initialization...${NC}"
echo ""

# Run k6 script
k6 run \
  -e PRODUCT_CATALOG_URL="$PRODUCT_CATALOG_URL" \
  -e INVENTORY_URL="$INVENTORY_URL" \
  -e MIN_STOCK="$MIN_STOCK" \
  -e MAX_STOCK="$MAX_STOCK" \
  simulation/stock-initialization.js

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║              STOCK INITIALIZATION COMPLETE                ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
