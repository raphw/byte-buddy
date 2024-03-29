package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamicTypeDefaultLoadedTest {

    private static final Class<?> MAIN_TYPE = Void.class, AUXILIARY_TYPE = Object.class;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private LoadedTypeInitializer mainLoadedTypeInitializer, auxiliaryLoadedTypeInitializer;

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
                mainLoadedTypeInitializer,
                Collections.singletonList(auxiliaryType),
                loadedTypes);
        when(auxiliaryType.getTypeDescription()).thenReturn(mainTypeDescription);
    }

    @Test
    public void testLoadedTypeDescription() throws Exception {
        assertThat(dynamicType.getLoaded(), CoreMatchers.<Class<?>>is(MAIN_TYPE));
        assertThat(dynamicType.getTypeDescription(), is(mainTypeDescription));
        assertThat(dynamicType.getLoadedAuxiliaryTypes().size(), is(1));
        assertThat(dynamicType.getLoadedAuxiliaryTypes().keySet(), hasItem(auxiliaryTypeDescription));
        assertThat(dynamicType.getLoadedAuxiliaryTypes().get(auxiliaryTypeDescription), CoreMatchers.<Class<?>>is(AUXILIARY_TYPE));
        assertThat(dynamicType.getAllLoaded().size(), is(2));
        assertThat(dynamicType.getAllLoaded().keySet(), hasItem(mainTypeDescription));
        assertThat(dynamicType.getAllLoaded().keySet(), hasItem(auxiliaryTypeDescription));
    }
}
