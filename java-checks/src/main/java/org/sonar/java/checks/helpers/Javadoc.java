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
package org.sonar.java.checks.helpers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
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

public final class Javadoc {
  private enum BlockTag {
    RETURN(Pattern.compile("^@return(\\s++)?(?<descr>.+)?"), false),
    PARAM(Pattern.compile("^@param\\s++(?<name>\\S*)(\\s++)?(?<descr>.+)?"), true),
    EXCEPTIONS(Pattern.compile("^(?:@throws|@exception)\\s++(?<name>\\S*)(\\s++)?(?<descr>.+)?"), true);

    private final Pattern pattern;
    private final boolean patternWithName;

    BlockTag(Pattern pattern, boolean patternWithName) {
      this.pattern = pattern;
      this.patternWithName = patternWithName;
    }

    private Pattern getPattern() {
      return pattern;
    }

    private boolean isPatternWithName() {
      return patternWithName;
    }
  }

  private static class BlockTagKey {
    private final BlockTag tag;
    private final String name;

    BlockTagKey(BlockTag tag, @Nullable String name) {
      this.tag = tag;
      this.name = name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      } else if (o instanceof BlockTagKey) {
        BlockTagKey other = ((BlockTagKey) o);
        return tag == other.tag && Objects.equals(name, other.name);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(tag, name);
    }

    private static BlockTagKey of(BlockTag tag, @Nullable String name) {
      return new BlockTagKey(tag, name);
    }
  }

  private static final Tree.Kind[] CLASS_KINDS = PublicApiChecker.classKinds();
  private static final Tree.Kind[] METHOD_KINDS = PublicApiChecker.methodKinds();
  private static final List<String> GENERIC_EXCEPTIONS = Arrays.asList("Exception", "java.lang.Exception");
  private static final Pattern BLOCK_TAG_LINE_PATTERN = Pattern.compile("^@\\S+.*");
  private static final Set<String> PLACEHOLDERS = ImmutableSet.of("TODO", "FIXME", "...", ".");

  private final List<String> elementParameters;
  private final List<String> elementExceptionNames;
  private final String mainDescription;
  private final Map<BlockTagKey, List<String>> blockTagDescriptions;
  private final EnumMap<BlockTag, List<String>> undocumentedNamedTags;

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

    List<String> javadocLines = cleanLines(PublicApiChecker.getApiJavadoc(tree));
    mainDescription = getDescription(javadocLines, -1, "");
    blockTagDescriptions = extractBlockTags(javadocLines, Arrays.asList(BlockTag.values()));
    undocumentedNamedTags = new EnumMap<>(BlockTag.class);
  }

  public boolean noMainDescription() {
    return isEmptyDescription(mainDescription);
  }

  public boolean noReturnDescription() {
    return isEmptyDescription(blockTagDescriptions.get(BlockTagKey.of(BlockTag.RETURN, null)));
  }

  public List<String> undocumentedParameters() {
    return undocumentedNamedTags.computeIfAbsent(BlockTag.PARAM, key -> computeUndocumentedParameters());
  }

  public List<String> undocumentedThrownExceptions() {
    return undocumentedNamedTags.computeIfAbsent(BlockTag.EXCEPTIONS, key -> computeUndocumentedThrownExceptions());
  }

  private List<String> computeUndocumentedParameters() {
    return elementParameters.stream()
      .filter(name -> isEmptyDescription(blockTagDescriptions.get(BlockTagKey.of(BlockTag.PARAM, name))))
      .collect(Collectors.toList());
  }

  private List<String> computeUndocumentedThrownExceptions() {
    Map<String, List<String>> thrownExceptionsMap = blockTagDescriptions.entrySet().stream()
      .filter(entry -> entry.getKey().tag == BlockTag.EXCEPTIONS && entry.getKey().name != null)
      .collect(Collectors.toMap(entry -> entry.getKey().name, Map.Entry::getValue));
    List<String> exceptionNames = elementExceptionNames;
    if (exceptionNames.size() == 1 && GENERIC_EXCEPTIONS.contains(toSimpleName(exceptionNames.get(0))) && !thrownExceptionsMap.isEmpty()) {
      // check for documented exceptions without description when only "Exception" is declared as being thrown
      return thrownExceptionsMap.entrySet().stream()
        .filter(e -> isEmptyDescription(e.getValue()))
        .map(Map.Entry::getKey)
        .map(Javadoc::toSimpleName)
        .collect(Collectors.toList());
    }
    return exceptionNames.stream()
      .map(Javadoc::toSimpleName)
      .filter(simpleName -> noDescriptionForException(thrownExceptionsMap, simpleName))
      .collect(Collectors.toList());
  }

  private boolean noDescriptionForException(Map<String, List<String>> thrownExceptionsMap, String exceptionSimpleName) {
    List<String> descriptions = thrownExceptionsMap.get(exceptionSimpleName);
    if (descriptions == null) {
      // exceptions used in javadoc is using fully qualified name when method declaration use simple name
      descriptions = thrownExceptionsMap.entrySet().stream()
        .filter(e -> toSimpleName(e.getKey()).equals(exceptionSimpleName))
        .map(Map.Entry::getValue)
        .flatMap(List::stream)
        .collect(Collectors.toList());
    }
    return isEmptyDescription(descriptions);
  }

  private static Map<BlockTagKey, List<String>> extractBlockTags(List<String> javadocLines, List<BlockTag> tags) {
    Map<BlockTagKey, List<String>> results = new HashMap<>();
    for (int i = 0; i < javadocLines.size(); i++) {
      for (int j = 0; j < tags.size(); j++) {
        BlockTag tag = tags.get(j);
        Matcher matcher = tag.getPattern().matcher(javadocLines.get(i));
        if (matcher.matches()) {
          BlockTagKey key = BlockTagKey.of(tag, tag.isPatternWithName() ? matcher.group("name") : null);
          List<String> descriptions = results.computeIfAbsent(key, k -> new ArrayList<>());
          String newDescription = getDescription(javadocLines, i, matcher.group("descr"));
          if (!newDescription.isEmpty()) {
            descriptions.add(newDescription);
            break;
          }
        }
      }
    }
    return results;
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

  private static boolean isEmptyDescription(String part) {
    return part.trim().isEmpty() || PLACEHOLDERS.contains(part.trim());
  }

  @CheckForNull
  private static String exceptionName(TypeTree typeTree) {
    switch (typeTree.kind()) {
      case IDENTIFIER:
        return ((IdentifierTree) typeTree).name();
      case MEMBER_SELECT:
        return ExpressionsHelper.concatenate((MemberSelectExpressionTree) typeTree);
      default:
        // Exceptions can not be specified other than with an identifier or a fully qualified name.
        // in SonarLint context, however, you may end up with something which does not compile
        return null;
    }
  }

  private static List<String> cleanLines(@Nullable String javadoc) {
    if (javadoc == null) {
      return Collections.emptyList();
    }
    String trimmedJavadoc = javadoc.trim();
    if (trimmedJavadoc.length() <= 4) {
      // Empty or malformed javadoc. for instance: '/**/'
      return Collections.emptyList();
    }
    // remove start and end of Javadoc as well as stars
    String[] lines = trimmedJavadoc
      .substring(3, trimmedJavadoc.length() - 2)
      .replaceAll("(?m)^\\s*\\*", "")
      .trim()
      .split("\\r?\\n");
    return Arrays.stream(lines).map(String::trim).collect(Collectors.toList());
  }

  private static String getDescription(List<String> lines, int lineIndex, @Nullable String currentValue) {
    StringBuilder sb = new StringBuilder();
    sb.append(currentValue != null ? currentValue : "");
    int currentIndex = lineIndex;
    while (currentIndex + 1 < lines.size() && !BLOCK_TAG_LINE_PATTERN.matcher(lines.get(currentIndex + 1)).matches()) {
      sb.append(" ");
      sb.append(lines.get(currentIndex + 1));
      currentIndex++;
    }
    return sb.toString().trim();
  }

  @VisibleForTesting
  String getMainDescription() {
    return mainDescription;
  }

  @VisibleForTesting
  Map<BlockTagKey, List<String>> getBlockTagDescriptions() {
    return blockTagDescriptions;
  }
}
