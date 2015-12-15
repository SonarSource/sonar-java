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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol.TypeSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1609",
  name = "@FunctionalInterface annotation should be used to flag Single Abstract Method interfaces",
  priority = Priority.MAJOR,
  tags = {Tag.JAVA_8})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("2min")
public class SAMAnnotatedCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final ImmutableMultimap<String, List<String>> OBJECT_METHODS = new ImmutableMultimap.Builder<String, List<String>>().
    put("equals", ImmutableList.of("Object")).
    put("getClass", ImmutableList.<String>of()).
    put("hashcode", ImmutableList.<String>of()).
    put("notify", ImmutableList.<String>of()).
    put("notifyAll", ImmutableList.<String>of()).
    put("toString", ImmutableList.<String>of()).
    put("wait", ImmutableList.<String>of()).
    put("wait", ImmutableList.of("long")).
    put("wait", ImmutableList.of("long", "int")).
    build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (hasOneAbstractMethod(classTree) && !isAnnotated(classTree)) {
      addIssue(
        tree,
        "Annotate the \"" + classTree.simpleName().name() + "\" interface with the @FunctionalInterface annotation" +
          context.getJavaVersion().java8CompatibilityMessage());
    }
  }

  private static boolean isAnnotated(ClassTree tree) {
    for (AnnotationTree annotationTree : tree.modifiers().annotations()) {
      Tree annotationType = annotationTree.annotationType();
      if (annotationType.is(Tree.Kind.IDENTIFIER) && "FunctionalInterface".equals(((IdentifierTree) annotationType).name())) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasOneAbstractMethod(ClassTree classTree) {
    TypeSymbol symbol = classTree.symbol();
    if (symbol != null) {
      List<Type> types = symbol.interfaces();
      for (Type type : types) {
        if (!type.symbol().memberSymbols().isEmpty()) {
          return false;
        }
      }
    }
    int methods = 0;
    for (Tree member : classTree.members()) {
      boolean isMethod = member.is(Tree.Kind.METHOD);
      if (!isMethod) {
        return false;
      }
      if (isNotObjectMethod((MethodTree) member) && isNonStaticNonDefaultMethod(member)) {
        methods++;
      }
    }
    return methods == 1;
  }

  private static boolean isNotObjectMethod(MethodTree method) {
    ImmutableCollection<List<String>> methods = OBJECT_METHODS.get(method.simpleName().name());
    if (methods != null) {
      for (List<String> types : methods) {
        if (sameParameters(method.parameters(), types)) {
          return false;
        }
      }
    }
    return true;
  }

  private static boolean sameParameters(List<VariableTree> provided, List<String> expectedTypes) {
    List<String> args = Lists.newArrayList(expectedTypes);
    if (provided.size() == args.size()) {
      for (VariableTree var : provided) {
        args.remove(var.type().symbolType().name());
      }
      if (args.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private static boolean isNonStaticNonDefaultMethod(Tree memberTree) {
    boolean result = memberTree.is(Tree.Kind.METHOD);
    if (result) {
      MethodTree methodTree = (MethodTree) memberTree;
      ModifiersTree modifiers = methodTree.modifiers();
      result = !ModifiersUtils.hasModifier(modifiers, Modifier.STATIC) && !ModifiersUtils.hasModifier(modifiers, Modifier.DEFAULT);
    }
    return result;
  }
}
