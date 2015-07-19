package net.bytebuddy.pool;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypePoolLazyDeclarationContextTest {

    private static final String FOO = "baz.foo", FOO_INTERNAL = "baz/foo", BAR = "bar", QUX = "qux";

    @Test
    public void testSelfDeclared() throws Exception {
        assertThat(TypePool.LazyTypeDescription.DeclarationContext.SelfDeclared.INSTANCE
                .isDeclaredInMethod(), is(false));
        assertThat(TypePool.LazyTypeDescription.DeclarationContext.SelfDeclared.INSTANCE
                .isDeclaredInType(), is(false));
        assertThat(TypePool.LazyTypeDescription.DeclarationContext.SelfDeclared.INSTANCE
                .isSelfDeclared(), is(true));
    }

    @Test
    public void testSelfDeclaredGetTypeIsNull() throws Exception {
        assertThat(TypePool.LazyTypeDescription.DeclarationContext.SelfDeclared.INSTANCE
                .getEnclosingType(mock(TypePool.class)), nullValue(TypeDescription.class));
    }

    @Test
    public void testSelfDeclaredGetMethodIsNull() throws Exception {
        assertThat(TypePool.LazyTypeDescription.DeclarationContext.SelfDeclared.INSTANCE
                .getEnclosingMethod(mock(TypePool.class)), nullValue(MethodDescription.class));
    }

    @Test
    public void testDeclaredInType() throws Exception {
        assertThat(new TypePool.LazyTypeDescription.DeclarationContext.DeclaredInType(FOO_INTERNAL)
                .isDeclaredInMethod(), is(false));
        assertThat(new TypePool.LazyTypeDescription.DeclarationContext.DeclaredInType(FOO_INTERNAL)
                .isDeclaredInType(), is(true));
        assertThat(new TypePool.LazyTypeDescription.DeclarationContext.DeclaredInType(FOO_INTERNAL)
                .isSelfDeclared(), is(false));
    }

    @Test
    public void testDeclaredInTypeGetTypeIsNotNull() throws Exception {
        TypePool typePool = mock(TypePool.class);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typePool.describe(FOO)).thenReturn(new TypePool.Resolution.Simple(typeDescription));
        assertThat(new TypePool.LazyTypeDescription.DeclarationContext.DeclaredInType(FOO_INTERNAL)
                .getEnclosingType(typePool), is(typeDescription));
    }

    @Test
    public void testDeclaredInTypeGetMethodIsNull() throws Exception {
        assertThat(new TypePool.LazyTypeDescription.DeclarationContext.DeclaredInType(FOO_INTERNAL)
                .getEnclosingMethod(mock(TypePool.class)), nullValue(MethodDescription.class));
    }

    @Test
    public void testDeclaredInMethod() throws Exception {
        assertThat(new TypePool.LazyTypeDescription.DeclarationContext.DeclaredInMethod(FOO_INTERNAL, BAR, QUX)
                .isDeclaredInMethod(), is(true));
        assertThat(new TypePool.LazyTypeDescription.DeclarationContext.DeclaredInMethod(FOO_INTERNAL, BAR, QUX)
                .isDeclaredInType(), is(false));
        assertThat(new TypePool.LazyTypeDescription.DeclarationContext.DeclaredInMethod(FOO_INTERNAL, BAR, QUX)
                .isSelfDeclared(), is(false));
    }

    @Test
    public void testDeclaredInMethodGetTypeIsNotNull() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        TypePool typePool = mock(TypePool.class);
        when(typePool.describe(FOO)).thenReturn(new TypePool.Resolution.Simple(typeDescription));
        assertThat(new TypePool.LazyTypeDescription.DeclarationContext.DeclaredInMethod(FOO_INTERNAL, BAR, QUX)
                .getEnclosingType(typePool), is(typeDescription));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeclaredInMethodGetMethodIsNull() throws Exception {
        MethodDescription methodDescription = mock(MethodDescription.class);
        when(methodDescription.getSourceCodeName()).thenReturn(BAR);
        when(methodDescription.getDescriptor()).thenReturn(QUX);
        TypeDescription typeDescription = mock(TypeDescription.class);
        TypePool typePool = mock(TypePool.class);
        when(typePool.describe(FOO)).thenReturn(new TypePool.Resolution.Simple(typeDescription));
        when(typeDescription.getDeclaredMethods())
                .thenReturn((MethodList) new MethodList.Explicit<MethodDescription>(Collections.singletonList(methodDescription)));
        assertThat(new TypePool.LazyTypeDescription.DeclarationContext.DeclaredInMethod(FOO_INTERNAL, BAR, QUX)
                .getEnclosingMethod(typePool), is(methodDescription));
    }
}
