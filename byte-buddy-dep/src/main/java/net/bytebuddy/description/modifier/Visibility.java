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
 * Describes a type's, field's or a method's visibility.
 */
public enum Visibility implements ModifierContributor.ForType, ModifierContributor.ForMethod, ModifierContributor.ForField {

    /**
     * A modifier contributor for {@code public} visibility.
     */
    PUBLIC(Opcodes.ACC_PUBLIC),

    /**
     * Modifier for a package-private visibility. (This is the default modifier.)
     */
    PACKAGE_PRIVATE(EMPTY_MASK),

    /**
     * A modifier contributor for {@code protected} visibility.
     */
    PROTECTED(Opcodes.ACC_PROTECTED),

    /**
     * A modifier contributor for {@code private} visibility.
     */
    PRIVATE(Opcodes.ACC_PRIVATE);

    /**
     * The mask the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new visibility representation.
     *
     * @param mask The modifier mask of this instance.
     */
    Visibility(int mask) {
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
        return Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == PACKAGE_PRIVATE;
    }

    /**
     * Returns {@code true} if this instance describes {@code public} visibility.
     *
     * @return {@code true} if this instance describes {@code public} visibility.
     */
    public boolean isPublic() {
        return (mask & Opcodes.ACC_PUBLIC) != 0;
    }

    /**
     * Returns {@code true} if this instance describes {@code protected} visibility.
     *
     * @return {@code true} if this instance describes {@code protected} visibility.
     */
    public boolean isProtected() {
        return (mask & Opcodes.ACC_PROTECTED) != 0;
    }

    /**
     * Returns {@code true} if this instance describes package-private visibility.
     *
     * @return {@code true} if this instance describes package-private visibility.
     */
    public boolean isPackagePrivate() {
        return !(isPublic() || isPrivate() || isProtected());
    }

    /**
     * Returns {@code true} if this instance describes {@code private} visibility.
     *
     * @return {@code true} if this instance describes {@code private} visibility.
     */
    public boolean isPrivate() {
        return (mask & Opcodes.ACC_PRIVATE) != 0;
    }

    /**
     * Expands the visibility to be at least as high as this visibility and the provided visibility.
     *
     * @param visibility A visibility to compare against.
     * @return A visibility that is as least as high as this and the supplied visibility.
     */
    public Visibility expandTo(Visibility visibility) {
        switch (visibility) {
            case PUBLIC:
                return PUBLIC;
            case PROTECTED:
                return this == PUBLIC
                        ? PUBLIC
                        : visibility;
            case PACKAGE_PRIVATE:
                return this == PRIVATE
                        ? PACKAGE_PRIVATE
                        : this;
            case PRIVATE:
                return this;
            default:
                throw new IllegalStateException("Unexpected visibility: " + visibility);
        }
    }
}
