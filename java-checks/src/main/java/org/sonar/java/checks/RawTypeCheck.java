/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3740")
public class RawTypeCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.VARIABLE, Tree.Kind.METHOD, Tree.Kind.NEW_CLASS, Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case VARIABLE:
        checkTypeTree(((VariableTree) tree).type());
        break;
      case METHOD:
        checkTypeTree(((MethodTree) tree).returnType());
        break;
      case NEW_CLASS:
        checkTypeTree(((NewClassTree) tree).identifier());
        break;
      case CLASS:
      case INTERFACE:
      case ENUM:
        ClassTree classTree = (ClassTree) tree;
        classTree.superInterfaces().forEach(this::checkTypeTree);
        checkTypeTree(classTree.superClass());
        break;
      default:
        // do nothing - can not occur
        break;
    }
  }

  private void checkTypeTree(@Nullable TypeTree typeTree) {
    if (typeTree == null) {
      return;
    }
    if (typeTree.is(Tree.Kind.IDENTIFIER)) {
      checkIdentifier((IdentifierTree) typeTree);
    } else if (typeTree.is(Tree.Kind.MEMBER_SELECT)) {
      checkIdentifier(((MemberSelectExpressionTree) typeTree).identifier());
    }
  }

  private void checkIdentifier(IdentifierTree identifier) {
    Type type = identifier.symbolType();
    if (JUtils.isRawType(type) && !type.equals(JUtils.declaringType(type))) {
      reportIssue(identifier, "Provide the parametrized type for this generic.");
    }
  }
}
