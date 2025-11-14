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

import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6355")
public class DeprecatedArgumentsCheck extends AbstractMissingDeprecatedChecker {

  private boolean isJava9 = false;

  @Override
  public void setContext(JavaFileScannerContext context) {
    isJava9 = context.getJavaVersion().isJava9Compatible();
    super.setContext(context);
  }

  @Override
  void handleDeprecatedElement(Tree tree, @CheckForNull AnnotationTree deprecatedAnnotation, boolean hasJavadocDeprecatedTag) {
    if (isJava9 && deprecatedAnnotation != null && deprecatedAnnotation.arguments().isEmpty()) {
      reportIssue(deprecatedAnnotation, "Add 'since' and/or 'forRemoval' arguments to the @Deprecated annotation.");
    }
  }

}
