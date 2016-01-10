package net.bytebuddy.description.type;

import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericVisitorSubstitutorForTokenNormalizationTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription.Generic source, target;

    @Before
    public void setUp() throws Exception {
        when(source.getSymbol()).thenReturn(FOO);
    }

    @Test
    public void testTargetType() throws Exception {
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTokenNormalization(target).onSimpleType(TargetType.GENERIC_DESCRIPTION), is(target));
    }

    @Test
    public void testNotTargetType() throws Exception {
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTokenNormalization(target).onSimpleType(source), is(source));
    }

    @Test
    public void testTypeVariable() throws Exception {
        assertThat(new TypeDescription.Generic.Visitor.Substitutor.ForTokenNormalization(target).onTypeVariable(source),
                is((TypeDescription.Generic) new TypeDescription.Generic.OfTypeVariable.Symbolic(FOO, Collections.emptyList()))); // TODO
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Substitutor.ForTokenNormalization.class)
                .refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
                    @Override
                    public void apply(TypeDescription mock) {
                        when(mock.asGenericType()).thenReturn(Mockito.mock(TypeDescription.Generic.class));
                    }
                }).apply();
    }
}