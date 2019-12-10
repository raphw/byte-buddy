import groovy.transform.CompileStatic
import org.gradle.api.JavaVersion

@CompileStatic
class VersionHelper {
    private final JavaVersion current

    VersionHelper() {
        // I'm not convinced all those hacks are necessary but I copied them
        def raw = System.getProperty("java.version")
        if (!raw.startsWith("1.") && raw.contains(".")) {
            current = JavaVersion.toVersion(raw.substring(0, raw.indexOf('.')))
        } else {
            current = JavaVersion.toVersion(raw)
        }
    }

    JavaVersion getVersionFor(String sysProp) {
        def sourceVersion = System.getProperty("net.bytebuddy.gradle.version.source")
        if (sourceVersion != null) {
            return JavaVersion.toVersion(sourceVersion)
        } else if (current > JavaVersion.VERSION_1_9) {
            return JavaVersion.VERSION_1_7
        } else if (current > JavaVersion.VERSION_1_8) {
            return JavaVersion.VERSION_1_6
        }
        return JavaVersion.VERSION_1_5
    }
}