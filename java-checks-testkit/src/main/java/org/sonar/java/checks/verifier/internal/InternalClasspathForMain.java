package org.sonar.java.checks.verifier.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;
import org.sonar.java.classpath.ClasspathForMain;

public class InternalClasspathForMain extends ClasspathForMain {

  private List<File> classpath = new ArrayList<>();

  public InternalClasspathForMain(Configuration settings, FileSystem fs) {
    super(settings, fs);
  }

  public InternalClasspathForMain(Configuration settings, FileSystem fs, List<File> classpath) {
    super(settings, fs);
    this.classpath = classpath;
  }

  @Override
  protected void init() {
    elements.addAll(getJdkJars());
    binaries.addAll(classpath);
    elements.addAll(binaries);
  }
}
