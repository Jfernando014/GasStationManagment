package com.edu.unicauca.gasstation.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;

@Modulith
@SpringBootApplication
public class GasStationApplication {
    public static void main(String[] args) {
        SpringApplication.run(GasStationApplication.class, args);
    }
}
