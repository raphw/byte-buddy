package com.blogspot.mydailyjava.bytebuddy.dynamic;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.TypeInitializer;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;
import java.util.LinkedHashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DynamicTypeDefaultUnloadedTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeInitializer mainTypeInitializer, auxiliaryTypeInitializer;
    @Mock
    private DynamicType<?> auxiliaryType;
    @Mock
    private ClassLoader classLoader;
    @Mock
    private ClassLoadingStrategy classLoadingStrategy;

    private byte[] typeByte, auxiliaryTypeByte;

    private DynamicType.Unloaded<?> unloaded;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        typeByte = new byte[]{0, 1, 2};
        auxiliaryTypeByte = new byte[]{4, 5, 6};
        unloaded = new DynamicType.Default.Unloaded<Object>(FOO,
                typeByte,
                mainTypeInitializer,
                Collections.<DynamicType<?>>singletonList(auxiliaryType));
        LinkedHashMap<String, Class<?>> loadedTypes = new LinkedHashMap<String, Class<?>>();
        loadedTypes.put(FOO, Void.class);
        loadedTypes.put(BAR, Object.class);
        when(classLoadingStrategy.load(any(ClassLoader.class), any(LinkedHashMap.class))).thenReturn(loadedTypes);
        when(auxiliaryType.getName()).thenReturn(BAR);
        when(auxiliaryType.getBytes()).thenReturn(auxiliaryTypeByte);
        when(auxiliaryType.getTypeInitializers()).thenReturn(Collections.singletonMap(BAR, auxiliaryTypeInitializer));
        when(auxiliaryType.getRawAuxiliaryTypes()).thenReturn(Collections.<String, byte[]>emptyMap());
    }

    @Test
    public void testBasicData() throws Exception {
        DynamicType.Loaded<?> loaded = unloaded.load(classLoader, classLoadingStrategy);
        assertThat(loaded.getName(), is(FOO));
        assertThat(loaded.getBytes(), is(typeByte));
        assertThat(loaded.getRawAuxiliaryTypes(), is(Collections.singletonMap(BAR, auxiliaryTypeByte)));
    }

    @Test
    public void testLoad() throws Exception {
        DynamicType.Loaded<?> loaded = unloaded.load(classLoader, classLoadingStrategy);
        assertEquals(Void.class, loaded.getLoaded());
        assertThat(loaded.getAuxiliaryTypes().size(), is(1));
        assertEquals(Object.class, loaded.getAuxiliaryTypes().get(BAR));
        verify(mainTypeInitializer).onLoad(Void.class);
        verify(auxiliaryTypeInitializer).onLoad(Object.class);
    }
}
