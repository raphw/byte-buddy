/*
 * Copyright 2014 - Present Rafael Winterhalter
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
import net.bytebuddy.utility.nullability.MaybeNull;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Defines a configuration for a Maven build's type transformation.
 */
@SuppressFBWarnings(value = "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", justification = "Written to by Maven.")
public class Initialization extends CoordinateConfiguration {

    /**
     * The fully-qualified name of the entry point or any constant name of {@link EntryPoint.Default}.
     */
    @MaybeNull
    public String entryPoint;

    /**
     * If validation should be disabled for the entry point.
     */
    public boolean validated;

    /**
     * Creates a new initialization configuration.
     */
    public Initialization() {
        entryPoint = EntryPoint.Default.REBASE.name();
        validated = true;
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
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should always be wrapped for clarity.")
    public EntryPoint getEntryPoint(ClassLoaderResolver classLoaderResolver, String groupId, String artifactId, String version, String packaging) throws MojoExecutionException {
        if (entryPoint == null || entryPoint.length() == 0) {
            throw new MojoExecutionException("Entry point name is not defined");
        }
        for (EntryPoint.Default entryPoint : EntryPoint.Default.values()) {
            if (this.entryPoint.equals(entryPoint.name())) {
                return validated
                        ? entryPoint
                        : new EntryPoint.Unvalidated(entryPoint);
            }
        }
        try {
            EntryPoint entryPoint = (EntryPoint) Class.forName(this.entryPoint, false, classLoaderResolver.resolve(asCoordinate(groupId, artifactId, version, packaging)))
                    .getDeclaredConstructor()
                    .newInstance();
            return validated
                    ? entryPoint
                    : new EntryPoint.Unvalidated(entryPoint);
        } catch (Exception exception) {
            throw new MojoExecutionException("Cannot create entry point: " + entryPoint, exception);
        }
    }
}
