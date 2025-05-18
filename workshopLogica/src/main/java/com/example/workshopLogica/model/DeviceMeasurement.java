package com.example.workshopLogica.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class DeviceMeasurement {
    @JsonProperty("_id")
    private String id;
    private Device device;
    private Measurement measurement;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss XXX")
    private OffsetDateTime measurementTime;
    private Location location;
}
