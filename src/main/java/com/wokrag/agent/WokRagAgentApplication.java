package com.wokrag.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WokRagAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(WokRagAgentApplication.class, args);
    }

}
