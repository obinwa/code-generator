package com.idea2code.code_generator.file;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface FileService {
    Path createDirectory(String directoryPath) throws IOException;
    Path writeFile(Path directory, String fileContent) throws IOException;
    Path zipFile(Path sourceDir, Path outputDir) throws IOException;
    public File convertMultipartToFile(MultipartFile multipartFile, Path targetDir) throws IOException;
}
