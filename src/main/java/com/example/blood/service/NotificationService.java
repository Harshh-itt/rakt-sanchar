package com.example.blood.service;

import com.example.blood.dto.NotificationResponse;
import com.example.blood.dto.PageResponse;
import com.example.blood.event.BadgeEarnedEvent;
import com.example.blood.event.DonationVerifiedEvent;
import com.example.blood.event.DonorRespondedEvent;
import com.example.blood.event.HospitalVerificationEvent;
import com.example.blood.event.RequestCreatedEvent;
import com.example.blood.event.StockLowEvent;
import com.example.blood.exception.ApiException;
import com.example.blood.model.DonorProfile;
import com.example.blood.model.DonorResponseStatus;
import com.example.blood.model.Hospital;
import com.example.blood.model.HospitalStatus;
import com.example.blood.model.Notification;
import com.example.blood.model.NotificationType;
import com.example.blood.model.User;
import com.example.blood.repository.DonorProfileRepository;
import com.example.blood.repository.HospitalRepository;
import com.example.blood.repository.NotificationRepository;
import com.example.blood.repository.UserRepository;
import com.example.blood.security.AccessPolicy;
import com.example.blood.security.CurrentUser;
import com.example.blood.util.GeoUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class NotificationService {

    public static final int MAX_MATCH_NOTIFICATIONS_PER_REQUEST = 20;
    private static final double DEFAULT_NOTIFICATION_RADIUS_KM = 25.0;

    private final NotificationRepository notificationRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final CompatibilityService compatibilityService;
    private final EligibilityService eligibilityService;
    private final AccessPolicy accessPolicy;

    public NotificationService(
            NotificationRepository notificationRepository,
            DonorProfileRepository donorProfileRepository,
            HospitalRepository hospitalRepository,
            UserRepository userRepository,
            CompatibilityService compatibilityService,
            EligibilityService eligibilityService,
            AccessPolicy accessPolicy
    ) {
        this.notificationRepository = notificationRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
        this.compatibilityService = compatibilityService;
        this.eligibilityService = eligibilityService;
        this.accessPolicy = accessPolicy;
    }

    public PageResponse<NotificationResponse> getMyNotifications(CurrentUser currentUser, int page, int size) {
        accessPolicy.requireAuthenticated(currentUser);
        String lang = resolveLanguage(currentUser);
        List<NotificationResponse> items = notificationRepository.findByUserId(currentUser.userId())
                .stream()
                .map(n -> toResponse(n, lang))
                .toList();
        return PageResponse.of(items, page, size);
    }

    public long getUnreadCount(CurrentUser currentUser) {
        accessPolicy.requireAuthenticated(currentUser);
        return notificationRepository.countUnreadByUserId(currentUser.userId());
    }

    public NotificationResponse markAsRead(CurrentUser currentUser, String notificationId) {
        accessPolicy.requireAuthenticated(currentUser);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Notification not found with id: " + notificationId));
        accessPolicy.requireOwner(currentUser, notification.getUserId());

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return toResponse(saved, resolveLanguage(currentUser));
    }

    public int markAllAsRead(CurrentUser currentUser) {
        accessPolicy.requireAuthenticated(currentUser);
        return notificationRepository.markAllReadByUserId(currentUser.userId());
    }

    @EventListener
    public void onRequestCreated(RequestCreatedEvent event) {
        Double reqLat = null;
        Double reqLon = null;
        if (event.hospitalId() != null && !event.hospitalId().isBlank()) {
            Optional<Hospital> hospOpt = hospitalRepository.findById(event.hospitalId());
            if (hospOpt.isPresent()) {
                reqLat = hospOpt.get().getLatitude();
                reqLon = hospOpt.get().getLongitude();
            }
        }
        boolean requestHasCoords = GeoUtils.hasCoordinates(reqLat, reqLon);
        final Double finalLat = reqLat;
        final Double finalLon = reqLon;

        record Candidate(DonorProfile donor, boolean exactMatch, Double distanceKm) {
        }

        List<Candidate> candidates = new ArrayList<>();
        for (DonorProfile donor : donorProfileRepository.findAll()) {
            if (donor.getUserId() == null || donor.getUserId().isBlank()) {
                continue;
            }
            if (donor.getUserId().equals(event.requesterUserId())) {
                continue;
            }
            if (!donor.isAvailable()) {
                continue;
            }
            if (!compatibilityService.canDonateTo(donor.getBloodGroup(), event.bloodGroup())) {
                continue;
            }
            if (!eligibilityService.evaluate(donor).eligible()) {
                continue;
            }

            boolean cityMatches = event.city() != null && donor.getCity() != null
                    && event.city().trim().equalsIgnoreCase(donor.getCity().trim());
            Double distanceKm = null;
            if (requestHasCoords && GeoUtils.hasCoordinates(donor.getLatitude(), donor.getLongitude())) {
                distanceKm = GeoUtils.haversineKm(finalLat, finalLon, donor.getLatitude(), donor.getLongitude());
            }

            if (!cityMatches && (distanceKm == null || distanceKm > DEFAULT_NOTIFICATION_RADIUS_KM)) {
                continue;
            }

            boolean exact = compatibilityService.isExactMatch(donor.getBloodGroup(), event.bloodGroup());
            candidates.add(new Candidate(donor, exact, distanceKm));
        }

        candidates.sort(
                Comparator.comparing(Candidate::exactMatch).reversed()
                        .thenComparing(Candidate::distanceKm, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(c -> c.donor().getId())
        );

        int limit = Math.min(MAX_MATCH_NOTIFICATIONS_PER_REQUEST, candidates.size());
        for (int i = 0; i < limit; i++) {
            DonorProfile donor = candidates.get(i).donor();
            Map<String, String> params = new HashMap<>();
            params.put("requestId", safe(event.requestId()));
            params.put("bloodGroup", event.bloodGroup() != null ? event.bloodGroup().getLabel() : "");
            params.put("urgency", event.urgency() != null ? event.urgency().name() : "");
            params.put("hospital", safe(event.hospitalName()));
            params.put("city", safe(event.city()));

            Notification notification = new Notification();
            notification.setUserId(donor.getUserId());
            notification.setType(NotificationType.REQUEST_MATCH);
            notification.setMessageKey("notification.request_match");
            notification.setParams(params);
            notification.setDedupKey("REQUEST_MATCH:" + event.requestId() + ":" + donor.getUserId());
            notification.setCreatedAt(event.occurredAt() != null ? event.occurredAt() : Instant.now());
            notificationRepository.save(notification);
        }
    }

    @EventListener
    public void onDonorResponded(DonorRespondedEvent event) {
        if (event.status() != DonorResponseStatus.ACCEPTED) {
            return;
        }
        Set<String> recipientUserIds = new LinkedHashSet<>();
        if (event.requesterUserId() != null && !event.requesterUserId().isBlank()) {
            recipientUserIds.add(event.requesterUserId());
        }
        if (event.hospitalId() != null && !event.hospitalId().isBlank()) {
            hospitalRepository.findById(event.hospitalId())
                    .map(Hospital::getOwnerUserId)
                    .filter(id -> id != null && !id.isBlank())
                    .ifPresent(recipientUserIds::add);
        }

        Map<String, String> params = new HashMap<>();
        params.put("requestId", safe(event.requestId()));
        params.put("donorUserId", safe(event.donorUserId()));
        params.put("donorName", safe(event.donorName()));

        for (String recipientId : recipientUserIds) {
            Notification notification = new Notification();
            notification.setUserId(recipientId);
            notification.setType(NotificationType.DONOR_ACCEPTED);
            notification.setMessageKey("notification.donor_accepted");
            notification.setParams(params);
            notification.setDedupKey("DONOR_ACCEPTED:" + event.requestId() + ":" + event.donorUserId());
            notification.setCreatedAt(event.occurredAt() != null ? event.occurredAt() : Instant.now());
            notificationRepository.save(notification);
        }
    }

    @EventListener
    public void onStockLow(StockLowEvent event) {
        if (event.hospitalId() == null) {
            return;
        }
        hospitalRepository.findById(event.hospitalId()).ifPresent(hospital -> {
            if (hospital.getOwnerUserId() == null || hospital.getOwnerUserId().isBlank()) {
                return;
            }
            Map<String, String> params = new HashMap<>();
            params.put("hospitalId", safe(event.hospitalId()));
            params.put("hospitalName", safe(event.hospitalName()));
            params.put("bloodGroup", event.bloodGroup() != null ? event.bloodGroup().getLabel() : "");
            params.put("units", String.valueOf(event.units()));
            params.put("threshold", String.valueOf(event.minThreshold()));

            Notification notification = new Notification();
            notification.setUserId(hospital.getOwnerUserId());
            notification.setType(NotificationType.STOCK_LOW);
            notification.setMessageKey("notification.stock_low");
            notification.setParams(params);
            notification.setCreatedAt(Instant.now());
            notificationRepository.save(notification);
        });
    }

    @EventListener
    public void onDonationVerified(DonationVerifiedEvent event) {
        if (event.donorUserId() == null || event.donorUserId().isBlank()) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("donationId", safe(event.donationId()));
        params.put("hospitalName", safe(event.hospitalName()));
        params.put("units", String.valueOf(event.units()));
        params.put("donatedAt", event.donatedAt() != null ? event.donatedAt().toString() : "");

        Notification notification = new Notification();
        notification.setUserId(event.donorUserId());
        notification.setType(NotificationType.DONATION_VERIFIED);
        notification.setMessageKey("notification.donation_verified");
        notification.setParams(params);
        notification.setDedupKey("DONATION_VERIFIED:" + event.donationId());
        notification.setCreatedAt(event.occurredAt() != null ? event.occurredAt() : Instant.now());
        notificationRepository.save(notification);
    }

    @EventListener
    public void onBadgeEarned(BadgeEarnedEvent event) {
        if (event.donorUserId() == null || event.donorUserId().isBlank() || event.badgeCode() == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("badgeCode", event.badgeCode().name());
        params.put("badgeTitle", safe(event.badgeTitle()));

        Notification notification = new Notification();
        notification.setUserId(event.donorUserId());
        notification.setType(NotificationType.BADGE_EARNED);
        notification.setMessageKey("notification.badge_earned");
        notification.setParams(params);
        notification.setDedupKey("BADGE_EARNED:" + event.donorUserId() + ":" + event.badgeCode().name());
        notification.setCreatedAt(event.occurredAt() != null ? event.occurredAt() : Instant.now());
        notificationRepository.save(notification);
    }

    @EventListener
    public void onHospitalVerification(HospitalVerificationEvent event) {
        if (event.ownerUserId() == null || event.ownerUserId().isBlank()) {
            return;
        }
        boolean approved = event.status() == HospitalStatus.VERIFIED;
        Map<String, String> params = new HashMap<>();
        params.put("hospitalId", safe(event.hospitalId()));
        params.put("hospitalName", safe(event.hospitalName()));
        if (event.rejectionReason() != null && !event.rejectionReason().isBlank()) {
            params.put("reason", event.rejectionReason());
        }

        Notification notification = new Notification();
        notification.setUserId(event.ownerUserId());
        notification.setType(approved ? NotificationType.HOSPITAL_APPROVED : NotificationType.HOSPITAL_REJECTED);
        notification.setMessageKey(approved ? "notification.hospital_approved" : "notification.hospital_rejected");
        notification.setParams(params);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
    }

    private NotificationResponse toResponse(Notification n, String lang) {
        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getType(),
                n.getMessageKey(),
                n.getParams() != null ? Map.copyOf(n.getParams()) : Map.of(),
                formatLocalizedMessage(n.getMessageKey(), n.getParams(), lang),
                n.isRead(),
                n.getCreatedAt()
        );
    }

    private String resolveLanguage(CurrentUser currentUser) {
        if (currentUser != null && currentUser.userId() != null) {
            Optional<User> userOpt = userRepository.findById(currentUser.userId());
            if (userOpt.isPresent() && userOpt.get().getPreferredLanguage() != null) {
                return userOpt.get().getPreferredLanguage();
            }
            if (currentUser.preferredLanguage() != null) {
                return currentUser.preferredLanguage();
            }
        }
        return "en";
    }

    public String formatLocalizedMessage(String key, Map<String, String> params, String lang) {
        Map<String, String> p = params != null ? params : Map.of();
        boolean hindi = "hi".equalsIgnoreCase(lang);

        if ("notification.request_match".equals(key)) {
            return hindi
                    ? String.format("%s (%s) में %s रक्त की %s आवश्यकता है।",
                    p.getOrDefault("hospital", "अस्पताल"),
                    p.getOrDefault("city", ""),
                    p.getOrDefault("bloodGroup", ""),
                    p.getOrDefault("urgency", "URGENT"))
                    : String.format("%s blood needed (%s) at %s in %s.",
                    p.getOrDefault("bloodGroup", ""),
                    p.getOrDefault("urgency", "URGENT"),
                    p.getOrDefault("hospital", "Hospital"),
                    p.getOrDefault("city", ""));
        }
        if ("notification.donor_accepted".equals(key)) {
            return hindi
                    ? String.format("रक्तदाता %s ने आपके रक्त अनुरोध को स्वीकार कर लिया है।",
                    p.getOrDefault("donorName", "एक रक्तदाता"))
                    : String.format("Donor %s accepted your blood request.",
                    p.getOrDefault("donorName", "A donor"));
        }
        if ("notification.stock_low".equals(key)) {
            return hindi
                    ? String.format("कम स्टॉक चेतावनी: %s रक्त समूह केवल %s यूनिट शेष है (न्यूनतम सीमा: %s)।",
                    p.getOrDefault("bloodGroup", ""),
                    p.getOrDefault("units", "0"),
                    p.getOrDefault("threshold", "0"))
                    : String.format("Low stock alert: %s has %s units remaining (threshold: %s).",
                    p.getOrDefault("bloodGroup", ""),
                    p.getOrDefault("units", "0"),
                    p.getOrDefault("threshold", "0"));
        }
        if ("notification.donation_verified".equals(key)) {
            return hindi
                    ? String.format("%s द्वारा आपका रक्तदान (%s यूनिट) सत्यापित किया गया। धन्यवाद!",
                    p.getOrDefault("hospitalName", "अस्पताल"),
                    p.getOrDefault("units", "1"))
                    : String.format("Your blood donation (%s unit(s)) at %s has been verified!",
                    p.getOrDefault("units", "1"),
                    p.getOrDefault("hospitalName", "the hospital"));
        }
        if ("notification.badge_earned".equals(key)) {
            return hindi
                    ? String.format("बधाई हो! आपने '%s' बैज अर्जित किया है।",
                    p.getOrDefault("badgeTitle", "Badge"))
                    : String.format("Congratulations! You earned the '%s' badge.",
                    p.getOrDefault("badgeTitle", "Badge"));
        }
        if ("notification.hospital_approved".equals(key)) {
            return hindi
                    ? String.format("आपका अस्पताल '%s' प्रशासन द्वारा सत्यापित हो गया है।",
                    p.getOrDefault("hospitalName", "Hospital"))
                    : String.format("Your hospital '%s' has been approved and verified.",
                    p.getOrDefault("hospitalName", "Hospital"));
        }
        if ("notification.hospital_rejected".equals(key)) {
            return hindi
                    ? String.format("अस्पताल सत्यापन अस्वीकृत (%s): %s",
                    p.getOrDefault("hospitalName", "Hospital"),
                    p.getOrDefault("reason", ""))
                    : String.format("Hospital verification rejected for '%s': %s",
                    p.getOrDefault("hospitalName", "Hospital"),
                    p.getOrDefault("reason", ""));
        }
        return key != null ? key : "";
    }

    private String safe(String val) {
        return val != null ? val : "";
    }
}
