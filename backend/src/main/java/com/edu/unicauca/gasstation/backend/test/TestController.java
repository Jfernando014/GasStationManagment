package com.edu.unicauca.gasstation.backend.test;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
* @author javiersolanop777
*/
@RestController
@RequestMapping("/test")
public class TestController {
    
    @GetMapping("/ping")
    public ResponseEntity<String> ping()
    {
        return ResponseEntity.ok("Service Up!");
    }
}
