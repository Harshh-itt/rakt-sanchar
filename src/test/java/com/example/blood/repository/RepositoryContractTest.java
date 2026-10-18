package com.example.blood.repository;

import com.example.blood.exception.ApiException;
import com.example.blood.model.BloodGroup;
import com.example.blood.model.BloodRequest;
import com.example.blood.model.BloodStock;
import com.example.blood.model.Donation;
import com.example.blood.model.DonationStatus;
import com.example.blood.model.DonorProfile;
import com.example.blood.model.Hospital;
import com.example.blood.model.HospitalStatus;
import com.example.blood.model.Notification;
import com.example.blood.model.NotificationType;
import com.example.blood.model.RequestStatus;
import com.example.blood.model.Urgency;
import com.example.blood.util.GeoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests written against the repository interfaces so any future persistent
 * database implementation can run through the exact same suite.
 */
class RepositoryContractTest {

    private DonorProfileRepository donorProfileRepository;
    private HospitalRepository hospitalRepository;
    private BloodStockRepository bloodStockRepository;
    private BloodRequestRepository bloodRequestRepository;
    private DonationRepository donationRepository;
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        donorProfileRepository = new InMemoryDonorProfileRepository();
        hospitalRepository = new InMemoryHospitalRepository();
        bloodStockRepository = new InMemoryBloodStockRepository();
        bloodRequestRepository = new InMemoryBloodRequestRepository();
        donationRepository = new InMemoryDonationRepository();
        notificationRepository = new InMemoryNotificationRepository();
    }

    @Test
    void donorProfileRepositoryEnforcesUniqueUserIdAndCaseInsensitiveFilters() {
        DonorProfile p1 = new DonorProfile();
        p1.setUserId("user-1");
        p1.setName("Kiran Rao");
        p1.setBloodGroup(BloodGroup.O_NEG);
        p1.setCity("Bengaluru");
        p1.setPhone("9876543210");
        p1.setAvailable(true);
        donorProfileRepository.save(p1);

        assertNotNull(p1.getId());
        assertEquals(1, donorProfileRepository.search(BloodGroup.O_NEG, "bengaluru", true).size());
        assertEquals(1, donorProfileRepository.search(BloodGroup.O_NEG, null, null).size());
        assertEquals(1, donorProfileRepository.search(null, "BENGALURU", null).size());
        assertEquals(0, donorProfileRepository.search(BloodGroup.A_POS, "Bengaluru", null).size());

        DonorProfile duplicateForSameUser = new DonorProfile();
        duplicateForSameUser.setUserId("user-1");
        duplicateForSameUser.setName("Duplicate");
        duplicateForSameUser.setBloodGroup(BloodGroup.A_POS);
        duplicateForSameUser.setCity("Delhi");
        duplicateForSameUser.setPhone("9123456789");

        assertThrows(ApiException.class, () -> donorProfileRepository.save(duplicateForSameUser));
    }

    @Test
    void bloodStockRepositoryEnforcesOneEntryPerHospitalAndBloodGroup() {
        BloodStock s1 = new BloodStock(null, "hosp-1", BloodGroup.A_POS, 10, 3, Instant.now());
        bloodStockRepository.save(s1);
        assertNotNull(s1.getId());

        BloodStock duplicate = new BloodStock(null, "hosp-1", BloodGroup.A_POS, 5, 2, Instant.now());
        assertThrows(ApiException.class, () -> bloodStockRepository.save(duplicate));

        // Updating existing stock with same ID succeeds
        s1.setUnits(15);
        bloodStockRepository.save(s1);
        assertEquals(15, bloodStockRepository.findByHospitalIdAndBloodGroup("hosp-1", BloodGroup.A_POS).orElseThrow().getUnits());
    }

    @Test
    void hospitalRepositoryFiltersVerifiedAndPreventsDuplicateRegistrationNumber() {
        Hospital h1 = new Hospital();
        h1.setOwnerUserId("owner-1");
        h1.setName("Apollo Care");
        h1.setRegistrationNumber("REG-100");
        h1.setCity("Mumbai");
        h1.setStatus(HospitalStatus.VERIFIED);
        hospitalRepository.save(h1);

        Hospital h2 = new Hospital();
        h2.setOwnerUserId("owner-2");
        h2.setName("Pending Clinic");
        h2.setRegistrationNumber("REG-200");
        h2.setCity("Mumbai");
        h2.setStatus(HospitalStatus.PENDING);
        hospitalRepository.save(h2);

        List<Hospital> verifiedInMumbai = hospitalRepository.searchVerified("mumbai", null);
        assertEquals(1, verifiedInMumbai.size());
        assertEquals("Apollo Care", verifiedInMumbai.get(0).getName());

        Hospital duplicateReg = new Hospital();
        duplicateReg.setOwnerUserId("owner-3");
        duplicateReg.setName("Other");
        duplicateReg.setRegistrationNumber("reg-100");
        assertThrows(ApiException.class, () -> hospitalRepository.save(duplicateReg));
    }

    @Test
    void bloodRequestRepositorySupportsAtomicMutationAndPriorityOrdering() {
        BloodRequest r1 = new BloodRequest("Patient 1", "A+", "Hosp A", "Pune", "Low");
        r1.setUnitsNeeded(2);
        bloodRequestRepository.save(r1);

        BloodRequest r2 = new BloodRequest("Patient 2", "A+", "Hosp B", "Pune", "CRITICAL");
        r2.setUnitsNeeded(1);
        bloodRequestRepository.save(r2);

        List<BloodRequest> results = bloodRequestRepository.search("pune", BloodGroup.A_POS, null, RequestStatus.OPEN);
        assertEquals(2, results.size());
        assertEquals("Patient 2", results.get(0).getPatientName()); // CRITICAL ranked first

        BloodRequest mutated = bloodRequestRepository.computeAtomically(r1.getId(), req -> {
            req.setUnitsFulfilled(req.getUnitsFulfilled() + 2);
            req.setStatus(RequestStatus.FULFILLED);
            return req;
        });
        assertEquals(RequestStatus.FULFILLED, mutated.getStatus());
        assertEquals(2, mutated.getUnitsFulfilled());
    }

    @Test
    void notificationRepositoryDeduplicatesByUserIdAndDedupKey() {
        Notification n1 = new Notification();
        n1.setUserId("donor-1");
        n1.setType(NotificationType.REQUEST_MATCH);
        n1.setMessageKey("notification.request.match");
        n1.setParams(Map.of("bloodGroup", "A+"));
        n1.setDedupKey("req-1:donor-1:REQUEST_MATCH");

        Notification saved1 = notificationRepository.save(n1);

        Notification n2 = new Notification();
        n2.setUserId("donor-1");
        n2.setType(NotificationType.REQUEST_MATCH);
        n2.setMessageKey("notification.request.match");
        n2.setDedupKey("req-1:donor-1:REQUEST_MATCH");

        Notification saved2 = notificationRepository.save(n2);
        assertEquals(saved1.getId(), saved2.getId());
        assertEquals(1, notificationRepository.findByUserId("donor-1").size());
        assertEquals(1, notificationRepository.countUnreadByUserId("donor-1"));

        notificationRepository.markAllReadByUserId("donor-1");
        assertEquals(0, notificationRepository.countUnreadByUserId("donor-1"));
    }

    @Test
    void donationRepositoryFiltersByDonorAndStatus() {
        Donation d1 = new Donation();
        d1.setDonorUserId("donor-u1");
        d1.setHospitalId("hosp-1");
        d1.setBloodGroup(BloodGroup.B_POS);
        d1.setDonatedAt(LocalDate.now().minusDays(10));
        d1.setStatus(DonationStatus.VERIFIED);
        donationRepository.save(d1);

        assertEquals(1, donationRepository.findByDonorUserIdAndStatus("donor-u1", DonationStatus.VERIFIED).size());
        assertEquals(0, donationRepository.findByDonorUserIdAndStatus("donor-u1", DonationStatus.PENDING).size());
    }

    @Test
    void geoUtilsCalculatesHaversineDistanceAccurately() {
        assertTrue(GeoUtils.hasCoordinates(12.9716, 77.5946));
        double distanceKm = GeoUtils.haversineKm(12.9716, 77.5946, 12.9352, 77.6245);
        assertTrue(distanceKm > 4.0 && distanceKm < 6.5, "Expected ~5.2 km between MG Road and Koramangala");
    }
}
