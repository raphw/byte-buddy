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
package net.bytebuddy.utility.visitor;

import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * A method visitor that maps the first available line number information, if available, to the beginning of the method.
 */
public class LineNumberPrependingMethodVisitor extends ExceptionTableSensitiveMethodVisitor {

    /**
     * A label indicating the start of the method.
     */
    private final Label startOfMethod;

    /**
     * {@code true} if the first line number was not yet discovered.
     */
    private boolean prependLineNumber;

    /**
     * Creates a new line number prepending method visitor.
     *
     * @param methodVisitor The method visitor to delegate to.
     */
    public LineNumberPrependingMethodVisitor(MethodVisitor methodVisitor) {
        super(OpenedClassReader.ASM_API, methodVisitor);
        startOfMethod = new Label();
        prependLineNumber = true;
    }

    @Override
    protected void onAfterExceptionTable() {
        super.visitLabel(startOfMethod);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if (prependLineNumber) {
            start = startOfMethod;
            prependLineNumber = false;
        }
        super.visitLineNumber(line, start);
    }
}
