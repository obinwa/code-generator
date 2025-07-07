package com.idea2code.code_generator.storage;

import reactor.core.publisher.Mono;

public interface StorageService {
  Mono<String> upload(Object content);      // both local and S3
  //Mono<File> retrieve(...);    // maybe both
  //void delete(...);
}
