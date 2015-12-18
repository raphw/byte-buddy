package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Field;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * Represents a {@link Field} constant for a given type.
 */
public class FieldConstant implements StackManipulation {

    /**
     * The {@link Class#getDeclaredField(String)} method.
     */
    private static final MethodDescription.InDefinedShape GET_DECLARED_FIELD;

    /*
     * Looks up the method for finding a class's declared field.
     */
    static {
        GET_DECLARED_FIELD = new TypeDescription.ForLoadedType(Class.class).getDeclaredMethods()
                .filter(named("getDeclaredField").and(takesArguments(String.class)))
                .getOnly();
    }

    /**
     * The field to be represent as a {@link Field}.
     */
    private final FieldDescription.InDefinedShape fieldDescription;

    /**
     * Creates a new field constant.
     *
     * @param fieldDescription The field to be represent as a {@link Field}.
     */
    public FieldConstant(FieldDescription.InDefinedShape fieldDescription) {
        this.fieldDescription = fieldDescription;
    }

    /**
     * Retruns a cached version of this field constant.
     *
     * @return A cached version of this field constant.
     */
    public StackManipulation cached() {
        return new Cached(this);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        return new Compound(
                ClassConstant.of(fieldDescription.getDeclaringType()),
                new TextConstant(fieldDescription.getInternalName()),
                MethodInvocation.invoke(GET_DECLARED_FIELD)
        ).apply(methodVisitor, implementationContext);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && fieldDescription.equals(((FieldConstant) other).fieldDescription);
    }

    @Override
    public int hashCode() {
        return fieldDescription.hashCode();
    }

    @Override
    public String toString() {
        return "FieldConstant{" +
                "fieldDescription=" + fieldDescription +
                '}';
    }

    /**
     * A cached version of a {@link FieldConstant}.
     */
    protected static class Cached implements StackManipulation {

        /**
         * The field constant stack manipulation.
         */
        private final StackManipulation fieldConstant;

        /**
         * Creates a new cached version of a field constant.
         *
         * @param fieldConstant The field constant stack manipulation.
         */
        public Cached(StackManipulation fieldConstant) {
            this.fieldConstant = fieldConstant;
        }

        @Override
        public boolean isValid() {
            return fieldConstant.isValid();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return FieldAccess.forField(implementationContext.cache(fieldConstant, new TypeDescription.ForLoadedType(Field.class)))
                    .getter()
                    .apply(methodVisitor, implementationContext);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass()) && fieldConstant.equals(((Cached) other).fieldConstant);
        }

        @Override
        public int hashCode() {
            return fieldConstant.hashCode();
        }

        @Override
        public String toString() {
            return "FieldConstant.Cached{" +
                    "fieldConstant=" + fieldConstant +
                    '}';
        }
    }
}
