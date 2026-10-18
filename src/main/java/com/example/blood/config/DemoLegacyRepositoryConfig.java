package com.example.blood.config;

import com.example.blood.model.Donor;
import com.example.blood.repository.DonorRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Provides an in-memory fallback implementation for the untouched legacy {@link DonorRepository}
 * when running under the {@code demo} profile without a MongoDB server.
 */
@Configuration
@Profile("demo")
public class DemoLegacyRepositoryConfig {

    @Bean
    @SuppressWarnings("unchecked")
    public DonorRepository demoDonorRepository() {
        Map<String, Donor> store = new ConcurrentHashMap<>();
        return (DonorRepository) Proxy.newProxyInstance(
                DonorRepository.class.getClassLoader(),
                new Class<?>[]{DonorRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    return switch (name) {
                        case "save" -> {
                            Donor donor = (Donor) args[0];
                            if (donor.getId() == null || donor.getId().isBlank()) {
                                donor.setId(UUID.randomUUID().toString());
                            }
                            store.put(donor.getId(), donor);
                            yield donor;
                        }
                        case "findAll" -> new ArrayList<>(store.values());
                        case "findById" -> Optional.ofNullable(store.get((String) args[0]));
                        case "deleteAll" -> {
                            store.clear();
                            yield null;
                        }
                        case "count" -> (long) store.size();
                        case "findByBloodGroupIgnoreCaseAndCityIgnoreCase" -> {
                            String bg = (String) args[0];
                            String city = (String) args[1];
                            yield store.values().stream()
                                    .filter(d -> d.getBloodGroup() != null && d.getBloodGroup().equalsIgnoreCase(bg))
                                    .filter(d -> d.getCity() != null && d.getCity().equalsIgnoreCase(city))
                                    .collect(Collectors.toList());
                        }
                        case "toString" -> "InMemoryDemoDonorRepositoryProxy";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException("Unsupported method in demo proxy: " + name);
                    };
                }
        );
    }
}
