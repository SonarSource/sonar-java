/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks.helpers;

import java.util.List;
import org.sonar.java.annotations.Beta;
import org.sonar.java.collections.ListUtils;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.JavaTree;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * For internal use only. Can not be used outside SonarJava analyzer.
 */
@Beta
public class QuickFixHelper {

  private QuickFixHelper() {
    // Utility class
  }

  public static InternalJavaIssueBuilder newIssue(JavaFileScannerContext context) {
    return (InternalJavaIssueBuilder) internalContext(context).newIssue();
  }

  public static DefaultJavaFileScannerContext internalContext(JavaFileScannerContext context) {
    return (DefaultJavaFileScannerContext) context;
  }

  public static SyntaxToken nextToken(Tree tree) {
    Tree parent = tree.parent();
    if (parent == null) {
      return tree.lastToken();
    }
    List<Tree> children = ((JavaTree) parent).getChildren();
    if (tree.equals(ListUtils.getLast(children))) {
      // last tree, check next from parent
      return nextToken(parent);
    }
    SyntaxToken nextToken = tree.lastToken();
    for (int i = children.indexOf(tree) + 1; i < children.size(); i++) {
      SyntaxToken token = children.get(i).firstToken();
      if (token != null) {
        nextToken = token;
        break;
      }
    }
    return nextToken;
  }

  public static SyntaxToken previousToken(Tree tree) {
    Tree parent = tree.parent();
    if (parent == null) {
      return tree.firstToken();
    }
    List<Tree> children = ((JavaTree) parent).getChildren();
    if (tree.equals(children.get(0))) {
      // first tree, check last from parent
      return previousToken(parent);
    }
    for (int i = children.indexOf(tree) - 1; i >= 0; i--) {
      SyntaxToken token = children.get(i).lastToken();
      if (token != null) {
        return token;
      }
    }
    return previousToken(parent);
  }

  public static String contentForTree(Tree tree, JavaFileScannerContext context) {
    return contentForRange(tree.firstToken(), tree.lastToken(), context);
  }

  public static String contentForRange(SyntaxToken firstToken, SyntaxToken endToken, JavaFileScannerContext context) {
    int startLine = firstToken.line();
    int endLine = endToken.line();

    int beginIndex = firstToken.column();
    int endIndex = endToken.column() + endToken.text().length();

    if (startLine == endLine) {
      // one-liners
      return context.getFileLines().get(startLine - 1).substring(beginIndex, endIndex);
    }

    // rely on file content KEEPING line separators
    List<String> lines = context.getFileLines().subList(startLine - 1, endLine);

    // rebuild content of tree as String
    StringBuilder sb = new StringBuilder();
    sb.append(lines.get(0)
      .substring(beginIndex))
      .append("\n");
    for (int i = 1; i < lines.size() - 1; i++) {
      sb.append(lines.get(i))
        .append("\n");
    }
    sb.append(ListUtils.getLast(lines).substring(0, endIndex));

    return sb.toString();
  }
}
