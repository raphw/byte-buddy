package net.bytebuddy.build.maven;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * A Maven coordinate.
 */
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
     * The projects packaging.
     */
    private String packaging;

    /**
     * Creates a new Maven coordinate.
     *
     * @param groupId    The project's group id.
     * @param artifactId The project's artifact id.
     * @param version    The project's version.
     * @param packaging  The project's packaging
     */
    protected MavenCoordinate(String groupId, String artifactId, String version, String packaging) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging;
    }

    /**
     * Returns this coordinate as a jar-file {@link Artifact}.
     *
     * @return An artifact representation of this coordinate.
     */
    public Artifact asArtifact() {
        return new DefaultArtifact(groupId, artifactId, packaging, version);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof MavenCoordinate)) {
            return false;
        }
        MavenCoordinate that = (MavenCoordinate) object;
        return groupId.equals(that.groupId)
                && artifactId.equals(that.artifactId)
                && version.equals(that.version)
                && packaging.equals(that.packaging);
    }

    @Override
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + packaging.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MavenCoordinate{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", packaging='" + packaging + '\'' +
                '}';
    }
}
