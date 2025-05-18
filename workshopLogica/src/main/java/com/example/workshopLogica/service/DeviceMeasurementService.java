package com.example.workshopLogica.service;

import com.clearspring.analytics.stream.cardinality.HyperLogLog;
import com.example.workshopLogica.model.Device;
import com.example.workshopLogica.model.DeviceMeasurement;
import com.example.workshopLogica.model.Location;
import com.example.workshopLogica.model.Measurement;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class DeviceMeasurementService {

    @Autowired
    private ObjectMapper objectMapper;

    // --- Algoritmos probabilísticos ---
    private BloomFilter<String> locationBloomFilter =
            BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 10000);

    private PriorityQueue<MeasurementSample> minHashSamples =
            new PriorityQueue<>(Comparator.comparingInt(MeasurementSample::getHash));
    private final int SAMPLE_SIZE = 100;

    private HyperLogLog hyperLogLog = new HyperLogLog(0.01); // 1% error

    private double amsSum = 0;
    private int amsCount = 0;
    private double totalTemperature = 0;
    private int countTemperature = 0;

    // Map para gestionar dispositivos y mediciones por deviceId
    private Map<String, DeviceMeasurement> deviceMap = new HashMap<>();

    // Map para varianza por región (simple cuadrícula)
    private Map<String, List<Double>> regionTemperatures = new HashMap<>();

    // Configuración geográfica (límites del área)
    private static final double LAT_MIN = -90;
    private static final double LAT_MAX = 90;
    private static final double LON_MIN = -180;
    private static final double LON_MAX = 180;

    // Dividir el área en 10x10 regiones
    private static final int GRID_SIZE = 10;
    private final double latStep = (LAT_MAX - LAT_MIN) / GRID_SIZE;
    private final double lonStep = (LON_MAX - LON_MIN) / GRID_SIZE;

    // Lista de nuevos dispositivos agregados (para retorno o procesamiento futuro)
    private final List<DeviceMeasurement> newDevices = new ArrayList<>();

    public List<DeviceMeasurement> processMeasurements(List<DeviceMeasurement> measurements) {
        newDevices.clear();

        measurements.forEach(measurement -> {
            try {
                String prettyJson = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(measurement);
                System.out.println("Medición procesada:\n" + prettyJson);

                String areaKey = measurement.getLocation().toString();

                if (!locationBloomFilter.mightContain(areaKey)) {
                    locationBloomFilter.put(areaKey);

                    double temp = measurement.getMeasurement().getValue();
                    updateAMS(temp);
                    addToMinHashSample(measurement);
                    processAnomaly(measurement);

                    deviceMap.put(measurement.getDevice().getId(), measurement);

                    String regionKey = getRegionKey(measurement.getLocation().getLatitude(), measurement.getLocation().getLongitude());
                    regionTemperatures.putIfAbsent(regionKey, new ArrayList<>());
                    regionTemperatures.get(regionKey).add(temp);

                } else {
                    System.out.println("Zona ya procesada, omitiendo...");
                }

            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error al formatear JSON", e);
            }
        });

        System.out.println("Anomalías únicas estimadas: " + hyperLogLog.cardinality());
        System.out.println("Varianza aproximada global: " + estimateVariance());

        // Agregar dispositivos nuevos en regiones con alta varianza
        addDevicesBasedOnVariance();

        // Puedes retornar los nuevos dispositivos junto a las mediciones procesadas si quieres
        List<DeviceMeasurement> allMeasurements = new ArrayList<>(measurements);
        allMeasurements.addAll(newDevices);
        return allMeasurements;
    }

    private String getRegionKey(double lat, double lon) {
        int latIndex = Math.min(GRID_SIZE - 1, Math.max(0, (int) ((lat - LAT_MIN) / latStep)));
        int lonIndex = Math.min(GRID_SIZE - 1, Math.max(0, (int) ((lon - LON_MIN) / lonStep)));
        return latIndex + "_" + lonIndex;
    }

    private void addDevicesBasedOnVariance() {
        Random rand = new Random();

        for (Map.Entry<String, List<Double>> entry : regionTemperatures.entrySet()) {
            String region = entry.getKey();
            List<Double> temps = entry.getValue();

            double mean = temps.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double variance = temps.stream().mapToDouble(t -> (t - mean) * (t - mean)).average().orElse(0);

            double varianceThreshold = 10;

            if (variance > varianceThreshold) {
                double prob = Math.min(1.0, variance / 100);

                if (rand.nextDouble() < prob) {
                    String[] parts = region.split("_");
                    int latIndex = Integer.parseInt(parts[0]);
                    int lonIndex = Integer.parseInt(parts[1]);

                    double newLat = LAT_MIN + latIndex * latStep + rand.nextDouble() * latStep;
                    double newLon = LON_MIN + lonIndex * lonStep + rand.nextDouble() * lonStep;

                    String newDeviceId = UUID.randomUUID().toString();

                    System.out.println("Agregando nuevo dispositivo en región " + region +
                            " con id " + newDeviceId +
                            " en lat " + newLat + ", lon " + newLon +
                            " debido a alta varianza: " + variance);

                    DeviceMeasurement newDevice = new DeviceMeasurement();
                    newDevice.setDevice(new Device(newDeviceId, null, null));
                    newDevice.setLocation(new Location(newLat, newLon));
                    newDevice.setMeasurement(new Measurement(0, "C"));
                    newDevices.add(newDevice);
                    deviceMap.put(newDeviceId, newDevice);
                    regionTemperatures.get(region).add(0.0);
                }
            }
        }
    }

    // MinWise Sampling
    private void addToMinHashSample(DeviceMeasurement m) {
        int hash = m.hashCode();
        if (minHashSamples.size() < SAMPLE_SIZE) {
            minHashSamples.add(new MeasurementSample(m, hash));
        } else if (minHashSamples.peek().getHash() < hash) {
            minHashSamples.poll();
            minHashSamples.add(new MeasurementSample(m, hash));
        }
    }

    private static class MeasurementSample {
        private final DeviceMeasurement measurement;
        private final int hash;

        public MeasurementSample(DeviceMeasurement measurement, int hash) {
            this.measurement = measurement;
            this.hash = hash;
        }

        public int getHash() {
            return hash;
        }

        public DeviceMeasurement getMeasurement() {
            return measurement;
        }
    }

    // HyperLogLog
    private void processAnomaly(DeviceMeasurement m) {
        if (isAnomaly(m)) {
            hyperLogLog.offer(m.getMeasurement().toString());
        }
    }

    private boolean isAnomaly(DeviceMeasurement m) {
        double val = m.getMeasurement().getValue();
        return val > 50 || val < -10;
    }

    // AMS Variance Estimation
    private void updateAMS(double temp) {
        double estimate = temp * temp;
        amsSum += estimate;
        amsCount++;
        totalTemperature += temp;
        countTemperature++;
    }

    private double estimateVariance() {
        if (amsCount == 0 || countTemperature == 0) return 0;
        double mean = totalTemperature / countTemperature;
        return (amsSum / amsCount) - (mean * mean);
    }
}
