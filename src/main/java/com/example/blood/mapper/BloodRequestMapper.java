package com.example.blood.mapper;

import com.example.blood.dto.BloodRequestDonorResponseDto;
import com.example.blood.dto.BloodRequestResponse;
import com.example.blood.model.BloodRequest;
import com.example.blood.model.DonorMatchResponse;
import com.example.blood.model.DonorProfile;
import com.example.blood.model.DonorResponseStatus;
import com.example.blood.repository.DonorProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class BloodRequestMapper {

    private final DonorProfileRepository donorProfileRepository;

    public BloodRequestMapper(DonorProfileRepository donorProfileRepository) {
        this.donorProfileRepository = donorProfileRepository;
    }

    public BloodRequestResponse toResponse(BloodRequest request) {
        return toResponse(request, false);
    }

    public BloodRequestResponse toResponse(BloodRequest request, boolean canViewAcceptedPhone) {
        List<BloodRequestDonorResponseDto> donorResponseDtos = request.getDonorResponses() != null
                ? request.getDonorResponses().values().stream()
                .sorted(Comparator.comparing(DonorMatchResponse::getRespondedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(dr -> toDonorResponseDto(dr, canViewAcceptedPhone))
                .toList()
                : List.of();

        return new BloodRequestResponse(
                request.getId(),
                request.getRequesterUserId(),
                request.getPatientName(),
                request.getBloodGroupEnum(),
                request.getUnitsNeeded(),
                request.getUnitsFulfilled(),
                request.getHospitalId(),
                request.getHospital(),
                request.getCity(),
                request.getHospitalLatitude(),
                request.getHospitalLongitude(),
                request.getUrgencyEnum(),
                request.getStatus(),
                request.getNeededBy(),
                donorResponseDtos,
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private BloodRequestDonorResponseDto toDonorResponseDto(DonorMatchResponse dr, boolean canViewAcceptedPhone) {
        Optional<DonorProfile> profileOpt = Optional.empty();
        if (dr.getDonorProfileId() != null && !dr.getDonorProfileId().isBlank()) {
            profileOpt = donorProfileRepository.findById(dr.getDonorProfileId());
        }
        if (profileOpt.isEmpty() && dr.getDonorUserId() != null && !dr.getDonorUserId().isBlank()) {
            profileOpt = donorProfileRepository.findByUserId(dr.getDonorUserId());
        }

        String donorName = profileOpt.map(DonorProfile::getName).orElse(dr.getDonorUserId());
        var bloodGroup = profileOpt.map(DonorProfile::getBloodGroup).orElse(null);
        String city = profileOpt.map(DonorProfile::getCity).orElse(null);
        String rawPhone = profileOpt.map(DonorProfile::getPhone).orElse(null);

        boolean revealPhone = canViewAcceptedPhone && dr.getStatus() == DonorResponseStatus.ACCEPTED;
        String phone = revealPhone ? rawPhone : DonorMapper.maskPhone(rawPhone);

        return new BloodRequestDonorResponseDto(
                dr.getDonorUserId(),
                dr.getDonorProfileId(),
                donorName,
                bloodGroup,
                city,
                phone,
                dr.getStatus(),
                dr.getRespondedAt()
        );
    }
}
