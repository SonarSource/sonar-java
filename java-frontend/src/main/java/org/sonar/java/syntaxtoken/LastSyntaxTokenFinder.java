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

import com.google.common.collect.Lists;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class LastSyntaxTokenFinder {

  private LastSyntaxTokenFinder() {
  }

  @Nullable
  public static SyntaxToken lastSyntaxToken(@Nullable Tree tree) {
    if (tree == null || tree.is(Tree.Kind.INFERED_TYPE)) {
      return null;
    } else if (tree.is(Tree.Kind.TOKEN)) {
      return (SyntaxToken) tree;
    }
    ArrayList<Tree> childrenAsList = Lists.newArrayList(((JavaTree) tree).children());
    for (Tree next : Lists.reverse(childrenAsList)) {
      SyntaxToken syntaxToken = lastSyntaxToken(next);
      if (syntaxToken != null) {
        return syntaxToken;
      }
    }
    return null;
  }
}
