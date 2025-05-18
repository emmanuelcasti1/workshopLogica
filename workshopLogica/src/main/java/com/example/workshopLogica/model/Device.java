package com.example.workshopLogica.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class Device {
    private String id;
    private String type;
    private String status;
}
