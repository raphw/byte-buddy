package net.bytebuddy.dynamic;

import net.bytebuddy.instrumentation.TypeInitializer;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamicTypeDefaultLoadedTest {

    private static final Class<?> MAIN_TYPE = Void.class, AUXILIARY_TYPE = Object.class;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeInitializer mainTypeInitializer, auxiliaryTypeInitializer;
    @Mock
    private TypeDescription mainTypeDescription, auxiliaryTypeDescription;

    private DynamicType.Loaded<?> dynamicType;

    @Before
    public void setUp() throws Exception {
        Map<TypeDescription, Class<?>> loadedTypes = new HashMap<TypeDescription, Class<?>>();
        loadedTypes.put(mainTypeDescription, MAIN_TYPE);
        loadedTypes.put(auxiliaryTypeDescription, AUXILIARY_TYPE);
        DynamicType auxiliaryType = mock(DynamicType.class);
        dynamicType = new DynamicType.Default.Loaded<Object>(mainTypeDescription,
                new byte[0],
                mainTypeInitializer,
                Collections.singletonList(auxiliaryType),
                loadedTypes);
        when(auxiliaryType.getDescription()).thenReturn(mainTypeDescription);
    }

    @Test
    public void testLoadedTypeDescription() throws Exception {
        assertEquals(MAIN_TYPE, dynamicType.getLoaded());
        assertThat(dynamicType.getDescription(), is(mainTypeDescription));
        assertThat(dynamicType.getLoadedAuxiliaryTypes().size(), is(1));
        assertThat(dynamicType.getLoadedAuxiliaryTypes().keySet(), hasItem(auxiliaryTypeDescription));
        assertEquals(AUXILIARY_TYPE, dynamicType.getLoadedAuxiliaryTypes().get(auxiliaryTypeDescription));
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(dynamicType.hashCode(), is(dynamicType.hashCode()));
        assertThat(dynamicType, is(((DynamicType) dynamicType)));
        DynamicType other = new DynamicType.Default.Loaded<Object>(auxiliaryTypeDescription,
                new byte[0],
                auxiliaryTypeInitializer,
                Collections.<DynamicType>emptyList(),
                Collections.<TypeDescription, Class<?>>emptyMap());
        assertThat(dynamicType.hashCode(), not(is(other.hashCode())));
        assertThat(dynamicType, not(is(other)));
    }
}
