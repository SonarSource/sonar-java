/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4265")
public class UnsupportedOperationUsageCheck extends BaseTreeVisitor implements JavaFileScanner {
  private static final Logger LOGGER = Loggers.get(UnsupportedOperationUsageCheck.class);
  private static Map<String, Set<String>> problemMethods = new HashMap<>();
  static {
    problemMethods.put("java.util.Arrays#asList([Ljava/lang/Object;)Ljava/util/List;",
      new HashSet<>(Arrays.asList("add", "addAll", "remove", "removeAll", "clear")));
    problemMethods.put("java.util.Collections#emptyList()Ljava/util/List;",
      new HashSet<>(Arrays.asList("add", "addAll")));
    problemMethods.put("java.util.Collections#emptySet()Ljava/util/Set;",
      new HashSet<>(Arrays.asList("add", "addAll")));
    problemMethods.put("java.util.Collections#emptyMap()Ljava/util/Map;",
      new HashSet<>(Arrays.asList("put", "putAll")));
    problemMethods.put("org.apache.commons.collections.list.FixedSizeList#decorate(Ljava/util/List;)Ljava/util/List;",
      new HashSet<>(Arrays.asList("add", "addAll", "remove", "removeAll", "clear", "retainAll")));
    problemMethods.put("org.apache.commons.collections4.list.FixedSizeList#fixedSizeList(Ljava/util/List;)Lorg/apache/commons/collections4/list/FixedSizeList;",
      new HashSet<>(Arrays.asList("add", "addAll", "remove", "removeIf", "removeAll", "clear", "retainAll")));

    Set<String> immutableSetMethods = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("add", "addAll", "clear", "remove", "removeIf", "removeAll", "retainAll")));
    Set<String> immutableListMethods = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("add", "addAll", "remove", "replaceAll", "removeIf", "removeAll",
      "retainAll", "set", "sort", "clear")));
    Set<String> immutableMapMethods = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("clear", "compute", "computeIfAbsent", "computeIfPresent", "merge", "put",
      "putAll", "putIfAbsent", "remove", "replace", "replaceAll")));

    String prefixSet = "java.util.Set#of(";
    String postfixSet = ")Ljava/util/Set;";
    String prefixList = "java.util.List#of(";
    String postfixList = ")Ljava/util/List;";
    String prefixMap = "java.util.Map#of(";
    String postfixMap = ")Ljava/util/Map;";
    String parameter = "Ljava/lang/Object;";
    for (int i = 0; i <= 10; i++) {
      String repeated = StringUtils.repeat(parameter, i);

      StringBuilder sbSet = new StringBuilder(prefixSet).append(repeated).append(postfixSet);
      problemMethods.put(sbSet.toString(), immutableSetMethods);

      StringBuilder sbList = new StringBuilder(prefixList).append(repeated).append(postfixList);
      problemMethods.put(sbList.toString(), immutableListMethods);

      StringBuilder sbMap = new StringBuilder(prefixMap).append(repeated).append(repeated).append(postfixMap);
      problemMethods.put(sbMap.toString(), immutableMapMethods);
    }
    problemMethods.put(new StringBuilder(prefixSet).append("[").append(parameter).append(postfixSet).toString(), immutableSetMethods);
    problemMethods.put(new StringBuilder(prefixList).append("[").append(parameter).append(postfixList).toString(), immutableListMethods);
  }
  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    String problemSignature = checkVariableInitialization(tree.expression());
    if (problemSignature != null && tree.variable() instanceof IdentifierTree) {
      processProblemSignature(problemSignature, (IdentifierTree) tree.variable());
    }
    super.visitAssignmentExpression(tree);
  }

  private void processProblemSignature(String problemSignature, IdentifierTree tree) {
    List<IdentifierTree> usages = tree.symbol().usages();
    for (IdentifierTree usage : usages) {
      IdentifierTree problemMethodCall = checkUnsupportedOperationCall(usage, problemMethods.getOrDefault(problemSignature, Collections.emptySet()));
      if (problemMethodCall != null) {
        context.reportIssue(this, problemMethodCall, "Remove this call to \"" + problemMethodCall.name()
          + "\"; it will cause an exception for this fixed-length collection.");
      }
    }
  }

  @Override
  public void visitVariable(VariableTree tree) {
    if (tree.initializer() != null) {
      String problemSignature = checkVariableInitialization(tree.initializer());
      if (problemSignature != null) {
        processProblemSignature(problemSignature, tree.simpleName());
      }
    }
    super.visitVariable(tree);
  }

  private String checkVariableInitialization(ExpressionTree tree) {
    if (tree instanceof MethodInvocationTree) {
      String signature = ((MethodInvocationTree) tree).methodSymbol().signature();
      if (problemMethods.containsKey(signature)) {
        return signature;
      }
    }
    return null;
  }

  private IdentifierTree checkUnsupportedOperationCall(IdentifierTree tree, Collection<String> methods) {
    ExpressionTree calledMethod = null;
    String calledMethodName = null;
    if (tree.parent() instanceof MemberSelectExpressionTree) {
      calledMethod = ((MemberSelectExpressionTree) tree.parent()).identifier();
      calledMethodName = ((MemberSelectExpressionTree) tree.parent()).identifier().name();
    } else if (tree.parent() instanceof AssignmentExpressionTree && ((AssignmentExpressionTree) tree.parent()).expression() instanceof MethodInvocationTree) {
      calledMethod = ((AssignmentExpressionTree) tree.parent()).expression();
      calledMethodName = ((MethodInvocationTree) calledMethod).methodSymbol().name();
    } else {
      LOGGER
        .debug(() -> "checkUnsupportedOperationCall is invoked with parameter tree which has parent of type "
          + tree.parent() != null ? tree.parent().getClass().getCanonicalName()
            : null + ". Therefore analysis can be not fully evaluated");
      return null;
    }
    if (calledMethodName != null && methods.contains(calledMethodName)) {
      return new IdentifierTreeImpl(constructInternalSyntaxToken(calledMethod, calledMethodName));
    }
    return null;
  }

  private InternalSyntaxToken constructInternalSyntaxToken(ExpressionTree tree, String methodName) {
    return new InternalSyntaxToken(tree.firstToken().range().start().line(), tree.firstToken().range().start().columnOffset(),
      methodName, Collections.emptyList(), false);
  }
}
