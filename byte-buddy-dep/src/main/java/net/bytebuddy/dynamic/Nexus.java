package net.bytebuddy.dynamic;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <p>
 * This nexus is a global dispatcher for initializing classes with
 * {@link net.bytebuddy.implementation.LoadedTypeInitializer}s. To do so, this class is to be loaded
 * by the system class loader in an explicit manner. Any instrumented class is then injected a code
 * block into its static type initializer that makes a call to this very same nexus which had the
 * loaded type initializer registered before hand.
 * </p>
 * <p>
 * <b>Note</b>: Availability of the {@link Nexus} class and its injection into the system class loader
 * can be disabled entirely by setting the {@link Nexus#PROPERTY} system property to {@code false}.
 * </p>
 * <p>
 * <b>Important</b>: The nexus must never be accessed directly but only by the {@link NexusAccessor}
 * which makes sure that the nexus is loaded by the system class loader. Otherwise, a class might not
 * be able to initialize itself if it is loaded by different class loader that does not have the
 * system class loader in its hierarchy.
 * </p>
 */
public class Nexus extends WeakReference<ClassLoader> {

    /**
     * A system property that allows to disable the use of the {@link Nexus} class which is normally injected into the system class loader.
     */
    public static final String PROPERTY = "net.bytebuddy.nexus.disabled";

    /**
     * An type-safe constant for a non-operational reference queue.
     */
    protected static final ReferenceQueue<ClassLoader> NO_QUEUE = null;

    /**
     * A map of keys identifying a loaded type by its name and class loader mapping their
     * potential {@link net.bytebuddy.implementation.LoadedTypeInitializer} where the class
     * loader of these initializers is however irrelevant.
     */
    private static final ConcurrentMap<Nexus, Object> TYPE_INITIALIZERS = new ConcurrentHashMap<Nexus, Object>();

    /**
     * The name of a type for which a loaded type initializer is registered.
     */
    private final String name;

    /**
     * The class loader's hash code upon registration.
     */
    private final int classLoaderHashCode;

    /**
     * A random value that uniquely identifies a Nexus entry in order to avoid conflicts when
     * applying the self-initialization strategy in multiple transformations.
     */
    private final int identification;

    /**
     * Creates a key for identifying a loaded type initializer.
     *
     * @param type           The loaded type for which a key is to be created.
     * @param identification An identification for the initializer to run.
     */
    private Nexus(Class<?> type, int identification) {
        this(nonAnonymous(type.getName()), type.getClassLoader(), NO_QUEUE, identification);
    }

    /**
     * Creates a key for identifying a loaded type initializer.
     *
     * @param name           The name of a type for which a loaded type initializer is registered.
     * @param classLoader    The class loader for which a loaded type initializer is registered.
     * @param referenceQueue The reference queue to notify upon the class loader's collection or {@code null} if no queue should be notified.
     * @param identification An identification for the initializer to run.
     */
    private Nexus(String name, ClassLoader classLoader, ReferenceQueue<? super ClassLoader> referenceQueue, int identification) {
        super(classLoader, classLoader == null
                ? null
                : referenceQueue);
        this.name = name;
        classLoaderHashCode = System.identityHashCode(classLoader);
        this.identification = identification;
    }

    /**
     * Normalizes a type name if it is loaded by an anonymous class loader.
     *
     * @param typeName The name as returned by {@link Class#getName()}.
     * @return The non-anonymous name of the given class.
     */
    private static String nonAnonymous(String typeName) {
        int anonymousLoaderIndex = typeName.indexOf('/');
        return anonymousLoaderIndex == -1
                ? typeName
                : typeName.substring(0, anonymousLoaderIndex);
    }

    /**
     * <p>
     * Initializes a loaded type. This method must only be invoked via the system class loader.
     * </p>
     * <p>
     * <b>Important</b>: This method must never be called directly but only by using a {@link NexusAccessor.InitializationAppender} which enforces to
     * access this class for the system class loader to assure a VM global singleton. This avoids a duplication of the class if this nexus is loaded
     * by different class loaders. For this reason, the last parameter must not use a Byte Buddy specific type as those types can be loaded by
     * different class loaders, too. Any access of the instance is done using Java reflection instead.
     * </p>
     *
     * @param type           The loaded type to initialize.
     * @param identification An identification for the initializer to run.
     * @throws Exception If an exception occurs.
     */
    @SuppressWarnings("unused")
    public static void initialize(Class<?> type, int identification) throws Exception {
        Object typeInitializer = TYPE_INITIALIZERS.remove(new Nexus(type, identification));
        if (typeInitializer != null) {
            typeInitializer.getClass().getMethod("onLoad", Class.class).invoke(typeInitializer, type);
        }
    }

    /**
     * <p>
     * Registers a new loaded type initializer.
     * </p>
     * <p>
     * <b>Important</b>: This method must never be called directly but only by using a {@link NexusAccessor} which enforces to access this class
     * for the system class loader to assure a VM global singleton. This avoids a duplication of the class if this nexus is loaded by different class
     * loaders. For this reason, the last parameter must not use a Byte Buddy specific type as those types can be loaded by different class loaders,
     * too. Any access of the instance is done using Java reflection instead.
     * </p>
     *
     * @param name            The name of the type for the loaded type initializer.
     * @param classLoader     The class loader of the type for the loaded type initializer.
     * @param referenceQueue  The reference queue to notify upon the class loader's collection which will be enqueued a reference which can be
     *                        handed to {@link Nexus#clean(Reference)} or {@code null} if no reference queue should be notified.
     * @param identification  An identification for the initializer to run.
     * @param typeInitializer The type initializer to register. The initializer must be an instance
     *                        of {@link net.bytebuddy.implementation.LoadedTypeInitializer} where
     *                        it does however not matter which class loader loaded this latter type.
     */
    public static void register(String name, ClassLoader classLoader, ReferenceQueue<? super ClassLoader> referenceQueue, int identification, Object typeInitializer) {
        TYPE_INITIALIZERS.put(new Nexus(name, classLoader, referenceQueue, identification), typeInitializer);
    }

    /**
     * <p>
     * Cleans any stale entries from this nexus. Entries are considered stale if their class loader was collected before a class was initialized.
     * </p>
     * <p>
     * <b>Important</b>: This method must never be called directly but only by using a {@link NexusAccessor} which enforces to access this class
     * for the system class loader to assure a VM global singleton. This avoids a duplication of the class if this nexus is loaded by different class
     * loaders. For this reason, the last parameter must not use a Byte Buddy specific type as those types can be loaded by different class loaders,
     * too. Any access of the instance is done using Java reflection instead.
     * </p>
     *
     * @param reference The stale reference to clean.
     */
    public static void clean(Reference<? super ClassLoader> reference) {
        TYPE_INITIALIZERS.remove(reference);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Nexus nexus = (Nexus) other;
        return classLoaderHashCode == nexus.classLoaderHashCode
                && identification == nexus.identification
                && name.equals(nexus.name)
                && get() == nexus.get();
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + classLoaderHashCode;
        result = 31 * result + identification;
        return result;
    }

    @Override
    public String toString() {
        return "Nexus{" +
                "name='" + name + '\'' +
                ", classLoaderHashCode=" + classLoaderHashCode +
                ", identification=" + identification +
                ", classLoader=" + get() +
                '}';
    }
}
