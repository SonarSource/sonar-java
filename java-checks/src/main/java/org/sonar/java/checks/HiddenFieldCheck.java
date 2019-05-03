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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sonar.check.Rule;
import org.sonar.java.RspecKey;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Rule(key = "HiddenFieldCheck")
@RspecKey("S1117")
public class HiddenFieldCheck extends IssuableSubscriptionVisitor {

  private final Deque<ImmutableMap<String, VariableTree>> fields = new LinkedList<>();
  private final Deque<List<VariableTree>> excludedVariables = new LinkedList<>();
  private final List<VariableTree> flattenExcludedVariables = new ArrayList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
        Tree.Kind.CLASS,
        Tree.Kind.ENUM,
        Tree.Kind.INTERFACE,
        Tree.Kind.ANNOTATION_TYPE,
        Tree.Kind.VARIABLE,
        Tree.Kind.METHOD,
        Tree.Kind.CONSTRUCTOR,
        Tree.Kind.STATIC_INITIALIZER
    );
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    fields.clear();
    excludedVariables.clear();
    flattenExcludedVariables.clear();
    super.setContext(context);
  }

  @Override
  public void visitNode(Tree tree) {
    if(!hasSemantic()) {
      return;
    }
    if (isClassTree(tree)) {
      ClassTree classTree = (ClassTree) tree;
      ImmutableMap.Builder<String, VariableTree> builder = ImmutableMap.builder();
      for (Tree member : classTree.members()) {
        if (member.is(Tree.Kind.VARIABLE)) {
          VariableTree variableTree = (VariableTree) member;
          builder.put(variableTree.simpleName().name(), variableTree);
        }
      }
      fields.push(builder.build());
      excludedVariables.push(new ArrayList<>());
    } else if (tree.is(Tree.Kind.VARIABLE)) {
      VariableTree variableTree = (VariableTree) tree;
      isVariableHidingField(variableTree);
    } else if (tree.is(Tree.Kind.STATIC_INITIALIZER)) {
      excludeVariablesFromBlock((BlockTree) tree);
    } else {
      MethodTree methodTree = (MethodTree) tree;
      excludedVariables.peek().addAll(methodTree.parameters());
      flattenExcludedVariables.addAll(methodTree.parameters());
      if (ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.STATIC)) {
        excludeVariablesFromBlock(methodTree.block());
      }
    }
  }

  private void isVariableHidingField(VariableTree variableTree) {
    for (ImmutableMap<String, VariableTree> variables : fields) {
      if (variables.values().contains(variableTree)) {
        return;
      }
      String identifier = variableTree.simpleName().name();
      VariableTree hiddenVariable = variables.get(identifier);
      if (!flattenExcludedVariables.contains(variableTree) && hiddenVariable != null && !isInStaticInnerClass(hiddenVariable, variableTree)) {
        int line = hiddenVariable.firstToken().line();
        reportIssue(variableTree.simpleName(), "Rename \"" + identifier + "\" which hides the field declared at line " + line + ".");
        return;
      }
    }
  }

  private static boolean isInStaticInnerClass(VariableTree hiddenVariable, VariableTree variableTree) {
    Symbol hiddenVariableOwner = hiddenVariable.symbol().owner();
    Symbol owner = variableTree.symbol().owner();
    while (!owner.equals(hiddenVariableOwner)) {
      if (owner.isTypeSymbol() && owner.isStatic()) {
        return true;
      }
      owner = owner.owner();
    }
    return false;
  }

  private static boolean isClassTree(Tree tree) {
    return tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.ENUM) || tree.is(Tree.Kind.INTERFACE) || tree.is(Tree.Kind.ANNOTATION_TYPE);
  }

  @Override
  public void leaveNode(Tree tree) {
    if(!hasSemantic()) {
      return;
    }
    if (isClassTree(tree)) {
      fields.pop();
      flattenExcludedVariables.removeAll(excludedVariables.pop());
    }
  }

  private void excludeVariablesFromBlock(@Nullable BlockTree blockTree) {
    if (blockTree != null) {
      List<VariableTree> variableTrees = new VariableList().scan(blockTree);
      excludedVariables.peek().addAll(variableTrees);
      flattenExcludedVariables.addAll(variableTrees);
    }
  }

  private static class VariableList {

    private List<VariableTree> variables;
    private List<Tree.Kind> visitNodes;
    private List<Tree.Kind> excludedNodes;

    List<VariableTree> scan(Tree tree) {
      visitNodes = nodesToVisit();
      excludedNodes = excludedNodes();
      variables = new ArrayList<>();
      visit(tree);
      return variables;
    }

    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.VARIABLE);
    }

    public List<Tree.Kind> excludedNodes() {
      return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.NEW_CLASS);
    }

    private void visit(Tree tree) {
      if (isSubscribed(tree)) {
        variables.add((VariableTree) tree);
      }
      visitChildren(tree);
    }

    private void visitChildren(Tree tree) {
      JavaTree javaTree = (JavaTree) tree;
      if (!javaTree.isLeaf()) {
        for (Tree next : javaTree.getChildren()) {
          if (next != null && !isExcluded(next)) {
            visit(next);
          }
        }
      }
    }

    private boolean isSubscribed(Tree tree) {
      return visitNodes.contains(tree.kind());
    }

    private boolean isExcluded(Tree tree) {
      return excludedNodes.contains(tree.kind());
    }
  }

}
