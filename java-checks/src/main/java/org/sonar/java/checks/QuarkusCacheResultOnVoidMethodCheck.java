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
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S8908")
public class QuarkusCacheResultOnVoidMethodCheck extends IssuableSubscriptionVisitor {

  private static final String CACHE_RESULT_ANNOTATION = "io.quarkus.cache.CacheResult";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    var mt = (MethodTree) tree;
    TypeTree returnType = mt.returnType();
    // returnType can only be null if the method is a constructor. Since the @CacheResult annotation is not allowed on constructors, and since
    // we hence only visit methods, not constructors, we assume that returnType is not null.
    Type symbolType = returnType.symbolType();
    if (symbolType.isUnknown() || !symbolType.isVoid()) {
      return;
    }
    for (AnnotationTree annotation : mt.modifiers().annotations()) {
      if (annotation.symbolType().is(CACHE_RESULT_ANNOTATION)) {
        reportIssue(annotation, "Methods annotated with \"@CacheResult\" should not return void.");
        return;
      }
    }
  }
}
