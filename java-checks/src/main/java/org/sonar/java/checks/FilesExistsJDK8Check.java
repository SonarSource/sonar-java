/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonarsource.analyzer.commons.collections.MapBuilder;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;

@Rule(key = "S3725")
public class FilesExistsJDK8Check extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String JAVA_NIO_FILE_FILES = "java.nio.file.Files";
  private static final String EXISTS = "exists";
  private static final String IS_DIRECTORY = "isDirectory";
  private static final Map<String, String> messageParam = MapBuilder.<String, String>newMap()
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
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes(JAVA_NIO_FILE_FILES)
      .names(EXISTS, "notExists", "isRegularFile", IS_DIRECTORY)
      .withAnyParameters()
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    String methodName = mit.methodSymbol().name();
    reportIssue(ExpressionUtils.methodName(mit), "Replace this with a call to the \"toFile()." + messageParam.get(methodName) + "()\" method");
  }

  @Override
  protected void onMethodReferenceFound(MethodReferenceTree methodReferenceTree) {
    String methodName = methodReferenceTree.method().symbol().name();
    reportIssue(methodReferenceTree.method(), "Replace this with a call to the \"toFile()." + messageParam.get(methodName) + "()\" method");
  }
}
