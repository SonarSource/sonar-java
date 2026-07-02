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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8913")
public class RestDataPanacheResourceImplementationCheck extends IssuableSubscriptionVisitor {

  private static final String PANACHE_ENTITY_RESOURCE = "io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource";
  private static final String PANACHE_REPOSITORY_RESOURCE = "io.quarkus.hibernate.orm.rest.data.panache.PanacheRepositoryResource";
  private static final String PANACHE_MONGO_ENTITY_RESOURCE = "io.quarkus.mongodb.rest.data.panache.PanacheMongoEntityResource";
  private static final String PANACHE_MONGO_REPOSITORY_RESOURCE = "io.quarkus.mongodb.rest.data.panache.PanacheMongoRepositoryResource";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.symbol().isUnknown()) {
      return;
    }
    if (implementsPanacheResourceInterface(classTree)) {
      reportIssue(classTree.simpleName(), "Remove this implementation class; Quarkus generates the resource implementation automatically and will ignore this one.");
    }
  }

  private static boolean implementsPanacheResourceInterface(ClassTree classTree) {
    Type classType = classTree.symbol().type();
    return isPanacheResourceType(classType);
  }

  private static boolean isPanacheResourceType(Type type) {
    return type.isSubtypeOf(PANACHE_ENTITY_RESOURCE)
      || type.isSubtypeOf(PANACHE_REPOSITORY_RESOURCE)
      || type.isSubtypeOf(PANACHE_MONGO_ENTITY_RESOURCE)
      || type.isSubtypeOf(PANACHE_MONGO_REPOSITORY_RESOURCE);
  }
}
