package xyz.unlimitedcodeworks.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread safe resource loader
 *
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-3-12.
 */
public class ResourceLoader {

    public static File inflateResource(String resPath) {
        return inflateResource(resPath, true);
    }
    public static File inflateResource(String resPath, boolean deleteOnExit) {
        InputStream in = null;
        try {
            String filename = new File(resPath).getName();
            Path tempPath = getTempDirectory().resolve(filename);
            File tempFile = tempPath.toFile();

            Object lock = new Object();
            synchronized (lock) {
                Object prev = inflatedResource.putIfAbsent(tempPath.toString(), lock);
                if (prev != null) {
                    synchronized (prev) {
                        return tempFile;
                    }
                }

                in = ResourceLoader.class.getResourceAsStream(resPath);
                try {
                    Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (FileAlreadyExistsException e) {
                }

                if (deleteOnExit)
                    tempFile.deleteOnExit();

                return tempFile;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load resource: " + resPath, e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
            }
        }
    }
    private static ConcurrentHashMap<String, Object> inflatedResource = new ConcurrentHashMap<>();

    private static Path tempDirectory = null;
    private static synchronized Path getTempDirectory() throws IOException {
        if (tempDirectory != null)
            return tempDirectory;
        tempDirectory = Files.createTempDirectory("cvld");
        tempDirectory.toFile().deleteOnExit();

        return tempDirectory;
    }
}

