package com.closetly.closetly_backend.location.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO {
    private Double lat;
    private Double lng;
    private Double latitude;
    private Double longitude;
    private String city;
    private String state;
    private String country;
}
