package net.bytebuddy.build.gradle.android.connector.adapter.legacy;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.BaseExtension;
import net.bytebuddy.build.gradle.android.connector.adapter.TransformationAdapter;
import net.bytebuddy.build.gradle.android.connector.adapter.legacy.transform.LegacyAndroidAppTransform;
import net.bytebuddy.build.gradle.android.transformation.AndroidTransformation;
import org.gradle.api.artifacts.Configuration;

/**
 * Adapter used when the host android project uses Android Gradle plugin version < 7.2
 */
public class LegacyAdapter implements TransformationAdapter {

    private final BaseExtension androidExtension;
    private final Configuration byteBuddyDependenciesConfiguration;

    public LegacyAdapter(BaseExtension androidExtension, Configuration byteBuddyDependenciesConfiguration) {
        this.androidExtension = androidExtension;
        this.byteBuddyDependenciesConfiguration = byteBuddyDependenciesConfiguration;
    }

    @Override
    public void adapt(AndroidTransformation transformation) {
        androidExtension.registerTransform(
                new LegacyAndroidAppTransform(
                        transformation,
                        (AppExtension) androidExtension,
                        byteBuddyDependenciesConfiguration
                )
        );
    }
}