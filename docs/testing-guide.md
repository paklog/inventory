# Testing Guide

Comprehensive guide to testing practices for the Inventory Management Service.

## Table of Contents

1. [Testing Philosophy](#testing-philosophy)
2. [Test Pyramid](#test-pyramid)
3. [Unit Testing](#unit-testing)
4. [Integration Testing](#integration-testing)
5. [Test Data Management](#test-data-management)
6. [Best Practices](#best-practices)
7. [Running Tests](#running-tests)
8. [Code Coverage](#code-coverage)
9. [Performance Testing](#performance-testing)

---

## Testing Philosophy

### Core Principles

1. **Tests are Documentation**: Tests should explain how the system works
2. **Fast Feedback**: Unit tests must be fast (< 1 second for entire suite)
3. **Reliable**: Tests should not be flaky (no random failures)
4. **Maintainable**: Tests should be easy to understand and modify
5. **Comprehensive**: Aim for > 80% code coverage, 100% for critical paths

### Test Goals

```
✅ Verify business logic correctness
✅ Catch regressions early
✅ Enable confident refactoring
✅ Document system behavior
✅ Ensure API contracts are maintained
```

---

## Test Pyramid

We follow the Testing Pyramid pattern:

```
        ╱╲
       ╱  ╲
      ╱ E2E╲        🔴 Few, slow, expensive
     ╱──────╲       Manual/automated end-to-end tests
    ╱        ╲
   ╱Integration╲   🟡 Some, medium speed
  ╱────────────╲   API tests, repository tests
 ╱              ╲
╱  Unit Tests    ╲  🟢 Many, fast, cheap
──────────────────  Domain logic, services, utilities
```

### Distribution (Target)

| Type | Count | Percentage | Speed |
|------|-------|------------|-------|
| **Unit Tests** | 400+ | 70% | < 1s total |
| **Integration Tests** | 80+ | 25% | < 30s total |
| **E2E Tests** | 20+ | 5% | < 2min total |

---

## Unit Testing

### Domain Layer Testing

Domain entities should be tested thoroughly as they contain critical business logic.

#### Example: ProductStock Aggregate Test

```java
@Test
void shouldAdjustQuantityOnHand() {
    // Given
    ProductStock stock = ProductStock.create("SKU-001", 100);

    // When
    stock.adjustQuantityOnHand(50, "STOCK_INTAKE");

    // Then
    assertEquals(150, stock.getQuantityOnHand());
    assertEquals(150, stock.getAvailableToPromise());

    // Verify event was generated
    List<DomainEvent> events = stock.getUncommittedEvents();
    assertEquals(1, events.size());
    assertTrue(events.get(0) instanceof StockLevelChangedEvent);

    StockLevelChangedEvent event = (StockLevelChangedEvent) events.get(0);
    assertEquals("SKU-001", event.getSku());
    assertEquals(100, event.getPreviousQuantityOnHand());
    assertEquals(150, event.getNewQuantityOnHand());
}

@Test
void shouldThrowExceptionWhenAllocatingMoreThanAvailable() {
    // Given
    ProductStock stock = ProductStock.create("SKU-001", 100);

    // When & Then
    assertThrows(InsufficientStockException.class, () -> {
        stock.allocate(150);
    });
}

@Test
void shouldCalculateAvailableToPromiseCorrectly() {
    // Given
    ProductStock stock = ProductStock.create("SKU-001", 100);

    // When
    stock.allocate(30);

    // Then
    assertEquals(100, stock.getQuantityOnHand());
    assertEquals(30, stock.getQuantityAllocated());
    assertEquals(70, stock.getAvailableToPromise());
}
```

#### Value Object Testing

```java
@Test
void shouldCreateStockLevel() {
    // When
    StockLevel stockLevel = StockLevel.of(100, 20);

    // Then
    assertEquals(100, stockLevel.getQuantityOnHand());
    assertEquals(20, stockLevel.getQuantityAllocated());
    assertEquals(80, stockLevel.getAvailable());
}

@Test
void shouldBeImmutable() {
    // Given
    StockLevel original = StockLevel.of(100, 20);

    // When
    StockLevel adjusted = original.adjustOnHand(50);

    // Then - original unchanged
    assertEquals(100, original.getQuantityOnHand());
    assertEquals(20, original.getQuantityAllocated());

    // New instance created
    assertEquals(150, adjusted.getQuantityOnHand());
    assertEquals(20, adjusted.getQuantityAllocated());
}
```

### Application Layer Testing

Service tests use mocked repositories to test business logic in isolation.

#### Example: Command Service Test

```java
@ExtendWith(MockitoExtension.class)
class InventoryCommandServiceTest {

    @Mock
    private ProductStockRepository productStockRepository;

    @Mock
    private InventoryLedgerRepository ledgerRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private InventoryMetricsService metricsService;

    @InjectMocks
    private InventoryCommandService commandService;

    @Test
    void shouldAdjustStock() {
        // Given
        String sku = "SKU-001";
        ProductStock existingStock = ProductStock.create(sku, 100);

        when(productStockRepository.findBySku(sku))
            .thenReturn(Optional.of(existingStock));
        when(productStockRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductStock result = commandService.adjustStock(
            sku, 50, "STOCK_INTAKE", "New shipment", "admin"
        );

        // Then
        assertEquals(150, result.getQuantityOnHand());

        // Verify interactions
        verify(productStockRepository).findBySku(sku);
        verify(productStockRepository).save(any(ProductStock.class));
        verify(ledgerRepository).save(any(InventoryLedgerEntry.class));
        verify(outboxRepository).saveAll(anyList());
        verify(metricsService).incrementStockAdjustment(eq(sku), eq(50), eq("STOCK_INTAKE"));
    }

    @Test
    void shouldThrowExceptionWhenStockNotFound() {
        // Given
        when(productStockRepository.findBySku("NONEXISTENT"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ProductStockNotFoundException.class, () -> {
            commandService.adjustStock("NONEXISTENT", 50, "STOCK_INTAKE", null, "admin");
        });
    }
}
```

---

## Integration Testing

Integration tests use **Testcontainers** to run tests against real infrastructure.

### Repository Integration Test

```java
@SpringBootTest
@Testcontainers
class ProductStockRepositoryIntegrationTest {

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7.0")
        .withExposedPorts(27017);

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
    }

    @Autowired
    private ProductStockRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndRetrieveProductStock() {
        // Given
        ProductStock stock = ProductStock.create("SKU-001", 100);
        stock.allocate(20);

        // When
        repository.save(stock);
        Optional<ProductStock> retrieved = repository.findBySku("SKU-001");

        // Then
        assertTrue(retrieved.isPresent());
        ProductStock found = retrieved.get();
        assertEquals("SKU-001", found.getSku());
        assertEquals(100, found.getQuantityOnHand());
        assertEquals(20, found.getQuantityAllocated());
        assertEquals(80, found.getAvailableToPromise());
    }

    @Test
    void shouldUpdateExistingStock() {
        // Given
        ProductStock stock = ProductStock.create("SKU-001", 100);
        repository.save(stock);

        // When
        ProductStock updated = repository.findBySku("SKU-001").get();
        updated.receiveStock(50);
        repository.save(updated);

        // Then
        ProductStock retrieved = repository.findBySku("SKU-001").get();
        assertEquals(150, retrieved.getQuantityOnHand());
    }

    @Test
    void shouldFindMultipleSkus() {
        // Given
        repository.save(ProductStock.create("SKU-001", 100));
        repository.save(ProductStock.create("SKU-002", 200));
        repository.save(ProductStock.create("SKU-003", 300));

        // When
        List<String> allSkus = repository.findAllSkus();

        // Then
        assertEquals(3, allSkus.size());
        assertTrue(allSkus.contains("SKU-001"));
        assertTrue(allSkus.contains("SKU-002"));
        assertTrue(allSkus.contains("SKU-003"));
    }
}
```

### Controller Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class InventoryControllerIntegrationTest {

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7.0");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductStockRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldAdjustStockLevel() throws Exception {
        // Given
        ProductStock stock = ProductStock.create("SKU-001", 100);
        repository.save(stock);

        UpdateStockLevelRequest request = new UpdateStockLevelRequest(
            50,
            "stock_intake",
            "New shipment"
        );

        // When & Then
        mockMvc.perform(patch("/inventory/stock_levels/SKU-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("SKU-001"))
            .andExpect(jsonPath("$.quantity_on_hand").value(150))
            .andExpect(jsonPath("$.quantity_allocated").value(0))
            .andExpect(jsonPath("$.available_to_promise").value(150));
    }

    @Test
    void shouldReturn404WhenStockNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/inventory/stock_levels/NONEXISTENT"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetInventoryHealthMetrics() throws Exception {
        // Given
        repository.save(ProductStock.create("SKU-001", 100));
        repository.save(ProductStock.create("SKU-002", 0));
        repository.save(ProductStock.create("SKU-003", 200));

        // When & Then
        mockMvc.perform(get("/inventory/inventory_health_metrics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total_skus").value(3))
            .andExpect(jsonPath("$.out_of_stock_skus").value(1));
    }
}
```

---

## Test Data Management

### Test Data Builders

Use the Builder pattern for creating test data:

```java
public class ProductStockTestBuilder {

    private String sku = "SKU-TEST-001";
    private int quantityOnHand = 100;
    private int quantityAllocated = 0;
    private Location location = new Location("WH-001", "A", "1", "B1");

    public static ProductStockTestBuilder aProductStock() {
        return new ProductStockTestBuilder();
    }

    public ProductStockTestBuilder withSku(String sku) {
        this.sku = sku;
        return this;
    }

    public ProductStockTestBuilder withQuantityOnHand(int quantity) {
        this.quantityOnHand = quantity;
        return this;
    }

    public ProductStockTestBuilder withAllocation(int quantity) {
        this.quantityAllocated = quantity;
        return this;
    }

    public ProductStock build() {
        ProductStock stock = ProductStock.create(sku, quantityOnHand);
        if (quantityAllocated > 0) {
            stock.allocate(quantityAllocated);
        }
        return stock;
    }
}
```

**Usage**:
```java
@Test
void testWithBuilder() {
    ProductStock stock = aProductStock()
        .withSku("SKU-001")
        .withQuantityOnHand(500)
        .withAllocation(100)
        .build();

    assertEquals(400, stock.getAvailableToPromise());
}
```

### Test Fixtures

Create reusable test data:

```java
public class TestFixtures {

    public static ProductStock createLowStockProduct() {
        return ProductStock.create("SKU-LOW-001", 5);
    }

    public static ProductStock createOutOfStockProduct() {
        return ProductStock.create("SKU-ZERO-001", 0);
    }

    public static ProductStock createInStockProduct() {
        return aProductStock()
            .withSku("SKU-IN-001")
            .withQuantityOnHand(1000)
            .build();
    }
}
```

---

## Best Practices

### 1. Test Naming

Use descriptive test names that explain the scenario:

✅ **Good**:
```java
@Test
void shouldThrowExceptionWhenAllocatingMoreThanAvailable() { }

@Test
void shouldGenerateStockLevelChangedEventWhenAdjustingQuantity() { }

@Test
void shouldCalculateAvailableToPromiseAsOnHandMinusAllocated() { }
```

❌ **Bad**:
```java
@Test
void test1() { }

@Test
void testAllocate() { }

@Test
void testCase() { }
```

### 2. AAA Pattern

Structure tests with Arrange-Act-Assert:

```java
@Test
void shouldAllocateStock() {
    // Arrange (Given)
    ProductStock stock = ProductStock.create("SKU-001", 100);

    // Act (When)
    stock.allocate(30);

    // Assert (Then)
    assertEquals(70, stock.getAvailableToPromise());
}
```

### 3. One Assertion Per Test (When Possible)

Focus tests on single behaviors:

```java
// Good - focused test
@Test
void shouldCalculateAvailableToPromise() {
    ProductStock stock = ProductStock.create("SKU-001", 100);
    stock.allocate(30);
    assertEquals(70, stock.getAvailableToPromise());
}

// Also good - multiple related assertions
@Test
void shouldMaintainInvariantsAfterAllocation() {
    ProductStock stock = ProductStock.create("SKU-001", 100);
    stock.allocate(30);

    assertEquals(100, stock.getQuantityOnHand());
    assertEquals(30, stock.getQuantityAllocated());
    assertEquals(70, stock.getAvailableToPromise());
}
```

### 4. Test Edge Cases

```java
@Test
void shouldHandleZeroQuantityAllocation() {
    ProductStock stock = ProductStock.create("SKU-001", 100);
    stock.allocate(0);
    assertEquals(100, stock.getAvailableToPromise());
}

@Test
void shouldHandleNegativeAdjustment() {
    ProductStock stock = ProductStock.create("SKU-001", 100);
    stock.adjustQuantityOnHand(-30, "DAMAGED");
    assertEquals(70, stock.getQuantityOnHand());
}
```

### 5. Use @DisplayName for Readability

```java
@DisplayName("ProductStock Aggregate")
class ProductStockTest {

    @Test
    @DisplayName("should throw exception when allocating more than available")
    void shouldThrowExceptionWhenAllocatingMoreThanAvailable() {
        // test
    }
}
```

---

## Running Tests

### All Tests

```bash
./mvnw clean test
```

### Specific Test Class

```bash
./mvnw test -Dtest=ProductStockTest
```

### Specific Test Method

```bash
./mvnw test -Dtest=ProductStockTest#shouldAdjustQuantityOnHand
```

### Integration Tests Only

```bash
./mvnw verify -Pfailsafe
```

### Skip Tests

```bash
./mvnw install -DskipTests
```

### Run Tests in Parallel

```bash
./mvnw test -T 4  # 4 threads
```

---

## Code Coverage

### Generate Coverage Report

```bash
./mvnw clean test jacoco:report
```

Open `target/site/jacoco/index.html` in browser.

### Coverage Targets

| Layer | Target | Critical Paths |
|-------|--------|----------------|
| Domain | 90%+ | 100% |
| Application | 80%+ | 95%+ |
| Infrastructure | 70%+ | N/A |
| **Overall** | **80%+** | **95%+** |

### Coverage Rules

**Must Cover**:
- ✅ All business logic
- ✅ All domain invariants
- ✅ All edge cases
- ✅ All error paths

**Can Skip**:
- ⏭️ DTOs (data classes only)
- ⏭️ Configuration classes
- ⏭️ Simple getters/setters
- ⏭️ Main class

---

## Performance Testing

### Load Testing with Gatling

```scala
class InventoryLoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8085")
    .acceptHeader("application/json")

  val scn = scenario("Stock Level Query")
    .exec(http("Get Stock Level")
      .get("/inventory/stock_levels/SKU-001")
      .check(status.is(200)))

  setUp(
    scn.inject(
      rampUsersPerSec(10) to 100 during (60 seconds),
      constantUsersPerSec(100) during (300 seconds)
    )
  ).protocols(httpProtocol)
}
```

### Performance Assertions

```java
@Test
@Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
void shouldReturnStockLevelWithin100ms() {
    // Test must complete in < 100ms
    StockLevelResponse response = queryService.getStockLevel("SKU-001");
    assertNotNull(response);
}
```

---

## CI/CD Integration

Tests run automatically on:
- **Pull Requests**: All tests must pass
- **Main Branch Push**: Full test suite + coverage
- **Nightly Build**: Performance tests + load tests

### GitHub Actions Workflow

```yaml
- name: Run Tests
  run: ./mvnw clean verify

- name: Upload Coverage
  uses: codecov/codecov-action@v3
  with:
    files: target/site/jacoco/jacoco.xml
```

---

## Troubleshooting

### Tests Taking Too Long

**Cause**: Integration tests with Testcontainers
**Solution**: Run unit tests only during development

```bash
./mvnw test -Dtest=*Test  # Excludes *IntegrationTest
```

### Flaky Tests

**Cause**: Timing issues, shared state
**Solution**:
- Use `@RepeatedTest(10)` to identify flaky tests
- Ensure proper cleanup in `@BeforeEach`
- Avoid Thread.sleep(), use Awaitility instead

```java
await().atMost(5, SECONDS)
    .until(() -> repository.findBySku("SKU-001").isPresent());
```

### Testcontainers Not Starting

**Cause**: Docker not running or insufficient resources
**Solution**:
- Ensure Docker is running
- Increase Docker memory to 4GB+
- Clear Docker containers: `docker system prune -a`

---

## Resources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Testcontainers](https://www.testcontainers.org/)
- [AssertJ](https://assertj.github.io/doc/)

---

**Last Updated**: 2025-10-05
**Maintained By**: Platform Team
