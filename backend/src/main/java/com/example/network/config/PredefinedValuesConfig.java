package com.example.network.config;

import com.example.network.dto.PredefinedValueDto;
import com.example.network.dto.PredefinedValuesDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class PredefinedValuesConfig {

    @Bean
    public PredefinedValuesDto predefinedValuesDto() {
        PredefinedValuesDto values = new PredefinedValuesDto();
        values.setTemperature(Arrays.asList(
                new PredefinedValueDto("4", "Refrigerated"),
                new PredefinedValueDto("8", "Cool"),
                new PredefinedValueDto("12", "Room Temperature")
        ));
        values.setSize(Arrays.asList(
                new PredefinedValueDto("10", "Small"),
                new PredefinedValueDto("15", "Medium"),
                new PredefinedValueDto("20", "Large")
        ));
        values.setStatus(Arrays.asList(
                new PredefinedValueDto("free", "Available for use"),
                new PredefinedValueDto("busy", "Occupied or reserved")
        ));
        values.setCondition(Arrays.asList(
                new PredefinedValueDto("good", "Fully operational"),
                new PredefinedValueDto("dirty", "Needs cleaning"),
                new PredefinedValueDto("broken", "Not functioning")
        ));
        return values;
    }
}
