package com.example.blood.controller;

import com.example.blood.model.Donor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/donors")
@CrossOrigin("*")
public class DonorController {

    private Map<Long, Donor> donorDB = new HashMap<>();
    private AtomicLong idCounter = new AtomicLong(1);

    public DonorController() {
        addDemo("Asha", "A+", "Mumbai", "9999999999", 27);
        addDemo("Vikram", "O+", "Bengaluru", "8888888888", 30);
        addDemo("Lina", "B+", "Delhi", "7777777777", 25);
        addDemo("Rahul", "AB-", "Kolkata", "6666666666", 32);
    }

    private void addDemo(String name, String group, String city, String phone, int age) {
        Long id = idCounter.getAndIncrement();
        donorDB.put(id, new Donor(id, name, group, city, phone, age));
    }

    @GetMapping
    public List<Donor> getDonors(@RequestParam(name = "q", required = false) String q) {

        List<Donor> list = new ArrayList<>(donorDB.values());

        if (q == null || q.trim().isEmpty()) {
            return list;
        }

        String finalQ = q.toLowerCase();

        return list.stream()
                .filter(d ->
                        d.getName().toLowerCase().contains(finalQ) ||
                                d.getCity().toLowerCase().contains(finalQ) ||
                                d.getBloodGroup().toLowerCase().contains(finalQ)
                )
                .collect(Collectors.toList());
    }

    @PostMapping
    public Donor addDonor(@RequestBody Donor donor) {
        Long id = idCounter.getAndIncrement();
        donor.setId(id);
        donorDB.put(id, donor);
        return donor;
    }
}

