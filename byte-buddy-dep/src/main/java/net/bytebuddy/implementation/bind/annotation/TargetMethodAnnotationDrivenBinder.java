package net.bytebuddy.implementation.bind.annotation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.*;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.JavaType;

import java.lang.annotation.Annotation;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.isGetter;
import static net.bytebuddy.matcher.ElementMatchers.isSetter;

/**
 * This {@link net.bytebuddy.implementation.bind.MethodDelegationBinder} binds
 * method by analyzing annotations found on the <i>target</i> method that is subject to a method binding.
 */
public class TargetMethodAnnotationDrivenBinder implements MethodDelegationBinder {

    /**
     * The processor for performing an actual method delegation.
     */
    private final DelegationProcessor delegationProcessor;

    /**
     * The provider for annotations to be supplied for binding of non-annotated parameters.
     */
    private final DefaultsProvider defaultsProvider;

    /**
     * The termination handler to be applied.
     */
    private final TerminationHandler terminationHandler;

    /**
     * An user-supplied assigner to use for variable assignments.
     */
    private final Assigner assigner;

    /**
     * A delegate for actually invoking a method.
     */
    private final MethodInvoker methodInvoker;

    /**
     * Creates a new method delegation binder that binds method based on annotations found on the target method.
     *
     * @param parameterBinders   A list of parameter binder delegates. Each such delegate is responsible for creating a
     *                           {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.ParameterBinding}
     *                           for a specific annotation.
     * @param defaultsProvider   A provider that creates an annotation for parameters that are not annotated by any annotation
     *                           that is handled by any of the registered {@code parameterBinders}.
     * @param terminationHandler The termination handler to be applied.
     * @param assigner           An assigner that is supplied to the {@code parameterBinders} and that is used for binding the return value.
     * @param methodInvoker      A delegate for applying the actual method invocation of the target method.
     */
    public TargetMethodAnnotationDrivenBinder(List<ParameterBinder<?>> parameterBinders,
                                              DefaultsProvider defaultsProvider,
                                              TerminationHandler terminationHandler,
                                              Assigner assigner,
                                              MethodInvoker methodInvoker) {
        delegationProcessor = DelegationProcessor.of(parameterBinders);
        this.defaultsProvider = defaultsProvider;
        this.terminationHandler = terminationHandler;
        this.assigner = assigner;
        this.methodInvoker = methodInvoker;
    }

    @Override
    public MethodBinding bind(Implementation.Target implementationTarget,
                              MethodDescription source,
                              MethodDescription target) {
        if (IgnoreForBinding.Verifier.check(target)) {
            return MethodBinding.Illegal.INSTANCE;
        }
        StackManipulation methodTermination = terminationHandler.resolve(assigner, source, target);
        if (!methodTermination.isValid()) {
            return MethodBinding.Illegal.INSTANCE;
        }
        MethodBinding.Builder methodDelegationBindingBuilder = new MethodBinding.Builder(methodInvoker, target);
        Iterator<AnnotationDescription> defaults = defaultsProvider.makeIterator(implementationTarget, source, target);
        for (ParameterDescription parameterDescription : target.getParameters()) {
            ParameterBinding<?> parameterBinding = delegationProcessor
                    .handler(parameterDescription.getDeclaredAnnotations(), defaults)
                    .bind(source,
                            parameterDescription,
                            implementationTarget,
                            assigner);
            if (!parameterBinding.isValid() || !methodDelegationBindingBuilder.append(parameterBinding)) {
                return MethodBinding.Illegal.INSTANCE;
            }
        }
        return methodDelegationBindingBuilder.build(methodTermination);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        TargetMethodAnnotationDrivenBinder that = (TargetMethodAnnotationDrivenBinder) other;
        return assigner.equals(that.assigner)
                && defaultsProvider.equals(that.defaultsProvider)
                && terminationHandler.equals(that.terminationHandler)
                && delegationProcessor.equals(that.delegationProcessor)
                && methodInvoker.equals(that.methodInvoker);
    }

    @Override
    public int hashCode() {
        int result = delegationProcessor.hashCode();
        result = 31 * result + defaultsProvider.hashCode();
        result = 31 * result + terminationHandler.hashCode();
        result = 31 * result + assigner.hashCode();
        result = 31 * result + methodInvoker.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TargetMethodAnnotationDrivenBinder{" +
                "delegationProcessor=" + delegationProcessor +
                ", defaultsProvider=" + defaultsProvider +
                ", terminationHandler=" + terminationHandler +
                ", assigner=" + assigner +
                ", methodInvoker=" + methodInvoker +
                '}';
    }

    /**
     * A parameter binder is used as a delegate for binding a parameter according to a particular annotation type found
     * on this parameter.
     *
     * @param <T> The {@link java.lang.annotation.Annotation#annotationType()} handled by this parameter binder.
     */
    @SuppressFBWarnings(value = "IC_SUPERCLASS_USES_SUBCLASS_DURING_INITIALIZATION", justification = "Safe initialization is implied")
    public interface ParameterBinder<T extends Annotation> {

        /**
         * The default parameter binders to be used.
         */
        List<ParameterBinder<?>> DEFAULTS = Collections.unmodifiableList(Arrays.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>asList(
                Argument.Binder.INSTANCE,
                AllArguments.Binder.INSTANCE,
                Origin.Binder.INSTANCE,
                This.Binder.INSTANCE,
                Super.Binder.INSTANCE,
                Default.Binder.INSTANCE,
                SuperCall.Binder.INSTANCE,
                DefaultCall.Binder.INSTANCE,
                FieldValue.Binder.INSTANCE,
                StubValue.Binder.INSTANCE,
                Empty.Binder.INSTANCE));

        /**
         * The annotation type that is handled by this parameter binder.
         *
         * @return The {@link java.lang.annotation.Annotation#annotationType()} handled by this parameter binder.
         */
        Class<T> getHandledType();

        /**
         * Creates a parameter binding for the given target parameter.
         *
         * @param annotation           The annotation that was cause for the delegation to this argument binder.
         * @param source               The intercepted source method.
         * @param target               Tge target parameter that is subject to be bound to
         *                             intercepting the {@code source} method.
         * @param implementationTarget The target of the current implementation that is subject to this binding.
         * @param assigner             An assigner that can be used for applying the binding.
         * @return A parameter binding for the requested target method parameter.
         */
        ParameterBinding<?> bind(AnnotationDescription.Loadable<T> annotation,
                                 MethodDescription source,
                                 ParameterDescription target,
                                 Implementation.Target implementationTarget,
                                 Assigner assigner);

        /**
         * <p>
         * Implements a parameter binder that binds a fixed value to a parameter with a given annotation.
         * </p>
         * <p>
         * This binder is only capable to store values that can either be expressed as Java byte code or as a constant pool value. This
         * includes primitive types, {@link String} values, {@link Class} values which can also be expressed as {@link TypeDescription}
         * instances or method handles and method types for classes of a version at least of Java 7. The latter instances can also be
         * expressed as unloaded {@link JavaConstant} representations.
         * </p>
         * <p>
         * <b>Important</b>: When supplying a method handle or a method type, all types that are implied must be visible to the instrumented
         * type or an {@link IllegalAccessException} will be thrown at runtime.
         * </p>
         *
         * @param <S> The bound annotation's type.
         */
        abstract class ForFixedValue<S extends Annotation> implements ParameterBinder<S> {

            @Override
            public ParameterBinding<?> bind(AnnotationDescription.Loadable<S> annotation,
                                            MethodDescription source,
                                            ParameterDescription target,
                                            Implementation.Target implementationTarget,
                                            Assigner assigner) {
                Object value = bind(annotation, source, target);
                if (value == null) {
                    return new ParameterBinding.Anonymous(DefaultValue.of(target.getType()));
                }
                StackManipulation stackManipulation;
                TypeDescription suppliedType;
                if (value instanceof Boolean) {
                    stackManipulation = IntegerConstant.forValue((Boolean) value);
                    suppliedType = new TypeDescription.ForLoadedType(boolean.class);
                } else if (value instanceof Byte) {
                    stackManipulation = IntegerConstant.forValue((Byte) value);
                    suppliedType = new TypeDescription.ForLoadedType(byte.class);
                } else if (value instanceof Short) {
                    stackManipulation = IntegerConstant.forValue((Short) value);
                    suppliedType = new TypeDescription.ForLoadedType(short.class);
                } else if (value instanceof Character) {
                    stackManipulation = IntegerConstant.forValue((Character) value);
                    suppliedType = new TypeDescription.ForLoadedType(char.class);
                } else if (value instanceof Integer) {
                    stackManipulation = IntegerConstant.forValue((Integer) value);
                    suppliedType = new TypeDescription.ForLoadedType(int.class);
                } else if (value instanceof Long) {
                    stackManipulation = LongConstant.forValue((Long) value);
                    suppliedType = new TypeDescription.ForLoadedType(long.class);
                } else if (value instanceof Float) {
                    stackManipulation = FloatConstant.forValue((Float) value);
                    suppliedType = new TypeDescription.ForLoadedType(float.class);
                } else if (value instanceof Double) {
                    stackManipulation = DoubleConstant.forValue((Double) value);
                    suppliedType = new TypeDescription.ForLoadedType(double.class);
                } else if (value instanceof String) {
                    stackManipulation = new TextConstant((String) value);
                    suppliedType = TypeDescription.STRING;
                } else if (value instanceof Class) {
                    stackManipulation = ClassConstant.of(new TypeDescription.ForLoadedType((Class<?>) value));
                    suppliedType = TypeDescription.CLASS;
                } else if (value instanceof TypeDescription) {
                    stackManipulation = ClassConstant.of((TypeDescription) value);
                    suppliedType = TypeDescription.CLASS;
                } else if (JavaType.METHOD_HANDLE.getTypeStub().isInstance(value)) {
                    stackManipulation = JavaConstant.MethodHandle.ofLoaded(value).asStackManipulation();
                    suppliedType = JavaType.METHOD_HANDLE.getTypeStub();
                } else if (value instanceof JavaConstant.MethodHandle) {
                    stackManipulation = new JavaConstantValue((JavaConstant.MethodHandle) value);
                    suppliedType = JavaType.METHOD_HANDLE.getTypeStub();
                } else if (JavaType.METHOD_TYPE.getTypeStub().isInstance(value)) {
                    stackManipulation = new JavaConstantValue(JavaConstant.MethodType.ofLoaded(value));
                    suppliedType = JavaType.METHOD_HANDLE.getTypeStub();
                } else if (value instanceof JavaConstant.MethodType) {
                    stackManipulation = new JavaConstantValue((JavaConstant.MethodType) value);
                    suppliedType = JavaType.METHOD_HANDLE.getTypeStub();
                } else {
                    throw new IllegalStateException("Not able to save in class's constant pool: " + value);
                }
                return new ParameterBinding.Anonymous(new StackManipulation.Compound(
                        stackManipulation,
                        assigner.assign(suppliedType.asGenericType(), target.getType(), Assigner.Typing.STATIC)
                ));
            }

            /**
             * Resolves a value for the given annotation on a parameter that is processed by a {@link net.bytebuddy.implementation.MethodDelegation}.
             *
             * @param annotation The annotation that triggered this binding.
             * @param source     The method for which a delegation is currently bound.
             * @param target     The parameter for which a value is bound.
             * @return The constant pool value that is bound to this parameter or {@code null} for binding this value.
             */
            protected abstract Object bind(AnnotationDescription.Loadable<S> annotation, MethodDescription source, ParameterDescription target);

            /**
             * <p>
             * A parameter binder that binds a fixed value to a parameter annotation when using a {@link net.bytebuddy.implementation.MethodDelegation}.
             * </p>
             * <p>
             * This binder is only capable to store
             * values that can either be expressed as Java byte code or as a constant pool value. This includes primitive types, {@link String} values,
             * {@link Class} values which can also be expressed as {@link TypeDescription} instances or method handles and method types for classes of
             * a version at least of Java 7. The latter instances can also be expressed as unloaded {@link JavaConstant} representations.
             * </p>
             *
             * @param <U> The bound annotation's type.
             */
            public static class OfConstant<U extends Annotation> extends ForFixedValue<U> {

                /**
                 * The type of the annotation that is bound by this binder.
                 */
                private final Class<U> type;

                /**
                 * The value that is assigned to any annotated parameter.
                 */
                private final Object value;

                /**
                 * Creates a binder for binding a fixed value to a parameter annotated with the given annotation.
                 *
                 * @param type  The type of the annotation that is bound by this binder.
                 * @param value The value that is assigned to any annotated parameter.
                 */
                protected OfConstant(Class<U> type, Object value) {
                    this.type = type;
                    this.value = value;
                }

                /**
                 * Creates a binder for binding a fixed value to a given annotation.
                 *
                 * @param type  The type of the annotation that is bound by this binder.
                 * @param value The value that is assigned to any annotated parameter.
                 * @param <V>   The bound annotation's type.
                 * @return A parameter binder that binds the given annotation to the supplied value.
                 */
                public static <V extends Annotation> ParameterBinder<V> of(Class<V> type, Object value) {
                    return new OfConstant<V>(type, value);
                }

                @Override
                public Class<U> getHandledType() {
                    return type;
                }

                @Override
                protected Object bind(AnnotationDescription.Loadable<U> annotation, MethodDescription source, ParameterDescription target) {
                    return value;
                }


                @Override
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    OfConstant<?> that = (OfConstant<?>) object;
                    return type.equals(that.type) && (value != null ? value.equals(that.value) : that.value == null);
                }

                @Override
                public int hashCode() {
                    int result = type.hashCode();
                    result = 31 * result + (value != null ? value.hashCode() : 0);
                    return result;
                }

                @Override
                public String toString() {
                    return "TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFixedValue.OfConstant{" +
                            "type=" + type +
                            ", value=" + value +
                            '}';
                }
            }
        }

        /**
         * A parameter binder that binds a field's value.
         *
         * @param <S> The {@link java.lang.annotation.Annotation#annotationType()} handled by this parameter binder.
         */
        abstract class ForFieldBinding<S extends Annotation> implements ParameterBinder<S> {

            /**
             * Indicates that a name should be extracted from an accessor method.
             */
            protected static final String BEAN_PROPERTY = "";

            /**
             * Resolves a field locator for a potential accessor method.
             *
             * @param fieldLocator      The field locator to use.
             * @param methodDescription The method description that is the potential accessor.
             * @return A resolution for a field locator.
             */
            private static FieldLocator.Resolution resolveAccessor(FieldLocator fieldLocator, MethodDescription methodDescription) {
                String fieldName;
                if (isSetter().matches(methodDescription)) {
                    fieldName = methodDescription.getInternalName().substring(3);
                } else if (isGetter().matches(methodDescription)) {
                    fieldName = methodDescription.getInternalName().substring(methodDescription.getInternalName().startsWith("is") ? 2 : 3);
                } else {
                    return FieldLocator.Resolution.Illegal.INSTANCE;
                }
                return fieldLocator.locate(Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1));
            }

            @Override
            public ParameterBinding<?> bind(AnnotationDescription.Loadable<S> annotation,
                                            MethodDescription source,
                                            ParameterDescription target,
                                            Implementation.Target implementationTarget,
                                            Assigner assigner) {
                if (!declaringType(annotation).represents(void.class)) {
                    if (declaringType(annotation).isPrimitive() || declaringType(annotation).isArray()) {
                        throw new IllegalStateException("A primitive type or array type cannot declare a field: " + source);
                    } else if (!implementationTarget.getInstrumentedType().isAssignableTo(declaringType(annotation))) {
                        return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
                    }
                }
                FieldLocator fieldLocator = declaringType(annotation).represents(void.class)
                        ? new FieldLocator.ForClassHierarchy(implementationTarget.getInstrumentedType())
                        : new FieldLocator.ForExactType(declaringType(annotation), implementationTarget.getInstrumentedType());
                FieldLocator.Resolution resolution = fieldName(annotation).equals(BEAN_PROPERTY)
                        ? resolveAccessor(fieldLocator, source)
                        : fieldLocator.locate(fieldName(annotation));
                return resolution.isResolved() && !(source.isStatic() && !resolution.getField().isStatic())
                        ? bind(resolution.getField(), annotation, source, target, implementationTarget, assigner)
                        : ParameterBinding.Illegal.INSTANCE;
            }

            /**
             * Extracts the field name from an annotation.
             *
             * @param annotation The annotation from which to extract the field name.
             * @return The field name defined by the handled annotation.
             */
            protected abstract String fieldName(AnnotationDescription.Loadable<S> annotation);

            /**
             * Extracts the declaring type from an annotation.
             *
             * @param annotation The annotation from which to extract the declaring type.
             * @return The declaring type defined by the handled annotation.
             */
            protected abstract TypeDescription declaringType(AnnotationDescription.Loadable<S> annotation);

            /**
             * Creates a parameter binding for the given target parameter.
             *
             * @param fieldDescription     The field for which this binder binds a value.
             * @param annotation           The annotation that was cause for the delegation to this argument binder.
             * @param source               The intercepted source method.
             * @param target               Tge target parameter that is subject to be bound to
             *                             intercepting the {@code source} method.
             * @param implementationTarget The target of the current implementation that is subject to this binding.
             * @param assigner             An assigner that can be used for applying the binding.
             * @return A parameter binding for the requested target method parameter.
             */
            protected abstract ParameterBinding<?> bind(FieldDescription fieldDescription,
                                                        AnnotationDescription.Loadable<S> annotation,
                                                        MethodDescription source,
                                                        ParameterDescription target,
                                                        Implementation.Target implementationTarget,
                                                        Assigner assigner);
        }
    }

    /**
     * Implementations of the defaults provider interface create annotations for parameters that are not annotated with
     * a known annotation.
     *
     * @see net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder
     */
    public interface DefaultsProvider {

        /**
         * Creates an iterator from which a value is pulled each time no processable annotation is found on a
         * method parameter.
         *
         * @param implementationTarget The target of the current implementation.
         * @param source               The source method that is bound to the {@code target} method.
         * @param target               Tge target method that is subject to be bound by the {@code source} method.
         * @return An iterator that supplies default annotations for
         */
        Iterator<AnnotationDescription> makeIterator(Implementation.Target implementationTarget,
                                                     MethodDescription source,
                                                     MethodDescription target);

        /**
         * A defaults provider that does not supply any defaults. If this defaults provider is used, a target
         * method is required to annotate each parameter with a known annotation.
         */
        enum Empty implements DefaultsProvider {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Iterator<AnnotationDescription> makeIterator(Implementation.Target implementationTarget,
                                                                MethodDescription source,
                                                                MethodDescription target) {
                return EmptyIterator.INSTANCE;
            }

            @Override
            public String toString() {
                return "TargetMethodAnnotationDrivenBinder.DefaultsProvider.Empty." + name();
            }

            /**
             * A trivial iterator without any elements.
             */
            protected enum EmptyIterator implements Iterator<AnnotationDescription> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public AnnotationDescription next() {
                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    throw new NoSuchElementException();
                }

                @Override
                public String toString() {
                    return "TargetMethodAnnotationDrivenBinder.DefaultsProvider.Empty.EmptyIterator." + name();
                }
            }
        }
    }

    /**
     * Responsible for creating a {@link StackManipulation}
     * that is applied after the interception method is applied.
     */
    public enum TerminationHandler {

        /**
         * A termination handler that returns the delegate method's return value.
         */
        RETURNING {
            @Override
            protected StackManipulation resolve(Assigner assigner, MethodDescription source, MethodDescription target) {
                return new StackManipulation.Compound(assigner.assign(target.isConstructor()
                                ? target.getDeclaringType().asGenericType()
                                : target.getReturnType(),
                        source.getReturnType(),
                        RuntimeType.Verifier.check(target)), MethodReturn.of(source.getReturnType().asErasure()));
            }
        },

        /**
         * A termination handler that drops the delegate method's return value,
         */
        DROPPING {
            @Override
            protected StackManipulation resolve(Assigner assigner, MethodDescription source, MethodDescription target) {
                return Removal.pop(target.isConstructor()
                        ? target.getDeclaringType().asErasure()
                        : target.getReturnType().asErasure());
            }
        };

        /**
         * Creates a stack manipulation that is to be applied after the method return.
         *
         * @param assigner The supplied assigner.
         * @param source   The source method that is bound to the {@code target} method.
         * @param target   The target method that is subject to be bound by the {@code source} method.
         * @return A stack manipulation that is applied after the method return.
         */
        protected abstract StackManipulation resolve(Assigner assigner, MethodDescription source, MethodDescription target);

        @Override
        public String toString() {
            return "TargetMethodAnnotationDrivenBinder.TerminationHandler." + name();
        }
    }

    /**
     * A delegation processor is a helper class for a
     * {@link net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder}
     * for performing its actual logic. By outsourcing this logic to this helper class, a cleaner implementation
     * can be provided.
     */
    protected static class DelegationProcessor {

        /**
         * A map of registered annotation types to the binder that is responsible for binding a parameter
         * that is annotated with the given annotation.
         */
        private final Map<TypeDescription, ParameterBinder<?>> parameterBinders;

        /**
         * Creates a new delegation processor.
         *
         * @param parameterBinders A mapping of parameter binders by their handling type.
         */
        protected DelegationProcessor(Map<TypeDescription, ParameterBinder<?>> parameterBinders) {
            this.parameterBinders = parameterBinders;
        }

        /**
         * Creates a new delegation processor.
         *
         * @param parameterBinders A list of parameter binder delegates. Each such delegate is responsible for creating
         *                         a {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.ParameterBinding}
         *                         for a specific annotation.
         * @return A corresponding delegation processor.
         */
        protected static DelegationProcessor of(List<ParameterBinder<?>> parameterBinders) {
            Map<TypeDescription, ParameterBinder<?>> parameterBinderMap = new HashMap<TypeDescription, ParameterBinder<?>>();
            for (ParameterBinder<?> parameterBinder : parameterBinders) {
                if (parameterBinderMap.put(new TypeDescription.ForLoadedType(parameterBinder.getHandledType()), parameterBinder) != null) {
                    throw new IllegalArgumentException("Attempt to bind two handlers to " + parameterBinder.getHandledType());
                }
            }
            return new DelegationProcessor(parameterBinderMap);
        }

        /**
         * Locates a handler which is responsible for processing the given parameter. If no explicit handler can
         * be located, a fallback handler is provided.
         *
         * @param annotations The annotations of the parameter for which a handler should be provided.
         * @param defaults    The defaults provider to be queried if no explicit handler mapping could be found.
         * @return A handler for processing the parameter with the given annotations.
         */
        private Handler handler(List<AnnotationDescription> annotations, Iterator<AnnotationDescription> defaults) {
            Handler handler = null;
            for (AnnotationDescription annotation : annotations) {
                ParameterBinder<?> parameterBinder = parameterBinders.get(annotation.getAnnotationType());
                if (parameterBinder != null && handler != null) {
                    throw new IllegalStateException("Ambiguous binding for parameter annotated with two handled annotation types");
                } else if (parameterBinder != null /* && handler == null */) {
                    handler = makeHandler(parameterBinder, annotation);
                }
            }
            if (handler == null) { // No handler was found: attempt using defaults provider.
                if (defaults.hasNext()) {
                    AnnotationDescription defaultAnnotation = defaults.next();
                    ParameterBinder<?> parameterBinder = parameterBinders.get(defaultAnnotation.getAnnotationType());
                    if (parameterBinder == null) {
                        return Handler.Unbound.INSTANCE;
                    } else {
                        handler = makeHandler(parameterBinder, defaultAnnotation);
                    }
                } else {
                    return Handler.Unbound.INSTANCE;
                }
            }
            return handler;
        }

        /**
         * Creates a handler for a given annotation.
         *
         * @param parameterBinder The parameter binder that should process an annotation.
         * @param annotation      An annotation instance that can be understood by this parameter binder.
         * @return A handler for processing the given annotation.
         */
        @SuppressWarnings("unchecked")
        private Handler makeHandler(ParameterBinder<?> parameterBinder, AnnotationDescription annotation) {
            return new Handler.Bound<Annotation>((ParameterBinder<Annotation>) parameterBinder,
                    (AnnotationDescription.Loadable<Annotation>) annotation.prepare(parameterBinder.getHandledType()));
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && parameterBinders.equals(((DelegationProcessor) other).parameterBinders);
        }

        @Override
        public int hashCode() {
            return parameterBinders.hashCode();
        }

        @Override
        public String toString() {
            return "TargetMethodAnnotationDrivenBinder.DelegationProcessor{" +
                    "parameterBinders=" + parameterBinders +
                    '}';
        }

        /**
         * A handler is responsible for processing a parameter's binding.
         */
        protected interface Handler {

            /**
             * Handles a parameter binding.
             *
             * @param source               The intercepted source method.
             * @param target               The target parameter that is subject to binding.
             * @param implementationTarget The target of the current implementation.
             * @param assigner             An assigner that can be used for applying the binding.
             * @return A parameter binding that reflects the given arguments.
             */
            ParameterBinding<?> bind(MethodDescription source,
                                     ParameterDescription target,
                                     Implementation.Target implementationTarget,
                                     Assigner assigner);

            /**
             * An unbound handler is a fallback for returning an illegal binding for parameters for which no parameter
             * binder could be located.
             */
            enum Unbound implements Handler {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public ParameterBinding<?> bind(MethodDescription source,
                                                ParameterDescription target,
                                                Implementation.Target implementationTarget,
                                                Assigner assigner) {
                    return ParameterBinding.Illegal.INSTANCE;
                }

                @Override
                public String toString() {
                    return "TargetMethodAnnotationDrivenBinder.DelegationProcessor.Handler.Unbound." + name();
                }
            }

            /**
             * A bound handler represents an unambiguous parameter binder that was located for a given array of
             * annotations.
             *
             * @param <T> The annotation type of a given handler.
             */
            class Bound<T extends Annotation> implements Handler {

                /**
                 * The parameter binder that is actually responsible for binding the parameter.
                 */
                private final ParameterBinder<T> parameterBinder;

                /**
                 * The annotation value that lead to the binding of this handler.
                 */
                private final AnnotationDescription.Loadable<T> annotation;

                /**
                 * Creates a new bound handler.
                 *
                 * @param parameterBinder The parameter binder that is actually responsible for binding the parameter.
                 * @param annotation      The annotation value that lead to the binding of this handler.
                 */
                public Bound(ParameterBinder<T> parameterBinder, AnnotationDescription.Loadable<T> annotation) {
                    this.parameterBinder = parameterBinder;
                    this.annotation = annotation;
                }

                @Override
                public ParameterBinding<?> bind(MethodDescription source,
                                                ParameterDescription target,
                                                Implementation.Target implementationTarget,
                                                Assigner assigner) {
                    return parameterBinder.bind(annotation,
                            source,
                            target,
                            implementationTarget,
                            assigner);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && parameterBinder.equals(((Bound<?>) other).parameterBinder)
                            && annotation.equals(((Bound<?>) other).annotation);
                }

                @Override
                public int hashCode() {
                    int result = parameterBinder.hashCode();
                    result = 31 * result + annotation.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "TargetMethodAnnotationDrivenBinder.DelegationProcessor.Handler.Bound{" +
                            "parameterBinder=" + parameterBinder +
                            ", annotation=" + annotation +
                            '}';
                }
            }
        }
    }
}
