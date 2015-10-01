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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.fest.assertions.Assertions.assertThat;

public class CFGTest {

  static CFGChecker checker(BlockChecker... checkers) {
    return new CFGChecker(checkers);
  }

  static BlockChecker block(final ElementChecker... checkers) {
    return new BlockChecker(checkers);
  }

  static BlockChecker emptyBlock(final int... successorIDs) {
    return new BlockChecker(successorIDs);
  }

  static ElementChecker element(final Tree.Kind kind) {
    return new ElementChecker(kind);
  }

  static ElementChecker element(final Tree.Kind kind, final String name) {
    return new ElementChecker(kind, name);
  }

  static ElementChecker element(final Tree.Kind kind, final int value) {
    return new ElementChecker(kind, value);
  }

  static TerminatorChecker terminator(final Tree.Kind kind) {
    return new TerminatorChecker(kind);
  }

  private static class CFGChecker {

    private final List<BlockChecker> checkers = new ArrayList<>();

    CFGChecker(BlockChecker... checkers) {
      for (BlockChecker checker : checkers) {
        this.checkers.add(checker);
      }
    }

    public void check(final CFG cfg) {
      try {
        assertThat(cfg.blocks()).as("Expected number of blocks").hasSize(checkers.size() + 1);
        final Iterator<BlockChecker> checkerIterator = checkers.iterator();
        final List<Block> blocks = new ArrayList<>(cfg.blocks());
        final Block exitBlock = blocks.remove(blocks.size()-1);
        for (final Block block : blocks) {
          checkerIterator.next().check(block);
        }
        assertThat(exitBlock.elements()).isEmpty();
        assertThat(exitBlock.successors()).isEmpty();
      } catch (final Throwable e) {
        cfg.debugTo(System.out);
        throw e;
      }
    }
  }

  private static class BlockChecker {

    private int[] successorIDs;
    private final List<ElementChecker> checkers = new ArrayList<>();
    private TerminatorChecker terminatorChecker;

    BlockChecker(final int... ids) {
      successors(ids);
    }

    BlockChecker(final ElementChecker... checkers) {
      for (final ElementChecker checker : checkers) {
        this.checkers.add(checker);
      }
    }

    BlockChecker successors(final int... ids) {
      successorIDs = new int[ids.length];
      int n = 0;
      for (int i : ids) {
        successorIDs[n++] = i;
      }
      Arrays.sort(successorIDs);
      return this;
    }

    BlockChecker terminator(final Kind kind) {
      this.terminatorChecker = new TerminatorChecker(kind);
      return this;
    }

    public void check(final Block block) {
      assertThat(block.elements()).as("Expected number of elements in block " + block.id).hasSize(checkers.size());
      final Iterator<ElementChecker> checkerIterator = checkers.iterator();
      for (final Tree element : block.elements()) {
        checkerIterator.next().check(element);
      }
      if (successorIDs != null) {
        assertThat(block.successors()).as("Expected number of successors in block " + block.id).hasSize(successorIDs.length);
        final int[] actualSuccessorIDs = new int[successorIDs.length];
        int n = 0;
        for (final Block successor : block.successors()) {
          actualSuccessorIDs[n++] = successor.id;
        }
        Arrays.sort(actualSuccessorIDs);
        assertThat(actualSuccessorIDs).as("Expected successors in block " + block.id).isEqualTo(successorIDs);
      }
      if (terminatorChecker != null) {
        terminatorChecker.check(block.terminator());
      }
    }
  }

  private static class ElementChecker {

    private final Tree.Kind kind;
    private final String name;

    ElementChecker(final Tree.Kind kind, final String name) {
      super();
      this.kind = kind;
      this.name = name;
      switch (kind) {
        case VARIABLE:
        case IDENTIFIER:
        case CHAR_LITERAL:
          break;
        default:
          throw new IllegalArgumentException("Unsupported element kind!");
      }
    }

    ElementChecker(final Tree.Kind kind, final int value) {
      super();
      this.kind = kind;
      this.name = Integer.toString(value);
      switch (kind) {
        case INT_LITERAL:
          break;
        default:
          throw new IllegalArgumentException("Unsupported element kind!");
      }
    }

    public ElementChecker(final Tree.Kind kind) {
      super();
      this.kind = kind;
      name = null;
      switch (kind) {
        case METHOD_INVOCATION:
        case MEMBER_SELECT:
        case NULL_LITERAL:
        case EQUAL_TO:
        case NOT_EQUAL_TO:
        case LESS_THAN:
        case LESS_THAN_OR_EQUAL_TO:
        case GREATER_THAN:
        case GREATER_THAN_OR_EQUAL_TO:
        case POSTFIX_INCREMENT:
        case PREFIX_INCREMENT:
        case POSTFIX_DECREMENT:
        case PREFIX_DECREMENT:
        case TRY_STATEMENT:
        case NEW_CLASS:
        case NEW_ARRAY:
        case INSTANCE_OF:
        case LAMBDA_EXPRESSION:
        case TYPE_CAST:
        case PLUS_ASSIGNMENT:
        case ASSIGNMENT:
        case ARRAY_ACCESS_EXPRESSION:
        case PLUS:
          break;
        default:
          throw new IllegalArgumentException("Unsupported element kind!");
      }
    }

    public void check(final Tree element) {
      assertThat(element.kind()).as("Element kind").isEqualTo(kind);
      switch (element.kind()) {
        case VARIABLE:
          assertThat(((VariableTree) element).simpleName().name()).as("Variable name").isEqualTo(name);
          break;
        case IDENTIFIER:
          assertThat(((IdentifierTree) element).identifierToken().text()).as("Identifier").isEqualTo(name);
          break;
        case INT_LITERAL:
          assertThat(((LiteralTree) element).token().text()).as("Integer").isEqualTo(name);
          break;
        case CHAR_LITERAL:
          assertThat(((LiteralTree) element).token().text()).as("String").isEqualTo(name);
          break;
        default:
          // No need to test any associated symbol for the other cases
          break;
      }
    }
  }

  private static class TerminatorChecker {

    private final Kind kind;

    private TerminatorChecker(final Tree.Kind kind) {
      this.kind = kind;
      switch (kind) {
        case IF_STATEMENT:
        case CONDITIONAL_OR:
        case CONDITIONAL_AND:
        case CONDITIONAL_EXPRESSION:
        case BREAK_STATEMENT:
        case CONTINUE_STATEMENT:
        case SWITCH_STATEMENT:
        case RETURN_STATEMENT:
        case FOR_STATEMENT:
        case FOR_EACH_STATEMENT:
        case WHILE_STATEMENT:
        case DO_STATEMENT:
        case THROW_STATEMENT:
          break;
        default:
          throw new IllegalArgumentException("Unexpected terminator kind!");
      }
    }

    public void check(final Tree element) {
      assertThat(element).as("Element kind").isNotNull();
      assertThat(element.kind()).as("Element kind").isEqualTo(kind);
    }

  }

  public static final ActionParser<Tree> parser = JavaParser.createParser(Charsets.UTF_8);

  private static CFG buildCFG(final String methodCode) {
    final CompilationUnitTree cut = (CompilationUnitTree) parser.parse("class A { "+methodCode+" }");
    final MethodTree tree = ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(0));
    return CFG.build(tree);
  }

  @Test
  public void empty_cfg() throws Exception {
    final CFG cfg = buildCFG("void fun() {}");
    final CFGChecker cfgChecker = checker(emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void simplest_cfg() throws Exception {
    final CFG cfg = buildCFG("void fun() { bar();}");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void straight_method_calls() throws Exception {
    final CFG cfg = buildCFG("void fun() { bar();qix();baz();}");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Tree.Kind.METHOD_INVOCATION),
        element(Tree.Kind.IDENTIFIER, "qix"),
        element(Tree.Kind.METHOD_INVOCATION),
        element(Tree.Kind.IDENTIFIER, "baz"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void single_declaration() throws Exception {
    final CFG cfg = buildCFG("void fun() {Object o;}");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.VARIABLE, "o")
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void if_then() throws Exception {
    final CFG cfg = buildCFG("void fun() {if(a) { foo(); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a")
        ).terminator(Tree.Kind.IF_STATEMENT).successors(1,2),
      block(
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(1),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void if_then_else() throws Exception {
    final CFG cfg = buildCFG("void fun() {if(a) { foo(); } else { bar(); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a")
        ).terminator(Tree.Kind.IF_STATEMENT).successors(2,3),
      block(
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(1),
      block(
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(1),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void if_then_elseif() throws Exception {
    final CFG cfg = buildCFG("void fun() {\nif(a) {\n foo(); \n } else if(b) {\n bar();\n } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a")
        ).terminator(Tree.Kind.IF_STATEMENT).successors(4,3),
      block(
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(1),
      block(
        element(Tree.Kind.IDENTIFIER, "b")
        ).terminator(Tree.Kind.IF_STATEMENT).successors(2,1),
      block(
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(1),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void conditionalOR() throws Exception {
    final CFG cfg = buildCFG("void fun() {if(a || b) { foo(); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a")
        ).terminator(Tree.Kind.CONDITIONAL_OR).successors(2, 3),
      block(
        element(Tree.Kind.IDENTIFIER, "b")
        ).terminator(Tree.Kind.IF_STATEMENT).successors(1, 2),
      block(
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(1),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void conditionalAND() throws Exception {
    final CFG cfg = buildCFG("void fun() {if((a && b)) { foo(); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a")
        ).terminator(Tree.Kind.CONDITIONAL_AND).successors(1, 3),
      block(
        element(Tree.Kind.IDENTIFIER, "b")
        ).terminator(Tree.Kind.IF_STATEMENT).successors(1, 2),
      block(
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(1),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void assignmentAND() throws Exception {
    final CFG cfg = buildCFG("void fun() {boolean bool = a && b;}");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a")
        ).terminator(Tree.Kind.CONDITIONAL_AND).successors(1, 2),
      block(
        element(Tree.Kind.IDENTIFIER, "b")
        ).successors(1),
      block(
        element(Tree.Kind.VARIABLE, "bool")
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void three_branch_if() throws Exception {
    final CFG cfg = buildCFG("void fun() { foo ? a : b; a.toString();}");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "foo")
        ).terminator(Tree.Kind.CONDITIONAL_EXPRESSION).successors(2, 3),
      block(
        element(Tree.Kind.IDENTIFIER, "a")
        ).successors(1),
      block(
        element(Tree.Kind.IDENTIFIER, "b")
        ).successors(1),
      block(
        element(Tree.Kind.IDENTIFIER, "a"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void switch_statement() throws Exception {
    final CFG cfg = buildCFG("void fun(int foo) { int a; switch(foo) { case 1: System.out.println(bar);case 2: System.out.println(qix);break; default: System.out.println(baz);} }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(5),
      block(
        element(Tree.Kind.IDENTIFIER, "qix"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.METHOD_INVOCATION)
        ).terminator(Tree.Kind.BREAK_STATEMENT).successors(1),
      emptyBlock(3),
      block(
        element(Tree.Kind.IDENTIFIER, "baz"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(1),
      block(
        element(Tree.Kind.VARIABLE, "a"),
        element(Tree.Kind.IDENTIFIER, "foo")
        ).terminator(Tree.Kind.SWITCH_STATEMENT).successors(3,5,6),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void return_statement() throws Exception {
    final CFG cfg = buildCFG("void fun(Object foo) { if(foo == null) return; }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.NULL_LITERAL),
        element(Tree.Kind.EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(1, 3),
      emptyBlock(0).terminator(Tree.Kind.RETURN_STATEMENT),
      emptyBlock(1),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void array_loop() throws Exception {
    final CFG cfg = buildCFG("void fun(Object foo) {System.out.println(''); for(int i =0;i<10;i++) { System.out.println(i); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.CHAR_LITERAL, "''"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.METHOD_INVOCATION),
        element(Tree.Kind.INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(4),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.FOR_STATEMENT).successors(1,3),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(2),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT)
        ).successors(4),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void array_loop_with_break() throws Exception {
    final CFG cfg = buildCFG("void fun(Object foo) { for(int i =0;i<10;i++) { if(i == 5) break; } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(7),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.FOR_STATEMENT).successors(1,6),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 5),
        element(Tree.Kind.EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(3,5),
      emptyBlock(1).terminator(Tree.Kind.BREAK_STATEMENT),
      emptyBlock(3),
      emptyBlock(2),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT)
        ).successors(7),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void array_loop_with_continue() throws Exception {
    final CFG cfg = buildCFG("void fun(Object foo) { for(int i =0;i<10;i++) { if(i == 5) continue; } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(7),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.FOR_STATEMENT).successors(1,6),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 5),
        element(Tree.Kind.EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(3,5),
      emptyBlock(2).terminator(Tree.Kind.CONTINUE_STATEMENT),
      emptyBlock(3),
      emptyBlock(2),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT)
        ).successors(7),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void foreach_loop() throws Exception {
    final CFG cfg = buildCFG("void fun(){ System.out.println(''); for(String foo:list) {System.out.println(foo);} System.out.println(''); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.CHAR_LITERAL, "''"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(3),
      block(
        element(Tree.Kind.IDENTIFIER, "list"),
        element(Tree.Kind.VARIABLE, "foo")
        ).terminator(Tree.Kind.FOR_EACH_STATEMENT).successors(1,2),
      block(
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(3),
      block(
        element(Tree.Kind.CHAR_LITERAL, "''"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void while_loop() throws Exception {
    final CFG cfg = buildCFG("void fun() {int i = 0; while(i < 10) {i++; System.out.println(i); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(4),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.WHILE_STATEMENT).successors(1,3),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(2),
      emptyBlock(4),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void while_loop_with_break() throws Exception {
    final CFG cfg = buildCFG("void fun() {int i = 0; while(i < 10) {i++; if(i == 5) break; } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(7),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.WHILE_STATEMENT).successors(1,6),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 5),
        element(Tree.Kind.EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(3,5),
      emptyBlock(1).terminator(Tree.Kind.BREAK_STATEMENT),
      emptyBlock(3),
      emptyBlock(2),
      emptyBlock(7),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void while_loop_with_continue() throws Exception {
    final CFG cfg = buildCFG("void fun() {int i = 0; while(i < 10) {i++; if(i == 5) continue; } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(7),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.WHILE_STATEMENT).successors(1,6),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 5),
        element(Tree.Kind.EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(3,5),
      emptyBlock(2).terminator(Tree.Kind.CONTINUE_STATEMENT),
      emptyBlock(3),
      emptyBlock(2),
      emptyBlock(7),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void do_while_loop() throws Exception {
    final CFG cfg = buildCFG("void fun() {int i = 0; do {i++; System.out.println(i); }while(i < 10); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(4),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(3),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.DO_STATEMENT).successors(1,2),
      emptyBlock(4),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void do_while_loop_with_break() throws Exception {
    final CFG cfg = buildCFG("void fun() {int i = 0; do { i++; if(i == 5) break; }while(i < 10); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(7),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 5),
        element(Tree.Kind.EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(4,6),
      emptyBlock(1).terminator(Tree.Kind.BREAK_STATEMENT),
      emptyBlock(4),
      emptyBlock(3),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.DO_STATEMENT).successors(1,2),
      emptyBlock(7),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void do_while_loop_with_continue() throws Exception {
    final CFG cfg = buildCFG("void fun() {int i = 0; do{i++; if(i == 5) continue; }while(i < 10); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(7),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 5),
        element(Tree.Kind.EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(4,6),
      emptyBlock(2).terminator(Tree.Kind.CONTINUE_STATEMENT),
      emptyBlock(4),
      emptyBlock(3),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.DO_STATEMENT).successors(1,2),
      emptyBlock(7),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void break_on_label() throws Exception {
    final CFG cfg = buildCFG("void fun() { foo: for(int i = 0; i<10;i++) { if(i==5) break foo; } }");
    final CFGChecker cfgChecker = checker(
      emptyBlock(8),
      block(
        element(Tree.Kind.INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(7),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.FOR_STATEMENT).successors(1,6),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 5),
        element(Tree.Kind.EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(3,5),
      emptyBlock(9).terminator(Tree.Kind.BREAK_STATEMENT),
      emptyBlock(3),
      emptyBlock(2),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT)
        ).successors(7),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void continue_on_label() throws Exception {
    final CFG cfg = buildCFG("void fun() { foo: for(int i = 0; i<10;i++) { if(i==5) continue foo; } }");
    final CFGChecker cfgChecker = checker(
      emptyBlock(8),
      block(
        element(Tree.Kind.INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(7),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.FOR_STATEMENT).successors(1,6),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.INT_LITERAL, 5),
        element(Tree.Kind.EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(3,5),
      emptyBlock(9).terminator(Tree.Kind.CONTINUE_STATEMENT),
      emptyBlock(3),
      emptyBlock(2),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT)
        ).successors(7),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void prefix_operators() throws Exception {
    final CFG cfg = buildCFG("void fun() { ++i;i++; }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.PREFIX_INCREMENT),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void try_statement() throws Exception {
    final CFG cfg = buildCFG("void fun() {try {System.out.println('');} finally { System.out.println(''); }}");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.TRY_STATEMENT)
        ).successors(3),
      block(
        element(Tree.Kind.CHAR_LITERAL, "''"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(2),
      block(
        element(Tree.Kind.CHAR_LITERAL, "''"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(1),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void throw_statement() throws Exception {
    final CFG cfg = buildCFG("void fun(Object a) {if(a==null) { throw new Exception();} System.out.println(''); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a"),
        element(Tree.Kind.NULL_LITERAL),
        element(Tree.Kind.EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(1,3),
      block(
        element(Tree.Kind.NEW_CLASS)
        ).terminator(Tree.Kind.THROW_STATEMENT).successors(0),
      emptyBlock(1),
      block(
        element(Tree.Kind.CHAR_LITERAL, "''"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void synchronized_statement() throws Exception {
    final CFG cfg = buildCFG("void fun(Object a) {if(a==null) { synchronized(a) { foo();bar();} } System.out.println(''); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a"),
        element(Tree.Kind.NULL_LITERAL),
        element(Tree.Kind.EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(1,2),
      block(
        element(Tree.Kind.IDENTIFIER, "a"),
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.METHOD_INVOCATION),
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(1),
      block(
        element(Tree.Kind.CHAR_LITERAL, "''"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void multiple_constructions() throws Exception {
    final CFG cfg = buildCFG("void fun(Object a) {if(a instanceof String) { a::toString;foo(y -> y+1); a += (String) a;  } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a"),
        element(Tree.Kind.INSTANCE_OF)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(1,2),
      block(
        element(Tree.Kind.LAMBDA_EXPRESSION),
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.METHOD_INVOCATION),
        element(Tree.Kind.IDENTIFIER, "a"),
        element(Tree.Kind.TYPE_CAST),
        element(Tree.Kind.IDENTIFIER, "a"),
        element(Tree.Kind.PLUS_ASSIGNMENT)
        ).successors(1),
      emptyBlock(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void array_access_expression() throws Exception {
    final CFG cfg = buildCFG("void fun(int[] array) { array[0] = 1; array[3+2] = 4; }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.INT_LITERAL, 1),
        element(Tree.Kind.INT_LITERAL, 0),
        element(Tree.Kind.IDENTIFIER, "array"),
        element(Tree.Kind.ARRAY_ACCESS_EXPRESSION),
        element(Tree.Kind.ASSIGNMENT),
        element(Tree.Kind.INT_LITERAL, 4),
        element(Tree.Kind.INT_LITERAL, 3),
        element(Tree.Kind.INT_LITERAL, 2),
        element(Tree.Kind.PLUS),
        element(Tree.Kind.IDENTIFIER, "array"),
        element(Tree.Kind.ARRAY_ACCESS_EXPRESSION),
        element(Tree.Kind.ASSIGNMENT)
        ).successors(0));
    cfgChecker.check(cfg);
  }

}
