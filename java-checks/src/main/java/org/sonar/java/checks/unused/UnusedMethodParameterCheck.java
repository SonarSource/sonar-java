/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks.unused;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.Javadoc;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1172")
public class UnusedMethodParameterCheck extends IssuableSubscriptionVisitor {

  private static final String AUTHORIZED_ANNOTATION = "javax.enterprise.event.Observes";
  private static final String SUPPRESS_WARNINGS_ANNOTATION = "java.lang.SuppressWarnings";
  private static final Collection<String> EXCLUDED_WARNINGS_SUPPRESSIONS = ImmutableList.of("\"rawtypes\"", "\"unchecked\"");
  private static final MethodMatcherCollection SERIALIZABLE_METHODS = MethodMatcherCollection.create(
    MethodMatcher.create().name("writeObject").addParameter("java.io.ObjectOutputStream"),
    MethodMatcher.create().name("readObject").addParameter("java.io.ObjectInputStream"));
  private static final String STRUTS_ACTION_SUPERCLASS = "org.apache.struts.action.Action";
  private static final Collection<String> EXCLUDED_STRUTS_ACTION_PARAMETER_TYPES = ImmutableList.of("org.apache.struts.action.ActionMapping",
    "org.apache.struts.action.ActionForm", "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    if (methodTree.block() == null || methodTree.parameters().isEmpty() || isExcluded(methodTree)) {
      return;
    }
    List<String> undocumentedParameters = new Javadoc(methodTree).undocumentedParameters();
    boolean overridableMethod = ((JavaSymbol.MethodJavaSymbol) methodTree.symbol()).isOverridable();
    List<IdentifierTree> unused = new ArrayList<>();
    for (VariableTree var : methodTree.parameters()) {
      Symbol symbol = var.symbol();
      if (symbol.usages().isEmpty()
        && !symbol.metadata().isAnnotatedWith(AUTHORIZED_ANNOTATION)
        && !isStrutsActionParameter(var)
        && (!overridableMethod || undocumentedParameters.contains(symbol.name()))) {
        unused.add(var.simpleName());
      }
    }
    Set<String> unresolvedIdentifierNames = unresolvedIdentifierNames(methodTree.block());
    // kill the noise regarding unresolved identifiers, and remove the one with matching names from the list of unused
    unused = unused.stream()
      .filter(id -> !unresolvedIdentifierNames.contains(id.name()))
      .collect(Collectors.toList());
    if (!unused.isEmpty()) {
      reportUnusedParameters(unused);
    }
  }

  private void reportUnusedParameters(List<IdentifierTree> unused) {
    List<JavaFileScannerContext.Location> locations = new ArrayList<>();
    for (IdentifierTree identifier : unused) {
      locations.add(new JavaFileScannerContext.Location("Remove this unused method parameter " + identifier.name() + "\".", identifier));
    }
    IdentifierTree firstUnused = unused.get(0);
    String msg;
    if (unused.size() > 1) {
      msg = "Remove these unused method parameters.";
    } else {
      msg = "Remove this unused method parameter \"" + firstUnused.name() + "\".";
    }
    reportIssue(firstUnused, msg, locations, null);
  }

  private static boolean isExcluded(MethodTree tree) {
    return MethodTreeUtils.isMainMethod(tree)
      || isAnnotated(tree)
      || isOverriding(tree)
      || isSerializableMethod(tree)
      || isDesignedForExtension(tree)
      || isUsedAsMethodReference(tree);
  }

  private static boolean isAnnotated(MethodTree tree) {
    // If any annotation doesn't match the @SuppressWarning then mark the method as annotated.
    return tree.modifiers().annotations().stream().anyMatch(annotation -> !isExcludedLiteral(annotation));
  }

  private static boolean isExcludedLiteral(Tree tree) {
    if (tree.is(Tree.Kind.ANNOTATION)) {
      AnnotationTree annotationTree = (AnnotationTree) tree;
      return annotationTree.annotationType().symbolType().is(SUPPRESS_WARNINGS_ANNOTATION)
        && annotationTree.arguments().stream().allMatch(UnusedMethodParameterCheck::isExcludedLiteral);
    } else if (tree.is(Tree.Kind.STRING_LITERAL)) {
      return EXCLUDED_WARNINGS_SUPPRESSIONS.contains(((LiteralTree) tree).value());
    } else if (tree.is(Tree.Kind.NEW_ARRAY)) {
      return ((NewArrayTree) tree).initializers().stream().allMatch(UnusedMethodParameterCheck::isExcludedLiteral);
    }

    // If it is some type we don't expect, then return false to avoid FP.
    return false;
  }

  private static boolean isDesignedForExtension(MethodTree tree) {
    if (tree.symbol().enclosingClass().isFinal()) {
      // methods of final class can not be overridden, because the class can not be extended
      return false;
    }
    ModifiersTree modifiers = tree.modifiers();
    return ModifiersUtils.hasModifier(modifiers, Modifier.DEFAULT)
      || (!ModifiersUtils.hasModifier(modifiers, Modifier.PRIVATE) && isEmptyOrThrowStatement(tree.block()));
  }

  private static boolean isStrutsActionParameter(VariableTree variableTree) {
    Type superClass = variableTree.symbol().enclosingClass().superClass();
    return superClass != null && superClass.isSubtypeOf(STRUTS_ACTION_SUPERCLASS)
      && EXCLUDED_STRUTS_ACTION_PARAMETER_TYPES.contains(variableTree.symbol().type().fullyQualifiedName());
  }

  private static boolean isEmptyOrThrowStatement(BlockTree block) {
    return block.body().isEmpty() || (block.body().size() == 1 && block.body().get(0).is(Tree.Kind.THROW_STATEMENT));
  }

  private static boolean isSerializableMethod(MethodTree methodTree) {
    return ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.PRIVATE) && SERIALIZABLE_METHODS.anyMatch(methodTree);
  }

  private static boolean isOverriding(MethodTree tree) {
    // if overriding cannot be determined, we consider it is overriding to avoid FP.
    return !Boolean.FALSE.equals(tree.isOverriding());
  }

  private static boolean isUsedAsMethodReference(MethodTree tree) {
    return tree.symbol().usages().stream()
      // no need to check which side of method reference, from an identifierTree, it's the only possibility as direct parent
      .anyMatch(identifier -> identifier.parent().is(Tree.Kind.METHOD_REFERENCE));
  }

  private static Set<String> unresolvedIdentifierNames(Tree tree) {
    UnresolvedIdentifierVisitor visitor = new UnresolvedIdentifierVisitor();
    tree.accept(visitor);
    return visitor.unresolvedIdentifierNames;
  }

  private static class UnresolvedIdentifierVisitor extends BaseTreeVisitor {

    private Set<String> unresolvedIdentifierNames = new HashSet<>();

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      // skip annotations and identifier, a method parameter will only be used in expression side (before the dot)
      scan(tree.expression());
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      ExpressionTree methodSelect = tree.methodSelect();
      if (!methodSelect.is(Tree.Kind.IDENTIFIER)) {
        // not interested in simple method invocations, we are targeting usage of method parameters
        scan(methodSelect);
      }
      scan(tree.typeArguments());
      scan(tree.arguments());
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (tree.symbol().isUnknown()) {
        unresolvedIdentifierNames.add(tree.name());
      }
      super.visitIdentifier(tree);
    }
  }
}
