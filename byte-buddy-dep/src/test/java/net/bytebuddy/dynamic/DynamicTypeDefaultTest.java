package net.bytebuddy.dynamic;

import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DynamicTypeDefaultTest {

    private static final String FOO = "foo", BAR = "bar", TEMP = "tmp", DOT_CLASS = ".class";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private LoadedTypeInitializer mainLoadedTypeInitializer, auxiliaryLoadedTypeInitializer;
    @Mock
    private DynamicType auxiliaryType;
    @Mock
    private TypeDescription typeDescription, auxiliaryTypeDescription;

    private DynamicType dynamicType;
    private byte[] binaryRepresentation, auxiliaryTypeBinaryRepresentation;

    private static void assertFile(File file, byte[] binaryRepresentation) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            byte[] buffer = new byte[binaryRepresentation.length + 1];
            assertThat(fileInputStream.read(buffer), is(binaryRepresentation.length));
            int index = 0;
            for (byte b : binaryRepresentation) {
                assertThat(buffer[index++], is(b));
            }
            assertThat(buffer[index], is((byte) 0));
        } finally {
            fileInputStream.close();
        }
        assertThat(file.delete(), is(true));
    }

    private static File makeTemporaryFolder() throws IOException {
        File file = File.createTempFile(TEMP, TEMP);
        try {
            File folder = new File(file.getParentFile(), TEMP + Math.abs(new Random().nextInt()));
            assertThat(folder.mkdir(), is(true));
            return folder;
        } finally {
            assertThat(file.delete(), is(true));
        }
    }

    @Before
    public void setUp() throws Exception {
        binaryRepresentation = new byte[]{0, 1, 2};
        auxiliaryTypeBinaryRepresentation = new byte[]{4, 5, 6};
        dynamicType = new DynamicType.Default(typeDescription,
                binaryRepresentation,
                mainLoadedTypeInitializer,
                Collections.singletonList(auxiliaryType));
        when(typeDescription.getName()).thenReturn(FOO);
        when(auxiliaryType.saveIn(any(File.class))).thenReturn(Collections.<TypeDescription, File>emptyMap());
        when(auxiliaryTypeDescription.getName()).thenReturn(BAR);
        when(auxiliaryType.getDescription()).thenReturn(auxiliaryTypeDescription);
        when(auxiliaryType.getBytes()).thenReturn(auxiliaryTypeBinaryRepresentation);
        when(auxiliaryType.getTypeInitializers()).thenReturn(Collections.singletonMap(auxiliaryTypeDescription, auxiliaryLoadedTypeInitializer));
        when(auxiliaryType.getRawAuxiliaryTypes()).thenReturn(Collections.<TypeDescription, byte[]>emptyMap());
    }

    @Test
    public void testByteArray() throws Exception {
        assertThat(dynamicType.getBytes(), is(binaryRepresentation));
    }

    @Test
    public void testTypeDescription() throws Exception {
        assertThat(dynamicType.getDescription(), is(typeDescription));
    }

    @Test
    public void testRawAuxiliaryTypes() throws Exception {
        assertThat(dynamicType.getRawAuxiliaryTypes().size(), is(1));
        assertThat(dynamicType.getRawAuxiliaryTypes().get(auxiliaryTypeDescription), is(auxiliaryTypeBinaryRepresentation));
    }

    @Test
    public void testTypeInitializersNotAlive() throws Exception {
        assertThat(dynamicType.hasAliveTypeInitializers(), is(false));
    }

    @Test
    public void testTypeInitializersAliveMain() throws Exception {
        when(mainLoadedTypeInitializer.isAlive()).thenReturn(true);
        assertThat(dynamicType.hasAliveTypeInitializers(), is(true));
    }

    @Test
    public void testTypeInitializersAliveAuxiliary() throws Exception {
        when(auxiliaryLoadedTypeInitializer.isAlive()).thenReturn(true);
        assertThat(dynamicType.hasAliveTypeInitializers(), is(true));
    }

    @Test
    public void testTypeInitializers() throws Exception {
        assertThat(dynamicType.getTypeInitializers().size(), is(2));
        assertThat(dynamicType.getTypeInitializers().get(typeDescription), is(mainLoadedTypeInitializer));
        assertThat(dynamicType.getTypeInitializers().get(auxiliaryTypeDescription), is(auxiliaryLoadedTypeInitializer));
    }

    @Test
    public void testFileSaving() throws Exception {
        File folder = makeTemporaryFolder();
        try {
            Map<TypeDescription, File> files = dynamicType.saveIn(folder);
            assertThat(files.size(), is(1));
            assertFile(files.get(typeDescription), binaryRepresentation);
        } finally {
            assertThat(folder.delete(), is(true));
        }
        verify(auxiliaryType).saveIn(folder);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(dynamicType.hashCode(), is(dynamicType.hashCode()));
        assertThat(dynamicType, is(dynamicType));
        DynamicType other = new DynamicType.Default(auxiliaryTypeDescription,
                binaryRepresentation,
                auxiliaryLoadedTypeInitializer,
                Collections.<DynamicType>emptyList());
        assertThat(dynamicType.hashCode(), not(is(other.hashCode())));
        assertThat(dynamicType, not(is(other)));
    }
}
