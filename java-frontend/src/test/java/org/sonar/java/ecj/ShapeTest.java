package org.sonar.java.ecj;

import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.Tree;

public class ShapeTest {

  @Test
  public void our() {
    Tree tree = JavaParser.createParser().parse("@Annotation(42) class C { void C() {  super();  } }");
    System.out.println(tree);
  }

  @Test
  public void ecj() {
    Tree tree = EcjParser.parse("@Annotation class C { void test() {  run((x) -> {});  } }");
    System.out.println(tree);
  }

}
