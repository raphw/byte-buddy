package net.bytebuddy.test.precompiled;

import net.bytebuddy.description.type.AbstractTypeDescriptionTest;

import java.util.List;

public record SampleRecord(@AbstractTypeDescriptionTest.SampleAnnotation @TypeAnnotation(42) List<@TypeAnnotation(84) String> foo) {
    /* empty */
}
