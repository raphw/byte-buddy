package net.bytebuddy.instrumentation.method;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.Opcodes;

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
     * sort of virtual method description which is fully aware of its state. All virtually invokable methods must be
     * contained in this lookup. Static methods, constructors and private methods must however only be contained
     * for the actual class's type.
     * <p>&nbsp;</p>
     * The
     *
     * @param typeDescription The type for which all invokable methods should be looked up.
     * @return The looked up methods for the given type.
     */
    Finding process(TypeDescription typeDescription);

    /**
     * A factory for creating a {@link net.bytebuddy.instrumentation.method.MethodLookupEngine}.
     */
    static interface Factory {

        /**
         * Returns a {@link net.bytebuddy.instrumentation.method.MethodLookupEngine}.
         *
         * @param extractDefaultMethods {@code true} if interface default methods should be resolved.
         * @return A {@link net.bytebuddy.instrumentation.method.MethodLookupEngine}.
         */
        MethodLookupEngine make(boolean extractDefaultMethods);
    }

    /**
     * A finding contains a class's extracted invokable methods which were computed by a
     * {@link net.bytebuddy.instrumentation.method.MethodLookupEngine}.
     */
    static interface Finding {

        /**
         * Returns the type description for which the finding was created.
         *
         * @return The type description for which the finding was created.
         */
        TypeDescription getTypeDescription();

        /**
         * Returns a list of methods that can be invoked on the analyzed type.
         *
         * @return A list of methods that can be invoked on the analyzed type.
         */
        MethodList getInvokableMethods();

        /**
         * Returns a map of interfaces that are eligible for default method invocation on the type this finding
         * was created for.
         *
         * @return A map of interfaces that are eligible for default method invocation on the type this finding
         * was created for.
         */
        Map<TypeDescription, Set<MethodDescription>> getInvokableDefaultMethods();

        /**
         * A default implementation of a {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.Finding}.
         */
        static class Default implements Finding {

            /**
             * The type that was analyzed for creating this finding.
             */
            private final TypeDescription lookedUpType;

            /**
             * A list of methods that are invokable on this type.
             */
            private final MethodList invokableMethods;

            /**
             * A map of interfaces that are eligible for default method invocation on the type this finding
             * was created for.
             */
            private final Map<TypeDescription, Set<MethodDescription>> invokableDefaultMethods;

            /**
             * Creates a default of a {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.Finding}.
             *
             * @param lookedUpType            The type that was analyzed for creating this finding.
             * @param invokableMethods        A list of methods that are invokable on this type.
             * @param invokableDefaultMethods A map of interfaces that are eligible for default method invocation on
             *                                the type this finding was created for.
             */
            public Default(TypeDescription lookedUpType,
                           MethodList invokableMethods,
                           Map<TypeDescription, Set<MethodDescription>> invokableDefaultMethods) {
                this.lookedUpType = lookedUpType;
                this.invokableMethods = invokableMethods;
                this.invokableDefaultMethods = invokableDefaultMethods;
            }

            @Override
            public TypeDescription getTypeDescription() {
                return lookedUpType;
            }

            @Override
            public MethodList getInvokableMethods() {
                return invokableMethods;
            }

            @Override
            public Map<TypeDescription, Set<MethodDescription>> getInvokableDefaultMethods() {
                return invokableDefaultMethods;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Default aDefault = (Default) other;
                return invokableDefaultMethods.equals(aDefault.invokableDefaultMethods)
                        && invokableMethods.equals(aDefault.invokableMethods)
                        && lookedUpType.equals(aDefault.lookedUpType);
            }

            @Override
            public int hashCode() {
                int result = lookedUpType.hashCode();
                result = 31 * result + invokableMethods.hashCode();
                result = 31 * result + invokableDefaultMethods.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodLookupEngine.Finding.Default{" +
                        "lookedUpType=" + lookedUpType +
                        ", defaultMethods=" + invokableMethods +
                        ", invokableDefaultMethods=" + invokableDefaultMethods +
                        '}';
            }
        }
    }

    /**
     * This method description represents a method that is defined in a non-interface type and overrides a method
     * in another class it directly or indirectly extends.
     */
    static class OverridenClassMethod extends MethodDescription.AbstractMethodDescription {

        /**
         * Describes the index of the most specific method in the method chain in order to improve
         * the readability of the code.
         */
        private static final int MOST_SPECIFIC = 0;

        /**
         * A list of overridden methods starting with the most specific method going down to the least specific.
         */
        private final List<MethodDescription> methodChain;

        /**
         * Creates a new overriding class method.
         *
         * @param methodChain A list of overridden methods starting with the most specific method going down to the
         *                    least specific.
         */
        protected OverridenClassMethod(List<MethodDescription> methodChain) {
            this.methodChain = methodChain;
        }

        /**
         * Creates a new method description of an overriding method to an overriden method. The overriding method is
         * considered to be a {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.OverridenClassMethod}
         * itself and is resolved appropriately.
         *
         * @param overridingMethod The most specific method that is overriding another method.
         * @param overriddenMethod The method that is overridden by the {@code overridingMethod}.
         * @return A method description that represents the overriding method while considering how to properly
         * specialize on invoking the overriden method.
         */
        public static MethodDescription of(MethodDescription overridingMethod, MethodDescription overriddenMethod) {
            List<MethodDescription> methodChain;
            if (overridingMethod instanceof OverridenClassMethod) {
                OverridenClassMethod overridenClassMethod = (OverridenClassMethod) overridingMethod;
                methodChain = new ArrayList<MethodDescription>(overridenClassMethod.methodChain.size() + 1);
                methodChain.addAll(overridenClassMethod.methodChain);
            } else {
                methodChain = new ArrayList<MethodDescription>(2);
                methodChain.add(overridingMethod);
            }
            methodChain.add(overriddenMethod);
            return new OverridenClassMethod(methodChain);
        }

        @Override
        public TypeDescription getReturnType() {
            return methodChain.get(MOST_SPECIFIC).getReturnType();
        }

        @Override
        public TypeList getParameterTypes() {
            return methodChain.get(MOST_SPECIFIC).getParameterTypes();
        }

        @Override
        public List<AnnotationList> getParameterAnnotations() {
            return methodChain.get(MOST_SPECIFIC).getParameterAnnotations();
        }

        @Override
        public TypeList getExceptionTypes() {
            return methodChain.get(MOST_SPECIFIC).getExceptionTypes();
        }

        @Override
        public boolean isConstructor() {
            return methodChain.get(MOST_SPECIFIC).isConstructor();
        }

        @Override
        public boolean isTypeInitializer() {
            return methodChain.get(MOST_SPECIFIC).isTypeInitializer();
        }

        @Override
        public boolean represents(Method method) {
            return methodChain.get(MOST_SPECIFIC).represents(method);
        }

        @Override
        public boolean represents(Constructor<?> constructor) {
            return methodChain.get(MOST_SPECIFIC).represents(constructor);
        }

        @Override
        public String getName() {
            return methodChain.get(MOST_SPECIFIC).getName();
        }

        @Override
        public String getInternalName() {
            return methodChain.get(MOST_SPECIFIC).getInternalName();
        }

        @Override
        public TypeDescription getDeclaringType() {
            return methodChain.get(MOST_SPECIFIC).getDeclaringType();
        }

        @Override
        public int getModifiers() {
            return methodChain.get(MOST_SPECIFIC).getModifiers();
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return methodChain.get(MOST_SPECIFIC).getDeclaredAnnotations();
        }

        @Override
        public AnnotationList getAnnotations() {
            return null; // TODO
        }

        @Override
        public boolean isSpecializableFor(TypeDescription targetType) {
            for (MethodDescription methodDescription : methodChain) {
                if (methodDescription.isSpecializableFor(targetType)) {
                    return true;
                } else if (methodDescription.getDeclaringType().isAssignableFrom(targetType)) {
                    return false;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "MethodLookupEngine.OverridenClassMethod{" +
                    "methodChain=" + methodChain +
                    '}';
        }
    }

    /**
     * This {@link net.bytebuddy.instrumentation.method.MethodDescription} represents methods that are defined
     * ambiguously on several interfaces of a common type.
     */
    static class ConflictingInterfaceMethod extends MethodDescription.AbstractMethodDescription {

        /**
         * An index that is guaranteed to exist but that expresses the fact that any method that is represented
         * by an instance of this class defines identical signatures by definition.
         */
        private static final int ANY = 0;

        /**
         * The modifiers for a conflicting interface method.
         */
        private static final int CONFLICTING_INTERFACE_MODIFIER = Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC;

        /**
         * The virtual host of this conflicting interface method. Usually the instrumented type for which a
         * method lookup is performed.
         */
        private final TypeDescription virtualHost;

        /**
         * The method descriptions that are represented by this instance.
         */
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
        protected static MethodDescription of(TypeDescription virtualHost,
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
            return methodDescriptions.get(ANY).getReturnType();
        }

        @Override
        public TypeList getParameterTypes() {
            return methodDescriptions.get(ANY).getParameterTypes();
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
        public boolean isTypeInitializer() {
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
        public List<AnnotationList> getParameterAnnotations() {
            return AnnotationList.Empty.asList(methodDescriptions.get(ANY).getParameterTypes().size());
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Empty();
        }

        @Override
        public AnnotationList getAnnotations() {
            return new AnnotationList.Empty();
        }

        @Override
        public String getName() {
            return methodDescriptions.get(ANY).getName();
        }

        @Override
        public String getInternalName() {
            return methodDescriptions.get(ANY).getInternalName();
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
                    "typeOfInterest=" + virtualHost +
                    ", methodDescriptions=" + methodDescriptions +
                    '}';
        }
    }

    /**
     * A default implementation of a method lookup engine. This engine queries each type and interface for its
     * declared methods and adds them in the same order as the would be returned by calling {@link Class#getMethods()}.
     * However, conflicting interface methods are represented by
     * {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.ConflictingInterfaceMethod} instances.
     */
    static class Default implements MethodLookupEngine {

        /**
         * Determines if default method lookup is enabled.
         */
        private final DefaultMethodLookup defaultMethodLookup;

        /**
         * Creates a new default method lookup engine.
         *
         * @param defaultMethodLookup Determines if default method lookup is enabled.
         */
        public Default(DefaultMethodLookup defaultMethodLookup) {
            this.defaultMethodLookup = defaultMethodLookup;
        }

        @Override
        public Finding process(TypeDescription typeDescription) {
            MethodBucket methodBucket = new MethodBucket(typeDescription);
            Set<TypeDescription> interfaces = new HashSet<TypeDescription>();
            TypeList defaultMethodRelevantInterfaces = typeDescription.getInterfaces();
            while ((typeDescription = typeDescription.getSupertype()) != null) {
                methodBucket.pushClass(typeDescription);
                interfaces.addAll(typeDescription.getInterfaces());
            }
            Map<TypeDescription, Set<MethodDescription>> defaultMethods = defaultMethodLookup.apply(methodBucket,
                    interfaces,
                    defaultMethodRelevantInterfaces);
            methodBucket.pushInterfaces(interfaces);
            return new Finding.Default(methodBucket.getTypeOfInterest(),
                    methodBucket.extractInvokableMethods(),
                    defaultMethods);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && defaultMethodLookup == ((Default) other).defaultMethodLookup;
        }

        @Override
        public int hashCode() {
            return defaultMethodLookup.hashCode();
        }

        @Override
        public String toString() {
            return "MethodLookupEngine.Default{" +
                    "defaultMethodLookup=" + defaultMethodLookup +
                    '}';
        }

        /**
         * Determines if default methods are extracted when analyzing a given type. This might not be relevant in
         * some contexts and is normally fully irrelevant when writing types in class file formats that do not
         * support default methods.
         */
        public static enum DefaultMethodLookup {

            /**
             * Enables the extraction of default methods.
             */
            ENABLED {
                @Override
                public Map<TypeDescription, Set<MethodDescription>> apply(MethodBucket methodBucket,
                                                                          Collection<TypeDescription> interfaces,
                                                                          Collection<TypeDescription> defaultMethodRelevantInterfaces) {
                    interfaces.removeAll(defaultMethodRelevantInterfaces);
                    return Collections.unmodifiableMap(methodBucket.pushInterfacesAndExtractDefaultMethods(defaultMethodRelevantInterfaces));
                }
            },

            /**
             * Disables the extraction of default methods.
             */
            DISABLED {
                @Override
                public Map<TypeDescription, Set<MethodDescription>> apply(MethodBucket methodBucket,
                                                                          Collection<TypeDescription> interfaces,
                                                                          Collection<TypeDescription> defaultMethodRelevantInterfaces) {
                    interfaces.addAll(defaultMethodRelevantInterfaces);
                    return Collections.emptyMap();
                }
            };

            /**
             * Applies default method extraction.
             *
             * @param methodBucket                    The method bucket that is used for performing a method extraction.
             * @param interfaces                      The interfaces of the instrumented type.
             * @param defaultMethodRelevantInterfaces The interfaces of the instrumented type that are relevant for
             *                                        default method extraction.
             * @return A map containing all extracted default methods.
             */
            public abstract Map<TypeDescription, Set<MethodDescription>> apply(MethodBucket methodBucket,
                                                                               Collection<TypeDescription> interfaces,
                                                                               Collection<TypeDescription> defaultMethodRelevantInterfaces);
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
            public MethodLookupEngine make(boolean extractDefaultMethods) {
                return new Default(extractDefaultMethods
                        ? DefaultMethodLookup.ENABLED
                        : DefaultMethodLookup.DISABLED);
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

            /**
             * The most specific type for which this method bucket extracts methods. This type is the only type
             * for which all methods are extracted since any method that are declared by this type fully belong
             * to it. Any other super type or interface type only inherit their virtual members to this type.
             */
            private final TypeDescription typeOfInterest;

            /**
             * A method matcher that matches any method that is inherited by the
             * {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.Default.MethodBucket#typeOfInterest}.
             */
            private final MethodMatcher virtualMethodMatcher;

            /**
             * Creates a new mutable method bucket.
             *
             * @param typeOfInterest The type for which a type extraction is performed.
             */
            private MethodBucket(TypeDescription typeOfInterest) {
                this.typeOfInterest = typeOfInterest;
                classMethods = new HashMap<String, MethodDescription>();
                interfaceMethods = new HashMap<String, MethodDescription>();
                processedTypes = new HashSet<TypeDescription>();
                virtualMethodMatcher = isMethod().and(not(isPrivate()
                        .or(isStatic())
                        .or(isPackagePrivate().and(not(isVisibleTo(typeOfInterest))))));
                pushClass(typeOfInterest, any());
            }

            /**
             * Pushes a new class into the bucket where all virtual methods relatively to the type of interest are
             * extracted.
             *
             * @param typeDescription The class for which all virtual members are to be extracted.
             */
            private void pushClass(TypeDescription typeDescription) {
                pushClass(typeDescription, virtualMethodMatcher);
            }

            /**
             * Pushes all methods of a given class into the bucket. Classes must be pushed before pushing any interfaces
             * because Java considers class methods as dominant over interface methods. Furthermore, more specific
             * classes must be pushed first in order to respect method overrides.
             *
             * @param typeDescription The (non-interface) class to push into the bucket.
             * @param methodMatcher   The method matcher for filtering methods of interest that are declared by the
             *                        given type.
             */
            private void pushClass(TypeDescription typeDescription, MethodMatcher methodMatcher) {
                if (processedTypes.add(typeDescription)) {
                    for (MethodDescription methodDescription : typeDescription.getDeclaredMethods().filter(methodMatcher)) {
                        String uniqueSignature = methodDescription.getUniqueSignature();
                        MethodDescription overridingMethod = classMethods.get(uniqueSignature);
                        classMethods.put(uniqueSignature, overridingMethod == null
                                ? methodDescription
                                : OverridenClassMethod.of(overridingMethod, methodDescription));
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

            /**
             * Pushes a collection of interfaces into the bucket which are additionally researched for invokable default
             * methods.
             *
             * @param typeDescriptions A collection of interfaces to push into the bucket. Duplicates will be
             *                         filtered automatically. All interfaces will additionally be analyzed for
             *                         default methods.
             * @return A map of all extracted default methods where each interface is mapped to its invokable default
             * methods.
             */
            private Map<TypeDescription, Set<MethodDescription>> pushInterfacesAndExtractDefaultMethods(Collection<? extends TypeDescription> typeDescriptions) {
                DefaultMethodLookup.Enabled defaultMethodLookup = new DefaultMethodLookup.Enabled(typeDescriptions);
                pushInterfaces(typeDescriptions, defaultMethodLookup);
                return defaultMethodLookup.materialize();
            }

            /**
             * Pushes a collection of interfaces into the bucket.
             *
             * @param typeDescriptions    A collection of interfaces to push into the bucket. Duplicates will be
             *                            filtered automatically.
             * @param defaultMethodLookup An implementation for looking up default methods for these interfaces.
             */
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
             * @param typeDescription             The interface type to process.
             * @param processedMethodsInHierarchy A set of unique method signatures that were already processed.
             * @param defaultMethodLookup         A processor for performing a lookup of default methods.
             */
            private void pushInterface(TypeDescription typeDescription,
                                       Set<String> processedMethodsInHierarchy,
                                       DefaultMethodLookup defaultMethodLookup) {
                Set<String> locallyProcessedMethods = new HashSet<String>(processedMethodsInHierarchy);
                if (processedTypes.add(typeDescription)) {
                    defaultMethodLookup.begin(typeDescription);
                    for (MethodDescription methodDescription : typeDescription.getDeclaredMethods().filter(virtualMethodMatcher)) {
                        String uniqueSignature = methodDescription.getUniqueSignature();
                        if (locallyProcessedMethods.add(uniqueSignature)) {
                            MethodDescription conflictingMethod = interfaceMethods.get(uniqueSignature);
                            MethodDescription resolvedMethod = methodDescription;
                            if (conflictingMethod != null && !conflictingMethod.getDeclaringType().isAssignableFrom(typeDescription)) {
                                resolvedMethod = ConflictingInterfaceMethod.of(typeOfInterest, conflictingMethod, methodDescription);
                            }
                            interfaceMethods.put(uniqueSignature, resolvedMethod);
                        }
                        defaultMethodLookup.register(methodDescription);
                    }
                    for (TypeDescription interfaceType : typeDescription.getInterfaces()) {
                        pushInterface(interfaceType, locallyProcessedMethods, defaultMethodLookup);
                    }
                    defaultMethodLookup.complete(typeDescription);
                }
            }

            /**
             * Extracts all currently registered invokable methods from this bucket.
             *
             * @return A list of all invokable methods that were pushed into this bucket.
             */
            private MethodList extractInvokableMethods() {
                List<MethodDescription> invokableMethods = new ArrayList<MethodDescription>(classMethods.size() + interfaceMethods.size());
                invokableMethods.addAll(classMethods.values());
                invokableMethods.addAll(interfaceMethods.values());
                return new MethodList.Explicit(invokableMethods);
            }

            /**
             * Returns the type of interest.
             *
             * @return The type of interest.
             */
            private TypeDescription getTypeOfInterest() {
                return typeOfInterest;
            }

            @Override
            public String toString() {
                return "MethodBucket{" +
                        "typeOfInterest=" + typeOfInterest +
                        ", classMethods=" + classMethods +
                        ", interfaceMethods=" + interfaceMethods +
                        ", processedTypes=" + processedTypes +
                        '}';
            }

            /**
             * A strategy for looking up default methods. Any strategy implements different callback methods while
             * a type is queried for its methods. There are certain guarantees given to the strategy:
             * <ol>
             * <li>The analysis of a type is always announced by calling the
             * {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.Default.MethodBucket.DefaultMethodLookup#begin(net.bytebuddy.instrumentation.type.TypeDescription)}
             * callback. This method is invoked before any method is registered for this type.
             * </li>
             * <li>
             * Any method that is discovered for a given type is announced by the
             * {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.Default.MethodBucket.DefaultMethodLookup#register(MethodDescription)}
             * callback.
             * </li>
             * <li>
             * Once all methods are announced, the
             * {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.Default.MethodBucket.DefaultMethodLookup#complete(net.bytebuddy.instrumentation.type.TypeDescription)}
             * is invoked. This callback is not invoked before all super interfaces of the registered interface was
             * fully processed by all these three phases.
             * </li>
             * </ol>
             */
            private static interface DefaultMethodLookup {

                /**
                 * Announces the begin of the analysis of a given type.
                 *
                 * @param typeDescription The type to analyze.
                 */
                void begin(TypeDescription typeDescription);

                /**
                 * Announces the begin of the analysis of a given type.
                 *
                 * @param methodDescription Announces a new method to be discovered for the last announced type.
                 */
                void register(MethodDescription methodDescription);

                /**
                 * Announces that a type was fully analyzed, including all of its super interfaces.
                 *
                 * @param typeDescription The type which was fully processed.
                 */
                void complete(TypeDescription typeDescription);

                /**
                 * A non-operative implementation of a
                 * {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.Default.MethodBucket.DefaultMethodLookup}
                 * that does not extract any default interfaces.
                 */
                static enum Disabled implements MethodBucket.DefaultMethodLookup {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public void begin(TypeDescription typeDescription) {

                    }

                    @Override
                    public void register(MethodDescription methodDescription) {

                    }

                    @Override
                    public void complete(TypeDescription typeDescription) {

                    }
                }

                /**
                 * A canonical implementation of an enabled
                 * {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.Default.MethodBucket.DefaultMethodLookup}.
                 */
                static class Enabled implements MethodBucket.DefaultMethodLookup {

                    /**
                     * The declared interfaces of an instrumented type that are to be extracted by this default
                     * method lookup.
                     */
                    private final Collection<? extends TypeDescription> declaredInterfaceTypes;

                    /**
                     * A mapping of interfaces to the default method that can be invoked on the given interface.
                     */
                    private final Map<TypeDescription, Set<MethodDescription>> defaultMethods;

                    /**
                     * A mapping of interfaces to all methods that are declared on a given interface where
                     * the methods are not necessarily default methods.
                     */
                    private final Map<TypeDescription, Set<String>> methodDeclarations;

                    /**
                     * Creates a new mutable canonical implementation of a default method lookup.
                     *
                     * @param declaredInterfaceTypes The interfaces that were declared by a type and that
                     *                               should finally be extracted by this default method lookup.
                     */
                    protected Enabled(Collection<? extends TypeDescription> declaredInterfaceTypes) {
                        this.declaredInterfaceTypes = declaredInterfaceTypes;
                        defaultMethods = new HashMap<TypeDescription, Set<MethodDescription>>();
                        methodDeclarations = new HashMap<TypeDescription, Set<String>>();
                    }

                    @Override
                    public void begin(TypeDescription typeDescription) {
                        defaultMethods.put(typeDescription, new HashSet<MethodDescription>());
                        methodDeclarations.put(typeDescription, new HashSet<String>());
                    }

                    @Override
                    public void register(MethodDescription methodDescription) {
                        methodDeclarations.get(methodDescription.getDeclaringType()).add(methodDescription.getUniqueSignature());
                        if (methodDescription.isDefaultMethod()) {
                            defaultMethods.get(methodDescription.getDeclaringType()).add(methodDescription);
                        }
                    }

                    @Override
                    public void complete(TypeDescription typeDescription) {
                        Set<String> methodDeclarations = this.methodDeclarations.get(typeDescription);
                        Set<MethodDescription> defaultMethods = this.defaultMethods.get(typeDescription);
                        for (TypeDescription interfaceType : typeDescription.getInterfaces()) {
                            for (MethodDescription methodDescription : this.defaultMethods.get(interfaceType)) {
                                if (!methodDeclarations.contains(methodDescription.getUniqueSignature())) {
                                    defaultMethods.add(methodDescription);
                                }
                            }
                        }
                    }

                    /**
                     * Returns a map of all default method interfaces that were extracted by this default method lookup.
                     * Once this method is called, this instance must not longer be used.
                     *
                     * @return A map of all default method interfaces pointing to all default methods that are invokable
                     * on the corresponding default method interface.
                     */
                    protected Map<TypeDescription, Set<MethodDescription>> materialize() {
                        defaultMethods.keySet().retainAll(declaredInterfaceTypes);
                        return Collections.unmodifiableMap(defaultMethods);
                    }
                }
            }
        }
    }
}
