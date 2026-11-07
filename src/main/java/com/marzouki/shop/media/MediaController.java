package com.marzouki.shop.media;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Serves product pictures. A key never changes its content, so browsers may keep the file for a year. */
@RestController
@RequiredArgsConstructor
public class MediaController {

    private final ImageStorage storage;

    @GetMapping("/media/{key:[0-9a-f]{32}\\.jpg}")
    public ResponseEntity<Resource> image(@PathVariable String key) {
        return storage.find(key)
                .map(resource -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                        .body(resource))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}