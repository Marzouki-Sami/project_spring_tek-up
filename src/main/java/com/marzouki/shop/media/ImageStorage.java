package com.marzouki.shop.media;

import java.util.Optional;
import org.springframework.core.io.Resource;

/** Where image files live. Local disk today; an object store such as S3 could implement this later. */
public interface ImageStorage {

    void save(String key, byte[] data);

    Optional<Resource> find(String key);

    void delete(String key);
}