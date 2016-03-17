package nl.tno.stormcv.util;

import backtype.storm.utils.Utils;
import nl.tno.stormcv.operation.OpenCVOp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

/**
 * A utility class used to load platform dependent (OpenCV) libraries and other resources and is used by {@link OpenCVOp} implementations. 
 * Libraries must be present on the classpath for this utility class to function properly.
 * If the library resides within a jar file it will be extracted  to the local tmp directory before it is loaded.
 * 
 * The project is shipped with the following OpenCV 2.4.8 binaries:
 * <ul>
 * <li>Windows dll's both 32 and 64 bit</li>
 * <li>Mac OS dylib, 64 bit only</li>
 * <li>Ubuntu 12.04 LTS so 64 bit only</li>
 * </ul>
 * 
 * It is possible to build your own libraries on the platform of your choosing, see:
 * http://docs.opencv.org/doc/tutorials/introduction/desktop_java/java_dev_intro.html
 * 
 * @author Corne Versloot
 */
public class NativeUtils {

	/**
	 * Loads the openCV library independent of OS and architecture
	 * @throws RuntimeException when the library cannot be found
	 * @throws IOException when the library could not be extracted or loaded
	 */
	public static void load() throws RuntimeException, IOException{
		try{
			System.loadLibrary("opencv_java310");
		} catch (UnsatisfiedLinkError e) {
            loadLibrary("opencv_java310");
		}
	}
	
	/**
	 * Loads the openCV library represented by the given name (should be present on the classpath). 
	 * @throws RuntimeException when the library cannot be found
	 * @throws IOException when the library could not be extracted or loaded
	 * @deprecated use @see nl.tno.stormcv.util.NativeUtils#loadLibrary() instead
	 */
	@Deprecated
	public static void load(String name) throws RuntimeException, IOException{
		if(!name.startsWith("/")) name = "/"+name;
		File libFile = NativeUtils.getAsLocalFile(name);
		Utils.sleep(500); // wait a bit to be sure the library is ready to be read
		System.load(libFile.getAbsolutePath());
	}

	/**
	 * Loads a native library represented by the given name (should be present on the classpath).
	 * @throws RuntimeException when the library cannot be found
	 */
	public static void loadLibrary(String libname) {
		String resPath = "/native/" + (is64Bit() ? "x64/" : "x86/")
				+ nativeLibraryName(libname);
		try {
			File libFile = getAsLocalFile(resPath);
			System.err.println("Loading " + libFile.getAbsolutePath());
			System.load(libFile.getAbsolutePath());
		} catch (Exception e) {
			throw new RuntimeException("Failed to load native library from resource at " + resPath, e);
		}
	}

    /**
     * Determines the OS dependent library file name.
     * @return the proper name of the library
     */
	private static String nativeLibraryName(String libname)
	{
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
	 * Attempts to first extract the library at path to the tmp dir and load it
	 * @param name
	 * @throws IOException
	 */
    public static File getAsLocalFile(String name) throws IOException {
    	if(!name.startsWith("/")) name = "/"+name;
    	
    	URL url = NativeUtils.class.getResource(name);
    	if(url == null) throw new FileNotFoundException("Unable to locate "+name);
    	
    	File file = null;
    	if(url.getProtocol().equals("jar")){
    		file = extractTmpFileFromJar(name, false);
    	}else{
    		file = new File(url.getFile());
    	}
    	return file;
    }
    
    /**
     * Tries to extract the resource at path to the systems tmp dir
     * @param path the path or file of the resource to be extracted
     * @param deleteOnExit
     * @return the file the resource was written to
     * @throws IOException in case the resource cannot be read or written to tmp
     */
    public static File extractTmpFileFromJar(String path, boolean deleteOnExit) throws IOException{
    	return ResourceLoader.inflateResource(path, deleteOnExit);
    }
}