/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.build;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.FieldPersistence;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.utility.RandomString;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A plugin that caches the return value of a method in a synthetic field. The caching mechanism is not thread-safe but can be used in a
 * concurrent setup if the cached value is frozen, i.e. only defines {@code final} fields. In this context, it is possible that
 * the method is executed multiple times by different threads but at the same time, this approach avoids a {@code volatile} field
 * declaration. For methods with a primitive return type, the type's default value is used to indicate that a method was not yet invoked.
 * For methods that return a reference type, {@code null} is used as an indicator. If a method returns such a value, this mechanism will
 * not work. This plugin does not need to be closed.
 */
@HashCodeAndEqualsPlugin.Enhance
public class CachedReturnPlugin extends Plugin.ForElementMatcher implements Plugin.Factory {

    /**
     * An infix between a field and the random suffix if no field name is chosen.
     */
    private static final String NAME_INFIX = "_";

    /**
     * A description of the {@link Enhance#value()} method.
     */
    private static final MethodDescription.InDefinedShape ENHANCE_VALUE = TypeDescription.ForLoadedType.of(Enhance.class)
            .getDeclaredMethods()
            .filter(named("value"))
            .getOnly();

    /**
     * {@code true} if existing fields should be ignored if the field name was explicitly given.
     */
    private final boolean ignoreExistingFields;

    /**
     * A random string to use for avoid field name collisions.
     */
    @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE)
    private final RandomString randomString;

    /**
     * Creates a plugin for caching method return values. If a field name exists before applying this plugin, an exception is raised.
     */
    public CachedReturnPlugin() {
        this(false);
    }

    /**
     * Creates a plugin for caching method return values.
     *
     * @param ignoreExistingFields {@code true} if existing fields should be ignored if the field name was explicitly given.
     */
    public CachedReturnPlugin(boolean ignoreExistingFields) {
        super(declaresMethod(isAnnotatedWith(Enhance.class)));
        this.ignoreExistingFields = ignoreExistingFields;
        randomString = new RandomString();
    }

    /**
     * {@inheritDoc}
     */
    public Plugin make() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Annotation presence is required by matcher.")
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        for (MethodDescription.InDefinedShape methodDescription : typeDescription.getDeclaredMethods()
                .filter(not(isBridge()).<MethodDescription>and(isAnnotatedWith(Enhance.class)))) {
            if (methodDescription.isAbstract()) {
                throw new IllegalStateException("Cannot cache the value of an abstract method: " + methodDescription);
            } else if (!methodDescription.getParameters().isEmpty()) {
                throw new IllegalStateException("Cannot cache the value of a method with parameters: " + methodDescription);
            } else if (methodDescription.getReturnType().represents(void.class)) {
                throw new IllegalStateException("Cannot cache void result for " + methodDescription);
            }
            String name = methodDescription.getDeclaredAnnotations().ofType(Enhance.class)
                    .getValue(ENHANCE_VALUE)
                    .resolve(String.class);
            if (name.length() == 0) {
                name = methodDescription.getName() + NAME_INFIX + randomString.nextString();
            } else if (ignoreExistingFields && !typeDescription.getDeclaredFields().filter(named(name)).isEmpty()) {
                return builder;
            }
            builder = builder
                    .defineField(name, methodDescription.getReturnType().asErasure(), methodDescription.isStatic()
                            ? Ownership.STATIC
                            : Ownership.MEMBER, methodDescription.isStatic()
                            ? FieldPersistence.PLAIN
                            : FieldPersistence.TRANSIENT, Visibility.PRIVATE, SyntheticState.SYNTHETIC)
                    .visit(AdviceResolver
                            .of(methodDescription.getReturnType())
                            .toAdvice(name).on(is(methodDescription)));
        }
        return builder;
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        /* do nothing */
    }

    /**
     * Indicates methods that should be cached, i.e. where the return value is stored in a synthetic field. For this to be
     * possible, the returned value should not be altered and the instance must be thread-safe if the value might be used from
     * multiple threads.
     */
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Enhance {

        /**
         * The fields name or an empty string if the name should be generated randomly.
         *
         * @return The fields name or an empty string if the name should be generated randomly.
         */
        String value() default "";
    }

    /**
     * Indicates the field that stores the cached value.
     */
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface CacheField {
        /* empty */
    }

    /**
     * A resolver for {@link Advice} that caches a method's return type.
     */
    protected enum AdviceResolver {

        /**
         * A resolver for a {@code boolean} type.
         */
        BOOLEAN(boolean.class, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.NOP, Opcodes.IFNE),

        /**
         * A resolver for a {@code byte} type.
         */
        BYTE(byte.class, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.NOP, Opcodes.IFNE),

        /**
         * A resolver for a {@code short} type.
         */
        SHORT(short.class, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.NOP, Opcodes.IFNE),

        /**
         * A resolver for a {@code char} type.
         */
        CHARACTER(char.class, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.NOP, Opcodes.IFNE),

        /**
         * A resolver for a {@code int} type.
         */
        INTEGER(int.class, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.NOP, Opcodes.IFNE),

        /**
         * A resolver for a {@code long} type.
         */
        LONG(long.class, Opcodes.LLOAD, Opcodes.LSTORE, Opcodes.L2I, Opcodes.IFNE),

        /**
         * A resolver for a {@code float} type.
         */
        FLOAT(float.class, Opcodes.FLOAD, Opcodes.FSTORE, Opcodes.F2I, Opcodes.IFNE),

        /**
         * A resolver for a {@code double} type.
         */
        DOUBLE(double.class, Opcodes.DLOAD, Opcodes.DSTORE, Opcodes.D2I, Opcodes.IFNE),

        /**
         * A resolver for a reference type.
         */
        REFERENCE(Object.class, Opcodes.ALOAD, Opcodes.ASTORE, Opcodes.NOP, Opcodes.IFNONNULL);

        /**
         * The created dynamic type to use for advice.
         */
        private final DynamicType dynamicType;

        /**
         * Creates an advice resolver.
         *
         * @param type    The type of the return type.
         * @param load    The byte code that loads a value onto the stack from the local variable array.
         * @param store   The byte code that stores a value to the local variable array.
         * @param convert An instruction to convert the cached value to a value that is applied on the branch instruction.
         * @param branch  A jump instruction that checks if the cached value is already set.
         */
        AdviceResolver(Class<?> type, int load, int store, int convert, int branch) {
            dynamicType = new ByteBuddy(ClassFileVersion.JAVA_V6)
                    .with(TypeValidation.DISABLED)
                    .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                    .name(CachedReturnPlugin.class.getName() + "$Advice$" + this)
                    .defineMethod("enter", type, Ownership.STATIC)
                    .withParameter(type)
                    .annotateParameter(AnnotationDescription.Builder.ofType(CachedReturnPlugin.CacheField.class).build())
                    .intercept(new Implementation.Simple(
                            MethodVariableAccess.of(TypeDescription.ForLoadedType.of(type)).loadFrom(0),
                            MethodReturn.of(TypeDescription.ForLoadedType.of(type))
                    ))
                    .annotateMethod(AnnotationDescription.Builder.ofType(Advice.OnMethodEnter.class)
                            .define("skipOn", Advice.OnNonDefaultValue.class)
                            .build())
                    .defineMethod("exit", void.class, Ownership.STATIC)
                    .withParameter(type)
                    .annotateParameter(AnnotationDescription.Builder.ofType(Advice.Return.class)
                            .define("readOnly", false)
                            .define("typing", Assigner.Typing.DYNAMIC)
                            .build())
                    .withParameter(type)
                    .annotateParameter(AnnotationDescription.Builder.ofType(CachedReturnPlugin.CacheField.class).build())
                    .intercept(new Implementation.Simple(new ExitAdviceByteCodeAppender(load, store, convert, branch, StackSize.of(type).getSize())))
                    .annotateMethod(AnnotationDescription.Builder.ofType(Advice.OnMethodExit.class).build())
                    .make();
        }

        /**
         * Creates an advice resolver for a given type definition.
         *
         * @param typeDefinition The type definition for which advice is to be created.
         * @return An appropriate advice resolver.
         */
        protected static AdviceResolver of(TypeDefinition typeDefinition) {
            if (typeDefinition.represents(boolean.class)) {
                return BOOLEAN;
            } else if (typeDefinition.represents(byte.class)) {
                return BYTE;
            } else if (typeDefinition.represents(short.class)) {
                return SHORT;
            } else if (typeDefinition.represents(char.class)) {
                return CHARACTER;
            } else if (typeDefinition.represents(int.class)) {
                return INTEGER;
            } else if (typeDefinition.represents(long.class)) {
                return LONG;
            } else if (typeDefinition.represents(float.class)) {
                return FLOAT;
            } else if (typeDefinition.represents(double.class)) {
                return DOUBLE;
            } else if (typeDefinition.isPrimitive()) {
                throw new IllegalArgumentException("Unexpected advice type: " + typeDefinition);
            } else {
                return REFERENCE;
            }
        }

        /**
         * Resolve advice for a given field name.
         *
         * @param name The name of the field to resolve the advice for.
         * @return An appropriate advice.
         */
        protected Advice toAdvice(String name) {
            return Advice.withCustomMapping()
                    .bind(CacheField.class, new CacheFieldOffsetMapping(name))
                    .to(dynamicType.getTypeDescription(), dynamicType);
        }

        /**
         * A byte code appender for the exit advice.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class ExitAdviceByteCodeAppender implements ByteCodeAppender {

            /**
             * The byte code that loads a value onto the stack from the local variable array.
             */
            private final int load;

            /**
             * The byte code that stores a value to the local variable array.
             */
            private final int store;

            /**
             * An instruction to convert the cached value to a value that is applied on the branch instruction.
             */
            private final int convert;

            /**
             * A jump instruction that checks if the cached value is already set.
             */
            private final int branch;

            /**
             * The size of the created type on the operand stack.
             */
            private final int size;

            /**
             * Creates a byte code appender for exit advice on a cached return plugin.
             *
             * @param load    The byte code that loads a value onto the stack from the local variable array.
             * @param store   The byte code that stores a value to the local variable array.
             * @param convert An instruction to convert the cached value to a value that is applied on the branch instruction.
             * @param branch  A jump instruction that checks if the cached value is already set.
             * @param size    The size of the created type on the operand stack.
             */
            protected ExitAdviceByteCodeAppender(int load, int store, int convert, int branch, int size) {
                this.load = load;
                this.store = store;
                this.convert = convert;
                this.branch = branch;
                this.size = size;
            }

            /**
             * {@inheritDoc}
             */
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                Label complete = new Label(), uncached = new Label();
                methodVisitor.visitVarInsn(load, 0);
                if (convert != Opcodes.NOP) {
                    methodVisitor.visitInsn(convert);
                }
                methodVisitor.visitJumpInsn(branch, uncached);
                methodVisitor.visitVarInsn(load, size);
                methodVisitor.visitVarInsn(store, 0);
                methodVisitor.visitJumpInsn(Opcodes.GOTO, complete);
                methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                methodVisitor.visitLabel(uncached);
                methodVisitor.visitVarInsn(load, 0);
                methodVisitor.visitVarInsn(store, size);
                methodVisitor.visitLabel(complete);
                methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                methodVisitor.visitInsn(Opcodes.RETURN);
                return new Size(size * 2, instrumentedMethod.getStackSize());
            }
        }
    }

    /**
     * An offset mapping for the cached field.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class CacheFieldOffsetMapping implements Advice.OffsetMapping {

        /**
         * The field's name.
         */
        private final String name;

        /**
         * Creates an offset mapping for the cached field.
         *
         * @param name The field's name.
         */
        protected CacheFieldOffsetMapping(String name) {
            this.name = name;
        }

        /**
         * {@inheritDoc}
         */
        public Target resolve(TypeDescription instrumentedType,
                              MethodDescription instrumentedMethod,
                              Assigner assigner,
                              Advice.ArgumentHandler argumentHandler,
                              Sort sort) {
            return new Target.ForField.ReadWrite(instrumentedType.getDeclaredFields().filter(named(name)).getOnly());
        }
    }
}
