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
package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.DefaultValue;

/**
 * This assigner is able to handle the {@code void} type. This means:
 * <ol>
 * <li>If a {@code void} type is assigned to the {@code void} it will consider this a trivial operation.</li>
 * <li>If a {@code void} type is assigned to a non-{@code void} type, it will pop the top value from the stack.</li>
 * <li>If a non-{@code void} type is assigned to a {@code void} type, it will load the target type's default value
 * only if this was configured at the assigner's construction.</li>
 * <li>If two non-{@code void} types are subject of the assignment, it will delegate the assignment to its chained
 * assigner.</li>
 * </ol>
 */
@HashCodeAndEqualsPlugin.Enhance
public class VoidAwareAssigner implements Assigner {

    /**
     * An assigner that is capable of handling assignments that do not involve {@code void} types.
     */
    private final Assigner chained;

    /**
     * Creates a new assigner that is capable of handling void types.
     *
     * @param chained A chained assigner which will be queried by this assigner to handle assignments that
     *                do not involve a {@code void} type.
     */
    public VoidAwareAssigner(Assigner chained) {
        this.chained = chained;
    }

    /**
     * {@inheritDoc}
     */
    public StackManipulation assign(TypeDescription.Generic source, TypeDescription.Generic target, Typing typing) {
        if (source.represents(void.class) && target.represents(void.class)) {
            return StackManipulation.Trivial.INSTANCE;
        } else if (source.represents(void.class) /* && target != void.class */) {
            return typing.isDynamic()
                    ? DefaultValue.of(target)
                    : StackManipulation.Illegal.INSTANCE;
        } else if (/* source != void.class && */ target.represents(void.class)) {
            return Removal.of(source);
        } else /* source != void.class && target != void.class */ {
            return chained.assign(source, target, typing);
        }
    }
}
