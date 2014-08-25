package net.bytebuddy.dynamic.loading;

import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.instrumentation.type.TypeDescription;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.*;

/**
 * <p>
 * The class reloading strategy allows to redefine loaded {@link java.lang.Class}es. Note that this strategy
 * will always attempt to load an existing class prior to its redefinition, even if this class is not yet loaded.
 * </p>
 * <p>
 * <b>Note</b>: In order to redefine any type, neither its name or its modifiers must be changed. Furthermore, no
 * fields or methods must be removed.
 * </p>
 */
public class ClassReloadingStrategy implements ClassLoadingStrategy {

    /**
     * The size of the buffer.
     */
    private static final int BUFFER_SIZE = 1024;

    /**
     * A convenience variable representing the end of a stream to make the code more readable.
     */
    private static final int END_OF_STREAM = -1;

    /**
     * A convenience variable representing the first index of an array to make the code more readable.
     */
    private static final int FIRST_INDEX = 0;

    /**
     * The name of the Byte Buddy agent class.
     */
    private static final String BYTE_BUDDY_AGENT_TYPE = "net.bytebuddy.agent.ByteBuddyAgent";

    /**
     * The name of the {@code ByteBuddyAgent} class's method for obtaining an instrumentation.
     */
    private static final String GET_INSTRUMENTATION_METHOD = "getInstrumentation";

    /**
     * Base for access to a reflective member to make the code more readable.
     */
    private static final Object STATIC_METHOD = null;

    /**
     * This instance's instrumentation.
     */
    private final Instrumentation instrumentation;

    /**
     * Creates a new class reloading strategy for the given instrumentation. The given instrumentation must support
     * {@link java.lang.instrument.Instrumentation#isRedefineClassesSupported()}.
     *
     * @param instrumentation The instrumentation to be used by this reloading strategy.
     */
    public ClassReloadingStrategy(Instrumentation instrumentation) {
        if (!instrumentation.isRedefineClassesSupported()) {
            throw new IllegalArgumentException("Instrumentation does not support class redefinition: " + instrumentation);
        }
        this.instrumentation = instrumentation;
    }

    /**
     * <p>
     * Obtains a {@link net.bytebuddy.dynamic.loading.ClassReloadingStrategy} from an installed Byte Buddy agent. This
     * agent must be installed either by adding the {@code byte-buddy-agent.jar} when starting up the JVM by
     * </p>
     * <p>
     * <code>
     * java -javaagent:byte-buddy-agent.jar -jar app.jar
     * </code>
     * </p>
     * or after the start up using the Attach API. A convenience installer for the OpenJDK is provided by the
     * {@code ByteBuddyAgent} within the {@code byte-buddy-agent} module.
     *
     * @return A class reloading strategy which uses the Byte Buddy agent's {@link java.lang.instrument.Instrumentation}.
     */
    public static ClassReloadingStrategy fromInstalledAgent() {
        try {
            return new ClassReloadingStrategy((Instrumentation) ClassLoader.getSystemClassLoader()
                    .loadClass(BYTE_BUDDY_AGENT_TYPE)
                    .getDeclaredMethod(GET_INSTRUMENTATION_METHOD)
                    .invoke(STATIC_METHOD));
        } catch (Exception e) {
            throw new IllegalStateException("The Byte Buddy agent is not installed or not accessible", e);
        }
    }

    /**
     * Drains an input stream into a byte array.
     *
     * @param inputStream The input stream to drain.
     * @return A byte array containing the content of the input stream.
     * @throws IOException If the stream reading causes an error.
     */
    private static byte[] drain(InputStream inputStream) throws IOException {
        List<byte[]> previousBytes = new LinkedList<byte[]>();
        byte[] currentArray = new byte[BUFFER_SIZE];
        int currentIndex = 0;
        int currentRead;
        do {
            currentRead = inputStream.read(currentArray, currentIndex, BUFFER_SIZE - currentIndex);
            currentIndex += currentRead;
            if (currentIndex == BUFFER_SIZE) {
                previousBytes.add(currentArray);
                currentArray = new byte[BUFFER_SIZE];
            }
        } while (currentRead != END_OF_STREAM);
        byte[] result = new byte[previousBytes.size() * BUFFER_SIZE + currentIndex];
        int arrayIndex = 0;
        for (byte[] previousByte : previousBytes) {
            System.arraycopy(previousByte, FIRST_INDEX, result, arrayIndex++ * BUFFER_SIZE, BUFFER_SIZE);
        }
        System.arraycopy(currentArray, FIRST_INDEX, result, arrayIndex * BUFFER_SIZE, currentIndex);
        return result;
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

    /**
     * Resets all classes to their original definition.
     *
     * @param type The types to reset.
     * @return This class reloading strategy.
     */
    public ClassReloadingStrategy reset(Class<?>... type) {
        List<ClassDefinition> classDefinitions = new ArrayList<ClassDefinition>(type.length);
        try {
            for (Class<?> aType : type) {
                InputStream inputStream = aType.getProtectionDomain().getCodeSource().getLocation().openStream();
                try {
                    classDefinitions.add(new ClassDefinition(aType, drain(inputStream)));
                } finally {
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Exception while resetting types " + Arrays.toString(type), e);
        }
        try {
            instrumentation.redefineClasses(classDefinitions.toArray(new ClassDefinition[classDefinitions.size()]));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot locate types " + Arrays.toString(type), e);
        } catch (UnmodifiableClassException e) {
            throw new IllegalStateException("Cannot reset types " + Arrays.toString(type), e);
        }
        return this;
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
