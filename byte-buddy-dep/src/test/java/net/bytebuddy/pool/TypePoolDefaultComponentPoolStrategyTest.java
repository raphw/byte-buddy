package net.bytebuddy.pool;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypePoolDefaultComponentPoolStrategyTest {

    private static final String FOO = "foo", BAR = "bar", BAR_DESCRIPTOR = "L" + BAR + ";", QUX = "qux", BAZ = "baz";

    @Test(expected = IllegalStateException.class)
    public void testIllegal() throws Exception {
        TypePool.Default.ComponentTypeLocator.Illegal.INSTANCE.bind(FOO);
    }

    @Test
    public void testForAnnotationProperty() throws Exception {
        TypePool typePool = mock(TypePool.class);
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typePool.describe(BAR)).thenReturn(new TypePool.Resolution.Simple(typeDescription));
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        when(typeDescription.getDeclaredMethods()).thenReturn(new MethodList.Explicit<MethodDescription.InDefinedShape>(methodDescription));
        when(methodDescription.getActualName()).thenReturn(FOO);
        TypeDescription.Generic returnType = mock(TypeDescription.Generic.class);
        TypeDescription rawReturnType = mock(TypeDescription.class);
        when(returnType.asErasure()).thenReturn(rawReturnType);
        when(methodDescription.getReturnType()).thenReturn(returnType);
        TypeDescription rawComponentType = mock(TypeDescription.class);
        when(rawReturnType.getComponentType()).thenReturn(rawComponentType);
        when(rawComponentType.getName()).thenReturn(QUX);
        assertThat(new TypePool.Default.ComponentTypeLocator.ForAnnotationProperty(typePool, BAR_DESCRIPTOR).bind(FOO).lookup(), is(QUX));
    }

    @Test
    public void testForArrayType() throws Exception {
        assertThat(new TypePool.Default.ComponentTypeLocator.ForArrayType("()[" + BAR_DESCRIPTOR).bind(FOO).lookup(), is(BAR));
    }
}
