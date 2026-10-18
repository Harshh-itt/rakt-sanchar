package com.example.blood.service;

import com.example.blood.dto.BloodStockResponse;
import com.example.blood.dto.BloodStockUpdateRequest;
import com.example.blood.event.StockLowEvent;
import com.example.blood.exception.ApiException;
import com.example.blood.exception.ErrorCode;
import com.example.blood.model.BloodGroup;
import com.example.blood.model.BloodStock;
import com.example.blood.model.Hospital;
import com.example.blood.model.HospitalStatus;
import com.example.blood.model.Role;
import com.example.blood.repository.BloodStockRepository;
import com.example.blood.repository.HospitalRepository;
import com.example.blood.security.AccessPolicy;
import com.example.blood.security.CurrentUser;
import com.example.blood.util.GeoUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class BloodStockService {

    private static final Duration STALE_THRESHOLD = Duration.ofHours(24);

    private final BloodStockRepository bloodStockRepository;
    private final HospitalRepository hospitalRepository;
    private final AccessPolicy accessPolicy;
    private final ApplicationEventPublisher eventPublisher;

    public BloodStockService(
            BloodStockRepository bloodStockRepository,
            HospitalRepository hospitalRepository,
            AccessPolicy accessPolicy,
            ApplicationEventPublisher eventPublisher
    ) {
        this.bloodStockRepository = bloodStockRepository;
        this.hospitalRepository = hospitalRepository;
        this.accessPolicy = accessPolicy;
        this.eventPublisher = eventPublisher;
    }

    public List<BloodStockResponse> getMyHospitalStock(CurrentUser currentUser) {
        Hospital hospital = requireOwnedHospital(currentUser);
        return bloodStockRepository.findByHospitalId(hospital.getId())
                .stream()
                .sorted(Comparator.comparing(s -> s.getBloodGroup().ordinal()))
                .map(stock -> toResponse(stock, hospital, null))
                .toList();
    }

    public BloodStockResponse upsertMyHospitalStock(
            CurrentUser currentUser,
            BloodGroup bloodGroup,
            BloodStockUpdateRequest request
    ) {
        Hospital hospital = requireVerifiedOwnedHospital(currentUser);

        BloodStock stock = bloodStockRepository
                .findByHospitalIdAndBloodGroup(hospital.getId(), bloodGroup)
                .orElseGet(() -> {
                    BloodStock s = new BloodStock();
                    s.setHospitalId(hospital.getId());
                    s.setBloodGroup(bloodGroup);
                    s.setMinThreshold(5);
                    return s;
                });

        stock.setUnits(request.units());
        if (request.minThreshold() != null) {
            stock.setMinThreshold(request.minThreshold());
        }
        stock.setUpdatedAt(Instant.now());

        BloodStock saved = bloodStockRepository.save(stock);

        if (saved.getUnits() <= saved.getMinThreshold()) {
            eventPublisher.publishEvent(new StockLowEvent(
                    hospital.getId(),
                    hospital.getName(),
                    hospital.getCity(),
                    hospital.getLatitude(),
                    hospital.getLongitude(),
                    saved.getBloodGroup(),
                    saved.getUnits(),
                    saved.getMinThreshold()
            ));
        }

        return toResponse(saved, hospital, null);
    }

    public void deleteMyHospitalStock(CurrentUser currentUser, BloodGroup bloodGroup) {
        Hospital hospital = requireVerifiedOwnedHospital(currentUser);
        bloodStockRepository.deleteByHospitalIdAndBloodGroup(hospital.getId(), bloodGroup);
    }

    public List<BloodStockResponse> searchPublicStock(
            BloodGroup bloodGroup,
            String city,
            Double lat,
            Double lng,
            Double radiusKm
    ) {
        List<BloodStockResponse> results = new ArrayList<>();
        for (BloodStock stock : bloodStockRepository.findAll()) {
            if (stock.getUnits() <= 0) {
                continue;
            }
            if (bloodGroup != null && stock.getBloodGroup() != bloodGroup) {
                continue;
            }
            Optional<Hospital> hospitalOpt = hospitalRepository.findById(stock.getHospitalId());
            if (hospitalOpt.isEmpty()) {
                continue;
            }
            Hospital hospital = hospitalOpt.get();
            if (hospital.getStatus() != HospitalStatus.VERIFIED) {
                continue;
            }
            if (city != null && !city.isBlank()) {
                if (hospital.getCity() == null || !hospital.getCity().equalsIgnoreCase(city.trim())) {
                    continue;
                }
            }

            Double distanceKm = null;
            if (lat != null && lng != null && hospital.getLatitude() != null && hospital.getLongitude() != null) {
                distanceKm = Math.round(GeoUtils.haversineKm(lat, lng, hospital.getLatitude(), hospital.getLongitude()) * 100.0) / 100.0;
                if (radiusKm != null && radiusKm > 0 && distanceKm > radiusKm) {
                    continue;
                }
            }

            results.add(toResponse(stock, hospital, distanceKm));
        }

        results.sort(Comparator
                .comparing(BloodStockResponse::distanceKm, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Comparator.comparingInt(BloodStockResponse::units).reversed()));
        return results;
    }

    private Hospital requireOwnedHospital(CurrentUser currentUser) {
        accessPolicy.requireRole(currentUser, Role.HOSPITAL);
        Hospital hospital = hospitalRepository.findByOwnerUserId(currentUser.userId())
                .orElseThrow(() -> ApiException.notFound("Hospital profile not found for current user"));
        accessPolicy.requireOwner(currentUser, hospital.getOwnerUserId());
        return hospital;
    }

    private Hospital requireVerifiedOwnedHospital(CurrentUser currentUser) {
        Hospital hospital = requireOwnedHospital(currentUser);
        if (hospital.getStatus() != HospitalStatus.VERIFIED) {
            throw new ApiException(ErrorCode.HOSPITAL_NOT_VERIFIED,
                    "Only VERIFIED hospitals can manage blood stock (current status: " + hospital.getStatus() + ")");
        }
        return hospital;
    }

    private BloodStockResponse toResponse(BloodStock stock, Hospital hospital, Double distanceKm) {
        boolean lowStock = stock.getUnits() <= stock.getMinThreshold();
        boolean stale = stock.getUpdatedAt() != null
                && stock.getUpdatedAt().isBefore(Instant.now().minus(STALE_THRESHOLD));
        return new BloodStockResponse(
                stock.getId(),
                hospital.getId(),
                hospital.getName(),
                hospital.getCity(),
                hospital.getPhone(),
                stock.getBloodGroup(),
                stock.getUnits(),
                stock.getMinThreshold(),
                lowStock,
                stale,
                distanceKm,
                stock.getUpdatedAt()
        );
    }
}
