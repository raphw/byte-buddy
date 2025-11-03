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
package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.nullability.MaybeNull;

/**
 * A plugin that includes a version number in the declared module-info class.
 */
@HashCodeAndEqualsPlugin.Enhance
public class ModuleVersionPlugin implements Plugin, Plugin.Factory {

    /**
     * The version to include or {@code null} to not include a version.
     */
    @MaybeNull
    @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
    private final String version;

    /**
     * Creates a new module version plugin.
     *
     * @param version The version to include or {@code null} to not include a version.
     */
    public ModuleVersionPlugin(@MaybeNull String version) {
        this.version = version;
    }

    /**
     * {@inheritDoc}
     */
    public Plugin make() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        return builder.adjustModule().version(version);
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(TypeDescription target) {
        return target.isModuleType();
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        /* do nothing */
    }
}
