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

import com.google.common.collect.Lists;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Rule(key = "S2886")
public class SyncGetterAndSetterCheck extends IssuableSubscriptionVisitor {

  private static final GetSetPredicate SETTER = new GetSetPredicate() {
    @Override
    public String getStartName() {
      return "set";
    }

    @Override
    public boolean apply(MethodTree methodTree) {
      return methodTree.simpleName().name().startsWith(getStartName()) && methodTree.parameters().size() == 1 && methodTree.returnType().symbolType().is("void");
    }
  };
  private static final GetSetPredicate GETTER = new GetSetPredicate() {
    @Override
    public String getStartName() {
      return "get";
    }

    @Override
    public boolean apply(MethodTree methodTree) {
      return methodTree.simpleName().name().startsWith(getStartName()) && methodTree.parameters().isEmpty() && !methodTree.returnType().symbolType().is("void");
    }
  };
  private static final GetSetPredicate GETTER_BOOLEAN = new GetSetPredicate() {
    @Override
    public String getStartName() {
      return "is";
    }

    @Override
    public boolean apply(MethodTree methodTree) {
      return methodTree.simpleName().name().startsWith(getStartName()) && methodTree.parameters().isEmpty() && !methodTree.returnType().symbolType().is("void");
    }
  };

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    checkMethodTree(methodTree, GETTER, SETTER);
    checkMethodTree(methodTree, GETTER_BOOLEAN, SETTER);
    checkMethodTree(methodTree, SETTER, GETTER);
    checkMethodTree(methodTree, SETTER, GETTER_BOOLEAN);
  }

  private void checkMethodTree(MethodTree methodTree, GetSetPredicate ownPredicate, GetSetPredicate pairPredicate) {
    if (isSynchronized(methodTree) && ownPredicate.apply(methodTree)) {
      // Synchronized getter, lookup the setter.
      Symbol.TypeSymbol owner = (Symbol.TypeSymbol) methodTree.symbol().owner();
      Collection<Symbol> pairedMethods = owner.lookupSymbols(pairPredicate.getStartName() + methodTree.symbol().name().substring(ownPredicate.getStartName().length()));
      pairedMethods.stream()
        .filter(Symbol::isMethodSymbol)
        .map(symbol -> (MethodTree) symbol.declaration())
        .filter(pairMethod -> pairPredicate.apply(pairMethod) && !isSynchronized(pairMethod))
        .forEach(pairMethod -> reportIssue(pairMethod.simpleName(), "Synchronize this method to match the synchronization on \"" + methodTree.simpleName().name() + "\".",
          Lists.newArrayList(new JavaFileScannerContext.Location("", methodTree.simpleName())), null));
    }
  }

  private static boolean isSynchronized(MethodTree methodTree) {
    return ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.SYNCHRONIZED) || hasSingleSyncStatement(methodTree);
  }

  private static boolean hasSingleSyncStatement(MethodTree methodTree) {
    BlockTree blockTree = methodTree.block();
    if (blockTree != null && blockTree.body().size() == 1 && blockTree.body().get(0).is(Tree.Kind.SYNCHRONIZED_STATEMENT)) {
      SynchronizedStatementTree sync = (SynchronizedStatementTree) blockTree.body().get(0);
      return ExpressionUtils.isThis(sync.expression());
    }
    return false;
  }

  private interface GetSetPredicate {
    String getStartName();

    boolean apply(MethodTree methodTree);
  }

}
