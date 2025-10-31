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
package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Defines if a type or member is supposed to be marked as synthetic.
 */
public enum SyntheticState implements ModifierContributor.ForType,
        ModifierContributor.ForModule,
        ModifierContributor.ForModule.OfRequire,
        ModifierContributor.ForModule.OfExport,
        ModifierContributor.ForModule.OfOpen,
        ModifierContributor.ForMethod,
        ModifierContributor.ForField,
        ModifierContributor.ForParameter {

    /**
     * Modifier for not marking a type member as synthetic. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for marking a type member as synthetic.
     */
    SYNTHETIC(Opcodes.ACC_SYNTHETIC);

    /**
     * The mask of the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new synthetic state representation.
     *
     * @param mask The modifier mask of this instance.
     */
    SyntheticState(int mask) {
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
        return Opcodes.ACC_SYNTHETIC;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Checks if the current state describes the synthetic state.
     *
     * @return {@code true} if the current state is synthetic.
     */
    public boolean isSynthetic() {
        return this == SYNTHETIC;
    }
}
