/*
 * Copyright 2014 - 2020 Rafael Winterhalter
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
package net.bytebuddy.description.type;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.CachedReturnPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.DeclaredByType;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.objectweb.asm.signature.SignatureWriter;

import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Represents a component of a Java record.
 */
public interface RecordComponentDescription extends DeclaredByType,
        NamedElement.WithDescriptor,
        AnnotationSource,
        ByteCodeElement.TypeDependant<RecordComponentDescription.InDefinedShape, RecordComponentDescription.Token> {

    /**
     * Returns the type of the record.
     *
     * @return The type of the record.
     */
    TypeDescription.Generic getType();

    /**
     * Returns the accessor for this record component.
     *
     * @return The accessor for this record component.
     */
    MethodDescription getAccessor();

    /**
     * Resolves this record component to a token where all types are detached.
     *
     * @param matcher The matcher to apply for detachment.
     * @return An appropriate token.
     */
    Token asToken(ElementMatcher<? super TypeDescription> matcher);

    /**
     * A description of a record component in generic shape.
     */
    interface InGenericShape extends RecordComponentDescription {

        /**
         * {@inheritDoc}
         */
        MethodDescription.InGenericShape getAccessor();
    }

    /**
     * A description of a record component in its defined shape.
     */
    interface InDefinedShape extends RecordComponentDescription {

        /**
         * {@inheritDoc}
         */
        MethodDescription.InDefinedShape getAccessor();

        /**
         * {@inheritDoc}
         */
        TypeDescription getDeclaringType();

        /**
         * An abstract base implementation of a record component description in its defined shape.
         */
        abstract class AbstractBase extends RecordComponentDescription.AbstractBase implements InDefinedShape {

            /**
             * {@inheritDoc}
             */
            public MethodDescription.InDefinedShape getAccessor() {
                return getDeclaringType().getDeclaredMethods().filter(named(getActualName())).getOnly();
            }

            /**
             * {@inheritDoc}
             */
            public InDefinedShape asDefined() {
                return this;
            }
        }
    }

    /**
     * An abstract base implementation for a record component description.
     */
    abstract class AbstractBase implements RecordComponentDescription {

        /**
         * {@inheritDoc}
         */
        public Token asToken(ElementMatcher<? super TypeDescription> matcher) {
            return new Token(getActualName(),
                    getType().accept(new TypeDescription.Generic.Visitor.Substitutor.ForDetachment(matcher)),
                    getDeclaredAnnotations());
        }

        /**
         * {@inheritDoc}
         */
        public String getDescriptor() {
            return getType().asErasure().getDescriptor();
        }

        /**
         * {@inheritDoc}
         */
        public String getGenericSignature() {
            TypeDescription.Generic recordComponentType = getType();
            try {
                return recordComponentType.getSort().isNonGeneric()
                        ? NON_GENERIC_SIGNATURE
                        : recordComponentType.accept(new TypeDescription.Generic.Visitor.ForSignatureVisitor(new SignatureWriter())).toString();
            } catch (GenericSignatureFormatError ignored) {
                return NON_GENERIC_SIGNATURE;
            }
        }

        @Override
        public int hashCode() {
            return getActualName().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (!(other instanceof RecordComponentDescription)) {
                return false;
            }
            RecordComponentDescription recordComponentDescription = (RecordComponentDescription) other;
            return getActualName().equals(recordComponentDescription.getActualName());
        }

        @Override
        public String toString() {
            return getType().getTypeName() + " " + getActualName();
        }
    }

    /**
     * Represents a loaded record component.
     */
    class ForLoadedRecordComponent extends InDefinedShape.AbstractBase {

        /**
         * The dispatcher to use.
         */
        protected static final Dispatcher DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

        /**
         * The represented record component.
         */
        private final AnnotatedElement recordComponent;

        /**
         * Creates a new representation of a loaded record component.
         *
         * @param recordComponent The represented record component.
         */
        protected ForLoadedRecordComponent(AnnotatedElement recordComponent) {
            this.recordComponent = recordComponent;
        }

        /**
         * Resolves an instance into a record component description.
         *
         * @param recordComponent The record component to represent.
         * @return A suitable description of the record component.
         */
        public static RecordComponentDescription of(Object recordComponent) {
            if (!DISPATCHER.isInstance(recordComponent)) {
                throw new IllegalArgumentException("Not a record component: " + recordComponent);
            }
            return new ForLoadedRecordComponent((AnnotatedElement) recordComponent);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription.Generic getType() {
            return new TypeDescription.Generic.LazyProjection.OfRecordComponent(recordComponent);
        }

        @Override
        public MethodDescription.InDefinedShape getAccessor() {
            return new MethodDescription.ForLoadedMethod(DISPATCHER.getAccessor(recordComponent));
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getDeclaringType() {
            return TypeDescription.ForLoadedType.of(DISPATCHER.getDeclaringType(recordComponent));
        }

        /**
         * {@inheritDoc}
         */
        public String getActualName() {
            return DISPATCHER.getName(recordComponent);
        }

        @Override
        public String getGenericSignature() {
            return DISPATCHER.getGenericSignature(recordComponent);
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotations(recordComponent.getDeclaredAnnotations());
        }

        /**
         * A dispatcher for resolving a {@code java.lang.reflect.RecordComponent}.
         */
        protected interface Dispatcher {

            /**
             * Checks if the supplied instance is a record component.
             *
             * @param instance The instance to evaluate.
             * @return {@code true} if the supplied instance is a record component.
             */
            boolean isInstance(Object instance);

            /**
             * Resolves a type's record components.
             *
             * @param type The type for which to read the record components.
             * @return An array of all declared record components.
             */
            Object[] getRecordComponents(Class<?> type);

            /**
             * Checks if the supplied type is a record.
             *
             * @param type The type to resolve.
             * @return {@code true} if the supplied type is a record.
             */
            boolean isRecord(Class<?> type);

            /**
             * Resolves a record component's name.
             *
             * @param recordComponent The record component to resolve the name for.
             * @return The record component's name.
             */
            String getName(Object recordComponent);

            /**
             * Resolves a record component's declaring type.
             *
             * @param recordComponent The record component to resolve the declared type for.
             * @return The record component's declaring type.
             */
            Class<?> getDeclaringType(Object recordComponent);

            /**
             * Resolves a record component's accessor method.
             *
             * @param recordComponent The record component to resolve the accessor method for.
             * @return The record component's accessor method.
             */
            Method getAccessor(Object recordComponent);

            /**
             * Resolves a record component's type.
             *
             * @param recordComponent The record component to resolve the type for.
             * @return The record component's type.
             */
            Class<?> getType(Object recordComponent);

            /**
             * Resolves a record component's generic type.
             *
             * @param recordComponent The record component to resolve the generic type for.
             * @return The record component's generic type.
             */
            Type getGenericType(Object recordComponent);

            /**
             * Returns the record component type's generic signature.
             *
             * @param recordComponent The record component to resolve the generic signature for.
             * @return The record component type's generic signature or {@code null} if no signature is defined.
             */
            String getGenericSignature(Object recordComponent);

            /**
             * Resolves a record component's annotated type.
             *
             * @param recordComponent The record component to resolve the annotated type for.
             * @return The record component's annotated type.
             */
            AnnotatedElement getAnnotatedType(Object recordComponent);

            /**
             * A creation action for creating a dispatcher.
             */
            enum CreationAction implements PrivilegedAction<Dispatcher> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher run() {
                    try {
                        Class<?> recordComponent = Class.forName("java.lang.reflect.RecordComponent");
                        return new ForJava14CapableVm(recordComponent,
                                Class.class.getMethod("getRecordComponents"),
                                Class.class.getMethod("isRecord"),
                                recordComponent.getMethod("getName"),
                                recordComponent.getMethod("getDeclaringRecord"),
                                recordComponent.getMethod("getAccessor"),
                                recordComponent.getMethod("getType"),
                                recordComponent.getMethod("getGenericType"),
                                recordComponent.getMethod("getGenericSignature"),
                                recordComponent.getMethod("getAnnotatedType"));
                    } catch (ClassNotFoundException ignored) {
                        return ForLegacyVm.INSTANCE;
                    } catch (NoSuchMethodException ignored) {
                        return ForLegacyVm.INSTANCE;
                    }
                }
            }

            /**
             * A dispatcher for a legacy VM that does not support records.
             */
            enum ForLegacyVm implements Dispatcher {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public boolean isInstance(Object instance) {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                @SuppressFBWarnings(value = "PZLA_PREFER_ZERO_LENGTH_ARRAYS", justification = "Null value return is aligned with OpenJDK return value.")
                public Object[] getRecordComponents(Class<?> type) {
                    return null;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isRecord(Class<?> type) {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public String getName(Object recordComponent) {
                    throw new IllegalStateException("The current VM does not support record components");
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> getDeclaringType(Object recordComponent) {
                    throw new IllegalStateException("The current VM does not support record components");
                }

                /**
                 * {@inheritDoc}
                 */
                public Method getAccessor(Object recordComponent) {
                    throw new IllegalStateException("The current VM does not support record components");
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> getType(Object recordComponent) {
                    throw new IllegalStateException("The current VM does not support record components");
                }

                /**
                 * {@inheritDoc}
                 */
                public Type getGenericType(Object recordComponent) {
                    throw new IllegalStateException("The current VM does not support record components");
                }

                /**
                 * {@inheritDoc}
                 */
                public String getGenericSignature(Object recordComponent) {
                    throw new IllegalStateException("The current VM does not support record components");
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotatedElement getAnnotatedType(Object recordComponent) {
                    throw new IllegalStateException("The current VM does not support record components");
                }
            }

            /**
             * A dispatcher for a Java 14-capable JVM.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForJava14CapableVm implements Dispatcher {

                /**
                 * The {@code java.lang.reflect.RecordComponent} type.
                 */
                private final Class<?> recordComponent;

                /**
                 * The {@code java.lang.Class#getRecordComponents()} method.
                 */
                private final Method getRecordComponents;

                /**
                 * The {@code java.lang.Class#isRecord()} method.
                 */
                private final Method isRecord;

                /**
                 * The {@code java.lang.reflect.RecordComponent#getName()} method.
                 */
                private final Method getName;

                /**
                 * The {@code java.lang.reflect.RecordComponent#getDeclaringType()} method.
                 */
                private final Method getDeclaringType;

                /**
                 * The {@code java.lang.reflect.RecordComponent#getAccessor()} method.
                 */
                private final Method getAccessor;

                /**
                 * The {@code java.lang.reflect.RecordComponent#getType()} method.
                 */
                private final Method getType;

                /**
                 * The {@code java.lang.reflect.RecordComponent#getGenericType()} method.
                 */
                private final Method getGenericType;

                /**
                 * The {@code java.lang.reflect.RecordComponent#getGenericSignature()} method.
                 */
                private final Method getGenericSignature;

                /**
                 * The {@code java.lang.reflect.RecordComponent#getAnnotatedType()} method.
                 */
                private final Method getAnnotatedType;

                /**
                 * Creates a dispatcher for a Java 14 capable VM.
                 *
                 * @param recordComponent     The {@code java.lang.reflect.RecordComponent} type.
                 * @param getRecordComponents The {@code java.lang.Class#getRecordComponents()} method.
                 * @param isRecord            The {@code java.lang.Class#isRecord()} method.
                 * @param getName             The {@code java.lang.reflect.RecordComponent#getName()} method.
                 * @param getDeclaringType    The {@code java.lang.reflect.RecordComponent#getDeclaringType()} method.
                 * @param getAccessor         The {@code java.lang.reflect.RecordComponent#getAccessor()} method.
                 * @param getType             The {@code java.lang.reflect.RecordComponent#getType()} method.
                 * @param getGenericType      The {@code java.lang.reflect.RecordComponent#getGenericType()} method.
                 * @param getGenericSignature The {@code java.lang.reflect.RecordComponent#getGenericSignature()} method.
                 * @param getAnnotatedType    The {@code java.lang.reflect.RecordComponent#getAnnotatedType()} method.
                 */
                protected ForJava14CapableVm(Class<?> recordComponent,
                                             Method getRecordComponents,
                                             Method isRecord,
                                             Method getName,
                                             Method getDeclaringType,
                                             Method getAccessor,
                                             Method getType,
                                             Method getGenericType,
                                             Method getGenericSignature,
                                             Method getAnnotatedType) {
                    this.recordComponent = recordComponent;
                    this.getRecordComponents = getRecordComponents;
                    this.isRecord = isRecord;
                    this.getName = getName;
                    this.getDeclaringType = getDeclaringType;
                    this.getAccessor = getAccessor;
                    this.getType = getType;
                    this.getGenericType = getGenericType;
                    this.getGenericSignature = getGenericSignature;
                    this.getAnnotatedType = getAnnotatedType;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isInstance(Object instance) {
                    return recordComponent.isInstance(instance);
                }

                /**
                 * {@inheritDoc}
                 */
                public Object[] getRecordComponents(Class<?> type) {
                    try {
                        return (Object[]) getRecordComponents.invoke(type);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.Class#getRecordComponents", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.Class#getRecordComponents", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isRecord(Class<?> type) {
                    try {
                        return (Boolean) isRecord.invoke(type);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.Class#isRecord", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.Class#isRecord", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public String getName(Object recordComponent) {
                    try {
                        return (String) getName.invoke(recordComponent);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflection.RecordComponent#getName", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflection.RecordComponent#getName", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> getDeclaringType(Object recordComponent) {
                    try {
                        return (Class<?>) getDeclaringType.invoke(recordComponent);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflection.RecordComponent#getDeclaringType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflection.RecordComponent#getDeclaringType", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Method getAccessor(Object recordComponent) {
                    try {
                        return (Method) getAccessor.invoke(recordComponent);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflection.RecordComponent#getAccessor", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflection.RecordComponent#getAccessor", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> getType(Object recordComponent) {
                    try {
                        return (Class<?>) getType.invoke(recordComponent);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflection.RecordComponent#getType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflection.RecordComponent#getType", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Type getGenericType(Object recordComponent) {
                    try {
                        return (Type) getGenericType.invoke(recordComponent);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflection.RecordComponent#getGenericType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflection.RecordComponent#getGenericType", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public String getGenericSignature(Object recordComponent) {
                    try {
                        return (String) getGenericSignature.invoke(recordComponent);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflection.RecordComponent#getGenericSignature", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflection.RecordComponent#getGenericSignature", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotatedElement getAnnotatedType(Object recordComponent) {
                    try {
                        return (AnnotatedElement) getAnnotatedType.invoke(recordComponent);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflection.RecordComponent#getAnnotatedType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflection.RecordComponent#getAnnotatedType", exception.getCause());
                    }
                }
            }
        }
    }

    /**
     * A latent record component description.
     */
    class Latent extends InDefinedShape.AbstractBase {

        /**
         * The record component's declaring type.
         */
        private final TypeDescription declaringType;

        /**
         * The record component's name.
         */
        private final String name;

        /**
         * The record component's type.
         */
        private final TypeDescription.Generic type;

        /**
         * The record component's annotations.
         */
        private final List<? extends AnnotationDescription> annotations;

        /**
         * Creates a new latent record component.
         *
         * @param declaringType The record component's declaring type.
         * @param token         The token representing the record component's detached properties.
         */
        public Latent(TypeDescription declaringType, Token token) {
            this(declaringType,
                    token.getName(),
                    token.getType(),
                    token.getAnnotations());
        }

        /**
         * Creates a new latent record component.
         *
         * @param declaringType The record component's declaring type-
         * @param name          The record component's name.
         * @param type          The record component's type.
         * @param annotations   The record component's annotations.
         */
        public Latent(TypeDescription declaringType, String name, TypeDescription.Generic type, List<? extends AnnotationDescription> annotations) {
            this.declaringType = declaringType;
            this.name = name;
            this.type = type;
            this.annotations = annotations;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription.Generic getType() {
            return type.accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(this));
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getDeclaringType() {
            return declaringType;
        }

        /**
         * {@inheritDoc}
         */
        public String getActualName() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Explicit(annotations);
        }
    }

    /**
     * A type substituting representation of a record component description.
     */
    class TypeSubstituting extends AbstractBase implements InGenericShape {

        /**
         * The type that declares this type-substituted record component.
         */
        private final TypeDescription.Generic declaringType;

        /**
         * The represented record component.
         */
        private final RecordComponentDescription recordComponentDescription;

        /**
         * A visitor that is applied to the parameter type.
         */
        private final TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor;

        /**
         * Creates a new type substituting representation of a record component description.
         *
         * @param declaringType              The type that declares this type-substituted record component.
         * @param recordComponentDescription The represented record component.
         * @param visitor                    A visitor that is applied to the parameter type.
         */
        public TypeSubstituting(TypeDescription.Generic declaringType,
                                RecordComponentDescription recordComponentDescription,
                                TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
            this.declaringType = declaringType;
            this.recordComponentDescription = recordComponentDescription;
            this.visitor = visitor;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDescription.InGenericShape getAccessor() {
            return declaringType.getDeclaredMethods().filter(named(getActualName())).getOnly();
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription.Generic getType() {
            return recordComponentDescription.getType().accept(visitor);
        }

        /**
         * {@inheritDoc}
         */
        public InDefinedShape asDefined() {
            return recordComponentDescription.asDefined();
        }

        /**
         * {@inheritDoc}
         */
        public TypeDefinition getDeclaringType() {
            return declaringType;
        }

        /**
         * {@inheritDoc}
         */
        public String getActualName() {
            return recordComponentDescription.getActualName();
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return recordComponentDescription.getDeclaredAnnotations();
        }
    }

    /**
     * A token representing a record component's properties detached from a type.
     */
    class Token implements ByteCodeElement.Token<Token> {

        /**
         * The token's name.
         */
        private final String name;

        /**
         * The token's type.
         */
        private final TypeDescription.Generic type;

        /**
         * The token's annotations.
         */
        private final List<? extends AnnotationDescription> annotations;

        /**
         * Creates a new record component token without annotations.
         *
         * @param name The token's name.
         * @param type The token's type.
         */
        public Token(String name, TypeDescription.Generic type) {
            this(name, type, Collections.<AnnotationDescription>emptyList());
        }

        /**
         * Creates a new record component token.
         *
         * @param name        The token's name.
         * @param type        The token's type.
         * @param annotations The token's annotations.
         */
        public Token(String name, TypeDescription.Generic type, List<? extends AnnotationDescription> annotations) {
            this.name = name;
            this.type = type;
            this.annotations = annotations;
        }

        /**
         * Returns the token's name.
         *
         * @return The token's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the token's type.
         *
         * @return The token's type.
         */
        public TypeDescription.Generic getType() {
            return type;
        }

        /**
         * Returns the token's annotations.
         *
         * @return The token's annotations.
         */
        public AnnotationList getAnnotations() {
            return new AnnotationList.Explicit(annotations);
        }

        /**
         * {@inheritDoc}
         */
        public Token accept(TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
            return new Token(name, type.accept(visitor), annotations);
        }

        @Override
        @CachedReturnPlugin.Enhance("hashCode")
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + type.hashCode();
            result = 31 * result + annotations.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other == null || getClass() != other.getClass()) {
                return false;
            }
            RecordComponentDescription.Token token = (RecordComponentDescription.Token) other;
            return name.equals(token.name)
                    && type.equals(token.type)
                    && annotations.equals(token.annotations);
        }
    }
}
