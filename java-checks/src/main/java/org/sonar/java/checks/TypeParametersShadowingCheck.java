/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.TypeParameters;

@Rule(key = "S4977")
public class TypeParametersShadowingCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String ISSUE_MESSAGE = "Rename \"%s\" which hides a type parameter from the outer scope.";

  private JavaFileScannerContext context;

  private Map<String, IdentifierTree> currentTypeParametersInScope;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    currentTypeParametersInScope = new HashMap<>();
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    processTree(tree, tree.typeParameters(), tree.symbol().isStatic(), super::visitClass);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    processTree(tree, tree.typeParameters(), tree.symbol().isStatic(), super::visitMethod);
  }

  private <T> void processTree(T tree, TypeParameters typeParameters, boolean isStatic, Consumer<T> visitTree) {
    Map<String, IdentifierTree> oldScope = currentTypeParametersInScope;
    if (isStatic) {
      currentTypeParametersInScope = new HashMap<>();
    }
    Map<String, IdentifierTree> declaredTypeParameters = processAndGetTypeParameters(typeParameters);
    currentTypeParametersInScope.putAll(declaredTypeParameters);
    visitTree.accept(tree);
    declaredTypeParameters.forEach(currentTypeParametersInScope::remove);
    if (isStatic) {
      currentTypeParametersInScope = oldScope;
    }
  }

  private Map<String, IdentifierTree> processAndGetTypeParameters(TypeParameters typeParameters) {
    Map<String, IdentifierTree> declaredTypeParameters = new HashMap<>();
    typeParameters.forEach(typeParameter -> {
      IdentifierTree id = typeParameter.identifier();
      String name = id.toString();

      IdentifierTree shadowedId = currentTypeParametersInScope.get(name);
      if (shadowedId != null) {
        context.reportIssue(this, id,
          String.format(ISSUE_MESSAGE, name),
          Collections.singletonList(new JavaFileScannerContext.Location("Shadowed type parameter", shadowedId)
        ), null);
      } else {
        // Entry added only in the else part, because we want to store only the first and outer most appearance of a type.
        // If a type is shadowed multiple times, we use only the outer most as secondary location.
        declaredTypeParameters.put(name, id);
      }
    });
    return declaredTypeParameters;
  }
}
