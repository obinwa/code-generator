package com.idea2code.code_generator.file;

import com.idea2code.code_generator.config.CodeGenProperties;
import com.idea2code.code_generator.exception.CodeGeneratorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class LocalFileServiceImp implements FileService{

    private final CodeGenProperties props;

    public LocalFileServiceImp(CodeGenProperties props) {
        this.props = props;
    }

    public Path createDirectory(String directoryPath)  {
        String id = UUID.randomUUID().toString();
        Path tempDirectory = Paths.get(directoryPath, id);
        try {
            Files.createDirectories(tempDirectory.toAbsolutePath());
        } catch (IOException e) {
            // TODO : wrap the exception in with more exlicit message and parameters and probably exception
            throw new CodeGeneratorException(HttpStatus.BAD_REQUEST, "Failed to create directory");
        }
        return tempDirectory;
    }

    @Override
    public Mono<Path> writeFile(Path directory, String fileContent) {
        return Mono.fromCallable(() -> {
            Path specFile = directory.resolve(props.getInputFile());
            Files.write(specFile, fileContent.getBytes());
            return specFile;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Reactive wrapper to zip a directory
     */
    @Override
    public Mono<Path> zipFile(Path sourceDir, Path outputDir) {
        return Mono.fromCallable(() -> {
            Path zipFile = outputDir.resolve(props.getOutputFile());
            zipDirectory(sourceDir.toFile(), zipFile.toFile());
            return zipFile;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Reactive wrapper for Multipart to File conversion
     */
    @Override
    public Mono<File> convertMultipartToFile(MultipartFile multipartFile, Path targetDir) {
        return Mono.fromCallable(() -> {
            if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
                throw new IOException("Target directory does not exist: " + targetDir);
            }

            String originalFileName = Paths.get(multipartFile.getOriginalFilename()).getFileName().toString();
            String uniqueFileName = UUID.randomUUID() + "_" + originalFileName;
            Path filePath = targetDir.resolve(uniqueFileName);
            File file = filePath.toFile();

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(multipartFile.getBytes());
            }catch (IOException exception){
                // TODO : wrap the exception in with more exlicit message and parameters and probably exception
                throw new CodeGeneratorException(HttpStatus.BAD_REQUEST, "Failed to convert multipart file");
            }

            return file;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Delete file synchronously (can also be wrapped if needed)
     */
    @Override
    public void deleteFile(File file) {
        if (file != null && file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                log.warn("Failed to delete temp file: {}", file.getAbsolutePath());
            }
        }
    }

    /**
     * Internal zip logic
     */
    private void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (
                FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos)
        ) {
            Path sourcePath = sourceDir.toPath();
            Files.walk(sourcePath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourcePath.relativize(path).toString());
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            // TODO : wrap the exception in with more exlicit message and parameters and probably exception
                            throw new CodeGeneratorException(HttpStatus.BAD_REQUEST, "Failed to zip directory");
                        }
                    });
        }
    }
}
