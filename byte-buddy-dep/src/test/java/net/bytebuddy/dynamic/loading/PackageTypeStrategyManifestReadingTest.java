package net.bytebuddy.dynamic.loading;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PackageTypeStrategyManifestReadingTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private PackageDefinitionStrategy.ManifestReading.SealBaseLocator sealBaseLocator;

    @Mock
    private ClassLoader classLoader;

    private URL url;

    @Before
    public void setUp() throws Exception {
        url = URI.create("file:/foo").toURL();
    }

    @Test
    public void testNoManifest() throws Exception {
        PackageDefinitionStrategy packageDefinitionStrategy = new PackageDefinitionStrategy.ManifestReading(sealBaseLocator);
        PackageDefinitionStrategy.Definition definition = packageDefinitionStrategy.define(classLoader, FOO, BAR);
        assertThat(definition.isDefined(), is(true));
        assertThat(definition.getImplementationTitle(), nullValue(String.class));
        assertThat(definition.getImplementationVersion(), nullValue(String.class));
        assertThat(definition.getImplementationVendor(), nullValue(String.class));
        assertThat(definition.getSpecificationTitle(), nullValue(String.class));
        assertThat(definition.getSpecificationVersion(), nullValue(String.class));
        assertThat(definition.getSpecificationVendor(), nullValue(String.class));
        assertThat(definition.getSealBase(), nullValue(URL.class));
        assertThat(definition.isCompatibleTo(getClass().getPackage()), is(true));
        verifyNoMoreInteractions(sealBaseLocator);
    }

    @Test
    public void testManifestMainAttributesNotSealed() throws Exception {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.SPECIFICATION_TITLE, FOO);
        manifest.getMainAttributes().put(Attributes.Name.SPECIFICATION_VERSION, BAR);
        manifest.getMainAttributes().put(Attributes.Name.SPECIFICATION_VENDOR, QUX);
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_TITLE, BAZ);
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, FOO + BAR);
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, QUX + BAZ);
        manifest.getMainAttributes().put(Attributes.Name.SEALED, Boolean.FALSE.toString());
        when(classLoader.getResourceAsStream(JarFile.MANIFEST_NAME)).then(new Answer<InputStream>() {
            public InputStream answer(InvocationOnMock invocationOnMock) throws Throwable {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                manifest.write(outputStream);
                return new ByteArrayInputStream(outputStream.toByteArray());
            }
        });
        PackageDefinitionStrategy packageDefinitionStrategy = new PackageDefinitionStrategy.ManifestReading(sealBaseLocator);
        PackageDefinitionStrategy.Definition definition = packageDefinitionStrategy.define(classLoader, FOO + "." + BAR, FOO + "." + BAR + "." + QUX);
        assertThat(definition.isDefined(), is(true));
        assertThat(definition.getSpecificationTitle(), is(FOO));
        assertThat(definition.getSpecificationVersion(), is(BAR));
        assertThat(definition.getSpecificationVendor(), is(QUX));
        assertThat(definition.getImplementationTitle(), is(BAZ));
        assertThat(definition.getImplementationVersion(), is(FOO + BAR));
        assertThat(definition.getImplementationVendor(), is(QUX + BAZ));
        assertThat(definition.getSealBase(), nullValue(URL.class));
        assertThat(definition.isCompatibleTo(getClass().getPackage()), is(true));
        verifyNoMoreInteractions(sealBaseLocator);
    }

    @Test
    public void testManifestPackageAttributesNotSealed() throws Exception {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.SPECIFICATION_TITLE, FOO + QUX);
        manifest.getMainAttributes().put(Attributes.Name.SPECIFICATION_VERSION, FOO + QUX);
        manifest.getMainAttributes().put(Attributes.Name.SPECIFICATION_VENDOR, FOO + QUX);
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_TITLE, FOO + QUX);
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, FOO + QUX);
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, FOO + QUX);
        manifest.getMainAttributes().put(Attributes.Name.SEALED, FOO + QUX);
        manifest.getEntries().put(FOO + "/" + BAR + "/", new Attributes());
        manifest.getAttributes(FOO + "/" + BAR + "/").put(Attributes.Name.SPECIFICATION_TITLE, FOO);
        manifest.getAttributes(FOO + "/" + BAR + "/").put(Attributes.Name.SPECIFICATION_VERSION, BAR);
        manifest.getAttributes(FOO + "/" + BAR + "/").put(Attributes.Name.SPECIFICATION_VENDOR, QUX);
        manifest.getAttributes(FOO + "/" + BAR + "/").put(Attributes.Name.IMPLEMENTATION_TITLE, BAZ);
        manifest.getAttributes(FOO + "/" + BAR + "/").put(Attributes.Name.IMPLEMENTATION_VERSION, FOO + BAR);
        manifest.getAttributes(FOO + "/" + BAR + "/").put(Attributes.Name.IMPLEMENTATION_VENDOR, QUX + BAZ);
        manifest.getAttributes(FOO + "/" + BAR + "/").put(Attributes.Name.SEALED, Boolean.FALSE.toString());
        when(classLoader.getResourceAsStream(JarFile.MANIFEST_NAME)).then(new Answer<InputStream>() {
            public InputStream answer(InvocationOnMock invocationOnMock) throws Throwable {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                manifest.write(outputStream);
                return new ByteArrayInputStream(outputStream.toByteArray());
            }
        });
        PackageDefinitionStrategy packageDefinitionStrategy = new PackageDefinitionStrategy.ManifestReading(sealBaseLocator);
        PackageDefinitionStrategy.Definition definition = packageDefinitionStrategy.define(classLoader, FOO + "." + BAR, FOO + "." + BAR + "." + QUX);
        assertThat(definition.isDefined(), is(true));
        assertThat(definition.getSpecificationTitle(), is(FOO));
        assertThat(definition.getSpecificationVersion(), is(BAR));
        assertThat(definition.getSpecificationVendor(), is(QUX));
        assertThat(definition.getImplementationTitle(), is(BAZ));
        assertThat(definition.getImplementationVersion(), is(FOO + BAR));
        assertThat(definition.getImplementationVendor(), is(QUX + BAZ));
        assertThat(definition.getSealBase(), nullValue(URL.class));
        assertThat(definition.isCompatibleTo(getClass().getPackage()), is(true));
        verifyNoMoreInteractions(sealBaseLocator);
    }

    @Test
    @IntegrationRule.Enforce
    public void testManifestMainAttributesSealed() throws Exception {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.SEALED, Boolean.TRUE.toString());
        when(classLoader.getResourceAsStream(JarFile.MANIFEST_NAME)).then(new Answer<InputStream>() {
            public InputStream answer(InvocationOnMock invocationOnMock) throws Throwable {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                manifest.write(outputStream);
                return new ByteArrayInputStream(outputStream.toByteArray());
            }
        });
        when(sealBaseLocator.findSealBase(classLoader, FOO + "." + BAR + "." + QUX)).thenReturn(url);
        PackageDefinitionStrategy packageDefinitionStrategy = new PackageDefinitionStrategy.ManifestReading(sealBaseLocator);
        PackageDefinitionStrategy.Definition definition = packageDefinitionStrategy.define(classLoader, FOO + "." + BAR, FOO + "." + BAR + "." + QUX);
        assertThat(definition.isDefined(), is(true));
        assertThat(definition.getSealBase(), is(url));
        verify(sealBaseLocator).findSealBase(classLoader, FOO + "." + BAR + "." + QUX);
        verifyNoMoreInteractions(sealBaseLocator);
    }

    @Test
    @IntegrationRule.Enforce
    public void testManifestPackageAttributesSealed() throws Exception {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getEntries().put(FOO + "/" + BAR + "/", new Attributes());
        manifest.getAttributes(FOO + "/" + BAR + "/").put(Attributes.Name.SEALED, Boolean.TRUE.toString());
        when(classLoader.getResourceAsStream(JarFile.MANIFEST_NAME)).then(new Answer<InputStream>() {
            public InputStream answer(InvocationOnMock invocationOnMock) throws Throwable {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                manifest.write(outputStream);
                return new ByteArrayInputStream(outputStream.toByteArray());
            }
        });
        when(sealBaseLocator.findSealBase(classLoader, FOO + "." + BAR + "." + QUX)).thenReturn(url);
        PackageDefinitionStrategy packageDefinitionStrategy = new PackageDefinitionStrategy.ManifestReading(sealBaseLocator);
        PackageDefinitionStrategy.Definition definition = packageDefinitionStrategy.define(classLoader, FOO + "." + BAR, FOO + "." + BAR + "." + QUX);
        assertThat(definition.isDefined(), is(true));
        assertThat(definition.getSealBase(), is(url));
        verify(sealBaseLocator).findSealBase(classLoader, FOO + "." + BAR + "." + QUX);
        verifyNoMoreInteractions(sealBaseLocator);
    }

    @Test
    public void testSealBaseLocatorNonSealing() throws Exception {
        assertThat(PackageDefinitionStrategy.ManifestReading.SealBaseLocator.NonSealing.INSTANCE.findSealBase(classLoader, FOO), nullValue(URL.class));
    }

    @Test
    @IntegrationRule.Enforce
    public void testSealBaseLocatorForFixedValue() throws Exception {
        assertThat(new PackageDefinitionStrategy.ManifestReading.SealBaseLocator.ForFixedValue(url).findSealBase(classLoader, FOO), is(url));
    }

    @Test
    @IntegrationRule.Enforce
    public void testSealBaseLocatorForTypeResourceUrlUnknownUrl() throws Exception {
        when(sealBaseLocator.findSealBase(classLoader, FOO + "." + BAR)).thenReturn(url);
        assertThat(new PackageDefinitionStrategy.ManifestReading.SealBaseLocator.ForTypeResourceUrl(sealBaseLocator)
                .findSealBase(classLoader, FOO + "." + BAR), is(url));
        verify(sealBaseLocator).findSealBase(classLoader, FOO + "." + BAR);
        verifyNoMoreInteractions(sealBaseLocator);
    }

    @Test
    @IntegrationRule.Enforce
    public void testSealBaseLocatorForTypeResourceUrlFileUrl() throws Exception {
        URL url = URI.create("file:/foo").toURL();
        when(classLoader.getResource(FOO + "/" + BAR + ClassFileLocator.CLASS_FILE_EXTENSION)).thenReturn(url);
        assertThat(new PackageDefinitionStrategy.ManifestReading.SealBaseLocator.ForTypeResourceUrl(sealBaseLocator)
                .findSealBase(classLoader, FOO + "." + BAR), is(url));
        verifyNoMoreInteractions(sealBaseLocator);
    }

    @Test
    @IntegrationRule.Enforce
    public void testSealBaseLocatorForTypeResourceUrlJarUrl() throws Exception {
        URL url = URI.create("jar:file:/foo.jar!/bar").toURL();
        when(classLoader.getResource(FOO + "/" + BAR + ClassFileLocator.CLASS_FILE_EXTENSION)).thenReturn(url);
        assertThat(new PackageDefinitionStrategy.ManifestReading.SealBaseLocator.ForTypeResourceUrl(sealBaseLocator)
                .findSealBase(classLoader, FOO + "." + BAR), is(URI.create("file:/foo.jar").toURL()));
        verifyNoMoreInteractions(sealBaseLocator);
    }

    @Test
    @IntegrationRule.Enforce
    @JavaVersionRule.Enforce(9)
    public void testSealBaseLocatorForTypeResourceUrlJavaRuntimeImageUrl() throws Exception {
        URL url = URI.create("jrt:/foo/bar").toURL();
        when(classLoader.getResource(FOO + "/" + BAR + ClassFileLocator.CLASS_FILE_EXTENSION)).thenReturn(url);
        assertThat(new PackageDefinitionStrategy.ManifestReading.SealBaseLocator.ForTypeResourceUrl(sealBaseLocator)
                .findSealBase(classLoader, FOO + "." + BAR), is(URI.create("jrt:/foo").toURL()));
        verifyNoMoreInteractions(sealBaseLocator);
    }

    @Test
    @IntegrationRule.Enforce
    @JavaVersionRule.Enforce(9)
    public void testSealBaseLocatorForTypeResourceUrlJavaRuntimeImageUrlRawModule() throws Exception {
        URL url = URI.create("jrt:/foo").toURL();
        when(classLoader.getResource(FOO + "/" + BAR + ClassFileLocator.CLASS_FILE_EXTENSION)).thenReturn(url);
        assertThat(new PackageDefinitionStrategy.ManifestReading.SealBaseLocator.ForTypeResourceUrl(sealBaseLocator)
                .findSealBase(classLoader, FOO + "." + BAR), is(URI.create("jrt:/foo").toURL()));
        verifyNoMoreInteractions(sealBaseLocator);
    }
}
