/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.spring;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.DefaultModuleScannerContext;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4605")
public class SpringInnovationCheck extends IssuableSubscriptionVisitor implements EndOfAnalysis {
  private static final String[] SPRING_INJECTION_ANNOTATIONS = {
    "org.springframework.beans.factory.annotation.Autowired"
  };

  record Location(AnalyzerMessage analyzerMessage) {}
  Map<String, Set<Location>> injections = new HashMap<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof ClassTree classTree) {
      DefaultJavaFileScannerContext defaultContext = (DefaultJavaFileScannerContext) context;

      for (Tree member: classTree.members()) {
        if (member instanceof VariableTree variableTree) {
          if (hasAnnotation(variableTree.symbol().metadata(), SPRING_INJECTION_ANNOTATIONS)) {
            String typeFqn = variableTree.symbol().type().fullyQualifiedName();

            Set<Location> locations = injections.computeIfAbsent(typeFqn, k -> new HashSet<>());

            // Just on the `simpleName()`, because for simplicity we don't want to include the annotation.
            AnalyzerMessage message = defaultContext.createAnalyzerMessage(this, variableTree.simpleName(), "More than one candidate implementation");
            locations.add(new Location(message));
          }
        }
      }
    }
  }

  private static boolean hasAnnotation(SymbolMetadata classSymbolMetadata, String... annotationName) {
    return Arrays.stream(annotationName).anyMatch(classSymbolMetadata::isAnnotatedWith);
  }

  @Override
  public void endOfAnalysis(ModuleScannerContext context) {
    var defaultContext = (DefaultModuleScannerContext) context;

    for(Map.Entry<String,Set<Location>> entry : injections.entrySet()) {
      String typeFqn = entry.getKey();
      Set<Location> locations = entry.getValue();
      if (defaultContext.projectContextModelReader.availableImpls().get(typeFqn).size() > 1) {
        for(Location location :locations) {
          defaultContext.reportIssue(location.analyzerMessage());
        }
      }
    }
  }
}
