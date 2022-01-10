/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "S00107", repositoryKey = "squid")
@Rule(key = "S107")
public class TooManyParametersCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final int DEFAULT_MAXIMUM = 7;

  @RuleProperty(
    key = "max",
    description = "Maximum authorized number of parameters",
    defaultValue = "" + DEFAULT_MAXIMUM)
  public int maximum = DEFAULT_MAXIMUM;

  @RuleProperty(
    key = "constructorMax",
    description = "Maximum authorized number of parameters for a constructor",
    defaultValue = "" + DEFAULT_MAXIMUM)
  public int constructorMax = DEFAULT_MAXIMUM;

  private JavaFileScannerContext context;

  private static final List<String> WHITE_LIST = Arrays.asList(
    "org.springframework.web.bind.annotation.RequestMapping",
    "org.springframework.web.bind.annotation.GetMapping",
    "org.springframework.web.bind.annotation.PostMapping",
    "org.springframework.web.bind.annotation.PutMapping",
    "org.springframework.web.bind.annotation.DeleteMapping",
    "org.springframework.web.bind.annotation.PatchMapping",
    "com.fasterxml.jackson.annotation.JsonCreator",
    "javax.ws.rs.GET",
    "javax.ws.rs.POST",
    "javax.ws.rs.PUT",
    "javax.ws.rs.PATCH",
    "org.springframework.beans.factory.annotation.Autowired",
    "javax.inject.Inject"
  );

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree tree) {
    super.visitMethod(tree);
    if (isOverriding(tree) || usesAuthorizedAnnotation(tree)) {
      return;
    }
    int max;
    String partialMessage;
    if (tree.is(Tree.Kind.CONSTRUCTOR)) {
      max = constructorMax;
      partialMessage = "Constructor";
    } else {
      max = maximum;
      partialMessage = "Method";
    }
    int size = tree.parameters().size();
    if (size > max) {
      context.reportIssue(this, tree.simpleName(), partialMessage + " has " + size + " parameters, which is greater than " + max + " authorized.");
    }
  }

  private static boolean isOverriding(MethodTree tree) {
    // In case of unknown hierarchy, isOverriding() returns null, we return true to avoid FPs.
    return !Boolean.FALSE.equals(tree.isOverriding());
  }

  private static boolean usesAuthorizedAnnotation(MethodTree method) {
    SymbolMetadata metadata = method.symbol().metadata();
    return hasUnknownAnnotation(metadata) || WHITE_LIST.stream().anyMatch(metadata::isAnnotatedWith);
  }

  private static boolean hasUnknownAnnotation(SymbolMetadata symbolMetadata) {
    return symbolMetadata.annotations().stream().anyMatch(annotation -> annotation.symbol().isUnknown());
  }

}
