package net.bytebuddy.implementation;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.collection.ArrayFactory;
import net.bytebuddy.implementation.bytecode.constant.MethodConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.genericFieldType;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * An adapter for adapting an {@link java.lang.reflect.InvocationHandler}. The adapter allows the invocation handler
 * to also intercept method calls to non-interface methods.
 */
@HashCodeAndEqualsPlugin.Enhance
public abstract class InvocationHandlerAdapter implements Implementation {

    /**
     * A type description of the {@link InvocationHandler}.
     */
    private static final TypeDescription.Generic INVOCATION_HANDLER_TYPE = TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(InvocationHandler.class);

    /**
     * Indicates that a value should not be cached.
     */
    private static final boolean UNCACHED = false;

    /**
     * Indicates that a {@link java.lang.reflect.Method} should be cached.
     */
    private static final boolean CACHED = true;

    /**
     * Indicates that a lookup of a method constant should not be looked up using an {@link java.security.AccessController}.
     */
    private static final boolean UNPRIVILEGED = false;

    /**
     * Indicates that a lookup of a method constant should be looked up using an {@link java.security.AccessController}.
     */
    private static final boolean PRIVILEGED = true;

    /**
     * The name of the field for storing an invocation handler.
     */
    protected final String fieldName;

    /**
     * The assigner that is used for assigning the return invocation handler's return value to the
     * intercepted method's return value.
     */
    protected final Assigner assigner;

    /**
     * Determines if the {@link java.lang.reflect.Method} instances that are handed to the intercepted methods are
     * cached in {@code static} fields.
     */
    protected final boolean cached;

    /**
     * Determines if the {@link java.lang.reflect.Method} instances are retrieved by using an {@link java.security.AccessController}.
     */
    protected final boolean privileged;

    /**
     * Creates a new invocation handler for a given field.
     *
     * @param fieldName  The name of the field.
     * @param cached     Determines if the {@link java.lang.reflect.Method} instances that are handed to the
     *                   intercepted methods are cached in {@code static} fields.
     * @param privileged Determines if the {@link java.lang.reflect.Method} instances are retrieved by using an {@link java.security.AccessController}.
     * @param assigner   The assigner to apply when defining this implementation.
     */
    protected InvocationHandlerAdapter(String fieldName, boolean cached, boolean privileged, Assigner assigner) {
        this.fieldName = fieldName;
        this.cached = cached;
        this.privileged = privileged;
        this.assigner = assigner;
    }

    /**
     * Creates an implementation for any instance of an {@link java.lang.reflect.InvocationHandler} that delegates
     * all method interceptions to the given instance which will be stored in a {@code static} field.
     *
     * @param invocationHandler The invocation handler to which all method calls are delegated.
     * @return An implementation that delegates all method interceptions to the given invocation handler.
     */
    public static InvocationHandlerAdapter of(InvocationHandler invocationHandler) {
        return of(invocationHandler, ForInstance.PREFIX + "$" + RandomString.hashOf(invocationHandler.hashCode()));
    }

    /**
     * Creates an implementation for any instance of an {@link java.lang.reflect.InvocationHandler} that delegates
     * all method interceptions to the given instance which will be stored in a {@code static} field.
     *
     * @param invocationHandler The invocation handler to which all method calls are delegated.
     * @param fieldName         The name of the field.
     * @return An implementation that delegates all method interceptions to the given invocation handler.
     */
    public static InvocationHandlerAdapter of(InvocationHandler invocationHandler, String fieldName) {
        return new ForInstance(fieldName, CACHED, UNPRIVILEGED, Assigner.DEFAULT, invocationHandler);
    }

    /**
     * Creates an implementation for any {@link java.lang.reflect.InvocationHandler} that delegates
     * all method interceptions to a field with the given name. This field has to be of a subtype of invocation
     * handler and needs to be set before any invocations are intercepted. Otherwise, a {@link java.lang.NullPointerException}
     * will be thrown.
     *
     * @param name The name of the field.
     * @return An implementation that delegates all method interceptions to an instance field of the given name.
     */
    public static InvocationHandlerAdapter toField(String name) {
        return toField(name, FieldLocator.ForClassHierarchy.Factory.INSTANCE);
    }

    /**
     * Creates an implementation for any {@link java.lang.reflect.InvocationHandler} that delegates
     * all method interceptions to a field with the given name. This field has to be of a subtype of invocation
     * handler and needs to be set before any invocations are intercepted. Otherwise, a {@link java.lang.NullPointerException}
     * will be thrown.
     *
     * @param name                The name of the field.
     * @param fieldLocatorFactory The field locator factory
     * @return An implementation that delegates all method interceptions to an instance field of the given name.
     */
    public static InvocationHandlerAdapter toField(String name, FieldLocator.Factory fieldLocatorFactory) {
        return new ForField(name, CACHED, UNPRIVILEGED, Assigner.DEFAULT, fieldLocatorFactory);
    }

    /**
     * Returns a list of stack manipulations that loads all arguments of an instrumented method.
     *
     * @param instrumentedMethod The method that is instrumented.
     * @return A list of stack manipulation that loads all arguments of an instrumented method.
     */
    private List<StackManipulation> argumentValuesOf(MethodDescription instrumentedMethod) {
        TypeList.Generic parameterTypes = instrumentedMethod.getParameters().asTypeList();
        List<StackManipulation> instruction = new ArrayList<StackManipulation>(parameterTypes.size());
        int currentIndex = 1;
        for (TypeDescription.Generic parameterType : parameterTypes) {
            instruction.add(new StackManipulation.Compound(
                    MethodVariableAccess.of(parameterType).loadFrom(currentIndex),
                    assigner.assign(parameterType, TypeDescription.Generic.OBJECT, Assigner.Typing.STATIC)));
            currentIndex += parameterType.getStackSize().getSize();
        }
        return instruction;
    }

    /**
     * By default, any {@link java.lang.reflect.Method} instance that is handed over to an
     * {@link java.lang.reflect.InvocationHandler} is cached in a static field. By invoking this method,
     * this feature can be disabled.
     *
     * @return A similar invocation handler adapter that applies caching.
     */
    public abstract WithoutPrivilegeConfiguration withoutMethodCache();

    /**
     * Configures an assigner to use with this invocation handler adapter.
     *
     * @param assigner The assigner to apply when defining this implementation.
     * @return This instrumentation with the given {@code assigner} configured.
     */
    public abstract Implementation withAssigner(Assigner assigner);

    /**
     * Configures that the method constants supplied to the invocation handler adapter are resolved using an {@link java.security.AccessController}.
     *
     * @return This instrumentation with a privileged lookup configured.
     */
    public abstract AssignerConfigurable withPrivilegedLookup();

    /**
     * Applies an implementation that delegates to a invocation handler.
     *
     * @param methodVisitor         The method visitor for writing the byte code to.
     * @param implementationContext The implementation context for the current implementation.
     * @param instrumentedMethod    The method that is instrumented.
     * @param preparingManipulation A stack manipulation that applies any preparation to the operand stack.
     * @param fieldDescription      The field that contains the value for the invocation handler.
     * @return The size of the applied assignment.
     */
    protected ByteCodeAppender.Size apply(MethodVisitor methodVisitor,
                                          Context implementationContext,
                                          MethodDescription instrumentedMethod,
                                          StackManipulation preparingManipulation,
                                          FieldDescription fieldDescription) {
        if (instrumentedMethod.isStatic()) {
            throw new IllegalStateException("It is not possible to apply an invocation handler onto the static method " + instrumentedMethod);
        }
        MethodConstant.CanCache methodConstant = privileged
                ? MethodConstant.ofPrivileged(instrumentedMethod.asDefined())
                : MethodConstant.of(instrumentedMethod.asDefined());
        StackManipulation.Size stackSize = new StackManipulation.Compound(
                preparingManipulation,
                FieldAccess.forField(fieldDescription).read(),
                MethodVariableAccess.loadThis(),
                cached ? methodConstant.cached() : methodConstant,
                ArrayFactory.forType(TypeDescription.Generic.OBJECT).withValues(argumentValuesOf(instrumentedMethod)),
                MethodInvocation.invoke(INVOCATION_HANDLER_TYPE.getDeclaredMethods().getOnly()),
                assigner.assign(TypeDescription.Generic.OBJECT, instrumentedMethod.getReturnType(), Assigner.Typing.DYNAMIC),
                MethodReturn.of(instrumentedMethod.getReturnType())
        ).apply(methodVisitor, implementationContext);
        return new ByteCodeAppender.Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
    }

    /**
     * Allows for the configuration of an {@link net.bytebuddy.implementation.bytecode.assign.Assigner}
     * of an {@link net.bytebuddy.implementation.InvocationHandlerAdapter}.
     */
    public interface AssignerConfigurable extends Implementation {

        /**
         * Configures an assigner to use with this invocation handler adapter.
         *
         * @param assigner The assigner to apply when defining this implementation.
         * @return This instrumentation with the given {@code assigner} configured.
         */
        Implementation withAssigner(Assigner assigner);
    }

    /**
     * Allows the configuration of privileged lookup for the resolution of {@link java.lang.reflect.Method}
     * constants that are provided to the invocation handler.
     */
    public interface WithoutPrivilegeConfiguration extends AssignerConfigurable {

        /**
         * Configures that the method constants supplied to the invocation handler adapter are resolved using an {@link java.security.AccessController}.
         *
         * @return This instrumentation with a privileged lookup configured.
         */
        AssignerConfigurable withPrivilegedLookup();
    }

    /**
     * An implementation of an {@link net.bytebuddy.implementation.InvocationHandlerAdapter} that delegates method
     * invocations to an adapter that is stored in a static field.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class ForInstance extends InvocationHandlerAdapter implements WithoutPrivilegeConfiguration {

        /**
         * The prefix for field that are created for storing the instrumented value.
         */
        private static final String PREFIX = "invocationHandler";

        /**
         * The invocation handler to which method interceptions are to be delegated.
         */
        protected final InvocationHandler invocationHandler;

        /**
         * Creates a new invocation handler adapter for delegating invocations to an invocation handler that is stored
         * in a static field.
         *
         * @param fieldName         The name of the field.
         * @param cached            Determines if the {@link java.lang.reflect.Method} instances that are handed to the
         *                          intercepted methods are cached in {@code static} fields.
         * @param privileged        Determines if the {@link java.lang.reflect.Method} instances are retrieved by
         *                          using an {@link java.security.AccessController}.
         * @param assigner          The assigner to apply when defining this implementation.
         * @param invocationHandler The invocation handler to which all method calls are delegated.
         */
        protected ForInstance(String fieldName, boolean cached, boolean privileged, Assigner assigner, InvocationHandler invocationHandler) {
            super(fieldName, cached, privileged, assigner);
            this.invocationHandler = invocationHandler;
        }

        @Override
        public WithoutPrivilegeConfiguration withoutMethodCache() {
            return new ForInstance(fieldName, UNCACHED, privileged, assigner, invocationHandler);
        }

        @Override
        public Implementation withAssigner(Assigner assigner) {
            return new ForInstance(fieldName, cached, privileged, assigner, invocationHandler);
        }

        @Override
        public AssignerConfigurable withPrivilegedLookup() {
            return new ForInstance(fieldName, cached, PRIVILEGED, assigner, invocationHandler);
        }

        /**
         * {@inheritDoc}
         */
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType
                    .withField(new FieldDescription.Token(fieldName,
                            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE | Opcodes.ACC_SYNTHETIC,
                            INVOCATION_HANDLER_TYPE))
                    .withInitializer(new LoadedTypeInitializer.ForStaticField(fieldName, invocationHandler));
        }

        /**
         * {@inheritDoc}
         */
        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(implementationTarget.getInstrumentedType());
        }

        /**
         * An appender for implementing the {@link ForInstance}.
         */
        @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
        protected class Appender implements ByteCodeAppender {

            /**
             * The instrumented type for which the methods are being intercepted.
             */
            private final TypeDescription instrumentedType;

            /**
             * Creates a new appender.
             *
             * @param instrumentedType The type that is instrumented.
             */
            protected Appender(TypeDescription instrumentedType) {
                this.instrumentedType = instrumentedType;
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                return ForInstance.this.apply(methodVisitor,
                        implementationContext,
                        instrumentedMethod,
                        StackManipulation.Trivial.INSTANCE,
                        instrumentedType.getDeclaredFields().filter(named(fieldName).and(genericFieldType(INVOCATION_HANDLER_TYPE))).getOnly());
            }
        }
    }

    /**
     * An implementation of an {@link net.bytebuddy.implementation.InvocationHandlerAdapter} that delegates method
     * invocations to an adapter that is stored in an instance field.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class ForField extends InvocationHandlerAdapter implements WithoutPrivilegeConfiguration {

        /**
         * The field locator factory to use.
         */
        private final FieldLocator.Factory fieldLocatorFactory;

        /**
         * Creates a new invocation handler adapter that loads its value from a field.
         *
         * @param fieldName           The name of the field.
         * @param cached              Determines if the {@link java.lang.reflect.Method} instances that are handed to the
         *                            intercepted methods are cached in {@code static} fields.
         * @param privileged          Determines if the {@link java.lang.reflect.Method} instances are retrieved by using
         *                            an {@link java.security.AccessController}.
         * @param assigner            The assigner to apply when defining this implementation.
         * @param fieldLocatorFactory The field locator factory to use.
         */
        protected ForField(String fieldName, boolean cached, boolean privileged, Assigner assigner, FieldLocator.Factory fieldLocatorFactory) {
            super(fieldName, cached, privileged, assigner);
            this.fieldLocatorFactory = fieldLocatorFactory;
        }

        @Override
        public WithoutPrivilegeConfiguration withoutMethodCache() {
            return new ForField(fieldName, UNCACHED, privileged, assigner, fieldLocatorFactory);
        }

        @Override
        public Implementation withAssigner(Assigner assigner) {
            return new ForField(fieldName, cached, privileged, assigner, fieldLocatorFactory);
        }

        @Override
        public AssignerConfigurable withPrivilegedLookup() {
            return new ForField(fieldName, cached, PRIVILEGED, assigner, fieldLocatorFactory);
        }

        /**
         * {@inheritDoc}
         */
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        /**
         * {@inheritDoc}
         */
        public ByteCodeAppender appender(Target implementationTarget) {
            FieldLocator.Resolution resolution = fieldLocatorFactory.make(implementationTarget.getInstrumentedType()).locate(fieldName);
            if (!resolution.isResolved()) {
                throw new IllegalStateException("Could not find a field named '" + fieldName + "' for " + implementationTarget.getInstrumentedType());
            } else if (!resolution.getField().getType().asErasure().isAssignableTo(InvocationHandler.class)) {
                throw new IllegalStateException("Field " + resolution.getField() + " does not declare a type that is assignable to invocation handler");
            }
            return new Appender(resolution.getField());
        }

        /**
         * An appender for implementing the {@link ForField}.
         */
        @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
        protected class Appender implements ByteCodeAppender {

            /**
             * The field that contains the invocation handler.
             */
            private final FieldDescription fieldDescription;

            /**
             * Creates a new appender.
             *
             * @param fieldDescription The field that contains the invocation handler.
             */
            protected Appender(FieldDescription fieldDescription) {
                this.fieldDescription = fieldDescription;
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                return ForField.this.apply(methodVisitor,
                        implementationContext,
                        instrumentedMethod,
                        fieldDescription.isStatic()
                                ? StackManipulation.Trivial.INSTANCE
                                : MethodVariableAccess.loadThis(),
                        fieldDescription);
            }
        }
    }
}
