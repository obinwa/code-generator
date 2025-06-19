package com.idea2code.code_generator.utility;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class CodeGeneratorUtils {

    public static Path createFolder(String outputDir) throws IOException {
        String id = UUID.randomUUID().toString();
        Path tempDirectory = Paths.get(outputDir, id);
        Files.createDirectory(tempDirectory);
        return tempDirectory;
    }
}
