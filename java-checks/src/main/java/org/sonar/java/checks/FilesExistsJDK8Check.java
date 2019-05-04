/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;

@Rule(key = "S3725")
public class FilesExistsJDK8Check extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String JAVA_NIO_FILE_FILES = "java.nio.file.Files";
  private static final String EXISTS = "exists";
  private static final String IS_DIRECTORY = "isDirectory";
  private static final Map<String, String> messageParam = ImmutableMap.<String, String>builder()
    .put(EXISTS, EXISTS)
    .put("notExists", EXISTS)
    .put("isRegularFile", "isFile")
    .put(IS_DIRECTORY, IS_DIRECTORY)
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.asInt() == 8;
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition(JAVA_NIO_FILE_FILES).name(EXISTS).withAnyParameters(),
      MethodMatcher.create().typeDefinition(JAVA_NIO_FILE_FILES).name("notExists").withAnyParameters(),
      MethodMatcher.create().typeDefinition(JAVA_NIO_FILE_FILES).name("isRegularFile").withAnyParameters(),
      MethodMatcher.create().typeDefinition(JAVA_NIO_FILE_FILES).name(IS_DIRECTORY).withAnyParameters()
      );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    String methodName = mit.symbol().name();
    reportIssue(ExpressionUtils.methodName(mit), "Replace this with a call to the \"toFile()." + messageParam.get(methodName) + "()\" method");
  }

  @Override
  protected void onMethodReferenceFound(MethodReferenceTree methodReferenceTree) {
    String methodName = methodReferenceTree.method().symbol().name();
    reportIssue(methodReferenceTree.method(), "Replace this with a call to the \"toFile()." + messageParam.get(methodName) + "()\" method");
  }
}
