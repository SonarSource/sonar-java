/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
    if (hasSemantic() && classTree.symbol().metadata().isAnnotatedWith(CONFIGURATION_ANNOTATION)) {
      AutowiredFieldVisitor autowiredFieldVisitor = new AutowiredFieldVisitor();
      classTree.accept(autowiredFieldVisitor);
      BeanMethodVisitor beanMethodVisitor = new BeanMethodVisitor(autowiredFieldVisitor.autowiredFields.keySet());
      classTree.accept(beanMethodVisitor);
      beanMethodVisitor.beanMethodsThatUseAutowiredField.entrySet().stream()
        .filter(fieldSymbolAndBeanMethods -> fieldSymbolAndBeanMethods.getValue().size() == 1)
        .forEach(fieldSymbolAndBeanMethods -> reportIssue(
          autowiredFieldVisitor.autowiredFields.get(fieldSymbolAndBeanMethods.getKey()).simpleName(),
          String.format(MESSAGE_FORMAT, fieldSymbolAndBeanMethods.getValue().get(0))));
    }
  }

  private static class AutowiredFieldVisitor extends BaseTreeVisitor {
    private final Map<Symbol, VariableTree> autowiredFields = new HashMap<>();

    @Override
    public void visitVariable(VariableTree tree) {
      Symbol symbol = tree.symbol();
      if (symbol.owner().isTypeSymbol() && AUTOWIRED_ANNOTATIONS.stream().anyMatch(a -> symbol.metadata().isAnnotatedWith(a))) {
        autowiredFields.put(symbol, tree);
      }
    }
  }

  private static class BeanMethodVisitor extends BaseTreeVisitor {
    private final Map<Symbol, List<String>> beanMethodsThatUseAutowiredField = new HashMap<>();

    BeanMethodVisitor(Set<Symbol> autowiredFields) {
      autowiredFields.forEach(f -> beanMethodsThatUseAutowiredField.put(f, new ArrayList<>()));
    }

    @Override
    public void visitMethod(MethodTree methodTree) {
      if (methodTree.symbol().metadata().isAnnotatedWith(BEAN_ANNOTATION)) {
        IdentifiersVisitor identifiersVisitor = new IdentifiersVisitor(beanMethodsThatUseAutowiredField.keySet());
        methodTree.accept(identifiersVisitor);
        // for each autowired field that is referenced in this method, add the current method name to the list
        identifiersVisitor.isFieldReferenced.entrySet().stream()
          .filter(Map.Entry::getValue)
          .map(Map.Entry::getKey)
          .forEach(field -> beanMethodsThatUseAutowiredField.get(field).add(methodTree.simpleName().name()));
      }
    }

    private static class IdentifiersVisitor extends BaseTreeVisitor {
      private final Map<Symbol, Boolean> isFieldReferenced = new HashMap<>();

      IdentifiersVisitor(Set<Symbol> autowiredFields) {
        autowiredFields.forEach(f -> isFieldReferenced.put(f, false));
      }

      @Override
      public void visitIdentifier(IdentifierTree identifierTree) {
        isFieldReferenced.computeIfPresent(identifierTree.symbol(), (fieldSym, isPresent) -> isPresent = true);
      }
    }
  }

}
