package com.marzouki.shop.media;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/** Stores images as files under one directory. Keys are generated here, never taken from users. */
@Slf4j
@Component
class LocalImageStorage implements ImageStorage {

    /** The only shape of key that exists: 32 hex characters and ".jpg". */
    static final Pattern KEY = Pattern.compile("[0-9a-f]{32}\\.jpg");

    private final Path root;

    LocalImageStorage(@Value("${app.images.dir:./data/images}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public void save(String key, byte[] data) {
        try {
            Files.createDirectories(root);
            Files.write(resolve(key), data);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store the image", e);
        }
    }

    @Override
    public Optional<Resource> find(String key) {
        if (!KEY.matcher(key).matches()) {
            return Optional.empty();
        }
        Path file = resolve(key);
        return Files.isRegularFile(file) ? Optional.of(new FileSystemResource(file)) : Optional.empty();
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("Could not delete the image file {}", key, e);
        }
    }

    private Path resolve(String key) {
        if (!KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Not an image key");
        }
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("Not an image key");
        }
        return file;
    }
}