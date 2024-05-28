package com.kurswork.ipz.filemanager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class FileInfo {
    public enum FileType {
        FILE("F"), DIRECTORY("D");

        private final String type;

        FileType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    private final String filename;
    private final FileType type;
    private final long size;
    private final LocalDateTime lastModified;

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public FileType getType() {
        return type;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public FileInfo(Path path) {
        this.filename = path.getFileName().toString();
        this.type = Files.isDirectory(path) ? FileType.DIRECTORY : FileType.FILE;
        this.size = getFileSize(path);
        this.lastModified = getLastModifiedTime(path);
    }

    private long getFileSize(Path path) {
        try {
            return Files.isDirectory(path) ? -1L : Files.size(path);
        } catch (IOException e) {
            throw new RuntimeException("Не вдається отримати розмір файлу для шляху: " + path, e);
        }
    }

    private LocalDateTime getLastModifiedTime(Path path) {
        try {
            return LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.UTC);
        } catch (IOException e) {
            throw new RuntimeException("Неможливо отримати час останньої зміни шляху: " + path, e);
        }
    }
}