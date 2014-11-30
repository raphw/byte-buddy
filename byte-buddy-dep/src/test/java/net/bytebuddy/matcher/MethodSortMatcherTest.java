package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class MethodSortMatcherTest extends AbstractElementMatcherTest<MethodSortMatcher<?>> {

    private static final String FOO = "foo";

    private static enum MockEngine {

        CONSTRUCTOR {
            @Override
            protected void prepare(MethodDescription mock) {
                when(mock.isConstructor()).thenReturn(true);
            }
        },

        DEFAULT_METHOD {
            @Override
            protected void prepare(MethodDescription mock) {
                when(mock.isDefaultMethod()).thenReturn(true);
            }
        },

        METHOD {
            @Override
            protected void prepare(MethodDescription mock) {
                when(mock.isMethod()).thenReturn(true);
            }
        },

        OVERRIDABLE {
            @Override
            protected void prepare(MethodDescription mock) {
                when(mock.isOverridable()).thenReturn(true);
            }
        },

        TYPE_INITIALIZER {
            @Override
            protected void prepare(MethodDescription mock) {
                when(mock.isTypeInitializer()).thenReturn(true);
            }
        },

        VISIBILITY_BRIDGE {
            @Override
            @SuppressWarnings("unchecked")
            protected void prepare(MethodDescription mock) {
                when(mock.isBridge()).thenReturn(true);
                TypeDescription typeDescription = Mockito.mock(TypeDescription.class);
                MethodList methodList = Mockito.mock(MethodList.class);
                when(mock.getDeclaringType()).thenReturn(typeDescription);
                when(typeDescription.getDeclaredMethods()).thenReturn(methodList);
                when(methodList.filter(any(ElementMatcher.class))).thenReturn(methodList);
                when(methodList.size()).thenReturn(0);
                when(mock.getParameterTypes()).thenReturn(new TypeList.Empty());
                when(mock.getReturnType()).thenReturn(Mockito.mock(TypeDescription.class));
                when(mock.getSourceCodeName()).thenReturn(FOO);
            }
        };

        protected abstract void prepare(MethodDescription mock);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {MethodSortMatcher.Sort.CONSTRUCTOR, MockEngine.CONSTRUCTOR},
                {MethodSortMatcher.Sort.DEFAULT_METHOD, MockEngine.DEFAULT_METHOD},
                {MethodSortMatcher.Sort.METHOD, MockEngine.METHOD},
                {MethodSortMatcher.Sort.OVERRIDABLE, MockEngine.OVERRIDABLE},
                {MethodSortMatcher.Sort.TYPE_INITIALIZER, MockEngine.TYPE_INITIALIZER},
                {MethodSortMatcher.Sort.VISIBILITY_BRIDGE, MockEngine.VISIBILITY_BRIDGE}
        });
    }

    private final MethodSortMatcher.Sort sort;

    private final MockEngine mockEngine;

    @SuppressWarnings("unchecked")
    public MethodSortMatcherTest(MethodSortMatcher.Sort sort, MockEngine mockEngine) {
        super((Class<MethodSortMatcher<?>>) (Object) MethodSortMatcher.class, sort.getDescription());
        this.sort = sort;
        this.mockEngine = mockEngine;
    }

    @Mock
    private MethodDescription methodDescription;

    @Test
    public void testMatch() throws Exception {
        mockEngine.prepare(methodDescription);
        assertThat(new MethodSortMatcher<MethodDescription>(sort).matches(methodDescription), is(true));
    }

    @Test
    public void testNoMatch() throws Exception {
        assertThat(new MethodSortMatcher<MethodDescription>(sort).matches(methodDescription), is(false));
    }

    @Override
    protected <S> ObjectPropertyAssertion<S> modify(ObjectPropertyAssertion<S> propertyAssertion) {
        return propertyAssertion.skipToString();
    }

    @Test
    public void testToString() throws Exception {
        assertThat(new MethodSortMatcher<MethodDescription>(sort).toString(), is(sort.getDescription()));
    }
}
