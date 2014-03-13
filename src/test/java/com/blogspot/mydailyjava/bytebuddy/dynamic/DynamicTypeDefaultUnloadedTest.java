package com.blogspot.mydailyjava.bytebuddy.dynamic;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.TypeInitializer;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.mockito.Mock;

public class DynamicTypeDefaultUnloadedTest {

    private static final String FOO = "foo", BAR = "bar";

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

    private byte[] typeByte, auxiliaryTypeByte;

    private DynamicType.Unloaded<?> unloaded;

    // TODO: Restore.

//    @Before
//    @SuppressWarnings("unchecked")
//    public void setUp() throws Exception {
//        binaryRepresentation = new byte[]{0, 1, 2};
//        auxiliaryTypeByte = new byte[]{4, 5, 6};
//        unloaded = new DynamicType.Default.Unloaded<Object>(FOO,
//                binaryRepresentation,
//                mainTypeInitializer,
//                Collections.<DynamicType<?>>singletonList(auxiliaryType));
//        LinkedHashMap<String, Class<?>> loadedTypes = new LinkedHashMap<String, Class<?>>();
//        loadedTypes.put(FOO, Void.class);
//        loadedTypes.put(BAR, Object.class);
//        when(classLoadingStrategy.load(any(ClassLoader.class), any(LinkedHashMap.class))).thenReturn(loadedTypes);
//        when(auxiliaryType.getDescription()).thenReturn(BAR);
//        when(auxiliaryType.getBytes()).thenReturn(auxiliaryTypeByte);
//        when(auxiliaryType.getTypeInitializers()).thenReturn(Collections.singletonMap(BAR, auxiliaryTypeInitializer));
//        when(auxiliaryType.getRawAuxiliaryTypes()).thenReturn(Collections.<String, byte[]>emptyMap());
//    }
//
//    @Test
//    public void testBasicData() throws Exception {
//        DynamicType.Loaded<?> loaded = unloaded.load(classLoader, classLoadingStrategy);
//        assertThat(loaded.getDescription(), is(FOO));
//        assertThat(loaded.getBytes(), is(binaryRepresentation));
//        assertThat(loaded.getRawAuxiliaryTypes(), is(Collections.singletonMap(BAR, auxiliaryTypeByte)));
//    }
//
//    @Test
//    public void testLoad() throws Exception {
//        DynamicType.Loaded<?> loaded = unloaded.load(classLoader, classLoadingStrategy);
//        assertEquals(Void.class, loaded.getLoaded());
//        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(1));
//        assertEquals(Object.class, loaded.getLoadedAuxiliaryTypes().get(BAR));
//        verify(mainTypeInitializer).onLoad(Void.class);
//        verify(auxiliaryTypeInitializer).onLoad(Object.class);
//    }
}
