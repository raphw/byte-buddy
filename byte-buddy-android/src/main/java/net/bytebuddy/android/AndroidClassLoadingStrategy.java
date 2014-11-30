package net.bytebuddy.android;

import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.file.DexFile;
import dalvik.system.DexClassLoader;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.RandomString;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

public class AndroidClassLoadingStrategy implements ClassLoadingStrategy {

    private static final String DEX_CLASS_FILE = "classes.dex";

    private static final String JAR_FILE_EXTENSION = ".jar";

    private static final String EMPTY_LIBRARY_PATH = null;

    private final DexCreator dexCreator;

    private final File privateDirectory;

    private final RandomString randomString;

    public AndroidClassLoadingStrategy(File privateDirectory) {
        this(privateDirectory, DexProcessor.ForSdkCompiler.INSTANCE);
    }

    public AndroidClassLoadingStrategy(File privateDirectory, DexProcessor dexProcessor) {
        dexCreator = new DexCreator(nonNull(dexProcessor));
        if (!privateDirectory.isDirectory()) {
            throw new IllegalArgumentException("Not a directory " + privateDirectory);
        }
        this.privateDirectory = privateDirectory;
        randomString = new RandomString();
    }

    @Override
    public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
        DexCreator.Creation creation = dexCreator.makeDexFile();
        for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
            creation.register(entry.getKey().getName(), entry.getValue());
        }
        File zipFile = new File(privateDirectory, randomString.nextString() + JAR_FILE_EXTENSION);
        try {
            if (!zipFile.createNewFile()) {
                throw new IllegalStateException("Cannot create " + zipFile);
            }
            JarOutputStream zipOutputStream = new JarOutputStream(new FileOutputStream(zipFile));
            try {
                zipOutputStream.putNextEntry(new JarEntry(DEX_CLASS_FILE));
                creation.writeTo(zipOutputStream);
                zipOutputStream.closeEntry();
            } finally {
                zipOutputStream.close();
            }
            ClassLoader dexClassLoader = new DexClassLoader(zipFile.getAbsolutePath(),
                    privateDirectory.getAbsolutePath(),
                    EMPTY_LIBRARY_PATH,
                    classLoader);
            Map<TypeDescription, Class<?>> loadedTypes = new HashMap<TypeDescription, Class<?>>(types.size());
            for (TypeDescription typeDescription : types.keySet()) {
                try {
                    loadedTypes.put(typeDescription, dexClassLoader.loadClass(typeDescription.getName()));
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Cannot load " + typeDescription, e);
                }
            }
            return loadedTypes;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write to zip file " + zipFile, e);
        } finally {
            if (!zipFile.delete()) {
                Logger.getAnonymousLogger().warning("Could not delete " + zipFile);
            }
        }
    }

    @Override
    public String toString() {
        return "AndroidClassLoadingStrategy{" +
                "dexCreator=" + dexCreator +
                ", privateDirectory=" + privateDirectory +
                '}';
    }

    public static interface DexProcessor {

        Process makeDexFile();

        static interface Process {

            void register(String name, byte[] binaryRepresentation);

            void store(OutputStream outputStream) throws IOException;
        }

        static enum ForSdkCompiler implements DexProcessor {

            INSTANCE;

            private final DexOptions dexOptions;

            private final CfOptions cfOptions;

            private ForSdkCompiler() {
                dexOptions = new DexOptions();
                cfOptions = new CfOptions();
            }

            @Override
            public DexProcessor.Process makeDexFile() {
                return new Process(new DexFile(dexOptions));
            }

            protected static class Process implements DexProcessor.Process {

                private static final String CLASS_FILE_EXTENSION = ".class";

                private final DexFile dexFile;

                protected Process(DexFile dexFile) {
                    this.dexFile = dexFile;
                }

                @Override
                public void register(String name, byte[] binaryRepresentation) {
                    dexFile.add(CfTranslator.translate(name.replace('.', '/') + CLASS_FILE_EXTENSION,
                            binaryRepresentation,
                            ForSdkCompiler.INSTANCE.cfOptions,
                            ForSdkCompiler.INSTANCE.dexOptions));
                }

                @Override
                public void store(OutputStream outputStream) throws IOException {
                    dexFile.writeTo(outputStream, null, false);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && dexFile.equals(((Process) other).dexFile);
                }

                @Override
                public int hashCode() {
                    return dexFile.hashCode();
                }

                @Override
                public String toString() {
                    return "AndroidClassLoadingStrategy.DexProcessor.ForSdkCompiler.Process{" +
                            "dexFile=" + dexFile +
                            '}';
                }
            }
        }
    }

    protected static class DexCreator {

        protected static class Creation {

            private final DexProcessor.Process process;

            protected Creation(DexProcessor.Process process) {
                this.process = process;
            }

            protected void register(String name, byte[] bytes) {
                try {
                    process.register(name, bytes);
                } catch (Exception e) {
                    throw new IllegalStateException("Cannot translate class " + name, e);
                }
            }

            protected void writeTo(OutputStream outputStream) throws IOException {
                process.store(outputStream);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && process.equals(((Creation) other).process);
            }

            @Override
            public int hashCode() {
                return process.hashCode();
            }

            @Override
            public String toString() {
                return "AndroidClassLoadingStrategy.DexCreator.Creation{" +
                        "process=" + process +
                        '}';
            }
        }

        private final DexProcessor dexProcessor;

        public DexCreator(DexProcessor dexProcessor) {
            this.dexProcessor = dexProcessor;
        }

        protected Creation makeDexFile() {
            return new Creation(dexProcessor.makeDexFile());
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && dexProcessor.equals(((DexCreator) other).dexProcessor);
        }

        @Override
        public int hashCode() {
            return dexProcessor.hashCode();
        }

        @Override
        public String toString() {
            return "AndroidClassLoadingStrategy.DexCreator{" +
                    "dexProcessor=" + dexProcessor +
                    '}';
        }
    }
}
