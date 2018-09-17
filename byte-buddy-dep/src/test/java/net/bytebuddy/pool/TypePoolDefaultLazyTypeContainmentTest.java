package net.bytebuddy.pool;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypePoolDefaultLazyTypeContainmentTest {

    private static final String FOO = "baz.foo", FOO_INTERNAL = "baz/foo", BAR = "bar", QUX = "qux";

    @Test
    public void testSelfDeclared() throws Exception {
        assertThat(TypePool.Default.LazyTypeDescription.TypeContainment.SelfContained.INSTANCE
                .isLocalType(), is(false));
        assertThat(TypePool.Default.LazyTypeDescription.TypeContainment.SelfContained.INSTANCE
                .isSelfContained(), is(true));
    }

    @Test
    public void testSelfDeclaredGetTypeIsNull() throws Exception {
        assertThat(TypePool.Default.LazyTypeDescription.TypeContainment.SelfContained.INSTANCE
                .getEnclosingType(mock(TypePool.class)), nullValue(TypeDescription.class));
    }

    @Test
    public void testSelfDeclaredGetMethodIsNull() throws Exception {
        assertThat(TypePool.Default.LazyTypeDescription.TypeContainment.SelfContained.INSTANCE
                .getEnclosingMethod(mock(TypePool.class)), nullValue(MethodDescription.class));
    }

    @Test
    public void testDeclaredInType() throws Exception {
        assertThat(new TypePool.Default.LazyTypeDescription.TypeContainment.WithinType(FOO_INTERNAL, false)
                .isLocalType(), is(false));
        assertThat(new TypePool.Default.LazyTypeDescription.TypeContainment.WithinType(FOO_INTERNAL, false)
                .isSelfContained(), is(false));
        assertThat(new TypePool.Default.LazyTypeDescription.TypeContainment.WithinType(FOO_INTERNAL, true)
                .isLocalType(), is(true));
        assertThat(new TypePool.Default.LazyTypeDescription.TypeContainment.WithinType(FOO_INTERNAL, true)
                .isSelfContained(), is(false));
    }

    @Test
    public void testDeclaredInTypeGetTypeIsNotNull() throws Exception {
        TypePool typePool = mock(TypePool.class);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typePool.describe(FOO)).thenReturn(new TypePool.Resolution.Simple(typeDescription));
        assertThat(new TypePool.Default.LazyTypeDescription.TypeContainment.WithinType(FOO_INTERNAL, false).getEnclosingType(typePool), is(typeDescription));
        assertThat(new TypePool.Default.LazyTypeDescription.TypeContainment.WithinType(FOO_INTERNAL, true).getEnclosingType(typePool), is(typeDescription));
    }

    @Test
    public void testDeclaredInTypeGetMethodIsNull() throws Exception {
        assertThat(new TypePool.Default.LazyTypeDescription.TypeContainment.WithinType(FOO_INTERNAL, false)
                .getEnclosingMethod(mock(TypePool.class)), nullValue(MethodDescription.class));
        assertThat(new TypePool.Default.LazyTypeDescription.TypeContainment.WithinType(FOO_INTERNAL, false)
                .getEnclosingMethod(mock(TypePool.class)), nullValue(MethodDescription.class));
        assertThat(new TypePool.Default.LazyTypeDescription.TypeContainment.WithinType(FOO_INTERNAL, true)
                .getEnclosingMethod(mock(TypePool.class)), nullValue(MethodDescription.class));
        assertThat(new TypePool.Default.LazyTypeDescription.TypeContainment.WithinType(FOO_INTERNAL, true)
                .getEnclosingMethod(mock(TypePool.class)), nullValue(MethodDescription.class));
    }

    @Test
    public void testDeclaredInMethod() throws Exception {
        assertThat(new TypePool.Default.LazyTypeDescription.TypeContainment.WithinMethod(FOO_INTERNAL, BAR, QUX)
                .isLocalType(), is(true));
        assertThat(new TypePool.Default.LazyTypeDescription.TypeContainment.WithinMethod(FOO_INTERNAL, BAR, QUX)
                .isSelfContained(), is(false));
    }

    @Test
    public void testDeclaredInMethodGetTypeIsNotNull() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        TypePool typePool = mock(TypePool.class);
        when(typePool.describe(FOO)).thenReturn(new TypePool.Resolution.Simple(typeDescription));
        assertThat(new TypePool.Default.LazyTypeDescription.TypeContainment.WithinMethod(FOO_INTERNAL, BAR, QUX)
                .getEnclosingType(typePool), is(typeDescription));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeclaredInMethodGetMethodIsNull() throws Exception {
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        when(methodDescription.getActualName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(QUX);
        TypeDescription typeDescription = mock(TypeDescription.class);
        TypePool typePool = mock(TypePool.class);
        when(typePool.describe(FOO)).thenReturn(new TypePool.Resolution.Simple(typeDescription));
        when(typeDescription.getDeclaredMethods()).thenReturn((MethodList) new MethodList.Explicit<MethodDescription>(methodDescription));
        assertThat(new TypePool.Default.LazyTypeDescription.TypeContainment.WithinMethod(FOO_INTERNAL, BAR, QUX).getEnclosingMethod(typePool),
                is(methodDescription));
    }
}
