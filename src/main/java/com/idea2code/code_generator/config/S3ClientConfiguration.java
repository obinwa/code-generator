package com.idea2code.code_generator.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.time.Duration;

@Configuration
public class S3ClientConfiguration {

    @Bean
    public S3AsyncClient s3client(S3ClientConfigProperties s3props,
                                  AwsCredentialsProvider credentialsProvider){
        SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
                .writeTimeout(Duration.ofMillis(s3props.getWriteTimeout()))
                .maxConcurrency(s3props.getMaxConcurrency())
                .build();
        S3Configuration s3Configuration = S3Configuration.builder()
                .checksumValidationEnabled(s3props.isChecksumValidationEnabled())
                .chunkedEncodingEnabled(s3props.isChunkedEncodingEnabled())
                .build();
        S3AsyncClientBuilder asyncClientBuilder = S3AsyncClient.builder()
                .httpClient(httpClient)
                .region(Region.of(s3props.getRegion()))
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(s3Configuration);

        if(s3props.getEndpoint() != null){
            asyncClientBuilder = asyncClientBuilder.endpointOverride(s3props.getEndpoint());
        }
        return asyncClientBuilder.build();
    }

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(S3ClientConfigProperties s3props){
        if(StringUtils.isBlank(s3props.getAccessKeyId())){
            return DefaultCredentialsProvider.create();
        }else{
            return () -> {
                return AwsBasicCredentials.create(
                    s3props.getAccessKeyId(),
                    s3props.getSecretAccessKey()
                );
            };
        }
    }
}
