package com.microservices.stockservice.controller;

import com.microservices.stockservice.model.Stock;
import com.microservices.stockservice.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stock")
public class StockController {

    @Autowired
    private StockService stockService;

    @PostMapping("/init")
    public ResponseEntity<String> initStock(@RequestParam Long productId, @RequestParam Integer quantity) {
        stockService.initializeStock(productId, quantity);
        return ResponseEntity.ok("Stock initialized successfully.");
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Stock> getStock(@PathVariable Long productId) {
        return ResponseEntity.ok(stockService.getStockByProductId(productId));
    }

    @PostMapping("/decrease")
    public ResponseEntity<String> decreaseStock(@RequestParam Long productId, @RequestParam Integer quantity) {
        try {
            stockService.decreaseQuantity(productId, quantity);
            return ResponseEntity.ok("Stock decreased successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
