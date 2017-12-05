/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4349")
public class OutputStreamOverrideWriteCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher WRITE_BYTES_INT_INT = MethodMatcher.create().typeDefinition(TypeCriteria.anyType()).name("write")
    .addParameter("byte[]")
    .addParameter("int")
    .addParameter("int");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if(!hasSemantic()) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    Type superType = classTree.symbol().superClass();
    IdentifierTree className = classTree.simpleName();
    if(className == null || superType == null || !(superType.is("java.io.OutputStream") || superType.is("java.io.FilterOutputStream"))) {
      return;
    }
    Optional<MethodTree> writeByteIntInt = classTree.members()
      .stream()
      .filter(m -> m.is(Tree.Kind.METHOD))
      .map(m -> (MethodTree) m)
      .filter(WRITE_BYTES_INT_INT::matches)
      .findFirst();

    if(!writeByteIntInt.isPresent()) {
      reportIssue(className, "Provide an override of \"write(byte[],int,int)\" for this class.");
    }

  }
}
