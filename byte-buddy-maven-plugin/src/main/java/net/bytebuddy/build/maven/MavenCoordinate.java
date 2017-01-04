package net.bytebuddy.build.maven;

import lombok.EqualsAndHashCode;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * A Maven coordinate.
 */
@EqualsAndHashCode
public class MavenCoordinate {

    /**
     * The project's group id.
     */
    private final String groupId;

    /**
     * The project's artifact id.
     */
    private final String artifactId;

    /**
     * The project's version.
     */
    private final String version;

    /**
     * Creates a new Maven coordinate.
     *
     * @param groupId    The project's group id.
     * @param artifactId The project's artifact id.
     * @param version    The project's version.
     */
    protected MavenCoordinate(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    /**
     * Returns this coordinate as a jar-file {@link Artifact}.
     *
     * @return An artifact representation of this coordinate.
     */
    public Artifact asArtifact() {
        return new DefaultArtifact(groupId, artifactId, "jar", version);
    }

    @Override
    public String toString() {
        return "MavenCoordinate{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
