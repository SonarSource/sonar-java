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
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

@Rule(
  key = "S2258",
  priority = Priority.BLOCKER,
  tags = {"cwe", "owasp-top10", "security"})
public class NullCipherCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    NewClassTree newClass = (NewClassTree) tree;
    IdentifierTree identifier = null;
    if (newClass.identifier().is(Tree.Kind.IDENTIFIER)) {
      identifier = (IdentifierTree) newClass.identifier();
    } else if (newClass.identifier().is(Tree.Kind.MEMBER_SELECT)) {
      identifier = ((MemberSelectExpressionTree) newClass.identifier()).identifier();
    }
    if (identifier != null && hasSemantic()) {
      Symbol reference = getSemanticModel().getReference(identifier);
      if (reference != null) {
        Type type = reference.getType();
        if (type != null && type.is("javax.crypto.NullCipher")) {
          addIssue(newClass, "Remove this use of the \"NullCipher\" class.");
        }
      }
    }
  }

}
