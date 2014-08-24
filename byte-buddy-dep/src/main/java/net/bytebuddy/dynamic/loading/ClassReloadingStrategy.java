package net.bytebuddy.dynamic.loading;

import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassReloadingStrategy implements ClassLoadingStrategy {

    public static final String BYTE_BUDDY_AGENT_TYPE = "net.bytebuddy.agent.ByteBuddyAgent";

    public static final String GET_INSTRUMENTATION_METHOD = "getInstrumentation";

    public static final Object STATIC_METHOD = null;
    private final Instrumentation instrumentation;

    public ClassReloadingStrategy(Instrumentation instrumentation) {
        if (!instrumentation.isRedefineClassesSupported()) {
            throw new IllegalArgumentException("Instrumentation does not support class redefinition: " + instrumentation);
        }
        this.instrumentation = instrumentation;
    }

    public static ClassLoadingStrategy fromInstalledAgent() {
        try {
            return new ClassReloadingStrategy((Instrumentation) ClassLoader.getSystemClassLoader()
                    .loadClass(BYTE_BUDDY_AGENT_TYPE)
                    .getDeclaredMethod(GET_INSTRUMENTATION_METHOD)
                    .invoke(STATIC_METHOD));
        } catch (Exception e) {
            throw new IllegalStateException("The Byte Buddy agent is not installed or not accessible", e);
        }
    }

    @Override
    public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
        Map<TypeDescription, Class<?>> loadedClasses = new HashMap<TypeDescription, Class<?>>(types.size());
        ClassLoaderByteArrayInjector classLoaderByteArrayInjector = new ClassLoaderByteArrayInjector(classLoader);
        List<ClassDefinition> classDefinitions = new ArrayList<ClassDefinition>(types.size());
        for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
            Class<?> type;
            try {
                type = classLoader.loadClass(entry.getKey().getName());
                classDefinitions.add(new ClassDefinition(type, entry.getValue()));
            } catch (ClassNotFoundException e) {
                type = classLoaderByteArrayInjector.inject(entry.getKey().getName(), entry.getValue());
            }
            loadedClasses.put(entry.getKey(), type);
        }
        try {
            instrumentation.redefineClasses(classDefinitions.toArray(new ClassDefinition[classDefinitions.size()]));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find class to redefine", e);
        } catch (UnmodifiableClassException e) {
            throw new IllegalStateException("Cannot redefine class", e);
        }
        return loadedClasses;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && instrumentation.equals(((ClassReloadingStrategy) other).instrumentation);
    }

    @Override
    public int hashCode() {
        return instrumentation.hashCode();
    }

    @Override
    public String toString() {
        return "ClassReloadingStrategy{instrumentation=" + instrumentation + '}';
    }
}
