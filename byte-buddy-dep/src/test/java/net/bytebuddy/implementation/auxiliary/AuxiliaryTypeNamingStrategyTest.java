package net.bytebuddy.implementation.auxiliary;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.RandomString;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class AuxiliaryTypeNamingStrategyTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription instrumentedType;

    @Mock
    private AuxiliaryType auxiliaryType;

    @Test
    public void testEnumerating() {
        when(instrumentedType.getName()).thenReturn(BAR);
        assertThat(new AuxiliaryType.NamingStrategy.Enumerating(FOO).name(instrumentedType, auxiliaryType), is(BAR
                + "$foo$"
                + RandomString.hashOf(auxiliaryType)));
    }

    @Test
    public void testSuffixing() {
        when(instrumentedType.getName()).thenReturn(BAR);
        when(auxiliaryType.getSuffix()).thenReturn(QUX);
        assertThat(new AuxiliaryType.NamingStrategy.Suffixing(FOO).name(instrumentedType, auxiliaryType), is(BAR
                + "$foo$"
                + QUX));
    }

    @Test
    public void testSuffixingRandom() {
        when(instrumentedType.getName()).thenReturn(BAR);
        assertThat(new AuxiliaryType.NamingStrategy.SuffixingRandom(FOO).name(instrumentedType, auxiliaryType), startsWith(BAR + "$foo$"));
    }
}
