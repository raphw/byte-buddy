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
import net.bytebuddy.build.Plugin;

/**
 * Describes an argument to a {@link Plugin} constuctor.
 */
@SuppressFBWarnings(value = "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", justification = "Written to by Maven.")
public class PluginArgument {

    /**
     * The argument index.
     */
    public int index;

    /**
     * The argument value.
     */
    public String value;

    /**
     * Resolves this plugin argument to an argument resolver.
     *
     * @return An argument resolver that represents this plugin argument.
     */
    public Plugin.Factory.UsingReflection.ArgumentResolver toArgumentResolver() {
        return new Plugin.Factory.UsingReflection.ArgumentResolver.ForIndex.WithDynamicType(index, value);
    }
}
