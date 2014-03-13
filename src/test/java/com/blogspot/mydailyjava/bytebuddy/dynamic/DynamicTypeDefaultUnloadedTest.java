package com.blogspot.mydailyjava.bytebuddy.dynamic;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.TypeInitializer;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DynamicTypeDefaultUnloadedTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeInitializer mainTypeInitializer, auxiliaryTypeInitializer;
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
                mainTypeInitializer,
                Collections.singletonList(auxiliaryType));
        Map<TypeDescription, Class<?>> loadedTypes = new HashMap<TypeDescription, Class<?>>();
        loadedTypes.put(typeDescription, Void.class);
        loadedTypes.put(auxiliaryTypeDescription, Object.class);
        when(classLoadingStrategy.load(any(ClassLoader.class), any(LinkedHashMap.class))).thenReturn(loadedTypes);
        when(auxiliaryType.getDescription()).thenReturn(auxiliaryTypeDescription);
        when(auxiliaryType.getBytes()).thenReturn(auxiliaryTypeByte);
        when(auxiliaryType.getTypeInitializers()).thenReturn(Collections.singletonMap(auxiliaryTypeDescription, auxiliaryTypeInitializer));
        when(auxiliaryType.getRawAuxiliaryTypes()).thenReturn(Collections.<TypeDescription, byte[]>emptyMap());
    }

    @Test
    public void testBasicData() throws Exception {
        DynamicType.Loaded<?> loaded = unloaded.load(classLoader, classLoadingStrategy);
        assertThat(loaded.getDescription(), is(typeDescription));
        assertThat(loaded.getBytes(), is(binaryRepresentation));
        assertThat(loaded.getRawAuxiliaryTypes(), is(Collections.singletonMap(auxiliaryTypeDescription, auxiliaryTypeByte)));
    }

    @Test
    public void testLoad() throws Exception {
        DynamicType.Loaded<?> loaded = unloaded.load(classLoader, classLoadingStrategy);
        assertEquals(Void.class, loaded.getLoaded());
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(1));
        assertEquals(Object.class, loaded.getLoadedAuxiliaryTypes().get(auxiliaryTypeDescription));
        verify(mainTypeInitializer).onLoad(Void.class);
        verify(auxiliaryTypeInitializer).onLoad(Object.class);
    }
}
