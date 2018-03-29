package net.bytebuddy.build.maven;

import org.eclipse.aether.artifact.Artifact;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MavenCoordinateTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", JAR = "jar";

    @Test
    public void testAsArtifact() throws Exception {
        Artifact artifact = new MavenCoordinate(FOO, BAR, QUX, JAR).asArtifact();
        assertThat(artifact.getGroupId(), is(FOO));
        assertThat(artifact.getArtifactId(), is(BAR));
        assertThat(artifact.getVersion(), is(QUX));
        assertThat(artifact.getExtension(), is(JAR));
    }
}
