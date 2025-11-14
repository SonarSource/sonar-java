/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.AnnotationsHelper.hasUnknownAnnotation;

@Rule(key = "S1258")
public class AtLeastOneConstructorCheck extends IssuableSubscriptionVisitor {

  private static final List<String> EXCLUDED_ANNOTATIONS = Arrays.asList(
    "javax.annotation.ManagedBean",
    "javax.ejb.MessageDriven",
    "javax.ejb.Singleton",
    "javax.ejb.Stateful",
    "javax.ejb.Stateless",
    "javax.jws.WebService",
    "javax.servlet.annotation.WebFilter",
    "javax.servlet.annotation.WebServlet",
    "org.apache.maven.plugins.annotations.Mojo",
    "org.codehaus.plexus.component.annotations.Component",
    "lombok.Builder");

  private static final List<String> AUTOWIRED_ANNOTATIONS = Arrays.asList(
    "javax.annotation.Resource",
    "javax.ejb.EJB",
    "javax.inject.Inject",
    "org.apache.maven.plugins.annotations.Component",
    "org.apache.maven.plugins.annotations.Parameter",
    "org.codehaus.plexus.component.annotations.Requirement",
    "org.codehaus.plexus.component.annotations.Configuration"
    );

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.CLASS, Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    checkClassTree((ClassTree) tree);
  }

  private void checkClassTree(ClassTree tree) {
    IdentifierTree simpleName = tree.simpleName();
    if (simpleName != null && !ModifiersUtils.hasModifier(tree.modifiers(), Modifier.ABSTRACT)
      && !isAnnotationExcluded(tree.symbol())
      && !isBuilderPatternName(simpleName.name())) {
      List<JavaFileScannerContext.Location> uninitializedVariables = new ArrayList<>();
      for (Tree member : tree.members()) {
        if (member.is(Kind.CONSTRUCTOR)) {
          // there is a constructor, no need to check further
          return;
        } else if (member.is(Kind.VARIABLE) && requiresInitialization((VariableTree) member)) {
          uninitializedVariables.add(new JavaFileScannerContext.Location("Uninitialized field", member));
        }
      }
      if (!uninitializedVariables.isEmpty()) {
        reportIssue(simpleName, "Add a constructor to the " + tree.declarationKeyword().text() + ", or provide default values.", uninitializedVariables, null);
      }
    }
  }

  private static boolean requiresInitialization(VariableTree variable) {
    Symbol symbol = variable.symbol();
    return variable.initializer() == null && symbol.isPrivate() && !symbol.isStatic() && !isAutowired(symbol);
  }

  private static boolean isAutowired(Symbol symbol) {
    SymbolMetadata metadata = symbol.metadata();
    return hasUnknownAnnotation(metadata) || AUTOWIRED_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith);
  }

  private static boolean isAnnotationExcluded(Symbol symbol) {
    SymbolMetadata metadata = symbol.metadata();
    return hasUnknownAnnotation(metadata) || EXCLUDED_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith);
  }

  private static boolean isBuilderPatternName(String name) {
    return name.endsWith("Builder");
  }

}
