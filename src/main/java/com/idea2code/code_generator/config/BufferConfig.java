package com.idea2code.code_generator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;

@Configuration
public class BufferConfig {

  @Bean
  public DataBufferFactory dataBufferFactory() {
    return new DefaultDataBufferFactory();
  }
}
