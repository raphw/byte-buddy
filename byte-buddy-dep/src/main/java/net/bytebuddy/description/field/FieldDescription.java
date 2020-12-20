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
package net.bytebuddy.description.field;

import net.bytebuddy.build.CachedReturnPlugin;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureWriter;

import java.lang.reflect.Field;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

/**
 * Implementations of this interface describe a Java field. Implementations of this interface must provide meaningful
 * {@code equal(Object)} and {@code hashCode()} implementations.
 */
public interface FieldDescription extends ByteCodeElement,
        ModifierReviewable.ForFieldDescription,
        NamedElement.WithGenericName,
        ByteCodeElement.TypeDependant<FieldDescription.InDefinedShape, FieldDescription.Token> {

    /**
     * A representative of a field's non-set default value.
     */
    Object NO_DEFAULT_VALUE = null;

    /**
     * Returns the type of the described field.
     *
     * @return The type of the described field.
     */
    TypeDescription.Generic getType();

    /**
     * Returns the field's actual modifiers as it is present in a class file, i.e. its modifiers including
     * a flag if this field is deprecated.
     *
     * @return The field's actual modifiers.
     */
    int getActualModifiers();

    /**
     * Returns a signature token representing this field.
     *
     * @return A signature token representing this field.
     */
    SignatureToken asSignatureToken();

    /**
     * Represents a field description in its generic shape, i.e. in the shape it is defined by a generic or raw type.
     */
    interface InGenericShape extends FieldDescription {

        /**
         * {@inheritDoc}
         */
        TypeDescription.Generic getDeclaringType();
    }

    /**
     * Represents a field in its defined shape, i.e. in the form it is defined by a class without its type variables being resolved.
     */
    interface InDefinedShape extends FieldDescription {

        /**
         * {@inheritDoc}
         */
        TypeDescription getDeclaringType();

        /**
         * An abstract base implementation of a field description in its defined shape.
         */
        abstract class AbstractBase extends FieldDescription.AbstractBase implements InDefinedShape {

            /**
             * {@inheritDoc}
             */
            public InDefinedShape asDefined() {
                return this;
            }
        }
    }

    /**
     * An abstract base implementation of a field description.
     */
    abstract class AbstractBase extends ModifierReviewable.AbstractBase implements FieldDescription {

        /**
         * {@inheritDoc}
         */
        public String getInternalName() {
            return getName();
        }

        /**
         * {@inheritDoc}
         */
        public String getActualName() {
            return getName();
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
            TypeDescription.Generic fieldType = getType();
            try {
                return fieldType.getSort().isNonGeneric()
                        ? NON_GENERIC_SIGNATURE
                        : fieldType.accept(new TypeDescription.Generic.Visitor.ForSignatureVisitor(new SignatureWriter())).toString();
            } catch (GenericSignatureFormatError ignored) {
                return NON_GENERIC_SIGNATURE;
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean isVisibleTo(TypeDescription typeDescription) {
            return getDeclaringType().asErasure().isVisibleTo(typeDescription)
                    && (isPublic()
                    || typeDescription.equals(getDeclaringType().asErasure())
                    || isProtected() && getDeclaringType().asErasure().isAssignableFrom(typeDescription)
                    || !isPrivate() && typeDescription.isSamePackage(getDeclaringType().asErasure())
                    || isPrivate() && typeDescription.isNestMateOf(getDeclaringType().asErasure()));
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAccessibleTo(TypeDescription typeDescription) {
            return isPublic()
                    || typeDescription.equals(getDeclaringType().asErasure())
                    || !isPrivate() && typeDescription.isSamePackage(getDeclaringType().asErasure())
                    || isPrivate() && typeDescription.isNestMateOf(getDeclaringType().asErasure());
        }

        /**
         * {@inheritDoc}
         */
        public int getActualModifiers() {
            return getModifiers() | (getDeclaredAnnotations().isAnnotationPresent(Deprecated.class)
                    ? Opcodes.ACC_DEPRECATED
                    : EMPTY_MASK);
        }

        /**
         * {@inheritDoc}
         */
        public FieldDescription.Token asToken(ElementMatcher<? super TypeDescription> matcher) {
            return new FieldDescription.Token(getName(),
                    getModifiers(),
                    getType().accept(new TypeDescription.Generic.Visitor.Substitutor.ForDetachment(matcher)),
                    getDeclaredAnnotations());
        }

        /**
         * {@inheritDoc}
         */
        public SignatureToken asSignatureToken() {
            return new SignatureToken(getInternalName(), getType().asErasure());
        }

        @Override
        @CachedReturnPlugin.Enhance("hashCode")
        public int hashCode() {
            return getDeclaringType().hashCode() + 31 * (17 + getName().hashCode());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (!(other instanceof FieldDescription)) {
                return false;
            }
            FieldDescription fieldDescription = (FieldDescription) other;
            return getName().equals(fieldDescription.getName()) && getDeclaringType().equals(fieldDescription.getDeclaringType());
        }

        /**
         * {@inheritDoc}
         */
        public String toGenericString() {
            StringBuilder stringBuilder = new StringBuilder();
            if (getModifiers() != EMPTY_MASK) {
                stringBuilder.append(Modifier.toString(getModifiers())).append(' ');
            }
            stringBuilder.append(getType().getActualName()).append(' ');
            stringBuilder.append(getDeclaringType().asErasure().getActualName()).append('.');
            return stringBuilder.append(getName()).toString();
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            if (getModifiers() != EMPTY_MASK) {
                stringBuilder.append(Modifier.toString(getModifiers())).append(' ');
            }
            stringBuilder.append(getType().asErasure().getActualName()).append(' ');
            stringBuilder.append(getDeclaringType().asErasure().getActualName()).append('.');
            return stringBuilder.append(getName()).toString();
        }
    }

    /**
     * An implementation of a field description for a loaded field.
     */
    class ForLoadedField extends InDefinedShape.AbstractBase {

        /**
         * The represented loaded field.
         */
        private final Field field;

        /**
         * Creates an immutable field description for a loaded field.
         *
         * @param field The represented field.
         */
        public ForLoadedField(Field field) {
            this.field = field;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription.Generic getType() {
            if (TypeDescription.AbstractBase.RAW_TYPES) {
                return TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(field.getType());
            }
            return new TypeDescription.Generic.LazyProjection.ForLoadedFieldType(field);
        }

        /**
         * {@inheritDoc}
         */
        @CachedReturnPlugin.Enhance("declaredAnnotations")
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotations(field.getDeclaredAnnotations());
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return field.getName();
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getDeclaringType() {
            return TypeDescription.ForLoadedType.of(field.getDeclaringClass());
        }

        /**
         * {@inheritDoc}
         */
        public int getModifiers() {
            return field.getModifiers();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isSynthetic() {
            return field.isSynthetic();
        }
    }

    /**
     * A latent field description describes a field that is not attached to a declaring
     * {@link TypeDescription}.
     */
    class Latent extends InDefinedShape.AbstractBase {

        /**
         * The type for which this field is defined.
         */
        private final TypeDescription declaringType;

        /**
         * The name of the field.
         */
        private final String name;

        /**
         * The field's modifiers.
         */
        private final int modifiers;

        /**
         * The type of the field.
         */
        private final TypeDescription.Generic fieldType;

        /**
         * The annotations of this field.
         */
        private final List<? extends AnnotationDescription> declaredAnnotations;

        /**
         * Creates a new latent field description. All provided types are attached to this instance before they are returned.
         *
         * @param declaringType The declaring type of the field.
         * @param token         A token representing the field's shape.
         */
        public Latent(TypeDescription declaringType, FieldDescription.Token token) {
            this(declaringType,
                    token.getName(),
                    token.getModifiers(),
                    token.getType(),
                    token.getAnnotations());
        }

        /**
         * Creates a new latent field description. All provided types are attached to this instance before they are returned.
         *
         * @param declaringType       The declaring type of the field.
         * @param name           The name of the field.
         * @param fieldType           The field's modifiers.
         * @param modifiers           The type of the field.
         * @param declaredAnnotations The annotations of this field.
         */
        public Latent(TypeDescription declaringType,
                      String name,
                      int modifiers,
                      TypeDescription.Generic fieldType,
                      List<? extends AnnotationDescription> declaredAnnotations) {
            this.declaringType = declaringType;
            this.name = name;
            this.modifiers = modifiers;
            this.fieldType = fieldType;
            this.declaredAnnotations = declaredAnnotations;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription.Generic getType() {
            return fieldType.accept(TypeDescription.Generic.Visitor.Substitutor.ForAttachment.of(this));
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Explicit(declaredAnnotations);
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return name;
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
        public int getModifiers() {
            return modifiers;
        }
    }

    /**
     * A field description that represents a given field but with a substituted field type.
     */
    class TypeSubstituting extends AbstractBase implements InGenericShape {

        /**
         * The declaring type of the field.
         */
        private final TypeDescription.Generic declaringType;

        /**
         * The represented field.
         */
        private final FieldDescription fieldDescription;

        /**
         * A visitor that is applied to the field type.
         */
        private final TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor;

        /**
         * Creates a field description with a substituted field type.
         *
         * @param declaringType    The declaring type of the field.
         * @param fieldDescription The represented field.
         * @param visitor          A visitor that is applied to the field type.
         */
        public TypeSubstituting(TypeDescription.Generic declaringType,
                                FieldDescription fieldDescription,
                                TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
            this.declaringType = declaringType;
            this.fieldDescription = fieldDescription;
            this.visitor = visitor;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription.Generic getType() {
            return fieldDescription.getType().accept(visitor);
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return fieldDescription.getDeclaredAnnotations();
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription.Generic getDeclaringType() {
            return declaringType;
        }

        /**
         * {@inheritDoc}
         */
        public int getModifiers() {
            return fieldDescription.getModifiers();
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return fieldDescription.getName();
        }

        /**
         * {@inheritDoc}
         */
        public InDefinedShape asDefined() {
            return fieldDescription.asDefined();
        }
    }

    /**
     * A token representing a field's properties detached from a type.
     */
    class Token implements ByteCodeElement.Token<Token> {

        /**
         * The name of the represented field.
         */
        private final String name;

        /**
         * The modifiers of the represented field.
         */
        private final int modifiers;

        /**
         * The type of the represented field.
         */
        private final TypeDescription.Generic type;

        /**
         * The annotations of the represented field.
         */
        private final List<? extends AnnotationDescription> annotations;

        /**
         * Creates a new field token without annotations. The field type must be represented in its detached form.
         *
         * @param name      The name of the represented field.
         * @param modifiers The modifiers of the represented field.
         * @param type      The type of the represented field.
         */
        public Token(String name, int modifiers, TypeDescription.Generic type) {
            this(name, modifiers, type, Collections.<AnnotationDescription>emptyList());
        }

        /**
         * Creates a new field token. The field type must be represented in its detached form.
         *
         * @param name        The name of the represented field.
         * @param modifiers   The modifiers of the represented field.
         * @param type        The type of the represented field.
         * @param annotations The annotations of the represented field.
         */
        public Token(String name, int modifiers, TypeDescription.Generic type, List<? extends AnnotationDescription> annotations) {
            this.name = name;
            this.modifiers = modifiers;
            this.type = type;
            this.annotations = annotations;
        }

        /**
         * Returns the name of the represented field.
         *
         * @return The name of the represented field.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the type of the represented field.
         *
         * @return The type of the represented field.
         */
        public TypeDescription.Generic getType() {
            return type;
        }

        /**
         * Returns the modifiers of the represented field.
         *
         * @return The modifiers of the represented field.
         */
        public int getModifiers() {
            return modifiers;
        }

        /**
         * Returns the annotations of the represented field.
         *
         * @return The annotations of the represented field.
         */
        public AnnotationList getAnnotations() {
            return new AnnotationList.Explicit(annotations);
        }

        /**
         * {@inheritDoc}
         */
        public Token accept(TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
            return new Token(name,
                    modifiers,
                    type.accept(visitor),
                    annotations);
        }

        /**
         * Creates a signature token that represents the method that is represented by this token.
         *
         * @param declaringType The declaring type of the field that this token represents.
         * @return A signature token representing this token.
         */
        public SignatureToken asSignatureToken(TypeDescription declaringType) {
            return new SignatureToken(name, type.accept(new TypeDescription.Generic.Visitor.Reducing(declaringType)));
        }

        @Override
        @CachedReturnPlugin.Enhance("hashCode")
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + modifiers;
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
            Token token = (Token) other;
            return modifiers == token.modifiers
                    && name.equals(token.name)
                    && type.equals(token.type)
                    && annotations.equals(token.annotations);
        }
    }

    /**
     * A token that uniquely identifies a field by its name and type erasure.
     */
    class SignatureToken {

        /**
         * The field's name.
         */
        private final String name;

        /**
         * The field's raw type.
         */
        private final TypeDescription type;

        /**
         * Creates a new signature token.
         *
         * @param name The field's name.
         * @param type The field's raw type.
         */
        public SignatureToken(String name, TypeDescription type) {
            this.name = name;
            this.type = type;
        }

        /**
         * Returns the name of the represented field.
         *
         * @return The name of the represented field.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the type of the represented field.
         *
         * @return The type of the represented field.
         */
        public TypeDescription getType() {
            return type;
        }

        @Override
        @CachedReturnPlugin.Enhance("hashCode")
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + type.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (!(other instanceof SignatureToken)) {
                return false;
            }
            SignatureToken signatureToken = (SignatureToken) other;
            return name.equals(signatureToken.name) && type.equals(signatureToken.type);
        }

        @Override
        public String toString() {
            return type + " " + name;
        }
    }
}
