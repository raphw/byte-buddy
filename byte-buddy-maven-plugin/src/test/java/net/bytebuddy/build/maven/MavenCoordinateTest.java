package net.bytebuddy.build.maven;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.eclipse.aether.artifact.Artifact;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MavenCoordinateTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Test
    public void testAsArtifact() throws Exception {
        Artifact artifact = new MavenCoordinate(FOO, BAR, QUX).asArtifact();
        assertThat(artifact.getGroupId(), is(FOO));
        assertThat(artifact.getArtifactId(), is(BAR));
        assertThat(artifact.getVersion(), is(QUX));
        assertThat(artifact.getExtension(), is("jar"));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MavenCoordinate.class).apply();
    }
}
