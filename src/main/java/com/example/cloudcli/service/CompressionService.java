package com.example.cloudcli.service;

import com.example.cloudcli.exception.BackupException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Service
public class CompressionService {

    public Path compress(Path source) throws IOException {

        if (Files.isDirectory(source)) {
            return compressDirectory(source);
        }

        Path compressed = source.resolveSibling(
                source.getFileName().toString() + ".gz"
        );

        try (InputStream in = Files.newInputStream(source);
             OutputStream out = new GZIPOutputStream(Files.newOutputStream(compressed))) {

            in.transferTo(out);
        }

        Files.delete(source);

        log.info("Compressed {} -> {}",
                source.getFileName(),
                compressed.getFileName());

        return compressed;
    }
    
    private Path compressDirectory(Path sourceDir) throws IOException {
        Path tarGzFile = sourceDir.resolveSibling(sourceDir.getFileName().toString() + ".tar.gz");
        
        try (OutputStream fos = Files.newOutputStream(tarGzFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {
            
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            
            Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        Path relativePath = sourceDir.getParent().relativize(file);
                        TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), relativePath.toString());
                        taos.putArchiveEntry(entry);
                        Files.copy(file, taos);
                        taos.closeArchiveEntry();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to add file to tar: " + file, e);
                    }
                });
        }
        
        // Delete the source directory after compression
        deleteDirectory(sourceDir);
        
        log.info("Compressed directory {} -> {}", sourceDir.getFileName(), tarGzFile.getFileName());
        
        return tarGzFile;
    }
    
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted((a, b) -> -a.compareTo(b)) // Reverse order to delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.warn("Failed to delete: {}", path, e);
                    }
                });
        }
    }
}