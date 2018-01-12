/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.RspecKey;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.PackageUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "UndocumentedApi")
@RspecKey("S1176")
public class UndocumentedApiCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Kind[] CLASS_KINDS = PublicApiChecker.classKinds();
  private static final Kind[] METHOD_KINDS = PublicApiChecker.methodKinds();

  private static final String DEFAULT_FOR_CLASSES = "**.api.**";
  private static final String DEFAULT_EXCLUSION = "**.internal.**";

  @RuleProperty(
    key = "forClasses",
    description = "Pattern of classes which should adhere to this constraint. Ex : **.api.**",
    defaultValue = DEFAULT_FOR_CLASSES)
  public String forClasses = DEFAULT_FOR_CLASSES;

  @RuleProperty(
    key = "exclusion",
    description = "Pattern of classes which are excluded from adhering to this constraint.",
    defaultValue = DEFAULT_EXCLUSION)
  public String exclusion = DEFAULT_EXCLUSION;

  private WildcardPattern[] inclusionPatterns;
  private WildcardPattern[] exclusionPatterns;

  private final Deque<ClassTree> classTrees = new LinkedList<>();
  private final Deque<Tree> currentParents = new LinkedList<>();

  private String packageName;
  private final Pattern setterPattern = Pattern.compile("set[A-Z].*");
  private final Pattern getterPattern = Pattern.compile("(get|is)[A-Z].*");
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (context.getSemanticModel() == null) {
      return;
    }
    classTrees.clear();
    currentParents.clear();
    packageName = "";
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    packageName = PackageUtils.packageName(tree.packageDeclaration(), ".");
    super.visitCompilationUnit(tree);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    // don't visit anonymous classes, nothing in an anonymous class is part of public api.
  }

  @Override
  public void visitClass(ClassTree tree) {
    //No anonymous class, no visit of new class trees.
    visitNode(tree, tree.simpleName(), tree.symbol().metadata());
    super.visitClass(tree);
    classTrees.pop();
    currentParents.pop();
  }

  @Override
  public void visitVariable(VariableTree tree) {
    visitNode(tree, tree.simpleName(), tree.symbol().metadata());
    super.visitVariable(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    visitNode(tree, tree.simpleName(), tree.symbol().metadata());
    super.visitMethod(tree);
    currentParents.pop();
  }

  private void visitNode(Tree tree, Tree reportTree, SymbolMetadata symbolMetadata) {
    if (!isExcluded(tree, symbolMetadata)) {
      Javadoc javadoc = new Javadoc(tree);
      if (javadoc.noMainDescription() && !isNonVoidMethodWithNoParameter(tree, javadoc)) {
        context.reportIssue(this, reportTree, "Document this public " + getType(tree) + " by adding an explicit description.");
      } else {
        List<String> undocumentedParameters = javadoc.undocumentedParameters(tree);
        if (!undocumentedParameters.isEmpty()) {
          context.reportIssue(this, reportTree, "Document the parameter(s): " + undocumentedParameters.stream().collect(Collectors.joining(", ")));
        }
        if (hasNonVoidReturnType(tree) && javadoc.noReturnDescription()) {
          context.reportIssue(this, reportTree, "Document this method return value.");
        }
        List<String> undocumentedExceptions = javadoc.undocumentedThrownExceptions(tree);
        if (!undocumentedExceptions.isEmpty()) {
          context.reportIssue(this, reportTree, "Document this method thrown exception(s): " + undocumentedExceptions.stream().collect(Collectors.joining(", ")));
        }
      }
    }
  }

  private boolean isNonVoidMethodWithNoParameter(Tree tree, Javadoc javadoc) {
    if (!tree.is(Tree.Kind.METHOD)) {
      return false;
    }

    return hasNonVoidReturnType(tree)
      && ((MethodTree) tree).parameters().isEmpty()
      // if return description is there, then it will be validated later
      && !javadoc.noReturnDescription();
  }

  private static String getType(Tree tree) {
    switch (tree.kind()) {
      case CONSTRUCTOR:
        return "constructor";
      case METHOD:
        return "method";
      case VARIABLE:
        return "field";
      case CLASS:
        return "class";
      case INTERFACE:
        return "interface";
      case ENUM:
        return "enum";
      case ANNOTATION_TYPE:
        return "annotation";
      default:
        return "";
    }
  }

  private boolean isExcluded(Tree tree, SymbolMetadata symbolMetadata) {
    return !isPublicApi(tree)
      || isAccessor(tree)
      || !isMatchingInclusionPattern()
      || isMatchingExclusionPattern()
      || isOverridingMethod(tree)
      || isVisibleForTestingMethod(tree, symbolMetadata)
      || hasDeprecatedAnnotation(symbolMetadata);
  }

  private boolean isAccessor(Tree tree) {
    if (!classTrees.isEmpty() && tree.is(Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) tree;
      String name = methodTree.simpleName().name();
      return (setterPattern.matcher(name).matches() && methodTree.parameters().size() == 1) ||
        (getterPattern.matcher(name).matches() && methodTree.parameters().isEmpty());
    }
    return false;
  }

  private boolean isPublicApi(Tree tree) {
    Tree currentParent = currentParents.peek();
    if (tree.is(CLASS_KINDS)) {
      classTrees.push((ClassTree) tree);
      currentParents.push(tree);
    } else if (tree.is(METHOD_KINDS)) {
      currentParents.push(tree);
    }

    return PublicApiChecker.isPublicApi(currentParent, tree);
  }

  private boolean isMatchingInclusionPattern() {
    return WildcardPattern.match(getInclusionPatterns(), className());
  }

  private boolean isMatchingExclusionPattern() {
    return WildcardPattern.match(getExclusionPatterns(), className());
  }

  private static boolean isOverridingMethod(Tree tree) {
    return tree.is(Tree.Kind.METHOD) && Boolean.TRUE.equals(((MethodTree) tree).isOverriding());
  }

  private static boolean isVisibleForTestingMethod(Tree tree, SymbolMetadata symbolMetadata) {
    return tree.is(Tree.Kind.METHOD)
      && (symbolMetadata.isAnnotatedWith("org.fest.util.VisibleForTesting") || symbolMetadata.isAnnotatedWith("com.google.common.annotations.VisibleForTesting"));
  }

  private static boolean hasDeprecatedAnnotation(SymbolMetadata symbolMetadata) {
    return symbolMetadata.isAnnotatedWith("java.lang.Deprecated");
  }

  private String className() {
    String className = packageName;
    IdentifierTree identifierTree = classTrees.peek().simpleName();
    if (identifierTree != null) {
      className += "." + identifierTree.name();
    }
    return className;
  }

  private WildcardPattern[] getInclusionPatterns() {
    if (inclusionPatterns == null) {
      if (StringUtils.isEmpty(forClasses)) {
        forClasses = "**";
      }

      inclusionPatterns = PatternUtils.createPatterns(forClasses);
    }
    return inclusionPatterns;
  }

  private WildcardPattern[] getExclusionPatterns() {
    if (exclusionPatterns == null) {
      if (StringUtils.isEmpty(exclusion)) {
        exclusionPatterns = new WildcardPattern[0];
      } else {
        exclusionPatterns = PatternUtils.createPatterns(exclusion);
      }
    }
    return exclusionPatterns;
  }

  private boolean hasNonVoidReturnType(Tree tree) {
    // Backward compatibility : ignore methods from annotations.
    if (tree.is(Tree.Kind.METHOD) && !classTrees.peek().is(Tree.Kind.ANNOTATION_TYPE)) {
      Tree returnType = ((MethodTree) tree).returnType();
      return returnType == null || !(returnType.is(Tree.Kind.PRIMITIVE_TYPE) && "void".equals(((PrimitiveTypeTree) returnType).keyword().text()));
    }
    return false;
  }

  private static class Javadoc {
    private static final Pattern PARAMETER_JAVADOC_PATTERN = Pattern.compile(".*@param\\s++(?<name>\\S*)(\\s++)?(?<descr>.+)?");
    private static final Pattern EXCEPTION_JAVADOC_PATTERN = Pattern.compile(".*@throws\\s++(?<name>\\S*)(\\s++)?(?<descr>.+)?");
    private static final Pattern RETURN_JAVADOC_PATTERN = Pattern.compile(".*@return(\\s++)?(?<descr>.+)?");
    private static final Set<String> PLACEHOLDERS = ImmutableSet.of("TODO", "FIXME", "...", ".");

    private final String mainDescription;
    private final Map<String, List<String>> parameters;
    private final Map<String, List<String>> thrownExceptions;
    private final String returnDescription;

    Javadoc(Tree tree) {
      String javadoc = PublicApiChecker.getApiJavadoc(tree);
      List<String> lines = cleanedlines(javadoc);

      mainDescription = extractMainDescription(lines);
      parameters = extractToMap(lines, PARAMETER_JAVADOC_PATTERN);
      thrownExceptions = extractToMap(lines, EXCEPTION_JAVADOC_PATTERN);
      returnDescription = extractReturnDescription(lines);
    }

    public boolean noMainDescription() {
      return isEmptyDescription(mainDescription);
    }

    public boolean noReturnDescription() {
      return isEmptyDescription(returnDescription);
    }

    public List<String> undocumentedParameters(Tree tree) {
      return getParameters(tree).stream()
        .filter(name -> isEmptyDescription(parameters.get(name)))
        .collect(Collectors.toList());
    }

    public List<String> undocumentedThrownExceptions(Tree tree) {
      List<String> exceptionNames = getExceptions(tree);
      if (exceptionNames.size() == 1 && "Exception".equals(exceptionNames.get(0)) && !thrownExceptions.isEmpty()) {
        // check for described exceptions when only "Exception" is used is declared as being thrown
        return thrownExceptions.entrySet().stream()
          .filter(e -> isEmptyDescription(e.getValue()))
          .map(Map.Entry::getKey)
          .map(Javadoc::toSimpleName)
          .collect(Collectors.toList());
      }
      return exceptionNames.stream()
        .filter(this::noDescriptionForException)
        .map(Javadoc::toSimpleName)
        .collect(Collectors.toList());
    }

    private boolean noDescriptionForException(String exceptionName) {
      // try getting the exception described with exact match
      List<String> descriptions = thrownExceptions.get(exceptionName);
      if (descriptions == null) {
        // exceptions used in javadoc is using simple name when method declaration use fully qualified name
        descriptions = thrownExceptions.get(toSimpleName(exceptionName));
      }
      if (descriptions == null) {
        // exceptions used in javadoc is using fully qualified name when method declaration use simple name
        descriptions = thrownExceptions.entrySet().stream()
          .filter(e -> toSimpleName(e.getKey()).equals(exceptionName))
          .map(Map.Entry::getValue)
          .flatMap(List::stream)
          .collect(Collectors.toList());
      }
      return isEmptyDescription(descriptions);
    }

    private static String toSimpleName(String exceptionName) {
      int lastDot = exceptionName.lastIndexOf('.');
      if (lastDot != -1) {
        return exceptionName.substring(lastDot + 1);
      }
      return exceptionName;
    }

    private static boolean isEmptyDescription(@Nullable List<String> descriptions) {
      return descriptions == null || descriptions.isEmpty() || descriptions.stream().anyMatch(Javadoc::isEmptyDescription);
    }

    private static boolean isEmptyDescription(@Nullable String part) {
      return part == null
        || part.trim().isEmpty()
        || PLACEHOLDERS.contains(part);
    }

    private static List<String> getParameters(Tree tree) {
      if (tree.is(METHOD_KINDS)) {
        return ((MethodTree) tree).parameters().stream()
          .map(VariableTree::simpleName)
          .map(IdentifierTree::name)
          .collect(Collectors.toList());
      }
      // don't check type parameters documentation for methods
      if (tree.is(CLASS_KINDS)) {
        return ((ClassTree) tree).typeParameters().stream()
          .map(TypeParameterTree::identifier)
          .map(IdentifierTree::name)
          .map(name -> "<" + name + ">")
          .collect(Collectors.toList());
      }
      return Collections.emptyList();
    }

    private static List<String> getExceptions(Tree tree) {
      if (tree.is(METHOD_KINDS)) {
        return ((MethodTree) tree).throwsClauses().stream()
          .map(Javadoc::exceptionName)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
      }
      return Collections.emptyList();
    }

    private static String exceptionName(TypeTree typeTree) {
      switch (typeTree.kind()) {
        case IDENTIFIER:
          return ((IdentifierTree) typeTree).name();
        case MEMBER_SELECT:
          return ExpressionsHelper.concatenate((MemberSelectExpressionTree) typeTree);
        default:
          // Should never happen - Throwable can not be extended by a parameterized type
          throw new IllegalStateException("Exceptions can not be specified other than with an identifier or a fully qualified name.");
      }
    }

    private static List<String> cleanedlines(@Nullable String javadoc) {
      if (javadoc == null) {
        return Collections.emptyList();
      }
      String[] lines = cleanupJavadoc(javadoc).split("\\r?\\n");
      return Arrays.stream(lines).map(String::trim).collect(Collectors.toList());
    }

    private static String cleanupJavadoc(String javadoc) {
      // remove start and end of Javadoc as well as stars.
      return javadoc.trim().substring(3).replace("*/", "").replace("*", "").trim();
    }

    private static String extractMainDescription(List<String> lines) {
      StringBuilder sb = new StringBuilder();
      for (String line : lines) {
        if (line.matches("(@param|@throws|@return).*")) {
          break;
        }
        sb.append(line).append(" ");
      }
      return sb.toString().trim();
    }

    private static Map<String, List<String>> extractToMap(List<String> lines, Pattern pattern) {
      Map<String, List<String>> results = new HashMap<>();
      for (int i = 0; i < lines.size(); i++) {
        Matcher matcher = pattern.matcher(lines.get(i));
        if (matcher.matches()) {
          List<String> descriptions = results.computeIfAbsent(matcher.group("name"), key -> new ArrayList<>());
          String newDescription = getNextLineIfNeeded(lines, i, matcher.group("descr"));
          if (newDescription != null) {
            descriptions.add(newDescription);
          }
        }
      }
      return results;
    }

    private static String extractReturnDescription(List<String> lines) {
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i);
        Matcher matcher = RETURN_JAVADOC_PATTERN.matcher(line);
        if (matcher.matches()) {
          String returnDescription = getNextLineIfNeeded(lines, i, matcher.group("descr"));
          if (returnDescription != null) {
            return returnDescription;
          }
        }
      }
      return "";
    }

    private static String getNextLineIfNeeded(List<String> lines, int currentIndex, @Nullable String currrentValue) {
      if (currrentValue == null && currentIndex < lines.size() - 1) {
        String nextLine = lines.get(currentIndex + 1);
        // not an element declaration
        if (!nextLine.startsWith("@")) {
          // assume the description is on the next line
          return nextLine;
        }
      }
      return currrentValue;
    }

  }

}
