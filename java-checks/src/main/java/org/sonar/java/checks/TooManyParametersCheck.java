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
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

import static org.sonar.java.checks.helpers.AnnotationsHelper.hasUnknownAnnotation;

@DeprecatedRuleKey(ruleKey = "S00107", repositoryKey = "squid")
@Rule(key = "S107")
public class TooManyParametersCheck extends IssuableSubscriptionVisitor {

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
    "javax.inject.Inject",
    "org.springframework.context.annotation.Bean",
    "io.micronaut.http.annotation.Get",
    "io.micronaut.http.annotation.Post",
    "io.micronaut.http.annotation.Put",
    "io.micronaut.http.annotation.Delete",
    "io.micronaut.http.annotation.Options",
    "io.micronaut.http.annotation.Patch",
    "io.micronaut.http.annotation.Head",
    "io.micronaut.http.annotation.Trace"
  );
  
  //if a class is annotated as one of these types, its constructor should be ignored
  private static final List<String> CLASS_CONSTRUCTOR_WHITELIST = Arrays.asList(
    "org.springframework.stereotype.Component",
    "org.springframework.context.annotation.Configuration",
    "org.springframework.stereotype.Service",
    "org.springframework.stereotype.Repository"
    );


  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS);
  }
  

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if(classUsesAuthorizedAnnotation(classTree)) {
      return;
    }
    classTree.members().stream()
    .filter(member -> member.is(Tree.Kind.CONSTRUCTOR, Tree.Kind.METHOD))
    .forEach( member -> visitMethod((MethodTree) member) );
  }
  
  private void visitMethod(MethodTree method) {
    if (isOverriding(method) || usesAuthorizedAnnotation(method)) {
      return;
    }
    int max;
    String partialMessage;
    if (method.is(Tree.Kind.CONSTRUCTOR)) {
      max = constructorMax;
      partialMessage = "Constructor";
    } else {
      max = maximum;
      partialMessage = "Method";
    }
    int size = method.parameters().size();
    if (size > max) {
      reportIssue(method.simpleName(), partialMessage + " has " + size + " parameters, which is greater than " + max + " authorized.");
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

  //As of Spring 4.3, classes (@Component, @Service, etc..) with a single constructor can omit the @Autowired annotation.
  private static boolean classUsesAuthorizedAnnotation(ClassTree methodParentClass) {
    SymbolMetadata parentClassMetadata = methodParentClass.symbol().metadata();
    
    //if the parent class is a Spring component
    if(CLASS_CONSTRUCTOR_WHITELIST.stream().anyMatch(parentClassMetadata::isAnnotatedWith)) {
      long numberOfConstructors = methodParentClass.members().stream().filter(member -> member.is(Tree.Kind.CONSTRUCTOR)).count();
      //if it only has 1 constructor, @Autowired is implicit, and it's an exception to the rule
      if(numberOfConstructors == 1) return true;
    }
    return false;
  }
  
}
