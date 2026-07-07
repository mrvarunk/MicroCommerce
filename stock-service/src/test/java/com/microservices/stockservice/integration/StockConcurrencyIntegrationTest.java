package com.microservices.stockservice.integration;

import com.microservices.stockservice.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * Integration tests for Stock service demonstrating Redis distributed lock behavior
 * under concurrent load. Tests verify that the distributed lock prevents overselling
 * even when multiple concurrent requests attempt to decrease stock simultaneously.
 *
 * This test is designed to work with Spring Boot Test for local testing and can also
 * be configured to run against a Kubernetes deployment via the base URL property.
 *
 * Test scenarios:
 * - Concurrent requests from multiple threads
 * - Proper synchronization using Redis locks
 * - Accurate inventory management under high concurrency
 * - Prevention of overselling (negative inventory)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Stock Service Concurrency Integration Tests")
public class StockConcurrencyIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final Long TEST_PRODUCT_ID = 9999L;
    private static final Integer INITIAL_STOCK = 5;
    private static final Integer CONCURRENT_REQUESTS = 50;
    private static final Integer REQUEST_QUANTITY = 1;
    private static final Integer EXPECTED_SUCCESSFUL_REQUESTS = INITIAL_STOCK;
    private static final Integer EXPECTED_FAILED_REQUESTS = CONCURRENT_REQUESTS - EXPECTED_SUCCESSFUL_REQUESTS;

    @BeforeEach
    void setUp() {
        initializeStock(TEST_PRODUCT_ID, INITIAL_STOCK);
    }

    @Test
    @DisplayName("Redis distributed lock prevents overselling with 50 concurrent decrease requests")
    void testConcurrentStockDecrementWithDistributedLock() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(CONCURRENT_REQUESTS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        try {
            // Submit all tasks but they won't start until the latch is released
            for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        startSignal.await();
                        ResponseEntity<String> response = decreaseStock(TEST_PRODUCT_ID, REQUEST_QUANTITY);

                        if (response.getStatusCode() == HttpStatus.OK) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failureCount.incrementAndGet();
                    } finally {
                        doneSignal.countDown();
                    }
                }, executor);
                futures.add(future);
            }

            startSignal.countDown();
            boolean completed = doneSignal.await(30, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(completed, "Not all concurrent requests completed within timeout");

            await()
                    .atMost(java.time.Duration.ofSeconds(10))
                    .untilAsserted(() -> {
                        assertEquals(EXPECTED_SUCCESSFUL_REQUESTS, successCount.get(),
                                "Expected exactly " + EXPECTED_SUCCESSFUL_REQUESTS + " successful requests");
                        assertEquals(EXPECTED_FAILED_REQUESTS, failureCount.get(),
                                "Expected exactly " + EXPECTED_FAILED_REQUESTS + " failed requests");
                    });

            // Verify final stock level
            Stock finalStock = getStock(TEST_PRODUCT_ID);
            assertEquals(0, finalStock.getQuantity(),
                    "Final stock quantity should be 0 after all successful decrements");
            assertEquals(TEST_PRODUCT_ID, finalStock.getProductId(),
                    "Product ID should match");

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Verify stock initialization works correctly")
    void testStockInitialization() {
        Long productId = 8888L;
        Integer quantity = 100;

        initializeStock(productId, quantity);

        Stock stock = getStock(productId);
        assertEquals(productId, stock.getProductId(), "Product ID should match initialized value");
        assertEquals(quantity, stock.getQuantity(), "Quantity should match initialized value");
    }

    @Test
    @DisplayName("Single decrease request succeeds when stock is available")
    void testSingleDecreaseWithAvailableStock() {
        Stock initialStock = getStock(TEST_PRODUCT_ID);
        Integer initialQuantity = initialStock.getQuantity();

        ResponseEntity<String> response = decreaseStock(TEST_PRODUCT_ID, 1);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Decrease should succeed with available stock");

        Stock updatedStock = getStock(TEST_PRODUCT_ID);
        assertEquals(initialQuantity - 1, updatedStock.getQuantity(),
                "Quantity should be decremented by 1");
    }

    @Test
    @DisplayName("Decrease request fails when stock is insufficient")
    void testDecreaseWithInsufficientStock() {
        initializeStock(TEST_PRODUCT_ID, 1);

        ResponseEntity<String> response1 = decreaseStock(TEST_PRODUCT_ID, 1);
        assertEquals(HttpStatus.OK, response1.getStatusCode(), "First decrease should succeed");

        ResponseEntity<String> response2 = decreaseStock(TEST_PRODUCT_ID, 1);
        assertEquals(HttpStatus.BAD_REQUEST, response2.getStatusCode(),
                "Second decrease should fail due to insufficient stock");
        assertTrue(response2.getBody().contains("Insufficient stock"),
                "Error message should indicate insufficient stock");
    }

    @Test
    @DisplayName("Get stock returns correct product and quantity")
    void testGetStock() {
        Stock stock = getStock(TEST_PRODUCT_ID);
        assertNotNull(stock, "Stock should be retrieved");
        assertEquals(TEST_PRODUCT_ID, stock.getProductId(), "Product ID should match");
        assertEquals(INITIAL_STOCK, stock.getQuantity(), "Quantity should match initialized value");
    }

    @Test
    @DisplayName("Concurrent requests from 5 different threads produce correct results")
    void testConcurrencyWith5Threads() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(10);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        try {
            Long testProductId = 7777L;
            initializeStock(testProductId, 5);

            for (int i = 0; i < 10; i++) {
                executor.submit(() -> {
                    try {
                        startSignal.await();
                        ResponseEntity<String> response = decreaseStock(testProductId, 1);

                        if (response.getStatusCode() == HttpStatus.OK) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failureCount.incrementAndGet();
                    } finally {
                        doneSignal.countDown();
                    }
                });
            }

            startSignal.countDown();
            boolean completed = doneSignal.await(15, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(completed, "All requests should complete within timeout");

            assertEquals(5, successCount.get(), "Exactly 5 requests should succeed");
            assertEquals(5, failureCount.get(), "Exactly 5 requests should fail");

            Stock finalStock = getStock(testProductId);
            assertEquals(0, finalStock.getQuantity(), "Final stock should be 0");

        } finally {
            executor.shutdownNow();
        }
    }

    // Helper methods

    private void initializeStock(Long productId, Integer quantity) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/stock/init?productId={productId}&quantity={quantity}",
                null,
                String.class,
                productId,
                quantity
        );
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Stock initialization should succeed");
    }

    private Stock getStock(Long productId) {
        ResponseEntity<Stock> response = restTemplate.getForEntity(
                "/api/v1/stock/{productId}",
                Stock.class,
                productId
        );
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Get stock should succeed");
        assertNotNull(response.getBody(), "Response body should not be null");
        return response.getBody();
    }

    private ResponseEntity<String> decreaseStock(Long productId, Integer quantity) {
        return restTemplate.postForEntity(
                "/api/v1/stock/decrease?productId={productId}&quantity={quantity}",
                null,
                String.class,
                productId,
                quantity
        );
    }
}
