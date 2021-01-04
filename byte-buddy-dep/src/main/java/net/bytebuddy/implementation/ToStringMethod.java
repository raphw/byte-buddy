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
package net.bytebuddy.implementation;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * An implementation of {@link Object#toString()} that concatenates the {@link String} representation of all fields that are declared by a class.
 */
@HashCodeAndEqualsPlugin.Enhance
public class ToStringMethod implements Implementation {

    /**
     * The {@link StringBuilder#StringBuilder(String)} constructor.
     */
    private static final MethodDescription.InDefinedShape STRING_BUILDER_CONSTRUCTOR = TypeDescription.ForLoadedType.of(StringBuilder.class)
            .getDeclaredMethods()
            .filter(isConstructor().and(ElementMatchers.takesArguments(String.class)))
            .getOnly();

    /**
     * The {@link StringBuilder#toString()} method.
     */
    private static final MethodDescription.InDefinedShape TO_STRING = TypeDescription.ForLoadedType.of(StringBuilder.class)
            .getDeclaredMethods()
            .filter(isToString())
            .getOnly();

    /**
     * A resolver for the prefix of a {@link String} representation.
     */
    private final PrefixResolver prefixResolver;

    /**
     * A token that is added between the prefix and the first field value.
     */
    private final String start;

    /**
     * A token that is added after the last field value.
     */
    private final String end;

    /**
     * A token that is added between two field values.
     */
    private final String separator;

    /**
     * A token that is added between a field's name and its value.
     */
    private final String definer;

    /**
     * A filter that determines what fields to ignore.
     */
    private final ElementMatcher.Junction<? super FieldDescription.InDefinedShape> ignored;

    /**
     * Creates a new {@code toString} implementation.
     *
     * @param prefixResolver A resolver for the prefix of a {@link String} representation.
     */
    protected ToStringMethod(PrefixResolver prefixResolver) {
        this(prefixResolver, "{", "}", ", ", "=", none());
    }

    /**
     * Creates a new {@code toString} implementation.
     *
     * @param prefixResolver A resolver for the prefix of a {@link String} representation.
     * @param start          A token that is added between the prefix and the first field value.
     * @param end            A token that is added after the last field value.
     * @param separator      A token that is added between two field values.
     * @param definer        A token that is added between a field's name and its value.
     * @param ignored        A filter that determines what fields to ignore.
     */
    private ToStringMethod(PrefixResolver prefixResolver,
                           String start,
                           String end,
                           String separator,
                           String definer,
                           ElementMatcher.Junction<? super FieldDescription.InDefinedShape> ignored) {
        this.prefixResolver = prefixResolver;
        this.start = start;
        this.end = end;
        this.separator = separator;
        this.definer = definer;
        this.ignored = ignored;
    }

    /**
     * Creates a {@link Object#toString()} implementation that is prefixed by the fully qualified class name of the instrumented type.
     *
     * @return A {@link Object#toString()} implementation that is prefixed by the fully qualified class name of the instrumented type.
     */
    public static ToStringMethod prefixedByFullyQualifiedClassName() {
        return prefixedBy(PrefixResolver.Default.FULLY_QUALIFIED_CLASS_NAME);
    }

    /**
     * Creates a {@link Object#toString()} implementation that is prefixed by the canonical class name of the instrumented type.
     *
     * @return A {@link Object#toString()} implementation that is prefixed by the canonical class name of the instrumented type.
     */
    public static ToStringMethod prefixedByCanonicalClassName() {
        return prefixedBy(PrefixResolver.Default.CANONICAL_CLASS_NAME);
    }

    /**
     * Creates a {@link Object#toString()} implementation that is prefixed by the simple class name of the instrumented type.
     *
     * @return A {@link Object#toString()} implementation that is prefixed by the simple class name of the instrumented type.
     */
    public static ToStringMethod prefixedBySimpleClassName() {
        return prefixedBy(PrefixResolver.Default.SIMPLE_CLASS_NAME);
    }

    /**
     * Creates a {@link Object#toString()} implementation that is prefixed by the supplied string.
     *
     * @param prefix The prefix to use.
     * @return A {@link Object#toString()} implementation that is prefixed by the supplied string.
     */
    public static ToStringMethod prefixedBy(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix cannot be null");
        }
        return prefixedBy(new PrefixResolver.ForFixedValue(prefix));
    }

    /**
     * Creates a {@link Object#toString()} implementation that is prefixed by the string that is supplied by the given prefix resolver.
     *
     * @param prefixResolver The prefix resolver to use.
     * @return A {@link Object#toString()} implementation that is prefixed by the string that is supplied by the given prefix resolver.
     */
    public static ToStringMethod prefixedBy(PrefixResolver prefixResolver) {
        return new ToStringMethod(prefixResolver);
    }

    /**
     * Returns a new version of this toString method implementation that ignores the specified fields additionally to any
     * previously specified fields.
     *
     * @param ignored A matcher to specify any fields that should be ignored.
     * @return A new version of this toString method implementation that also ignores any fields matched by the provided matcher.
     */
    public ToStringMethod withIgnoredFields(ElementMatcher<? super FieldDescription.InDefinedShape> ignored) {
        return new ToStringMethod(prefixResolver, start, end, separator, definer, this.ignored.<FieldDescription.InDefinedShape>or(ignored));
    }

    /**
     * Changes the tokens used for the {@link Object#toString()} implementation.
     *
     * @param start     A token that is added between the prefix and the first field value.
     * @param end       A token that is added after the last field value.
     * @param separator A token that is added between two field values.
     * @param definer   A token that is added between two field values.
     * @return A new instance of this implementation that uses the supplied tokens.
     */
    public Implementation withTokens(String start, String end, String separator, String definer) {
        if (start == null || end == null || separator == null || definer == null) {
            throw new IllegalArgumentException("Token values cannot be null");
        }
        return new ToStringMethod(prefixResolver, start, end, separator, definer, ignored);
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
    public Appender appender(Target implementationTarget) {
        if (implementationTarget.getInstrumentedType().isInterface()) {
            throw new IllegalStateException("Cannot implement meaningful toString method for " + implementationTarget.getInstrumentedType());
        }
        String prefix = prefixResolver.resolve(implementationTarget.getInstrumentedType());
        if (prefix == null) {
            throw new IllegalStateException("Prefix for toString method cannot be null");
        }
        return new Appender(prefix,
                start,
                end,
                separator,
                definer,
                implementationTarget.getInstrumentedType().getDeclaredFields().filter(not(isStatic().or(ignored))));
    }

    /**
     * An appender to implement {@link ToStringMethod}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class Appender implements ByteCodeAppender {

        /**
         * The prefix to use.
         */
        private final String prefix;

        /**
         * A token that is added between the prefix and the first field value.
         */
        private final String start;

        /**
         * A token that is added after the last field value.
         */
        private final String end;

        /**
         * A token that is added between two field values.
         */
        private final String separator;

        /**
         * A token that is added between a field's name and its value.
         */
        private final String definer;

        /**
         * The list of fields to include in the {@link Object#toString()} implementation.
         */
        private final List<? extends FieldDescription.InDefinedShape> fieldDescriptions;

        /**
         * Creates a new appender.
         *
         * @param prefix            The prefix to use.
         * @param start             A token that is added between the prefix and the first field value.
         * @param end               A token that is added after the last field value.
         * @param separator         A token that is added between two field values.
         * @param definer           A token that is added between a field's name and its value.
         * @param fieldDescriptions The list of fields to include in the {@link Object#toString()} implementation.
         */
        protected Appender(String prefix,
                           String start,
                           String end,
                           String separator,
                           String definer,
                           List<? extends FieldDescription.InDefinedShape> fieldDescriptions) {
            this.prefix = prefix;
            this.start = start;
            this.end = end;
            this.separator = separator;
            this.definer = definer;
            this.fieldDescriptions = fieldDescriptions;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            if (instrumentedMethod.isStatic()) {
                throw new IllegalStateException("toString method must not be static: " + instrumentedMethod);
            } else if (!instrumentedMethod.getReturnType().asErasure().isAssignableFrom(String.class)) {
                throw new IllegalStateException("toString method does not return String-compatible type: " + instrumentedMethod);
            }
            List<StackManipulation> stackManipulations = new ArrayList<StackManipulation>(Math.max(0, fieldDescriptions.size() * 7 - 2) + 10);
            stackManipulations.add(TypeCreation.of(TypeDescription.ForLoadedType.of(StringBuilder.class)));
            stackManipulations.add(Duplication.SINGLE);
            stackManipulations.add(new TextConstant(prefix));
            stackManipulations.add(MethodInvocation.invoke(STRING_BUILDER_CONSTRUCTOR));
            stackManipulations.add(new TextConstant(start));
            stackManipulations.add(ValueConsumer.STRING);
            boolean first = true;
            for (FieldDescription.InDefinedShape fieldDescription : fieldDescriptions) {
                if (first) {
                    first = false;
                } else {
                    stackManipulations.add(new TextConstant(separator));
                    stackManipulations.add(ValueConsumer.STRING);
                }
                stackManipulations.add(new TextConstant(fieldDescription.getName() + definer));
                stackManipulations.add(ValueConsumer.STRING);
                stackManipulations.add(MethodVariableAccess.loadThis());
                stackManipulations.add(FieldAccess.forField(fieldDescription).read());
                stackManipulations.add(ValueConsumer.of(fieldDescription.getType().asErasure()));
            }
            stackManipulations.add(new TextConstant(end));
            stackManipulations.add(ValueConsumer.STRING);
            stackManipulations.add(MethodInvocation.invoke(TO_STRING));
            stackManipulations.add(MethodReturn.REFERENCE);
            return new Size(new StackManipulation.Compound(stackManipulations).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
        }
    }

    /**
     * A prefix resolver is responsible for providing a value that is prepended to a {@link Object#toString()} implementation.
     */
    public interface PrefixResolver {

        /**
         * Resolves the prefixed value.
         *
         * @param instrumentedType The instrumented type.
         * @return The value to be prefixed.
         */
        String resolve(TypeDescription instrumentedType);

        /**
         * Default implementations for a prefix resolver.
         */
        enum Default implements PrefixResolver {

            /**
             * A prefix resolver for the instrumented type's fully qualified class name.
             */
            FULLY_QUALIFIED_CLASS_NAME {
                /** {@inheritDoc} */
                public String resolve(TypeDescription instrumentedType) {
                    return instrumentedType.getName();
                }
            },

            /**
             * A prefix resolver for the instrumented type's fully qualified class name.
             */
            CANONICAL_CLASS_NAME {
                /** {@inheritDoc} */
                public String resolve(TypeDescription instrumentedType) {
                    return instrumentedType.getCanonicalName();
                }
            },

            /**
             * A prefix resolver for the instrumented type's simple class name.
             */
            SIMPLE_CLASS_NAME {
                /** {@inheritDoc} */
                public String resolve(TypeDescription instrumentedType) {
                    return instrumentedType.getSimpleName();
                }
            }
        }

        /**
         * A prefix resolver that returns a fixed value.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForFixedValue implements PrefixResolver {

            /**
             * The prefix to prepend.
             */
            private final String prefix;

            /**
             * Creates a new prefix resolver that returns a fixed value.
             *
             * @param prefix The prefix to prepend.
             */
            protected ForFixedValue(String prefix) {
                this.prefix = prefix;
            }

            /**
             * {@inheritDoc}
             */
            public String resolve(TypeDescription instrumentedType) {
                return prefix;
            }
        }
    }

    /**
     * A value consumer that is responsible for adding a field value to the string creating {@link StringBuilder}.
     */
    protected enum ValueConsumer implements StackManipulation {

        /**
         * A value consumer for a {@code boolean} value.
         */
        BOOLEAN {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Z)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },

        /**
         * A value consumer for a {@code char} value.
         */
        CHARACTER {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },

        /**
         * A value consumer for an {@code int} value.
         */
        INTEGER {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },

        /**
         * A value consumer for a {@code long} value.
         */
        LONG {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A value consumer for a {@code float} value.
         */
        FLOAT {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(F)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },

        /**
         * A value consumer for a {@code double} value.
         */
        DOUBLE {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(D)Ljava/lang/StringBuilder;", false);
                return new Size(-1, 0);
            }
        },

        /**
         * A value consumer for a {@link String} value.
         */
        STRING {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },


        /**
         * A value consumer for a {@link CharSequence} value.
         */
        CHARACTER_SEQUENCE {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/CharSequence;)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },

        /**
         * A value consumer for a reference type.
         */
        OBJECT {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },

        /**
         * A value consumer for a {@code boolean} array type.
         */
        BOOLEAN_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", "([Z)Ljava/lang/String;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },

        /**
         * A value consumer for a {@code byte} array type.
         */
        BYTE_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", "([B)Ljava/lang/String;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },

        /**
         * A value consumer for a {@code short} array type.
         */
        SHORT_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", "([S)Ljava/lang/String;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },

        /**
         * A value consumer for a {@code char} array type.
         */
        CHARACTER_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", "([C)Ljava/lang/String;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },

        /**
         * A value consumer for an {@code int} array type.
         */
        INTEGER_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", "([I)Ljava/lang/String;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },

        /**
         * A value consumer for a {@code long} array type.
         */
        LONG_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", "([J)Ljava/lang/String;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },

        /**
         * A value consumer for a {@code float} array type.
         */
        FLOAT_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", "([F)Ljava/lang/String;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },

        /**
         * A value consumer for a {@code double} array type.
         */
        DOUBLE_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", "([D)Ljava/lang/String;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },

        /**
         * A value consumer for a reference array type.
         */
        REFERENCE_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", "([Ljava/lang/Object;)Ljava/lang/String;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        },

        /**
         * A value consumer for a nested array type.
         */
        NESTED_ARRAY {
            /** {@inheritDoc} */
            public Size apply(MethodVisitor methodVisitor, Context implementationContext) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "deepToString", "([Ljava/lang/Object;)Ljava/lang/String;", false);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                return new Size(0, 0);
            }
        };

        /**
         * Resolves an appropriate value resolver for a given type.
         *
         * @param typeDescription The type for which to resolve a value resolver.
         * @return An appropriate stack manipulation.
         */
        protected static StackManipulation of(TypeDescription typeDescription) {
            if (typeDescription.represents(boolean.class)) {
                return BOOLEAN;
            } else if (typeDescription.represents(char.class)) {
                return CHARACTER;
            } else if (typeDescription.represents(byte.class)
                    || typeDescription.represents(short.class)
                    || typeDescription.represents(int.class)) {
                return INTEGER;
            } else if (typeDescription.represents(long.class)) {
                return LONG;
            } else if (typeDescription.represents(float.class)) {
                return FLOAT;
            } else if (typeDescription.represents(double.class)) {
                return DOUBLE;
            } else if (typeDescription.represents(String.class)) {
                return STRING;
            } else if (typeDescription.isAssignableTo(CharSequence.class)) {
                return CHARACTER_SEQUENCE;
            } else if (typeDescription.represents(boolean[].class)) {
                return BOOLEAN_ARRAY;
            } else if (typeDescription.represents(byte[].class)) {
                return BYTE_ARRAY;
            } else if (typeDescription.represents(short[].class)) {
                return SHORT_ARRAY;
            } else if (typeDescription.represents(char[].class)) {
                return CHARACTER_ARRAY;
            } else if (typeDescription.represents(int[].class)) {
                return INTEGER_ARRAY;
            } else if (typeDescription.represents(long[].class)) {
                return LONG_ARRAY;
            } else if (typeDescription.represents(float[].class)) {
                return FLOAT_ARRAY;
            } else if (typeDescription.represents(double[].class)) {
                return DOUBLE_ARRAY;
            } else if (typeDescription.isArray()) {
                return typeDescription.getComponentType().isArray()
                        ? NESTED_ARRAY
                        : REFERENCE_ARRAY;
            } else {
                return OBJECT;
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return true;
        }
    }
}
