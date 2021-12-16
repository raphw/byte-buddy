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
package net.bytebuddy.description.type;

import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.build.CachedReturnPlugin;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.DeclaredByType;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.objectweb.asm.signature.SignatureWriter;

import javax.annotation.Nonnull;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Represents a component of a Java record.
 */
public interface RecordComponentDescription extends DeclaredByType.WithMandatoryDeclaration,
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
        @Nonnull
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
        @MaybeNull
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
        public boolean equals(@MaybeNull Object other) {
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
         * A dispatcher for accessing {@code java.lang.RecordComponent} types.
         */
        protected static final RecordComponent RECORD_COMPONENT = doPrivileged(JavaDispatcher.of(RecordComponent.class));

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
         * Resolves an instance into a record component description.
         *
         * @param recordComponent The record component to represent.
         * @return A suitable description of the record component.
         */
        public static RecordComponentDescription of(Object recordComponent) {
            if (!RECORD_COMPONENT.isInstance(recordComponent)) {
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
            return new MethodDescription.ForLoadedMethod(RECORD_COMPONENT.getAccessor(recordComponent));
        }

        /**
         * {@inheritDoc}
         */
        @Nonnull
        public TypeDescription getDeclaringType() {
            return TypeDescription.ForLoadedType.of(RECORD_COMPONENT.getDeclaringRecord(recordComponent));
        }

        /**
         * {@inheritDoc}
         */
        public String getActualName() {
            return RECORD_COMPONENT.getName(recordComponent);
        }

        @Override
        @MaybeNull
        public String getGenericSignature() {
            return RECORD_COMPONENT.getGenericSignature(recordComponent);
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotations(recordComponent.getDeclaredAnnotations());
        }

        /**
         * A dispatcher for accessing methods of {@code java.lang.reflect.RecordComponent}.
         */
        @JavaDispatcher.Proxied("java.lang.reflect.RecordComponent")
        protected interface RecordComponent {

            /**
             * Checks if the supplied instance is a record component.
             *
             * @param instance The instance to evaluate.
             * @return {@code true} if the supplied instance is a record component.
             */
            @JavaDispatcher.Instance
            boolean isInstance(Object instance);

            /**
             * Resolves a record component's name.
             *
             * @param value The record component to resolve the name for.
             * @return The record component's name.
             */
            String getName(Object value);

            /**
             * Resolves a record component's declaring type.
             *
             * @param value The record component to resolve the declared type for.
             * @return The record component's declaring type.
             */
            Class<?> getDeclaringRecord(Object value);

            /**
             * Resolves a record component's accessor method.
             *
             * @param value The record component to resolve the accessor method for.
             * @return The record component's accessor method.
             */
            Method getAccessor(Object value);

            /**
             * Resolves a record component's type.
             *
             * @param value The record component to resolve the type for.
             * @return The record component's type.
             */
            Class<?> getType(Object value);

            /**
             * Resolves a record component's generic type.
             *
             * @param value The record component to resolve the generic type for.
             * @return The record component's generic type.
             */
            Type getGenericType(Object value);

            /**
             * Returns the record component type's generic signature.
             *
             * @param value The record component to resolve the generic signature for.
             * @return The record component type's generic signature or {@code null} if no signature is defined.
             */
            @MaybeNull
            String getGenericSignature(Object value);

            /**
             * Resolves a record component's annotated type.
             *
             * @param value The record component to resolve the annotated type for.
             * @return The record component's annotated type.
             */
            AnnotatedElement getAnnotatedType(Object value);
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
        @Nonnull
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
        @Nonnull
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
        public boolean equals(@MaybeNull Object other) {
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
