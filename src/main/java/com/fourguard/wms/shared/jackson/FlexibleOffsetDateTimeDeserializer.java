package com.fourguard.wms.shared.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FlexibleOffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

    @Override
    public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        text = text.trim();

        try {
            return OffsetDateTime.parse(text);
        } catch (DateTimeParseException e1) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return localDateTime.atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException e2) {
                try {
                    LocalDate localDate = LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
                    return localDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
                } catch (DateTimeParseException e3) {
                    throw ctxt.weirdStringException(text, OffsetDateTime.class,
                            "Formato de fecha inválido. Se espera ISO date (YYYY-MM-DD) o ISO offset (YYYY-MM-DDTHH:mm:ssZ)");
                }
            }
        }
    }
}
