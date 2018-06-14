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
    if (classTree.symbol().metadata().isAnnotatedWith(CONFIGURATION_ANNOTATION) && isNotStatic(classTree)) {
      // we only inspect specific members to avoid analysing inner classes twice
      AutowiredFieldVisitor autowiredFieldVisitor = new AutowiredFieldVisitor();
      classTree.members().stream().filter(m -> m.is(Tree.Kind.VARIABLE)).forEach(m -> m.accept(autowiredFieldVisitor));
      MethodVisitor methodVisitor = new MethodVisitor(autowiredFieldVisitor.autowiredFields.keySet());
      classTree.members().stream().filter(m -> m.is(Tree.Kind.METHOD)).forEach(m -> m.accept(methodVisitor));
      methodVisitor.methodsThatUseAutowiredFields.entrySet().stream()
        .filter(methodsThatUseField -> methodsThatUseField.getValue().size() == 1 &&
          methodsThatUseField.getValue().get(0).symbol().metadata().isAnnotatedWith(BEAN_ANNOTATION))
        .forEach(methodsThatUseField -> reportIssue(
          autowiredFieldVisitor.autowiredFields.get(methodsThatUseField.getKey()).simpleName(),
          String.format(MESSAGE_FORMAT, methodsThatUseField.getValue().get(0).simpleName().name())));
    }
  }

  private static boolean isNotStatic(ClassTree classTree) {
    return classTree.modifiers().modifiers().stream().noneMatch(m -> m.modifier().toString().equalsIgnoreCase("static"));
  }

  private static class AutowiredFieldVisitor extends BaseTreeVisitor {
    private final Map<Symbol, VariableTree> autowiredFields = new HashMap<>();

    @Override
    public void visitVariable(VariableTree tree) {
      Symbol symbol = tree.symbol();
      if (AUTOWIRED_ANNOTATIONS.stream().anyMatch(a -> symbol.metadata().isAnnotatedWith(a))) {
        autowiredFields.put(symbol, tree);
      }
    }
  }

  private static class MethodVisitor extends BaseTreeVisitor {
    private final Map<Symbol, List<MethodTree>> methodsThatUseAutowiredFields = new HashMap<>();

    MethodVisitor(Set<Symbol> autowiredFields) {
      autowiredFields.forEach(f -> methodsThatUseAutowiredFields.put(f, new ArrayList<>()));
    }

    @Override
    public void visitMethod(MethodTree methodTree) {
      IdentifiersVisitor identifiersVisitor = new IdentifiersVisitor(methodsThatUseAutowiredFields.keySet());
      methodTree.accept(identifiersVisitor);
      // for each autowired field that is referenced in this method, add the current method name to the list
      identifiersVisitor.isFieldReferenced.entrySet().stream()
        .filter(Map.Entry::getValue)
        .map(Map.Entry::getKey)
        .forEach(field -> methodsThatUseAutowiredFields.get(field).add(methodTree));
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

}
