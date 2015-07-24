package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class MethodSortMatcherTest extends AbstractElementMatcherTest<MethodSortMatcher<?>> {

    private static final String FOO = "foo", BAR = "bar";

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
                {MethodSortMatcher.Sort.TYPE_BRIDGE, MockEngine.TYPE_BRIDGE},
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
            protected void prepare(MethodDescription target) {
                when(target.isConstructor()).thenReturn(true);
            }
        },

        DEFAULT_METHOD {
            @Override
            protected void prepare(MethodDescription target) {
                when(target.isDefaultMethod()).thenReturn(true);
            }
        },

        METHOD {
            @Override
            protected void prepare(MethodDescription target) {
                when(target.isMethod()).thenReturn(true);
            }
        },

        OVERRIDABLE {
            @Override
            protected void prepare(MethodDescription target) {
                when(target.isOverridable()).thenReturn(true);
            }
        },

        TYPE_INITIALIZER {
            @Override
            protected void prepare(MethodDescription target) {
                when(target.isTypeInitializer()).thenReturn(true);
            }
        },

        TYPE_BRIDGE {
            @Override
            @SuppressWarnings("unchecked")
            protected void prepare(MethodDescription target) {
                when(target.isBridge()).thenReturn(true);
                TypeDescription declaringType = mock(TypeDescription.class);
                when(declaringType.asRawType()).thenReturn(declaringType);
                when(target.getDeclaringType()).thenReturn(declaringType);
                TypeDescription superType = mock(TypeDescription.class);
                when(superType.asRawType()).thenReturn(superType);
                when(superType.iterator()).thenReturn(new GenericTypeDescription.SuperTypeIterator(superType));
                when(declaringType.getSuperType()).thenReturn(superType);
                MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
                when(methodDescription.asDefined()).thenReturn(methodDescription);
                MethodDescription.Token methodToken = mock(MethodDescription.Token.class);
                when(methodDescription.asToken()).thenReturn(methodToken);
                when(target.asToken()).thenReturn(methodToken);
                when(methodDescription.getInternalName()).thenReturn(FOO);
                when(methodDescription.getParameters()).thenReturn(new ParameterList.Empty());
                MethodDescription.InDefinedShape bridgeTarget = mock(MethodDescription.InDefinedShape.class);
                when(bridgeTarget.getSourceCodeName()).thenReturn(FOO);
                when(bridgeTarget.getParameters()).thenReturn(new ParameterList.Empty());
                when(declaringType.getDeclaredMethods())
                        .thenReturn(new MethodList.Explicit<MethodDescription.InDefinedShape>(Collections.singletonList(bridgeTarget)));
                when(superType.getDeclaredMethods())
                        .thenReturn(new MethodList.Explicit<MethodDescription.InDefinedShape>(Collections.singletonList(methodDescription)));
            }
        },

        VISIBILITY_BRIDGE {
            @Override
            @SuppressWarnings("unchecked")
            protected void prepare(MethodDescription target) {
                when(target.isBridge()).thenReturn(true);
                TypeDescription declaringType = mock(TypeDescription.class);
                when(declaringType.asRawType()).thenReturn(declaringType);
                when(target.getDeclaringType()).thenReturn(declaringType);
                TypeDescription superType = mock(TypeDescription.class);
                when(superType.asRawType()).thenReturn(superType);
                when(superType.iterator()).thenReturn(new GenericTypeDescription.SuperTypeIterator(superType));
                when(declaringType.getSuperType()).thenReturn(superType);
                MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
                when(methodDescription.asDefined()).thenReturn(methodDescription);
                MethodDescription.Token methodToken = mock(MethodDescription.Token.class);
                when(methodDescription.asToken()).thenReturn(methodToken);
                when(target.asToken()).thenReturn(methodToken);
                when(methodDescription.getInternalName()).thenReturn(FOO);
                when(methodDescription.getParameters()).thenReturn(new ParameterList.Empty());
                MethodDescription.InDefinedShape bridgeTarget = mock(MethodDescription.InDefinedShape.class);
                when(bridgeTarget.getSourceCodeName()).thenReturn(BAR);
                when(bridgeTarget.getParameters()).thenReturn(new ParameterList.Empty());
                when(declaringType.getDeclaredMethods())
                        .thenReturn(new MethodList.Explicit<MethodDescription.InDefinedShape>(Collections.singletonList(bridgeTarget)));
                when(superType.getDeclaredMethods())
                        .thenReturn(new MethodList.Explicit<MethodDescription.InDefinedShape>(Collections.singletonList(methodDescription)));
            }
        };

        protected abstract void prepare(MethodDescription target);
    }
}
