package net.bytebuddy.description.field;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.objectweb.asm.signature.SignatureWriter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Implementations of this interface describe a Java field. Implementations of this interface must provide meaningful
 * {@code equal(Object)} and {@code hashCode()} implementations.
 */
public interface FieldDescription extends ByteCodeElement, NamedElement.WithGenericName {

    GenericTypeDescription getType();

    Token asToken(ElementMatcher<? super TypeDescription> targetTypeMatcher);

    /**
     * An abstract base implementation of a field description.
     */
    abstract class AbstractFieldDescription extends AbstractModifierReviewable implements FieldDescription {

        @Override
        public String getInternalName() {
            return getName();
        }

        @Override
        public String getSourceCodeName() {
            return getName();
        }

        @Override
        public String getDescriptor() {
            return getType().asRawType().getDescriptor();
        }

        @Override
        public String getGenericSignature() {
            GenericTypeDescription fieldType = getType();
            return fieldType.getSort().isNonGeneric()
                    ? null
                    : fieldType.accept(new GenericTypeDescription.Visitor.ForSignatureVisitor(new SignatureWriter())).toString();
        }

        @Override
        public boolean isVisibleTo(TypeDescription typeDescription) {
            return getDeclaringType().isVisibleTo(typeDescription)
                    && (isPublic()
                    || typeDescription.equals(getDeclaringType())
                    || (isProtected() && getDeclaringType().isAssignableFrom(typeDescription))
                    || (!isPrivate() && typeDescription.isSamePackage(getDeclaringType())));
        }

        @Override
        public FieldDescription.Token asToken(ElementMatcher<? super TypeDescription> targetTypeMatcher) {
            return new FieldDescription.Token(getName(),
                    getModifiers(),
                    getType().accept(new GenericTypeDescription.Visitor.Substitutor.ForDetachment(targetTypeMatcher)),
                    getDeclaredAnnotations());
        }

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof FieldDescription
                    && getName().equals(((FieldDescription) other).getName())
                    && getDeclaringType().equals(((FieldDescription) other).getDeclaringType());
        }

        @Override
        public int hashCode() {
            return getDeclaringType().hashCode() + 31 * getName().hashCode();
        }

        @Override
        public String toGenericString() {
            StringBuilder stringBuilder = new StringBuilder();
            if (getModifiers() != EMPTY_MASK) {
                stringBuilder.append(Modifier.toString(getModifiers())).append(" ");
            }
            stringBuilder.append(getType().getSourceCodeName()).append(" ");
            stringBuilder.append(getDeclaringType().getSourceCodeName()).append(".");
            return stringBuilder.append(getName()).toString();
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            if (getModifiers() != EMPTY_MASK) {
                stringBuilder.append(Modifier.toString(getModifiers())).append(" ");
            }
            stringBuilder.append(getType().asRawType().getSourceCodeName()).append(" ");
            stringBuilder.append(getDeclaringType().getSourceCodeName()).append(".");
            return stringBuilder.append(getName()).toString();
        }
    }

    /**
     * An implementation of a field description for a loaded field.
     */
    class ForLoadedField extends AbstractFieldDescription {

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

        @Override
        public GenericTypeDescription getType() {
            return new GenericTypeDescription.LazyProjection.OfLoadedFieldType(field);
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotation(field.getDeclaredAnnotations());
        }

        @Override
        public String getName() {
            return field.getName();
        }

        @Override
        public TypeDescription getDeclaringType() {
            return new TypeDescription.ForLoadedType(field.getDeclaringClass());
        }

        @Override
        public int getModifiers() {
            return field.getModifiers();
        }

        @Override
        public boolean isSynthetic() {
            return field.isSynthetic();
        }
    }

    /**
     * A latent field description describes a field that is not attached to a declaring
     * {@link TypeDescription}.
     */
    class Latent extends AbstractFieldDescription {

        /**
         * The name of the field.
         */
        private final String fieldName;

        /**
         * The type for which this field is defined.
         */
        private final TypeDescription declaringType;

        /**
         * The type of the field.
         */
        private final GenericTypeDescription fieldType;

        /**
         * The field's modifiers.
         */
        private final int modifiers;

        private final List<? extends AnnotationDescription> declaredAnnotations;

        public Latent(TypeDescription declaringType, FieldDescription.Token token) {
            this(declaringType,
                    token.getName(),
                    token.getType(),
                    token.getModifiers(),
                    token.getAnnotations());
        }

        public Latent(TypeDescription declaringType,
                      String fieldName,
                      GenericTypeDescription fieldType,
                      int modifiers,
                      List<? extends AnnotationDescription> declaredAnnotations) {
            this.declaringType = declaringType;
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.modifiers = modifiers;
            this.declaredAnnotations = declaredAnnotations;
        }

        @Override
        public GenericTypeDescription getType() {
            return fieldType.accept(GenericTypeDescription.Visitor.Substitutor.ForAttachment.of(this));
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Explicit(declaredAnnotations);
        }

        @Override
        public String getName() {
            return fieldName;
        }

        @Override
        public TypeDescription getDeclaringType() {
            return declaringType;
        }

        @Override
        public int getModifiers() {
            return modifiers;
        }
    }

    class Token implements ByteCodeElement.Token<Token> {

        private final String name;

        private final int modifiers;

        private final GenericTypeDescription type;

        private final List<? extends AnnotationDescription> annotations;

        public Token(String name,
                     int modifiers,
                     GenericTypeDescription type,
                     List<? extends AnnotationDescription> annotations) {
            this.name = name;
            this.modifiers = modifiers;
            this.type = type;
            this.annotations = annotations;
        }

        public String getName() {
            return name;
        }

        public GenericTypeDescription getType() {
            return type;
        }

        public int getModifiers() {
            return modifiers;
        }

        public AnnotationList getAnnotations() {
            return new AnnotationList.Explicit(annotations);
        }

        @Override
        public Token accept(GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor) {
            return new Token(getName(),
                    getModifiers(),
                    getType().accept(visitor),
                    getAnnotations());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Token)) return false;
            Token token = (Token) other;
            return getName().equals(token.getName());
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        @Override
        public String toString() {
            return "FieldDescription.Token{" +
                    "name='" + name + '\'' +
                    ", modifiers=" + modifiers +
                    ", type=" + type +
                    ", annotations=" + annotations +
                    '}';
        }
    }
}
