package com.microservices.stockservice.service;

import com.microservices.stockservice.model.Stock;
import com.microservices.stockservice.repository.StockRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class StockService {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private RedissonClient redissonClient;

    public void initializeStock(Long productId, Integer quantity) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElse(new Stock());
        stock.setProductId(productId);
        stock.setQuantity(quantity);
        stockRepository.save(stock);
    }

    public Stock getStockByProductId(Long productId) {
        return stockRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Product stock entry not found for ID: " + productId));
    }

    public void decreaseQuantity(Long productId, Integer quantity) {
        String lockKey = "lock:product:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (isLocked) {
                Stock stock = stockRepository.findByProductId(productId)
                        .orElseThrow(() -> new RuntimeException("Stock record not found for product ID: " + productId));

                if (stock.getQuantity() < quantity) {
                    throw new RuntimeException("Insufficient stock! Remaining: " + stock.getQuantity());
                }

                stock.setQuantity(stock.getQuantity() - quantity);
                stockRepository.save(stock);
            } else {
                throw new RuntimeException("System is busy processing this item, please try again.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Stock reduction operation interrupted.", e);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
