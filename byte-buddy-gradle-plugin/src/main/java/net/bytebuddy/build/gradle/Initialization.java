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
package net.bytebuddy.build.gradle;

import net.bytebuddy.build.EntryPoint;
import org.gradle.api.GradleException;

import java.io.File;

/**
 * Defines an entry point for a Byte Buddy transformation in a Gradle build.
 */
public class Initialization extends AbstractUserConfiguration {

    /**
     * The fully-qualified name of the entry point or any constant name of {@link EntryPoint.Default}.
     */
    private String entryPoint;

    /**
     * Creates a default initialization instance.
     *
     * @return A default initialization instance.
     */
    public static Initialization makeDefault() {
        Initialization initialization = new Initialization();
        initialization.setEntryPoint(EntryPoint.Default.REBASE.name());
        return initialization;
    }

    /**
     * Sets the default entry point or any constant name of {@link net.bytebuddy.build.EntryPoint.Default}.
     *
     * @param entryPoint The default entry point or any constant name of {@link net.bytebuddy.build.EntryPoint.Default}.
     */
    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    /**
     * Resolves this initialization to an entry point instance.
     *
     * @param classLoaderResolver The class loader resolver to use if appropriate.
     * @param root                The root file describing the current tasks classes.
     * @param classPath           The class path of the current task.
     * @return A resolved entry point.
     */
    public EntryPoint getEntryPoint(ClassLoaderResolver classLoaderResolver, File root, Iterable<? extends File> classPath) {
        if (entryPoint == null || entryPoint.length() == 0) {
            throw new GradleException("Entry point name is not defined");
        }
        for (EntryPoint.Default entryPoint : EntryPoint.Default.values()) {
            if (this.entryPoint.equals(entryPoint.name())) {
                return entryPoint;
            }
        }
        try {
            return (EntryPoint) Class.forName(entryPoint, false, classLoaderResolver.resolve(getClassPath(root, classPath)))
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception exception) {
            throw new GradleException("Cannot create entry point: " + entryPoint, exception);
        }
    }
}
