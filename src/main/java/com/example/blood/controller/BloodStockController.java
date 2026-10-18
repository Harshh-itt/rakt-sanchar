package com.example.blood.controller;

import com.example.blood.dto.BloodStockResponse;
import com.example.blood.model.BloodGroup;
import com.example.blood.service.BloodStockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
public class BloodStockController {

    private final BloodStockService bloodStockService;

    public BloodStockController(BloodStockService bloodStockService) {
        this.bloodStockService = bloodStockService;
    }

    @GetMapping("/search")
    public List<BloodStockResponse> searchStock(
            @RequestParam(name = "bloodGroup", required = false) BloodGroup bloodGroup,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lng", required = false) Double lng,
            @RequestParam(name = "radiusKm", required = false) Double radiusKm
    ) {
        return bloodStockService.searchPublicStock(bloodGroup, city, lat, lng, radiusKm);
    }
}
