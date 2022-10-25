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
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaType;
import net.bytebuddy.utility.OpenedClassReader;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.annotation.*;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A build tool plugin that instruments methods to dispatch to methods {@code java.security.AccessController} with equal signature.
 * This can be useful to avoid using types from the {@code java.security} package which were deprecated for removal in Java 17.
 * Annotated methods are dispatched to the JVM's access controller only if this type is available on the current VM, and if no system
 * property is added and set to disable such dispatch. In the process, {@code java.security.AccessControlContext} is represented by
 * {@link Object}.
 */
@HashCodeAndEqualsPlugin.Enhance
public class AccessControllerPlugin extends Plugin.ForElementMatcher implements Plugin.Factory {

    /**
     * The binary name of {@code java.security.AccessController}.
     */
    private static final String ACCESS_CONTROLLER = "java.security.AccessController";

    /**
     * The name of the generated field.
     */
    private static final String NAME = "ACCESS_CONTROLLER";

    /**
     * An empty array to create frames without additional allocation.
     */
    private static final Object[] EMPTY = new Object[0];

    /**
     * A map to all signatures of {@code java.security.AccessController} from a signature that does not contain any
     * types that are deprecated for removal.
     */
    private static final Map<MethodDescription.SignatureToken, MethodDescription.SignatureToken> SIGNATURES;

    /*
     * Adds all relevant access controller signatures to validating collection.
     */
    static {
        SIGNATURES = new HashMap<MethodDescription.SignatureToken, MethodDescription.SignatureToken>();
        SIGNATURES.put(new MethodDescription.SignatureToken("doPrivileged",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedAction.class)), new MethodDescription.SignatureToken("doPrivileged",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedAction.class)));
        SIGNATURES.put(new MethodDescription.SignatureToken("doPrivilegedWithCombiner",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedAction.class)), new MethodDescription.SignatureToken("doPrivilegedWithCombiner",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedAction.class)));
        SIGNATURES.put(new MethodDescription.SignatureToken("doPrivileged",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedAction.class),
                TypeDescription.ForLoadedType.of(Object.class)), new MethodDescription.SignatureToken("doPrivileged",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedAction.class),
                JavaType.ACCESS_CONTROL_CONTEXT.getTypeStub()));
        SIGNATURES.put(new MethodDescription.SignatureToken("doPrivileged",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedAction.class),
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(Permission[].class)), new MethodDescription.SignatureToken("doPrivileged",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedAction.class),
                JavaType.ACCESS_CONTROL_CONTEXT.getTypeStub(),
                TypeDescription.ForLoadedType.of(Permission[].class)));
        SIGNATURES.put(new MethodDescription.SignatureToken("doPrivilegedWithCombiner",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedAction.class),
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(Permission[].class)), new MethodDescription.SignatureToken("doPrivilegedWithCombiner",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedAction.class),
                JavaType.ACCESS_CONTROL_CONTEXT.getTypeStub(),
                TypeDescription.ForLoadedType.of(Permission[].class)));
        SIGNATURES.put(new MethodDescription.SignatureToken("doPrivileged",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedExceptionAction.class)), new MethodDescription.SignatureToken("doPrivileged",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedExceptionAction.class)));
        SIGNATURES.put(new MethodDescription.SignatureToken("doPrivilegedWithCombiner",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedExceptionAction.class)), new MethodDescription.SignatureToken("doPrivilegedWithCombiner",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedExceptionAction.class)));
        SIGNATURES.put(new MethodDescription.SignatureToken("doPrivileged",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedExceptionAction.class),
                TypeDescription.ForLoadedType.of(Object.class)), new MethodDescription.SignatureToken("doPrivileged",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedExceptionAction.class),
                JavaType.ACCESS_CONTROL_CONTEXT.getTypeStub()));
        SIGNATURES.put(new MethodDescription.SignatureToken("doPrivileged",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedExceptionAction.class),
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(Permission[].class)), new MethodDescription.SignatureToken("doPrivileged",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedExceptionAction.class),
                JavaType.ACCESS_CONTROL_CONTEXT.getTypeStub(),
                TypeDescription.ForLoadedType.of(Permission[].class)));
        SIGNATURES.put(new MethodDescription.SignatureToken("doPrivilegedWithCombiner",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedExceptionAction.class),
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(Permission[].class)), new MethodDescription.SignatureToken("doPrivilegedWithCombiner",
                TypeDescription.ForLoadedType.of(Object.class),
                TypeDescription.ForLoadedType.of(PrivilegedExceptionAction.class),
                JavaType.ACCESS_CONTROL_CONTEXT.getTypeStub(),
                TypeDescription.ForLoadedType.of(Permission[].class)));
        SIGNATURES.put(new MethodDescription.SignatureToken("getContext",
                TypeDescription.ForLoadedType.of(Object.class)), new MethodDescription.SignatureToken("getContext",
                JavaType.ACCESS_CONTROL_CONTEXT.getTypeStub()));
        SIGNATURES.put(new MethodDescription.SignatureToken("checkPermission",
                TypeDescription.ForLoadedType.of(void.class),
                TypeDescription.ForLoadedType.of(Permission.class)), new MethodDescription.SignatureToken("checkPermission",
                TypeDescription.ForLoadedType.of(void.class),
                TypeDescription.ForLoadedType.of(Permission.class)));
    }

    /**
     * The property to control if the access controller should be used even
     * if available or {@code null} if such a property should not be available.
     */
    @MaybeNull
    @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
    private final String property;

    /**
     * Creates a new plugin to weave access controller dispatches without
     * a property to allow for disabling the access controller handling.
     */
    public AccessControllerPlugin() {
        this(null);
    }

    /**
     * Creates a new plugin to weave access controller dispatches.
     *
     * @param property The property to control if the access controller should be used even
     *                 if available or {@code null} if such a property should not be available.
     */
    @UsingReflection.Priority(Integer.MAX_VALUE)
    public AccessControllerPlugin(@MaybeNull String property) {
        super(declaresMethod(isAnnotatedWith(Enhance.class)));
        this.property = property;
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
    @SuppressFBWarnings(value = "SBSC_USE_STRINGBUFFER_CONCATENATION", justification = "Collision is unlikely and buffer overhead not justified.")
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        String name = NAME;
        while (!typeDescription.getDeclaredFields().filter(named(name)).isEmpty()) {
            name += "$";
        }
        return builder
                .defineField(name, boolean.class, Visibility.PRIVATE, Ownership.STATIC, FieldManifestation.FINAL)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().method(isAnnotatedWith(Enhance.class), new AccessControlWrapper(name)))
                .initializer(property == null
                        ? new Initializer.WithoutProperty(typeDescription, name)
                        : new Initializer.WithProperty(typeDescription, name, property));
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        /* do nothing */
    }

    /**
     * A byte code appender to create an initializer segment that determines if
     * the {@code java.security.AccessController} is available.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected abstract static class Initializer implements ByteCodeAppender {

        /**
         * The instrumented type.
         */
        private final TypeDescription instrumentedType;

        /**
         * The name of the field to determine the use of access controller dispatch.
         */
        private final String name;

        /**
         * Creates a new initializer.
         *
         * @param instrumentedType The instrumented type.
         * @param name             The name of the field to determine the use of access controller dispatch.
         */
        protected Initializer(TypeDescription instrumentedType, String name) {
            this.instrumentedType = instrumentedType;
            this.name = name;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
            Label start = new Label(), end = new Label(), classNotFound = new Label(), securityException = new Label(), complete = new Label();
            methodVisitor.visitTryCatchBlock(start, end, classNotFound, Type.getInternalName(ClassNotFoundException.class));
            methodVisitor.visitTryCatchBlock(start, end, securityException, Type.getInternalName(SecurityException.class));
            methodVisitor.visitLabel(start);
            methodVisitor.visitLdcInsn(ACCESS_CONTROLLER);
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                    Type.getInternalName(Class.class),
                    "forName",
                    Type.getMethodDescriptor(Type.getType(Class.class),
                            Type.getType(String.class),
                            Type.getType(boolean.class),
                            Type.getType(ClassLoader.class)),
                    false);
            methodVisitor.visitInsn(Opcodes.POP);
            int size = onAccessController(methodVisitor);
            methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC, instrumentedType.getInternalName(), name, Type.getDescriptor(boolean.class));
            methodVisitor.visitLabel(end);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, complete);
            methodVisitor.visitLabel(classNotFound);
            implementationContext.getFrameGeneration().same1(methodVisitor,
                    TypeDescription.ForLoadedType.of(ClassNotFoundException.class),
                    Collections.<TypeDefinition>emptyList());
            methodVisitor.visitInsn(Opcodes.POP);
            methodVisitor.visitInsn(Opcodes.ICONST_0);
            methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC, instrumentedType.getInternalName(), name, Type.getDescriptor(boolean.class));
            methodVisitor.visitJumpInsn(Opcodes.GOTO, complete);
            methodVisitor.visitLabel(securityException);
            implementationContext.getFrameGeneration().same1(methodVisitor,
                    TypeDescription.ForLoadedType.of(SecurityException.class),
                    Collections.<TypeDefinition>emptyList());
            methodVisitor.visitInsn(Opcodes.POP);
            methodVisitor.visitInsn(Opcodes.ICONST_1);
            methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC, instrumentedType.getInternalName(), name, Type.getDescriptor(boolean.class));
            methodVisitor.visitLabel(complete);
            implementationContext.getFrameGeneration().same(methodVisitor, Collections.<TypeDefinition>emptyList());
            return new Size(Math.max(3, size), 0);
        }

        /**
         * Invoked to determine if the access controller should be used after the class was found.
         *
         * @param methodVisitor The method visitor to dispatch to.
         * @return The size of the stack required to implement the implemented dispatch.
         */
        protected abstract int onAccessController(MethodVisitor methodVisitor);

        /**
         * An initializer that uses a property to determine if the access controller should be actually used even if it is available.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class WithProperty extends Initializer {

            /**
             * The name of the property.
             */
            private final String property;

            /**
             * Creates an initializer that uses a property to determine if the access controller should be actually used even if it is available.
             *
             * @param instrumentedType The instrumented type.
             * @param name             The name of the field to determine the use of access controller dispatch.
             * @param property         The name of the property.
             */
            protected WithProperty(TypeDescription instrumentedType, String name, String property) {
                super(instrumentedType, name);
                this.property = property;
            }

            @Override
            protected int onAccessController(MethodVisitor methodVisitor) {
                methodVisitor.visitLdcInsn(property);
                methodVisitor.visitLdcInsn("true");
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                        Type.getInternalName(System.class),
                        "getProperty",
                        Type.getMethodDescriptor(Type.getType(String.class), Type.getType(String.class), Type.getType(String.class)),
                        false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                        Type.getInternalName(Boolean.class),
                        "parseBoolean",
                        Type.getMethodDescriptor(Type.getType(boolean.class), Type.getType(String.class)),
                        false);
                return 2;
            }
        }

        /**
         * An initializer that always uses the access controller if it is available.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class WithoutProperty extends Initializer {

            /**
             * Creates an initializer that always uses the access controller if it is available.
             *
             * @param instrumentedType The instrumented type.
             * @param name             The name of the field to determine the use of access controller dispatch.
             */
            protected WithoutProperty(TypeDescription instrumentedType, String name) {
                super(instrumentedType, name);
            }

            @Override
            protected int onAccessController(MethodVisitor methodVisitor) {
                methodVisitor.visitInsn(Opcodes.ICONST_1);
                return 1;
            }
        }
    }

    /**
     * An wrapper for a method that represents a method of {@code AccessController} which is weaved.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class AccessControlWrapper implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

        /**
         * The name of the field.
         */
        private final String name;

        /**
         * Creates a new access control wrapper.
         *
         * @param name The name of the field.
         */
        protected AccessControlWrapper(String name) {
            this.name = name;
        }

        /**
         * {@inheritDoc}
         */
        public MethodVisitor wrap(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  MethodVisitor methodVisitor,
                                  Implementation.Context implementationContext,
                                  TypePool typePool,
                                  int writerFlags,
                                  int readerFlags) {
            MethodDescription.SignatureToken token = SIGNATURES.get(instrumentedMethod.asDefined().asSignatureToken());
            if (token == null) {
                throw new IllegalStateException(instrumentedMethod + " does not have a method with a matching signature in " + ACCESS_CONTROLLER);
            } else if (instrumentedMethod.isPublic() || instrumentedMethod.isProtected()) {
                throw new IllegalStateException(instrumentedMethod + " is either public or protected what is not permitted to avoid context leaks");
            }
            return new PrefixingMethodVisitor(methodVisitor,
                    instrumentedType,
                    token,
                    name,
                    instrumentedMethod.isStatic() ? 0 : 1,
                    implementationContext.getFrameGeneration());
        }

        /**
         * A method visitor to implement a weaved method to dispatch to an {@code java.security.AccessController}, if available.
         */
        protected static class PrefixingMethodVisitor extends MethodVisitor {

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * The target signature of the method declared by the JVM access controller.
             */
            private final MethodDescription.SignatureToken token;

            /**
             * The name of the field.
             */
            private final String name;

            /**
             * The base offset of the weaved method.
             */
            private final int offset;

            /**
             * Indicates the frame generation mode to apply.
             */
            private final Implementation.Context.FrameGeneration frameGeneration;

            /**
             * Creates a new prefixing method visitor.
             *
             * @param methodVisitor    The method visitor to write to.
             * @param instrumentedType The instrumented type.
             * @param token            The target signature of the method declared by the JVM access controller.
             * @param name             The name of the field.
             * @param offset           The base offset of the instrumented method.
             * @param frameGeneration  Indicates the frame generation mode to apply.
             */
            protected PrefixingMethodVisitor(MethodVisitor methodVisitor,
                                             TypeDescription instrumentedType,
                                             MethodDescription.SignatureToken token,
                                             String name,
                                             int offset,
                                             Implementation.Context.FrameGeneration frameGeneration) {
                super(OpenedClassReader.ASM_API, methodVisitor);
                this.instrumentedType = instrumentedType;
                this.token = token;
                this.name = name;
                this.offset = offset;
                this.frameGeneration = frameGeneration;
            }

            @Override
            public void visitCode() {
                mv.visitCode();
                mv.visitFieldInsn(Opcodes.GETSTATIC, instrumentedType.getInternalName(), name, Type.getDescriptor(boolean.class));
                Label label = new Label();
                mv.visitJumpInsn(Opcodes.IFEQ, label);
                int offset = this.offset;
                for (TypeDescription typeDescription : token.getParameterTypes()) {
                    mv.visitVarInsn(Type.getType(typeDescription.getDescriptor()).getOpcode(Opcodes.ILOAD), offset);
                    if (typeDescription.equals(JavaType.ACCESS_CONTROL_CONTEXT.getTypeStub())) {
                        mv.visitTypeInsn(Opcodes.CHECKCAST, typeDescription.getInternalName());
                    }
                    offset += typeDescription.getStackSize().getSize();
                }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        ACCESS_CONTROLLER.replace('.', '/'),
                        token.getName(),
                        token.getDescriptor(),
                        false);
                mv.visitInsn(Type.getType(token.getReturnType().getDescriptor()).getOpcode(Opcodes.IRETURN));
                mv.visitLabel(label);
                frameGeneration.same(mv, token.getParameterTypes());
            }

            @Override
            public void visitMaxs(int stackSize, int localVariableLength) {
                mv.visitMaxs(Math.max(Math.max(StackSize.of(token.getParameterTypes()),
                        token.getReturnType().getStackSize().getSize()), stackSize), localVariableLength);
            }
        }
    }

    /**
     * Indicates that the annotated method represents a pseudo implementation of {@code java.security.AccessController}
     * which can be weaved to dispatch to the access controller if this is possible on the current VM and not explicitly
     * disabled on the current VM via a system property.
     */
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Enhance {
        /* empty */
    }
}
