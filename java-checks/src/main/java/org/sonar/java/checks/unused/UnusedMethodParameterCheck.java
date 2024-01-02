/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.Javadoc;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.helpers.UnresolvedIdentifiersVisitor;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.collections.SetUtils;

import static org.sonar.java.checks.helpers.AnnotationsHelper.hasUnknownAnnotation;

@Rule(key = "S1172")
public class UnusedMethodParameterCheck extends IssuableSubscriptionVisitor {
  private static final String PRIMARY_SINGULAR_MESSAGE_FORMAT = "Remove this unused method parameter %s.";
  private static final String PRIMARY_PLURAL_MESSAGE_FORMAT = "Remove these unused method parameters %s.";
  private static final String SECONDARY_MESSAGE_FORMAT = "Parameter \"%s\"";
  private static final String QUICK_FIX_MESSAGE = "Remove \"%s\"";

  private static final Set<String> AUTHORIZED_ANNOTATIONS = Set.of("javax.enterprise.event.Observes", "jakarta.enterprise.event.Observes");
  private static final String SUPPRESS_WARNINGS_ANNOTATION = "java.lang.SuppressWarnings";
  private static final Set<String> EXCLUDED_WARNINGS_SUPPRESSIONS = SetUtils.immutableSetOf("\"rawtypes\"", "\"unchecked\"");
  private static final MethodMatchers SERIALIZABLE_METHODS = MethodMatchers.or(
    MethodMatchers.create().ofAnyType().names("writeObject").addParametersMatcher("java.io.ObjectOutputStream").build(),
    MethodMatchers.create().ofAnyType().names("readObject").addParametersMatcher("java.io.ObjectInputStream").build());
  private static final String STRUTS_ACTION_SUPERCLASS = "org.apache.struts.action.Action";
  private static final Set<String> EXCLUDED_STRUTS_ACTION_PARAMETER_TYPES = Set.of(
    "org.apache.struts.action.ActionMapping",
    "org.apache.struts.action.ActionForm",
    "javax.servlet.http.HttpServletRequest",
    "javax.servlet.http.HttpServletResponse",
    "jakarta.servlet.http.HttpServletRequest",
    "jakarta.servlet.http.HttpServletResponse");

  private static final UnresolvedIdentifiersVisitor UNRESOLVED_IDENTIFIERS_VISITOR = new UnresolvedIdentifiersVisitor();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (methodTree.block() == null || methodTree.parameters().isEmpty() || isExcluded(methodTree)) {
      return;
    }
    Set<String> undocumentedParameters = new HashSet<>(new Javadoc(methodTree).undocumentedParameters());
    boolean overridableMethod = methodTree.symbol().isOverridable();
    List<IdentifierTree> unused = new ArrayList<>();
    for (VariableTree parameter : methodTree.parameters()) {
      Symbol symbol = parameter.symbol();
      if (symbol.usages().isEmpty()
        && !isAnnotatedWithAuthorizedAnnotation(symbol)
        && !isStrutsActionParameter(parameter)
        && (!overridableMethod || undocumentedParameters.contains(symbol.name()))) {
        unused.add(parameter.simpleName());
      }
    }
    Set<String> unresolvedIdentifierNames = UNRESOLVED_IDENTIFIERS_VISITOR.check(methodTree.block());
    // kill the noise regarding unresolved identifiers, and remove the one with matching names from the list of unused
    unused = unused.stream()
      .filter(id -> !unresolvedIdentifierNames.contains(id.name()))
      .collect(Collectors.toList());
    if (!unused.isEmpty()) {
      reportUnusedParameters(methodTree, unused);
    }
  }

  private static boolean isAnnotatedWithAuthorizedAnnotation(Symbol symbol) {
    SymbolMetadata metadata = symbol.metadata();
    return AUTHORIZED_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith) || hasUnknownAnnotation(metadata);
  }

  private void reportUnusedParameters(MethodTree methodTree, List<IdentifierTree> unused) {
    IdentifierTree firstUnused = unused.get(0);
    List<JavaFileScannerContext.Location> secondaryLocations = unused.stream()
      .skip(1)
      .map(identifier -> new JavaFileScannerContext.Location(String.format(SECONDARY_MESSAGE_FORMAT, identifier.name()), identifier))
      .collect(Collectors.toList());
    String parameterNames = unused.stream().map(identifier -> "\"" + identifier.name() + "\"").collect(Collectors.joining(", "));
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(firstUnused)
      .withMessage(unused.size() > 1 ? PRIMARY_PLURAL_MESSAGE_FORMAT : PRIMARY_SINGULAR_MESSAGE_FORMAT, parameterNames)
      .withSecondaries(secondaryLocations)
      .withQuickFixes(() -> createQuickFixes(methodTree, unused))
      .report();
  }

  private static List<JavaQuickFix> createQuickFixes(MethodTree methodTree, List<IdentifierTree> unused) {
    List<JavaQuickFix> quickFixes = new ArrayList<>();
    List<VariableTree> parameters = methodTree.parameters();
    for (int i = 0; i < parameters.size(); i++) {
      VariableTree parameter = parameters.get(i);
      if (unused.contains(parameter.simpleName())) {
        boolean isFirst = i == 0;
        boolean isLast = i == parameters.size() - 1;
        AnalyzerMessage.TextSpan textSpanToRemove;
        if (isFirst && isLast) {
          // no need to remove any comma
          textSpanToRemove = AnalyzerMessage.textSpanBetween(methodTree.openParenToken(), false, methodTree.closeParenToken(), false);
        } else if (isLast) {
          // also remove the previous comma and spaces between previousParameter and parameter
          VariableTree previousParameter = parameters.get(i - 1);
          textSpanToRemove = AnalyzerMessage.textSpanBetween(previousParameter.endToken(), true, methodTree.closeParenToken(), false);
        } else {
          // also remove the next comma and spaces between parameter and nextParameter
          VariableTree nextParameter = parameters.get(i + 1);
          textSpanToRemove = AnalyzerMessage.textSpanBetween(parameter, true, nextParameter, false);
        }
        quickFixes.add(JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE, parameter.simpleName().name())
          .addTextEdit(JavaTextEdit.removeTextSpan(textSpanToRemove))
          .build());
      }
    }
    return quickFixes;
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
    }
    if (tree.is(Tree.Kind.STRING_LITERAL)) {
      return EXCLUDED_WARNINGS_SUPPRESSIONS.contains(((LiteralTree) tree).value());
    }
    if (tree.is(Tree.Kind.NEW_ARRAY)) {
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
    return ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.PRIVATE) && SERIALIZABLE_METHODS.matches(methodTree);
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

}
