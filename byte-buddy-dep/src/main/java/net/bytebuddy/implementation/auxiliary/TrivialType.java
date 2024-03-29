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
package net.bytebuddy.implementation.auxiliary;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.MethodAccessorFactory;
import net.bytebuddy.utility.RandomString;

import java.util.Collections;

/**
 * A trivial type that extends {@link java.lang.Object} without defining any fields, methods or constructors.
 * This type is meant to be used as a marker type only.
 */
public enum TrivialType implements AuxiliaryType {

    /**
     * A trivial type that defines the {@link SignatureRelevant} annotation.
     */
    SIGNATURE_RELEVANT(true),

    /**
     * A non-annotated trivial type.
     */
    PLAIN(false);

    /**
     * Determines if this type determines the {@link SignatureRelevant} annotation.
     */
    private final boolean eager;

    /**
     * Creates a new trivial type.
     *
     * @param eager Determines if this type determines the {@link SignatureRelevant} annotation.
     */
    TrivialType(boolean eager) {
        this.eager = eager;
    }

    /**
     * {@inheritDoc}
     */
    public String getSuffix() {
        return RandomString.hashOf(name().hashCode());
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType make(String auxiliaryTypeName,
                            ClassFileVersion classFileVersion,
                            MethodAccessorFactory methodAccessorFactory) {
        return new ByteBuddy(classFileVersion)
                .with(TypeValidation.DISABLED)
                .with(MethodGraph.Empty.INSTANCE) // avoid parsing the graph
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .annotateType(eager
                        ? Collections.singletonList(AnnotationDescription.Builder.ofType(SignatureRelevant.class).build(false))
                        : Collections.<AnnotationDescription>emptyList())
                .name(auxiliaryTypeName)
                .modifiers(DEFAULT_TYPE_MODIFIER)
                .make();
    }
}
