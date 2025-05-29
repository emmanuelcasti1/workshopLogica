package com.example.workshopLogica.controller;

import com.example.workshopLogica.model.DeviceMeasurement;
import com.example.workshopLogica.service.DeviceMeasurementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
