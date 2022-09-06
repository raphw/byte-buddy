package net.bytebuddy.build.gradle.android;

import com.android.build.api.instrumentation.InstrumentationParameters;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

/**
 * The parameters provided to the Byte Buddy instrumentation.
 */
public interface ByteBuddyInstrumentationParameters extends InstrumentationParameters {

    /**
     * Returns the boot class path of Android.
     *
     * @return The boot class path of Android.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    ConfigurableFileCollection getAndroidBootClasspath();

    /**
     * Returns Byte Buddy's class path.
     *
     * @return Byte Buddy's class path.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    ConfigurableFileCollection getByteBuddyClasspath();

    /**
     * Returns the runtime class path.
     *
     * @return The runtime class path.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    ConfigurableFileCollection getRuntimeClasspath();

    /**
     * Returns the local classes' directories.
     *
     * @return The local classes' directories.
     */
    @Classpath
    ConfigurableFileCollection getLocalClassesDirectories();

    /**
     * Returns the Byte Buddy service to use.
     *
     * @return The Byte Buddy service to use.
     */
    @Internal
    Property<ByteBuddyService> getByteBuddyService();
}
