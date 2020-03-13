/*
 * SonarQube Java
 * Copyright (C) 2013-2020 SonarSource SA
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
package org.sonar.samples.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JspCodeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.Collections.singletonList;

@Rule(key = "jspcheck", priority = Priority.MINOR, name = "JspCodeCheck", description = "JspCodeCheck")
public class JspCodeCheck extends IssuableSubscriptionVisitor implements JspCodeVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    Path visitedClasses = new File(context.getWorkingDirectory(), "visit.txt").toPath();
    String name = ((ClassTree) tree).simpleName().name();
    try {
      Files.write(visitedClasses, (name + "\n").getBytes(), APPEND, CREATE);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
