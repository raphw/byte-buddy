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
package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Describes the manifestation of a method, i.e. if a method is final, abstract or native.
 * Note that an {@code abstract} method must never be static and can only be declared for an
 * {@code abstract} type.
 */
public enum MethodManifestation implements ModifierContributor.ForMethod {

    /**
     * Modifier for a non-native, non-abstract, non-final method. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for a native method.
     */
    NATIVE(Opcodes.ACC_NATIVE),

    /**
     * Modifier for an abstract method.
     */
    ABSTRACT(Opcodes.ACC_ABSTRACT),

    /**
     * Modifier for a final method.
     */
    FINAL(Opcodes.ACC_FINAL),

    /**
     * Modifier for a native and final method.
     */
    FINAL_NATIVE(Opcodes.ACC_FINAL | Opcodes.ACC_NATIVE),

    /**
     * Modifier for a bridge method.
     */
    BRIDGE(Opcodes.ACC_BRIDGE),

    /**
     * Modifier for a final bridge method.
     */
    FINAL_BRIDGE(Opcodes.ACC_FINAL | Opcodes.ACC_BRIDGE);

    /**
     * The mask the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new method manifestation.
     *
     * @param mask The modifier mask of this instance.
     */
    MethodManifestation(int mask) {
        this.mask = mask;
    }

    /**
     * {@inheritDoc}
     */
    public int getMask() {
        return mask;
    }

    /**
     * {@inheritDoc}
     */
    public int getRange() {
        return Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_FINAL | Opcodes.ACC_BRIDGE;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Returns {@code true} if this instance represents a {@code native} method.
     *
     * @return {@code true} if this instance represents a {@code native} method.
     */
    public boolean isNative() {
        return (mask & Opcodes.ACC_NATIVE) != 0;
    }

    /**
     * Returns {@code true} if this instance represents a {@code abstract} method.
     *
     * @return {@code true} if this instance represents a {@code abstract} method.
     */
    public boolean isAbstract() {
        return (mask & Opcodes.ACC_ABSTRACT) != 0;
    }

    /**
     * Returns {@code true} if this instance represents a {@code final} method.
     *
     * @return {@code true} if this instance represents a {@code final} method.
     */
    public boolean isFinal() {
        return (mask & Opcodes.ACC_FINAL) != 0;
    }

    /**
     * Returns {@code true} if this instance represents a bridge method.
     *
     * @return {@code true} if this instance represents a bridge method.
     */
    public boolean isBridge() {
        return (mask & Opcodes.ACC_BRIDGE) != 0;
    }
}
