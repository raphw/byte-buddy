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
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class DynamicTypeDefaultLoadedTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeInitializer mainTypeInitializer, auxiliaryTypeInitializer;
    @Mock
    private DynamicType auxiliaryType;
    @Mock
    private TypeDescription typeDescription, auxiliaryTypeDescription;

    private DynamicType.Loaded<?> dynamicType;

    @Before
    public void setUp() throws Exception {
        byte[] binaryRepresentation = new byte[]{0, 1, 2};
        byte[] auxiliaryTypeByte = new byte[]{4, 5, 6};
        Map<TypeDescription, Class<?>> loadedTypes = new HashMap<TypeDescription, Class<?>>();
        loadedTypes.put(typeDescription, Void.class);
        loadedTypes.put(auxiliaryTypeDescription, Object.class);
        dynamicType = new DynamicType.Default.Loaded<Object>(typeDescription,
                binaryRepresentation,
                mainTypeInitializer,
                Collections.singletonList(auxiliaryType),
                loadedTypes);
        when(auxiliaryType.getDescription()).thenReturn(typeDescription);
        when(auxiliaryType.getBytes()).thenReturn(auxiliaryTypeByte);
        when(auxiliaryType.getTypeInitializers()).thenReturn(Collections.singletonMap(auxiliaryTypeDescription, auxiliaryTypeInitializer));
        when(auxiliaryType.getRawAuxiliaryTypes()).thenReturn(Collections.<TypeDescription, byte[]>emptyMap());
    }

    @Test
    public void testTypes() throws Exception {
        assertEquals(Void.class, dynamicType.getLoaded());
        assertThat(dynamicType.getLoadedAuxiliaryTypes().size(), is(1));
        assertEquals(Object.class, dynamicType.getLoadedAuxiliaryTypes().get(auxiliaryTypeDescription));
    }
}
