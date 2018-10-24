/*
 * Copyright 2014 - 2018 Rafael Winterhalter
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
package net.bytebuddy.asm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.*;

/**
 * <p>
 * This class visitor wrapper ensures that class files of a version previous to Java 5 do not store class entries in the generated class's constant pool.
 * All found class instances are instead mapped as {@link String} values where the class constant is retrieved by a call to {@link Class#forName(String)}.
 * </p>
 * <p>
 * <b>Warning</b>: This can lead to subtle bugs as classes that are not available yield a {@link ClassNotFoundException} instead of a
 * {@link NoClassDefFoundError}. The former, checked exception could therefore be thrown even if the method that unsuccessfully loads a class does
 * not declared the checked exception. Furthermore, {@link Class} constants are not cached as fields within the class as the Java compiler implemented
 * class constants before Java 5. As a benefit for this limitation, the registered wrapper does not require any additional work by a {@link ClassWriter}
 * or {@link ClassReader}, i.e. does not set any flags.
 * </p>
 */
public enum TypeConstantAdjustment implements AsmVisitorWrapper {

    /**
     * The singleton instance.
     */
    INSTANCE;

    /**
     * {@inheritDoc}
     */
    public int mergeWriter(int flags) {
        return flags;
    }

    /**
     * {@inheritDoc}
     */
    public int mergeReader(int flags) {
        return flags;
    }

    /**
     * {@inheritDoc}
     */
    public ClassVisitor wrap(TypeDescription instrumentedType,
                             ClassVisitor classVisitor,
                             Implementation.Context implementationContext,
                             TypePool typePool,
                             FieldList<FieldDescription.InDefinedShape> fields,
                             MethodList<?> methods,
                             int writerFlags,
                             int readerFlags) {
        return new TypeConstantDissolvingClassVisitor(classVisitor);
    }

    /**
     * A class visitor that checks a class file version for its support of storing class constants in the constant pool and remaps such constants
     * on discovery if that is not the case.
     */
    protected static class TypeConstantDissolvingClassVisitor extends ClassVisitor {

        /**
         * {@code true} if the class file version supports class constants in a constant pool.
         */
        private boolean supportsTypeConstants;

        /**
         * Creates a new type constant dissolving class visitor.
         *
         * @param classVisitor The underlying class visitor.
         */
        protected TypeConstantDissolvingClassVisitor(ClassVisitor classVisitor) {
            super(OpenedClassReader.ASM_API, classVisitor);
        }

        @Override
        public void visit(int version, int modifiers, String name, String signature, String superClassName, String[] interfaceName) {
            supportsTypeConstants = ClassFileVersion.ofMinorMajor(version).isAtLeast(ClassFileVersion.JAVA_V5);
            super.visit(version, modifiers, name, signature, superClassName, interfaceName);
        }

        @Override
        public MethodVisitor visitMethod(int modifiers, String name, String descriptor, String signature, String[] exception) {
            MethodVisitor methodVisitor = super.visitMethod(modifiers, name, descriptor, signature, exception);
            return supportsTypeConstants || methodVisitor == null
                    ? methodVisitor
                    : new TypeConstantDissolvingMethodVisitor(methodVisitor);
        }

        /**
         * A method visitor that remaps class constants to invocations of {@link Class#forName(String)}.
         */
        protected static class TypeConstantDissolvingMethodVisitor extends MethodVisitor {

            /**
             * The internal name of the {@link Class} class.
             */
            private static final String JAVA_LANG_CLASS = "java/lang/Class";

            /**
             * The {@code forName} method name.
             */
            private static final String FOR_NAME = "forName";

            /**
             * The descriptor of the {@code forName} method.
             */
            private static final String DESCRIPTOR = "(Ljava/lang/String;)Ljava/lang/Class;";

            /**
             * Creates a new type constant dissolving method visitor.
             *
             * @param methodVisitor The underlying method visitor.
             */
            protected TypeConstantDissolvingMethodVisitor(MethodVisitor methodVisitor) {
                super(OpenedClassReader.ASM_API, methodVisitor);
            }

            @Override
            @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT", justification = "Fall through to default case is intentional")
            public void visitLdcInsn(Object value) {
                if (value instanceof Type) {
                    Type type = (Type) value;
                    switch (type.getSort()) {
                        case Type.OBJECT:
                        case Type.ARRAY:
                            super.visitLdcInsn(type.getInternalName().replace('/', '.'));
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, JAVA_LANG_CLASS, FOR_NAME, DESCRIPTOR, false);
                            return;
                    }
                }
                super.visitLdcInsn(value);
            }
        }
    }
}
