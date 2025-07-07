package com.idea2code.code_generator.storage.s3;

import com.idea2code.code_generator.config.S3ClientConfigProperties;
import com.idea2code.code_generator.models.FileStorageException;
import com.idea2code.code_generator.models.UploadState;
import com.idea2code.code_generator.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class S3StorageService implements StorageService {
  @Autowired
  public S3AsyncClient s3client;
  @Autowired
  public S3ClientConfigProperties s3props;
  @Autowired
  private DataBufferFactory dataBufferFactory;

  @Override
  public Mono<String> upload(Object content) {
    if (content instanceof Flux) { //Use FLux<DataBuffer> for very large files
      return uploadWithFlux(null,(Flux<DataBuffer>) content);
    } else {
      return Mono.error(new IllegalArgumentException("Unsupported content type"));
    }
  }

  private Mono<String> uploadWithFlux(String filename, Flux<DataBuffer> content) {
    String fileKey = UUID.randomUUID().toString();
    filename = filename == null ? fileKey : filename;
    Map<String, String> metadata = new HashMap<String, String>();
    metadata.put("filename", filename);

    UploadState uploadState = new UploadState(s3props.getBucket(), fileKey);
    log.info("In service for s3 upload");
    CompletableFuture<CreateMultipartUploadResponse> uploadRequest = s3client.createMultipartUpload(
      CreateMultipartUploadRequest.builder()
        .contentType("application/zip")
        .key(fileKey)
        .metadata(metadata)
        .bucket(s3props.getBucket())
        .build()
    );

    return Mono
      .fromFuture(uploadRequest)
      .flatMapMany((response) -> {
        checkResult(response);
        uploadState.setUploadId(response.uploadId());
        return content;
      })
      //chunk file content into sublists of multipartMinSize
      .bufferUntil((buffer) -> {
        int newBufferCount = uploadState.getBuffered() + buffer.readableByteCount();
        uploadState.setBuffered(newBufferCount);
        if(newBufferCount >= s3props.getMultipartMinSize()){
          uploadState.setBuffered(0);
          return true;
        }else{
          return false;
        }
      })
      //merge sublists of 5MB and upload to s3
      .map((buffers) -> concatBuffers(buffers))
      .flatMap((buffer) -> uploadPart(uploadState, buffer))
      .reduce(uploadState, (UploadState state, CompletedPart completedPart) -> {
        state.completedParts.put(completedPart.partNumber(), completedPart);
        return state;
      })
      .flatMap((state) -> completeUpload(state))
      .map((response) -> {
        checkResult(response);
        return uploadState.getFileKey();
      });
  }

  private Mono<CompletedPart> uploadPartFormatted(UploadState uploadState, DataBuffer buffer) {
    final int partNumber = ++uploadState.partCounter;

    return Mono.fromFuture(s3client.uploadPart(UploadPartRequest.builder()
          .bucket(uploadState.getBucket())
          .key(uploadState.getFileKey())
          .partNumber(partNumber)
          .uploadId(uploadState.getUploadId())
          .contentLength((long) buffer.readableByteCount())
          .build(),
        AsyncRequestBody.fromByteBuffers(buffer.asByteBuffer())))
      .doOnNext(this::checkResult)
      .map(response -> CompletedPart.builder()
        .partNumber(partNumber)
        .eTag(response.eTag())
        .build())
      .doFinally(signal -> DataBufferUtils.release(buffer));
  }

  private Mono<CompletedPart> uploadPart(UploadState uploadState, DataBuffer buffer) {
    final int partNumber = ++uploadState.partCounter;
    log.info("In upload parts function {}", partNumber);
    CompletableFuture<UploadPartResponse> request = s3client.uploadPart(
      UploadPartRequest.builder()
        .bucket(uploadState.getBucket())
        .key(uploadState.getFileKey())
        .partNumber(partNumber)
        .uploadId(uploadState.getUploadId())
        .contentLength((long) buffer.capacity())
        .build(),
      AsyncRequestBody.fromByteBuffers(buffer.asByteBuffer())
    );

    return Mono
      .fromFuture(request)
      .map((uploadPartResponse) -> {
        checkResult(uploadPartResponse);
        return CompletedPart.builder()
          .eTag(uploadPartResponse.eTag())
          .partNumber(partNumber)
          .build();
      });

  }


  private DataBuffer concatBuffers(List<DataBuffer> buffers) {
    // Calculate total size
    int totalSize = buffers.stream().mapToInt(DataBuffer::readableByteCount).sum();

    // Create new buffer with combined capacity
    DataBuffer combined = dataBufferFactory.allocateBuffer(totalSize);

    // Write all buffers into the combined one
    buffers.forEach(buffer -> {
      combined.write(buffer);
      DataBufferUtils.release(buffer);  // Release the original buffer
    });
    return combined;
  }

  private void checkResult(SdkResponse response) {
    if (response.sdkHttpResponse() == null || !response.sdkHttpResponse().isSuccessful()) {
      log.info("failed to upload file to s3");
      throw new FileStorageException("Failed to upload file to S3. Response: " +
        (response.sdkHttpResponse() != null ?
          response.sdkHttpResponse().statusCode() : "null"));
    }
  }

  private Mono<CompleteMultipartUploadResponse> completeUpload(UploadState uploadState){
    log.info("In upload completion function");
    CompletedMultipartUpload multipartUpload = CompletedMultipartUpload.builder()
      .parts(uploadState.completedParts.values())
      .build();

    return Mono.fromFuture(s3client.completeMultipartUpload(
      CompleteMultipartUploadRequest.builder()
        .bucket(uploadState.getBucket())
        .uploadId(uploadState.getUploadId())
        .multipartUpload(multipartUpload)
        .key(uploadState.getFileKey())
        .build()
      )
    );
  }
}
