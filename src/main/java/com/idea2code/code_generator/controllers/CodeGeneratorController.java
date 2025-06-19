package com.idea2code.code_generator.controllers;


import com.idea2code.code_generator.models.CodeGenRequest;
import com.idea2code.code_generator.models.CodeGenResponse;
import com.idea2code.code_generator.services.CodeGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;

@RestController
@RequestMapping("/api/codegen")
public class CodeGeneratorController {
    @Autowired
    private CodeGeneratorService codeGeneratorService;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<CodeGenResponse> generateCode(
            @RequestParam("file") MultipartFile specification,
            @RequestParam String language
    ) throws IOException {
        return codeGeneratorService.generateCode(specification, language);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<CodeGenResponse> generateCode(
            @RequestBody CodeGenRequest codeGenRequest
    ) throws IOException {
        return codeGeneratorService.generateCode(codeGenRequest);
    }


}
