package com.idea2code.code_generator.services;

import com.idea2code.code_generator.config.CodeGenProperties;
import com.idea2code.code_generator.exception.CodeGeneratorException;
import com.idea2code.code_generator.file.FileService;
import com.idea2code.code_generator.models.CodeGenRequest;
import com.idea2code.code_generator.models.CodeGenResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.idea2code.code_generator.utility.CodeGeneratorUtils.createFolder;


@Service
@Slf4j
public class CodeGeneratorService {
    private final CodeGenProperties props;
    private Path outputDirectory;
    private final  FileService fileService;


    public CodeGeneratorService(CodeGenProperties props, FileService fileService) {
        this.props = props;
        this.fileService = fileService;
    }

    @PostConstruct
    private void createOutputDirectory() {
        try {
            outputDirectory = fileService.createDirectory(props.getOutputDir());
        } catch (IOException exception) {
            log.info("Could not create output " + exception.getMessage());
            exception.printStackTrace();
            throw new CodeGeneratorException(HttpStatus.BAD_GATEWAY, "An error occurred, try again");
        }
    }

    public CodeGenResponse generateCode(MultipartFile openAPISpec, String language) throws IOException {
        //do validation
        File specFile = fileService.convertMultipartToFile(openAPISpec,outputDirectory);
        Path generatedCodeDirectory = convertSpecToCode(specFile.toPath(), language, outputDirectory);
        Path zippedCodeFile = fileService.zipFile(outputDirectory,generatedCodeDirectory);
        //delete specFile

        return CodeGenResponse.builder()
            .downloadUrl(zippedCodeFile.toString())
            .status("SUCCESS")
            .build();
    }

    protected Path convertSpecToCode(Path specFile, String language, Path outputDirectory){
        Path generatedCodeDir = outputDirectory.resolve("generated");
        CodegenConfigurator configurator = new CodegenConfigurator()
            .setInputSpec(specFile.toAbsolutePath().toString())
            .setGeneratorName(language)
            .setOutputDir(generatedCodeDir.toString());
        new DefaultGenerator().opts(configurator.toClientOptInput()).generate();
        return generatedCodeDir;
    }
}
