import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const stockAddedRate = new Rate('stock_added');

// Configuration
const PRODUCT_CATALOG_URL = __ENV.PRODUCT_CATALOG_URL || 'http://localhost:8082';
const INVENTORY_URL = __ENV.INVENTORY_URL || 'http://localhost:8085';
const MIN_STOCK = parseInt(__ENV.MIN_STOCK || '500');
const MAX_STOCK = parseInt(__ENV.MAX_STOCK || '2000');

// K6 execution options
export const options = {
  stages: [
    { duration: '10s', target: 1 }, // Ramp up to 1 VU
    { duration: '30s', target: 1 }, // Stay at 1 VU for 30s
    { duration: '5s', target: 0 },  // Ramp down to 0
  ],
  thresholds: {
    'errors': ['rate<0.1'], // Error rate should be less than 10%
    'http_req_duration': ['p(95)<5000'], // 95% of requests should be below 5s
  },
};

/**
 * Fetch all products from the product catalog
 */
function fetchProducts() {
  console.log('📦 Fetching products from catalog...');

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  let allProducts = [];
  let currentPage = 0;
  let totalPages = 1;

  // Fetch all pages
  while (currentPage < totalPages) {
    const url = `${PRODUCT_CATALOG_URL}/products?offset=${currentPage * 20}&limit=20`;
    const response = http.get(url, params);

    const success = check(response, {
      'catalog response status is 200': (r) => r.status === 200,
    });

    if (!success) {
      console.error(`❌ Failed to fetch products (page ${currentPage}): ${response.status} - ${response.body}`);
      errorRate.add(1);
      break;
    }

    try {
      const data = JSON.parse(response.body);

      if (data.content && Array.isArray(data.content)) {
        allProducts = allProducts.concat(data.content);
        totalPages = data.total_pages || 1;
        console.log(`✅ Fetched page ${currentPage + 1}/${totalPages} - ${data.content.length} products`);
      }

      currentPage++;
    } catch (e) {
      console.error(`❌ Error parsing response: ${e.message}`);
      errorRate.add(1);
      break;
    }

    sleep(0.5); // Small delay between pages
  }

  console.log(`📊 Total products fetched: ${allProducts.length}`);
  return allProducts;
}

/**
 * Generate random stock quantity between MIN_STOCK and MAX_STOCK
 */
function getRandomStockQuantity() {
  return Math.floor(Math.random() * (MAX_STOCK - MIN_STOCK + 1)) + MIN_STOCK;
}

/**
 * Add stock for a single SKU using absolute set operation
 */
function addStockForSku(sku, quantity) {
  const url = `${INVENTORY_URL}/inventory/stock_levels/${sku}/set`;

  const payload = JSON.stringify({
    quantity: quantity,
    reason_code: 'INITIAL_STOCK',
    comment: `Weekly stock initialization - ${new Date().toISOString()}`,
    source_system: 'K6_SIMULATION',
    source_operator_id: 'automation',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const response = http.put(url, payload, params);

  const success = check(response, {
    'stock set successfully': (r) => r.status === 200,
  });

  if (success) {
    stockAddedRate.add(1);
    try {
      const data = JSON.parse(response.body);
      console.log(`✅ Stock set for ${sku}: ${data.quantity_on_hand} units`);
    } catch (e) {
      console.log(`✅ Stock set for ${sku}: ${quantity} units`);
    }
  } else {
    errorRate.add(1);
    console.error(`❌ Failed to set stock for ${sku}: ${response.status} - ${response.body}`);
  }

  return success;
}

/**
 * Main execution function
 */
export default function () {
  // Fetch products from catalog
  const products = fetchProducts();

  if (products.length === 0) {
    console.warn('⚠️  No products found in catalog. Exiting...');
    return;
  }

  console.log(`\n🚀 Starting stock initialization for ${products.length} SKUs...`);
  console.log(`📊 Stock range: ${MIN_STOCK} - ${MAX_STOCK} units per SKU\n`);

  let successCount = 0;
  let failureCount = 0;

  // Add stock for each product
  products.forEach((product, index) => {
    const sku = product.sku;
    const quantity = getRandomStockQuantity();

    console.log(`[${index + 1}/${products.length}] Processing ${sku} - ${quantity} units`);

    const success = addStockForSku(sku, quantity);

    if (success) {
      successCount++;
    } else {
      failureCount++;
    }

    // Small delay between requests to avoid overwhelming the server
    sleep(0.2);
  });

  // Summary
  console.log('\n' + '='.repeat(60));
  console.log('📊 STOCK INITIALIZATION SUMMARY');
  console.log('='.repeat(60));
  console.log(`Total SKUs processed: ${products.length}`);
  console.log(`✅ Successful: ${successCount}`);
  console.log(`❌ Failed: ${failureCount}`);
  console.log(`Success rate: ${((successCount / products.length) * 100).toFixed(2)}%`);
  console.log('='.repeat(60) + '\n');
}

/**
 * Setup function - runs once before all iterations
 */
export function setup() {
  console.log('\n' + '='.repeat(60));
  console.log('🎯 K6 STOCK INITIALIZATION SCRIPT');
  console.log('='.repeat(60));
  console.log(`Product Catalog: ${PRODUCT_CATALOG_URL}`);
  console.log(`Inventory Service: ${INVENTORY_URL}`);
  console.log(`Stock Range: ${MIN_STOCK} - ${MAX_STOCK} units`);
  console.log(`Timestamp: ${new Date().toISOString()}`);
  console.log('='.repeat(60) + '\n');

  // Test connectivity
  console.log('🔍 Testing service connectivity...');

  // Test Product Catalog
  const catalogResponse = http.get(`${PRODUCT_CATALOG_URL}/products?limit=1`);
  if (catalogResponse.status === 200) {
    console.log('✅ Product Catalog service is reachable');
  } else {
    console.error(`❌ Product Catalog service error: ${catalogResponse.status}`);
  }

  // Test Inventory Service
  const inventoryResponse = http.get(`${INVENTORY_URL}/inventory/inventory_health_metrics`);
  if (inventoryResponse.status === 200 || inventoryResponse.status === 400) {
    console.log('✅ Inventory service is reachable');
  } else {
    console.error(`❌ Inventory service error: ${inventoryResponse.status}`);
  }

  console.log('\n');
}

/**
 * Teardown function - runs once after all iterations
 */
export function teardown(data) {
  console.log('\n' + '='.repeat(60));
  console.log('✅ SCRIPT EXECUTION COMPLETED');
  console.log('='.repeat(60));
  console.log(`Finished at: ${new Date().toISOString()}`);
  console.log('='.repeat(60) + '\n');
}
