package com.blogspot.mydailyjava.bytebuddy.proxy;

import com.blogspot.mydailyjava.bytebuddy.TypeManifestation;
import com.blogspot.mydailyjava.bytebuddy.Visibility;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.loading.ByteArrayClassLoader;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.loading.ClassLoaderByteArrayInjector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DynamicType<T> {

    static enum ClassLoadingStrategy {

        WRAPPER,
        INJECTION;

        protected Map<String, Class<?>> load(ClassLoader classLoader, Map<String, byte[]> types) {
            Map<String, Class<?>> loadedTypes = new HashMap<String, Class<?>>(types.size());
            switch (this) {
                case WRAPPER:
                    classLoader = new ByteArrayClassLoader(classLoader, types);
                    for (String name : types.keySet()) {
                        try {
                            loadedTypes.put(name, classLoader.loadClass(name));
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException("Cannot load class " + name, e);
                        }
                    }
                    break;
                case INJECTION:
                    ClassLoaderByteArrayInjector classLoaderByteArrayInjector = new ClassLoaderByteArrayInjector(classLoader);
                    for (Map.Entry<String, byte[]> entry : types.entrySet()) {
                        loadedTypes.put(entry.getKey(), classLoaderByteArrayInjector.load(entry.getKey(), entry.getValue()));
                    }
                    break;
                default:
                    throw new AssertionError();
            }
            return loadedTypes;
        }
    }

    static interface Builder<T> {

        static interface LocatedMethodInterception<T> {

            Builder<T> intercept(ByteCodeAppender.Factory byteCodeAppenderFactory);
        }

        Builder<T> implementInterface(Class<?> interfaceType);

        Builder<T> classVersion(int classVersion);

        Builder<T> name(String name);

        Builder<T> visibility(Visibility visibility);

        Builder<T> manifestation(TypeManifestation typeManifestation);

        Builder<T> makeSynthetic(boolean synthetic);

        Builder<T> ignoredMethods(MethodMatcher ignoredMethods);

        LocatedMethodInterception<T> method(MethodMatcher methodMatcher);

        Unloaded<T> make();
    }

    static interface Loaded<T> extends DynamicType<T> {

        Class<? extends T> getMainType();

        Map<String, Class<?>> getHelperTypes();
    }

    static interface Unloaded<T> extends DynamicType<T> {

        Loaded<T> load(ClassLoader classLoader, ClassLoadingStrategy classLoadingStrategy);
    }

    static class Default<T> implements DynamicType<T> {

        public static class Unloaded<T> extends Default<T> implements DynamicType.Unloaded<T> {

            public Unloaded(String mainTypeName,
                            byte[] mainTypeByte,
                            List<? extends Instrumentation.ClassLoadingCallback> classLoadingCallbacks,
                            Set<? extends DynamicType<?>> helperTypes) {
                super(mainTypeName, mainTypeByte, classLoadingCallbacks, helperTypes);
            }

            @Override
            public DynamicType.Loaded<T> load(ClassLoader classLoader, ClassLoadingStrategy classLoadingStrategy) {
                Map<String, byte[]> types = getHelperTypesRaw();
                types.put(getMainTypeName(), getMainTypeByte());
                return new Default.Loaded<T>(mainTypeName,
                        mainTypeByte,
                        classLoadingCallbacks,
                        helperTypes,
                        initialize(classLoadingStrategy.load(classLoader, types)));
            }

            private Map<String, Class<?>> initialize(Map<String, Class<?>> types) {
                for (Map.Entry<String, Instrumentation.ClassLoadingCallback> entry : getTypeInitializers().entrySet()) {
                    entry.getValue().onLoad(types.get(entry.getKey()));
                }
                return types;
            }
        }

        public static class Loaded<T> extends Default<T> implements DynamicType.Loaded<T> {

            private final Map<String, Class<?>> types;

            public Loaded(String mainTypeName,
                          byte[] mainTypeByte,
                          List<? extends Instrumentation.ClassLoadingCallback> classLoadingCallbacks,
                          Set<? extends DynamicType<?>> helperTypes,
                          Map<String, Class<?>> types) {
                super(mainTypeName, mainTypeByte, classLoadingCallbacks, helperTypes);
                this.types = types;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends T> getMainType() {
                return (Class<? extends T>) types.get(getMainTypeName());
            }

            @Override
            public Map<String, Class<?>> getHelperTypes() {
                Map<String, Class<?>> helperTypes = new HashMap<String, Class<?>>(types);
                types.remove(getMainTypeName());
                return helperTypes;
            }
        }

        private static final String CLASS_FILE_EXTENSION = ".class";

        protected final String mainTypeName;
        protected final byte[] mainTypeByte;
        protected final List<? extends Instrumentation.ClassLoadingCallback> classLoadingCallbacks;
        protected final Set<? extends DynamicType<?>> helperTypes;

        public Default(String mainTypeName,
                       byte[] mainTypeByte,
                       List<? extends Instrumentation.ClassLoadingCallback> classLoadingCallbacks,
                       Set<? extends DynamicType<?>> helperTypes) {
            this.mainTypeName = mainTypeName;
            this.mainTypeByte = mainTypeByte;
            this.classLoadingCallbacks = classLoadingCallbacks;
            this.helperTypes = helperTypes;
        }

        @Override
        public String getMainTypeName() {
            return mainTypeName;
        }

        @Override
        public Map<String, Instrumentation.ClassLoadingCallback> getTypeInitializers() {
            Instrumentation.ClassLoadingCallback classLoadingCallback = Instrumentation.ClassLoadingCallback.Compound.of(classLoadingCallbacks);
            Map<String, Instrumentation.ClassLoadingCallback> classLoadingCallbacks = new HashMap<String, Instrumentation.ClassLoadingCallback>();
            for (DynamicType<?> helperType : helperTypes) {
                classLoadingCallbacks.putAll(helperType.getTypeInitializers());
            }
            classLoadingCallbacks.put(getMainTypeName(), classLoadingCallback);
            return classLoadingCallbacks;
        }

        @Override
        public byte[] getMainTypeByte() {
            return mainTypeByte;
        }

        @Override
        public Map<String, byte[]> getHelperTypesRaw() {
            Map<String, byte[]> helperTypeByte = new HashMap<String, byte[]>(helperTypes.size());
            for (DynamicType<?> helperType : helperTypes) {
                helperTypeByte.put(helperType.getMainTypeName(), helperType.getMainTypeByte());
                helperTypeByte.putAll(helperType.getHelperTypesRaw());
            }
            return helperTypeByte;
        }

        @Override
        public File saveIn(File folder) throws IOException {
            File target = new File(folder, getMainTypeName().replace('.', File.separatorChar) + CLASS_FILE_EXTENSION);
            FileOutputStream fileOutputStream = new FileOutputStream(target);
            try {
                fileOutputStream.write(getMainTypeByte());
            } finally {
                fileOutputStream.close();
            }
            for (DynamicType<?> helperType : helperTypes) {
                helperType.saveIn(folder);
            }
            return target;
        }
    }

    String getMainTypeName();

    byte[] getMainTypeByte();

    Map<String, byte[]> getHelperTypesRaw();

    Map<String, Instrumentation.ClassLoadingCallback> getTypeInitializers();

    File saveIn(File folder) throws IOException;
}
