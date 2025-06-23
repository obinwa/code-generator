package com.idea2code.code_generator.config;

import io.netty.handler.logging.LogLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Configuration
@ConfigurationProperties("codegen")
@Setter
@Getter
public class CodeGenProperties {
    private String outputDir = "/codegen";
    private String inputFile = "spec.yaml";
    private String outputFile = "output.zip";
    private int connectTimeoutMs;
    private int readTimeoutMs;
    private int writeTimeoutMs;
}
