/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2206")
public class PersistenceAnnotationsMixedCheck extends IssuableSubscriptionVisitor {

  private static final List<String> ENTITY_ANNOTATIONS = List.of("javax.persistence.Entity", "jakarta.persistence.Entity", "javax.persistence.Embeddable", "jakarta.persistence" +
    ".Embeddable", "javax.persistence.MappedSuperclass", "jakarta.persistence.MappedSuperclass");

  private static final List<String> PERSISTENCE_MAPPING_ANNOTATIONS = List.of("javax.persistence.Id", "jakarta.persistence.Id", "javax.persistence.EmbeddedId", "jakarta" +
    ".persistence.EmbeddedId", "javax.persistence.Column", "jakarta.persistence.Column", "javax.persistence.Basic", "jakarta.persistence.Basic", "javax.persistence.OneToOne",
    "jakarta.persistence.OneToOne", "javax.persistence.OneToMany", "jakarta.persistence.OneToMany", "javax.persistence.ManyToOne", "jakarta.persistence.ManyToOne", "javax" +
      ".persistence.ManyToMany", "jakarta.persistence.ManyToMany", "javax.persistence.ElementCollection", "jakarta.persistence.ElementCollection", "javax.persistence.Embedded",
    "jakarta.persistence.Embedded", "javax.persistence.Lob", "jakarta.persistence.Lob", "javax.persistence.Temporal", "jakarta.persistence.Temporal", "javax.persistence" +
      ".Enumerated", "jakarta.persistence.Enumerated", "javax.persistence.GeneratedValue", "jakarta.persistence.GeneratedValue", "javax.persistence.Version", "jakarta" +
      ".persistence.Version");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    SymbolMetadata classMetadata = classTree.symbol().metadata();
    if (ENTITY_ANNOTATIONS.stream().noneMatch(classMetadata::isAnnotatedWith)) {
      return;
    }

    boolean hasAnnotatedFields =
      classTree.members().stream().filter(m -> m.is(Tree.Kind.VARIABLE)).map(VariableTree.class::cast).anyMatch(v -> hasPersistenceAnnotation(v.symbol().metadata()));

    boolean hasAnnotatedMethods =
      classTree.members().stream().filter(m -> m.is(Tree.Kind.METHOD)).map(MethodTree.class::cast).anyMatch(method -> hasPersistenceAnnotation(method.symbol().metadata()));

    if (hasAnnotatedFields && hasAnnotatedMethods) {
      reportIssue(classTree.simpleName(), "Annotate either fields or getters for persistence, but not both.");
    }
  }

  private static boolean hasPersistenceAnnotation(SymbolMetadata metadata) {
    return PERSISTENCE_MAPPING_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith);
  }
}
