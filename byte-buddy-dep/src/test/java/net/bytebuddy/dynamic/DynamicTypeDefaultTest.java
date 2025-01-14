package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.RandomString;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.stubbing.Answer;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DynamicTypeDefaultTest {

    private static final String CLASS_FILE_EXTENSION = ClassFileLocator.CLASS_FILE_EXTENSION;

    private static final String FOOBAR = "foo/bar", QUXBAZ = "qux/baz", BARBAZ = "bar/baz", FOO = "foo", BAR = "bar";

    private static final byte[] BINARY_FIRST = new byte[]{1, 2, 3}, BINARY_SECOND = new byte[]{4, 5, 6}, BINARY_THIRD = new byte[]{7, 8, 9};

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private LoadedTypeInitializer mainLoadedTypeInitializer, auxiliaryLoadedTypeInitializer;

    @Mock
    private DynamicType auxiliaryType;

    @Mock
    private TypeDescription typeDescription, auxiliaryTypeDescription;

    private DynamicType dynamicType;

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

    private static void assertJarFile(File file, Manifest manifest, Map<String, byte[]> expectedEntries) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        try {
            JarInputStream jarInputStream = new JarInputStream(inputStream);
            assertThat(jarInputStream.getManifest(), is(manifest));
            JarEntry jarEntry;
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                byte[] binary = expectedEntries.remove(jarEntry.getName());
                assertThat(binary, notNullValue());
                byte[] buffer = new byte[binary.length];
                assertThat(jarInputStream.read(buffer), is(buffer.length));
                assertThat(Arrays.equals(buffer, binary), is(true));
                assertThat(jarInputStream.read(buffer), is(-1));
                jarInputStream.closeEntry();
            }
            assertThat(expectedEntries.size(), is(0));
            jarInputStream.close();
        } finally {
            inputStream.close();
        }
    }

    @Before
    public void setUp() throws Exception {
        dynamicType = new DynamicType.Default(typeDescription,
                BINARY_FIRST,
                mainLoadedTypeInitializer,
                Collections.singletonList(auxiliaryType));
        when(typeDescription.getName()).thenReturn(FOOBAR.replace('/', '.'));
        when(typeDescription.getInternalName()).thenReturn(FOOBAR);
        when(auxiliaryType.saveIn(any(File.class))).thenReturn(Collections.<TypeDescription, File>emptyMap());
        when(auxiliaryTypeDescription.getName()).thenReturn(QUXBAZ.replace('/', '.'));
        when(auxiliaryTypeDescription.getInternalName()).thenReturn(QUXBAZ);
        when(auxiliaryType.getTypeDescription()).thenReturn(auxiliaryTypeDescription);
        when(auxiliaryType.getBytes()).thenReturn(BINARY_SECOND);
        when(auxiliaryType.locate(anyString())).thenAnswer(new Answer<ClassFileLocator.Resolution>() {

            public ClassFileLocator.Resolution answer(InvocationOnMock invocation) {
                return new ClassFileLocator.Resolution.Illegal(invocation.getArgument(0, String.class));
            }
        });
        when(auxiliaryType.locate(QUXBAZ.replace('/', '.'))).thenReturn(new ClassFileLocator.Resolution.Explicit(BINARY_SECOND));
        when(auxiliaryType.getLoadedTypeInitializers()).thenReturn(Collections.singletonMap(auxiliaryTypeDescription, auxiliaryLoadedTypeInitializer));
        when(auxiliaryType.getAuxiliaryTypes()).thenReturn(Collections.<TypeDescription, byte[]>emptyMap());
        when(auxiliaryType.getAllTypes()).thenReturn(Collections.singletonMap(auxiliaryTypeDescription, BINARY_SECOND));
    }

    @Test
    public void testByteArray() throws Exception {
        assertThat(dynamicType.getBytes(), is(BINARY_FIRST));
    }

    @Test
    public void testTypeDescription() throws Exception {
        assertThat(dynamicType.getTypeDescription(), is(typeDescription));
    }

    @Test
    public void testRawAuxiliaryTypes() throws Exception {
        assertThat(dynamicType.getAuxiliaryTypes().size(), is(1));
        assertThat(dynamicType.getAuxiliaryTypes().get(auxiliaryTypeDescription), is(BINARY_SECOND));
    }

    @Test
    public void testTypeInitializersNotAlive() throws Exception {
        assertThat(dynamicType.hasAliveLoadedTypeInitializers(), is(false));
    }

    @Test
    public void testTypeInitializersAliveMain() throws Exception {
        when(mainLoadedTypeInitializer.isAlive()).thenReturn(true);
        assertThat(dynamicType.hasAliveLoadedTypeInitializers(), is(true));
    }

    @Test
    public void testTypeInitializersAliveAuxiliary() throws Exception {
        when(auxiliaryType.hasAliveLoadedTypeInitializers()).thenReturn(true);
        assertThat(dynamicType.hasAliveLoadedTypeInitializers(), is(true));
    }

    @Test
    public void testTypeInitializers() throws Exception {
        assertThat(dynamicType.getLoadedTypeInitializers().size(), is(2));
        assertThat(dynamicType.getLoadedTypeInitializers().get(typeDescription), is(mainLoadedTypeInitializer));
        assertThat(dynamicType.getLoadedTypeInitializers().get(auxiliaryTypeDescription), is(auxiliaryLoadedTypeInitializer));
    }

    @Test
    public void testFileSaving() throws Exception {
        File folder = temporaryFolder.newFolder();
        boolean folderDeletion, fileDeletion;
        try {
            Map<TypeDescription, File> files = dynamicType.saveIn(folder);
            assertThat(files.size(), is(1));
            assertFile(files.get(typeDescription), BINARY_FIRST);
        } finally {
            folderDeletion = new File(folder, FOO).delete();
            fileDeletion = folder.delete();
        }
        assertThat(folderDeletion, is(true));
        assertThat(fileDeletion, is(true));
        verify(auxiliaryType).saveIn(folder);
    }

    @Test
    public void testJarCreation() throws Exception {
        File file = temporaryFolder.newFile();
        assertThat(file.delete(), is(true));
        boolean fileDeletion;
        try {
            assertThat(dynamicType.toJar(file), is(file));
            assertThat(file.exists(), is(true));
            assertThat(file.isFile(), is(true));
            assertThat(file.length() > 0L, is(true));
            Map<String, byte[]> bytes = new HashMap<String, byte[]>();
            bytes.put(FOOBAR + CLASS_FILE_EXTENSION, BINARY_FIRST);
            bytes.put(QUXBAZ + CLASS_FILE_EXTENSION, BINARY_SECOND);
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            assertJarFile(file, manifest, bytes);
        } finally {
            fileDeletion = file.delete();
        }
        assertThat(fileDeletion, is(true));
    }

    @Test
    public void testJarWithExplicitManifestCreation() throws Exception {
        File file = temporaryFolder.newFile();
        assertThat(file.delete(), is(true));
        boolean fileDeletion;
        try {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, BAR);
            assertThat(dynamicType.toJar(file, manifest), is(file));
            assertThat(file.exists(), is(true));
            assertThat(file.isFile(), is(true));
            assertThat(file.length() > 0L, is(true));
            Map<String, byte[]> bytes = new HashMap<String, byte[]>();
            bytes.put(FOOBAR + CLASS_FILE_EXTENSION, BINARY_FIRST);
            bytes.put(QUXBAZ + CLASS_FILE_EXTENSION, BINARY_SECOND);
            assertJarFile(file, manifest, bytes);
        } finally {
            fileDeletion = file.delete();
        }
        assertThat(fileDeletion, is(true));
    }

    @Test
    public void testJarTargetInjection() throws Exception {
        File sourceFile = temporaryFolder.newFile();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, BAR);
        OutputStream outputStream = new FileOutputStream(sourceFile);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(outputStream, manifest);
            jarOutputStream.putNextEntry(new JarEntry(BARBAZ + CLASS_FILE_EXTENSION));
            jarOutputStream.write(BINARY_THIRD);
            jarOutputStream.closeEntry();
            jarOutputStream.putNextEntry(new JarEntry(FOOBAR + CLASS_FILE_EXTENSION));
            jarOutputStream.write(BINARY_THIRD);
            jarOutputStream.closeEntry();
            jarOutputStream.close();
        } finally {
            outputStream.close();
        }
        File file = temporaryFolder.newFile();
        assertThat(file.delete(), is(true));
        boolean fileDeletion;
        try {
            assertThat(dynamicType.inject(sourceFile, file), is(file));
            assertThat(file.exists(), is(true));
            assertThat(file.isFile(), is(true));
            assertThat(file.length() > 0L, is(true));
            Map<String, byte[]> bytes = new HashMap<String, byte[]>();
            bytes.put(FOOBAR + CLASS_FILE_EXTENSION, BINARY_FIRST);
            bytes.put(QUXBAZ + CLASS_FILE_EXTENSION, BINARY_SECOND);
            bytes.put(BARBAZ + CLASS_FILE_EXTENSION, BINARY_THIRD);
            assertJarFile(file, manifest, bytes);
        } finally {
            fileDeletion = file.delete() & sourceFile.delete();
        }
        assertThat(fileDeletion, is(true));
    }

    @Test
    public void testJarSelfInjection() throws Exception {
        File file = temporaryFolder.newFile();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, BAR);
        OutputStream outputStream = new FileOutputStream(file);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(outputStream, manifest);
            jarOutputStream.putNextEntry(new JarEntry(BARBAZ + CLASS_FILE_EXTENSION));
            jarOutputStream.write(BINARY_THIRD);
            jarOutputStream.closeEntry();
            jarOutputStream.putNextEntry(new JarEntry(FOOBAR + CLASS_FILE_EXTENSION));
            jarOutputStream.write(BINARY_THIRD);
            jarOutputStream.closeEntry();
            jarOutputStream.close();
        } finally {
            outputStream.close();
        }
        boolean fileDeletion;
        try {
            assertThat(dynamicType.inject(file), is(file));
            assertThat(file.exists(), is(true));
            assertThat(file.isFile(), is(true));
            assertThat(file.length() > 0L, is(true));
            Map<String, byte[]> bytes = new HashMap<String, byte[]>();
            bytes.put(FOOBAR + CLASS_FILE_EXTENSION, BINARY_FIRST);
            bytes.put(QUXBAZ + CLASS_FILE_EXTENSION, BINARY_SECOND);
            bytes.put(BARBAZ + CLASS_FILE_EXTENSION, BINARY_THIRD);
            assertJarFile(file, manifest, bytes);
        } finally {
            fileDeletion = file.delete();
        }
        assertThat(fileDeletion, is(true));
    }

    @Test
    public void testJarSelfInjectionWithDuplicateSpecification() throws Exception {
        File file = temporaryFolder.newFile();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, BAR);
        OutputStream outputStream = new FileOutputStream(file);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(outputStream, manifest);
            jarOutputStream.putNextEntry(new JarEntry(BARBAZ + CLASS_FILE_EXTENSION));
            jarOutputStream.write(BINARY_THIRD);
            jarOutputStream.closeEntry();
            jarOutputStream.putNextEntry(new JarEntry(FOOBAR + CLASS_FILE_EXTENSION));
            jarOutputStream.write(BINARY_THIRD);
            jarOutputStream.closeEntry();
            jarOutputStream.close();
        } finally {
            outputStream.close();
        }
        boolean fileDeletion;
        try {
            assertThat(dynamicType.inject(file, file), is(file));
            assertThat(file.exists(), is(true));
            assertThat(file.isFile(), is(true));
            assertThat(file.length() > 0L, is(true));
            Map<String, byte[]> bytes = new HashMap<String, byte[]>();
            bytes.put(FOOBAR + CLASS_FILE_EXTENSION, BINARY_FIRST);
            bytes.put(QUXBAZ + CLASS_FILE_EXTENSION, BINARY_SECOND);
            bytes.put(BARBAZ + CLASS_FILE_EXTENSION, BINARY_THIRD);
            assertJarFile(file, manifest, bytes);
        } finally {
            fileDeletion = file.delete();
        }
        assertThat(fileDeletion, is(true));
    }

    @Test
    public void testIterationOrderAll() throws Exception {
        Iterator<TypeDescription> types = dynamicType.getAllTypes().keySet().iterator();
        assertThat(types.hasNext(), is(true));
        assertThat(types.next(), is(typeDescription));
        assertThat(types.hasNext(), is(true));
        assertThat(types.next(), is(auxiliaryTypeDescription));
        assertThat(types.hasNext(), is(false));
    }

    @Test
    public void testIterationOrderAuxiliary() throws Exception {
        Iterator<TypeDescription> types = dynamicType.getAuxiliaryTypes().keySet().iterator();
        assertThat(types.hasNext(), is(true));
        assertThat(types.next(), is(auxiliaryTypeDescription));
        assertThat(types.hasNext(), is(false));
    }

    @Test
    public void testIterationOrderAllDescriptions() throws Exception {
        when(auxiliaryType.getAllTypeDescriptions()).thenReturn(Collections.singleton(auxiliaryTypeDescription));
        Iterator<TypeDescription> types = dynamicType.getAllTypeDescriptions().iterator();
        assertThat(types.hasNext(), is(true));
        assertThat(types.next(), is(typeDescription));
        assertThat(types.hasNext(), is(true));
        assertThat(types.next(), is(auxiliaryTypeDescription));
        assertThat(types.hasNext(), is(false));
    }

    @Test
    public void testIterationOrderAuxiliaryDescriptions() throws Exception {
        when(auxiliaryType.getAllTypeDescriptions()).thenReturn(Collections.singleton(auxiliaryTypeDescription));
        Iterator<TypeDescription> types = dynamicType.getAuxiliaryTypeDescriptions().iterator();
        assertThat(types.hasNext(), is(true));
        assertThat(types.next(), is(auxiliaryTypeDescription));
        assertThat(types.hasNext(), is(false));
    }

    @Test
    public void testClassFileLocator() throws Exception {
        assertThat(dynamicType.locate(FOOBAR.replace('/', '.')).isResolved(), is(true));
        assertThat(dynamicType.locate(QUXBAZ.replace('/', '.')).isResolved(), is(true));
        assertThat(dynamicType.locate(BARBAZ.replace('/', '.')).isResolved(), is(false));
    }
}
