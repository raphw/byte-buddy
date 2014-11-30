package net.bytebuddy.android;

import dalvik.system.DexClassLoader;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.instrumentation.type.TypeDescription;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

public class AndroidClassLoadingStrategy implements ClassLoadingStrategy {

    private static final String EMPTY_LIBRARY_PATH = "";

    private final DexCreator dexCreator;

    private final File directory;

    public AndroidClassLoadingStrategy(File directory) {
        this(directory, DexProcessor.ForSdkCompiler.INSTANCE);
    }

    public AndroidClassLoadingStrategy(File directory, DexProcessor dexProcessor) {
        dexCreator = new DexCreator(nonNull(dexProcessor));
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Not a directory " + directory);
        }
        this.directory = directory;
    }

    @Override
    public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
        DexCreator.Creation creation = dexCreator.makeDexFile();
        for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
            creation.register(entry.getKey().getName(), entry.getValue());
        }
        File dexFile = new File(directory, UUID.randomUUID().toString());
        try {

            try {
                if (!dexFile.createNewFile()) {
                    throw new IllegalStateException("Cannot create " + dexFile);
                }
                creation.writeTo(dexFile);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot write dex file", e);
            }
            ClassLoader dexClassLoader = new DexClassLoader(dexFile.getAbsolutePath(),
                    directory.getAbsolutePath(),
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
        } finally {
            if (!dexFile.delete()) {
                Logger.getAnonymousLogger().warning("Could not delete " + dexFile);
            }
        }
    }

    @Override
    public String toString() {
        return "AndroidClassLoadingStrategy{" +
                "dexCreator=" + dexCreator +
                ", directory=" + directory +
                '}';
    }

    public static interface DexProcessor {

        Process createNewDexFile();

        static interface Process {

            void register(String name, byte[] binaryRepresentation);

            void store(Writer writer);
        }

        static enum ForSdkCompiler implements DexProcessor {

            INSTANCE(ForSdkCompiler.class.getClassLoader());

            private final Handler handler;

            private ForSdkCompiler(ClassLoader classLoader) {
                Handler handler;
                try {
                    handler = new Handler.Default(classLoader);
                } catch (Exception e) {
                    handler = new Handler.Exceptional(e);
                }
                this.handler = handler;
            }

            @Override
            public DexProcessor.Process createNewDexFile() {
                try {
                    return new Process(handler.getDexFileConstructor().newInstance(handler.getDexOptions()));
                } catch (Throwable e) {
                    throw new IllegalStateException("Cannot create new dex file", e);
                }
            }

            public boolean isAvailable() {
                return handler.isHealthy();
            }

            protected static class Process implements DexProcessor.Process {

                private static final Object STATIC_METHOD = null;

                private static final String CLASS_FILE_EXTENSION = ".class";

                private final Object dexFile;

                protected Process(Object dexFile) {
                    this.dexFile = dexFile;
                }

                @Override
                public void register(String name, byte[] binaryRepresentation) {
                    try {
                        ForSdkCompiler.INSTANCE.handler.getDexAddMethod().invoke(dexFile,
                                ForSdkCompiler.INSTANCE.handler.getDexCompileMethod().invoke(STATIC_METHOD,
                                        name.replace('.', '/') + CLASS_FILE_EXTENSION,
                                        binaryRepresentation,
                                        ForSdkCompiler.INSTANCE.handler.getCompilerOptions(),
                                        ForSdkCompiler.INSTANCE.handler.getDexOptions()));
                    } catch (Throwable e) {
                        throw new IllegalStateException("Cannot register " + name, e);
                    }
                }

                @Override
                public void store(Writer writer) {
                    try {
                        ForSdkCompiler.INSTANCE.handler.getDexToFileMethod().invoke(dexFile, writer, false);
                    } catch (Throwable e) {
                        throw new IllegalStateException("Cannot write dex file", e);
                    }
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

            protected static interface Handler {

                Object getCompilerOptions();

                Object getDexOptions();

                Constructor<?> getDexFileConstructor();

                Method getDexAddMethod();

                Method getDexToFileMethod();

                Method getDexCompileMethod();

                boolean isHealthy();

                static class Default implements Handler {

                    private final Object compilerOptions;

                    private final Object dexOptions;

                    private final Constructor<?> dexFileConstructor;

                    private final Method dexAddMethod;

                    private final Method dexToFileMethod;

                    private final Method dexCompileMethod;

                    public Default(ClassLoader classLoader) throws Exception {
                        Class<?> dexCompilerOptionsType = classLoader.loadClass("com.android.dx.dex.cf.CfOptions");
                        compilerOptions = dexCompilerOptionsType.newInstance();
                        Class<?> dexOptionsType = classLoader.loadClass("com.android.dx.dex.DexOptions");
                        dexOptions = dexOptionsType.newInstance();
                        Class<?> dexFileType = classLoader.loadClass("com.android.dx.dex.file.DexFile");
                        dexFileConstructor = dexFileType.getDeclaredConstructor(dexOptionsType);
                        dexAddMethod = dexFileType.getDeclaredMethod("add", classLoader.loadClass("com.android.dx.dex.file.ClassDefItem"));
                        dexToFileMethod = dexFileType.getDeclaredMethod("toDex", Writer.class, boolean.class);
                        dexCompileMethod = classLoader.loadClass("com.android.dx.dex.cf.CfTranslator")
                                .getDeclaredMethod("translate", String.class, byte[].class, dexCompilerOptionsType, dexOptionsType);
                    }

                    public Object getCompilerOptions() {
                        return compilerOptions;
                    }

                    public Object getDexOptions() {
                        return dexOptions;
                    }

                    public Constructor<?> getDexFileConstructor() {
                        return dexFileConstructor;
                    }

                    public Method getDexAddMethod() {
                        return dexAddMethod;
                    }

                    public Method getDexToFileMethod() {
                        return dexToFileMethod;
                    }

                    public Method getDexCompileMethod() {
                        return dexCompileMethod;
                    }

                    @Override
                    public boolean isHealthy() {
                        return true;
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        Default aDefault = (Default) other;
                        return compilerOptions.equals(aDefault.compilerOptions)
                                && dexAddMethod.equals(aDefault.dexAddMethod)
                                && dexCompileMethod.equals(aDefault.dexCompileMethod)
                                && dexFileConstructor.equals(aDefault.dexFileConstructor)
                                && dexOptions.equals(aDefault.dexOptions)
                                && dexToFileMethod.equals(aDefault.dexToFileMethod);
                    }

                    @Override
                    public int hashCode() {
                        int result = compilerOptions.hashCode();
                        result = 31 * result + dexOptions.hashCode();
                        result = 31 * result + dexFileConstructor.hashCode();
                        result = 31 * result + dexAddMethod.hashCode();
                        result = 31 * result + dexToFileMethod.hashCode();
                        result = 31 * result + dexCompileMethod.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "AndroidClassLoadingStrategy.DexProcessor.ForSdkCompiler.Handler.Default{" +
                                "compilerOptions=" + compilerOptions +
                                ", dexOptions=" + dexOptions +
                                ", dexFileConstructor=" + dexFileConstructor +
                                ", dexAddMethod=" + dexAddMethod +
                                ", dexToFileMethod=" + dexToFileMethod +
                                ", dexCompileMethod=" + dexCompileMethod +
                                '}';
                    }
                }

                static class Exceptional implements Handler {

                    private static final String MESSAGE = "It seems like the required 'dx.jar' is not available.";

                    private final Exception exception;

                    public Exceptional(Exception exception) {
                        this.exception = exception;
                    }

                    @Override
                    public Object getCompilerOptions() {
                        throw new IllegalStateException(MESSAGE, exception);
                    }

                    @Override
                    public Object getDexOptions() {
                        throw new IllegalStateException(MESSAGE, exception);
                    }

                    @Override
                    public Constructor<?> getDexFileConstructor() {
                        throw new IllegalStateException(MESSAGE, exception);
                    }

                    @Override
                    public Method getDexAddMethod() {
                        throw new IllegalStateException(MESSAGE, exception);
                    }

                    @Override
                    public Method getDexToFileMethod() {
                        throw new IllegalStateException(MESSAGE, exception);
                    }

                    @Override
                    public Method getDexCompileMethod() {
                        throw new IllegalStateException(MESSAGE, exception);
                    }

                    @Override
                    public boolean isHealthy() {
                        return false;
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && exception.equals(((Exceptional) other).exception);
                    }

                    @Override
                    public int hashCode() {
                        return exception.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "AndroidClassLoadingStrategy.DexProcessor.ForSdkCompiler.Handler.Exceptional{" +
                                "exception=" + exception +
                                '}';
                    }
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

            protected void writeTo(File file) throws IOException {
                Writer writer = new OutputStreamWriter(new FileOutputStream(file));
                try {
                    process.store(writer);
                } finally {
                    writer.close();
                }
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
            return new Creation(dexProcessor.createNewDexFile());
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
