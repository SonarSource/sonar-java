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
/*
 * Creation : 20 avr. 2015
 */
package org.sonar.samples.java.checks;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Only to bring out the unit test requirement about classpath when bytecode methods used (see rule unit test class)
 */
@Rule(key = "AvoidSuperClass")
public class AvoidSuperClassRule extends IssuableSubscriptionVisitor {

  public static final List<String> SUPER_CLASS_AVOID = Collections.singletonList("org.slf4j.Logger");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    // Register to the kind of nodes you want to be called upon visit.
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    // Visit CLASS node only => cast could be done
    ClassTree treeClazz = (ClassTree) tree;

    // No extends => stop to visit class
    if (treeClazz.superClass() == null) {
      return;
    }

    // For 'symbolType' usage, jar in dependencies must be on classpath, !unknownSymbol! result otherwise
    String superClassName = treeClazz.superClass().symbolType().fullyQualifiedName();

    // Check if superClass avoid
    if (SUPER_CLASS_AVOID.contains(superClassName)) {
      reportIssue(tree, String.format("The usage of super class %s is forbidden", superClassName));
    }
  }

}
