package net.bytebuddy.utility;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class JavaConstantTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private JavaConstant.Visitor<Object> visitor;

    @Mock
    private Object value;

    @Test(expected = IllegalArgumentException.class)
    public void testValueWrap() throws Exception {
        JavaConstant.Simple.of(TypeDescription.ForLoadedType.of(int.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testVisitSimpleValue() throws Exception {
        when(visitor.onValue((JavaConstant.Simple<String>) JavaConstant.Simple.ofLoaded(FOO))).thenReturn(value);
        assertThat(JavaConstant.Simple.ofLoaded(FOO).accept(visitor), is(value));
        verify(visitor).onValue((JavaConstant.Simple<String>) JavaConstant.Simple.ofLoaded(FOO));
        verifyNoMoreInteractions(visitor);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testVisitSimpleType() throws Exception {
        when(visitor.onType((JavaConstant.Simple<TypeDescription>) JavaConstant.Simple.of(TypeDescription.ForLoadedType.of(Object.class)))).thenReturn(value);
        assertThat(JavaConstant.Simple.of(TypeDescription.ForLoadedType.of(Object.class)).accept(visitor), is(value));
        verify(visitor).onType((JavaConstant.Simple<TypeDescription>) JavaConstant.Simple.of(TypeDescription.ForLoadedType.of(Object.class)));
        verifyNoMoreInteractions(visitor);
    }

    @Test
    public void testVisitMethodType() throws Exception {
        JavaConstant.MethodType constant = JavaConstant.MethodType.of(void.class);
        when(visitor.onMethodType(constant)).thenReturn(value);
        assertThat(constant.accept(visitor), is(value));
        verify(visitor).onMethodType(constant);
        verifyNoMoreInteractions(visitor);
    }

    @Test
    public void testVisitMethodHandle() throws Exception {
        JavaConstant.MethodHandle constant = JavaConstant.MethodHandle.of(Object.class.getMethod("toString"));
        when(visitor.onMethodHandle(constant)).thenReturn(value);
        assertThat(constant.accept(visitor), is(value));
        verify(visitor).onMethodHandle(constant);
        verifyNoMoreInteractions(visitor);
    }

    @Test
    public void testVisitDynamic() throws Exception {
        JavaConstant.Dynamic constant = JavaConstant.Dynamic.ofNullConstant();
        when(visitor.onDynamic(constant)).thenReturn(value);
        assertThat(constant.accept(visitor), is(value));
        verify(visitor).onDynamic(constant);
        verifyNoMoreInteractions(visitor);
    }
}
