package net.bytebuddy.android;

import android.annotation.TargetApi;
import android.os.Build;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.file.DexFile;
import dalvik.system.DexClassLoader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.utility.RandomString;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

/**
 * <p>
 * A class loading strategy that allows to load a dynamically created class at the runtime of an Android
 * application. For this, a {@link dalvik.system.DexClassLoader} is used under the covers.
 * </p>
 * <p>
 * This class loader requires to write files to the file system which are then processed by the Android VM. It is
 * <b>not</b> permitted by Android's security checks to store these files in a shared folder where they could be
 * manipulated by a third application what would break Android's sandbox model. An example for a forbidden storage
 * would therefore be the external storage. Instead, the class loading application must either supply a designated
 * directory, such as by creating a directory using {@link android.content.Context#getDir(String, int)} with specifying
 * {@link android.content.Context#MODE_PRIVATE} visibility for the created folder or by using the
 * {@code getCodeCacheDir} directory which is exposed for Android API versions 21 or higher.
 * </p>
 * <p>
 * By default, this Android {@link net.bytebuddy.dynamic.loading.ClassLoadingStrategy} uses the Android SDK's dex compiler in
 * <i>version 1.7</i> which requires the Java class files in version {@link net.bytebuddy.ClassFileVersion#JAVA_V6} as
 * its input. This version is slightly outdated but newer versions are not available in Maven Central which is why this
 * outdated version is included with this class loading strategy. Newer version can however be easily adapted by
 * implementing the methods of a {@link net.bytebuddy.android.AndroidClassLoadingStrategy.DexProcessor} to
 * appropriately delegate to the newer dex compiler. In case that the dex compiler's API was not altered, it would
 * even be sufficient to include the newer dex compiler to the Android application's build path while also excluding
 * the version that ships with this class loading strategy. While most parts of the Android SDK's components are
 * licensed under the <i>Apache 2.0 license</i>, please also note
 * <a href="https://developer.android.com/sdk/terms.html">their terms and conditions</a>.
 * </p>
 */
@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class AndroidClassLoadingStrategy implements ClassLoadingStrategy {

    /**
     * The name of the dex file that the {@link dalvik.system.DexClassLoader} expects to find inside of a jar file
     * that is handed to it as its argument.
     */
    private static final String DEX_CLASS_FILE = "classes.dex";

    /**
     * The file name extension of a jar file.
     */
    private static final String JAR_FILE_EXTENSION = ".jar";

    /**
     * A value for a {@link dalvik.system.DexClassLoader} to indicate that the library path is empty.
     */
    private static final String EMPTY_LIBRARY_PATH = null;

    /**
     * The dex creator to be used by this Android class loading strategy.
     */
    private final DexProcessor dexProcessor;

    /**
     * A directory that is <b>not shared with other applications</b> to be used for storing generated classes and
     * their processed forms.
     */
    private final File privateDirectory;

    /**
     * A generator for random string values.
     */
    private final RandomString randomString;

    /**
     * Creates a new Android class loading strategy that uses the given folder for storing classes. The created
     * class loading strategy makes use of the
     * {@link net.bytebuddy.android.AndroidClassLoadingStrategy.DexProcessor.ForSdkCompiler} which uses the
     * Android SDK's dex compiler in version 1.7 which is automatically included with this strategy. Doing so,
     * the dex compiler is applied with default options.
     *
     * @param privateDirectory A directory that is <b>not shared with other applications</b> to be used for storing
     *                         generated classes and their processed forms.
     */
    public AndroidClassLoadingStrategy(File privateDirectory) {
        this(privateDirectory, DexProcessor.ForSdkCompiler.makeDefault());
    }

    /**
     * Creates a new Android class loading strategy that uses the given folder for storing classes.
     *
     * @param privateDirectory A directory that is <b>not shared with other applications</b> to be used for storing
     *                         generated classes and their processed forms.
     * @param dexProcessor     The dex processor to be used for creating a dex file out of Java files.
     */
    public AndroidClassLoadingStrategy(File privateDirectory, DexProcessor dexProcessor) {
        if (!privateDirectory.isDirectory()) {
            throw new IllegalArgumentException("Not a directory " + privateDirectory);
        }
        this.privateDirectory = privateDirectory;
        this.dexProcessor = dexProcessor;
        randomString = new RandomString();
    }

    @Override
    public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
        DexProcessor.Conversion conversion = dexProcessor.create();
        for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
            conversion.register(entry.getKey().getName(), entry.getValue());
        }
        File zipFile = new File(privateDirectory, randomString.nextString() + JAR_FILE_EXTENSION);
        try {
            if (!zipFile.createNewFile()) {
                throw new IllegalStateException("Cannot create " + zipFile);
            }
            JarOutputStream zipOutputStream = new JarOutputStream(new FileOutputStream(zipFile));
            try {
                zipOutputStream.putNextEntry(new JarEntry(DEX_CLASS_FILE));
                conversion.drainTo(zipOutputStream);
                zipOutputStream.closeEntry();
            } finally {
                zipOutputStream.close();
            }
            ClassLoader dexClassLoader = dexProcessor.makeClassLoader(zipFile, privateDirectory, classLoader);
            Map<TypeDescription, Class<?>> loadedTypes = new HashMap<TypeDescription, Class<?>>(types.size());
            for (TypeDescription typeDescription : types.keySet()) {
                try {
                    loadedTypes.put(typeDescription, dexClassLoader.loadClass(typeDescription.getName()));
                } catch (ClassNotFoundException exception) {
                    throw new IllegalStateException("Cannot load " + typeDescription, exception);
                }
            }
            return loadedTypes;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write to zip file " + zipFile, exception);
        } finally {
            if (!zipFile.delete()) {
                Logger.getAnonymousLogger().warning("Could not delete " + zipFile);
            }
        }
    }

    @Override
    public String toString() {
        return "AndroidClassLoadingStrategy{" +
                "dexProcessor=" + dexProcessor +
                ", privateDirectory=" + privateDirectory +
                ", randomString=" + randomString +
                '}';
    }

    /**
     * A dex processor is responsible for converting a collection of Java class files into a Android dex file.
     */
    public interface DexProcessor {

        /**
         * Creates a new conversion process which allows to store several Java class files in the created dex
         * file before writing this dex file to a specified {@link java.io.OutputStream}.
         *
         * @return A mutable conversion process.
         */
        Conversion create();

        /**
         * Creates a class loader capable of loading dex files.
         *
         * @param zipFile           The zip file containing a <i>classes.dex</i> file.
         * @param privateDirectory  A directory that is <b>not shared with other applications</b> to be used for
         *                          storing generated classes and their processed forms.
         * @param parentClassLoader The parent class loader.
         * @return A class loader for the specified data.
         */
        ClassLoader makeClassLoader(File zipFile, File privateDirectory, ClassLoader parentClassLoader);

        /**
         * Represents an ongoing conversion of several Java class files into an Android dex file.
         */
        interface Conversion {

            /**
             * Adds a Java class to the generated dex file.
             *
             * @param name                 The binary name of the Java class.
             * @param binaryRepresentation The binary representation of this class.
             */
            void register(String name, byte[] binaryRepresentation);

            /**
             * Writes an Android dex file containing all registered Java classes to the provided output stream.
             *
             * @param outputStream The output stream to write the generated dex file to.
             * @throws IOException If an error occurs while writing the file.
             */
            void drainTo(OutputStream outputStream) throws IOException;
        }

        /**
         * An implementation of a dex processor based on the Android SDK's <i>dx.jar</i> with an API that is
         * compatible to version 1.7.
         */
        class ForSdkCompiler implements DexProcessor {

            /**
             * An API version for a DEX file that ensures compatibility to the underlying compiler.
             */
            private static final int DEX_COMPATIBLE_API_VERSION = 13;

            /**
             * Creates a default dex processor that ensures API version compatibility.
             *
             * @return A dex processor using an SDK compiler that ensures compatibility.
             */
            protected static DexProcessor makeDefault() {
                DexOptions dexOptions = new DexOptions();
                dexOptions.targetApiLevel = DEX_COMPATIBLE_API_VERSION;
                return new ForSdkCompiler(dexOptions, new CfOptions());
            }

            /**
             * The file name extension of a Java class file.
             */
            private static final String CLASS_FILE_EXTENSION = ".class";

            /**
             * Indicates that a dex file should be written without providing a human readable output.
             */
            private static final Writer NO_PRINT_OUTPUT = null;

            /**
             * Indicates that the dex file creation should not be verbose.
             */
            private static final boolean NOT_VERBOSE = false;

            /**
             * The dex file options to be applied when converting a Java class file.
             */
            private final DexOptions dexFileOptions;

            /**
             * The dex compiler options to be applied when converting a Java class file.
             */
            private final CfOptions dexCompilerOptions;

            /**
             * Creates a new Android SDK dex compiler-based dex processor.
             *
             * @param dexFileOptions     The dex file options to apply.
             * @param dexCompilerOptions The dex compiler options to apply.
             */
            public ForSdkCompiler(DexOptions dexFileOptions, CfOptions dexCompilerOptions) {
                this.dexFileOptions = dexFileOptions;
                this.dexCompilerOptions = dexCompilerOptions;
            }

            @Override
            public DexProcessor.Conversion create() {
                return new Conversion(new DexFile(dexFileOptions));
            }

            @Override
            @SuppressFBWarnings(value = "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED", justification = "Android discourages the use access controllers")
            public ClassLoader makeClassLoader(File zipFile, File privateDirectory, ClassLoader parentClassLoader) {
                return new DexClassLoader(zipFile.getAbsolutePath(), privateDirectory.getAbsolutePath(), EMPTY_LIBRARY_PATH, parentClassLoader);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass()) &&
                        dexCompilerOptions.equals(((ForSdkCompiler) other).dexCompilerOptions)
                        && dexFileOptions.equals(((ForSdkCompiler) other).dexFileOptions);
            }

            @Override
            public int hashCode() {
                int result = dexFileOptions.hashCode();
                result = 31 * result + dexCompilerOptions.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AndroidClassLoadingStrategy.DexProcessor.ForSdkCompiler{" +
                        "dexFileOptions=" + dexFileOptions +
                        ", dexCompilerOptions=" + dexCompilerOptions +
                        '}';
            }

            /**
             * Represents a to-dex-file-conversion of a
             * {@link net.bytebuddy.android.AndroidClassLoadingStrategy.DexProcessor.ForSdkCompiler}.
             */
            protected class Conversion implements DexProcessor.Conversion {

                /**
                 * The dex file that is created by this conversion.
                 */
                private final DexFile dexFile;

                /**
                 * Creates a new ongoing to-dex-file conversion.
                 *
                 * @param dexFile The dex file that is created by this conversion.
                 */
                protected Conversion(DexFile dexFile) {
                    this.dexFile = dexFile;
                }

                @Override
                public void register(String name, byte[] binaryRepresentation) {
                    dexFile.add(CfTranslator.translate(name.replace('.', '/') + CLASS_FILE_EXTENSION,
                            binaryRepresentation,
                            dexCompilerOptions,
                            dexFileOptions));
                }

                @Override
                public void drainTo(OutputStream outputStream) throws IOException {
                    dexFile.writeTo(outputStream, NO_PRINT_OUTPUT, NOT_VERBOSE);
                }

                /**
                 * Returns the outer instance.
                 *
                 * @return The outer instance.
                 */
                private ForSdkCompiler getOuter() {
                    return ForSdkCompiler.this;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && ForSdkCompiler.this.equals(((Conversion) other).getOuter())
                            && dexFile.equals(((Conversion) other).dexFile);
                }

                @Override
                public int hashCode() {
                    return dexFile.hashCode() + 31 * ForSdkCompiler.this.hashCode();
                }

                @Override
                public String toString() {
                    return "AndroidClassLoadingStrategy.DexProcessor.ForSdkCompiler.Conversion{" +
                            "dexProcessor=" + ForSdkCompiler.this +
                            ", dexFile=" + dexFile +
                            '}';
                }
            }
        }
    }
}
