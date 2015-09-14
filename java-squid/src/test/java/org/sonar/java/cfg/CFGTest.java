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
package org.sonar.java.cfg;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class CFGTest {

  public static final ActionParser<Tree> parser = JavaParser.createParser(Charsets.UTF_8);

  private static CFG buildCFG(String methodCode) {
    CompilationUnitTree cut = (CompilationUnitTree) parser.parse("class A { "+methodCode+" }");
    MethodTree tree = ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(0));
    return CFG.build(tree);
  }

  @Test
  public void simplest_cfg() throws Exception {
    CFG cfg = buildCFG("void fun() {}");
    assertThat(cfg.reversedBlocks()).hasSize(2);
    cfg = buildCFG("void fun() { bar();}");
    assertThat(cfg.reversedBlocks()).hasSize(2);
    cfg = buildCFG("void fun() { bar();qix();baz();}");
    assertThat(cfg.reversedBlocks()).hasSize(2);
  }

  @Test
  public void cfg_local_variable() throws Exception {
    CFG cfg = buildCFG("void fun() {Object o;}");
    assertThat(cfg.reversedBlocks()).hasSize(2);
    assertThat(cfg.reversedBlocks().get(0).elements()).isEmpty();
    assertThat(cfg.reversedBlocks().get(1).elements()).hasSize(1);
  }

  @Test
  public void cfg_if_statement() throws Exception {
    CFG cfg = buildCFG("void fun() {if(a) { foo(); } }");
    assertThat(cfg.reversedBlocks()).hasSize(4);
    assertThat(successors(cfg.reversedBlocks().get(1))).containsOnly(0);
    assertThat(successors(cfg.reversedBlocks().get(2))).containsOnly(1);
    assertThat(successors(cfg.reversedBlocks().get(3))).containsOnly(1, 2);
    assertThat(cfg.reversedBlocks().get(3).terminator()).isNotNull();
    assertThat(cfg.reversedBlocks().get(3).elements()).hasSize(1);
    assertThat(cfg.reversedBlocks().get(3).terminator().is(Tree.Kind.IF_STATEMENT)).isTrue();

    cfg = buildCFG("void fun() {if(a) { foo(); } else { bar(); } }");
    assertThat(cfg.reversedBlocks()).hasSize(5);
    assertThat(successors(cfg.reversedBlocks().get(1))).containsOnly(0);
    assertThat(successors(cfg.reversedBlocks().get(2))).containsOnly(1);
    assertThat(successors(cfg.reversedBlocks().get(3))).containsOnly(1);
    assertThat(successors(cfg.reversedBlocks().get(4))).containsOnly(2, 3);
    assertThat(cfg.reversedBlocks().get(4).terminator()).isNotNull();
    assertThat(cfg.reversedBlocks().get(4).elements()).hasSize(1);
    assertThat(cfg.reversedBlocks().get(3).elements()).hasSize(2);
    assertThat(cfg.reversedBlocks().get(2).elements()).hasSize(2);
    assertThat(cfg.reversedBlocks().get(4).terminator().is(Tree.Kind.IF_STATEMENT)).isTrue();

    cfg = buildCFG("void fun() {\nif(a) {\n foo(); \n } else if(b) {\n bar();\n } }");
    assertThat(cfg.reversedBlocks()).hasSize(6);
    assertThat(cfg.reversedBlocks().get(5).terminator().is(Tree.Kind.IF_STATEMENT)).isTrue();
    assertThat(cfg.reversedBlocks().get(3).terminator().is(Tree.Kind.IF_STATEMENT)).isTrue();
  }

  @Test
  public void conditional_or_and() throws Exception {
    CFG cfg = buildCFG("void fun() {if(a || b) { foo(); } }");
    assertThat(cfg.reversedBlocks()).hasSize(5);
    assertThat(cfg.reversedBlocks().get(4).terminator().is(Tree.Kind.CONDITIONAL_OR)).isTrue();
    assertThat(cfg.reversedBlocks().get(3).terminator().is(Tree.Kind.IF_STATEMENT)).isTrue();

    cfg = buildCFG("void fun() {if((a && b)) { foo(); } }");
    assertThat(cfg.reversedBlocks()).hasSize(5);
    assertThat(cfg.reversedBlocks().get(4).terminator().is(Tree.Kind.CONDITIONAL_AND)).isTrue();
    assertThat(cfg.reversedBlocks().get(3).terminator().is(Tree.Kind.IF_STATEMENT)).isTrue();

    cfg = buildCFG("void fun() {boolean bool = a && b;}");
    assertThat(cfg.reversedBlocks()).hasSize(4);
    assertThat(cfg.reversedBlocks().get(3).terminator().is(Tree.Kind.CONDITIONAL_AND)).isTrue();
  }

  @Test
  public void conditional_expression() throws Exception {
    CFG cfg = buildCFG("void fun() { foo ? a : b; a.toString();}");
    assertThat(cfg.reversedBlocks()).hasSize(5);
    assertThat(cfg.reversedBlocks().get(4).terminator().is(Tree.Kind.CONDITIONAL_EXPRESSION)).isTrue();
  }

  @Test
  public void test_switch() throws Exception {
    CFG cfg = buildCFG("void fun(int foo) { int a; switch(foo) { case 1: System.out.println(bar);case 2: System.out.println(qix);break; default: System.out.println(baz);} }");
    assertThat(cfg.reversedBlocks().get(2).terminator().is(Tree.Kind.SWITCH_STATEMENT)).isTrue();
  }

  @Test
  public void return_statement() throws Exception {
    CFG cfg = buildCFG("void fun(Object foo) { if(foo == null) return; }");
    assertThat(cfg.reversedBlocks()).hasSize(5);
    assertThat(cfg.reversedBlocks().get(3).terminator().is(Tree.Kind.RETURN_STATEMENT)).isTrue();
  }

  @Test
  public void for_loops() {
    CFG cfg = buildCFG("void fun(Object foo) {System.out.println(''); for(int i =0;i<10;i++) { System.out.println(i); } }");
    assertThat(cfg.reversedBlocks()).hasSize(6);
    assertThat(cfg.reversedBlocks().get(4).terminator().is(Tree.Kind.FOR_STATEMENT)).isTrue();

    cfg = buildCFG("void fun(Object foo) { for(int i =0;i<10;i++) { if(i == 5) break; } }");
    //orphan nodes are created because of break (not a problem as they won't be visited during se)
    assertThat(cfg.reversedBlocks()).hasSize(9);
    assertThat(cfg.reversedBlocks().get(5).terminator().is(Tree.Kind.BREAK_STATEMENT)).isTrue();
    //orphan nodes are created because of continue (not a problem as they won't be visited during se)
    cfg = buildCFG("void fun(Object foo) { for(int i =0;i<10;i++) { if(i == 5) continue; } }");
    assertThat(cfg.reversedBlocks().get(5).terminator().is(Tree.Kind.CONTINUE_STATEMENT)).isTrue();
    assertThat(cfg.reversedBlocks()).hasSize(9);

    cfg = buildCFG("void fun(){ System.out.println(''); for(String foo:list) {System.out.println(foo);} System.out.println(''); }");
    assertThat(cfg.reversedBlocks()).hasSize(5);
    assertThat(cfg.reversedBlocks().get(3).terminator().is(Tree.Kind.FOR_EACH_STATEMENT)).isTrue();
  }

  @Test
  public void while_loops() {
    CFG cfg = buildCFG("void fun() {int i = 0; while(i < 10) {i++; System.out.println(i); } }");
    assertThat(cfg.reversedBlocks()).hasSize(6);
    assertThat(cfg.reversedBlocks().get(4).terminator().is(Tree.Kind.WHILE_STATEMENT)).isTrue();
    cfg = buildCFG("void fun() {int i = 0; while(i < 10) {i++; if(i == 5) break; } }");
    //orphan nodes are created because of break (not a problem as they won't be visited during se)
    assertThat(cfg.reversedBlocks()).hasSize(9);
    assertThat(cfg.reversedBlocks().get(5).terminator().is(Tree.Kind.BREAK_STATEMENT)).isTrue();
    //orphan nodes are created because of continue (not a problem as they won't be visited during se)
    cfg = buildCFG("void fun() {int i = 0; while(i < 10) {i++; if(i == 5) continue; } }");
    assertThat(cfg.reversedBlocks().get(5).terminator().is(Tree.Kind.CONTINUE_STATEMENT)).isTrue();
    assertThat(cfg.reversedBlocks()).hasSize(9);
  }

  @Test
  public void do_while_loops() {
    CFG cfg = buildCFG("void fun() {int i = 0; do {i++; System.out.println(i); }while(i < 10); }");
    assertThat(cfg.reversedBlocks()).hasSize(6);
    assertThat(cfg.reversedBlocks().get(3).terminator().is(Tree.Kind.DO_STATEMENT)).isTrue();
    cfg = buildCFG("void fun() {int i = 0; do { i++; if(i == 5) break; }while(i < 10); }");
    //orphan nodes are created because of break (not a problem as they won't be visited during se)
    assertThat(cfg.reversedBlocks()).hasSize(9);
    assertThat(cfg.reversedBlocks().get(6).terminator().is(Tree.Kind.BREAK_STATEMENT)).isTrue();
    //orphan nodes are created because of continue (not a problem as they won't be visited during se)
    cfg = buildCFG("void fun() {int i = 0; do{i++; if(i == 5) continue; }while(i < 10); }");
    assertThat(cfg.reversedBlocks()).hasSize(9);
    assertThat(cfg.reversedBlocks().get(6).terminator().is(Tree.Kind.CONTINUE_STATEMENT)).isTrue();
  }

  @Test
  public void label_statement() throws Exception {
    CFG cfg = buildCFG("void fun() { foo: for(int i = 0; i<10;i++) { if(i==5) break foo; } }");
    assertThat(cfg.reversedBlocks()).hasSize(10);
    cfg = buildCFG("void fun() { foo: for(int i = 0; i<10;i++) { if(i==5) continue foo; } }");
    assertThat(cfg.reversedBlocks()).hasSize(10);
  }

  @Test
  public void prefix_operators()  {
    CFG cfg = buildCFG("void fun() { ++i;i++; }");
    List<Tree> elements = cfg.reversedBlocks().get(1).elements();
    assertThat(elements.get(0).kind()).isEqualTo(Tree.Kind.IDENTIFIER);
    assertThat(elements.get(1).kind()).isEqualTo(Tree.Kind.PREFIX_INCREMENT);
    assertThat(elements.get(2).kind()).isEqualTo(Tree.Kind.IDENTIFIER);
    assertThat(elements.get(3).kind()).isEqualTo(Tree.Kind.POSTFIX_INCREMENT);

  }

  @Test
  public void try_statement() throws Exception {
    //Only two blocks connected to each other for now. s
    CFG cfg = buildCFG("void fun() {try {System.out.println('');} finally { System.out.println(''); }}");
    assertThat(cfg.reversedBlocks()).hasSize(5);
  }


  @Test
  public void throw_statement() throws Exception {
    CFG cfg = buildCFG("void fun(Object a) {if(a==null) { throw new Exception();} System.out.println(''); }");
    assertThat(cfg.reversedBlocks()).hasSize(5);
    assertThat(cfg.reversedBlocks().get(3).successors()).hasSize(1);
    //verify that throw statement jumps to exit block.
    assertThat(cfg.reversedBlocks().get(3).successors().get(0)).isEqualTo(cfg.reversedBlocks().get(0));
  }

  @Test
  public void synchronized_statement() throws Exception {
    CFG cfg = buildCFG("void fun(Object a) {if(a==null) { synchronized(a) { foo();bar();} } System.out.println(''); }");
    assertThat(cfg.reversedBlocks()).hasSize(4);
    assertThat(cfg.reversedBlocks().get(2).elements()).hasSize(5);
    assertThat(cfg.reversedBlocks().get(2).elements().get(0).is(Tree.Kind.IDENTIFIER)).isTrue();
    assertThat(((IdentifierTree) cfg.reversedBlocks().get(2).elements().get(0)).name()).isEqualTo("a");
  }

  @Test
  public void test_multiple_constructions() {
    CFG cfg = buildCFG("void fun(Object a) {if(a instanceof String) { a::toString;foo(y -> y+1); a += (String) a;  } }");
    assertThat(cfg.reversedBlocks()).hasSize(4);
    cfg = buildCFG("Object fun(Object a) {return new Foo<String>() {} ;} ");
    cfg = buildCFG("Object fun(Object a) {a = String[].class; return int.class;} ");
    cfg = buildCFG("Object fun(Object a) {class A {} String[]::new;} ");
  }


  @Test
  public void array_access_expression() throws Exception {
    CFG cfg = buildCFG("void fun(int[] array) { array[0] = 1; array[3+2] = 4; }");
    assertThat(cfg.reversedBlocks()).hasSize(2);
    assertThat(cfg.reversedBlocks().get(1).elements()).hasSize(12);
    cfg.debugTo(System.out);
    assertThat(cfg.reversedBlocks().get(1).elements().get(3).kind()).isEqualTo(Tree.Kind.ARRAY_ACCESS_EXPRESSION);
    assertThat(cfg.reversedBlocks().get(1).elements().get(10).kind()).isEqualTo(Tree.Kind.ARRAY_ACCESS_EXPRESSION);
  }

  private static int[] successors(CFG.Block block) {
    int[] successors = new int[block.successors().size()];
    for (int i = 0; i < block.successors().size(); i++) {
      successors[i] = block.successors().get(i).id;
    }
    return successors;
  }




}
