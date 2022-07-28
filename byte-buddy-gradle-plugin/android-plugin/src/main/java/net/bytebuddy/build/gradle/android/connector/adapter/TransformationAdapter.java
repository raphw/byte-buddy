package net.bytebuddy.build.gradle.android.connector.adapter;

import net.bytebuddy.build.gradle.android.transformation.AndroidTransformation;

public interface TransformationAdapter {

    void adapt(AndroidTransformation transformation);
}