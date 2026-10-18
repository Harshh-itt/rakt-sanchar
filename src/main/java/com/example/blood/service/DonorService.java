package com.example.blood.service;

import com.example.blood.dto.DonorCreateRequest;
import com.example.blood.dto.DonorResponse;
import com.example.blood.dto.PageResponse;
import com.example.blood.exception.ApiException;
import com.example.blood.exception.ErrorCode;
import com.example.blood.mapper.DonorMapper;
import com.example.blood.model.BloodGroup;
import com.example.blood.model.DonorProfile;
import com.example.blood.repository.DonorProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;

@Service
public class DonorService {

    private final DonorProfileRepository donorProfileRepository;
    private final DonorMapper donorMapper;
    private final int minAge;
    private final int maxAge;

    public DonorService(
            DonorProfileRepository donorProfileRepository,
            DonorMapper donorMapper,
            @Value("${app.eligibility.min-age:18}") int minAge,
            @Value("${app.eligibility.max-age:65}") int maxAge
    ) {
        this.donorProfileRepository = donorProfileRepository;
        this.donorMapper = donorMapper;
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    public DonorResponse createDonor(DonorCreateRequest request) {
        int resolvedAge = resolveAndValidateAge(request.age(), request.dateOfBirth());
        LocalDate resolvedDob = request.dateOfBirth() != null
                ? request.dateOfBirth()
                : LocalDate.now().minusYears(resolvedAge);

        DonorProfile profile = new DonorProfile();
        profile.setName(request.name().trim());
        profile.setBloodGroup(request.bloodGroup());
        profile.setCity(request.city().trim());
        profile.setPhone(request.phone().trim());
        profile.setAge(resolvedAge);
        profile.setDateOfBirth(resolvedDob);
        profile.setLatitude(request.latitude());
        profile.setLongitude(request.longitude());
        profile.setAvailable(request.available() == null || request.available());
        profile.setDeclaredLastDonationDate(request.declaredLastDonationDate());
        profile.setInternalAuditNote("CREATED_VIA_API");

        DonorProfile saved = donorProfileRepository.save(profile);
        return donorMapper.toResponse(saved, false, true, null, "ELIGIBLE");
    }

    public PageResponse<DonorResponse> searchDonors(
            BloodGroup bloodGroup,
            String city,
            Boolean available,
            int page,
            int size
    ) {
        List<DonorResponse> filtered = donorProfileRepository.search(bloodGroup, city, available)
                .stream()
                .map(d -> donorMapper.toResponse(d, false, true, null, "ELIGIBLE"))
                .toList();
        return PageResponse.of(filtered, page, size);
    }

    public DonorResponse getDonorById(String id) {
        DonorProfile profile = donorProfileRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Donor not found with id: " + id));
        return donorMapper.toResponse(profile, false, true, null, "ELIGIBLE");
    }

    private int resolveAndValidateAge(Integer age, LocalDate dob) {
        Integer computedAge = age;
        if (dob != null) {
            if (dob.isAfter(LocalDate.now())) {
                throw new ApiException(
                        ErrorCode.VALIDATION_ERROR,
                        HttpStatus.BAD_REQUEST,
                        "Date of birth cannot be in the future",
                        Map.of("fields", Map.of("dateOfBirth", "Date of birth cannot be in the future"))
                );
            }
            computedAge = Period.between(dob, LocalDate.now()).getYears();
        }
        if (computedAge == null) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Either age or dateOfBirth is required",
                    Map.of("fields", Map.of("age", "Either age or dateOfBirth is required"))
            );
        }
        if (computedAge < minAge || computedAge > maxAge) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Donor age must be between " + minAge + " and " + maxAge + " years",
                    Map.of("fields", Map.of("age", "Age must be between " + minAge + " and " + maxAge))
            );
        }
        return computedAge;
    }
}
