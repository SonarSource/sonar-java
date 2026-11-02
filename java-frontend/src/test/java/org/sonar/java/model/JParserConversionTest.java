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

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.java.test.TestResourceScope;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

@TestResourceScope.Scoped
class JParserConversionTest {
  private static final Set<Tree.Kind> COVERED = EnumSet.noneOf(Tree.Kind.class);

  @ParameterizedTest(name = "{0}")
  @MethodSource("sources")
  void syntax_contract(String name, String unitName, String source) {
    var actual = PsiSyntaxTestSupport.parse(unitName, source);
    PsiSyntaxTestSupport.assertStructure(actual, source);
    COVERED.addAll(PsiSyntaxTestSupport.kinds(actual));
  }

  @AfterAll
  static void every_syntax_kind_is_exercised() {
    var expected = EnumSet.allOf(Tree.Kind.class);
    expected.remove(Tree.Kind.OTHER);
    assertThat(COVERED).containsExactlyInAnyOrderElementsOf(expected);
  }

  @Test
  void variable_groups_preserve_names_dimensions_and_initializers() {
    var tree = PsiSyntaxTestSupport.parse("C.java", "class C { int first=1, second[]=null; void run() { int x=2,y=3; } }");
    var type = (ClassTree) tree.types().getFirst();
    assertThat(type.members()).hasSize(3);
    var first = (VariableTree) type.members().get(0);
    var second = (VariableTree) type.members().get(1);
    assertThat(first.simpleName().name()).isEqualTo("first");
    assertThat(first.type().kind()).isEqualTo(Tree.Kind.PRIMITIVE_TYPE);
    assertThat(((LiteralTree) first.initializer()).value()).isEqualTo("1");
    assertThat(second.simpleName().name()).isEqualTo("second");
    assertThat(second.type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(second.initializer().kind()).isEqualTo(Tree.Kind.NULL_LITERAL);
    var statements = ((MethodTree) type.members().get(2)).block().body();
    assertThat(statements).hasSize(2);
    assertThat(statements.stream().map(VariableTree.class::cast).map(variable -> variable.simpleName().name()))
      .containsExactly("x", "y");
  }

  @Test
  void receiver_parameters_do_not_become_ordinary_parameters() {
    var tree = PsiSyntaxTestSupport.parse("C.java", "class C { void run(C this, int value) {} }");
    var method = (MethodTree) ((ClassTree) tree.types().getFirst()).members().getFirst();
    assertThat(method.parameters()).extracting(parameter -> parameter.simpleName().name()).containsExactly("value");
  }

  @Test
  void empty_declarations_preserve_trailing_semicolons() {
    var tree = PsiSyntaxTestSupport.parse("C.java", ";; class C {;; int value;; void run() {};;};;");
    assertThat(tree.types()).extracting(Tree::kind).containsExactly(Tree.Kind.CLASS, Tree.Kind.EMPTY_STATEMENT, Tree.Kind.EMPTY_STATEMENT);
    assertThat(((ClassTree) tree.types().getFirst()).members()).extracting(Tree::kind)
      .containsExactly(Tree.Kind.EMPTY_STATEMENT, Tree.Kind.EMPTY_STATEMENT, Tree.Kind.VARIABLE, Tree.Kind.EMPTY_STATEMENT, Tree.Kind.METHOD, Tree.Kind.EMPTY_STATEMENT, Tree.Kind.EMPTY_STATEMENT);
  }

  @Test
  void records_keep_components_and_the_compact_constructor() {
    var tree = PsiSyntaxTestSupport.parse("R.java", "record R<T>(T value, int... counts) { R { if(value == null) throw new IllegalArgumentException(); } }");
    var type = (ClassTree) tree.types().getFirst();
    assertThat(type.kind()).isEqualTo(Tree.Kind.RECORD);
    assertThat(type.recordComponents()).extracting(component -> component.simpleName().name()).containsExactly("value", "counts");
    assertThat(type.recordComponents().get(1).type()).isInstanceOf(ArrayTypeTree.class);
    assertThat(type.members()).hasSize(1);
    assertThat(((MethodTree) type.members().getFirst()).block().body()).extracting(Tree::kind).containsExactly(Tree.Kind.IF_STATEMENT);
  }

  @Test
  void switch_rules_keep_multiple_labels_and_yield_blocks() {
    var tree = PsiSyntaxTestSupport.parse("C.java", "class C { int run(int value) { return switch(value) { case 1,2 -> 3; default -> { yield 4; } }; } }");
    var method = (MethodTree) ((ClassTree) tree.types().getFirst()).members().getFirst();
    var expression = (SwitchExpressionTree) ((ReturnStatementTree) method.block().body().getFirst()).expression();
    assertThat(expression.cases()).hasSize(2);
    var first = expression.cases().get(0).labels().getFirst();
    assertThat(first.isFallThrough()).isFalse();
    assertThat(first.expressions()).extracting(value -> ((LiteralTree) value).value()).containsExactly("1", "2");
    assertThat(expression.cases().get(1).body()).extracting(Tree::kind).containsExactly(Tree.Kind.BLOCK);
  }

  static Stream<Arguments> sources() {
    String unicode = "\\" + "u";
    return Stream.of(
      sample("empty", ""),
      sample("ordinary instanceof", "class A { boolean m(Object x){return x instanceof String;} }"),
      sample("annotation declaration tokens", "@A(B.class) public @interface C {}"),
      sample("qualified new with construction qualifier", "class A { Object x=new A().new B(); }"),
      sample("all literals", "class A { Object[] x={1,2L,3.0f,4.0d,true,false,'x',\"x\",null}; String s=\"\"\"\n x\n \"\"\"; }"),
      sample("empty enum", "enum A {; int x;}"),
      sample("inferred declarations", "class A { void m(){var x=1;for(var y:xs){} try(var _=new A()){}catch(E _){} } }"),
      sample("receiver parameters", "class A { void m(A this,int x){} class B { B(A A.this){} } }"),
      sample("wildcards and primitive types", "class A<T extends Number>{ X<?> x; boolean b; byte c; char d; short e; int f; long g; float h; double i; }"),
      sample("remaining operators", "class A { Object[] x={a--, ++a, --a, +a, -a, a-b, a<<b, a>>>b, a<b,a>b,a<=b,a>=b}; }"),
      sample("unicode spelling", "cl" + unicode + "0061ss A { int " + unicode + "0078 = 1; void m(){ " + unicode + "0078++; } }"),
      sample("generic closer whitespace", "class A { X<Y /*c*/ > x; X<Y   > y; X<Y<Z /*d*/ > /*e*/ > z; }"),
      sample("generic closer comments", "class A { X<Y<Z/*c*/>> x; X<Y/*d*/> y; int m(){return a >>> b;} }"),
      sample("trivia", "/*before*/ class A {} // eof"),
      sample("line endings", "class A {\r\n\tint x;\r String y;\n}"),
      sample("markdown", "/// first\n/// second\nclass A {}"),
      Arguments.of("package annotations", "package-info.java", "@Deprecated package a.b;"),
      sample("imports", "package a; import a.B; import static a.B.c; import a.*; import static a.B.*; class C {}"),
      sample("module import", "import module java.base; class A {}"),
      Arguments.of("module directives", "module-info.java", "@Deprecated open module a.b { requires static transitive c.d; exports a.b to c.d, e.f; opens x.y; uses a.B; provides a.B with c.D, e.F; }"),
      sample("empty declarations", ";; class A {;; int x;; void m() {};;};;"),
      sample("type parameters", "class A<@B T extends a.B<T> & C, U> { <V extends D> V m(V v) throws E, F { return v; } }"),
      sample("qualified annotated types", "class A { a.@B C<@D ? extends E>.@F G<? super H> field; }"),
      sample("nested generic closers", "class A { X<Y<Z<String>>> value; void m() { a.<X<Y<Z>>>call(); } }"),
      sample("annotated dimensions", "class A { @B int @C [] @D [] x @E [], y @F []; void m(int @G ... args) {} }"),
      sample("variable groups", "class A { final int a=1,b=2; void m() { int x=1,y[]=null,z; for(int i=0,j=1;i<3;i++,j--){} } }"),
      sample("annotation members", "@interface A { String value() default \"x\"; int[] x() default {1,2,}; B nested() default @B(x=1); }"),
      sample("annotations", "@A({@B(x=1), @B(x=2)}) class C { @D(E.class) int x; }"),
      sample("initializers", "class A { { int x=1; } static { int y=2; } A(){} A(int x){this();} }"),
      sample("enums", "enum A { @B X, Y(1) { void m(){} }, Z(); int x; A(){} A(int n){} }"),
      sample("records", "record A<T>(@B T x, int... y) implements C { A { if(x==null)throw new E(); } A(T x){this(x,1);} }"),
      sample("sealed declarations", "sealed interface A permits B,C {} final class B implements A {} non-sealed class C implements A {}"),
      sample("local and anonymous classes", "class A { void m(){class B{} Object x=new a.B<String>(1){int y; void n(){}};} }"),
      sample("qualified construction", "class A { Object m(){ return outer.new <String> Inner<Integer>(1); } }"),
      sample("operators", "class A { int m(int a,int b){a+=b; a>>>=1; return ((a+b)*2>>1)^(~a&b)|a/b%2;} }"),
      sample("assignments", "class A { void m(){a=b=1; a-=1;a*=2;a/=2;a%=2;a&=2;a|=2;a^=2;a<<=2;a>>=2;} }"),
      sample("conditional", "class A { boolean m(){ return a && b || !c ? x == y : x != y; } }"),
      sample("casts", "class A { Object m(){ return (A & B & C) value; } }"),
      sample("arrays", "class A { int[][] m(){int[][] a={{1,2},{3,4},}; a[0][1]++; return new int[2][];} }"),
      sample("annotated new arrays", "class A { Object x=new @B String @C [] @D [] {{\"x\"}}; Object y=new int @E [1] @F [2]; }"),
      sample("calls and keywords", "class A { A(){<String>this(1);} A(int x){outer.<String>super();} void m(){this.m();super.m();Outer.super.m();Outer.this.m();} }"),
      sample("class literals and references", "class A { Object[] x={int.class, String[].class, A::new, int[]::new, a::m, A::<String>m, super::m, A.super::m}; }"),
      sample("lambdas", "class A { Object[] x={a->a, (a,b)->a+b, ()->{return 1;}, (@B var x, var y)->x, (String... args)->args}; }"),
      sample("loops and labels", "class A { void m(){outer:for(;;){for(String x:xs){if(x==null)continue outer;else break;}do{x++;}while(x<3);while(x>0)x--;break outer;}} }"),
      sample("try and resources", "class A { void m(){try(A x=new A();B y=new B();ref;){throw new E();}catch(E|F|G e){assert x:y;}finally{synchronized(this){}}} }"),
      sample("switch groups", "class A { void m(){switch(x){case 0:case 1:x++;break;case 2,3:x--;default:;}} }"),
      sample("switch rules", "class A { int m(){return switch(x){case 1,2->3;case 4->{yield 5;}default->throw new E();};} }"),
      sample("patterns and guards", "class A { int m(Object x){return switch(x){case R(String s, R(int i, _)) when i>0 -> i;case null,default->0;};} }"),
      sample("instanceof patterns", "class A { boolean m(Object x){return x instanceof R(String s, int _) || x instanceof String t || x instanceof int i;} }"),
      sample("flexible constructors", "class A extends B { A(int x){int y=x; if(y<0)throw new E();super(y);} }"),
      sample("compact source", "int value=1; void main(){System.out.println(value);}"),
      sample("supplementary identifiers", "class A { int 𐐀; String s=\"😀\"; }")
    );
  }

  private static Arguments sample(String name, String source) {
    return Arguments.of(name, "File.java", source);
  }
}
