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

        TYPE_BRIDGE {
            @Override
            @SuppressWarnings("unchecked")
            protected void prepare(MethodDescription mock) {
                when(mock.isBridge()).thenReturn(true);
                when(mock.getInternalName()).thenReturn(FOO);
                when(mock.getParameters()).thenReturn((ParameterList) new ParameterList.Empty());
                TypeDescription typeDescription = Mockito.mock(TypeDescription.class);
                when(typeDescription.asRawType()).thenReturn(typeDescription);
                when(typeDescription.getDeclaredMethods()).thenReturn(new MethodList.Empty());
                when(mock.getDeclaringType()).thenReturn(typeDescription);
                TypeDescription superType = Mockito.mock(TypeDescription.class);
                when(typeDescription.getSuperType()).thenReturn(superType);
                when(superType.iterator()).thenReturn(new GenericTypeDescription.SuperTypeIterator(superType));
                MethodDescription bridgeTarget = Mockito.mock(MethodDescription.class);
                MethodDescription.InDefinedShape definedBridgeTarget = Mockito.mock(MethodDescription.InDefinedShape.class);
                when(bridgeTarget.asDefined()).thenReturn(definedBridgeTarget);
                MethodDescription.Token methodToken = Mockito.mock(MethodDescription.Token.class);
                when(definedBridgeTarget.asToken()).thenReturn(methodToken);
                when(mock.asToken()).thenReturn(methodToken);
                when(superType.getDeclaredMethods())
                        .thenReturn((MethodList) new MethodList.Explicit<MethodDescription>(Collections.singletonList(bridgeTarget)));
            }
        },

        VISIBILITY_BRIDGE {
            @Override
            @SuppressWarnings("unchecked")
            protected void prepare(MethodDescription mock) {
                when(mock.isBridge()).thenReturn(true);
                when(mock.getInternalName()).thenReturn(FOO);
                when(mock.getParameters()).thenReturn((ParameterList) new ParameterList.Empty());
                TypeDescription typeDescription = Mockito.mock(TypeDescription.class);
                when(typeDescription.asRawType()).thenReturn(typeDescription);
                when(typeDescription.getDeclaredMethods()).thenReturn(new MethodList.Empty());
                when(mock.getDeclaringType()).thenReturn(typeDescription);
                TypeDescription superType = Mockito.mock(TypeDescription.class);
                when(typeDescription.getSuperType()).thenReturn(superType);
                when(superType.iterator()).thenReturn(new GenericTypeDescription.SuperTypeIterator(superType));
                MethodDescription.InDefinedShape bridgeTarget = Mockito.mock(MethodDescription.InDefinedShape.class);
                when(bridgeTarget.asDefined()).thenReturn(bridgeTarget);
                MethodDescription.Token methodToken = Mockito.mock(MethodDescription.Token.class);
                when(bridgeTarget.asToken()).thenReturn(methodToken);
                when(mock.asToken()).thenReturn(methodToken);
                when(superType.getDeclaredMethods())
                        .thenReturn((MethodList) new MethodList.Explicit<MethodDescription>(Collections.singletonList(bridgeTarget)));
            }
        };

        protected abstract void prepare(MethodDescription mock);
    }
}
