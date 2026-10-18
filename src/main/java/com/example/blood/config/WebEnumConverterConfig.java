package com.example.blood.config;

import com.example.blood.model.BloodGroup;
import com.example.blood.model.HospitalStatus;
import com.example.blood.model.RequestStatus;
import com.example.blood.model.Urgency;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebEnumConverterConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, BloodGroup>() {
            @Override
            public BloodGroup convert(String source) {
                if (source == null || source.isBlank()) {
                    return null;
                }
                return BloodGroup.fromValue(source);
            }
        });

        registry.addConverter(new Converter<String, Urgency>() {
            @Override
            public Urgency convert(String source) {
                if (source == null || source.isBlank()) {
                    return null;
                }
                return Urgency.fromValue(source);
            }
        });

        registry.addConverter(new Converter<String, HospitalStatus>() {
            @Override
            public HospitalStatus convert(String source) {
                if (source == null || source.isBlank()) {
                    return null;
                }
                return HospitalStatus.valueOf(source.trim().toUpperCase());
            }
        });

        registry.addConverter(new Converter<String, RequestStatus>() {
            @Override
            public RequestStatus convert(String source) {
                if (source == null || source.isBlank()) {
                    return null;
                }
                return RequestStatus.valueOf(source.trim().toUpperCase());
            }
        });
    }
}
