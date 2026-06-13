package com.closetly.closetly_backend.location.service;

import com.closetly.closetly_backend.location.dto.LocationDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
public class LocationService {

    @Value("${opencage.api.key}")
    private String opencageApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LocationService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public LocationDTO reverseGeocode(Double lat, Double lng) {
        String url = String.format("https://api.opencagedata.com/geocode/v1/json?q=%s,%s&key=%s", lat, lng, opencageApiKey);
        
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode firstResult = root.path("results").get(0);
            
            if (firstResult == null || firstResult.isMissingNode()) {
                throw new RuntimeException("Reverse geocoding failed - no results");
            }
            
            JsonNode components = firstResult.path("components");
            JsonNode geometry = firstResult.path("geometry");
            
            LocationDTO location = new LocationDTO();
            location.setLat(lat);
            location.setLng(lng);
            location.setLatitude(lat);
            location.setLongitude(lng);
            location.setCity(components.path("city").asText(
                    components.path("town").asText(
                            components.path("village").asText(
                                    components.path("county").asText("")))));
            location.setState(components.path("state").asText(null));
            location.setCountry(components.path("country").asText(null));
            
            return location;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse OpenCage API response", e);
        }
    }

    public LocationDTO searchCity(String query) {
        String url = String.format("https://api.opencagedata.com/geocode/v1/json?q=%s&key=%s", 
                java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8), opencageApiKey);
        
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode firstResult = root.path("results").get(0);
            
            if (firstResult == null || firstResult.isMissingNode()) {
                throw new RuntimeException("Location search returned no results");
            }
            
            JsonNode components = firstResult.path("components");
            JsonNode geometry = firstResult.path("geometry");
            
            Double lat = geometry.path("lat").asDouble();
            Double lng = geometry.path("lng").asDouble();
            
            if (lat == null || lng == null) {
                throw new RuntimeException("Geocoding result is missing coordinates");
            }
            
            LocationDTO location = new LocationDTO();
            location.setLat(lat);
            location.setLng(lng);
            location.setLatitude(lat);
            location.setLongitude(lng);
            location.setCity(components.path("city").asText(
                    components.path("town").asText(
                            components.path("village").asText(
                                    components.path("county").asText(query)))));
            location.setState(components.path("state").asText(null));
            location.setCountry(components.path("country").asText(null));
            
            return location;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse OpenCage API response", e);
        }
    }
}
