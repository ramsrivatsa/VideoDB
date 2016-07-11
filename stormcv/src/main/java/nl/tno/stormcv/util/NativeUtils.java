package nl.tno.stormcv.util;

import nl.tno.stormcv.operation.OpenCVOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.unlimitedcodeworks.utils.ResourceLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

/**
 * A utility class used to load platform dependent (OpenCV) libraries and other resources and is used by {@link OpenCVOp} implementations.
 * Libraries must be present on the classpath for this utility class to function properly.
 * If the library resides within a jar file it will be extracted  to the local tmp directory before it is loaded.
 * <p/>
 * The project is shipped with the following OpenCV 2.4.8 binaries:
 * <ul>
 * <li>Windows dll's both 32 and 64 bit</li>
 * <li>Mac OS dylib, 64 bit only</li>
 * <li>Ubuntu 12.04 LTS so 64 bit only</li>
 * </ul>
 * <p/>
 * It is possible to build your own libraries on the platform of your choosing, see:
 * http://docs.opencv.org/doc/tutorials/introduction/desktop_java/java_dev_intro.html
 *
 * @author Corne Versloot
 */
public class NativeUtils {
    private static Logger logger = LoggerFactory.getLogger(NativeUtils.class);

    /**
     * Loads the openCV library independent of OS and architecture
     *
     * @throws RuntimeException when the library cannot be found
     * @throws IOException      when the library could not be extracted or loaded
     */
    public static void load() throws RuntimeException, IOException {
        try {
            System.loadLibrary("opencv_java310");
        } catch (UnsatisfiedLinkError e) {
            loadLibrary("opencv_java310");
        }
    }

    /**
     * Loads a native library represented by the given name (should be present on the classpath).
     *
     * @throws RuntimeException when the library cannot be found
     */
    public static void loadLibrary(String libname) {
        String resPath = "/native/" + (is64Bit() ? "x64/" : "x86/")
                + nativeLibraryName(libname);
        try {
            File libFile = getAsLocalFile(resPath);
            System.load(libFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load native library from resource at " + resPath, e);
        }
    }

    /**
     * Loads a native library represented by the given name.
     * @param libname
     * @param trySystem if true, try directly pass the libname to System.loadLibrary first
     */
    public static void loadLibrary(String libname, boolean trySystem) {
        if (trySystem) {
            try {
                System.loadLibrary(libname);
            } catch (UnsatisfiedLinkError e) {
                logger.warn("Can't load {} from system, load from resources instead", libname);
                logger.warn("Current java library path is {}", System.getProperty("java.library.path"));
                logger.warn("Underlaying exception:");
                e.printStackTrace();
                loadLibrary(libname);
            }
        } else {
            loadLibrary(libname);
        }
    }

    /**
     * Determines the OS dependent library file name.
     *
     * @return the proper name of the library
     */
    private static String nativeLibraryName(String libname) {
        String osName = System.getProperty("os.name").toLowerCase();
        String prefix;
        String ext;
        if (osName.startsWith("windows")) {
            prefix = "";
            ext = ".dll";
        } else if (osName.startsWith("mac os x")) {
            prefix = "lib";
            ext = ".dylib";
        } else {
            prefix = "lib";
            ext = ".so";
        }
        return prefix + libname + ext;
    }

    private static boolean is64Bit() {
        String model = System.getProperty("sun.arch.data.model");
        try {
            int bitness = Integer.parseInt(model);
            if (bitness == 64) {
                return true;
            }
        } catch (NumberFormatException e) {
            System.err.println("Bitness '" + model + "' detected - continue with 32 bit");
        }
        return false;
    }

    /**
     * Get file in resource or class path
     *
     * @param name the resource name
     * @throws IOException
     */
    public static File getAsLocalFile(String name) throws IOException {
        URL url = NativeUtils.class.getResource(name);
        if (url == null) throw new FileNotFoundException("Unable to locate resource: " + name);

        File file = null;
        if (url.getProtocol().equals("jar")) {
            file = extractTmpFileFromJar(name, true);
        } else {
            file = new File(url.getFile());
        }
        return file;
    }

    /**
     * Tries to extract the resource at path to the systems tmp dir
     *
     * @param path         the path or file of the resource to be extracted
     * @param deleteOnExit whether automatically delete the tmp file on exit
     * @return the file the resource was written to
     * @throws IOException in case the resource cannot be read or written to tmp
     */
    private static File extractTmpFileFromJar(String path, boolean deleteOnExit) throws IOException {
        return ResourceLoader.inflateResource(path, deleteOnExit);
    }
}