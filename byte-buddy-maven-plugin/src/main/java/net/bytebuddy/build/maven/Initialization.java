/*
 * Copyright 2014 - 2018 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.build.maven;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.EntryPoint;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Defines a configuration for a Maven build's type transformation.
 */
@SuppressFBWarnings(value = "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", justification = "Written to by Maven.")
public class Initialization extends AbstractUserConfiguration {

    /**
     * The fully-qualified name of the entry point or any constant name of {@link EntryPoint.Default}.
     */
    public String entryPoint;

    /**
     * Creates a default initialization instance.
     *
     * @return A default initialization instance.
     */
    public static Initialization makeDefault() {
        Initialization initialization = new Initialization();
        initialization.entryPoint = EntryPoint.Default.REBASE.name();
        return initialization;
    }

    /**
     * Resolves the described entry point.
     *
     * @param classLoaderResolver The class loader resolved to use.
     * @param groupId             This project's group id.
     * @param artifactId          This project's artifact id.
     * @param version             This project's version id.
     * @param packaging           This project's packaging
     * @return The resolved entry point.
     * @throws MojoExecutionException If the entry point cannot be created.
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Applies Maven exception wrapper")
    public EntryPoint getEntryPoint(ClassLoaderResolver classLoaderResolver, String groupId, String artifactId, String version, String packaging) throws MojoExecutionException {
        if (entryPoint == null || entryPoint.length() == 0) {
            throw new MojoExecutionException("Entry point name is not defined");
        }
        for (EntryPoint.Default entryPoint : EntryPoint.Default.values()) {
            if (this.entryPoint.equals(entryPoint.name())) {
                return entryPoint;
            }
        }
        try {
            return (EntryPoint) Class.forName(entryPoint, false, classLoaderResolver.resolve(asCoordinate(groupId, artifactId, version, packaging)))
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception exception) {
            throw new MojoExecutionException("Cannot create entry point: " + entryPoint, exception);
        }
    }
}
