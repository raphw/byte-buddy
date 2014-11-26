package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class TypeWriterDefaultTest {

    private static final byte[] MAIN = new byte[42], FIRST = new byte[42 * 2];

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription instrumentedType, otherAuxiliaryDescription;
    @Mock
    private LoadedTypeInitializer loadedTypeInitializer;
    @Mock
    private InstrumentedType.TypeInitializer typeInitializer;
    @Mock
    private ClassFileVersion classFileVersion;
    @Mock
    private TypeWriter.Engine engine, otherEngine;
    @Mock
    private DynamicType firstAuxiliary, secondAuxiliary;

    private List<DynamicType> explicitAuxiliaryTypes;

    @Before
    public void setUp() throws Exception {
        explicitAuxiliaryTypes = Arrays.asList(firstAuxiliary);
        when(engine.create(any(Instrumentation.Context.ExtractableView.class))).thenReturn(MAIN);
        when(firstAuxiliary.getTypeDescription()).thenReturn(otherAuxiliaryDescription);
        when(firstAuxiliary.getBytes()).thenReturn(FIRST);
    }

    @Test
    public void testDynamicTypeCreation() throws Exception {
        DynamicType dynamicType = new TypeWriter.Default<Object>(instrumentedType,
                loadedTypeInitializer,
                typeInitializer,
                explicitAuxiliaryTypes,
                classFileVersion,
                engine).make();
        assertThat(dynamicType.getBytes(), is(MAIN));
        assertThat(dynamicType.getTypeDescription(), is(instrumentedType));
        assertThat(dynamicType.getLoadedTypeInitializers().get(instrumentedType), is(loadedTypeInitializer));
        assertThat(dynamicType.getRawAuxiliaryTypes().size(), is(1));
        assertThat(dynamicType.getRawAuxiliaryTypes().get(otherAuxiliaryDescription), is(FIRST));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeWriter.Default.class).apply();
    }
}
