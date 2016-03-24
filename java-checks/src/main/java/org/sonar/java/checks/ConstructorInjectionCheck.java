/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayList;
import java.util.List;

@Rule(
  key = "S3306",
  name = "Constructor injection should be used instead of field injection",
  priority = Priority.MAJOR,
  tags = {Tag.DESIGN, Tag.PITFALL})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation("5min")
public class ConstructorInjectionCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    List<Tree> members = ((ClassTree) tree).members();
    List<MethodTree> constructors = filterByKind(members, Tree.Kind.CONSTRUCTOR);
    for (MethodTree constructor : constructors) {
      if (isPrivateConstructor(constructor)) {
        return;
      }
    }
    List<VariableTree> fields = filterByKind(members, Tree.Kind.VARIABLE);
    for (VariableTree field : fields) {
      if (isAnnotatedWithInject(field)) {
        reportIssue(field.simpleName(), "Use constructor injection for this field.");
      }
    }
  }

  private static boolean isPrivateConstructor(MethodTree constructor) {
    return constructor.symbol().isPrivate();
  }

  private static boolean isAnnotatedWithInject(VariableTree field) {
    return field.symbol().metadata().isAnnotatedWith("javax.inject.Inject");
  }

  @SuppressWarnings("unchecked")
  private static <X extends Tree> List<X> filterByKind(List<? extends Tree> list, final Tree.Kind kind) {
    List<Tree> filteredList = new ArrayList<>(list);
    CollectionUtils.filter(filteredList, new Predicate() {
      @Override
      public boolean evaluate(Object object) {
        return ((Tree) object).is(kind);
      }
    });
    return (List<X>) filteredList;
  }

}
