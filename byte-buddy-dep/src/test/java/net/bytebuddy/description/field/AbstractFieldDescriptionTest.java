package net.bytebuddy.description.field;

import com.sun.org.apache.bcel.internal.classfile.Synthetic;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.packaging.VisibilityFieldTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractFieldDescriptionTest {

    private Field first, second, genericField;

    protected abstract FieldDescription.InDefinedShape describe(Field field);

    @Before
    public void setUp() throws Exception {
        first = FirstSample.class.getDeclaredField("first");
        second = SecondSample.class.getDeclaredField("second");
        genericField = GenericField.class.getDeclaredField("foo");
    }

    @Test
    public void testPrecondition() throws Exception {
        assertThat(describe(first), not(describe(second)));
        assertThat(describe(first), is(describe(first)));
        assertThat(describe(second), is(describe(second)));
        assertThat(describe(first), is((FieldDescription) new FieldDescription.ForLoadedField(first)));
        assertThat(describe(second), is((FieldDescription) new FieldDescription.ForLoadedField(second)));
    }

    @Test
    public void testFieldType() throws Exception {
        assertThat(describe(first).getType(), is((TypeDefinition) new TypeDescription.ForLoadedType(first.getType())));
        assertThat(describe(second).getType(), is((TypeDefinition) new TypeDescription.ForLoadedType(second.getType())));
    }

    @Test
    public void testFieldName() throws Exception {
        assertThat(describe(first).getName(), is(first.getName()));
        assertThat(describe(second).getName(), is(second.getName()));
        assertThat(describe(first).getInternalName(), is(first.getName()));
        assertThat(describe(second).getInternalName(), is(second.getName()));
    }

    @Test
    public void testDescriptor() throws Exception {
        assertThat(describe(first).getDescriptor(), is(Type.getDescriptor(first.getType())));
        assertThat(describe(second).getDescriptor(), is(Type.getDescriptor(second.getType())));
    }

    @Test
    public void testFieldModifier() throws Exception {
        assertThat(describe(first).getModifiers(), is(first.getModifiers()));
        assertThat(describe(second).getModifiers(), is(second.getModifiers()));
    }

    @Test
    public void testFieldDeclaringType() throws Exception {
        assertThat(describe(first).getDeclaringType(), is((TypeDescription) new TypeDescription.ForLoadedType(first.getDeclaringClass())));
        assertThat(describe(second).getDeclaringType(), is((TypeDescription) new TypeDescription.ForLoadedType(second.getDeclaringClass())));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(describe(first).hashCode(), is(new TypeDescription.ForLoadedType(FirstSample.class).hashCode() + 31 * first.getName().hashCode()));
        assertThat(describe(second).hashCode(), is(new TypeDescription.ForLoadedType(SecondSample.class).hashCode() + 31 * second.getName().hashCode()));
        assertThat(describe(first).hashCode(), is(describe(first).hashCode()));
        assertThat(describe(second).hashCode(), is(describe(second).hashCode()));
        assertThat(describe(first).hashCode(), not(describe(second).hashCode()));
    }

    @Test
    public void testEquals() throws Exception {
        FieldDescription identical = describe(first);
        assertThat(identical, is(identical));
        FieldDescription equalFirst = mock(FieldDescription.class);
        when(equalFirst.getName()).thenReturn(first.getName());
        when(equalFirst.getDeclaringType()).thenReturn(new TypeDescription.ForLoadedType(FirstSample.class));
        assertThat(describe(first), is(equalFirst));
        FieldDescription equalSecond = mock(FieldDescription.class);
        when(equalSecond.getName()).thenReturn(second.getName());
        when(equalSecond.getDeclaringType()).thenReturn(new TypeDescription.ForLoadedType(SecondSample.class));
        assertThat(describe(second), is(equalSecond));
        FieldDescription equalFirstTypeOnly = mock(FieldDescription.class);
        when(equalFirstTypeOnly.getName()).thenReturn(second.getName());
        when(equalFirstTypeOnly.getDeclaringType()).thenReturn(new TypeDescription.ForLoadedType(FirstSample.class));
        assertThat(describe(first), not(equalFirstTypeOnly));
        FieldDescription equalFirstNameOnly = mock(FieldDescription.class);
        when(equalFirstNameOnly.getName()).thenReturn(first.getName());
        when(equalFirstNameOnly.getDeclaringType()).thenReturn(new TypeDescription.ForLoadedType(SecondSample.class));
        assertThat(describe(first), not(equalFirstNameOnly));
        assertThat(describe(first), not(equalSecond));
        assertThat(describe(first), not(new Object()));
        assertThat(describe(first), not(equalTo(null)));
    }

    @Test
    public void testToString() throws Exception {
        assertThat(describe(first).toString(), is(first.toString()));
        assertThat(describe(second).toString(), is(second.toString()));
    }

    @Test
    public void testSynthetic() throws Exception {
        assertThat(describe(first).isSynthetic(), is(first.isSynthetic()));
        assertThat(describe(second).isSynthetic(), is(second.isSynthetic()));
    }

    @Test
    public void testIsVisibleTo() throws Exception {
        assertThat(describe(PublicType.class.getDeclaredField("publicField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("protectedField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("packagePrivateField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("privateField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("publicField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(FirstSample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("protectedField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(FirstSample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("packagePrivateField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(FirstSample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("privateField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(FirstSample.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredField("publicField"))
                .isVisibleTo(TypeDescription.OBJECT), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("protectedField"))
                .isVisibleTo(TypeDescription.OBJECT), is(false));
        assertThat(describe(PublicType.class.getDeclaredField("packagePrivateField"))
                .isVisibleTo(TypeDescription.OBJECT), is(false));
        assertThat(describe(PublicType.class.getDeclaredField("privateField"))
                .isVisibleTo(TypeDescription.OBJECT), is(false));
        assertThat(describe(PublicType.class.getDeclaredField("publicField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(VisibilityFieldTestHelper.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("protectedField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(VisibilityFieldTestHelper.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("packagePrivateField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(VisibilityFieldTestHelper.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredField("privateField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(VisibilityFieldTestHelper.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredField("publicField"))
                .isVisibleTo(TypeDescription.OBJECT), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredField("protectedField"))
                .isVisibleTo(TypeDescription.OBJECT), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredField("packagePrivateField"))
                .isVisibleTo(TypeDescription.OBJECT), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredField("privateField"))
                .isVisibleTo(TypeDescription.OBJECT), is(false));
        assertThat(describe(PackagePrivateFieldType.class.getDeclaredField("packagePrivateType"))
                .isVisibleTo(new TypeDescription.ForLoadedType(PackagePrivateFieldType.class)), is(true));
        assertThat(describe(PackagePrivateFieldType.class.getDeclaredField("packagePrivateType"))
                .isVisibleTo(TypeDescription.OBJECT), is(true));
    }

    @Test
    public void testAnnotations() throws Exception {
        assertThat(describe(first).getDeclaredAnnotations(), is((AnnotationList) new AnnotationList.Empty()));
        assertThat(describe(second).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.ForLoadedAnnotations(second.getDeclaredAnnotations())));
    }

    @Test
    public void testGenericTypes() throws Exception {
        assertThat(describe(genericField).getType(), is(TypeDefinition.Sort.describe(genericField.getGenericType())));
        assertThat(describe(genericField).getType().asErasure(), is((TypeDescription) new TypeDescription.ForLoadedType(genericField.getType())));
    }

    @Test
    public void testToGenericString() throws Exception {
        assertThat(describe(genericField).toGenericString(), is(genericField.toGenericString()));
    }

    @Test
    public void testGetActualModifiers() throws Exception {
        assertThat(describe(first).getActualModifiers(), is(first.getModifiers()));
        assertThat(describe(DeprecationSample.class.getDeclaredField("foo")).getActualModifiers(), is(Opcodes.ACC_DEPRECATED | Opcodes.ACC_PRIVATE));
    }

    @Test
    public void testSyntheticField() throws Exception {
        assertThat(describe(SyntheticField.class.getDeclaredFields()[0]).getModifiers(), is(SyntheticField.class.getDeclaredFields()[0].getModifiers()));
        assertThat(describe(SyntheticField.class.getDeclaredFields()[0]).isSynthetic(), is(SyntheticField.class.getDeclaredFields()[0].isSynthetic()));
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface SampleAnnotation {

    }

    @SuppressWarnings("unused")
    protected static class FirstSample {

        private Void first;
    }

    @SuppressWarnings("unused")
    protected static class SecondSample {

        @SampleAnnotation
        int second;
    }

    @SuppressWarnings("unused")
    public static class PublicType {

        public Void publicField;

        protected Void protectedField;

        Void packagePrivateField;

        private Void privateField;
    }

    @SuppressWarnings("unused")
    static class PackagePrivateType {

        public Void publicField;

        protected Void protectedField;

        Void packagePrivateField;

        private Void privateField;
    }

    @SuppressWarnings("unused")
    static class GenericField {

        List<String> foo;
    }

    public static class PackagePrivateFieldType {

        public PackagePrivateType packagePrivateType;
    }

    private static class DeprecationSample {

        @Deprecated
        private Void foo;
    }

    private class SyntheticField {
        /* empty */
    }
}
