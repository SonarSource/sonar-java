/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
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
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;
import org.sonarsource.analyzer.commons.collections.MapBuilder;
import org.sonarsource.analyzer.commons.collections.SetUtils;

@DeprecatedRuleKey(ruleKey = "HiddenFieldCheck", repositoryKey = "squid")
@Rule(key = "S1117")
public class HiddenFieldCheck extends IssuableSubscriptionVisitor {

  private final Deque<Map<String, VariableTree>> fields = new LinkedList<>();
  private final Deque<List<VariableTree>> excludedVariables = new LinkedList<>();
  private final Set<VariableTree> flattenExcludedVariables = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(
        Tree.Kind.CLASS,
        Tree.Kind.ENUM,
        Tree.Kind.INTERFACE,
        Tree.Kind.ANNOTATION_TYPE,
        Tree.Kind.RECORD,
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
    if (isClassTree(tree)) {
      ClassTree classTree = (ClassTree) tree;
      MapBuilder<String, VariableTree> builder = MapBuilder.newMap();
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
    for (Map<String, VariableTree> variables : fields) {
      if (variables.containsValue(variableTree)) {
        return;
      }
      String identifier = variableTree.simpleName().name();
      VariableTree hiddenVariable = variables.get(identifier);
      if (!flattenExcludedVariables.contains(variableTree) && hiddenVariable != null && !isInStaticInnerClass(hiddenVariable, variableTree)) {
        int line = hiddenVariable.firstToken().range().start().line();
        reportIssue(variableTree.simpleName(), "Rename \"" + identifier + "\" which hides the field declared at line " + line + ".");
        return;
      }
    }
  }

  private static boolean isInStaticInnerClass(VariableTree hiddenVariable, VariableTree variableTree) {
    Symbol hiddenVariableOwner = hiddenVariable.symbol().owner();
    Symbol owner = variableTree.symbol().owner();
    while (owner != null && !owner.equals(hiddenVariableOwner)) {
      if (owner.isTypeSymbol() && owner.isStatic()) {
        return true;
      }
      owner = owner.owner();
    }
    return false;
  }

  private static boolean isClassTree(Tree tree) {
    return tree.is(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE, Tree.Kind.RECORD);
  }

  @Override
  public void leaveNode(Tree tree) {
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
    private Set<Tree.Kind> visitNodes;
    private Set<Tree.Kind> excludedNodes;

    List<VariableTree> scan(Tree tree) {
      visitNodes = nodesToVisit();
      excludedNodes = excludedNodes();
      variables = new ArrayList<>();
      visit(tree);
      return variables;
    }

    public Set<Tree.Kind> nodesToVisit() {
      return Collections.singleton(Tree.Kind.VARIABLE);
    }

    public Set<Tree.Kind> excludedNodes() {
      return SetUtils.immutableSetOf(Tree.Kind.METHOD, Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.NEW_CLASS);
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
