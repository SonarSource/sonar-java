/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.syntaxtoken;

import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

public class FirstSyntaxTokenFinder {

  private FirstSyntaxTokenFinder() {
  }

  @Nullable
  public static SyntaxToken firstSyntaxToken(@Nullable Tree tree) {
    if (tree == null || tree.is(Tree.Kind.INFERED_TYPE)) {
      return null;
    } else if (tree.is(Tree.Kind.TOKEN)) {
      return (SyntaxToken) tree;
    }
    if(tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if(mit.typeArguments() !=null && mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        SyntaxToken syntaxToken = firstSyntaxToken(((MemberSelectExpressionTree) mit.methodSelect()).expression());
        if (syntaxToken != null) {
          return syntaxToken;
        }
      }
    }
    for (Tree next : ((JavaTree) tree).getChildren()) {
      SyntaxToken syntaxToken = firstSyntaxToken(next);
      if (syntaxToken != null) {
        return syntaxToken;
      }
    }
    return null;
  }
}
