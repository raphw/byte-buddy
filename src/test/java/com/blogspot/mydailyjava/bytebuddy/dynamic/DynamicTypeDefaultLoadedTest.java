package com.blogspot.mydailyjava.bytebuddy.dynamic;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.TypeInitializer;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

public class DynamicTypeDefaultLoadedTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeInitializer mainTypeInitializer, auxiliaryTypeInitializer;
    @Mock
    private DynamicType auxiliaryType;

    private DynamicType.Loaded<?> dynamicType;

    // TODO: Restore.

//    @Before
//    public void setUp() throws Exception {
//        byte[] binaryRepresentation = new byte[]{0, 1, 2};
//        byte[] auxiliaryTypeByte = new byte[]{4, 5, 6};
//        Map<String, Class<?>> types = new HashMap<String, Class<?>>();
//        types.put(FOO, Void.class);
//        types.put(BAR, Object.class);
//        dynamicType = new DynamicType.Default.Loaded<Object>(FOO,
//                binaryRepresentation,
//                mainTypeInitializer,
//                Collections.<DynamicType<?>>singletonList(auxiliaryType),
//                types);
//        when(auxiliaryType.getDescription()).thenReturn(BAR);
//        when(auxiliaryType.getBytes()).thenReturn(auxiliaryTypeByte);
//        when(auxiliaryType.getTypeInitializers()).thenReturn(Collections.singletonMap(BAR, auxiliaryTypeInitializer));
//        when(auxiliaryType.getRawAuxiliaryTypes()).thenReturn(Collections.<String, byte[]>emptyMap());
//    }

    @Test
    public void testTypes() throws Exception {
        assertEquals(Void.class, dynamicType.getLoaded());
        assertThat(dynamicType.getLoadedAuxiliaryTypes().size(), is(1));
        assertEquals(Object.class, dynamicType.getLoadedAuxiliaryTypes().get(BAR));
    }
}
