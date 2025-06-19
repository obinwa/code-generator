package com.idea2code.code_generator.services;


import com.idea2code.code_generator.CodeGeneratorApplication;
import com.idea2code.code_generator.config.CodeGenProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.util.AssertionErrors.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = CodeGeneratorApplication.class)
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
class CodeGeneratorServiceTest {
    @Autowired
    private CodeGeneratorService codeGeneratorService;
    @Autowired
    private CodeGenProperties props;

    @TempDir
    Path tempDir;


    @Test
    void testGetter() {
        CodeGenProperties properties = new CodeGenProperties();
        properties.setOutputDir("Hello");
        assertEquals("Success","Hello", properties.getOutputDir());
    }

    @Test
    void testConvertSpecToCode_generateCode() throws IOException {
        Path specFile = tempDir.resolve("openapi.yaml");
        try(InputStream inputStream = getClass().getClassLoader().getResourceAsStream("openapi.yaml")){
            assertThat(inputStream).isNotNull();
            Files.copy(inputStream,specFile);
        }

        Path generatedCodeDir = codeGeneratorService.convertSpecToCode(specFile,"java",tempDir);

        assertThat(generatedCodeDir).exists();
        assertThat(generatedCodeDir.resolve("src")).exists();
    }
}

