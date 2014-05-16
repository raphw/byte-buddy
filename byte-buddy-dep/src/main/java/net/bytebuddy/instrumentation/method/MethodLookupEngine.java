package net.bytebuddy.instrumentation.method;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

/**
 * A method lookup engine is responsible for finding all methods that can be invoked on a given
 * {@link net.bytebuddy.instrumentation.type.TypeDescription}. This includes the resolution of overridden methods
 * in order to avoid an enlistment of duplicate methods of identical signature.
 */
public interface MethodLookupEngine {

    /**
     * Retrieves all methods that can be called on a given type. The resulting list of methods must not contain
     * any duplicates when considering byte code signatures. Furthermore, if a method is overriden, a method must
     * be contained in its most specific version. In the process, class methods must shadow interface methods of
     * identical signature. As an only deviation from the JVM's {@link Class#getMethods()}, methods of identical
     * signature of incompatible interfaces must only be returned once. These methods should be represented by some
     * sort of virtual method description which is fully aware of its state. All virtually invoked methods must be
     * contained in this lookup. Static methods, constructors and private methods must however only be contained
     * for the actual class's type.
     *
     * @param typeDescription The type for which all reachable methods should be looked up.
     * @return A list of unique, reachable methods for the given type.
     */
    MethodList getReachableMethods(TypeDescription typeDescription);

    /**
     * A factory for creating a {@link net.bytebuddy.instrumentation.method.MethodLookupEngine}.
     */
    static interface Factory {

        /**
         * Returns a {@link net.bytebuddy.instrumentation.method.MethodLookupEngine}.
         *
         * @return A {@link net.bytebuddy.instrumentation.method.MethodLookupEngine}.
         */
        MethodLookupEngine make(ClassFileVersion classFileVersion);
    }

    /**
     * This {@link net.bytebuddy.instrumentation.method.MethodDescription} represents methods that are defined
     * ambiguously on several interfaces of a common type.
     */
    static class ConflictingInterfaceMethod extends MethodDescription.AbstractMethodDescription {

        private static final int CONFLICTING_INTERFACE_MODIFIER = Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC;
        private final TypeDescription virtualHost;
        private final List<MethodDescription> methodDescriptions;

        /**
         * Creates a new conflicting interface method.
         *
         * @param virtualHost        The virtual host of the methods that are not really declared by any type.
         * @param methodDescriptions The methods that are in conflict to another. All of these methods must be
         *                           methods that are declared in an interface and none of these methods must
         *                           override another.
         */
        protected ConflictingInterfaceMethod(TypeDescription virtualHost, List<MethodDescription> methodDescriptions) {
            this.virtualHost = virtualHost;
            this.methodDescriptions = methodDescriptions;
        }

        /**
         * Creates a new method description for at least two conflicting interface methods. This factory is intended
         * for the use by {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.Default} and assumes
         * similar properties to the latter classes resolution algorithm:
         * <ul>
         * <li>It is illegal to add a method of identical byte code signature after already adding this method for a
         * sub interface where this method was overriden. It is however legal to add a method of a super interface
         * before adding a method of a sub interface.</li>
         * <li>The first argument is checked for being a
         * {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.ConflictingInterfaceMethod} and is resolved
         * accordingly. The second argument is however not considered to be a conflicting interface method.</li>
         * </ul>
         *
         * @param virtualHost       The virtual host which should be used as a declaring class for this virtual method.
         * @param conflictingMethod The method which was already registered when a new method of identical signature
         *                          was discovered. This method might itself be a conflicting interface method and is
         *                          then resolved for the methods it represents method when processing.
         * @param discoveredMethod  The new discovered method. This method must not be a conflicting interface method.
         * @return A new method description that represents the conflicting methods.
         */
        private static MethodDescription of(TypeDescription virtualHost,
                                            MethodDescription conflictingMethod,
                                            MethodDescription discoveredMethod) {
            List<MethodDescription> methodDescriptions;
            if (conflictingMethod instanceof ConflictingInterfaceMethod) {
                List<MethodDescription> known = ((ConflictingInterfaceMethod) conflictingMethod).methodDescriptions;
                methodDescriptions = new ArrayList<MethodDescription>(known.size() + 1);
                for (MethodDescription methodDescription : known) {
                    if (!methodDescription.getDeclaringType().isAssignableFrom(discoveredMethod.getDeclaringType())) {
                        methodDescriptions.add(methodDescription);
                    }
                }
                methodDescriptions.add(discoveredMethod);
            } else {
                methodDescriptions = Arrays.asList(conflictingMethod, discoveredMethod);
            }
            return new ConflictingInterfaceMethod(virtualHost, methodDescriptions);
        }

        @Override
        public TypeDescription getReturnType() {
            return methodDescriptions.get(0).getReturnType();
        }

        @Override
        public TypeList getParameterTypes() {
            return methodDescriptions.get(0).getParameterTypes();
        }

        @Override
        public Annotation[][] getParameterAnnotations() {
            return new Annotation[0][0];
        }

        @Override
        public TypeList getExceptionTypes() {
            return new TypeList.Empty();
        }

        @Override
        public boolean isConstructor() {
            return false;
        }

        @Override
        public boolean represents(Method method) {
            return false;
        }

        @Override
        public boolean represents(Constructor<?> constructor) {
            return false;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return false;
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }

        @Override
        public String getName() {
            return methodDescriptions.get(0).getName();
        }

        @Override
        public String getInternalName() {
            return methodDescriptions.get(0).getInternalName();
        }

        @Override
        public TypeDescription getDeclaringType() {
            return virtualHost;
        }

        @Override
        public int getModifiers() {
            return CONFLICTING_INTERFACE_MODIFIER;
        }

        @Override
        public boolean isSpecializableFor(TypeDescription targetType) {
            MethodDescription invokableMethod = null;
            for (MethodDescription methodDescription : methodDescriptions) {
                if (!methodDescription.isAbstract() && methodDescription.getDeclaringType().isAssignableFrom(targetType)) {
                    if (invokableMethod == null) {
                        invokableMethod = methodDescription;
                    } else {
                        return false;
                    }
                }
            }
            return invokableMethod != null;
        }

        @Override
        public String toString() {
            return "MethodLookupEngine.ConflictingInterfaceMethod{" +
                    "virtualHost=" + virtualHost +
                    ", methodDescriptions=" + methodDescriptions +
                    '}';
        }
    }

    /**
     * An abstract implementation of a method lookup engine which maintains a cache of prior look-ups.
     */
    abstract static class AbstractCachingBase implements MethodLookupEngine {

        /**
         * The cache of prior look-ups.
         */
        protected final Map<TypeDescription, MethodList> reachableMethods;

        /**
         * Creates a new instance of a caching lookup engine.
         */
        protected AbstractCachingBase() {
            reachableMethods = new HashMap<TypeDescription, MethodList>();
        }

        @Override
        public MethodList getReachableMethods(TypeDescription typeDescription) {
            MethodList result = reachableMethods.get(typeDescription);
            if (result == null) {
                result = doGetReachableMethods(typeDescription);
                reachableMethods.put(typeDescription, result);
            }
            return result;
        }

        /**
         * Retrieves the reachable methods for a non-cached method.
         *
         * @param typeDescription The type for which all reachable methods should be looked up.
         * @return A list of reachable methods which are added to the cache and returned to the requester.
         */
        protected abstract MethodList doGetReachableMethods(TypeDescription typeDescription);

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && reachableMethods.equals(((AbstractCachingBase) other).reachableMethods);
        }

        @Override
        public int hashCode() {
            return reachableMethods.hashCode();
        }

        @Override
        public String toString() {
            return "MethodLookupEngine." + getClass().getSimpleName() + "{" +
                    ", reachableMethods=" + reachableMethods +
                    '}';
        }
    }

    /**
     * A default implementation of a method lookup engine. This engine queries each type and interface for its
     * declared methods and adds them in the same order as the would be returned by calling {@link Class#getMethods()}.
     * However, conflicting interface methods are represented by
     * {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.ConflictingInterfaceMethod} instances.
     */
    static class Default extends AbstractCachingBase {

        private final boolean defaultMethodLookup;

        public Default(boolean defaultMethodLookup) {
            this.defaultMethodLookup = defaultMethodLookup;
        }

        @Override
        protected MethodList doGetReachableMethods(TypeDescription typeDescription) {
            MethodBucket methodBucket = new MethodBucket(typeDescription, defaultMethodLookup);
            Set<TypeDescription> interfaces = new HashSet<TypeDescription>();
            while ((typeDescription = typeDescription.getSupertype()) != null) {
                methodBucket.pushClass(typeDescription);
                interfaces.addAll(typeDescription.getInterfaces());
            }
            methodBucket.pushInterfaces(interfaces);
            return methodBucket.toMethodList();
        }

        /**
         * A factory for creating {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.Default} lookup
         * engines.
         */
        public static enum Factory implements MethodLookupEngine.Factory {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public MethodLookupEngine make(ClassFileVersion classFileVersion) {
                return new Default(classFileVersion.isSupportsDefaultMethods());
            }
        }

        /**
         * A method bucket is used to identify unique methods of all super types and interfaces of a given type. A
         * method bucket will therefore always filter any declared method of identical signature that was already
         * added to the bucket by pushing a previous type. For this to work, a method bucket's methods must be invoked
         * in a given order:
         * <ol>
         * <li>A user needs to push all classes into the method bucket, beginning with the most specific class down to
         * the {@link java.lang.Object} class. By pushing classes first, interface methods can never <i>override</i>
         * a method that was declared by a class.</li>
         * <li>A user needs to push all interfaces that war registered on any of these classes into the bucket. The
         * bucket will make sure that no interface is processed twice by remembering interfaces that were already
         * pushed. The method bucket will furthermore process extension interfaces of any pushed interface. Whenever
         * an interface method is registered, several possible scenarios can occur:
         * <ol>
         * <li>A method of the interface was already declared by a class: These methods are always ignored.</li>
         * <li>A method of the interface was already declared by a sub interface: In this case, the method.</li>
         * </ol>
         * </li>
         * </ol>
         */
        private static class MethodBucket {

            private static interface DefaultMethodLookup {

                boolean isEnabled(TypeDescription typeDescription);

                TypeDescription locateDispatcherType(MethodDescription methodDescription,
                                                     TypeDescription typeDescription);

                static enum Disabled implements DefaultMethodLookup {

                    INSTANCE;

                    @Override
                    public boolean isEnabled(TypeDescription typeDescription) {
                        return false;
                    }

                    @Override
                    public TypeDescription locateDispatcherType(MethodDescription methodDescription, TypeDescription typeDescription) {
                        throw new IllegalStateException();
                    }
                }

                static class Enabled implements DefaultMethodLookup {

                    private final Set<TypeDescription> hostingTypeInterfaces;

                    public Enabled(Collection<? extends TypeDescription> hostingTypeInterfaces) {
                        this.hostingTypeInterfaces = new HashSet<TypeDescription>(hostingTypeInterfaces);
                    }

                    @Override
                    public boolean isEnabled(TypeDescription typeDescription) {
                        return true;
                    }

                    @Override
                    public TypeDescription locateDispatcherType(MethodDescription methodDescription,
                                                                TypeDescription typeDescription) {
                        return hostingTypeInterfaces.contains(typeDescription)
                                ? methodDescription.getDeclaringType()
                                : typeDescription;
                    }

                    @Override
                    public String toString() {
                        return "MethodBucket.DefaultMethodLookup.Enabled{" +
                                "hostingTypeInterfaces=" + hostingTypeInterfaces +
                                '}';
                    }
                }
            }

            /**
             * A map of class methods by their unique signature, represented as strings.
             */
            private final Map<String, MethodDescription> classMethods;

            /**
             * A map of interface methods by their unique signature, represented as strings.
             */
            private final Map<String, MethodDescription> interfaceMethods;

            /**
             * A marker pool of types that were already pushed into this bucket.
             */
            private final Set<TypeDescription> processedTypes;

            private final Map<MethodDescription, TypeDescription> defaultMethods;

            private final TypeDescription virtualHost;

            private final MethodMatcher virtualMethodMatcher;

            /**
             * Creates a new mutable method bucket.
             */
            private MethodBucket(TypeDescription hostingType, boolean defaultMethodLookup) {
                this.virtualHost = hostingType;
                classMethods = new HashMap<String, MethodDescription>();
                interfaceMethods = new HashMap<String, MethodDescription>();
                processedTypes = new HashSet<TypeDescription>();
                defaultMethods = new HashMap<MethodDescription, TypeDescription>();
                virtualMethodMatcher = isMethod().and(not(isPrivate().or(isStatic()).or(isPackagePrivate().and(not(isVisibleTo(hostingType))))));
                pushClass(hostingType, any());
                TypeList hostingTypeInterfaces = hostingType.getInterfaces();
                pushInterfaces(hostingTypeInterfaces, defaultMethodLookup
                        ? new DefaultMethodLookup.Enabled(hostingTypeInterfaces)
                        : DefaultMethodLookup.Disabled.INSTANCE);
            }

            private void pushClass(TypeDescription typeDescription) {
                pushClass(typeDescription, virtualMethodMatcher);
            }

            /**
             * Pushes all methods of a given class into the bucket. Classes must be pushed before pushing any interfaces
             * because Java considers class methods as dominant over interface methods. Furthermore, more specific
             * classes must be pushed first in order to respect method overrides.
             *
             * @param typeDescription The (non-interface) class to push into the bucket.
             */
            private void pushClass(TypeDescription typeDescription, MethodMatcher methodMatcher) {
                if (processedTypes.add(typeDescription)) {
                    for (MethodDescription methodDescription : typeDescription.getDeclaredMethods().filter(methodMatcher)) {
                        String uniqueSignature = methodDescription.getUniqueSignature();
                        if (!classMethods.containsKey(uniqueSignature)) {
                            classMethods.put(uniqueSignature, methodDescription);
                        }
                    }
                }
            }

            /**
             * Pushes a collection of interfaces into the bucket. This method must not be called before pushing
             * all classes into the bucket.
             *
             * @param typeDescriptions A collection of interfaces to push into the bucket. Duplicates will be
             *                         filtered automatically.
             */
            private void pushInterfaces(Collection<? extends TypeDescription> typeDescriptions) {
                pushInterfaces(typeDescriptions, DefaultMethodLookup.Disabled.INSTANCE);
            }

            private void pushInterfaces(Collection<? extends TypeDescription> typeDescriptions,
                                        DefaultMethodLookup defaultMethodLookup) {
                Set<String> processedMethods = new HashSet<String>(classMethods.keySet());
                for (TypeDescription interfaceTypeDescription : typeDescriptions) {
                    pushInterface(interfaceTypeDescription, processedMethods, defaultMethodLookup);
                }
            }

            /**
             * This extraction algorithm adds all methods of an interface and this interface's extended interfaces.
             * For this to work, the extraction is executed in several stages:
             * <ol>
             * <li>An interface is globally marked as <i>processed</i>. After this, the extraction is never applied
             * again for this interface.</li>
             * <li>For each method, it is checked if the method is already considered as processed. If this is the
             * case, the method is skipped. A method is always marked as processed if it was already declared by a
             * (non-interface) class.</li>
             * <li>If a new non-processed method is discovered, a method of identical signature might already be
             * registered in the bucket. When registering a new interface method, there are four possible scenarios:
             * <ol>
             * <li>No other interface has declared a method of identical signature. In this case, the method can
             * merely be added to the map of registered interface methods.</li>
             * <li>A super type interface has registered a method of identical signature. In this case, the new
             * method overrides that method. We can therefore replace the already registered method with the new
             * method.</li>
             * <li>A sub type interface has registered a method of identical signature. This scenario can however
             * never occur. This property is explained later in this documentation.</li>
             * <li>A non-compatible interface has declared a method of identical signature. In this case, the interface
             * method exists twice from the view of the Java virtual machine. As a matter of fact,
             * {@link Class#getMethods()} returns both methods of identical signature, in this case. This is however
             * impractical for Byte Buddy which would then define two identical methods on overriding what is
             * illegal for the JVM's view. We will therefore merge both methods to a synthetic method which is
             * aware of this conflicting property.</li>
             * </ol>
             * </li>
             * </ol>
             * When an interface is pushed into this bucket, it will subsequently process all interfaces that are
             * implemented by this interface. For their processing, an extended set of {@code processedMethods} is
             * supplied where all processed methods of this interface are added to the original set. This way, it
             * is impossible to discover a method of a sub interface when processing an interface. Additionally, if the
             * same super interface was pushed into the bucket at a later point, the interface would not be processed
             * again since it was already marked as processed by adding it to
             * {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.Default.MethodBucket#processedTypes}.
             *
             * @param typeDescription  The interface type to process.
             * @param processedMethods A set of unique method signatures that were already processed.
             */
            private void pushInterface(TypeDescription typeDescription,
                                       Set<String> processedMethods,
                                       DefaultMethodLookup defaultMethodLookup) {
                Set<String> locallyProcessedMethods = new HashSet<String>(processedMethods);
                if (processedTypes.add(typeDescription)) {
                    for (MethodDescription methodDescription : typeDescription.getDeclaredMethods().filter(virtualMethodMatcher)) {
                        String uniqueSignature = methodDescription.getUniqueSignature();
                        if (!processedMethods.contains(uniqueSignature)) {
                            locallyProcessedMethods.add(uniqueSignature);
                            MethodDescription conflictingMethod = interfaceMethods.get(uniqueSignature);
                            if (conflictingMethod != null && !conflictingMethod.getDeclaringType().isAssignableFrom(typeDescription)) {
                                methodDescription = ConflictingInterfaceMethod.of(virtualHost, conflictingMethod, methodDescription);
                            }
                            interfaceMethods.put(uniqueSignature, methodDescription);
                        }
                        if (defaultMethodLookup.isEnabled(typeDescription)) {
                            if (methodDescription.isDefaultMethod()) {
                                defaultMethods.put(methodDescription, defaultMethodLookup.locateDispatcherType(methodDescription, typeDescription));
                            }
                        }
                    }
                    for (TypeDescription interfaceType : typeDescription.getInterfaces()) {
                        pushInterface(interfaceType, locallyProcessedMethods, defaultMethodLookup);
                    }
                }
            }

            private MethodList toMethodList() {
                List<MethodDescription> methodDescriptions = new ArrayList<MethodDescription>(classMethods.size() + interfaceMethods.size());
                methodDescriptions.addAll(classMethods.values());
                methodDescriptions.addAll(interfaceMethods.values());
                return new MethodList.Explicit(methodDescriptions);
            }

            @Override
            public String toString() {
                return "MethodBucket{" +
                        "classMethods=" + classMethods +
                        ", interfaceMethods=" + interfaceMethods +
                        ", processedTypes=" + processedTypes +
                        ", virtualHost=" + virtualHost +
                        ", virtualMethodMatcher=" + virtualMethodMatcher +
                        '}';
            }
        }
    }
}
