/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.syntaxtoken;

import com.google.common.base.Charsets;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.fest.assertions.Assertions.assertThat;

public class LastSyntaxTokenFinderTest {

  @Test
  public void compilationUnit() {
    CompilationUnitTree compilationUnit = getCompilationUnit("class Test {}");
    InternalSyntaxToken lastToken = (InternalSyntaxToken) getLastSyntaxToken(compilationUnit);
    assertThat(lastToken.isEOF()).isTrue();

    compilationUnit = getCompilationUnit("package myPackage; import A;");
    lastToken = (InternalSyntaxToken) getLastSyntaxToken(compilationUnit);
    assertThat(lastToken.isEOF()).isTrue();

    compilationUnit = getCompilationUnit("package myPackage;");
    lastToken = (InternalSyntaxToken) getLastSyntaxToken(compilationUnit);
    assertThat(lastToken.isEOF()).isTrue();

    compilationUnit = getCompilationUnit("");
    lastToken = (InternalSyntaxToken) getLastSyntaxToken(compilationUnit);
    assertThat(lastToken.isEOF()).isTrue();
  }

  @Test
  public void classes() {
    CompilationUnitTree compilationUnit = getCompilationUnit("class Test {}");
    SyntaxToken lastToken = getLastSyntaxToken(getFirstClass(compilationUnit));
    assertThat(lastToken.text()).isEqualTo("}");
  }

  @Test
  public void variable() {
    CompilationUnitTree compilationUnit = getCompilationUnit("class Test { Integer i; }");
    SyntaxToken lastToken = getLastSyntaxToken(getFirstVariable(getFirstClass(compilationUnit)));
    assertThat(lastToken.text()).isEqualTo(";");
    compilationUnit = getCompilationUnit("interface Test { void foo(Integer i);}");
    lastToken = getLastSyntaxToken(((MethodTree) getFirstClass(compilationUnit).members().get(0)).parameters().get(0));
    assertThat(lastToken.text()).isEqualTo("i");
  }

  @Test
  public void method() {
    CompilationUnitTree compilationUnit = getCompilationUnit("class Test { void foo() {} }");
    SyntaxToken lastToken = getLastSyntaxToken(getFirstMethod(compilationUnit));
    assertThat(lastToken.text()).isEqualTo("}");

    compilationUnit = getCompilationUnit("class Test { abstract void foo(); }");
    lastToken = getLastSyntaxToken(getFirstMethod(compilationUnit));
    assertThat(lastToken.text()).isEqualTo(";");

    compilationUnit = getCompilationUnit("class Test { abstract int foo(); }");
    lastToken = getLastSyntaxToken(getFirstMethod(compilationUnit).returnType());
    assertThat(lastToken.text()).isEqualTo("int");
  }

  @Test
  public void enumeration() {
    CompilationUnitTree compilationUnit = getCompilationUnit("enum Test { }");
    SyntaxToken lastToken = getLastSyntaxToken(getFirstClass(compilationUnit));
    assertThat(lastToken.text()).isEqualTo("}");

    compilationUnit = getCompilationUnit("enum Test { A; }");
    lastToken = getLastSyntaxToken(getFirstClass(compilationUnit).members().get(0));
    assertThat(lastToken.text()).isEqualTo(";");

    compilationUnit = getCompilationUnit("enum Test { A, B; }");
    lastToken = getLastSyntaxToken(getFirstClass(compilationUnit).members().get(0));
    assertThat(lastToken.text()).isEqualTo(",");

    compilationUnit = getCompilationUnit("enum Test { A }");
    lastToken = getLastSyntaxToken(getFirstClass(compilationUnit).members().get(0));
    assertThat(lastToken.text()).isEqualTo("A");

    compilationUnit = getCompilationUnit("enum Test { A (4); }");
    lastToken = getLastSyntaxToken(getFirstClass(compilationUnit).members().get(0));
    assertThat(lastToken.text()).isEqualTo(";");

    compilationUnit = getCompilationUnit("enum Test { A (4) }");
    lastToken = getLastSyntaxToken(getFirstClass(compilationUnit).members().get(0));
    assertThat(lastToken.text()).isEqualTo(")");

    compilationUnit = getCompilationUnit("enum Test { A (4) { }; }");
    lastToken = getLastSyntaxToken(getFirstClass(compilationUnit).members().get(0));
    assertThat(lastToken.text()).isEqualTo(";");

    compilationUnit = getCompilationUnit("enum Test { A (4) { } }");
    lastToken = getLastSyntaxToken(getFirstClass(compilationUnit).members().get(0));
    assertThat(lastToken.text()).isEqualTo("}");
  }

  @Test
  public void modifiers() {
    String p = "class Foo {}";
    ClassTree c = getFirstClass(getCompilationUnit(p));
    SyntaxToken lastToken = getLastSyntaxToken(c.modifiers());
    assertThat(lastToken).isNull();

    p = "public @Deprecated class Foo {}";
    c = getFirstClass(getCompilationUnit(p));
    lastToken = getLastSyntaxToken(c.modifiers());
    assertThat(lastToken.text()).isEqualTo("Deprecated");

    p = "public @MyAnnotation(42) class Foo {}";
    c = getFirstClass(getCompilationUnit(p));
    lastToken = getLastSyntaxToken(c.modifiers());
    assertThat(lastToken.text()).isEqualTo(")");

    p = "@Deprecated public class Foo {}";
    c = getFirstClass(getCompilationUnit(p));
    lastToken = getLastSyntaxToken(c.modifiers());
    assertThat(lastToken.text()).isEqualTo("public");
  }

  @Test
  public void wildcard() {
    String p =
      "class Foo {"
        + "  void foo(Collection<?> c) {"
        + "  }"
        + "}";
    MethodTree m = getFirstMethod(getCompilationUnit(p));
    SyntaxToken lastToken = getLastSyntaxToken(((ParameterizedTypeTree) m.parameters().get(0).type()).typeArguments().get(0));
    assertThat(lastToken.text()).isEqualTo("?");

    p =
      "class Foo {"
        + "  void foo(Collection<? extends Closeable> c) {"
        + "  }"
        + "}";
    m = getFirstMethod(getCompilationUnit(p));
    lastToken = getLastSyntaxToken(((ParameterizedTypeTree) m.parameters().get(0).type()).typeArguments().get(0));
    assertThat(lastToken.text()).isEqualTo("Closeable");
  }

  @Test
  public void type_parameters() {
    String p = "class Foo<T, S extends Closeable> {}";
    ClassTree c = getFirstClass(getCompilationUnit(p));
    SyntaxToken lastToken = getLastSyntaxToken(c.typeParameters());
    assertThat(lastToken.text()).isEqualTo(">");

    lastToken = getLastSyntaxToken(c.typeParameters().get(0));
    assertThat(lastToken.text()).isEqualTo("T");

    lastToken = getLastSyntaxToken(c.typeParameters().get(1));
    assertThat(lastToken.text()).isEqualTo("Closeable");
  }

  @Test
  public void parameterized_type() {
    String p = "class Foo {"
      + "  Collection<T> foo(T t) {"
      + "  }"
      + "}";
    MethodTree m = getFirstMethod(getCompilationUnit(p));
    SyntaxToken lastToken = getLastSyntaxToken(m.returnType());
    assertThat(lastToken.text()).isEqualTo(">");
  }

  @Test
  public void type_arguments() {
    String p = "class Foo {"
      + "  void foo(Collection<Foo> c) {"
      + "  }"
      + "}";
    MethodTree m = getFirstMethod(getCompilationUnit(p));
    SyntaxToken lastToken = getLastSyntaxToken(((ParameterizedTypeTree) m.parameters().get(0).type()).typeArguments());
    assertThat(lastToken.text()).isEqualTo(">");
  }

  @Test
  public void array_type() {
    CompilationUnitTree compilationUnit = getCompilationUnit("class Test { int[][] i; }");
    SyntaxToken lastToken = getLastSyntaxToken(getFirstVariable(getFirstClass(compilationUnit)).type());
    assertThat(lastToken.text()).isEqualTo("]");
    assertThat(lastToken.column()).isEqualTo(19);

    MethodTree method = getFirstMethod(getCompilationUnit("class Test { void m(int ... a) {} }"));
    lastToken = getLastSyntaxToken(method.parameters().get(0).type());
    assertThat(lastToken.text()).isEqualTo("...");
  }

  @Test
  public void static_initializer() {
    String p =
      "class Foo {"
        + "  static {"
        + "  }"
        + "}";
    ClassTree classTree = getFirstClass(getCompilationUnit(p));
    SyntaxToken lastToken = getLastSyntaxToken(classTree.members().get(0));
    assertThat(lastToken.text()).isEqualTo("}");
  }

  @Test
  public void try_catch() {
    String p =
      "class Foo {"
        + "  void foo() {"
        + "    try { }"
        + "    catch(Exception e) { }"
        + "  }"
        + "}";

    CompilationUnitTree compilationUnit = getCompilationUnit(p);
    TryStatementTree tryStatementTree = (TryStatementTree) getFirstMethod(compilationUnit).block().body().get(0);
    SyntaxToken lastToken = getLastSyntaxToken(tryStatementTree);
    assertThat(lastToken.text()).isEqualTo("}");

    lastToken = getLastSyntaxToken(tryStatementTree.catches().get(0));
    assertThat(lastToken.text()).isEqualTo("}");

    p =
      "class Foo {"
        + "  void foo() {"
        + "    try { }"
        + "    finally { }"
        + "  }"
        + "}";
    compilationUnit = getCompilationUnit(p);
    tryStatementTree = (TryStatementTree) getFirstMethod(compilationUnit).block().body().get(0);
    lastToken = getLastSyntaxToken(tryStatementTree);
    assertThat(lastToken.text()).isEqualTo("}");

    p =
      "class Foo {"
        + "  void foo() {"
        + "    try (Closeable c = new Closeable()) { }"
        + "  }"
        + "}";
    compilationUnit = getCompilationUnit(p);
    tryStatementTree = (TryStatementTree) getFirstMethod(compilationUnit).block().body().get(0);
    lastToken = getLastSyntaxToken(tryStatementTree);
    assertThat(lastToken.text()).isEqualTo("}");
  }

  @Test
  public void union_type() {
    String p =
      "class Foo {"
        + "  void foo() {"
        + "    try {"
        + "    } catch (IOException | SQLException ex) {"
        + "    }"
        + "  }"
        + "}";
    TryStatementTree t = (TryStatementTree) getFirstStatement(p);
    SyntaxToken lastToken = getLastSyntaxToken(t.catches().get(0).parameter().type());
    assertThat(lastToken.text()).isEqualTo("SQLException");
  }

  @Test
  public void empty_statement() {
    String p =
      "class Foo {"
        + "  void foo() {"
        + "    ;"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, ";");
  }

  @Test
  public void expression() {
    String p =
      "class Foo {"
        + "  void foo() {"
        + "    bar();"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, ")");
  }

  @Test
  public void member_select() {
    String p =
      "class Foo {"
        + "  void foo() {"
        + "    A.b.c;"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "c");
  }

  @Test
  public void if_statement() {
    String p =
      "class Foo {"
        + "  void foo(boolean test) {"
        + "    if (test) { }"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "}");

    p =
      "class Foo {"
        + "  void foo(boolean test) {"
        + "    if (test) { }"
        + "    else { }"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "}");
  }

  @Test
  public void assert_statement() {
    String p =
      "class Foo {"
        + "  void foo(boolean test) {"
        + "    assert true;"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, ";");
  }

  @Test
  public void switch_statement() {
    String p =
      "class Foo {"
        + "  void foo(MyEnum myEnum) {"
        + "    switch(myEnum) {"
        + "      case A:"
        + "        bar();"
        + "    }"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "}");

    SwitchStatementTree switchStatementTree = (SwitchStatementTree) getFirstStatement(p);
    CaseGroupTree firstCaseGroup = switchStatementTree.cases().get(0);
    SyntaxToken lastToken = getLastSyntaxToken(firstCaseGroup);
    assertThat(lastToken.text()).isEqualTo(";");

    lastToken = getLastSyntaxToken(firstCaseGroup.labels().get(0));
    assertThat(lastToken.text()).isEqualTo(":");

    p =
      "class Foo {"
        + "  void foo(MyEnum myEnum) {"
        + "    switch(myEnum) {"
        + "      case A:"
        + "        bar();"
        + "      default:"
        + "    }"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "}");

    switchStatementTree = (SwitchStatementTree) getFirstStatement(p);
    CaseGroupTree lastCaseGroup = switchStatementTree.cases().get(1);
    lastToken = getLastSyntaxToken(lastCaseGroup);
    assertThat(lastToken.text()).isEqualTo(":");
  }

  @Test
  public void while_statement() {
    String p =
      "class Foo {"
        + "  void foo() {"
        + "    while(true) {"
        + "    }"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "}");
  }

  @Test
  public void do_while_statement() {
    String p =
      "class Foo {"
        + "  void foo() {"
        + "    do {"
        + "    } while (true);"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, ";");
  }

  @Test
  public void for_statement() {
    String p =
      "class Foo {"
        + "  void foo() {"
        + "    for( ; ; ) {"
        + "    }"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "}");

    p =
      "class Foo {"
        + "  void foo() {"
        + "    for( ; ; )"
        + "      doStuff();"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, ";");
  }

  @Test
  public void foreach_statement() {
    String p =
      "class Foo {"
        + "  void foo(List list) {"
        + "    for(Object o : list) {"
        + "    }"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "}");
  }

  @Test
  public void break_statement() {
    String p =
      "class Foo {"
        + "  void foo(List list) {"
        + "    break;"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, ";");
  }

  @Test
  public void continue_statement() {
    String p =
      "class Foo {"
        + "  void foo(List list) {"
        + "    continue;"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, ";");
  }

  @Test
  public void return_statement() {
    String p =
      "class Foo {"
        + "  void foo(List list) {"
        + "    return;"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, ";");
  }

  @Test
  public void throw_statement() {
    String p =
      "class Foo {"
        + "  void foo(List list) {"
        + "    throw new Exception();"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, ";");
  }

  @Test
  public void synchronized_statement() {
    String p =
      "class Foo {"
        + "  void foo(List list) {"
        + "    synchronized (new Object()) {}"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "}");
  }

  @Test
  public void new_class_statement() {
    String p =
      "class Foo {"
        + "  void foo(List list) {"
        + "    new Foo();"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, ")");

    p =
      "class T {"
        + "  T m() {"
        + "    this.new T(true, false) {};"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "}");
  }

  @Test
  public void literal() {
    String p =
      "class Foo {"
        + "  void foo(List list) {"
        + "    \"test\";"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "\"test\"");
  }

  @Test
  public void label() {
    String p =
      "class Foo {"
        + "  void foo() {"
        + "    FOO:"
        + "      return;"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, ";");
  }

  @Test
  public void unary_expression() {
    String p =
      "class Foo {"
        + "  void foo(int i) {"
        + "    ++i;"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "i");

    p =
      "class Foo {"
        + "  void foo(int i) {"
        + "    i++;"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "++");
  }

  @Test
  public void parenthesis() {
    String p =
      "class Foo {"
        + "  void foo(List list) {"
        + "    ((Object) list);"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, ")");
  }

  @Test
  public void binaryExpression() {
    String p =
      "class Foo {"
        + "  void foo(int a, int b) {"
        + "    a + b;"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "b");
  }

  @Test
  public void array_access() {
    String p =
      "class Foo {"
        + "  void foo(int[] array) {"
        + "    array[4];"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "]");
  }

  @Test
  public void conditional_expression() {
    String p =
      "class Foo {"
        + "  void foo(boolean test) {"
        + "    test ? 1 : 2;"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "2");
  }

  @Test
  public void assignment() {
    String p =
      "class Foo {"
        + "  void foo(boolean test) {"
        + "    test = false;"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "false");
  }

  @Test
  public void new_array() {
    String p =
      "class Foo {"
        + "  void foo() {"
        + "    new int[3];"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "]");

    p =
      "class Foo {"
        + "  void foo() {"
        + "    new int[] {1, 2, 3};"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "}");
  }

  @Test
  public void type_cast() {
    String p =
      "class Foo {"
        + "  void foo(Foo f) {"
        + "    (Object) f;"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "f");
  }

  @Test
  public void instance_of() {
    String p =
      "class Foo {"
        + "  void foo(Foo f) {"
        + "    f instanceof Object;"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "Object");
  }

  @Test
  public void lambda() {
    String p =
      "class Foo {"
        + "  void foo(Person p) {"
        + "    p -> p.printPerson();"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, ")");
  }

  @Test
  public void method_reference() {
    String p =
      "class Foo {"
        + "  void foo() {"
        + "    HashSet::new;"
        + "  }"
        + "}";
    assertLastStatementlastTokenValue(p, "new");
  }

  @Test
  public void syntax_token() {
    String p = "class Foo {}";
    ClassTree firstClass = getFirstClass(getCompilationUnit(p));
    assertLastStatementFirstTokenValue(firstClass.declarationKeyword(), "class");
  }

  private void assertLastStatementFirstTokenValue(Tree tree, String expected) {
    assertThat(getLastSyntaxToken(tree).text()).isEqualTo(expected);
  }

  private void assertLastStatementlastTokenValue(String p, String expected) {
    assertThat(getLastSyntaxToken(getFirstStatement(p)).text()).isEqualTo(expected);
  }

  private SyntaxToken getLastSyntaxToken(Tree tree) {
    return LastSyntaxTokenFinder.lastSyntaxToken(tree);
  }

  private CompilationUnitTree getCompilationUnit(String p) {
    return (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse(p);
  }

  private MethodTree getFirstMethod(CompilationUnitTree compilationUnitTree) {
    return getFirstMethod(getFirstClass(compilationUnitTree));
  }

  private ClassTree getFirstClass(CompilationUnitTree compilationUnitTree) {
    return ((ClassTree) compilationUnitTree.types().get(0));
  }

  private MethodTree getFirstMethod(ClassTree classTree) {
    return (MethodTree) classTree.members().get(0);
  }

  private VariableTree getFirstVariable(ClassTree classTree) {
    return (VariableTree) classTree.members().get(0);
  }

  private Tree getFirstStatement(String p) {
    CompilationUnitTree compilationUnit = getCompilationUnit(p);
    StatementTree statementTree = getFirstMethod(getFirstClass(compilationUnit)).block().body().get(0);
    if (statementTree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      return ((ExpressionStatementTree) statementTree).expression();
    }
    return statementTree;
  }
}
