/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks.security;

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
    return List.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
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
