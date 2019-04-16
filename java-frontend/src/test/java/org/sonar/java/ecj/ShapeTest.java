package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;

public class ShapeTest {

  @Test
  public void wip() {
    test("class C { C c = new C() { }; }");
  }

  /**
   * @see InfixExpression#extendedOperands()
   */
  @Test
  public void extended_operands() {
    test("class C { void m() { m( 1 - 2 - 3 ); } }");
  }

  private static void test(String source) {
    JavaTree oldTree = (JavaTree) JavaParser.createParser().parse(source);
    String expected = toString(oldTree);
    System.out.println(expected);

    ETree newTree = (ETree) EcjParser.parse(source);
    assertEquals(expected, toString(newTree));
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
    out.append(node.kind()).append('\n');
    indent += 2;

    Iterator<? extends Tree> i = iteratorFor(node);
    while (i.hasNext()) {
      Tree child = i.next();
      toString(out, indent, child);
    }
  }

  private static Iterator<Tree> iteratorFor(Tree node) {
    final Iterator<Tree> iterator;
    if (node instanceof ETree) {
      iterator = (Iterator<Tree>) ((ETree) node).children();
    } else {
      iterator = ((JavaTree) node).getChildren().iterator();
    }
    return Iterators.filter(
      iterator,
      child -> child != null && !child.is(Tree.Kind.TOKEN)
    );
  }

}
