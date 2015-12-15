/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.java.bytecode.asm.AsmField;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.visitor.BytecodeContext;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.java.bytecode.visitor.DefaultBytecodeContext;
import org.sonar.java.filters.SuppressWarningsFilter;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaResourceLocator;
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

  public static List<AnalyzerMessage> scan(String target, final BytecodeVisitor visitor) {
    final File file = new File(target);

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
    BytecodeVisitor visitorWithFakeContext = new ByteCodeVisitorWithFakeContext(visitor, file, javaResourceLocator, analyzerMessages);
    JavaSquid javaSquid = new JavaSquid(new JavaConfiguration(Charset.forName("UTF-8")), null, null, javaResourceLocator, visitorWithFakeContext);
    javaSquid.scan(Collections.singleton(file), Collections.<File>emptyList(), Collections.singleton(bytecodeFile));

    Collection<SourceCode> sources = javaSquid.getIndex().search(new QueryByType(SourceFile.class));
    if (sources.size() != 1) {
      throw new IllegalStateException("Only one SourceFile was expected whereas " + sources.size() + " has been returned.");
    }
    return analyzerMessages;
  }

  private static class ByteCodeVisitorWithFakeContext extends BytecodeVisitor {
    private final BytecodeVisitor visitor;
    private final BytecodeContext fakeContext;

    private ByteCodeVisitorWithFakeContext(BytecodeVisitor visitor, final File file, JavaResourceLocator javaResourceLocator, final List<AnalyzerMessage> analyzerMessages) {
      this.visitor = visitor;
      this.fakeContext = new DefaultBytecodeContext(javaResourceLocator) {
        @Override
        public void reportIssue(JavaCheck check, Resource resource, String message, int line) {
          analyzerMessages.add(new AnalyzerMessage(check, file, line, message, 0));
        }
      };
    }

    @Override
    protected BytecodeContext getContext() {
      return fakeContext;
    }

    @Override
    public void setContext(BytecodeContext context) {
      visitor.setContext(fakeContext);
    }

    @Override
    public void visitClass(AsmClass asmClass) {
      visitor.visitClass(asmClass);
    }

    @Override
    public void visitMethod(AsmMethod asmMethod) {
      visitor.visitMethod(asmMethod);
    }

    @Override
    public void visitField(AsmField asmField) {
      visitor.visitField(asmField);
    }

    @Override
    public void visitEdge(AsmEdge asmEdge) {
      visitor.visitEdge(asmEdge);
    }

    @Override
    public void leaveClass(AsmClass asmClass) {
      visitor.leaveClass(asmClass);
    }
  }
}
