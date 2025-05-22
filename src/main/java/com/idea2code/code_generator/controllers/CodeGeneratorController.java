package com.idea2code.code_generator.controllers;


import com.idea2code.code_generator.models.CodeGenRequest;
import com.idea2code.code_generator.models.CodeGenResponse;
import com.idea2code.code_generator.services.CodeGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/codegen")
public class CodeGeneratorController {
    @Autowired
    private CodeGeneratorService codeGeneratorService;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CodeGenResponse> generateCode(
            @RequestParam("file") MultipartFile specification,
            @RequestParam String language
    ) throws IOException {
        CodeGenResponse response = codeGeneratorService.generateCode(specification, language);
        return ResponseEntity.ok(response);
    }
}
