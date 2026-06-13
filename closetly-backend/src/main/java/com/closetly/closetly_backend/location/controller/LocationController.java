package com.closetly.closetly_backend.location.controller;

import com.closetly.closetly_backend.location.dto.LocationDTO;
import com.closetly.closetly_backend.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/reverse-geocode")
    public ResponseEntity<LocationDTO> reverseGeocode(
            @RequestParam Double lat,
            @RequestParam Double lng) {
        LocationDTO location = locationService.reverseGeocode(lat, lng);
        return ResponseEntity.ok(location);
    }

    @GetMapping("/search-city")
    public ResponseEntity<LocationDTO> searchCity(@RequestParam String query) {
        LocationDTO location = locationService.searchCity(query);
        return ResponseEntity.ok(location);
    }
}
