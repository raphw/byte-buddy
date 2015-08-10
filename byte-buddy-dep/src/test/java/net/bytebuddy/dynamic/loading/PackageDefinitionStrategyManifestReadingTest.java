package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class PackageDefinitionStrategyManifestReadingTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private PackageDefinitionStrategy.ManifestReading.SealBaseLocator sealBaseLocator;

    @Mock
    private ClassLoader classLoader;

    private URL url;

    @Before
    public void setUp() throws Exception {
        url = new URL("file:/foo");
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
        verifyZeroInteractions(sealBaseLocator);
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
        when(classLoader.getResourceAsStream("/META-INF/MANIFEST.MF")).then(new Answer<InputStream>() {
            @Override
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
        verifyZeroInteractions(sealBaseLocator);
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
        when(classLoader.getResourceAsStream("/META-INF/MANIFEST.MF")).then(new Answer<InputStream>() {
            @Override
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
        verifyZeroInteractions(sealBaseLocator);
    }

    @Test
    @IntegrationRule.Enforce
    public void testManifestMainAttributesSealed() throws Exception {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.SEALED, Boolean.TRUE.toString());
        when(classLoader.getResourceAsStream("/META-INF/MANIFEST.MF")).then(new Answer<InputStream>() {
            @Override
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
        when(classLoader.getResourceAsStream("/META-INF/MANIFEST.MF")).then(new Answer<InputStream>() {
            @Override
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
        URL url = new URL("file:/foo");
        when(classLoader.getResource(FOO + "/" + BAR + ".class")).thenReturn(url);
        assertThat(new PackageDefinitionStrategy.ManifestReading.SealBaseLocator.ForTypeResourceUrl(sealBaseLocator)
                .findSealBase(classLoader, FOO + "." + BAR), is(url));
        verifyZeroInteractions(sealBaseLocator);
    }

    @Test
    @IntegrationRule.Enforce
    public void testSealBaseLocatorForTypeResourceUrlJarUrl() throws Exception {
        URL url = new URL("jar:file:/foo.jar!/bar");
        when(classLoader.getResource(FOO + "/" + BAR + ".class")).thenReturn(url);
        assertThat(new PackageDefinitionStrategy.ManifestReading.SealBaseLocator.ForTypeResourceUrl(sealBaseLocator)
                .findSealBase(classLoader, FOO + "." + BAR), is(new URL("file:/foo.jar")));
        verifyZeroInteractions(sealBaseLocator);
    }

    @Test
    @IntegrationRule.Enforce
    @Ignore("Cannot yet determine tests specific to Java 9")
    public void testSealBaseLocatorForTypeResourceUrlJavaRuntimeImageUrl() throws Exception {
        URL url = new URL("jrt:/foo/bar");
        when(classLoader.getResource(FOO + "/" + BAR + ".class")).thenReturn(url);
        assertThat(new PackageDefinitionStrategy.ManifestReading.SealBaseLocator.ForTypeResourceUrl(sealBaseLocator)
                .findSealBase(classLoader, FOO + "." + BAR), is(new URL("jrt:/foo")));
        verifyZeroInteractions(sealBaseLocator);
    }

    @Test
    @IntegrationRule.Enforce
    @Ignore("Cannot yet determine tests specific to Java 9")
    public void testSealBaseLocatorForTypeResourceUrlJavaRuntimeImageUrlRawModule() throws Exception {
        URL url = new URL("jrt:/foo");
        when(classLoader.getResource(FOO + "/" + BAR + ".class")).thenReturn(url);
        assertThat(new PackageDefinitionStrategy.ManifestReading.SealBaseLocator.ForTypeResourceUrl(sealBaseLocator)
                .findSealBase(classLoader, FOO + "." + BAR), is(new URL("jrt:/foo")));
        verifyZeroInteractions(sealBaseLocator);
    }

    @Test
    @IntegrationRule.Enforce
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(PackageDefinitionStrategy.ManifestReading.class).apply();
        ObjectPropertyAssertion.of(PackageDefinitionStrategy.ManifestReading.SealBaseLocator.NonSealing.class).apply();
        final Iterator<URL> urls = Arrays.asList(new URL("file://foo"), new URL("file://bar")).iterator();
        ObjectPropertyAssertion.of(PackageDefinitionStrategy.ManifestReading.SealBaseLocator.ForFixedValue.class)
                .create(new ObjectPropertyAssertion.Creator<URL>() {
                    @Override
                    public URL create() {
                        return urls.next();
                    }
                }).apply();
        ObjectPropertyAssertion.of(PackageDefinitionStrategy.ManifestReading.SealBaseLocator.ForTypeResourceUrl.class).apply();
    }
}
