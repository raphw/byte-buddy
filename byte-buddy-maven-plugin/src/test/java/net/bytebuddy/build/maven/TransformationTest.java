package net.bytebuddy.build.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TransformationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz", JAR = "jar";

    @Test
    public void testResolved() throws Exception {
        Transformation transformation = new Transformation();
        transformation.plugin = FOO;
        transformation.groupId = BAR;
        transformation.artifactId = QUX;
        transformation.version = BAZ;
        transformation.packaging = JAR;
        assertThat(transformation.getPlugin(), is(FOO));
        assertThat(transformation.getRawPlugin(), is(FOO));
        assertThat(transformation.getGroupId(FOO), is(BAR));
        assertThat(transformation.getArtifactId(FOO), is(QUX));
        assertThat(transformation.getVersion(FOO), is(BAZ));
        assertThat(transformation.getPackaging(JAR), is(JAR));
    }

    @Test
    public void testUndefined() throws Exception {
        Transformation transformation = new Transformation();
        assertThat(transformation.getGroupId(BAR), is(BAR));
        assertThat(transformation.getArtifactId(QUX), is(QUX));
        assertThat(transformation.getVersion(BAZ), is(BAZ));
        assertThat(transformation.getPackaging(JAR), is(JAR));
    }

    @Test
    public void testEmpty() throws Exception {
        Transformation transformation = new Transformation();
        transformation.groupId = "";
        transformation.artifactId = "";
        transformation.version = "";
        transformation.packaging = "";
        assertThat(transformation.getGroupId(BAR), is(BAR));
        assertThat(transformation.getArtifactId(QUX), is(QUX));
        assertThat(transformation.getVersion(BAZ), is(BAZ));
        assertThat(transformation.getPackaging(JAR), is(JAR));
    }

    @Test(expected = MojoExecutionException.class)
    public void testUndefinedName() throws Exception {
        new Transformation().getPlugin();
    }

    @Test
    public void testAsCoordinateResolved() throws Exception {
        Transformation transformation = new Transformation();
        transformation.groupId = BAR;
        transformation.artifactId = QUX;
        transformation.version = BAZ;
        assertThat(transformation.asCoordinate(FOO, FOO, FOO, JAR), is(new MavenCoordinate(BAR, QUX, BAZ, JAR)));
    }

    @Test
    public void testAsCoordinateUnresolved() throws Exception {
        Transformation transformation = new Transformation();
        assertThat(transformation.asCoordinate(BAR, QUX, BAZ, JAR), is(new MavenCoordinate(BAR, QUX, BAZ, JAR)));
    }
}
