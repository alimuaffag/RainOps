package com.rainops.incident.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CreateIncidentRequestTests {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private CreateIncidentRequest validRequest() {
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("Water accumulation");
        request.setDescription("Water blocks the entrance");
        request.setDistrict("Al Nuzhah");
        request.setLocationDescription("School entrance");
        return request;
    }

    @Test
    void acceptsRequiredFieldsWithoutOptionalCoordinatesOrPriority() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    @Test
    void rejectsBlankTitle() {
        CreateIncidentRequest request = validRequest();
        request.setTitle("   ");
        assertThat(validator.validate(request))
                .anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    void validatesCoordinatePairRangeAndPrecision() {
        CreateIncidentRequest request = validRequest();
        request.setLatitude(new BigDecimal("24.713552"));
        assertThat(validator.validate(request))
                .anyMatch(v -> v.getPropertyPath().toString().equals("coordinatesComplete"));

        request.setLongitude(new BigDecimal("46.675296"));
        assertThat(validator.validate(request)).isEmpty();

        request.setLatitude(new BigDecimal("91"));
        assertThat(validator.validate(request))
                .anyMatch(v -> v.getPropertyPath().toString().equals("latitude"));

        request.setLatitude(new BigDecimal("24.1234567"));
        assertThat(validator.validate(request))
                .anyMatch(v -> v.getPropertyPath().toString().equals("latitude"));
    }
}
