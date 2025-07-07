package com.idea2code.code_generator.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
@Slf4j
public class FileDownloader {

  private final WebClient webClient;

  public FileDownloader(WebClient webClient) {
    this.webClient = webClient;
  }

  /**
   * Downloads a YAML (Swagger) file from a URL and saves it to a temporary file.
   * Returns a Mono<Path> pointing to the saved file, to be chained in a reactive flow.
   */
  public Mono<Path> downloadSwaggerFromUrl(String fileUrl) {
    // Generate temp file path
    Path tempFilePath = Path.of(System.getProperty("java.io.tmpdir"), "openapi-" + UUID.randomUUID() + ".yaml");

    return webClient
      .get()
      .uri(fileUrl)
      .accept(MediaType.APPLICATION_YAML)
      .retrieve()
      .bodyToFlux(DataBuffer.class)
      .publishOn(Schedulers.boundedElastic()) // file I/O is blocking
      .transform(flux -> DataBufferUtils.write(flux, tempFilePath, StandardOpenOption.CREATE))
      .then(Mono.just(tempFilePath))
      .doOnSubscribe(sub -> log.info("Starting download for: {}", fileUrl))
      .doOnSuccess(path -> log.info("Downloaded and saved to: {}", path))
      .doOnError(error -> log.error("Failed to download {}: {}", fileUrl, error.getMessage(), error));
  }

  private Mono<Void> writeToFile(DataBuffer buffer, Path path) {
    return Mono.fromRunnable(() -> {
      try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
        Channels.newChannel(os).write(buffer.asByteBuffer());
      } catch (Exception e) {
        throw new RuntimeException("Failed to write to file: " + path, e);
      }
    });
  }

  //TODO:  add MDC logging
  private void logSuccess(String url, Path path) {
    log.info("✅ Downloaded from %s to %s%n", url, path.toString());
  }

  //TODO:  add MDC logging
  private void logFailure(String url, Throwable error) {
    log.info("❌ Failed to download from %s: %s%n", url, error.getMessage());
  }
}
