package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.model.JavaTree;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ShapeTest {

  @Test
  public void wip() {
    test("class C { Object nop(Object o) { Object r = nop(void.class); /* comment */ return r; } }");
  }

  @Test
  public void empty_declarations() {
    test("/* first */ ;");

    test("package p; /* after package declaration */ ;");

    test("import i; /* after import */ ;");

    test("interface I { /* first */ ; }");

    test("interface I { void m(); /* after method without body */ ; }");

    test("class C { void m() { } /* after method with body */ ; }");

    test("enum E { C ; /* after enum constants */ ; }");
  }

  @Test
  public void enum_declaration() {
    test("enum E { C }");
    test("enum E { C , }");
    test("enum E { C , ; }");
  }

  @Test
  public void weird() {
    // TODO seen in ReplaceLambdaByMethodRefCheck
    test("interface I { void m() { something(); /* comment */ something(); } }");
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

  @Test
  public void statement_try_with_resources() {
    statement("try (Closeable r1 = () -> {} ; Closeable r2 = () -> {}) { }");
    statement("try (r1) { }");
  }

  private static void statement(String source) {
    test("class C { void m() { " + source + " } }");
  }

  private static void test(String source) {
    Formatter formatter = new Formatter();

    CompilationUnitTree oldTree = (CompilationUnitTree) JavaParser.createParser().parse(source);
    SemanticModel.createFor(oldTree, new SquidClassLoader(Collections.emptyList()));
    String expected = formatter.toString(oldTree);
    System.out.println(expected);

    ETree newTree = (ETree) EcjParser.parse(source);
    String actual = formatter.toString(newTree);

    assertEquals(expected, actual);
  }

  static class Formatter {
    boolean showTokens = false;
    boolean showSemantic = false;

    String toString(Tree node) {
      StringBuilder out = new StringBuilder();
      append(out, 0, node);
      return out.toString();
    }

    private void append(StringBuilder out, int indent, Tree node) {
      for (int i = 0; i < indent; i++) {
        out.append(' ');
      }
      out.append(node.kind());

      if (node.is(Tree.Kind.TRIVIA)) {
        out.append(' ').append(((SyntaxTrivia) node).comment());

      } else if (node.is(Tree.Kind.TOKEN)) {
        out.append(' ').append(((SyntaxToken) node).text());

      } else if (node.is(Tree.Kind.IDENTIFIER)) {
        out.append(" name=").append(((IdentifierTree) node).name());
      }

      if (showSemantic) {
        appendSemantic(out, node);
      }

      out.append('\n');
      indent += 2;

      Iterator<? extends Tree> i = iteratorFor(node);
      while (i.hasNext()) {
        Tree child = i.next();
        if (child.is(Tree.Kind.TOKEN) && !showTokens) {
          continue;
        }
        append(out, indent, child);
      }
    }

    private void appendSemantic(StringBuilder out, Tree node) {
      if (node.is(Tree.Kind.CLASS)) {
        ClassTree n = (ClassTree) node;
        out.append(" symbol.name=").append(n.symbol().name());
        out.append(" symbol.type=").append(n.symbol().type().name());

      } else if (node.is(Tree.Kind.VARIABLE)) {
        VariableTree n = (VariableTree) node;
        out.append(" symbol.name=").append(n.symbol().name());
        out.append(" symbol.type=").append(n.symbol().type().name());

      } else if (node.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)) {
        MethodTree n = (MethodTree) node;
        out.append(" symbol.name=").append(n.symbol().name());

      } else if (node.is(Tree.Kind.IDENTIFIER)) {

        // Try to skip identifiers which are not bound in old tree
        if (!node.parent().is(Tree.Kind.VARIABLE, Tree.Kind.METHOD, Tree.Kind.CLASS, Tree.Kind.METHOD_INVOCATION)) {
          IdentifierTree n = (IdentifierTree) node;
          out.append(" symbol.name=").append(n.symbol().name());
          out.append(" type=").append(n.symbolType().fullyQualifiedName());
        }

      } else if (node instanceof ExpressionTree) {
        ExpressionTree e = (ExpressionTree) node;
        out.append(" type=").append(e.symbolType().fullyQualifiedName());
      }
    }
  }

  static Iterator<? extends Tree> iteratorFor(Tree node) {
    if (node.kind() == Tree.Kind.TOKEN) {
      return ((SyntaxToken) node).trivias().iterator();
    }
    if (node.kind() == Tree.Kind.INFERED_TYPE || node.kind() == Tree.Kind.TRIVIA) {
      // old tree throws exception
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
      child -> child != null && /* not empty list: */ !(child.is(Tree.Kind.LIST) && ((List) child).isEmpty())
    );
  }

}
