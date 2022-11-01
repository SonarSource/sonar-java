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
package org.sonar.java.checks.spring;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationValue;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
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
  // Made name to represent no annotation
  private static final String NOT_TRANSACTIONAL = "SONAR_NOT_TRANSACTIONAL";

  private static final Map<String, Set<String>> INCOMPATIBLE_PROPAGATION_MAP = buildIncompatiblePropagationMap();

  private static Map<String, Set<String>> buildIncompatiblePropagationMap() {
    Map<String, Set<String>> map = new HashMap<>();
    map.put(NOT_TRANSACTIONAL, new HashSet<>(Arrays.asList(MANDATORY, NESTED, REQUIRED, REQUIRES_NEW)));
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
        if (calleeMethodSymbol.isUnknown()) {
          return;
        }
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
      return ExpressionUtils.isThis(((MemberSelectExpressionTree) expression).expression());
    }
    return expression.is(Tree.Kind.IDENTIFIER);
  }

  private void checkIncompatiblePropagation(MethodInvocationTree methodInvocation, @Nullable String callerPropagation, Symbol calleeMethodSymbol, String calleePropagation) {
    Set<String> incompatiblePropagation = INCOMPATIBLE_PROPAGATION_MAP.getOrDefault(callerPropagation, Collections.emptySet());
    if (incompatiblePropagation.contains(calleePropagation)) {
      String message = "\"" + calleeMethodSymbol.name() + "'s\" @Transactional requirement is incompatible with the one for this method.";
      List<JavaFileScannerContext.Location> secondaryLocations = Collections.singletonList(
        new JavaFileScannerContext.Location("Incompatible method definition.", ((MethodTree) calleeMethodSymbol.declaration()).simpleName()));
      reportIssue(ExpressionUtils.methodName(methodInvocation), message, secondaryLocations, null);
    }
  }

  private static Map<Symbol, String> collectMethodsPropagation(ClassTree classTree) {
    Map<Symbol, String> methodPropagationMap = new HashMap<>();
    // When the propagation of the class itself is unknown (incomplete semantic), we do nothing to avoid FP.
    getPropagationIfKnown(classTree.symbol(), NOT_TRANSACTIONAL).ifPresent(classPropagation -> {
      for (Tree member : classTree.members()) {
        if (member.is(Tree.Kind.METHOD)) {
          MethodTree method = (MethodTree) member;
          if (method.symbol().isPublic()) {
            getPropagationIfKnown(method.symbol(), classPropagation).ifPresent(propagation ->
              methodPropagationMap.put(method.symbol(), propagation)
            );
          }
        }
      }
    });
    return methodPropagationMap;
  }

  private static boolean hasSameValues(Collection<String> methodsPropagationList) {
    return methodsPropagationList.stream().distinct().count() <= 1;
  }

  /**
   * Returns Optional.Empty if the Propagation can not be reliably known: if something has unknown type in the process.
   */
  private static Optional<String> getPropagationIfKnown(Symbol symbol, String inheritedPropagation) {
    String defaultValue = NOT_TRANSACTIONAL.equals(inheritedPropagation) ? REQUIRED : inheritedPropagation;
    Optional<String> propagation = Optional.of(inheritedPropagation);

    for (SymbolMetadata.AnnotationInstance annotationInstance : symbol.metadata().annotations()) {
      Symbol annotationSymbol = annotationInstance.symbol();
      Type annotationType = annotationSymbol.type();
      if (annotationSymbol.isUnknown()) {
        return Optional.empty();
      } else if (annotationType.is(SPRING_TRANSACTIONAL_ANNOTATION)) {
        propagation = getAnnotationAttributeAsString(annotationInstance.values(), "propagation", defaultValue);
      } else if (annotationType.is(JAVAX_TRANSACTIONAL_ANNOTATION)) {
        propagation = getAnnotationAttributeAsString(annotationInstance.values(), "value", defaultValue);
      }
    }
    return propagation;
  }

  private static Optional<String> getAnnotationAttributeAsString(List<AnnotationValue> values, String attributeName, String defaultValue) {
    for (AnnotationValue annotationValue : values) {
      if (attributeName.equals(annotationValue.name())) {
        Object value = annotationValue.value();
        if (value instanceof Symbol.VariableSymbol) {
          // expected values are constant from a Enum, translated into variable symbol
          return Optional.of(((Symbol.VariableSymbol) value).name());
        } else {
          return Optional.empty();
        }
      }
    }
    return Optional.of(defaultValue);
  }

}
