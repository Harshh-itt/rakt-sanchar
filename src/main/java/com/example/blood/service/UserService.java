package com.example.blood.service;

import com.example.blood.dto.LanguageUpdateRequest;
import com.example.blood.dto.UserOnboardingRequest;
import com.example.blood.dto.UserResponse;
import com.example.blood.exception.ApiException;
import com.example.blood.exception.ErrorCode;
import com.example.blood.model.DonorProfile;
import com.example.blood.model.Hospital;
import com.example.blood.model.HospitalStatus;
import com.example.blood.model.Role;
import com.example.blood.model.User;
import com.example.blood.repository.DonorProfileRepository;
import com.example.blood.repository.HospitalRepository;
import com.example.blood.repository.UserRepository;
import com.example.blood.security.AccessPolicy;
import com.example.blood.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final HospitalRepository hospitalRepository;
    private final AccessPolicy accessPolicy;

    public UserService(
            UserRepository userRepository,
            DonorProfileRepository donorProfileRepository,
            HospitalRepository hospitalRepository,
            AccessPolicy accessPolicy
    ) {
        this.userRepository = userRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.hospitalRepository = hospitalRepository;
        this.accessPolicy = accessPolicy;
    }

    public UserResponse getCurrentUserProfile(CurrentUser currentUser) {
        accessPolicy.requireAuthenticated(currentUser);
        User user = userRepository.findById(currentUser.userId())
                .orElseGet(() -> userRepository.save(new User(
                        currentUser.userId(),
                        currentUser.name(),
                        currentUser.role(),
                        currentUser.onboarded(),
                        currentUser.preferredLanguage(),
                        Instant.now()
                )));
        return toResponse(user);
    }

    public UserResponse completeOnboarding(CurrentUser currentUser, UserOnboardingRequest request) {
        accessPolicy.requireAuthenticated(currentUser);
        if (request.role() == Role.ADMIN) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Self-onboarding as ADMIN is not allowed");
        }

        User user = userRepository.findById(currentUser.userId())
                .orElseGet(() -> new User(
                        currentUser.userId(),
                        request.name().trim(),
                        request.role(),
                        false,
                        "en",
                        Instant.now()
                ));

        user.setName(request.name().trim());
        user.setRole(request.role());
        user.setOnboarded(true);
        if (request.preferredLanguage() != null && !request.preferredLanguage().isBlank()) {
            user.setPreferredLanguage(request.preferredLanguage().trim().toLowerCase(Locale.ROOT));
        }
        User savedUser = userRepository.save(user);

        if (request.role() == Role.DONOR && request.bloodGroup() != null) {
            upsertDonorProfileFromOnboarding(savedUser, request);
        } else if (request.role() == Role.HOSPITAL &&
                (request.hospitalName() != null || request.registrationNumber() != null || request.city() != null)) {
            upsertHospitalFromOnboarding(savedUser, request);
        }

        return toResponse(savedUser);
    }

    public UserResponse updateLanguage(CurrentUser currentUser, LanguageUpdateRequest request) {
        accessPolicy.requireAuthenticated(currentUser);
        User user = userRepository.findById(currentUser.userId())
                .orElseGet(() -> new User(
                        currentUser.userId(),
                        currentUser.name(),
                        currentUser.role(),
                        currentUser.onboarded(),
                        "en",
                        Instant.now()
                ));
        user.setPreferredLanguage(request.preferredLanguage().trim().toLowerCase(Locale.ROOT));
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public java.util.List<UserResponse> listAllUsers(CurrentUser currentUser) {
        accessPolicy.requireRole(currentUser, Role.ADMIN);
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private void upsertDonorProfileFromOnboarding(User user, UserOnboardingRequest request) {
        DonorProfile profile = donorProfileRepository.findByUserId(user.getId())
                .orElseGet(DonorProfile::new);
        profile.setUserId(user.getId());
        profile.setName(user.getName());
        profile.setBloodGroup(request.bloodGroup());
        if (request.dateOfBirth() != null) {
            profile.setDateOfBirth(request.dateOfBirth());
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            profile.setPhone(request.phone().trim());
        }
        if (request.city() != null && !request.city().isBlank()) {
            profile.setCity(request.city().trim());
        }
        if (request.pincode() != null && !request.pincode().isBlank()) {
            profile.setPincode(request.pincode().trim());
        }
        profile.setLatitude(request.latitude());
        profile.setLongitude(request.longitude());
        profile.setLastDonationDate(request.lastDonationDate());
        if (request.medicalConditionsConfirmed() != null) {
            profile.setMedicalConditionsConfirmed(request.medicalConditionsConfirmed());
        }
        if (request.available() != null) {
            profile.setAvailable(request.available());
        }
        donorProfileRepository.save(profile);
    }

    private void upsertHospitalFromOnboarding(User user, UserOnboardingRequest request) {
        Hospital hospital = hospitalRepository.findByOwnerUserId(user.getId())
                .orElseGet(Hospital::new);
        hospital.setOwnerUserId(user.getId());
        hospital.setName(request.hospitalName() != null && !request.hospitalName().isBlank()
                ? request.hospitalName().trim()
                : user.getName());
        if (request.registrationNumber() != null && !request.registrationNumber().isBlank()) {
            hospital.setRegistrationNumber(request.registrationNumber().trim());
        } else if (hospital.getRegistrationNumber() == null) {
            hospital.setRegistrationNumber("REG-" + user.getId().toUpperCase(Locale.ROOT));
        }
        hospital.setContactPerson(request.contactPerson() != null && !request.contactPerson().isBlank()
                ? request.contactPerson().trim()
                : user.getName());
        if (request.phone() != null && !request.phone().isBlank()) {
            hospital.setPhone(request.phone().trim());
        }
        if (request.address() != null && !request.address().isBlank()) {
            hospital.setAddress(request.address().trim());
        }
        if (request.city() != null && !request.city().isBlank()) {
            hospital.setCity(request.city().trim());
        }
        if (request.pincode() != null && !request.pincode().isBlank()) {
            hospital.setPincode(request.pincode().trim());
        }
        hospital.setLatitude(request.latitude());
        hospital.setLongitude(request.longitude());
        if (hospital.getStatus() == null) {
            hospital.setStatus(HospitalStatus.PENDING);
        }
        hospitalRepository.save(hospital);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getRole(),
                user.isOnboarded(),
                user.getPreferredLanguage(),
                user.getCreatedAt()
        );
    }
}
