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
package org.sonar.java.checks.spring;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationValue;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2229")
public class SpringIncompatibleTransactionalCheck extends IssuableSubscriptionVisitor {

  private static final String SPRING_TRANSACTIONAL_ANNOTATION = "org.springframework.transaction.annotation.Transactional";
  private static final String JAVAX_TRANSACTIONAL_ANNOTATION = "javax.transaction.Transactional";

  private static final String MANDATORY = "MANDATORY";
  private static final String NESTED = "NESTED";
  private static final String NEVER = "NEVER";
  private static final String NOT_SUPPORTED = "NOT_SUPPORTED";
  private static final String REQUIRED = "REQUIRED";
  private static final String REQUIRES_NEW = "REQUIRES_NEW";
  private static final String SUPPORTS = "SUPPORTS";

  private static final Map<String, Set<String>> INCOMPATIBLE_PROPAGATION_MAP = buildIncompatiblePropagationMap();

  private static Map<String, Set<String>> buildIncompatiblePropagationMap() {
    Map<String, Set<String>> map = new HashMap<>();
    map.put(null, new HashSet<>(Arrays.asList(MANDATORY, NESTED, REQUIRED, REQUIRES_NEW)));
    map.put(MANDATORY, new HashSet<>(Arrays.asList(NESTED, NEVER, NOT_SUPPORTED, REQUIRES_NEW)));
    map.put(NESTED, new HashSet<>(Arrays.asList(NESTED, NEVER, NOT_SUPPORTED, REQUIRES_NEW)));
    map.put(NEVER, new HashSet<>(Arrays.asList(MANDATORY, NESTED, REQUIRED, REQUIRES_NEW)));
    map.put(NOT_SUPPORTED, new HashSet<>(Arrays.asList(MANDATORY, NESTED, REQUIRED, REQUIRES_NEW)));
    map.put(REQUIRED, new HashSet<>(Arrays.asList(NESTED, NEVER, NOT_SUPPORTED, REQUIRES_NEW)));
    map.put(REQUIRES_NEW, new HashSet<>(Arrays.asList(NESTED, NEVER, NOT_SUPPORTED, REQUIRES_NEW)));
    map.put(SUPPORTS, new HashSet<>(Arrays.asList(MANDATORY, NESTED, NEVER, NOT_SUPPORTED, REQUIRED, REQUIRES_NEW)));
    return map;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    Map<Symbol, String> methodsPropagationMap = collectMethodsPropagation(classTree);
    if (hasSameValues(methodsPropagationMap.values())) {
      return;
    }
    methodsPropagationMap
      .forEach((symbol, propagation) -> checkMethodInvocations((MethodTree) symbol.declaration(), propagation, methodsPropagationMap));
  }

  private void checkMethodInvocations(MethodTree method, @Nullable String callerPropagation, Map<Symbol, String> methodsPropagationMap) {
    BlockTree methodBody = method.block();
    if (methodBody == null) {
      return;
    }
    methodBody.accept(new BaseTreeVisitor() {
      @Override
      public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
        super.visitMethodInvocation(methodInvocation);
        Symbol calleeMethodSymbol = methodInvocation.symbol();
        if (methodsPropagationMap.containsKey(calleeMethodSymbol) && methodInvocationOnThisInstance(methodInvocation)) {
          String calleePropagation = methodsPropagationMap.get(calleeMethodSymbol);
          checkIncompatiblePropagation(methodInvocation, callerPropagation, calleeMethodSymbol, calleePropagation);
        }
      }
    });
  }

  private static boolean methodInvocationOnThisInstance(MethodInvocationTree methodInvocation) {
    if (methodInvocation.symbol().isStatic()) {
      return false;
    }
    ExpressionTree expression = methodInvocation.methodSelect();
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      expression = ((MemberSelectExpressionTree) expression).expression();
      return expression.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) expression).name().equals("this");
    }
    return expression.is(Tree.Kind.IDENTIFIER);
  }

  private void checkIncompatiblePropagation(MethodInvocationTree methodInvocation, @Nullable String callerPropagation, Symbol calleeMethodSymbol, String calleePropagation) {
    Set<String> incompatiblePropagation = INCOMPATIBLE_PROPAGATION_MAP.getOrDefault(callerPropagation, Collections.emptySet());
    if (incompatiblePropagation.contains(calleePropagation)) {
      String message = "\"" + calleeMethodSymbol.name() + "'s\" @Transactional requirement is incompatible with the one for this method.";
      List<JavaFileScannerContext.Location> secondaryLocations = Collections.singletonList(
        new JavaFileScannerContext.Location("", ((MethodTree) calleeMethodSymbol.declaration()).simpleName()));
      reportIssue(ExpressionUtils.methodName(methodInvocation), message, secondaryLocations, null);
    }
  }

  private static Map<Symbol, String> collectMethodsPropagation(ClassTree classTree) {
    Map<Symbol, String> methodPropagationMap = new HashMap<>();
    String classPropagation = getPropagation(classTree.symbol(), null);
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.METHOD)) {
        MethodTree method = (MethodTree) member;
        if (method.symbol().isPublic()) {
          methodPropagationMap.put(method.symbol(), getPropagation(method.symbol(), classPropagation));
        }
      }
    }
    return methodPropagationMap;
  }

  private static boolean hasSameValues(Collection<String> methodsPropagationList) {
    return methodsPropagationList.stream().distinct().count() <= 1;
  }

  @CheckForNull
  private static String getPropagation(Symbol symbol, @Nullable String inheritedPropagation) {
    String defaultValue = inheritedPropagation != null ? inheritedPropagation : REQUIRED;
    List<AnnotationValue> values = symbol.metadata().valuesForAnnotation(SPRING_TRANSACTIONAL_ANNOTATION);
    if (values != null) {
      return getAnnotationAttributeAsString(values, "propagation", defaultValue);
    } else {
      values = symbol.metadata().valuesForAnnotation(JAVAX_TRANSACTIONAL_ANNOTATION);
      if (values != null) {
        return getAnnotationAttributeAsString(values, "value", defaultValue);
      } else {
        return inheritedPropagation;
      }
    }
  }

  private static String getAnnotationAttributeAsString(List<AnnotationValue> values, String attributeName, String defaultValue) {
    return values.stream()
      .filter(annotationValue -> annotationValue.name().equals(attributeName))
      .map(AnnotationValue::value)
      .filter(Tree.class::isInstance)
      .map(Tree.class::cast)
      .map(tree -> tree.is(Tree.Kind.MEMBER_SELECT) ? ((MemberSelectExpressionTree) tree).identifier() : tree)
      .filter(tree -> tree.is(Tree.Kind.IDENTIFIER))
      .map(IdentifierTree.class::cast)
      .map(IdentifierTree::name)
      .findFirst()
      .orElse(defaultValue);
  }

}
