package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DynamicTypeDefaultUnloadedTest {

    private static final Class<?> MAIN_TYPE = Void.class, AUXILIARY_TYPE = Object.class;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private LoadedTypeInitializer mainLoadedTypeInitializer, auxiliaryLoadedTypeInitializer;

    @Mock
    private DynamicType auxiliaryType;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private ClassLoadingStrategy classLoadingStrategy;

    @Mock
    private TypeDescription typeDescription, auxiliaryTypeDescription;

    private byte[] binaryRepresentation, auxiliaryTypeByte;

    private DynamicType.Unloaded<?> unloaded;


    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        binaryRepresentation = new byte[]{0, 1, 2};
        auxiliaryTypeByte = new byte[]{4, 5, 6};
        unloaded = new DynamicType.Default.Unloaded<Object>(typeDescription,
                binaryRepresentation,
                mainLoadedTypeInitializer,
                Collections.singletonList(auxiliaryType));
        Map<TypeDescription, Class<?>> loadedTypes = new HashMap<TypeDescription, Class<?>>();
        loadedTypes.put(typeDescription, MAIN_TYPE);
        loadedTypes.put(auxiliaryTypeDescription, AUXILIARY_TYPE);
        when(classLoadingStrategy.load(any(ClassLoader.class), any(LinkedHashMap.class))).thenReturn(loadedTypes);
        when(auxiliaryType.getTypeDescription()).thenReturn(auxiliaryTypeDescription);
        when(auxiliaryType.getBytes()).thenReturn(auxiliaryTypeByte);
        when(auxiliaryType.getLoadedTypeInitializers()).thenReturn(Collections.singletonMap(auxiliaryTypeDescription, auxiliaryLoadedTypeInitializer));
        when(auxiliaryType.getAuxiliaryTypes()).thenReturn(Collections.<TypeDescription, byte[]>emptyMap());
    }

    @Test
    public void testQueries() throws Exception {
        DynamicType.Loaded<?> loaded = unloaded.load(classLoader, classLoadingStrategy);
        assertThat(loaded.getTypeDescription(), is(typeDescription));
        assertThat(loaded.getBytes(), is(binaryRepresentation));
        assertThat(loaded.getAuxiliaryTypes(), is(Collections.singletonMap(auxiliaryTypeDescription, auxiliaryTypeByte)));
    }

    @Test
    public void testTypeLoading() throws Exception {
        DynamicType.Loaded<?> loaded = unloaded.load(classLoader, classLoadingStrategy);
        assertThat(loaded.getLoaded(), CoreMatchers.<Class<?>>is(MAIN_TYPE));
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(1));
        assertThat(loaded.getLoadedAuxiliaryTypes().get(auxiliaryTypeDescription), CoreMatchers.<Class<?>>is(AUXILIARY_TYPE));
        verify(mainLoadedTypeInitializer).onLoad(MAIN_TYPE);
        verify(auxiliaryLoadedTypeInitializer).onLoad(AUXILIARY_TYPE);
    }

    @Test
    public void testTypeInclusion() throws Exception {
        DynamicType additionalType = mock(DynamicType.class);
        TypeDescription additionalTypeDescription = mock(TypeDescription.class);
        when(additionalType.getTypeDescription()).thenReturn(additionalTypeDescription);
        DynamicType.Unloaded<?> dynamicType = unloaded.include(additionalType);
        assertThat(dynamicType.getAuxiliaryTypes().size(), is(2));
        assertThat(dynamicType.getAuxiliaryTypes().containsKey(additionalTypeDescription), is(true));
        assertThat(dynamicType.getAuxiliaryTypes().containsKey(auxiliaryTypeDescription), is(true));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(DynamicType.Default.Unloaded.class).apply();
    }
}
