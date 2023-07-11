package com.kvaster.iptv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileLoader {
    private static final Logger LOG = LoggerFactory.getLogger(FileLoader.class);

    private static final String FILE_SCHEME = "file://";

    public static CompletableFuture<String> tryLoadString(String url) {
        try {
            return complete(loadString(url));
        } catch (Exception e) {
            return completeWithError(e);
        }
    }

    public static CompletableFuture<byte[]> tryLoadBytes(String url) {
        try {
            return complete(loadBytes(url));
        } catch (Exception e) {
            return completeWithError(e);
        }
    }

    private static <T> CompletableFuture<T> complete(T value) {
        if (value == null) {
            return null;
        }

        var future = new CompletableFuture<T>();
        future.complete(value);
        return future;
    }

    private static <T> CompletableFuture<T> completeWithError(Throwable e) {
        var future = new CompletableFuture<T>();
        future.completeExceptionally(e);
        return future;
    }

    private static String loadString(String url) throws IOException {
        byte[] data = loadBytes(url);
        if (data == null) {
            return null;
        }
        return new String(data);
    }

    private static byte[] loadBytes(String url) throws IOException {
        if (url.startsWith(FILE_SCHEME)) {
            return Files.readAllBytes(Path.of(url.substring(FILE_SCHEME.length())));
        } else {
            return null;
        }
    }
}
