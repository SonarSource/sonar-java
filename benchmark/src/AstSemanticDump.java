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
package org.sonar.java.benchmark;

import com.google.gson.JsonObject;
import com.sonarsource.scanner.engine.sensor.test.fixtures.TestInputFileBuilder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.model.JParserConfig;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * Writes one JSON object per line for every node of the SonarJava tree, plus the symbol and type
 * that node carries. The same source is compiled against the ECJ frontend and against the SemSitter
 * frontend, so the two dumps share this schema.
 */
public final class AstSemanticDump {

  private AstSemanticDump() {
  }

  public static void main(String[] args) throws Exception {
    Path out = null;
    Path root = null;
    String classpath = "";
    List<Path> files = new ArrayList<>();
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if ("--out".equals(arg)) {
        out = Path.of(args[++i]);
      } else if ("--root".equals(arg)) {
        root = Path.of(args[++i]).toAbsolutePath().normalize();
      } else if ("--classpath".equals(arg)) {
        classpath = args[++i];
      } else {
        files.add(Path.of(arg).toAbsolutePath().normalize());
      }
    }
    if (out == null || files.isEmpty()) {
      throw new IllegalArgumentException("usage: AstSemanticDump --out dump.jsonl [--root DIR] [--classpath a.jar:b.jar] FILE...");
    }
    files.sort(Comparator.comparing(Path::toString));
    List<File> entries = classpathEntries(classpath);
    List<InputFile> inputFiles = new ArrayList<>();
    Map<InputFile, String> names = new HashMap<>();
    for (Path file : files) {
      String relative = relative(root, file);
      String source = Files.readString(file);
      File base = root == null ? file.getParent().toFile() : root.toFile();
      InputFile input = new TestInputFileBuilder("bench", base, file.toFile())
        .setCharset(StandardCharsets.UTF_8)
        .setLanguage("java")
        .setContents(source)
        .build();
      inputFiles.add(input);
      names.put(input, relative);
    }
    JParserConfig config = JParserConfig.Mode.BATCH.create(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, entries);
    Map<String, Tree> trees = new HashMap<>();
    Map<String, String> errors = new HashMap<>();
    config.parse(inputFiles, () -> false, new AnalysisProgress(inputFiles.size()), (input, result) -> {
      String name = names.get(input);
      try {
        trees.put(name, result.get());
      } catch (Exception failure) {
        errors.put(name, describe(failure));
      }
    });
    StringBuilder body = new StringBuilder();
    List<String> order = new ArrayList<>(names.values());
    order.sort(Comparator.naturalOrder());
    for (String name : order) {
      if (errors.containsKey(name)) {
        JsonObject error = new JsonObject();
        error.addProperty("file", name);
        error.addProperty("error", errors.get(name));
        body.append(error).append('\n');
        continue;
      }
      Tree tree = trees.get(name);
      if (tree == null) {
        JsonObject error = new JsonObject();
        error.addProperty("file", name);
        error.addProperty("error", "no tree");
        body.append(error).append('\n');
        continue;
      }
      new DumpVisitor(name, body).scan(tree);
    }
    if (out.getParent() != null) {
      Files.createDirectories(out.getParent());
    }
    Files.writeString(out, body.toString());
  }

  private static String describe(Throwable failure) {
    String message = failure.getMessage() == null ? failure.toString() : failure.getMessage();
    Throwable root = failure;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    if (root == failure) {
      return message;
    }
    StringBuilder cause = new StringBuilder(message).append(": ").append(root);
    for (StackTraceElement frame : root.getStackTrace()) {
      if (frame.getClassName().startsWith("org.sonar")) {
        cause.append(" at ").append(frame.getClassName()).append('.').append(frame.getMethodName()).append(':').append(frame.getLineNumber());
        break;
      }
    }
    return cause.toString();
  }

  private static List<File> classpathEntries(String classpath) {
    List<File> entries = new ArrayList<>();
    if (classpath.isBlank()) {
      return entries;
    }
    for (String entry : classpath.split(java.io.File.pathSeparator)) {
      if (!entry.isBlank()) {
        entries.add(new File(entry));
      }
    }
    return entries;
  }

  private static String relative(@Nullable Path root, Path file) {
    if (root == null) {
      return file.getFileName().toString();
    }
    return root.relativize(file).toString().replace('\\', '/');
  }

  private static final class DumpVisitor extends BaseTreeVisitor {
    private final String file;
    private final StringBuilder body;
    private final java.util.ArrayDeque<String> stack = new java.util.ArrayDeque<>();

    private DumpVisitor(String file, StringBuilder body) {
      this.file = file;
      this.body = body;
    }

    @Override
    protected void scan(@Nullable List<? extends Tree> trees) {
      if (trees == null) {
        return;
      }
      Map<Tree.Kind, Integer> seen = new HashMap<>();
      for (Tree tree : trees) {
        if (tree == null) {
          continue;
        }
        int index = seen.merge(tree.kind(), 1, Integer::sum) - 1;
        enter(tree, index);
      }
    }

    @Override
    protected void scan(@Nullable Tree tree) {
      if (tree != null) {
        enter(tree, 0);
      }
    }

    private void enter(Tree tree, int index) {
      String parent = stack.isEmpty() ? "" : stack.peek();
      String path = (parent.isEmpty() ? "" : parent + "/") + tree.kind().name() + "[" + index + "]";
      stack.push(path);
      emit(tree, path);
      tree.accept(this);
      stack.pop();
    }

    private void emit(Tree tree, String path) {
      JsonObject node = new JsonObject();
      node.addProperty("file", file);
      node.addProperty("path", path);
      node.addProperty("kind", tree.kind().name());
      span(tree, node);
      String text = text(tree);
      if (text != null) {
        node.addProperty("text", text);
      }
      Symbol symbol = symbol(tree);
      if (symbol != null) {
        String erased = symbolId(symbol, true);
        String full = symbolId(symbol, false);
        node.addProperty("symbol", erased);
        if (!erased.equals(full)) {
          node.addProperty("symbolFull", full);
        }
      }
      Type type = type(tree);
      if (type != null) {
        String erased = typeName(type, true);
        String full = typeName(type, false);
        node.addProperty("type", erased);
        if (!erased.equals(full)) {
          node.addProperty("typeFull", full);
        }
      }
      body.append(node).append('\n');
    }

    private static void span(Tree tree, JsonObject node) {
      SyntaxToken first = tree.firstToken();
      SyntaxToken last = tree.lastToken();
      if (first == null || last == null || first.range() == null || last.range() == null) {
        return;
      }
      node.addProperty("sl", first.range().start().line());
      node.addProperty("sc", first.range().start().columnOffset());
      node.addProperty("el", last.range().end().line());
      node.addProperty("ec", last.range().end().columnOffset());
    }

    @Nullable
    private static String text(Tree tree) {
      String value = null;
      if (tree instanceof IdentifierTree identifier) {
        value = identifier.name();
      } else if (tree instanceof LiteralTree literal) {
        value = literal.value();
      } else if (tree instanceof SyntaxToken token) {
        value = token.text();
      }
      if (value == null || value.length() > 80) {
        return null;
      }
      return value;
    }

    @Nullable
    private static Symbol symbol(Tree tree) {
      if (tree instanceof IdentifierTree identifier) {
        return identifier.symbol();
      }
      if (tree instanceof MethodInvocationTree invocation) {
        return invocation.methodSymbol();
      }
      if (tree instanceof MethodTree method) {
        return method.symbol();
      }
      if (tree instanceof VariableTree variable) {
        return variable.symbol();
      }
      if (tree instanceof ClassTree clazz) {
        return clazz.symbol();
      }
      if (tree instanceof NewClassTree creation) {
        return creation.methodSymbol();
      }
      return null;
    }

    @Nullable
    private static Type type(Tree tree) {
      if (tree instanceof ExpressionTree expression) {
        return expression.symbolType();
      }
      return null;
    }

    private static String symbolId(Symbol symbol, boolean erase) {
      if (symbol.isUnknown()) {
        return "unknown";
      }
      if (symbol.isMethodSymbol()) {
        Symbol.MethodSymbol method = (Symbol.MethodSymbol) symbol;
        return ownerName(method, erase) + "#" + method.name() + "(" + join(method.parameterTypes(), erase) + ")";
      }
      if (symbol.isTypeSymbol()) {
        return typeName(symbol.type(), erase);
      }
      if (symbol.isVariableSymbol()) {
        String owner = ownerName(symbol, erase);
        return owner.isEmpty() ? symbol.name() : owner + "#" + symbol.name();
      }
      return symbol.name();
    }

    private static String ownerName(Symbol symbol, boolean erase) {
      Symbol owner = symbol.owner();
      if (owner == null || owner.isUnknown() || owner.isPackageSymbol()) {
        return "";
      }
      if (owner.isTypeSymbol() || owner.isMethodSymbol()) {
        return symbolId(owner, erase);
      }
      return owner.name();
    }

    private static String join(List<Type> types, boolean erase) {
      StringBuilder joined = new StringBuilder();
      for (Type type : types) {
        if (!joined.isEmpty()) {
          joined.append(',');
        }
        joined.append(typeName(type, erase));
      }
      return joined.toString();
    }

    private static String typeName(@Nullable Type type, boolean erase) {
      if (type == null || type.isUnknown()) {
        return "unknown";
      }
      String name = type.fullyQualifiedName();
      return erase ? erase(name) : name;
    }

    private static String erase(String name) {
      StringBuilder out = new StringBuilder();
      int depth = 0;
      for (int i = 0; i < name.length(); i++) {
        char character = name.charAt(i);
        if (character == '<') {
          depth++;
        } else if (character == '>') {
          depth = Math.max(0, depth - 1);
        } else if (depth == 0) {
          out.append(character);
        }
      }
      return out.toString();
    }
  }
}
