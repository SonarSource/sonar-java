/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6300")
public class AndroidUnencryptedFilesCheck extends AbstractMethodDetection {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofSubTypes("java.nio.file.Files")
        .names("write")
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofSubTypes("java.io.FileWriter",
          "java.io.FileOutputStream")
        .constructor()
        .withAnyParameters()
        .build()
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    reportIfInAndroidContext(ExpressionUtils.methodName(mit));
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    reportIfInAndroidContext(newClassTree.identifier());
  }

  private void reportIfInAndroidContext(Tree tree) {
    if (context.inAndroidContext()) {
      reportIssue(tree, "Make sure using unencrypted files is safe here.");
    }
  }

}
