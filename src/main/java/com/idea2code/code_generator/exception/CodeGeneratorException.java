package com.idea2code.code_generator.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
@Getter
@Builder
@AllArgsConstructor
public class CodeGeneratorException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String message;
}
