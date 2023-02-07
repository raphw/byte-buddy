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
package net.bytebuddy.matcher;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;

/**
 * A matcher to consider if a class file version reaches a given boundary.
 *
 * @param <T> The exact type of the type description that is matched.
 */
@HashCodeAndEqualsPlugin.Enhance
public class ClassFileVersionMatcher<T extends TypeDescription> extends ElementMatcher.Junction.ForNonNullValues<T> {

    /**
     * The targeted class file version.
     */
    private final ClassFileVersion classFileVersion;

    /**
     * {@code true} if the targeted class file version should be at most of the supplied version.
     */
    private final boolean atMost;

    /**
     * Creates a class file version matcher.
     *
     * @param classFileVersion The targeted class file version.
     * @param atMost           {@code true} if the targeted class file version should be at most of the supplied version.
     */
    public ClassFileVersionMatcher(ClassFileVersion classFileVersion, boolean atMost) {
        this.classFileVersion = classFileVersion;
        this.atMost = atMost;
    }

    @Override
    protected boolean doMatch(T target) {
        ClassFileVersion classFileVersion = target.getClassFileVersion();
        return classFileVersion != null && (atMost ? classFileVersion.isAtMost(this.classFileVersion) : classFileVersion.isAtLeast(this.classFileVersion));
    }

    @Override
    public String toString() {
        return "hasClassFileVersion(at " + (atMost ? "most" : "least") + " " + classFileVersion + ")";
    }
}
