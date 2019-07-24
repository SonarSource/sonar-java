/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2444")
public class StaticFieldInitializationCheck extends AbstractInSynchronizeChecker {

  private Deque<Boolean> classWithSynchronizedMethod = new LinkedList<>();
  private Deque<Boolean> withinStaticInitializer = new LinkedList<>();
  private Deque<Boolean> methodUsesLocks = new LinkedList<>();
  private MethodMatcherCollection locks = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition("java.util.concurrent.locks.Lock").name("lock").withoutParameter(),
    MethodMatcher.create().typeDefinition("java.util.concurrent.locks.Lock").name("tryLock").withoutParameter()
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    List<Tree.Kind> nodesToVisit = new ArrayList<>(super.nodesToVisit());
    nodesToVisit.add(Tree.Kind.CLASS);
    nodesToVisit.add(Tree.Kind.ASSIGNMENT);
    nodesToVisit.add(Tree.Kind.STATIC_INITIALIZER);
    return nodesToVisit;
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    classWithSynchronizedMethod.push(false);
    withinStaticInitializer.push(false);
    methodUsesLocks.push(false);
    super.setContext(context);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    withinStaticInitializer.clear();
    methodUsesLocks.clear();
    classWithSynchronizedMethod.clear();
  }

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case CLASS:
        classWithSynchronizedMethod.push(hasSynchronizedMethod((ClassTree) tree));
        break;
      case STATIC_INITIALIZER:
        withinStaticInitializer.push(true);
        break;
      case METHOD:
        methodUsesLocks.push(false);
        break;
      case METHOD_INVOCATION:
        if (locks.anyMatch((MethodInvocationTree) tree) && methodUsesLocks.size() != 1) {
          methodUsesLocks.pop();
          methodUsesLocks.push(true);
        }
        break;
      case ASSIGNMENT:
        AssignmentExpressionTree aet = (AssignmentExpressionTree) tree;
        if (hasSemantic()
          && aet.variable().is(Tree.Kind.IDENTIFIER)
          && !isInSyncBlock()
          && !isInStaticInitializer()
          && !isUsingLock()
          && isInClassWithSynchronizedMethod()) {
          IdentifierTree variable = (IdentifierTree) aet.variable();
          if (isStaticNotVolatileObject(variable)) {
            reportIssue(variable, "Synchronize this lazy initialization of '" + variable.name() + "'");
          }
        }
        break;
      default:
        // Do nothing
    }
    super.visitNode(tree);
  }

  private boolean isInStaticInitializer() {
    return withinStaticInitializer.peek();
  }

  private static Boolean hasSynchronizedMethod(ClassTree tree) {
    return tree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .map(MethodTree::modifiers)
      .anyMatch(modifiers -> ModifiersUtils.hasModifier(modifiers, Modifier.SYNCHRONIZED));
  }

  private boolean isInClassWithSynchronizedMethod() {
    return classWithSynchronizedMethod.peek();
  }

  private boolean isUsingLock() {
    return methodUsesLocks.peek();
  }

  @Override
  public void leaveNode(Tree tree) {
    switch (tree.kind()) {
      case CLASS:
        classWithSynchronizedMethod.pop();
        break;
      case STATIC_INITIALIZER:
        withinStaticInitializer.pop();
        break;
      case METHOD:
        methodUsesLocks.pop();
        break;
      default:
        // do nothing
    }
    super.leaveNode(tree);
  }

  private static boolean isStaticNotVolatileObject(IdentifierTree variable) {
    Symbol symbol = variable.symbol();
    if (symbol.isUnknown()) {
      return false;
    }
    return isStaticNotFinalNotVolatile(symbol) && !symbol.type().isPrimitive();
  }

  private static boolean isStaticNotFinalNotVolatile(Symbol symbol) {
    return symbol.isStatic() && !symbol.isVolatile() && !symbol.isFinal();
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.emptyList();
  }

}
