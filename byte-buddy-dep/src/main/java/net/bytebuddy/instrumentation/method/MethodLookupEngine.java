package net.bytebuddy.instrumentation.method;

import jdk.internal.org.objectweb.asm.Opcodes;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;

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

    MethodList getReachableMethods(TypeDescription typeDescription);

    static interface Factory {

        MethodLookupEngine make();
    }

    static class ConflictingInterfaceMethod extends MethodDescription.AbstractMethodDescription {

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

        private final TypeDescription virtualHost;
        private final List<MethodDescription> methodDescriptions;

        private ConflictingInterfaceMethod(TypeDescription virtualHost, List<MethodDescription> methodDescriptions) {
            this.virtualHost = virtualHost;
            this.methodDescriptions = methodDescriptions;
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
            return Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC;
        }

        @Override
        public boolean isInvokableOn(TypeDescription typeDescription) {
            for (MethodDescription methodDescription : methodDescriptions) {
                if (super.isInvokableOn(methodDescription.getDeclaringType())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "MethodLookupEngine.ConflictingInterfaceMethod{" +
                    "virtualHost=" + virtualHost +
                    ", methodDescriptions=" + methodDescriptions +
                    '}';
        }
    }

    static abstract class AbstractCachingBase implements MethodLookupEngine {

        protected final Map<TypeDescription, MethodList> reachableMethods;

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

    static class Default extends AbstractCachingBase {

        public static enum Factory implements MethodLookupEngine.Factory {

            INSTANCE;

            @Override
            public MethodLookupEngine make() {
                return new Default();
            }
        }

        @Override
        protected MethodList doGetReachableMethods(TypeDescription typeDescription) {
            MethodBucket methodBucket = new MethodBucket(typeDescription);
            Set<TypeDescription> interfaces = new HashSet<TypeDescription>(typeDescription.getInterfaces());
            while ((typeDescription = typeDescription.getSupertype()) != null) {
                methodBucket.pushClass(typeDescription);
                interfaces.addAll(typeDescription.getInterfaces());
            }
            methodBucket.pushInterfaces(interfaces);
            return methodBucket.toMethodList();
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
             * A marker pool of interfaces that were already processed by this bucket.
             */
            private final Set<TypeDescription> processedInterfaces;

            private final TypeDescription virtualHost;

            private final MethodMatcher superTypeMatcher;

            /**
             * Creates a new mutable method bucket.
             */
            private MethodBucket(TypeDescription hostingType) {
                this.virtualHost = hostingType;
                classMethods = new HashMap<String, MethodDescription>();
                interfaceMethods = new HashMap<String, MethodDescription>();
                processedInterfaces = new HashSet<TypeDescription>();
                superTypeMatcher = isMethod().and(not(isPrivate().or(isStatic()).or(isPackagePrivate().and(not(isVisibleTo(hostingType))))));
                pushClass(hostingType, any());
            }

            private void pushClass(TypeDescription typeDescription) {
                pushClass(typeDescription, superTypeMatcher);
            }

            /**
             * Pushes all methods of a given class into the bucket. Classes must be pushed before pushing any interfaces
             * because Java considers class methods as dominant over interface methods. Furthermore, more specific
             * classes must be pushed first in order to respect method overrides.
             *
             * @param typeDescription The (non-interface) class to push into the bucket.
             */
            private void pushClass(TypeDescription typeDescription, MethodMatcher methodMatcher) {
                for (MethodDescription methodDescription : typeDescription.getDeclaredMethods().filter(methodMatcher)) {
                    String uniqueSignature = methodDescription.getUniqueSignature();
                    if (!classMethods.containsKey(uniqueSignature)) {
                        classMethods.put(uniqueSignature, methodDescription);
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
                Set<String> processedMethods = new HashSet<String>(classMethods.keySet());
                for (TypeDescription interfaceTypeDescription : typeDescriptions) {
                    pushInterface(interfaceTypeDescription, processedMethods);
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
             * {@link net.bytebuddy.instrumentation.method.MethodLookupEngine.Default.MethodBucket#processedInterfaces}.
             *
             * @param typeDescription  The interface type to process.
             * @param processedMethods A set of unique method signatures that were already processed.
             */
            private void pushInterface(TypeDescription typeDescription, Set<String> processedMethods) {
                Set<String> locallyProcessedMethods = new HashSet<String>(processedMethods);
                if (processedInterfaces.add(typeDescription)) {
                    for (MethodDescription methodDescription : typeDescription.getDeclaredMethods().filter(superTypeMatcher)) {
                        String uniqueSignature = methodDescription.getUniqueSignature();
                        if (!processedMethods.contains(uniqueSignature)) {
                            locallyProcessedMethods.add(uniqueSignature);
                            MethodDescription conflictingMethod = interfaceMethods.get(uniqueSignature);
                            if (conflictingMethod != null && !conflictingMethod.getDeclaringType().isAssignableFrom(typeDescription)) {
                                methodDescription = ConflictingInterfaceMethod.of(virtualHost, conflictingMethod, methodDescription);
                            }
                            interfaceMethods.put(uniqueSignature, methodDescription);
                        }
                    }
                    for (TypeDescription interfaceType : typeDescription.getInterfaces()) {
                        pushInterface(interfaceType, locallyProcessedMethods);
                    }
                }
            }

            private MethodList toMethodList() {
                List<MethodDescription> methodDescriptions = new ArrayList<MethodDescription>(classMethods.size() + interfaceMethods.size());
                methodDescriptions.addAll(classMethods.values());
                methodDescriptions.addAll(interfaceMethods.values());
                return new MethodList.Explicit(methodDescriptions);
            }
        }
    }
}
