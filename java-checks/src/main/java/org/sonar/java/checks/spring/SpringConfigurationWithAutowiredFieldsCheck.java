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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3305")
public class SpringConfigurationWithAutowiredFieldsCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE_FORMAT = "Inject this field value directly into \"%s\", the only method that uses it.";

  private static final String CONFIGURATION_ANNOTATION = "org.springframework.context.annotation.Configuration";
  private static final String BEAN_ANNOTATION = "org.springframework.context.annotation.Bean";
  private static final List<String> AUTOWIRED_ANNOTATIONS = Arrays.asList(
    "org.springframework.beans.factory.annotation.Autowired",
    "javax.inject.Inject");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.symbol().metadata().isAnnotatedWith(CONFIGURATION_ANNOTATION)) {
      Map<Symbol, VariableTree> autowiredFields = new HashMap<>();
      classTree.members().forEach(m -> collectAutowiredFields(m, autowiredFields));

      Map<Symbol, List<MethodTree>> methodsThatUseAutowiredFields = new HashMap<>();
      autowiredFields.keySet().forEach(f -> methodsThatUseAutowiredFields.put(f, new ArrayList<>()));
      classTree.members().forEach(m -> collectMethodsThatUseAutowiredFields(m, methodsThatUseAutowiredFields));

      // report autowired fields that are used by a single method, if that method is @Bean
      methodsThatUseAutowiredFields.entrySet().stream()
        .filter(methodsForField -> methodsForField.getValue().size() == 1 &&
          methodsForField.getValue().get(0).symbol().metadata().isAnnotatedWith(BEAN_ANNOTATION))
        .forEach(methodsForField -> reportIssue(
          autowiredFields.get(methodsForField.getKey()).simpleName(),
          String.format(MESSAGE_FORMAT, methodsForField.getValue().get(0).simpleName().name())));
    }
  }

  private static void collectAutowiredFields(Tree tree, Map<Symbol, VariableTree> autowiredFields) {
    if (!tree.is(Tree.Kind.VARIABLE)) {
      return;
    }
    VariableTree variable = (VariableTree) tree;
    Symbol variableSymbol = variable.symbol();
    if (AUTOWIRED_ANNOTATIONS.stream().anyMatch(a -> variableSymbol.metadata().isAnnotatedWith(a))) {
      autowiredFields.put(variableSymbol, variable);
    }
  }

  private static void collectMethodsThatUseAutowiredFields(Tree tree, Map<Symbol, List<MethodTree>> methodsThatUseAutowiredFields) {
    if (!tree.is(Tree.Kind.METHOD)) {
      return;
    }
    IdentifiersVisitor identifiersVisitor = new IdentifiersVisitor(methodsThatUseAutowiredFields.keySet());
    tree.accept(identifiersVisitor);
    // for each autowired field that is referenced in this method, add the current method name to the list
    identifiersVisitor.isFieldReferenced.entrySet().stream()
      .filter(Map.Entry::getValue)
      .map(Map.Entry::getKey)
      .forEach(field -> methodsThatUseAutowiredFields.get(field).add((MethodTree) tree));
  }

  private static class IdentifiersVisitor extends BaseTreeVisitor {
    private final Map<Symbol, Boolean> isFieldReferenced = new HashMap<>();

    IdentifiersVisitor(Set<Symbol> autowiredFields) {
      autowiredFields.forEach(f -> isFieldReferenced.put(f, false));
    }

    @Override
    public void visitIdentifier(IdentifierTree identifierTree) {
      isFieldReferenced.computeIfPresent(identifierTree.symbol(), (fieldSym, isPresent) -> true);
    }
  }
}
