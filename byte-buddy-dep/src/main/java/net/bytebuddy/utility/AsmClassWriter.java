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
package net.bytebuddy.utility;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import net.bytebuddy.utility.nullability.AlwaysNull;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.privilege.GetSystemPropertyAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.lang.reflect.Method;
import java.security.PrivilegedAction;

/**
 * A facade for creating a {@link ClassVisitor} that writes a class file.
 */
public interface AsmClassWriter {

    /**
     * Returns the {@link ClassVisitor} to use for writing the class file.
     *
     * @return An appropriate class visitor.
     */
    ClassVisitor getVisitor();

    /**
     * Returns the binary representation of the created class file.
     *
     * @return The binary representation of the created class file.
     */
    byte[] getBinaryRepresentation();

    /**
     * A factory for creating an {@link AsmClassWriter}.
     */
    interface Factory {

        /**
         * Creates a new class writer for the given flags.
         *
         * @param flags The flags to consider while writing a class file.
         * @return An appropriate class writer.
         */
        AsmClassWriter make(int flags);

        /**
         * Creates a new class writer for the given flags, possibly based on a previous class file representation.
         *
         * @param flags       The flags to consider while writing a class file.
         * @param classReader A class reader to consider for writing a class file.
         * @return An appropriate class writer.
         */
        AsmClassWriter make(int flags, AsmClassReader classReader);

        /**
         * Creates a new class writer for the given flags.
         *
         * @param flags    The flags to consider while writing a class file.
         * @param typePool A type pool to use for resolving type information for frame generation.
         * @return An appropriate class writer.
         */
        AsmClassWriter make(int flags, TypePool typePool);

        /**
         * Creates a new class writer for the given flags, possibly based on a previous class file representation.
         *
         * @param flags       The flags to consider while writing a class file.
         * @param classReader A class reader to consider for writing a class file.
         * @param typePool    A type pool to use for resolving type information for frame generation.
         * @return An appropriate class writer.
         */
        AsmClassWriter make(int flags, AsmClassReader classReader, TypePool typePool);

        /**
         * Default implementations for factories of {@link AsmClassWriter}s.
         */
        enum Default implements Factory {

            /**
             * Uses a processor as it is configured by {@link OpenedClassReader#PROCESSOR_PROPERTY},
             * or {@link Default#ASM_FIRST} if no implicit processor is defined.
             */
            IMPLICIT {
                /**
                 * {@inheritDoc}
                 */
                public AsmClassWriter make(int flags, AsmClassReader classReader, TypePool typePool) {
                    return FACTORY.make(flags, classReader, typePool);
                }
            },

            /**
             * A factory for a class reader that uses ASM's internal implementation whenever possible.
             */
            ASM_FIRST {
                /**
                 * {@inheritDoc}
                 */
                public AsmClassWriter make(int flags, AsmClassReader classReader, TypePool typePool) {
                    return ClassFileVersion.ofThisVm().isGreaterThan(ClassFileVersion.latest())
                            ? CLASS_FILE_API_ONLY.make(flags, classReader, typePool)
                            : ASM_ONLY.make(flags, classReader, typePool);
                }
            },

            /**
             * A factory for a class writer that uses the class file API whenever possible.
             */
            CLASS_FILE_API_FIRST {
                /**
                 * {@inheritDoc}
                 */
                public AsmClassWriter make(int flags, AsmClassReader classReader, TypePool typePool) {
                    return ClassFileVersion.ofThisVm().isAtLeast(ClassFileVersion.JAVA_V24)
                            ? CLASS_FILE_API_ONLY.make(flags, classReader, typePool)
                            : ASM_ONLY.make(flags, classReader, typePool);
                }
            },

            /**
             * A factory that will always use ASM's internal implementation.
             */
            ASM_ONLY {
                /**
                 * {@inheritDoc}
                 */
                public AsmClassWriter make(int flags, AsmClassReader classReader, TypePool typePool) {
                    ClassReader unwrapped = classReader.unwrap(ClassReader.class);
                    return new ForAsm(unwrapped == null
                            ? new FrameComputingClassWriter(flags, typePool)
                            : new FrameComputingClassWriter(unwrapped, flags, typePool));
                }
            },

            /**
             * A factory that will always use the Class File API.
             */
            CLASS_FILE_API_ONLY {
                /**
                 * {@inheritDoc}
                 */
                @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "False positive in FindBugs.")
                public AsmClassWriter make(int flags, AsmClassReader classReader, TypePool typePool) {
                    Object jdkClassReader = JDK_CLASS_READER == null ? null : classReader.unwrap(JDK_CLASS_READER);
                    if (jdkClassReader == null) {
                        return new ForClassFileApi(ForClassFileApi.DISPATCHER.make(flags,
                                SuperClassResolvingJdkClassWriter.GET_SUPER_CLASS,
                                new SuperClassResolvingJdkClassWriter(typePool)));
                    } else {
                        return new ForClassFileApi(ForClassFileApi.DISPATCHER.make(jdkClassReader,
                                flags,
                                SuperClassResolvingJdkClassWriter.GET_SUPER_CLASS,
                                new SuperClassResolvingJdkClassWriter(typePool)));
                    }
                }
            };

            /**
             * The {@code codes.rafael.asmjdkbridge.JdkClassReader} type or {@code null} if not available.
             */
            @MaybeNull
            private static final Class<?> JDK_CLASS_READER;

            /**
             * The implicit factory to use for writing class files.
             */
            private static final Factory FACTORY;

            /*
             * Resolves the implicit writer factory, if any and locates a possible {@code JdkClassReader} type.
             */
            static {
                String processor;
                try {
                    processor = doPrivileged(new GetSystemPropertyAction(OpenedClassReader.PROCESSOR_PROPERTY));
                } catch (Throwable ignored) {
                    processor = null;
                }
                FACTORY = processor == null ? Default.ASM_FIRST : Default.valueOf(processor);
                Class<?> type;
                try {
                    type = ClassFileVersion.ofThisVm().isAtLeast(ClassFileVersion.JAVA_V24)
                        ? Class.forName("codes.rafael.asmjdkbridge.JdkClassReader")
                        : null;
                } catch (ClassNotFoundException ignored) {
                    type = null;
                }
                JDK_CLASS_READER = type;
            }

            /**
             * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
             *
             * @param action The action to execute from a privileged context.
             * @param <T>    The type of the action's resolved value.
             * @return The action's resolved value.
             */
            @MaybeNull
            @AccessControllerPlugin.Enhance
            private static <T> T doPrivileged(PrivilegedAction<T> action) {
                return action.run();
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassWriter make(int flags) {
                return make(flags, TypePool.Empty.INSTANCE);
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassWriter make(int flags, AsmClassReader classReader) {
                return make(flags, classReader, TypePool.Empty.INSTANCE);
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassWriter make(int flags, TypePool typePool) {
                return make(flags, EmptyAsmClassReader.INSTANCE, typePool);
            }

            /**
             * An empty class reader for ASM that never unwraps an underlying implementation.
             */
            protected enum EmptyAsmClassReader implements AsmClassReader {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                @AlwaysNull
                public <T> T unwrap(Class<T> type) {
                    return null;
                }

                /**
                 * {@inheritDoc}
                 */
                public void accept(ClassVisitor classVisitor, int flags) {
                    throw new UnsupportedOperationException();
                }
            }
        }

        /**
         * A class writer factory that suppresses any class reader implementation that might be provided
         * upon constructing a class writer.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Suppressing implements Factory {

            /**
             * The factory to delegate to.
             */
            private final Factory delegate;

            /**
             * Creates a suppressing class writer factory.
             *
             * @param delegate The factory to delegate to.
             */
            public Suppressing(Factory delegate) {
                this.delegate = delegate;
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassWriter make(int flags) {
                return delegate.make(flags);
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassWriter make(int flags, AsmClassReader classReader) {
                return delegate.make(flags);
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassWriter make(int flags, TypePool typePool) {
                return delegate.make(flags, typePool);
            }

            /**
             * {@inheritDoc}
             */
            public AsmClassWriter make(int flags, AsmClassReader classReader, TypePool typePool) {
                return delegate.make(flags, typePool);
            }
        }
    }

    /**
     * Am implementation that uses ASM's internal {@link ClassWriter}.
     */
    class ForAsm implements AsmClassWriter {

        /**
         * The represented class writer.
         */
        private final ClassWriter classWriter;

        /**
         * Creates a new class writer based upon ASM's own implementation.
         *
         * @param classWriter The represented class writer.
         */
        public ForAsm(ClassWriter classWriter) {
            this.classWriter = classWriter;
        }

        /**
         * {@inheritDoc}
         */
        public ClassVisitor getVisitor() {
            return classWriter;
        }

        /**
         * {@inheritDoc}
         */
        public byte[] getBinaryRepresentation() {
            return classWriter.toByteArray();
        }
    }

    /**
     * A Class File API-based implementation for a class writer.
     */
    class ForClassFileApi implements AsmClassWriter {

        /**
         * The dispatcher for interacting with a {@code codes.rafael.asmjdkbridge.JdkClassWriter}.
         */
        private static final JdkClassWriter DISPATCHER = doPrivileged(JavaDispatcher.of(
                JdkClassWriter.class,
                ForClassFileApi.class.getClassLoader()));

        /**
         * The represented class writer.
         */
        private final ClassVisitor classWriter;

        /**
         * Creates a new class file API-based class writer.
         *
         * @param classWriter The represented class writer.
         */
        public ForClassFileApi(ClassVisitor classWriter) {
            if (!DISPATCHER.isInstance(classWriter)) {
                throw new IllegalArgumentException("Not a JDK class writer: " + classWriter);
            }
            this.classWriter = classWriter;
        }

        /**
         * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
         *
         * @param action The action to execute from a privileged context.
         * @param <T>    The type of the action's resolved value.
         * @return The action's resolved value.
         */
        @AccessControllerPlugin.Enhance
        private static <T> T doPrivileged(PrivilegedAction<T> action) {
            return action.run();
        }

        /**
         * {@inheritDoc}
         */
        public ClassVisitor getVisitor() {
            return classWriter;
        }

        /**
         * {@inheritDoc}
         */
        public byte[] getBinaryRepresentation() {
            return DISPATCHER.toByteArray(classWriter);
        }

        /**
         * An API to interact with {@code codes.rafael.asmjdkbridge.JdkClassWriter}.
         */
        @JavaDispatcher.Proxied("codes.rafael.asmjdkbridge.JdkClassWriter")
        protected interface JdkClassWriter {

            /**
             * Checks if the supplied instance is a {@code codes.rafael.asmjdkbridge.JdkClassWriter}.
             *
             * @param value The value to evaluate.
             * @return {@code true} if the supplied instance is a  {@code codes.rafael.asmjdkbridge.JdkClassWriter}.
             */
            @JavaDispatcher.Instance
            boolean isInstance(ClassVisitor value);

            /**
             * Create a new {@code codes.rafael.asmjdkbridge.JdkClassWriter}.
             *
             * @param flags         The flags to consider.
             * @param getSuperClass A resolver for the super class.
             * @param target        The target to invoke the super class resolver upon.
             * @return A new {@code codes.rafael.asmjdkbridge.JdkClassWriter}.
             */
            @JavaDispatcher.IsConstructor
            ClassVisitor make(int flags, Method getSuperClass, Object target);

            /**
             * Create a new {@code codes.rafael.asmjdkbridge.JdkClassWriter}.
             *
             * @param classReader   The class reader of which to reuse the constant pool.
             * @param flags         The flags to consider.
             * @param getSuperClass A resolver for the super class.
             * @param target        The target to invoke the super class resolver upon.
             * @return A new {@code codes.rafael.asmjdkbridge.JdkClassWriter}.
             */
            @JavaDispatcher.IsConstructor
            ClassVisitor make(@JavaDispatcher.Proxied("codes.rafael.asmjdkbridge.JdkClassReader") Object classReader,
                              int flags,
                              Method getSuperClass,
                              Object target);

            /**
             * Reads the created class file byte array from a given {@code codes.rafael.asmjdkbridge.JdkClassWriter}.
             *
             * @param value The {@code codes.rafael.asmjdkbridge.JdkClassWriter} to read from.
             * @return The generated class file.
             */
            byte[] toByteArray(ClassVisitor value);
        }
    }

    /**
     * A class writer that piggy-backs on Byte Buddy's {@link TypePool} to avoid class loading or look-up errors when redefining a class.
     * This is not available when creating a new class where automatic frame computation is however not normally a requirement.
     */
    class FrameComputingClassWriter extends ClassWriter {

        /**
         * The type pool to use for computing stack map frames, if required.
         */
        private final TypePool typePool;

        /**
         * Creates a new frame computing class writer.
         *
         * @param flags    The flags to be handed to the writer.
         * @param typePool The type pool to use for computing stack map frames, if required.
         */
        public FrameComputingClassWriter(int flags, TypePool typePool) {
            super(flags);
            this.typePool = typePool;
        }

        /**
         * Creates a new frame computing class writer.
         *
         * @param classReader The class reader from which the original class is read.
         * @param flags       The flags to be handed to the writer.
         * @param typePool    The type pool to use for computing stack map frames, if required.
         */
        public FrameComputingClassWriter(ClassReader classReader, int flags, TypePool typePool) {
            super(classReader, flags);
            this.typePool = typePool;
        }

        /**
         * {@inheritDoc}
         */
        protected String getCommonSuperClass(String leftTypeName, String rightTypeName) {
            TypeDescription leftType = typePool.describe(leftTypeName.replace('/', '.')).resolve();
            TypeDescription rightType = typePool.describe(rightTypeName.replace('/', '.')).resolve();
            if (leftType.isAssignableFrom(rightType)) {
                return leftType.getInternalName();
            } else if (leftType.isAssignableTo(rightType)) {
                return rightType.getInternalName();
            } else if (leftType.isInterface() || rightType.isInterface()) {
                return TypeDescription.ForLoadedType.of(Object.class).getInternalName();
            } else {
                do {
                    TypeDescription.Generic superClass = leftType.getSuperClass();
                    if (superClass == null) {
                        return TypeDescription.ForLoadedType.of(Object.class).getInternalName();
                    }
                    leftType = superClass.asErasure();
                } while (!leftType.isAssignableFrom(rightType));
                return leftType.getInternalName();
            }
        }
    }

    /**
     * A pseudo-JDK class writer that resolves super classes using a {@link TypePool}, to pass in the constructor.
     */
    class SuperClassResolvingJdkClassWriter {

        /**
         * The {@link SuperClassResolvingJdkClassWriter#getSuperClass(String)} method.
         */
        protected static final Method GET_SUPER_CLASS;

        /*
         * Resolve the method instance to use for reflection.
         */
        static {
            Method getSuperClass;
            try {
                getSuperClass = SuperClassResolvingJdkClassWriter.class.getMethod("getSuperClass", String.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Failed to resolve own method", e);
            }
            GET_SUPER_CLASS = getSuperClass;
        }

        /**
         * The {@link TypePool} to use.
         */
        private final TypePool typePool;

        /**
         * Creates a super class resolving JDK class writer.
         *
         * @param typePool The {@link TypePool} to use.
         */
        public SuperClassResolvingJdkClassWriter(TypePool typePool) {
            this.typePool = typePool;
        }

        /**
         * Resolves the super class for a given internal class name, or {@code null} if a given class
         * represents an interface. The provided class name will never be {@link Object}.
         *
         * @param internalName The internal name of the class or interface of which to return a super type.
         * @return The internal name of the super class or {@code null} if the provided type name represents
         * an interface.
         */
        @MaybeNull
        @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Object class can never be passed.")
        public String getSuperClass(String internalName) {
            TypeDescription typeDescription = typePool.describe(internalName.replace('/', '.')).resolve();
            return typeDescription.isInterface()
                    ? null
                    : typeDescription.getSuperClass().asErasure().getInternalName();
        }
    }
}
