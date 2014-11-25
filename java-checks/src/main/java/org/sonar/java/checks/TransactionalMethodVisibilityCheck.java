/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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

import com.google.common.collect.ImmutableList;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

@Rule(
  key = "S2230",
  priority = Priority.CRITICAL,
  tags = {"bug", "spring"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class TransactionalMethodVisibilityCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree method = (MethodTree) tree;
    boolean isPublic = method.modifiers().modifiers().contains(Modifier.PUBLIC);
    if (!isPublic && hasTransactionalAnnotation(method)) {
      addIssue(method, "Make this method \"public\" or remove the \"@Transactional\" annotation");
    }
  }

  private boolean hasTransactionalAnnotation(MethodTree method) {
    for (AnnotationTree annotation : method.modifiers().annotations()) {
      Type annotationType = ((AbstractTypedTree) annotation).getSymbolType();
      if (annotationType.is("org.springframework.transaction.annotation.Transactional")) {
        return true;
      }
    }
    return false;
  }

}
