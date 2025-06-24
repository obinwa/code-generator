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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.nio.file.*;


@Service
@Slf4j
public class CodeGeneratorService {
    private final CodeGenProperties props;
    private Path outputDirectory;
    private final FileService fileService;
    private final FileDownloader swaggerDownloader;


    public CodeGeneratorService(CodeGenProperties props, FileService fileService,FileDownloader swaggerDownloader) {
        this.props = props;
        this.fileService = fileService;
        this.swaggerDownloader = swaggerDownloader;
    }

    @PostConstruct
    private void createOutputDirectory() {
        outputDirectory = fileService.createDirectory(props.getOutputDir());
    }

    /**
     * Generates code from a multipart OpenAPI spec file.
     */
    public Mono<CodeGenResponse> generateCode(MultipartFile openAPISpec, String language) {
        return fileService.convertMultipartToFile(openAPISpec, outputDirectory)
                .flatMap(specFile -> generateCodeInternal(specFile, language));
    }

    /**
     * Generates code from a remote OpenAPI spec URL.
     */
    public Mono<CodeGenResponse> generateCode(CodeGenRequest request) {
        return swaggerDownloader.downloadSwaggerFromUrl(request.getOpenApiSpecUrl())
                .flatMap(specFile -> generateCodeInternal(specFile.toFile(), request.getLanguage()));
    }

    // TODO: Replace with code that separates code genenration from zipping
    private Mono<File> getFileFromUrl(String url) {
        //swaggerDownloader.downloadSwaggerFromUrl(url)
                //.flatMap(codegenService::generateFromSpec)
                //.flatMap(codegenService::uploadZipToBucket)
               // .map(bucketUrl -> ServerResponse.ok().bodyValue(new CodegenResponse(bucketUrl)))
                //.onErrorResume(error -> ServerResponse.status(500).bodyValue(error.getMessage()));

        return Mono.error(new UnsupportedOperationException("Remote file download not implemented"));
    }

    /**
     * Internal logic for generating code and zipping result.
     */
    private Mono<CodeGenResponse> generateCodeInternal(File specFile, String language) {
        return Mono.fromCallable(() -> convertSpecToCode(specFile.toPath(), language, outputDirectory))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(generatedDir -> fileService.zipFile(outputDirectory, generatedDir))
                .map(zippedFile -> CodeGenResponse.builder()
                        .downloadUrl(zippedFile.toString())
                        .status("SUCCESS")
                        .build())
                .doFinally(signal -> fileService.deleteFile(specFile));
    }

    /**
     * Converts OpenAPI spec to generated code directory using OpenAPI Generator.
     */
    protected Path convertSpecToCode(Path specFile, String language, Path outputDir) {
        Path generatedCodeDir = outputDir.resolve("generated");
        CodegenConfigurator configurator = new CodegenConfigurator()
                .setInputSpec(specFile.toAbsolutePath().toString())
                .setGeneratorName(language)
                .setOutputDir(generatedCodeDir.toString());

        new DefaultGenerator().opts(configurator.toClientOptInput()).generate();
        return generatedCodeDir;
    }
}
