package com.tms.backend.converter;

import java.time.ZoneId;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ZoneIdConverter implements AttributeConverter<ZoneId, String> {

    @Override
    public String convertToDatabaseColumn(ZoneId zoneId) {
        return zoneId != null ? zoneId.getId() : null;
    }

    @Override
    public ZoneId convertToEntityAttribute(String dbData) {
        // return dbData != null ? ZoneId.of(dbData) : null;
        if (dbData == null || dbData.isBlank()) {
            return null; // or ZoneId.systemDefault()
        }
        return ZoneId.of(dbData);
    }
}