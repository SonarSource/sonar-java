/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import org.mockito.Matchers;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.resources.Resource;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.DefaultJavaResourceLocator;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.JavaSquid;
import org.sonar.java.bytecode.visitor.DefaultBytecodeContext;
import org.sonar.java.filters.SuppressWarningsFilter;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.api.CodeVisitor;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.indexer.QueryByType;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BytecodeFixture {

  private BytecodeFixture() {
  }

  public static List<AnalyzerMessage> scan(String target, CodeVisitor visitor) {
    final File baseDir = new File("src/test/java/");
    final File file = new File(baseDir, "org/sonar/java/checks/targets/" + target + ".java");

    File bytecodeFile = new File("target/test-classes/");

    if (!file.isFile()) {
      throw new IllegalArgumentException("File '" + file.getName() + "' not found.");
    }
    SensorContext sensorContext = mock(SensorContext.class);
    when(sensorContext.getResource(Matchers.any(InputPath.class))).thenReturn(org.sonar.api.resources.File.create(file.getPath()));
    DefaultFileSystem fs = new DefaultFileSystem(null);
    fs.add(new DefaultInputFile(file.getPath()));
    DefaultJavaResourceLocator javaResourceLocator = new DefaultJavaResourceLocator(fs, null, new SuppressWarningsFilter());
    javaResourceLocator.setSensorContext(sensorContext);
    final List<AnalyzerMessage> analyzerMessages = new ArrayList<>();
    JavaSquid javaSquid = new JavaSquid(new JavaConfiguration(Charset.forName("UTF-8")), null, null, javaResourceLocator, new DefaultBytecodeContext(javaResourceLocator) {
      @Override
      public void reportIssue(JavaCheck check, Resource resource, String message, int line) {
        analyzerMessages.add(new AnalyzerMessage(check, file, line, message, 0));
      }
    }, visitor);
    javaSquid.scan(Collections.singleton(file), Collections.<File>emptyList(), Collections.singleton(bytecodeFile));

    Collection<SourceCode> sources = javaSquid.getIndex().search(new QueryByType(SourceFile.class));
    if (sources.size() != 1) {
      throw new IllegalStateException("Only one SourceFile was expected whereas " + sources.size() + " has been returned.");
    }
    return analyzerMessages;
  }
}
