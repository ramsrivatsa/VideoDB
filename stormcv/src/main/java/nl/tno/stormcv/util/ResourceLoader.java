package nl.tno.stormcv.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-3-12.
 */
public class ResourceLoader {

    public static File inflateResource(String resPath) {
        return inflateResource(resPath, true);
    }
    public static File inflateResource(String resPath, boolean deleteOnExit) {
        InputStream in = null;
        try {
            in = ResourceLoader.class.getResourceAsStream(resPath);
            String filename = new File(resPath).getName();

            //String[] tokens = filename.split("\\.(?=[^\\.]+$)");
            //Path tempFile = Files.createTempFile(tokens[0], tokens[1], fileAttributes);
            Path tempPath = getTempDirectory().resolve(filename);
            File tempFile = tempPath.toFile();

            if (tempFile.exists())
                return tempFile;

            if (deleteOnExit)
                tempFile.deleteOnExit();

            Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);

            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load resource: " + resPath, e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
            }
        }
    }

    private static Path tempDirectory = null;
    private static Path getTempDirectory() throws IOException {
        if (tempDirectory != null)
            return tempDirectory;
        tempDirectory = Files.createTempDirectory("cvld");
        tempDirectory.toFile().deleteOnExit();

        return tempDirectory;
    }
}

