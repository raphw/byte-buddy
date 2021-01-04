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
package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import org.objectweb.asm.MethodVisitor;

import java.io.*;

/**
 * A constant that represents a value in its serialized form.
 */
@HashCodeAndEqualsPlugin.Enhance
public class SerializedConstant implements StackManipulation {

    /**
     * A charset that does not change the supplied byte array upon encoding or decoding.
     */
    private static final String CHARSET = "ISO-8859-1";

    /**
     * The serialized value.
     */
    private final String serialization;

    /**
     * Creates a new constant for a serialized value.
     *
     * @param serialization The serialized value.
     */
    protected SerializedConstant(String serialization) {
        this.serialization = serialization;
    }

    /**
     * Creates a new stack manipulation to load the supplied value onto the stack.
     *
     * @param value The value to serialize.
     * @return A stack manipulation to serialize the supplied value.
     */
    public static StackManipulation of(Serializable value) {
        if (value == null) {
            return NullConstant.INSTANCE;
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            try {
                objectOutputStream.writeObject(value);
            } finally {
                objectOutputStream.close();
            }
            return new SerializedConstant(byteArrayOutputStream.toString(CHARSET));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot serialize " + value, exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValid() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        try {
            return new StackManipulation.Compound(
                    TypeCreation.of(TypeDescription.ForLoadedType.of(ObjectInputStream.class)),
                    Duplication.SINGLE,
                    TypeCreation.of(TypeDescription.ForLoadedType.of(ByteArrayInputStream.class)),
                    Duplication.SINGLE,
                    new TextConstant(serialization),
                    new TextConstant(CHARSET),
                    MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(String.class.getMethod("getBytes", String.class))),
                    MethodInvocation.invoke(new MethodDescription.ForLoadedConstructor(ByteArrayInputStream.class.getConstructor(byte[].class))),
                    MethodInvocation.invoke(new MethodDescription.ForLoadedConstructor(ObjectInputStream.class.getConstructor(InputStream.class))),
                    MethodInvocation.invoke(new MethodDescription.ForLoadedMethod(ObjectInputStream.class.getMethod("readObject")))
            ).apply(methodVisitor, implementationContext);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Could not locate Java API method", exception);
        }
    }
}
