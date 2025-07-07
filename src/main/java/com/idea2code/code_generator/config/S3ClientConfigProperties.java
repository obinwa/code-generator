package com.idea2code.code_generator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
@ConfigurationProperties("s3")
@Setter
@Getter
public class S3ClientConfigProperties {
    private int writeTimeout = 0;
    private int maxConcurrency = 64;
    private boolean checksumValidationEnabled = false;
    private boolean chunkedEncodingEnabled = true;
    private String region = "eu-west-1";
    private URI endpoint;
    private String accessKeyId;
    private String secretAccessKey;
    private String bucket = "chidi-generated-code-files";
    private int MultipartMinSize = 5 * 1024 * 1024;
}
