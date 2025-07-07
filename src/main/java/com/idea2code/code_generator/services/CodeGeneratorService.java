package com.idea2code.code_generator.services;

import com.idea2code.code_generator.config.CodeGenProperties;
import com.idea2code.code_generator.models.CodeGenRequest;
import com.idea2code.code_generator.models.CodeGenResponse;
import com.idea2code.code_generator.storage.StorageService;
import com.idea2code.code_generator.storage.file.FileService;
import com.idea2code.code_generator.storage.s3.S3StorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.nio.file.Path;


@Service
@Slf4j
public class CodeGeneratorService {
  private final CodeGenProperties props;
  private final FileService fileService;
  private final FileDownloader swaggerDownloader;
  private StorageService fileUploader;
  private Path outputDirectory;
  private Path outputZipDirectory;


  public CodeGeneratorService(CodeGenProperties props,
                              FileService fileService,
                              FileDownloader swaggerDownloader,
                              S3StorageService s3Uploader) {
    this.props = props;
    this.fileService = fileService;
    this.swaggerDownloader = swaggerDownloader;
    this.fileUploader = s3Uploader;
  }

  @PostConstruct
  private void createOutputDirectory() {

    outputDirectory = fileService.createDirectory(props.getOutputDir());
    outputZipDirectory = fileService.createDirectory(props.getOutputZipDir());
  }

  /**
   * Generates code from a multipart OpenAPI spec file.
   */
  public Mono<CodeGenResponse> generateCode(MultipartFile openAPISpec, String language) {
    return fileService.convertMultipartToFile(openAPISpec, outputDirectory)
      .flatMap(specFile -> generateCodeInternal(specFile, language));
  }

  /**
   * Test s3 upload
   */
  public Mono<String> uploadGeneratedCode(FilePart file) {
    return fileUploader.upload(file.content());
  }

  /**
   * Generates code from a remote OpenAPI spec URL.
   */
  public Mono<CodeGenResponse> generateCodeOld(CodeGenRequest request) {
    return swaggerDownloader.downloadSwaggerFromUrl(request.getOpenApiSpecUrl())
      .flatMap(specFile -> generateCodeInternal(specFile.toFile(), request.getLanguage()));
  }


  public Mono<CodeGenResponse> generateCode(CodeGenRequest request) {
    return swaggerDownloader.downloadSwaggerFromUrl(request.getOpenApiSpecUrl())
      .flatMap(specFile ->
        Mono.fromCallable(() -> convertSpecToCode(specFile, request.getLanguage(), outputDirectory))
          .subscribeOn(Schedulers.boundedElastic())
      )
      .flatMap(generatedDir ->
        fileService.zipFile(generatedDir, outputZipDirectory) // assumed to return Mono<File or Path>
      )
      .map(zippedFile -> CodeGenResponse.builder()
        .downloadUrl(zippedFile.toString())
        .status("SUCCESS")
        .build())
      .doOnSuccess(res -> log.info("Generated code and zipped to: {}", res.getDownloadUrl()))
      .doOnError(e -> log.error("Error during code generation", e));
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
      .doOnSubscribe(sub -> log.info("Starting code generation for file: {}", specFile))
      .doOnSuccess(res -> log.info("Generated code and zipped to: {}", res.getDownloadUrl()))
      .doOnError(e -> log.error("Error during code generation", e));
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
