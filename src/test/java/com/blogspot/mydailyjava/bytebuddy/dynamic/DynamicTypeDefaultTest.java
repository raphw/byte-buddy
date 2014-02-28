package com.blogspot.mydailyjava.bytebuddy.dynamic;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.TypeInitializer;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DynamicTypeDefaultTest {

    private static final String FOO = "foo", BAR = "bar", TEMP = "tmp", DOT_CLASS = ".class";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeInitializer mainTypeInitializer, auxiliaryTypeInitializer;
    @Mock
    private DynamicType<?> auxiliaryType;

    private DynamicType<?> dynamicType;

    private byte[] typeByte, auxiliaryTypeByte;

    @Before
    public void setUp() throws Exception {
        typeByte = new byte[]{0, 1, 2};
        auxiliaryTypeByte = new byte[]{4, 5, 6};
        dynamicType = new DynamicType.Default<Object>(FOO,
                typeByte,
                mainTypeInitializer,
                Collections.<DynamicType<?>>singletonList(auxiliaryType));
        when(auxiliaryType.getName()).thenReturn(BAR);
        when(auxiliaryType.getBytes()).thenReturn(auxiliaryTypeByte);
        when(auxiliaryType.getTypeInitializers()).thenReturn(Collections.singletonMap(BAR, auxiliaryTypeInitializer));
        when(auxiliaryType.getRawAuxiliaryTypes()).thenReturn(Collections.<String, byte[]>emptyMap());
    }

    @Test
    public void testByteArray() throws Exception {
        assertThat(dynamicType.getBytes(), is(typeByte));
    }

    @Test
    public void testName() throws Exception {
        assertThat(dynamicType.getName(), is(FOO));
    }

    @Test
    public void testRawAuxiliaryTypes() throws Exception {
        assertThat(dynamicType.getRawAuxiliaryTypes().size(), is(1));
        assertThat(dynamicType.getRawAuxiliaryTypes().get(BAR), is(auxiliaryTypeByte));
    }

    @Test
    public void testTypeInitializersNotAlive() throws Exception {
        assertThat(dynamicType.hasAliveTypeInitializers(), is(false));
    }

    @Test
    public void testTypeInitializersAliveMain() throws Exception {
        when(mainTypeInitializer.isAlive()).thenReturn(true);
        assertThat(dynamicType.hasAliveTypeInitializers(), is(true));
    }

    @Test
    public void testTypeInitializersAliveAuxiliary() throws Exception {
        when(auxiliaryTypeInitializer.isAlive()).thenReturn(true);
        assertThat(dynamicType.hasAliveTypeInitializers(), is(true));
    }

    @Test
    public void testTypeInitializers() throws Exception {
        assertThat(dynamicType.getTypeInitializers().size(), is(2));
        assertThat(dynamicType.getTypeInitializers().get(FOO), is(mainTypeInitializer));
        assertThat(dynamicType.getTypeInitializers().get(BAR), is(auxiliaryTypeInitializer));
    }

    @Test
    public void testFileSaving() throws Exception {
        File folder = makeTemporaryFolder();
        try {
            File mainFile = dynamicType.saveIn(folder);
            assertThat(mainFile, equalTo(new File(folder, FOO + DOT_CLASS)));
            assertThat(folder.list().length, is(1));
            assertEqualsAndDelete(mainFile, typeByte);
        } finally {
            assertThat(folder.delete(), is(true));
        }
        verify(auxiliaryType).saveIn(folder);
    }

    private static void assertEqualsAndDelete(File file, byte[] binaryRepresentation) throws IOException {
        assertThat(file.isFile(), is(true));
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            byte[] buffer = new byte[binaryRepresentation.length + 1];
            assertThat(fileInputStream.read(buffer), is(binaryRepresentation.length));
            for (int index = 0; index < binaryRepresentation.length; index++) {
                assertThat(buffer[index], equalTo(binaryRepresentation[index]));
            }
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
}
