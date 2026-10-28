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
package org.sonar.java.model.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethodCallExpression;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PsiTreeConverterTest {

  private static PsiTestSession session;
  private static final Set<Tree.Kind> observedKinds = java.util.EnumSet.noneOf(Tree.Kind.class);

  @BeforeAll
  static void open() {
    session = new PsiTestSession();
  }

  @AfterAll
  static void close() {
    if (session != null) {
      session.close();
    }
    var expected = java.util.EnumSet.allOf(Tree.Kind.class);
    expected.remove(Tree.Kind.OTHER);
    assertThat(observedKinds).containsAll(expected);
  }

  static List<String> sources() {
    return List.of(
      "",
      "// comment only\n",
      "/* header */ package sample; import java.util.*; import static java.util.List.of; class A {} // trailing",
      "import module java.base; class A {}",
      "class A { public static final int VALUE = 1; String name; void m(int x, String y) { return; } }",
      "class A { int a=1,b=2; void m(){ int x=3,y=4; x=y; } }",
      "class A { ; static { foo(); } { foo(); } A() { this(1); } A(int n) { super(); } void foo() {} }",
      "@interface A { int value() default 1; String[] names() default {\"a\",\"b\",}; }",
      "@Deprecated class A<T extends Number & Comparable<T>> extends B implements C, D {} class B {} interface C {} interface D {}",
      "class A { java.util.Map<String, java.util.List<Integer>> map; java.util.List<? extends Number> numbers; java.util.List<? super String> strings; java.util.List<?> any; }",
      "class A { int[] a; int b[]; int[][] c; void m(String... args) {} }",
      "record A(int x, String y) implements java.io.Serializable { A { } A(int x) { this(x, null); } }",
      "enum A { X, Y(1) { void m() {} }; A() {} A(int x) {} }",
      "class A { Object m() { return new Runnable() { public void run() {} }; } }",
      "class A { Object m() { class Local {} return new Local(); } }",
      "class A { void m() { if (true) foo(); else bar(); while (true) { break; } do { continue; } while (false); } }",
      "class A { void m() { for (int i=0,j=1;i<10;i++,j--) { } for(;;); for(String s : values) foo(s); } }",
      "class A { void m() { label: while(true) { if (true) break label; continue label; } assert true; assert false : \"detail\"; synchronized(this) {} throw new RuntimeException(); } }",
      "class A { void m() { try { foo(); } catch (IllegalArgumentException | IllegalStateException ex) {} finally {} } }",
      "class A { void m() { try (var a = open(); var b = open();) {} } }",
      "class A { void m() { switch(x) { case 1: case 2: foo(); break; default: bar(); } } }",
      "class A { int m() { return switch(x) { case 1, 2 -> 3; default -> { yield 4; } }; } }",
      "class A { void m() { int a = 1+2*3-4/5%6; boolean b = a<2 && a>=1 || a==3; a += 2; a >>>= 1; ++a; a--; Object x = b ? a : null; } }",
      "class A { Object m(Object x) { return (Runnable & java.io.Serializable) x; } boolean test(Object x) { return x instanceof String s; } }",
      "class A { Object a = (x,y) -> x; Object b = x -> x; Object c = (String x) -> { return x; }; Object d = String::valueOf; Object e = A::new; }",
      "class A { int[][] a = new int[2][]; String[] b = new String[]{\"a\",\"b\",}; Object c = int[].class; int d = a[0][1]; }",
      "class A { Object a = this; Object b = A.this; void m() { super.foo(); this.foo(); foo(); obj.<String>foo(); } }",
      "class A { void m(int a, int b) { a = +a * -b / a % b + a - b; a = ~a; boolean x = !true || false && a<b && a>b && a<=b && a>=b && a==b && a!=b; a = a & b | a ^ b; a = a << b >> a >>> b; a*=b; a/=b; a%=b; a+=b; a-=b; a<<=b; a>>=b; a>>>=b; a&=b; a^=b; a|=b; ++a; --a; a++; a--; } }",
      "class A { long a = -9223372036854775808L; int b = -2147483648; float c = 1f; double d = 0x1.fp2; char e = 'a'; boolean f = true; }",
      "class A { String text = \"\"\"\nhello\nworld\n\"\"\"; }",
      "sealed interface A permits B {} final class B implements A {}",
      "class A { int m(Object o) { return switch(o) { case String s when !s.isEmpty() -> 1; case null, default -> 0; }; } }",
      "record Pair(int x, int y) {} class A { boolean m(Object o) { return o instanceof Pair(int x, int y); } }",
      "void main() { System.out.println(\"hello\"); }",
      "class A { A(int x) { int y=x+1; super(); } }",
      "/// heading\n/// description\nclass A {}",
      "package p; ;",
      "package p; ; class A {}",
      "enum E { ; }",
      "enum E { ; void m() {} }",
      "class A { int \u0061; String emoji=\"😀\"; }");
  }

  @ParameterizedTest
  @MethodSource("sources")
  void matches_ecj_syntax(String source) {
    compare("File.java", source);
  }

  static List<String> existingModelSources() throws IOException {
    String code = Files.readString(Path.of("src/test/java/org/sonar/java/model/JavaTreeModelTest.java"));
    List<String> sources = session.read("JavaTreeModelTest.java", code, file -> {
      List<String> result = new ArrayList<>();
      collectModelSources(file, result);
      return result.stream().distinct().toList();
    });
    assertThat(sources).hasSizeGreaterThan(150);
    return sources;
  }

  private static void collectModelSources(PsiElement element, List<String> sources) {
    if (element instanceof PsiMethodCallExpression call
      && Set.of("firstType", "firstTypeMember", "firstMethodFirstStatement", "expressionOfReturnStatement", "compilationUnit", "parse",
        "annotations", "firstMethodInvocation", "firstConstructorInvocation", "firstMethodParameter")
        .contains(call.getMethodExpression().getReferenceName())
      && call.getArgumentList().getExpressions().length != 0
      && call.getArgumentList().getExpressions()[0] instanceof PsiLiteralExpression literal
      && literal.getValue() instanceof String source) {
      sources.add(source);
    }
    for (PsiElement child : element.getChildren()) {
      collectModelSources(child, sources);
    }
  }

  @ParameterizedTest
  @MethodSource("existingModelSources")
  void existing_model_syntax(String source) {
    compare("File.java", source);
  }

  @Test
  void module_declarations() {
    compare("package-info.java", "@Deprecated package sample;");
    compare("module-info.java", "@Deprecated open module sample.module { requires static transitive java.base; exports sample.api to other.module, another; opens sample.impl; uses sample.Service; provides sample.Service with sample.Impl, sample.Other; }");
  }

  @Test
  void physical_line_endings_and_comments() {
    for (String newline : List.of("\n", "\r\n", "\r")) {
      compare("File.java", "/* header */" + newline + "class A { // line" + newline + "\tString s = \"😀\"; /* tail */ }" + newline + "// eof");
    }
  }

  @Test
  void existing_token_completeness_source() throws IOException {
    compare("TreeTokenCompletenessTest.java", Files.readString(Path.of("src/test/java/org/sonar/java/model/TreeTokenCompletenessTest.java")));
  }

  static List<Path> existingSourceFiles() throws IOException {
    List<Path> result = new ArrayList<>();
    for (String directory : List.of("model", "ast", "highlighter", "cfg")) {
      try (var files = Files.walk(Path.of("src/test/files", directory))) {
        result.addAll(files.filter(path -> path.toString().endsWith(".java")).sorted().toList());
      }
    }
    return result;
  }

  @ParameterizedTest
  @MethodSource("existingSourceFiles")
  void existing_source_files(Path file) throws IOException {
    compare(file.getFileName().toString(), Files.readString(file));
  }

  @Test
  void unicode_identifiers_use_the_source_bridge_while_raw_psi_remains_unsupported() {
    String escape = Character.toString(92) + "u0061";
    String source = "class A { int " + escape + " = 1; int b = " + escape + "; }";
    assertThat(JParserTestUtils.parse(source).types()).hasSize(1);
    session.read("File.java", source, file -> {
      assertThat(com.intellij.psi.util.PsiTreeUtil.findChildOfType(file, com.intellij.psi.PsiErrorElement.class)).isNotNull();
      assertThatThrownBy(() -> PsiTreeConverter.convert(file, source))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unicode escapes");
      assertThatThrownBy(() -> PsiTreeConverter.convert(file, PsiSourceSnapshot.identity(source)))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("parse errors");
      return null;
    });
    compare("File.java", source);
  }

  static List<String> unicodeSources() {
    String slash = Character.toString(92);
    return List.of(
      "cl" + escape("0061") + "ss " + escape("0041") + " " + escape("007b") + " " + escape("0069") + "nt " + escape("0061") + " = " + escape("0031") + escape("003b") + " int b = " + escape("0061") + "; " + escape("007d"),
      "class A { int na" + escape("006d") + "e; int m" + escape("0065") + "thod() { return na" + escape("006d") + "e; } }",
      "class A { int " + slash + "uuuu0061; int " + escape("D801") + escape("DC00") + "; }",
      "class A { int a" + escape("0000") + "b; int a" + escape("200c") + "c; }",
      "class A { String s = \"" + escape("0061") + "\"; char c = '" + escape("0062") + "'; }",
      "class A { int " + escape("0061") + "; String s = \"" + slash.repeat(2) + "u0061\"; String n = \"" + slash + "n\"; }",
      "class A { char c = '" + escape("005c") + escape("005c") + "'; }",
      "class A { String s = \"" + escape("005c") + escape("005c") + "u0061\"; }",
      "class A { // " + escape("005c") + "u000a int hidden;\n int visible; }",
      "class A { // comment" + escape("000a") + "int a; }",
      "class A { // comment" + escape("000d") + "int a; }",
      "class A { /* before " + escape("000a") + " after */ int a; } // " + escape("0061"),
      "/// heading " + escape("0061") + "\n/// next\nclass A {}",
      "import java" + escape("002e") + "util.List; class A { java" + escape("002e") + "util.List<String> a; }",
      "class A { java.util.List<Str" + escape("0069") + "ng> a; }",
      "class A { java.util.List<String" + escape("003e") + " a; }",
      "class A { java.util.List<java.util.List<String" + escape("003e") + escape("003e") + " a; }",
      "class A { int a = 8 " + escape("003e") + escape("003e") + " 1; }",
      "class A { int a=1" + escape("002c") + "b=2" + escape("003b") + " void m() { try(var r=open()" + escape("003b") + ") {} } }",
      "class A { String s = " + escape("0022") + "hello" + escape("0022") + "; }",
      "class A { String s = " + escape("0022").repeat(3) + "\nhello\n" + escape("0022").repeat(3) + "; }",
      "class A { int " + escape("0061") + "; String s = \"\"\"\nfirst" + slash + "\nsecond\n\"\"\"; }",
      "class A { int " + escape("0061") + "; String s = \"\"\"\n" + slash.repeat(2) + "u000a\n\"\"\"; }");
  }

  @ParameterizedTest
  @MethodSource("unicodeSources")
  void unicode_syntax_matches_ecj(String source) {
    compare("File.java", source);
  }

  static List<String> escapedModelSources() throws IOException {
    List<String> result = new ArrayList<>();
    for (String source : existingModelSources()) {
      for (char c : new char[] {'a', '>', '.', ';', ',', ' '}) {
        if (source.indexOf(c) >= 0) {
          result.add(source.replace(Character.toString(c), escape(String.format("%04x", (int) c))));
        }
      }
    }
    return result;
  }

  @ParameterizedTest
  @MethodSource("escapedModelSources")
  void escaped_existing_model_syntax(String source) {
    compare("File.java", source);
  }

  static List<String> unicodeLineEndings() {
    return List.of(escape("000d") + escape("000a"), escape("000d") + "\n", "\r" + escape("000a"),
      escape("000a") + "\r\n", escape("000d") + " ", escape("000a") + escape("000a"));
  }

  @ParameterizedTest
  @MethodSource("unicodeLineEndings")
  void unicode_line_comment_terminators(String ending) {
    compare("File.java", "class A { // comment" + ending + "int a; }");
  }

  @Test
  void unicode_backslash_eligibility_in_comments() {
    String slash = Character.toString(92);
    for (String prefix : List.of("", escape("005c"), escape("005c") + slash, escape("005c") + slash.repeat(2))) {
      for (int count = 0; count <= 8; count++) {
        compare("File.java", "class A { // " + prefix + slash.repeat(count) + "u000a\n int a; }");
      }
    }
  }

  @Test
  void escaped_module_names_and_contextual_keywords() {
    compare("module-info.java", "open mod" + escape("0075") + "le sample" + escape("002e") + "module { requires trans" + escape("0069") + "tive; }");
    compare("module-info.java", "module sample { requires static trans" + escape("0069") + "tive other" + escape("002e") + "module; }");
    compare("File.java", "import mod" + escape("0075") + "le java" + escape("002e") + "base; class A {}");
  }

  @Test
  void escaped_comment_delimiters_retain_the_ecj_validation_failure() {
    for (String source : List.of(escape("002f") + "/ comment\nclass A {}", "/* comment *" + escape("002f") + "class A {}",
      "/" + escape("002a") + "* javadoc */ class A {}", escape("002f") + "// markdown\nclass A {}")) {
      assertThatThrownBy(() -> JParserTestUtils.parse(source)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid comment kind");
      assertThatThrownBy(() -> session.convert("File.java", source)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid comment kind");
    }
  }

  static List<String> unicodeGenericClosers() {
    return List.of(
      "class A { List<String /* inner */ " + escape("003e") + " a; }",
      "class A { List<List<String /* inner */ " + escape("003e") + " /* outer */ > a; }",
      "class A { List<List<String> /* outer */ " + escape("003e") + " a; }",
      "class A { Object x = obj.<String" + escape("003e") + "foo(); }");
  }

  @ParameterizedTest
  @MethodSource("unicodeGenericClosers")
  void unicode_generic_closers_with_trivia(String source) {
    compare("File.java", source);
  }

  private static String escape(String hex) {
    return Character.toString(92) + "u" + hex;
  }

  @Test
  void generic_closers_comments_shifts_and_text_block_continuations() {
    for (String source : List.of(
      "class A { java.util.List<java.util.List<String /* inner */ > /* outer */ > field; int i = 8 >> 1 >>> 1; }",
      "class A { String s = \"\"\"\nfirst" + Character.toString(92) + "\nsecond\n\"\"\"; }",
      "class A { Object f = new java.util.ArrayList<>(); Object g = int[]::new; Object h = this::m; Object i = super::m; }",
      "record P(int a, int b) {} class A { boolean m(Object o) { return o instanceof P(int _, _); } }",
      "class A { void m(Object o) { switch(o) { case String _ -> foo(); default -> bar(); } } }",
      "class A { void m() { Object resource = null; try (resource) {} } }",
      "import java.util.List;; class A {};;")) {
      compare("File.java", source);
    }
  }

  @Test
  void nested_punctuation_and_keywords() {
    for (String source : List.of(
      "@interface Ann { Class<?> value(); } @Ann(String.class) class A {}",
      "class A { boolean a,b,c,d,e; boolean value = (a ? b : c) ? d : e; }",
      "class A { Object x; boolean value = f(x instanceof String) instanceof Object; Object f(boolean b) { return null; } }",
      "class A { int[] a; int[][] b = new int[a[0]][a[1]]; }",
      "class A { String[] a = new @Ann String[2]; String[][] b = new @Ann String @Ann [2] @Ann [3]; }",
      "class A { class B {} Object b = new A().new B(); }",
      "class A { void m() { for (@Ann(true ? 1 : 2) String s : values) {} } } @interface Ann { int value(); }")) {
      compare("File.java", source);
    }
  }

  @Test
  void psi_tokens_reconstruct_the_original_snapshot() {
    for (String newline : List.of("\n", "\r\n", "\r")) {
      String source = "\t/* leading */" + newline + "/// markdown" + newline + "/// continuation" + newline
        + "class A { /** javadoc */ String s = \"😀\"; String block = \"\"\"" + newline
        + "first" + Character.toString(92) + newline + "second" + newline + "\"\"\"; } // trailing" + newline;
      for (String original : List.of(source, source.replace("class", "cl" + escape("0061") + "ss").replace(" ", escape("0020")))) {
        var snapshot = PsiSourceSnapshot.from(original);
        session.read("File.java", snapshot.psiText(), file -> {
          var index = new PsiTokenIndex(file, snapshot);
          int previousEnd = 0;
          StringBuilder reconstructed = new StringBuilder();
          for (var token : index.tokens()) {
            for (var trivia : token.trivias()) {
              previousEnd = appendPiece(original, previousEnd, trivia.range(), trivia.comment(), reconstructed);
            }
            previousEnd = appendPiece(original, previousEnd, token.range(), token.text(), reconstructed);
          }
          assertThat(index.eof().range().start()).isEqualTo(new org.sonar.java.model.LineColumnConverter(original).toPosition(original.length()));
          assertThat(reconstructed).hasToString(original);
          return null;
        });
      }
    }
  }

  private static int appendPiece(String source, int previousEnd, org.sonar.plugins.java.api.location.Range range, String text, StringBuilder result) {
    int start = absoluteOffset(source, range.start());
    int end = absoluteOffset(source, range.end());
    assertThat(start).isGreaterThanOrEqualTo(previousEnd);
    assertThat(PsiSourceSnapshot.from(source.substring(previousEnd, start)).translated().chars().allMatch(Character::isWhitespace)).isTrue();
    assertThat(source.substring(start, end)).isEqualTo(text);
    result.append(source, previousEnd, start).append(text);
    return end;
  }

  private static int absoluteOffset(String source, org.sonar.plugins.java.api.location.Position position) {
    var matcher = java.util.regex.Pattern.compile("\\r\\n?|\\n").matcher(source);
    int start = 0;
    for (int line = 1; line < position.line(); line++) {
      assertThat(matcher.find()).isTrue();
      start = matcher.end();
    }
    return start + position.columnOffset();
  }

  @Test
  void mapping_inventory_covers_the_ecj_converter() throws IOException {
    String parser = Files.readString(Path.of("src/main/java/org/sonar/java/model/JParser.java"));
    var matcher = java.util.regex.Pattern.compile("ASTNode\\.([A-Z_]+)").matcher(parser);
    Set<String> branches = new java.util.TreeSet<>();
    while (matcher.find()) {
      branches.add(matcher.group(1));
    }
    try (var input = getClass().getResourceAsStream("/psi-syntax-mapping.json")) {
      var ledger = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(java.util.Objects.requireNonNull(input), java.nio.charset.StandardCharsets.UTF_8));
      assertThat(ledger.getAsJsonObject().getAsJsonObject("ecjNodes").keySet()).containsExactlyInAnyOrderElementsOf(branches);
    }
  }

  @Test
  void rejects_mismatched_snapshot() {
    session.read("A.java", "class A {}", file -> {
      assertThatThrownBy(() -> PsiTreeConverter.convert(file, "class B {}"))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("snapshot");
      return null;
    });
    String original = "class A { int " + escape("0061") + "; }";
    var snapshot = PsiSourceSnapshot.from(original);
    session.read("A.java", original, file -> {
      assertThatThrownBy(() -> PsiTreeConverter.convert(file, snapshot))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("snapshot");
      return null;
    });
  }

  @Test
  void rejects_parse_errors() {
    String source = "class A { void m() { int x = 1 } }";
    session.read("A.java", source, file -> {
      assertThatThrownBy(() -> PsiTreeConverter.convert(file, source))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("parse errors");
      return null;
    });
  }

  @Test
  void converted_tree_outlives_psi() {
    String source = "class A { int " + escape("0061") + "; String s = \"" + escape("005c") + escape("005c") + "\"; }";
    JavaTree result;
    try (var isolated = new PsiTestSession()) {
      result = isolated.convert("A.java", source);
    }
    assertThat(snapshot(result)).isEqualTo(snapshot((JavaTree) JParserTestUtils.parse(source)));
  }

  @Test
  void mapped_psi_leaves_reconstruct_the_original_source() {
    for (String original : unicodeSources()) {
      var source = PsiSourceSnapshot.from(original);
      session.read("File.java", source.psiText(), file -> {
        var reconstructed = new StringBuilder();
        appendOriginalLeaves(file, source, reconstructed);
        assertThat(reconstructed).hasToString(original);
        return null;
      });
    }
  }

  private static void appendOriginalLeaves(PsiElement element, PsiSourceSnapshot source, StringBuilder result) {
    if (element.getFirstChild() == null) {
      result.append(source.originalText(element.getTextRange().getStartOffset(), element.getTextRange().getEndOffset()));
    } else {
      for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
        appendOriginalLeaves(child, source, result);
      }
    }
  }

  private static void compare(String name, String source) {
    JavaTree expected = (JavaTree) JParserTestUtils.parse(name, source, JParserTestUtils.DEFAULT_CLASSPATH);
    JavaTree actual = session.convert(name, source);
    assertThat(snapshot(actual)).as(source).isEqualTo(snapshot(expected));
  }

  private static List<String> snapshot(JavaTree root) {
    List<String> result = new ArrayList<>();
    snapshot(root, "root", result);
    var paths = new java.util.IdentityHashMap<Tree, String>();
    collectPaths(root, "root", paths);
    appendParents(root, "root", paths, result);
    return result;
  }

  private static void collectPaths(JavaTree tree, String path, java.util.IdentityHashMap<Tree, String> paths) {
    paths.putIfAbsent(tree, path);
    if (!tree.isLeaf()) {
      var children = tree.getChildren();
      for (int i = 0; i < children.size(); i++) {
        collectPaths((JavaTree) children.get(i), path + "/" + i, paths);
      }
    }
  }

  private static void appendParents(JavaTree tree, String path, java.util.IdentityHashMap<Tree, String> paths, List<String> result) {
    result.add(path + " parent=" + paths.get(tree.parent()));
    if (!tree.isLeaf()) {
      var children = tree.getChildren();
      for (int i = 0; i < children.size(); i++) {
        appendParents((JavaTree) children.get(i), path + "/" + i, paths, result);
      }
    }
  }

  private static void appendAccessors(JavaTree tree, String path, List<String> result) {
    var methods = Arrays.stream(tree.kind().getAssociatedInterface().getMethods())
      .filter(method -> method.getDeclaringClass().isInterface() && method.getParameterCount() == 0 && !method.getName().equals("parent"))
      .filter(method -> Tree.class.isAssignableFrom(method.getReturnType())
        || Iterable.class.isAssignableFrom(method.getReturnType()) && method.getGenericReturnType().getTypeName().contains("org.sonar.plugins.java.api.tree."))
      .sorted(java.util.Comparator.comparing(java.lang.reflect.Method::getName)).toList();
    for (var method : methods) {
      try {
        Object value = method.invoke(tree);
        if (value instanceof Iterable<?> values && !(value instanceof SyntaxToken)) {
          List<String> items = new ArrayList<>();
          for (Object item : values) {
            items.add(describe((Tree) item));
          }
          result.add(path + " " + method.getName() + "=" + items);
        } else {
          result.add(path + " " + method.getName() + "=" + describe((Tree) value));
        }
      } catch (ReflectiveOperationException e) {
        throw new AssertionError(path + " " + method, e);
      }
    }
  }

  private static String describe(Tree tree) {
    if (tree == null) {
      return "null";
    }
    if (tree instanceof org.sonar.plugins.java.api.tree.SyntaxTrivia trivia) {
      return trivia.commentKind() + "@" + trivia.range() + ":" + trivia.comment();
    }
    if (tree instanceof SyntaxToken token) {
      return token.text() + "@" + token.range();
    }
    return tree.kind() + "[" + describe(tree.firstToken()) + "," + describe(tree.lastToken()) + "]";
  }

  private static void snapshot(JavaTree tree, String path, List<String> result) {
    observedKinds.add(tree.kind());
    appendAccessors(tree, path, result);
    String prefix = path + " " + tree.getClass().getSimpleName() + " " + tree.kind();
    if (tree instanceof SyntaxToken token) {
      result.add(prefix + " " + token.text() + " " + token.range() + " EOF=" + ((InternalSyntaxToken) token).isEOF());
      for (var trivia : token.trivias()) {
        observedKinds.add(Tree.Kind.TRIVIA);
        result.add(path + " trivia " + trivia.commentKind() + " " + trivia.comment() + " " + trivia.range());
      }
    } else if (!tree.isLeaf()) {
      result.add(prefix);
      var children = tree.getChildren();
      for (int i = 0; i < children.size(); i++) {
        var child = (JavaTree) children.get(i);
        snapshot(child, path + "/" + i, result);
      }
    } else {
      result.add(prefix);
    }
  }
}
