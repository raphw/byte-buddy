package net.bytebuddy.description.field;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.packaging.FieldDescriptionTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
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
        assertThat(describe(first).getType(), is((TypeDefinition) TypeDescription.ForLoadedType.of(first.getType())));
        assertThat(describe(second).getType(), is((TypeDefinition) TypeDescription.ForLoadedType.of(second.getType())));
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
    @SuppressWarnings("cast")
    public void testFieldDeclaringType() throws Exception {
        assertThat(describe(first).getDeclaringType(), is((TypeDescription) TypeDescription.ForLoadedType.of(first.getDeclaringClass())));
        assertThat(describe(second).getDeclaringType(), is((TypeDescription) TypeDescription.ForLoadedType.of(second.getDeclaringClass())));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(describe(first).hashCode(), is(TypeDescription.ForLoadedType.of(FirstSample.class).hashCode() + 31 * (17 + first.getName().hashCode())));
        assertThat(describe(second).hashCode(), is(TypeDescription.ForLoadedType.of(SecondSample.class).hashCode() + 31 * (17 + second.getName().hashCode())));
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
        when(equalFirst.getDeclaringType()).thenReturn(TypeDescription.ForLoadedType.of(FirstSample.class));
        assertThat(describe(first), is(equalFirst));
        FieldDescription equalSecond = mock(FieldDescription.class);
        when(equalSecond.getName()).thenReturn(second.getName());
        when(equalSecond.getDeclaringType()).thenReturn(TypeDescription.ForLoadedType.of(SecondSample.class));
        assertThat(describe(second), is(equalSecond));
        FieldDescription equalFirstTypeOnly = mock(FieldDescription.class);
        when(equalFirstTypeOnly.getName()).thenReturn(second.getName());
        when(equalFirstTypeOnly.getDeclaringType()).thenReturn(TypeDescription.ForLoadedType.of(FirstSample.class));
        assertThat(describe(first), not(equalFirstTypeOnly));
        FieldDescription equalFirstNameOnly = mock(FieldDescription.class);
        when(equalFirstNameOnly.getName()).thenReturn(first.getName());
        when(equalFirstNameOnly.getDeclaringType()).thenReturn(TypeDescription.ForLoadedType.of(SecondSample.class));
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
    public void testTransient() throws Exception {
        assertThat(describe(first).isTransient(), is(Modifier.isTransient(first.getModifiers())));
        assertThat(describe(TransientSample.class.getDeclaredField("foo")).isTransient(),
                is(Modifier.isTransient(TransientSample.class.getDeclaredField("foo").getModifiers())));
    }

    @Test
    public void testIsVisibleTo() throws Exception {
        assertThat(describe(PublicType.class.getDeclaredField("publicField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("protectedField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("packagePrivateField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("privateField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(PublicType.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("publicField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(FirstSample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("protectedField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(FirstSample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("packagePrivateField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(FirstSample.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("privateField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(FirstSample.class)), is(ClassFileVersion.of(FirstSample.class).isAtLeast(ClassFileVersion.JAVA_V11)));
        assertThat(describe(PublicType.class.getDeclaredField("publicField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("protectedField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredField("packagePrivateField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredField("privateField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredField("publicField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(FieldDescriptionTestHelper.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("protectedField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(FieldDescriptionTestHelper.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("packagePrivateField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(FieldDescriptionTestHelper.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredField("privateField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(FieldDescriptionTestHelper.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredField("publicField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredField("protectedField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredField("packagePrivateField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredField("privateField"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(false));
        assertThat(describe(PackagePrivateFieldType.class.getDeclaredField("packagePrivateType"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(PackagePrivateFieldType.class)), is(true));
        assertThat(describe(PackagePrivateFieldType.class.getDeclaredField("packagePrivateType"))
                .isVisibleTo(TypeDescription.ForLoadedType.of(Object.class)), is(true));
    }

    @Test
    public void testAnnotations() throws Exception {
        assertThat(describe(first).getDeclaredAnnotations(), is((AnnotationList) new AnnotationList.Empty()));
        assertThat(describe(second).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.ForLoadedAnnotations(second.getDeclaredAnnotations())));
    }

    @Test
    @SuppressWarnings("cast")
    public void testGenericTypes() throws Exception {
        assertThat(describe(genericField).getType(), is(TypeDefinition.Sort.describe(genericField.getGenericType())));
        assertThat(describe(genericField).getType().asErasure(), is((TypeDescription) TypeDescription.ForLoadedType.of(genericField.getType())));
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

        @SuppressWarnings("unused")
        Object m() { // Since Java 18, a reference to the outer class is required to retain the synthetic field.
            return AbstractFieldDescriptionTest.this;
        }
    }

    private static class TransientSample {

        public transient Void foo;
    }
}
