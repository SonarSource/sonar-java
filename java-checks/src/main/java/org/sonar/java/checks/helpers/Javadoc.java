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
package org.sonar.java.checks.helpers;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class Javadoc {
  private static final Tree.Kind[] CLASS_KINDS = PublicApiChecker.classKinds();
  private static final Tree.Kind[] METHOD_KINDS = PublicApiChecker.methodKinds();
  private static final List<String> GENERIC_EXCEPTIONS = Arrays.asList("Exception", "java.lang.Exception");
  private static final Pattern PARAMETER_JAVADOC_PATTERN = Pattern.compile("^@param\\s++(?<name>\\S*)(\\s++)?(?<descr>.+)?");
  private static final Pattern EXCEPTION_JAVADOC_PATTERN = Pattern.compile("^(?:@throws|@exception)\\s++(?<name>\\S*)(\\s++)?(?<descr>.+)?");
  private static final Pattern RETURN_JAVADOC_PATTERN = Pattern.compile("^@return(\\s++)?(?<descr>.+)?");
  private static final Pattern BLOCK_TAG_PATTERN = Pattern.compile("^@\\S+.*");
  private static final Set<String> PLACEHOLDERS = ImmutableSet.of("TODO", "FIXME", "...", ".");

  private final List<String> elementParameters;
  private final List<String> elementExceptionNames;
  private final List<String> javadocLines;
  private String mainDescription;
  private String returnDescription;
  private Map<String, List<String>> javadocParameters;
  private Map<String, List<String>> javadocExceptions;

  public Javadoc(Tree tree) {
    if (tree.is(METHOD_KINDS)) {
      elementParameters = ((MethodTree) tree).parameters().stream()
        .map(VariableTree::simpleName)
        .map(IdentifierTree::name)
        .collect(Collectors.toList());
      elementExceptionNames = ((MethodTree) tree).throwsClauses().stream()
        .map(Javadoc::exceptionName)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    } else if (tree.is(CLASS_KINDS)) {
      elementParameters = ((ClassTree) tree).typeParameters().stream()
        .map(TypeParameterTree::identifier)
        .map(IdentifierTree::name)
        .map(name -> "<" + name + ">")
        .collect(Collectors.toList());
      elementExceptionNames = Collections.emptyList();
    } else {
      elementParameters = Collections.emptyList();
      elementExceptionNames = Collections.emptyList();
    }

    javadocLines = cleanLines(PublicApiChecker.getApiJavadoc(tree));
  }

  public boolean noMainDescription() {
    return isEmptyDescription(getMainDescription());
  }

  public boolean noReturnDescription() {
    return isEmptyDescription(getReturnDescription());
  }

  public Set<String> undocumentedParameters() {
    Map<String, List<String>> javadocParametersMap = getJavadocParameters();
    return elementParameters.stream()
      .filter(name -> isEmptyDescription(javadocParametersMap.get(name)))
      .collect(Collectors.toSet());
  }

  public Set<String> undocumentedThrownExceptions() {
    Map<String, List<String>> thrownExceptionsMap = getJavadocExceptions();
    List<String> exceptionNames = elementExceptionNames;
    if (exceptionNames.size() == 1 && GENERIC_EXCEPTIONS.contains(toSimpleName(exceptionNames.get(0))) && !thrownExceptionsMap.isEmpty()) {
      // check for documented exceptions without description when only "Exception" is declared as being thrown
      return thrownExceptionsMap.entrySet().stream()
        .filter(e -> isEmptyDescription(e.getValue()))
        .map(Map.Entry::getKey)
        .map(Javadoc::toSimpleName)
        .collect(Collectors.toSet());
    }
    return exceptionNames.stream()
      .filter(this::noDescriptionForException)
      .map(Javadoc::toSimpleName)
      .collect(Collectors.toSet());
  }

  private boolean noDescriptionForException(String exceptionName) {
    Map<String, List<String>> thrownExceptionsMap = getJavadocExceptions();
    List<String> descriptions = thrownExceptionsMap.get(exceptionName);
    if (descriptions == null) {
      // exceptions used in javadoc is using simple name when method declaration use fully qualified name
      descriptions = thrownExceptionsMap.get(toSimpleName(exceptionName));
    }
    if (descriptions == null) {
      // exceptions used in javadoc is using fully qualified name when method declaration use simple name
      descriptions = thrownExceptionsMap.entrySet().stream()
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
    return part == null || part.trim().isEmpty() || PLACEHOLDERS.contains(part.trim());
  }

  private static String exceptionName(TypeTree typeTree) {
    switch (typeTree.kind()) {
      case IDENTIFIER:
        return ((IdentifierTree) typeTree).name();
      case MEMBER_SELECT:
        return ExpressionsHelper.concatenate((MemberSelectExpressionTree) typeTree);
      default:
        throw new IllegalStateException("Exceptions can not be specified other than with an identifier or a fully qualified name.");
    }
  }

  private static List<String> cleanLines(@Nullable String javadoc) {
    if (javadoc == null) {
      return Collections.emptyList();
    }
    String trimmedJavadoc = javadoc.trim();
    // remove start and end of Javadoc as well as stars
    String[] lines = trimmedJavadoc
      .substring(3, trimmedJavadoc.length() - 2)
      .replaceAll("(?m)^\\s*\\*", "")
      .trim()
      .split("\\r?\\n");
    return Arrays.stream(lines).map(String::trim).collect(Collectors.toList());
  }

  private static Map<String, List<String>> extractToMap(List<String> lines, Pattern pattern) {
    Map<String, List<String>> results = new HashMap<>();
    for (int i = 0; i < lines.size(); i++) {
      Matcher matcher = pattern.matcher(lines.get(i));
      if (matcher.matches()) {
        List<String> descriptions = results.computeIfAbsent(matcher.group("name"), key -> new ArrayList<>());
        String newDescription = getFullDescription(lines, i, matcher.group("descr"));
        if (!newDescription.isEmpty()) {
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
        String returnDescription = getFullDescription(lines, i, matcher.group("descr"));
        if (!returnDescription.isEmpty()) {
          return returnDescription;
        }
      }
    }
    return "";
  }

  private static String getFullDescription(List<String> lines, int i, @Nullable String currentValue) {
    StringBuilder sb = new StringBuilder();
    sb.append(currentValue != null ? currentValue : "");
    int currentIndex = i;
    while (currentIndex + 1 < lines.size() && !BLOCK_TAG_PATTERN.matcher(lines.get(currentIndex + 1)).matches()) {
      sb.append(" ");
      sb.append(lines.get(currentIndex + 1));
      currentIndex++;
    }
    return sb.toString().trim();
  }

  private String getMainDescription() {
    if (mainDescription == null) {
      mainDescription = getFullDescription(javadocLines, -1, "");
    }
    return mainDescription;
  }

  private Map<String, List<String>> getJavadocParameters() {
    if (javadocParameters == null) {
      javadocParameters = extractToMap(javadocLines, PARAMETER_JAVADOC_PATTERN);
    }
    return javadocParameters;
  }

  private Map<String, List<String>> getJavadocExceptions() {
    if (javadocExceptions == null) {
      javadocExceptions = extractToMap(javadocLines, EXCEPTION_JAVADOC_PATTERN);
    }
    return javadocExceptions;
  }

  private String getReturnDescription() {
    if (returnDescription == null) {
      returnDescription = extractReturnDescription(javadocLines);
    }
    return returnDescription;
  }

  List<String> getJavadocLines() {
    return javadocLines;
  }
}
