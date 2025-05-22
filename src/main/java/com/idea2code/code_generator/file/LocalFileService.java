package com.idea2code.code_generator.file;

import com.idea2code.code_generator.config.CodeGenProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class LocalFileService implements FileService {
    private final CodeGenProperties props;

    public LocalFileService(CodeGenProperties props) {
        this.props = props;
    }

    public Path createDirectory(String directoryPath) throws IOException {
        String id = UUID.randomUUID().toString();
        Path tempDirectory = Paths.get(directoryPath, id);
        Files.createDirectories(tempDirectory.toAbsolutePath());
        return tempDirectory;
    }

    public Path writeFile(Path directory, String fileContent) throws IOException {
        Path specFile = directory.resolve(props.getInputFile());
        Files.write(specFile, fileContent.getBytes());
        return specFile;
    }

    public Path zipFile(Path sourceDir, Path outputDir) throws IOException {
        Path zipFile = outputDir.resolve(props.getOutputFile());
        zipDirectory(sourceDir.toFile(), zipFile.toFile());
        return zipFile;
    }

    private void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos)
        ) {
            Path sourcePath = sourceDir.toPath();

            Files.walk(sourcePath).filter(path -> !Files.isDirectory(path)).forEach(path -> {
                ZipEntry zipEntry = new ZipEntry(sourcePath.relativize(path).toString());
                try {
                    zos.putNextEntry(zipEntry);
                    Files.copy(path, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    public File convertMultipartToFile(MultipartFile multipartFile,Path targetDir) throws IOException {
        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            throw new IOException("Target directory does not exist: " + targetDir);
        }

        // Sanitize filename and make it unique
        String originalFileName = Paths.get(multipartFile.getOriginalFilename()).getFileName().toString();
        String uniqueFileName = UUID.randomUUID() + "_" + originalFileName;

        // Create file path
        Path filePath = targetDir.resolve(uniqueFileName);
        File file = filePath.toFile();

        // Write content
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(multipartFile.getBytes());
        }

        return file;

    }
}
