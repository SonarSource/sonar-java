package org.sonar.java.ecj;

import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;

public class SemaTest {

  @Test
  public void this_constructor_invocation__this_keyword_should_be_bound_to_constructor() {
    MethodInvocationTree t = subject(Tree.Kind.METHOD_INVOCATION, "class C { C() { this(); } }");
    IdentifierTree thisKeyword = (IdentifierTree) t.methodSelect();
    assertEquals("<init>", thisKeyword.symbol().name());
  }

  @Test
  public void super_constructor_invocation__super_keyword_should_be_bound_to_constructor() {
    MethodInvocationTree t = subject(Tree.Kind.METHOD_INVOCATION, "class C { C() { super(); } }");
    IdentifierTree superKeyword = (IdentifierTree) t.methodSelect();
    assertEquals("<init>", superKeyword.symbol().name());
  }

  @Test
  public void class_instance_creation__identifier_should_be_bound_to_constructor() {
    NewClassTree t = subject(Tree.Kind.NEW_CLASS, "class C { void m() { new C(); } }");
    IdentifierTree identifier = (IdentifierTree) t.identifier();
    assertEquals("<init>", identifier.symbol().name());
  }

  @Test
  public void enum_constant_declaration__identifier_should_be_bound_to_constructor() {
    EnumConstantTree t = subject(Tree.Kind.ENUM_CONSTANT, "enum E { C }");
    assertEquals("<init>", t.simpleName().symbol().name());
  }

  @org.junit.Ignore
  @Test
  public void this_expression__this_keyword_should_be_bound() {
    MemberSelectExpressionTree t = subject(Tree.Kind.MEMBER_SELECT, "class C { void m() { equals( C.this ); } }");
    assertEquals("this", t.identifier().symbol().name());
  }

  private static <T> T subject(Tree.Kind kind, String source) {
//    Tree tree = old(source);
    Tree tree = EcjParser.parse(source);

    @SuppressWarnings("unchecked")
    T result = (T) find(kind, tree);
    return result;
  }

  private static Tree old(String source) {
    CompilationUnitTree oldTree = (CompilationUnitTree) JavaParser.createParser().parse(source);
    SemanticModel.createFor(oldTree, new SquidClassLoader(Collections.emptyList()));
    return oldTree;
  }

  private static Tree find(Tree.Kind kind, Tree node) {
    if (node.is(kind)) {
      return node;
    }

    Iterator<? extends Tree> i = ShapeTest.iteratorFor(node);
    while (i.hasNext()) {
      Tree child = find(kind, i.next());
      if (child != null) {
        return child;
      }
    }
    return null;
  }

}
