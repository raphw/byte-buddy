package net.bytebuddy.dynamic.loading;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PackageDefinitionStrategyTypeSimpleTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    private PackageDefinitionStrategy.Definition definition;

    private URL sealBase;

    @Before
    public void setUp() throws Exception {
        sealBase = new URL("file://foo");
        definition = new PackageDefinitionStrategy.Definition.Simple(FOO, BAR, QUX, BAZ, FOO + BAR, QUX + BAZ, sealBase);
    }

    @Test
    public void testIsDefined() throws Exception {
        assertThat(definition.isDefined(), is(true));
    }

    @Test
    public void testSpecificationTitle() throws Exception {
        assertThat(definition.getSpecificationTitle(), is(FOO));
    }

    @Test
    public void testSpecificationVersion() throws Exception {
        assertThat(definition.getSpecificationVersion(), is(BAR));
    }

    @Test
    public void testSpecificationVendor() throws Exception {
        assertThat(definition.getSpecificationVendor(), is(QUX));
    }

    @Test
    public void testImplementationTitle() throws Exception {
        assertThat(definition.getImplementationTitle(), is(BAZ));
    }

    @Test
    public void testImplementationVersion() throws Exception {
        assertThat(definition.getImplementationVersion(), is(FOO + BAR));
    }

    @Test
    public void testImplementationVendor() throws Exception {
        assertThat(definition.getImplementationVendor(), is(QUX + BAZ));
    }

    @Test
    @IntegrationRule.Enforce
    public void testSealBase() throws Exception {
        assertThat(definition.getSealBase(), is(sealBase));
    }

    @Test
    public void testSealedNotCompatibleToUnsealed() throws Exception {
        assertThat(definition.isCompatibleTo(getClass().getPackage()), is(false));
    }

    @Test
    public void testNonSealedIsCompatibleToUnsealed() throws Exception {
        assertThat(new PackageDefinitionStrategy.Definition.Simple(FOO, BAR, QUX, BAZ, FOO + BAR, QUX + BAZ, null)
                .isCompatibleTo(getClass().getPackage()), is(true));
    }

    @Test
    public void testNonSealedIsCompatibleToSealed() throws Exception {
        File file = File.createTempFile(FOO, BAR);
        try {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(Attributes.Name.SEALED, Boolean.TRUE.toString());
            URL url = new ByteBuddy().subclass(Object.class).name("foo.Bar").make().toJar(file, manifest).toURI().toURL();
            ClassLoader classLoader = new URLClassLoader(new URL[]{url}, null);
            Package definedPackage = classLoader.loadClass("foo.Bar").getPackage();
            assertThat(new PackageDefinitionStrategy.Definition.Simple(FOO, BAR, QUX, BAZ, FOO + BAR, QUX + BAZ, null)
                    .isCompatibleTo(definedPackage), is(false));
        } finally {
            file.deleteOnExit();
        }
    }

    @Test
    public void testSealedIsCompatibleToSealed() throws Exception {
        File file = File.createTempFile(FOO, BAR);
        try {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(Attributes.Name.SEALED, Boolean.TRUE.toString());
            URL url = new ByteBuddy().subclass(Object.class).name("foo.Bar").make().toJar(file, manifest).toURI().toURL();
            ClassLoader classLoader = new URLClassLoader(new URL[]{url}, null);
            Package definedPackage = classLoader.loadClass("foo.Bar").getPackage();
            assertThat(new PackageDefinitionStrategy.Definition.Simple(FOO, BAR, QUX, BAZ, FOO + BAR, QUX + BAZ, url)
                    .isCompatibleTo(definedPackage), is(true));
        } finally {
            file.deleteOnExit();
        }
    }

    @Test
    @IntegrationRule.Enforce
    public void testObjectProperties() throws Exception {
        final Iterator<URL> urls = Arrays.asList(new URL("file:/foo"), new URL("file:/bar")).iterator();
        ObjectPropertyAssertion.of(PackageDefinitionStrategy.Definition.Simple.class).create(new ObjectPropertyAssertion.Creator<URL>() {
            @Override
            public URL create() {
                return urls.next();
            }
        }).apply();
    }
}
