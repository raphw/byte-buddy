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
package net.bytebuddy.plugin.gradle.test.jar;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.build.AndroidDescriptor;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * A sample plugin for instrumenting a jar file.
 */
public class PluginForLocalClass implements Plugin {

    /**
     * The Android descriptor.
     */
    private final AndroidDescriptor androidDescriptor;

    /**
     * Creates a new plugin.
     *
     * @param androidDescriptor The Android descriptor.
     */
    public PluginForLocalClass(AndroidDescriptor androidDescriptor) {
        this.androidDescriptor = androidDescriptor;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(TypeDescription typeDescription) {
        return typeDescription.getSimpleName().contains("Some") && androidDescriptor.getTypeScope(typeDescription).equals(AndroidDescriptor.TypeScope.LOCAL);
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        return builder.visit(Advice.to(AdviceForLocalClass.class).on(ElementMatchers.named("method")));
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        /* do nothing */
    }
}