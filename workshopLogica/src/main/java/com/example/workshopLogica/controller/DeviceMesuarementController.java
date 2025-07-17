package com.example.workshopLogica.controller;

import com.example.workshopLogica.model.DeviceMeasurement;
import com.example.workshopLogica.service.DeviceMeasurementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/measurements")
public class DeviceMesuarementController {
    @Autowired
    private DeviceMeasurementService measurementService;

    @PostMapping()
    public ResponseEntity<?> processMeasurements(@RequestBody List<DeviceMeasurement> measurements) {
        List<DeviceMeasurement> processed = measurementService.processMeasurements(measurements);
        return ResponseEntity.ok(processed);
    }

    // Nuevo endpoint para recuperar la última medición de un dispositivo por ID
    @GetMapping("/device/{id}")
    public ResponseEntity<?> getDeviceMeasurement(@PathVariable String id) {
        DeviceMeasurement deviceMeasurement = measurementService.getDeviceMeasurementById(id);

        if (deviceMeasurement != null) {
            return ResponseEntity.ok(deviceMeasurement);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // PageRank
    @GetMapping("/pagerank")
    public ResponseEntity<Map<String, Double>> getPageRank(
            @RequestParam(defaultValue = "0.85") double damping,
            @RequestParam(defaultValue = "10") int iterations) {
        Map<String, Double> pageRanks = measurementService.calculatePageRank(damping, iterations);
        return ResponseEntity.ok(pageRanks);
    }

    // Markov Chain Stationary Distribution
    @GetMapping("/markov")
    public ResponseEntity<Map<String, Double>> getMarkovDistribution(
            @RequestParam(defaultValue = "20") double threshold) {
        Map<String, Double> distribution = measurementService.calculateMarkovStationaryDistribution(threshold);
        return ResponseEntity.ok(distribution);
    }

    // MapReduce de varianza regional
    @GetMapping("/variance")
    public ResponseEntity<Map<String, Double>> getRegionalVariance() {
        Map<String, Double> variances = measurementService.mapReduceRegionalVariance();
        return ResponseEntity.ok(variances);
    }

}
