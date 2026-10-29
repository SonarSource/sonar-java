/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.testing.JavaFileScannerContextForTests;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public final class FrontendContractSnapshot {

  public static final Gson JSON = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();

  private final List<Tree> trees = new ArrayList<>();
  private final Map<Tree, Integer> treeIds = new IdentityHashMap<>();
  private final Map<Symbol, Integer> symbolIds = new IdentityHashMap<>();
  private final Map<Type, Integer> typeIds = new IdentityHashMap<>();

  public static String json(Object value) {
    return JSON.toJson(canonical(value));
  }

  private static Object canonical(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> sorted = new TreeMap<>();
      map.forEach((key, entry) -> sorted.put((String) key, canonical(entry)));
      return sorted;
    }
    if (value instanceof List<?> list) {
      return list.stream().map(FrontendContractSnapshot::canonical).toList();
    }
    return value;
  }

  public Map<String, Object> capture(JavaTree.CompilationUnitTreeImpl root) {
    index(root);
    List<Map<String, Object>> nodes = new ArrayList<>();
    for (Tree tree : trees) {
      Map<String, Object> node = new LinkedHashMap<>();
      node.put("id", treeIds.get(tree));
      node.put("kind", tree.kind().name());
      node.put("class", tree.getClass().getName());
      node.put("parent", treeIds.get(tree.parent()));
      node.put("firstToken", treeIds.get(tree.firstToken()));
      node.put("lastToken", treeIds.get(tree.lastToken()));
      node.put("children", children(tree).stream().map(treeIds::get).toList());
      if (tree instanceof SyntaxToken token) {
        node.put("text", token.text());
        node.put("range", token.range().toString());
        node.put("trivia", token.trivias().stream().map(trivia -> List.of(trivia.commentKind().name(), trivia.comment(), trivia.commentContent(), trivia.range().toString())).toList());
      }
      if (tree instanceof ExpressionTree expression) {
        Type type = expression.symbolType();
        node.put("type", type(type));
        node.put("typeIdentity", typeIds.computeIfAbsent(type, ignored -> typeIds.size()));
      }
      Symbol symbol = symbol(tree);
      if (symbol != null) {
        node.put("symbol", symbol(symbol));
        node.put("symbolIdentity", symbolIds.computeIfAbsent(symbol, ignored -> symbolIds.size()));
        node.put("declaration", treeIds.get(symbol.declaration()));
        node.put("usages", symbol.usages().stream().map(treeIds::get).toList());
      }
      nodes.add(node);
    }
    List<Map<String, Object>> warnings = new ArrayList<>();
    for (JProblem.Type kind : JProblem.Type.values()) {
      root.warnings(kind).stream().sorted(Comparator.comparing(JWarning::start).thenComparing(JWarning::end).thenComparing(JWarning::message))
        .forEach(warning -> warnings.add(Map.of("type", kind.name(), "message", warning.message(), "start", warning.start().toString(),
          "end", warning.end().toString(), "tree", treeIds.get(warning.syntaxTree()))));
    }
    return Map.of("nodes", nodes, "warnings", warnings,
      "undefinedTypes", root.undefinedTypes().stream().map(problem -> List.of(problem.type().name(), problem.message(), problem.toString())).sorted(Comparator.comparing(Object::toString)).toList(),
      "equalSymbols", equalPairs(symbolIds), "equalTypes", equalPairs(typeIds));
  }

  private void index(Tree tree) {
    if (treeIds.containsKey(tree)) {
      return;
    }
    treeIds.put(tree, trees.size());
    trees.add(tree);
    children(tree).forEach(this::index);
  }

  private static List<Tree> children(Tree tree) {
    return tree.is(Tree.Kind.TOKEN, Tree.Kind.INFERED_TYPE) ? List.of() : ((JavaTree) tree).getChildren();
  }

  @Nullable
  private static Symbol symbol(Tree tree) {
    return switch (tree) {
      case ClassTree declaration -> declaration.symbol();
      case MethodTree declaration -> declaration.symbol();
      case VariableTree declaration -> declaration.symbol();
      case TypeParameterTree parameter -> parameter.symbol();
      case IdentifierTree identifier -> identifier.symbol();
      case MethodInvocationTree invocation -> invocation.methodSymbol();
      case LambdaExpressionTree lambda -> lambda.symbol();
      default -> null;
    };
  }

  private static Map<String, Object> type(Type type) {
    return Map.of("name", type.name(), "fullyQualifiedName", type.fullyQualifiedName(), "unknown", type.isUnknown(), "array", type.isArray());
  }

  private static Map<String, Object> symbol(Symbol symbol) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("name", symbol.name());
    result.put("unknown", symbol.isUnknown());
    result.put("type", type(symbol.type()));
    result.put("owner", symbol.owner() == null ? null : symbol.owner().name());
    if (symbol instanceof Symbol.MethodSymbol method) {
      result.put("signature", method.signature());
      result.put("parameters", method.parameterTypes().stream().map(FrontendContractSnapshot::type).toList());
      result.put("throws", method.thrownTypes().stream().map(FrontendContractSnapshot::type).toList());
    }
    result.put("annotations", symbol.metadata().annotations().stream().map(annotation -> annotation.symbol().type().fullyQualifiedName()).toList());
    result.put("nullability", symbol.metadata().nullabilityData().type().name());
    return result;
  }

  private static <T> List<List<Integer>> equalPairs(Map<T, Integer> identities) {
    List<T> values = identities.keySet().stream().sorted(Comparator.comparingInt(identities::get)).toList();
    List<List<Integer>> pairs = new ArrayList<>();
    for (int i = 0; i < values.size(); i++) {
      for (int j = 0; j < values.size(); j++) {
        if (values.get(i).equals(values.get(j))) {
          pairs.add(List.of(i, j));
        }
      }
    }
    return pairs;
  }

  public static List<Map<String, Object>> issues(JavaFileScannerContextForTests context) {
    return context.getIssues().stream().map(issue -> {
      Map<String, Object> result = message(issue);
      result.put("check", issue.getCheck().getClass().getName());
      result.put("flows", issue.flows.stream().map(flow -> flow.stream().map(FrontendContractSnapshot::message).toList()).toList());
      result.put("quickFixes", context.getQuickFixes().getOrDefault(issue.primaryLocation(), List.of()).stream().map(FrontendContractSnapshot::quickFix).toList());
      return result;
    }).toList();
  }

  private static Map<String, Object> message(AnalyzerMessage issue) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("message", issue.getMessage());
    result.put("cost", issue.getCost());
    result.put("location", issue.primaryLocation() == null ? null : issue.primaryLocation().toString());
    result.put("input", issue.getInputComponent() instanceof InputFile file ? file.relativePath() : issue.getInputComponent().key());
    return result;
  }

  private static Map<String, Object> quickFix(JavaQuickFix fix) {
    return Map.of("description", fix.getDescription(), "edits", fix.getTextEdits().stream()
      .map(edit -> Map.of("range", edit.getTextSpan().toString(), "replacement", edit.getReplacement())).toList());
  }
}
