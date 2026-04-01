package com.helma.helmabackend.controller;

import com.helma.helmabackend.dto.PredictionRequest;
import com.helma.helmabackend.dto.PredictionResult;
import com.helma.helmabackend.service.FlaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class PredictionController {
    @Autowired
    private FlaskService flaskService;

    @PostMapping("/predict")
    public ResponseEntity<PredictionResult> predict(@RequestBody PredictionRequest req) {
        PredictionResult result = flaskService.predict(req);
        return ResponseEntity.ok(result);
    }

}
