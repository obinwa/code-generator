package com.idea2code.code_generator.controllers;


import com.idea2code.code_generator.models.CodeGenRequest;
import com.idea2code.code_generator.models.CodeGenResponse;
import com.idea2code.code_generator.services.CodeGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/codegen")
public class CodeGeneratorController {
  @Autowired
  private CodeGeneratorService codeGeneratorService;

  @PostMapping(
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  public Mono<Object> uploadGenerateCode(
    @RequestPart("uploadedFile") Mono<FilePart> filePartMono
  ) throws IOException {
    log.info("Upload controller START");
    return filePartMono.flatMap(filePart -> codeGeneratorService.uploadGeneratedCode(filePart))
      .map(url -> {
        log.info(String.format("S3 URL {}",url));
        return url;
      });

  }


  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<CodeGenResponse> generateCode(
    @RequestBody CodeGenRequest codeGenRequest
  ) throws IOException {
    return codeGeneratorService.generateCode(codeGenRequest);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleAll(Exception ex) {
    ex.printStackTrace();
    return ResponseEntity.badRequest().body("Error: " + ex.getMessage());
  }


}
