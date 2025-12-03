package com.example.blood.controller;

import com.example.blood.model.BloodRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin("*")
public class BloodRequestController {

    private Map<Long, BloodRequest> requestDB = new HashMap<>();
    private AtomicLong idCounter = new AtomicLong(1);

    @PostMapping
    public BloodRequest addRequest(@RequestBody BloodRequest req) {
        Long id = idCounter.getAndIncrement();
        req.setId(id);
        requestDB.put(id, req);
        return req;
    }

    @GetMapping
    public List<BloodRequest> getRequests() {
        return new ArrayList<>(requestDB.values());
    }
}

