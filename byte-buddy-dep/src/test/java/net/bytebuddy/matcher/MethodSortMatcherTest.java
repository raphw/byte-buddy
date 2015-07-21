package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class MethodSortMatcherTest extends AbstractElementMatcherTest<MethodSortMatcher<?>> {

    private static final String FOO = "foo";

    private final MethodSortMatcher.Sort sort;

    private final MockEngine mockEngine;

    @Mock
    private MethodDescription methodDescription;

    @SuppressWarnings("unchecked")
    public MethodSortMatcherTest(MethodSortMatcher.Sort sort, MockEngine mockEngine) {
        super((Class<MethodSortMatcher<?>>) (Object) MethodSortMatcher.class, sort.getDescription());
        this.sort = sort;
        this.mockEngine = mockEngine;
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

    private enum MockEngine {

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
                MethodDescription.Token methodToken = Mockito.mock(MethodDescription.Token.class);
                when(mock.asToken()).thenReturn(methodToken);
                TypeDescription declaringType = Mockito.mock(TypeDescription.class);
                when(declaringType.asRawType()).thenReturn(declaringType);
                when(mock.getDeclaringType()).thenReturn(declaringType);
                TypeDescription superType = Mockito.mock(TypeDescription.class);
                when(declaringType.getSuperType()).thenReturn(superType);
                MethodDescription.InDeclaredForm methodDescription = Mockito.mock(MethodDescription.InDeclaredForm.class);
                when(methodDescription.isOverridable()).thenReturn(true);
                when(methodDescription.asToken()).thenReturn(methodToken);
                when(superType.getDeclaredMethods())
                        .thenReturn(new MethodList.Explicit<MethodDescription.InDeclaredForm>(Collections.singletonList(methodDescription)));
            }
        };

        protected abstract void prepare(MethodDescription mock);
    }
}
