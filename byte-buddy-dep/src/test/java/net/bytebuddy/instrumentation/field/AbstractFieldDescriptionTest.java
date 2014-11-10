package net.bytebuddy.instrumentation.field;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.packaging.VisibilityFieldTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractFieldDescriptionTest {

    protected abstract FieldDescription describe(Field field);

    private Field first, second;

    @Before
    public void setUp() throws Exception {
        first = FirstSample.class.getDeclaredField("first");
        second = SecondSample.class.getDeclaredField("second");
    }

    @Test
    public void testPrecondition() throws Exception {
        assertThat(describe(first), not(equalTo(describe(second))));
        assertThat(describe(first), equalTo(describe(first)));
        assertThat(describe(second), equalTo(describe(second)));
        assertThat(describe(first), is((FieldDescription) new FieldDescription.ForLoadedField(first)));
        assertThat(describe(second), is((FieldDescription) new FieldDescription.ForLoadedField(second)));
    }

    @Test
    public void testFieldType() throws Exception {
        assertThat(describe(first).getFieldType(), is((TypeDescription) new TypeDescription.ForLoadedType(first.getType())));
        assertThat(describe(second).getFieldType(), is((TypeDescription) new TypeDescription.ForLoadedType(second.getType())));
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
        assertThat(describe(first).hashCode(), not(is(describe(second).hashCode())));
    }

    @Test
    public void testEquals() throws Exception {
        FieldDescription identical = describe(first);
        assertThat(identical, equalTo(identical));
        FieldDescription equalFirst = mock(FieldDescription.class);
        when(equalFirst.getName()).thenReturn(first.getName());
        when(equalFirst.getDeclaringType()).thenReturn(new TypeDescription.ForLoadedType(FirstSample.class));
        assertThat(describe(first), equalTo(equalFirst));
        FieldDescription equalSecond = mock(FieldDescription.class);
        when(equalSecond.getName()).thenReturn(second.getName());
        when(equalSecond.getDeclaringType()).thenReturn(new TypeDescription.ForLoadedType(SecondSample.class));
        assertThat(describe(second), equalTo(equalSecond));
        FieldDescription equalFirstTypeOnly = mock(FieldDescription.class);
        when(equalFirstTypeOnly.getName()).thenReturn(second.getName());
        when(equalFirstTypeOnly.getDeclaringType()).thenReturn(new TypeDescription.ForLoadedType(FirstSample.class));
        assertThat(describe(first), not(equalTo(equalFirstTypeOnly)));
        FieldDescription equalFirstNameOnly = mock(FieldDescription.class);
        when(equalFirstNameOnly.getName()).thenReturn(first.getName());
        when(equalFirstNameOnly.getDeclaringType()).thenReturn(new TypeDescription.ForLoadedType(SecondSample.class));
        assertThat(describe(first), not(equalTo(equalFirstNameOnly)));
        assertThat(describe(first), not(equalTo(equalSecond)));
        assertThat(describe(first), not(equalTo(new Object())));
        assertThat(describe(first), not(equalTo(null)));
    }

    @Test
    public void testToString() throws Exception {
        assertThat(describe(first).toString(), is(first.toString()));
        assertThat(describe(second).toString(), is(second.toString()));
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
                .isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("protectedField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredField("packagePrivateField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredField("privateField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredField("publicField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(VisibilityFieldTestHelper.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("protectedField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(VisibilityFieldTestHelper.class)), is(true));
        assertThat(describe(PublicType.class.getDeclaredField("packagePrivateField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(VisibilityFieldTestHelper.class)), is(false));
        assertThat(describe(PublicType.class.getDeclaredField("privateField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(VisibilityFieldTestHelper.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredField("publicField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredField("protectedField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredField("packagePrivateField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(describe(PackagePrivateType.class.getDeclaredField("privateField"))
                .isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
    }

    protected static class FirstSample {

        private Void first;
    }

    protected static class SecondSample {

        int second;
    }

    public static class PublicType {

        public Void publicField;

        protected Void protectedField;

        Void packagePrivateField;

        private Void privateField;
    }

    static class PackagePrivateType {

        public Void publicField;

        protected Void protectedField;

        Void packagePrivateField;

        private Void privateField;
    }
}
