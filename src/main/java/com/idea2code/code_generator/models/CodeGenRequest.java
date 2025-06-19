package com.idea2code.code_generator.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class CodeGenRequest {
    private String openApiSpecUrl;
    private String language;
    private String dbType;
    private String outputFormat;
}
