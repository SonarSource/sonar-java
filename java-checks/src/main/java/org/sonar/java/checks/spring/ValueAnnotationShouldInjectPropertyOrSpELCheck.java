/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6804")
public class ValueAnnotationShouldInjectPropertyOrSpELCheck extends IssuableSubscriptionVisitor {

  private static final String SPRING_VALUE = "org.springframework.beans.factory.annotation.Value";
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    AnnotationTree ann = (AnnotationTree) tree;
    List<ExpressionTree> arguments = ann.arguments();
    if(ann.symbolType().is(SPRING_VALUE)
      && !inMethodDeclaration(ann)){

      LiteralTree literal = (LiteralTree)arguments.get(0);
      String value = literal.value();

      if(!isPropertyName(value) && !isSpEL(value)){
        reportIssue(
          ann,
          "Either replace the \"@Value\" annotation with a standard field initialization," +
          " use \"${propertyname}\" to inject a property " +
          "or use \"#{expression}\" to evaluate a SpEL expression.");
      }

    }
  }

  private static boolean inMethodDeclaration(AnnotationTree ann){
    boolean appliedOnMethod = parentHasKind(ann.parent().parent(), Tree.Kind.METHOD);
    boolean appliedOnParameter = parentHasKind(ann.parent().parent(), Tree.Kind.VARIABLE)
      && parentHasKind(ann.parent().parent().parent(), Tree.Kind.METHOD);

    return appliedOnMethod || appliedOnParameter;
  }

  private static boolean parentHasKind(@Nullable Tree parent, Tree.Kind kind){
    return parent!=null && parent.is(kind);
  }

  private static boolean isPropertyName(String value){
    return value.startsWith("\"${") && value.endsWith("}\"");
  }

  private static boolean isSpEL(String value){
    return value.startsWith("\"#{") && value.endsWith("}\"");
  }


}
