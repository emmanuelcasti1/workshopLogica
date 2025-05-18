package com.example.workshopLogica.controller;

import com.example.workshopLogica.model.DeviceMeasurement;
import com.example.workshopLogica.service.DeviceMeasurementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
