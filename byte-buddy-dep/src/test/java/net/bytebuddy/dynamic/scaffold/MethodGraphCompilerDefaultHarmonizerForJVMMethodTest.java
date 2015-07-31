package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodGraphCompilerDefaultHarmonizerForJVMMethodTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final int MODIFIERS = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private GenericTypeDescription first, second;

    @Mock
    private TypeDescription firstRaw, secondRaw;

    @Mock
    private ParameterDescription.Token firstParameter, secondParameter;

    @Before
    public void setUp() throws Exception {
        when(first.asRawType()).thenReturn(firstRaw);
        when(second.asRawType()).thenReturn(secondRaw);
        when(firstParameter.getType()).thenReturn(first);
        when(secondParameter.getType()).thenReturn(second);
    }

    @Test
    public void testMethodEqualityHashCode() throws Exception {
        assertThat(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)).hashCode(),
                is(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)).hashCode()));
    }

    @Test
    public void testMethodNameInequalityHashCode() throws Exception {
        assertThat(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)).hashCode(),
                is(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(BAR,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)).hashCode()));
    }

    @Test
    public void testReturnTypeInequalityHashCode() throws Exception {
        assertThat(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)).hashCode(),
                not(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        second,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)).hashCode()));
    }

    @Test
    public void testParameterTypeInequalityHashCode() throws Exception {
        assertThat(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)).hashCode(),
                not(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(secondParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)).hashCode()));
    }

    @Test
    public void testParameterTypeLengthInequalityHashCode() throws Exception {
        assertThat(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)).hashCode(),
                not(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)).hashCode()));
    }

    @Test
    public void testMethodEquality() throws Exception {
        assertThat(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)),
                is(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE))));
    }

    @Test
    public void testMethodNameInequality() throws Exception {
        assertThat(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)),
                is(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(BAR,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE))));
    }

    @Test
    public void testReturnTypeInequality() throws Exception {
        assertThat(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)),
                not(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        second,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE))));
    }

    @Test
    public void testParameterTypeInequality() throws Exception {
        assertThat(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)),
                not(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(secondParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE))));
    }

    @Test
    public void testParameterTypeLengthInequality() throws Exception {
        assertThat(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.singletonList(firstParameter),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE)),
                not(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE.wrap(new MethodDescription.Token(FOO,
                        MODIFIERS,
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        first,
                        Collections.<ParameterDescription.Token>emptyList(),
                        Collections.singletonList(mock(GenericTypeDescription.class)),
                        Collections.singletonList(mock(AnnotationDescription.class)),
                        MethodDescription.NO_DEFAULT_VALUE))));
    }

    @Test
    public void testFactory() throws Exception {
        assertThat(MethodGraph.Compiler.Default.forJVMHierarchy(), is((MethodGraph.Compiler) new MethodGraph.Compiler
                .Default<MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.Token>(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.INSTANCE,
                MethodGraph.Compiler.Default.Merger.Directional.LEFT)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.Token.class).create(new ObjectPropertyAssertion.Creator<MethodDescription.Token>() {
            @Override
            public MethodDescription.Token create() {
                MethodDescription.Token methodToken = mock(MethodDescription.Token.class);
                TypeDescription typeDescription = mock(TypeDescription.class);
                when(typeDescription.asRawType()).thenReturn(typeDescription);
                when(methodToken.getReturnType()).thenReturn(typeDescription);
                when(methodToken.getParameterTokens())
                        .thenReturn(new ByteCodeElement.Token.TokenList<ParameterDescription.Token>(Collections.<ParameterDescription.Token>emptyList()));
                return methodToken;
            }
        }).applyBasic();
        ObjectPropertyAssertion.of(MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.class).apply();
    }
}
