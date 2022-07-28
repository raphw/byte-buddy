package net.bytebuddy.test.utility;

import java.io.File;

public class ProjectInfo {
    final String name;
    final File dir;
    final File mainDir;
    final File buildGradeFile;

    public ProjectInfo(String name, File dir, File mainDir, File buildGradeFile) {
        this.name = name;
        this.dir = dir;
        this.mainDir = mainDir;
        this.buildGradeFile = buildGradeFile;
    }
}
