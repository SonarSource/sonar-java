package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ShapeTest {

  @Test
  public void wip() {
    test("class C { void m() { e( new int[0][1][2] ); } }");
  }

  @Test
  public void err() {
    test("class C { interface Foo { public m(); // comment\n } interface Bar { } }");
  }

  @Test
  public void shift_vs_greater_token() {
    test("class C { Map<Object, List<Object>> f; }");
    test("class C { Map<Object, List<Object> > f; }");
  }

  @Test
  public void empty_declaration() {
    test(";");
  }

  /**
   * @see InfixExpression#extendedOperands()
   */
  @Test
  public void extended_operands() {
    test("class C { void m() { m( 1 - 2 - 3 ); } }");
  }

  /**
   * @see MethodDeclaration#extraDimensions()
   * @see VariableDeclarationFragment#extraDimensions()
   * @see SingleVariableDeclaration#extraDimensions()
   */
  @Test
  public void extra_dimensions() {
    test("interface I { int m(int p[])[]; int v[] = null; }");
  }

  @Test
  public void enum_declaration() {
    test("enum E implements I { }");
  }

  @Test
  public void initializer() {
    test("class C { {} }");
    test("class C { static {} }");
  }

  @Test
  public void super_constructor_invocation() {
    test("class C { C() { super(); } }");
  }

  @Test
  public void super_method_invocation() {
    test("class C { void m() { super.toString(); } }");
    test("class C { void m() { C.super.toString(); } }");
  }

  @Test
  public void expression_this() {
    test("class C { void m() { equals( this ); } }");
    test("class C { void m() { equals( C.this ); } }");
  }

  @Test
  public void statements() {
    statement(";");

    statement("label: ;");

    statement("if (true) { }");
    statement("if (true) { } else { }");

    statement("while (true) ;");

    statement("do { } while (true);");

    statement("synchronized (this) { }");

    statement("try { } finally { }");
    statement("try { } catch (Exception e) { }");
    statement("try { } catch (Exception e) { } finally { }");
  }

  private static void statement(String source) {
    test("class C { void m() { " + source + " } }");
  }

  private static void test(String source) {
    JavaTree oldTree = (JavaTree) JavaParser.createParser().parse(source);
    String expected = toString(oldTree);
    System.out.println(expected);

    ETree newTree = (ETree) EcjParser.parse(source);
    String actual = toString(newTree);

    assertEquals(expected, actual);
  }

  private static String toString(Tree node) {
    StringBuilder out = new StringBuilder();
    toString(out, 0, node);
    return out.toString();
  }

  private static void toString(StringBuilder out, int indent, Tree node) {
    for (int i = 0; i < indent; i++) {
      out.append(' ');
    }
    out.append(node.kind());
    if (node.is(Tree.Kind.TRIVIA)) {
      out.append(' ').append(((SyntaxTrivia) node).comment());
    }
    if (node.is(Tree.Kind.TOKEN)) {
      out.append(' ').append(((SyntaxToken) node).text());
    }
    if (node.is(Tree.Kind.IDENTIFIER)) {
      out.append(' ').append(((IdentifierTree) node).name());
    }
    out.append('\n');
    indent += 2;

    Iterator<? extends Tree> i = iteratorFor(node);
    while (i.hasNext()) {
      Tree child = i.next();
      toString(out, indent, child);
    }
  }

  static Iterator<? extends Tree> iteratorFor(Tree node) {
    if (node.kind() == Tree.Kind.TOKEN) {
      return ((SyntaxToken) node).trivias().iterator();
    }
    if (node.kind() == Tree.Kind.INFERED_TYPE || node.kind() == Tree.Kind.TRIVIA) {
      return Collections.emptyIterator();
    }
    final Iterator<Tree> iterator;
    if (node instanceof ETree) {
      iterator = (Iterator<Tree>) ((ETree) node).children();
    } else {
      iterator = ((JavaTree) node).getChildren().iterator();
    }
    return Iterators.filter(
      iterator,
      child -> child != null && !child.is(Tree.Kind.TOKEN) && /* not empty list: */ !(child.is(Tree.Kind.LIST) && ((List) child).isEmpty())
    );
  }

}
