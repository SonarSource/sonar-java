/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.cfg;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.testing.ThreadLocalLogTester;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.java.cfg.CFGTestUtils.buildCFG;
import static org.sonar.java.cfg.CFGTestUtils.buildCFGFromLambda;
import static org.sonar.plugins.java.api.tree.Tree.Kind.ARRAY_ACCESS_EXPRESSION;
import static org.sonar.plugins.java.api.tree.Tree.Kind.ASSERT_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.ASSIGNMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.BOOLEAN_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.BREAK_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CASE_GROUP;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CHAR_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CONDITIONAL_AND;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CONDITIONAL_EXPRESSION;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CONDITIONAL_OR;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CONTINUE_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.DEFAULT_PATTERN;
import static org.sonar.plugins.java.api.tree.Tree.Kind.DO_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.EQUAL_TO;
import static org.sonar.plugins.java.api.tree.Tree.Kind.FOR_EACH_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.FOR_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.GREATER_THAN;
import static org.sonar.plugins.java.api.tree.Tree.Kind.GUARDED_PATTERN;
import static org.sonar.plugins.java.api.tree.Tree.Kind.IDENTIFIER;
import static org.sonar.plugins.java.api.tree.Tree.Kind.IF_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.INSTANCE_OF;
import static org.sonar.plugins.java.api.tree.Tree.Kind.INT_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.LAMBDA_EXPRESSION;
import static org.sonar.plugins.java.api.tree.Tree.Kind.LESS_THAN;
import static org.sonar.plugins.java.api.tree.Tree.Kind.LOGICAL_COMPLEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.MEMBER_SELECT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.METHOD_INVOCATION;
import static org.sonar.plugins.java.api.tree.Tree.Kind.METHOD_REFERENCE;
import static org.sonar.plugins.java.api.tree.Tree.Kind.MULTIPLY_ASSIGNMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.NEW_ARRAY;
import static org.sonar.plugins.java.api.tree.Tree.Kind.NEW_CLASS;
import static org.sonar.plugins.java.api.tree.Tree.Kind.NULL_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.NULL_PATTERN;
import static org.sonar.plugins.java.api.tree.Tree.Kind.PATTERN_INSTANCE_OF;
import static org.sonar.plugins.java.api.tree.Tree.Kind.PLUS;
import static org.sonar.plugins.java.api.tree.Tree.Kind.PLUS_ASSIGNMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.POSTFIX_INCREMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.PREFIX_INCREMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.RECORD;
import static org.sonar.plugins.java.api.tree.Tree.Kind.RECORD_PATTERN;
import static org.sonar.plugins.java.api.tree.Tree.Kind.RETURN_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.STRING_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.SWITCH_EXPRESSION;
import static org.sonar.plugins.java.api.tree.Tree.Kind.SWITCH_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.SYNCHRONIZED_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.THROW_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.TRY_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.TYPE_CAST;
import static org.sonar.plugins.java.api.tree.Tree.Kind.TYPE_PATTERN;
import static org.sonar.plugins.java.api.tree.Tree.Kind.UNARY_MINUS;
import static org.sonar.plugins.java.api.tree.Tree.Kind.UNBOUNDED_WILDCARD;
import static org.sonar.plugins.java.api.tree.Tree.Kind.VARIABLE;
import static org.sonar.plugins.java.api.tree.Tree.Kind.WHILE_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.YIELD_STATEMENT;

class CFGTest {

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester().setLevel(Level.DEBUG);

  static CFGChecker checker(BlockChecker... checkers) {
    return new CFGChecker(checkers);
  }

  static BlockChecker block(final ElementChecker... checkers) {
    return new BlockChecker(checkers);
  }

  static BlockChecker terminator(final Tree.Kind kind, final int... successorIDs) {
    return new BlockChecker(kind, successorIDs);
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

  private static class CFGChecker {

    private final List<BlockChecker> checkers = new ArrayList<>();

    CFGChecker(BlockChecker... checkers) {
      Collections.addAll(this.checkers, checkers);
    }

    public void check(final CFG cfg) {
      try {
        assertThat(cfg.blocks()).as("Expected number of blocks").hasSize(checkers.size() + 1);
        final Iterator<BlockChecker> checkerIterator = checkers.iterator();
        final List<Block> blocks = new ArrayList<>(cfg.blocks());
        final Block exitBlock = blocks.remove(blocks.size() - 1);
        for (final Block block : blocks) {
          checkerIterator.next().check(block);
          checkLinkedBlocks(block.id(), "Successor", cfg.blocks(), block.successors());
          checkLinkedBlocks(block.id(), "Predecessors", cfg.blocks(), block.predecessors());
        }
        assertThat(exitBlock.elements()).isEmpty();
        assertThat(exitBlock.successors()).isEmpty();
        assertThat(cfg.blocks()).as("CFG entry block is no longer in the list of blocks!").contains(cfg.entryBlock());
      } catch (final Throwable e) {
        System.out.println(CFGDebug.toString(cfg));
        throw e;
      }
    }

    private void checkLinkedBlocks(int id, String type, List<Block> blocks, Set<Block> linkedBlocks) {
      for (Block block : linkedBlocks) {
        assertThat(block).as(type + " block " + id + " is missing from he list of blocks").isIn(blocks);
      }
    }
  }

  private static class BlockChecker {

    private int[] successorIDs = new int[] {};
    private int[] exceptionsIDs = new int[] {};
    private final List<ElementChecker> checkers = new ArrayList<>();
    private TerminatorChecker terminatorChecker;
    private int ifTrue = -1;
    private int ifFalse = -1;
    private int exitId = -1;
    private Integer successorWithoutJump = null;
    private boolean hasNoExit = false;
    private boolean isCatchBlock = false;
    private boolean isFinallyBlock = false;
    private boolean withCaseGroup = false;

    BlockChecker(final int... ids) {
      if( ids.length <= 1) {
        throw new IllegalArgumentException("creating a block with only one successors should not be possible!");
      }
      successors(ids);
    }

    BlockChecker(final Tree.Kind kind, final int... ids) {
      successors(ids);
      terminator(kind);
    }

    BlockChecker(final ElementChecker... checkers) {
      Collections.addAll(this.checkers, checkers);
      if (this.checkers.isEmpty()) {
        throw new IllegalArgumentException("Only terminator may have no elements!");
      }
    }

    BlockChecker successors(final int... ids) {
      if (ifTrue != -1 || ifFalse != -1) {
        throw new IllegalArgumentException("Cannot mix true/false with generic successors!");
      }
      successorIDs = new int[ids.length];
      int n = 0;
      for (int i : ids) {
        successorIDs[n++] = i;
      }
      Arrays.sort(successorIDs);
      return this;
    }

    BlockChecker successorWithoutJump(final int id) {
      this.successorWithoutJump = id;
      return this;
    }

    BlockChecker exceptions(final int... ids) {
      exceptionsIDs = new int[ids.length];
      int n = 0;
      for (int i : ids) {
        exceptionsIDs[n++] = i;
      }
      Arrays.sort(exceptionsIDs);
      return this;
    }

    BlockChecker isCatchBlock() {
      isCatchBlock = true;
      return this;
    }

    BlockChecker isFinallyBlock() {
      isFinallyBlock = true;
      return this;
    }

    BlockChecker ifTrue(final int id) {
      if (successorIDs.length > 0) {
        throw new IllegalArgumentException("Cannot mix true/false with generic successors!");
      }
      ifTrue = id;
      return this;
    }

    BlockChecker ifFalse(final int id) {
      if (successorIDs.length > 0) {
        throw new IllegalArgumentException("Cannot mix true/false with generic successors!");
      }
      ifFalse = id;
      return this;
    }

    BlockChecker terminator(final Tree.Kind kind) {
      this.terminatorChecker = new TerminatorChecker(kind);
      return this;
    }

    BlockChecker hasCaseGroup() {
      this.withCaseGroup = true;
      return this;
    }

    public void check(final Block block) {
      assertThat(block.elements()).as("Expected number of elements in block " + block.id()).hasSize(checkers.size());
      final Iterator<ElementChecker> checkerIterator = checkers.iterator();
      for (final Tree element : block.elements()) {
        checkerIterator.next().check(element);
      }
      if (successorIDs.length == 0) {
        if (ifTrue != -1) {
          assertThat(block.trueBlock().id()).as("Expected true successor block " + block.id()).isEqualTo(ifTrue);
        }
        if (ifFalse != -1) {
          assertThat(block.falseBlock().id()).as("Expected true successor block " + block.id()).isEqualTo(ifFalse);
        }
        if(exitId != -1) {
          assertThat(block.exitBlock().id()).as("Expected exit successor block " + block.id()).isEqualTo(exitId);
        }
      } else {
        assertThat(block.successors()).as("Expected number of successors in block " + block.id()).hasSize(successorIDs.length);
        final int[] actualSuccessorIDs = new int[successorIDs.length];
        int n = 0;
        for (final Block successor : block.successors()) {
          actualSuccessorIDs[n++] = successor.id();
        }
        Arrays.sort(actualSuccessorIDs);
        assertThat(actualSuccessorIDs).as("Expected successors in block " + block.id()).isEqualTo(successorIDs);
      }
      assertThat(block.exceptions().stream().mapToInt(Block::id).sorted().toArray()).as("Expected exceptions in block " + block.id()).isEqualTo(exceptionsIDs);
      if (terminatorChecker != null) {
        terminatorChecker.check(block.terminator());
      }
      if (withCaseGroup) {
        assertThat(block.caseGroup()).as("Block case group").isNotNull();
      } else {
        assertThat(block.caseGroup()).as("Block case group").isNull();
      }
      if (isCatchBlock) {
        assertThat(block.isCatchBlock()).as("Block B" + block.id() + " expected to be flagged as 'catch' block").isTrue();
      }
      if (isFinallyBlock) {
        assertThat(block.isFinallyBlock()).as("Block B" + block.id() + " expected to be flagged as 'finally' block").isTrue();
      }
      if(hasNoExit) {
        assertThat(block.exitBlock()).as("Block B" + block.id() + " has an unexpected exit block").isNull();
      }
      if (successorWithoutJump != null) {
        assertThat(block.successorWithoutJump()).isNotNull();
        assertThat(block.successorWithoutJump().id()).isEqualTo(successorWithoutJump);
      }
    }

    BlockChecker exit(final int id) {
      exitId = id;
      return this;
    }

    BlockChecker hasNoExitBlock() {
      hasNoExit = true;
      return this;
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
        case VARIABLE,
          CLASS,
          ENUM,
          INTERFACE,
          RECORD,
          ANNOTATION_TYPE,
          IDENTIFIER,
          CHAR_LITERAL,
          STRING_LITERAL,
          BOOLEAN_LITERAL,
          INT_LITERAL,
          METHOD_INVOCATION:
          break;
        default:
          throw new IllegalArgumentException("Unsupported element kind! "+kind);
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
        case METHOD_INVOCATION,
          ASSERT_STATEMENT,
          METHOD_REFERENCE,
          MEMBER_SELECT,
          NULL_LITERAL,
          EQUAL_TO,
          NOT_EQUAL_TO,
          LESS_THAN,
          LESS_THAN_OR_EQUAL_TO,
          GREATER_THAN,
          GREATER_THAN_OR_EQUAL_TO,
          POSTFIX_INCREMENT,
          PREFIX_INCREMENT,
          POSTFIX_DECREMENT,
          PREFIX_DECREMENT,
          TRY_STATEMENT,
          NEW_CLASS,
          NEW_ARRAY,
          INSTANCE_OF,
          PATTERN_INSTANCE_OF,
          SWITCH_EXPRESSION,
          LAMBDA_EXPRESSION,
          TYPE_CAST,
          PLUS_ASSIGNMENT,
          ASSIGNMENT,
          ARRAY_ACCESS_EXPRESSION,
          LOGICAL_COMPLEMENT,
          MULTIPLY_ASSIGNMENT,
          UNARY_MINUS,
          PLUS,
          CASE_GROUP,
          NULL_PATTERN,
          TYPE_PATTERN,
          GUARDED_PATTERN,
          DEFAULT_PATTERN,
          RECORD_PATTERN,
          UNBOUNDED_WILDCARD:
          break;
        default:
          throw new IllegalArgumentException("Unsupported element kind: " + kind);
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
          assertThat(((LiteralTree) element).token().text()).as("Character").isEqualTo(name);
          break;
        case STRING_LITERAL:
          String value = LiteralUtils.trimQuotes(((LiteralTree) element).token().text());
          assertThat(value).as("String").isEqualTo(name);
          break;
        case BOOLEAN_LITERAL:
          assertThat(((LiteralTree) element).token().text()).as("Boolean").isEqualTo(name);
          break;
        case METHOD_INVOCATION:
          if (name != null) {
            MethodInvocationTree method = (MethodInvocationTree) element;
            MemberSelectExpressionTree select = (MemberSelectExpressionTree) method.methodSelect();
            assertThat(select.identifier()).as("Method").hasToString(name);
          }
          break;
        case CLASS,
          ENUM,
          INTERFACE,
          RECORD,
          ANNOTATION_TYPE:
          assertThat(((ClassTree) element).simpleName().name()).as("Type name").isEqualTo(name);
          break;
        default:
          // No need to test any associated symbol for the other cases
          break;
      }
    }
  }

  private static class TerminatorChecker {

    private final Tree.Kind kind;

    private TerminatorChecker(final Tree.Kind kind) {
      this.kind = kind;
      switch (kind) {
        case IF_STATEMENT,
          CONDITIONAL_OR,
          CONDITIONAL_AND,
          CONDITIONAL_EXPRESSION,
          BREAK_STATEMENT,
          YIELD_STATEMENT,
          CONTINUE_STATEMENT,
          SWITCH_STATEMENT,
          SWITCH_EXPRESSION,
          RETURN_STATEMENT,
          FOR_STATEMENT,
          FOR_EACH_STATEMENT,
          WHILE_STATEMENT,
          DO_STATEMENT,
          THROW_STATEMENT,
          SYNCHRONIZED_STATEMENT:
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

  @Test
  void empty_cfg() {
    final CFG cfg = buildCFG("void fun() {}");
    final CFGChecker cfgChecker = checker();
    cfgChecker.check(cfg);
    assertThat(cfg.entryBlock().isMethodExitBlock()).as("entry is an exit").isTrue();
  }

  @Test
  void simplest_cfg() {
    final CFG cfg = buildCFG("void fun() { bar();}");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
    CFG.Block entry = cfg.entryBlock();
    assertThat(entry.isMethodExitBlock()).as("1st block is not an exit").isFalse();
    assertThat(entry.successors()).as("number of successors").hasSize(1);
    CFG.Block exit = entry.successors().iterator().next();
    assertThat(exit.isMethodExitBlock()).as("2nd block is an exit").isTrue();
  }

  @Test
  void cfg_with_record() {
    final CFG cfg = buildCFG("void fun() { record R(int x) {} bar();}");
    final CFGChecker cfgChecker = checker(
      block(
        element(RECORD, "R"),
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
    CFG.Block entry = cfg.entryBlock();
    assertThat(entry.isMethodExitBlock()).as("1st block is not an exit").isFalse();
    assertThat(entry.successors()).as("number of successors").hasSize(1);
    CFG.Block exit = entry.successors().iterator().next();
    assertThat(exit.isMethodExitBlock()).as("2nd block is an exit").isTrue();
  }

  @Test
  void straight_method_calls() {
    final CFG cfg = buildCFG("void fun() { bar();qix();baz();}");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION),
        element(IDENTIFIER, "qix"),
        element(METHOD_INVOCATION),
        element(IDENTIFIER, "baz"),
        element(METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void single_declaration() {
    final CFG cfg = buildCFG("void fun() {Object o;}");
    final CFGChecker cfgChecker = checker(
      block(
        element(VARIABLE, "o")).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void if_then() {
    final CFG cfg = buildCFG("void fun() {if(a) { foo(); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "a")
        ).terminator(IF_STATEMENT).successors(0, 1),
      block(
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void if_then_else() {
    final CFG cfg = buildCFG("void fun() {if(a) { foo(); } else { bar(); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "a"))
      .terminator(IF_STATEMENT).successors(1, 2),
      block(
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)).successors(0),
      block(
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void if_then_elseif() {
    final CFG cfg = buildCFG("void fun() {\nif(a) {\n foo(); \n } else if(b) {\n bar();\n } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "a")
        ).terminator(IF_STATEMENT).successors(2, 3),
      block(
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)
        ).successors(0),
      block(
        element(IDENTIFIER, "b")
        ).terminator(IF_STATEMENT).successors(0, 1),
      block(
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void conditionalOR() {
    final CFG cfg = buildCFG("void fun() {if(a || b) { foo(); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "a")
        ).terminator(CONDITIONAL_OR).successors(1, 2),
      block(
        element(IDENTIFIER, "b")
        ).terminator(IF_STATEMENT).successors(0, 1),
      block(
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void conditionalAND() {
    final CFG cfg = buildCFG("void fun() {if((a && b)) { foo(); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "a")
        ).terminator(CONDITIONAL_AND).successors(0, 2),
      block(
        element(IDENTIFIER, "b")
        ).terminator(IF_STATEMENT).successors(0, 1),
      block(
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void assignmentAND() {
    final CFG cfg = buildCFG("void fun() {boolean bool = a && b;}");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "a")).terminator(CONDITIONAL_AND).successors(1, 2),
      block(
        element(IDENTIFIER, "b")).successors(1),
      block(
        element(VARIABLE, "bool")).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void ternary_operator() {
    final CFG cfg = buildCFG("void fun() { Object c = foo ? a : b; a.toString();}");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "foo")).terminator(CONDITIONAL_EXPRESSION).successors(2, 3),
      block(
        element(IDENTIFIER, "a")).successors(1),
      block(
        element(IDENTIFIER, "b")).successors(1),
      block(
        element(VARIABLE, "c"),
        element(IDENTIFIER, "a"),
        element(METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void switch_statement() {
    CFG cfg = buildCFG("""
      void foo(int i, int j, int k) {
          switch (i==-1 ? j:k) {
            default:;
          }
        }
      """);

    assertThat(cfg.blocks().get(0).id()).isEqualTo(5);

    cfg = buildCFG("""
      void fun(int foo) {
        int a;
        switch(foo) {
          case 1:
            System.out.println(bar);
          case 2:
            System.out.println(qix);
            break;
          default:
            System.out.println(baz);
        }
      }
      """);
    CFGChecker cfgChecker = checker(
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)
        ).hasCaseGroup().successors(3),
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(IDENTIFIER, "qix"),
        element(METHOD_INVOCATION)
        ).hasCaseGroup().terminator(BREAK_STATEMENT).successors(0),
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(IDENTIFIER, "baz"),
        element(METHOD_INVOCATION)
        ).hasCaseGroup().successors(0),
      block(
        element(VARIABLE, "a"),
        element(IDENTIFIER, "foo"),
        element(INT_LITERAL, 1),
        element(INT_LITERAL, 2)
        ).terminator(SWITCH_STATEMENT).successors(2, 3, 4));
    cfgChecker.check(cfg);
  }

  @Test
  void switch_statement_with_piledUpCases_againstDefault() {
    final CFG cfg = buildCFG("""
        void fun(int foo) {
          int a;
          switch (foo) {
            case 1:
              System.out.println(bar);
            case 2:
              System.out.println(qix);
              break;
            case 3:
            case 4:
            default:
              System.out.println(baz);
          }
        }
        """);
    final CFGChecker cfgChecker = checker(
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(3),
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(IDENTIFIER, "qix"),
        element(METHOD_INVOCATION)).terminator(BREAK_STATEMENT).hasCaseGroup().successors(0),
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(IDENTIFIER, "baz"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(0),
      block(
        element(VARIABLE, "a"),
        element(IDENTIFIER, "foo"),
        element(INT_LITERAL, 1),
        element(INT_LITERAL, 2),
        element(INT_LITERAL, 3),
        element(INT_LITERAL, 4)).terminator(SWITCH_STATEMENT).successors(2, 3, 4));
    cfgChecker.check(cfg);
  }

  @Test
  void switch_statement_without_default() {
    final CFG cfg = buildCFG("""
        void fun(int foo) {
          int a;
          switch (foo) {
            case 1:
              System.out.println(bar);
            case 2:
              System.out.println(qix);
              break;
          }
          Integer.toString(foo);
        }
      """);
    final CFGChecker cfgChecker = checker(
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(3),
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(IDENTIFIER, "qix"),
        element(METHOD_INVOCATION)).terminator(BREAK_STATEMENT).hasCaseGroup().successors(1),
      block(
        element(VARIABLE, "a"),
        element(IDENTIFIER, "foo"),
        element(INT_LITERAL, 1),
        element(INT_LITERAL, 2)).terminator(SWITCH_STATEMENT).successors(1, 3, 4),
      block(
        element(IDENTIFIER, "Integer"),
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  /**
   * Introduced with Java 12
   */
  @Test
  void switch_statement_without_fallthrough() {
    final CFG cfg = buildCFG("""
        void fun(int foo) throws Exception {
          int a;
          switch (foo) {
            case 1     -> {
              fun(bar1);
              fun(bar2);
            }
            case 2,3,4 -> fun(qix);
            case 5     -> fun(gul);
            case 6     -> throw new Exception("boom");
            default    -> fun(def);
          }
          Integer.toString(foo);
        }
      """);
    final CFGChecker cfgChecker = checker(
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "bar1"),
        element(METHOD_INVOCATION),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "bar2"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(1),
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "qix"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(1),
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "gul"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(1),
      block(
        element(CASE_GROUP),
        element(STRING_LITERAL, "boom"),
        element(NEW_CLASS)).hasCaseGroup().terminator(THROW_STATEMENT).successors(0),
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "def"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(1),
      block(
        element(VARIABLE, "a"),
        element(IDENTIFIER, "foo"),
        element(INT_LITERAL, 1),
        element(INT_LITERAL, 2),
        element(INT_LITERAL, 3),
        element(INT_LITERAL, 4),
        element(INT_LITERAL, 5),
        element(INT_LITERAL, 6)).terminator(SWITCH_STATEMENT).successors(3, 4, 5, 6, 7),
      block(
        element(IDENTIFIER, "Integer"),
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void switch_expression_without_default() {
    final CFG cfg = buildCFG("""
        int fun(MyEnum foo) {
          int a = switch (foo) {
            case BAR -> 1;
            case QIX -> 2;
          };
          return a;
        }
        enum MyEnum { BAR, QIX; }
      """);
    final CFGChecker cfgChecker = checker(
      block(
        element(CASE_GROUP),
        element(INT_LITERAL, 1)).hasCaseGroup().successors(1),
      block(
        element(CASE_GROUP),
        element(INT_LITERAL, 2)).hasCaseGroup().successors(1),
      block(
        element(IDENTIFIER, "foo"),
        element(IDENTIFIER, "BAR"),
        element(IDENTIFIER, "QIX")).terminator(SWITCH_EXPRESSION).successors(3, 4),
      block(
        element(VARIABLE, "a"),
        element(IDENTIFIER, "a")).terminator(RETURN_STATEMENT).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void switch_expression_without_fallthrough() {
    final CFG cfg = buildCFG("""
        int fun(int foo) throws Exception {
          int a = switch (foo) {
            case 1 -> fun(bar1) + fun(bar2);
            case 2, 3, 4 -> fun(qix);
            case 5 -> throw new Exception("boom");
            default -> fun(def);
          };
          return a;
        }
      """);
    final CFGChecker cfgChecker = checker(
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "bar1"),
        element(METHOD_INVOCATION),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "bar2"),
        element(METHOD_INVOCATION),
        element(PLUS)).hasCaseGroup().successors(1),
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "qix"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(1),
      block(
        element(CASE_GROUP),
        element(STRING_LITERAL, "boom"),
        element(NEW_CLASS)).hasCaseGroup().terminator(THROW_STATEMENT).successors(0),
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "def"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(1),
      block(
        element(IDENTIFIER, "foo"),
        element(INT_LITERAL, 1),
        element(INT_LITERAL, 2),
        element(INT_LITERAL, 3),
        element(INT_LITERAL, 4),
        element(INT_LITERAL, 5)).terminator(SWITCH_EXPRESSION).successors(3, 4, 5, 6),
      block(
        element(VARIABLE, "a"),
        element(IDENTIFIER, "a")).terminator(RETURN_STATEMENT).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void switch_expression_with_fallthrough() {
    final CFG cfg = buildCFG("""
        int fun(int foo) throws Exception {
          int a = switch (foo) {
            case 1:
              fun(bar);
            case 2:
            case 3:
            case 4:
              yield fun(bar1) + fun(bar2);
            case 5:
              throw new Exception("boom");
            case 6:
              yield foo;
            default:
              yield fun(def);
          };
          return a;
        }
      """);
    final CFGChecker cfgChecker = checker(
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(6),
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "bar1"),
        element(METHOD_INVOCATION),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "bar2"),
        element(METHOD_INVOCATION),
        element(PLUS)).hasCaseGroup().terminator(YIELD_STATEMENT).successors(1),
      block(
        element(CASE_GROUP),
        element(STRING_LITERAL, "boom"),
        element(NEW_CLASS)).hasCaseGroup().terminator(THROW_STATEMENT).successors(0),
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "foo")).hasCaseGroup().successors(1),
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "def"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(1),
      block(
        element(IDENTIFIER, "foo"),
        element(INT_LITERAL, 1),
        element(INT_LITERAL, 2),
        element(INT_LITERAL, 3),
        element(INT_LITERAL, 4),
        element(INT_LITERAL, 5),
        element(INT_LITERAL, 6)).terminator(SWITCH_EXPRESSION).successors(3, 4, 5, 6, 7),
      block(
        element(VARIABLE, "a"),
        element(IDENTIFIER, "a")).terminator(RETURN_STATEMENT).successors(0));
    cfgChecker.check(cfg);
  }

  // FIXME add tests for jdk 17
  @Test
  void switch_with_pattern() {
    final CFG cfg = buildCFG("  static int switch_array_default_null_pattern(Object o) {\n"
      + "    return switch (o) {\n"
      // array type pattern
      + "      case Object[] arr -> arr.length;\n"
      // guarded pattern
      + "      case Rectangle r when r.volume() > 42 -> -1;\n"
      // record pattern
      + "      case Triangle(int a, var b, int c) -> 0;\n"
      // default and null pattern
      + "      case null, default -> 42;\n"
      + "    };\n"
      + "  }\n"
      + " record Triangle(int a, int b, int c) {}\n");
    final CFGChecker cfgChecker = checker(
      block(
        element(CASE_GROUP),
        element(IDENTIFIER, "arr"),
        element(MEMBER_SELECT)).hasCaseGroup().terminator(YIELD_STATEMENT).successors(1),
      block(
        element(CASE_GROUP),
        element(INT_LITERAL, 1),
        element(UNARY_MINUS)).hasCaseGroup().terminator(YIELD_STATEMENT).successors(1),
      block(
        element(CASE_GROUP),
        element(INT_LITERAL, 0)).hasCaseGroup().terminator(YIELD_STATEMENT).successors(1),
      block(
        element(CASE_GROUP),
        element(INT_LITERAL, 42)).hasCaseGroup().terminator(YIELD_STATEMENT).successors(1),
      block(
        element(IDENTIFIER, "o"),
        element(TYPE_PATTERN),
        element(VARIABLE, "arr"),
        element(GUARDED_PATTERN),
        element(TYPE_PATTERN),
        element(VARIABLE, "r"),
        element(IDENTIFIER, "r"),
        element(METHOD_INVOCATION),
        element(INT_LITERAL, 42),
        element(GREATER_THAN),
        element(RECORD_PATTERN),
        element(IDENTIFIER, "Triangle"),
        element(TYPE_PATTERN),
        element(VARIABLE, "a"),
        element(TYPE_PATTERN),
        element(VARIABLE, "b"),
        element(TYPE_PATTERN),
        element(VARIABLE, "c"),
        element(NULL_PATTERN),
        element(NULL_LITERAL),
        element(DEFAULT_PATTERN)
      ).terminator(SWITCH_EXPRESSION).successors(3, 4, 5, 6),
      terminator(RETURN_STATEMENT).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void return_statement() {
    final CFG cfg = buildCFG("void fun(Object foo) { if(foo == null) return; }");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "foo"),
        element(NULL_LITERAL),
        element(EQUAL_TO)
        ).terminator(IF_STATEMENT).successors(0, 1),
      terminator(RETURN_STATEMENT, 0).successorWithoutJump(0));
    cfgChecker.check(cfg);
  }

  @Test
  void array_loop() {
    final CFG cfg = buildCFG("void fun(Object foo) {System.out.println('c'); for(int i =0;i<10;i++) { System.out.println(i); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(CHAR_LITERAL, "'c'"),
        element(METHOD_INVOCATION),
        element(INT_LITERAL, 0),
        element(VARIABLE, "i")
        ).successors(3),
      block(
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(LESS_THAN)
        ).terminator(FOR_STATEMENT).successors(0, 2),
      block(
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(IDENTIFIER, "i"),
        element(METHOD_INVOCATION)
        ).successors(1),
      block(
        element(IDENTIFIER, "i"),
        element(POSTFIX_INCREMENT)
        ).successors(3));
    cfgChecker.check(cfg);
  }

  @Test
  void array_loop_with_break() {
    final CFG cfg = buildCFG("void fun(Object foo) { for(int i =0;i<10;i++) { if(i == 5) break; } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(VARIABLE, "i")
        ).successors(4),
      block(
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(LESS_THAN)
        ).terminator(FOR_STATEMENT).successors(0, 3),
      block(
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(IF_STATEMENT).successors(1, 2),
      terminator(BREAK_STATEMENT, 0).successorWithoutJump(1),
      block(
        element(IDENTIFIER, "i"),
        element(POSTFIX_INCREMENT)
        ).successors(4));
    cfgChecker.check(cfg);
  }

  @Test
  void array_loop_with_continue() {
    final CFG cfg = buildCFG("void fun(Object foo) { for(int i =0;i<10;i++) { if(i == 5) continue; } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(VARIABLE, "i")
        ).successors(4),
      block(
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(LESS_THAN)
        ).terminator(FOR_STATEMENT).successors(0, 3),
      block(
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(IF_STATEMENT).successors(1, 2),
      terminator(CONTINUE_STATEMENT, 1).successorWithoutJump(1),
      block(
        element(IDENTIFIER, "i"),
        element(POSTFIX_INCREMENT)
        ).successors(4));
    cfgChecker.check(cfg);
  }

  @Test
  void foreach_loop_continue() {
    final CFG cfg = buildCFG("void fun(){ System.out.println(\"start\"); for(String foo:list) {System.out.println(foo); if(foo.length()> 2) {continue;}  System.out.println('c');} System.out.println(\"end\"); }");
    final CFGChecker cfgChecker = checker(
        block(
          element(IDENTIFIER, "System"),
          element(MEMBER_SELECT),
          element(STRING_LITERAL, "start"),
          element(METHOD_INVOCATION)).successors(6),
        block(
            element(IDENTIFIER, "list")).successors(2),
        block(
          element(IDENTIFIER, "System"),
          element(MEMBER_SELECT),
          element(IDENTIFIER, "foo"),
          element(METHOD_INVOCATION),
          element(IDENTIFIER, "foo"),
          element(METHOD_INVOCATION),
          element(INT_LITERAL, 2),
          element(GREATER_THAN)
        ).terminator(IF_STATEMENT).successors(3, 4),
        terminator(CONTINUE_STATEMENT).successors(2).successorWithoutJump(3),
        block(
          element(IDENTIFIER, "System"),
          element(MEMBER_SELECT),
          element(CHAR_LITERAL, "'c'"),
          element(METHOD_INVOCATION)).successors(2),
        block(
            element(VARIABLE, "foo")).terminator(FOR_EACH_STATEMENT).successors(1, 5),
        block(
          element(IDENTIFIER, "System"),
          element(MEMBER_SELECT),
          element(STRING_LITERAL, "end"),
          element(METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void foreach_loop() {
    CFG cfg = buildCFG("void fun(){ System.out.println('c'); for(String foo:list) {System.out.println(foo);} System.out.println('d'); }");
    CFGChecker cfgChecker = checker(
        block(
          element(IDENTIFIER, "System"),
          element(MEMBER_SELECT),
          element(CHAR_LITERAL, "'c'"),
          element(METHOD_INVOCATION)).successors(4),
        block(
            element(IDENTIFIER, "list")).successors(2),
        block(
          element(IDENTIFIER, "System"),
          element(MEMBER_SELECT),
          element(IDENTIFIER, "foo"),
          element(METHOD_INVOCATION)).successors(2),
        block(
            element(VARIABLE, "foo")).terminator(FOR_EACH_STATEMENT).successors(1, 3),
        block(
          element(IDENTIFIER, "System"),
          element(MEMBER_SELECT),
          element(CHAR_LITERAL, "'d'"),
          element(METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
    cfg = buildCFG("""
          void fun(){
            for (String n : dir.list(foo() ? "**" : "")) {
              if (s.isEmpty()) {
                relativePath = n;
              }
            }
          }
        """);
    cfgChecker = new CFGChecker(
        block(
          element(IDENTIFIER, "dir"),
          element(IDENTIFIER, "foo"),
          element(METHOD_INVOCATION)).terminator(CONDITIONAL_EXPRESSION).ifTrue(6).ifFalse(5),
        block(element(STRING_LITERAL, "**")).successors(4),
        block(element(STRING_LITERAL, "")).successors(4),
        block(
            element(METHOD_INVOCATION)).successors(1),
        block(
            element(IDENTIFIER, "s"),
            element(METHOD_INVOCATION)).terminator(IF_STATEMENT).ifTrue(2).ifFalse(1),
        block(
            element(IDENTIFIER, "n"),
            element(ASSIGNMENT)).successors(1),
        block(element(VARIABLE, "n")).terminator(FOR_EACH_STATEMENT).ifFalse(0).ifTrue(3)
        );
    cfgChecker.check(cfg);
  }

  @Test
  void while_loop() {
    final CFG cfg = buildCFG("void fun() {int i = 0; while(i < 10) {i++; System.out.println(i); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(VARIABLE, "i")
        ).successors(2),
      block(
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(LESS_THAN)
        ).terminator(WHILE_STATEMENT).successors(0, 1),
      block(
        element(IDENTIFIER, "i"),
        element(POSTFIX_INCREMENT),
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(IDENTIFIER, "i"),
        element(METHOD_INVOCATION)
        ).successors(2));
    cfgChecker.check(cfg);
  }

  @Test
  void while_loop_with_break() {
    final CFG cfg = buildCFG("void fun() {int i = 0; while(i < 10) {i++; if(i == 5) break; } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(VARIABLE, "i")
        ).successors(3),
      block(
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(LESS_THAN)
        ).terminator(WHILE_STATEMENT).successors(0, 2),
      block(
        element(IDENTIFIER, "i"),
        element(POSTFIX_INCREMENT),
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(IF_STATEMENT).successors(1, 3),
      terminator(BREAK_STATEMENT, 0).successorWithoutJump(3));
    cfgChecker.check(cfg);
  }

  @Test
  void while_loop_with_continue() {
    final CFG cfg = buildCFG("void fun() {int i = 0; while(i < 10) {i++; if(i == 5) continue; } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(VARIABLE, "i")
        ).successors(3),
      block(
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(LESS_THAN)
        ).terminator(WHILE_STATEMENT).successors(0, 2),
      block(
        element(IDENTIFIER, "i"),
        element(POSTFIX_INCREMENT),
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(IF_STATEMENT).successors(1, 3),
      terminator(CONTINUE_STATEMENT, 3).successorWithoutJump(3));
    cfgChecker.check(cfg);
  }

  @Test
  void continue_in_try_finally() {
    final CFG cfg = buildCFG("""
        void fun() {
          while (foo()) {
            try {
              bar("try");
              continue;
            } finally {
              qix("finally");
            }
          }
        }
        """);
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)
      ).terminator(WHILE_STATEMENT).successors(0, 4),
      block(element(TRY_STATEMENT)).successors(3),
      block(
        element(IDENTIFIER, "bar"),
        element(STRING_LITERAL, "try"),
        element(METHOD_INVOCATION)
      ).successors(2).exceptions(1),
      terminator(CONTINUE_STATEMENT, 1).hasNoExitBlock().successorWithoutJump(1),
      block(element(IDENTIFIER, "qix"),
        element(STRING_LITERAL, "finally"),
        element(METHOD_INVOCATION)
      ).successors(0, 5)
      );
    cfgChecker.check(cfg);
  }

  @Test
  void break_in_try_finally() {
    final CFG cfg = buildCFG("""
        void fun() {
          while (foo()) {
            try {
              bar("try");
              break;
            } finally {
              qix("finally");
            }
          }
        }
        """);
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)
      ).terminator(WHILE_STATEMENT).successors(0, 4),
      block(element(TRY_STATEMENT)).successors(3),
      block(
        element(IDENTIFIER, "bar"),
        element(STRING_LITERAL, "try"),
        element(METHOD_INVOCATION)
      ).successors(2).exceptions(1),
      terminator(BREAK_STATEMENT, 1).hasNoExitBlock().successorWithoutJump(1),
      block(element(IDENTIFIER, "qix"),
        element(STRING_LITERAL, "finally"),
        element(METHOD_INVOCATION)
      ).successors(0)
    );
    cfgChecker.check(cfg);
  }

  @Test
  void do_while_loop() {
    final CFG cfg = buildCFG("void fun() {int i = 0; do {i++; System.out.println(i); }while(i < 10); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(VARIABLE, "i")
        ).successors(2),
      block(
        element(IDENTIFIER, "i"),
        element(POSTFIX_INCREMENT),
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(IDENTIFIER, "i"),
        element(METHOD_INVOCATION)
        ).successors(1),
      block(
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(LESS_THAN)
        ).terminator(DO_STATEMENT).successors(0, 2));
    cfgChecker.check(cfg);
  }

  @Test
  void do_while_loop_with_break() {
    final CFG cfg = buildCFG("void fun() {int i = 0; do { i++; if(i == 5) break; }while(i < 10); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(VARIABLE, "i")
        ).successors(3),
      block(
        element(IDENTIFIER, "i"),
        element(POSTFIX_INCREMENT),
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(IF_STATEMENT).successors(1, 2),
      terminator(BREAK_STATEMENT, 0).successorWithoutJump(1),
      block(
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(LESS_THAN)
        ).terminator(DO_STATEMENT).successors(0, 3));
    cfgChecker.check(cfg);
  }

  @Test
  void do_while_loop_with_continue() {
    final CFG cfg = buildCFG("void fun() {int i = 0; do{i++; if(i == 5) continue; }while(i < 10); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(VARIABLE, "i")
        ).successors(3),
      block(
        element(IDENTIFIER, "i"),
        element(POSTFIX_INCREMENT),
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(IF_STATEMENT).successors(1, 2),
      terminator(CONTINUE_STATEMENT, 1).successorWithoutJump(1),
      block(
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(LESS_THAN)
        ).terminator(DO_STATEMENT).successors(0, 3));
    cfgChecker.check(cfg);
  }

  @Test
  void break_on_label() {
    final CFG cfg = buildCFG("""
        void fun() {
          foo: for (int i = 0; i < 10; i++) {
            if (i == 5)
              break foo;
          }
        }
      """);
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(VARIABLE, "i")
        ).successors(4),
      block(
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(LESS_THAN)
        ).terminator(FOR_STATEMENT).successors(0, 3),
      block(
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(IF_STATEMENT).successors(1, 2),
      terminator(BREAK_STATEMENT, 0).successorWithoutJump(1),
      block(
        element(IDENTIFIER, "i"),
        element(POSTFIX_INCREMENT)
        ).successors(4));
    cfgChecker.check(cfg);
  }

  @Test
  void continue_on_label() {
    final CFG cfg = buildCFG("""
      void fun() {
        foo: for (int i = 0; i < 10; i++) {
          plop();
          if (i == 5)
            continue foo;
          plop();
        }
      }
      """);
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(VARIABLE, "i")
        ).successors(5),
      block(
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(LESS_THAN)
        ).terminator(FOR_STATEMENT).successors(0, 4),
      block(
        element(IDENTIFIER, "plop"),
        element(METHOD_INVOCATION),
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(IF_STATEMENT).successors(2,3),
      terminator(CONTINUE_STATEMENT, 1).successorWithoutJump(2),
        block(
            element(IDENTIFIER, "plop"),
            element(METHOD_INVOCATION)
        ).successors(1),
      block(
        element(IDENTIFIER, "i"),
        element(POSTFIX_INCREMENT)
        ).successors(5));
    cfgChecker.check(cfg);
  }

  @Test
  void assignement_order_of_evaluation() {
    CFG cfg = buildCFG("""
        void foo() {
          int[] a = {4,4};
          int b = 1;
          a[b] = b = 0;
        }
      """);
    CFGChecker checker = checker(
      block(
        element(INT_LITERAL, 4),
        element(INT_LITERAL, 4),
        element(NEW_ARRAY),
        element(VARIABLE, "a"),
        element(INT_LITERAL, 1),
        element(VARIABLE, "b"),
        element(IDENTIFIER, "a"),
        element(IDENTIFIER, "b"),
        element(ARRAY_ACCESS_EXPRESSION),
        element(INT_LITERAL, 0),
        element(ASSIGNMENT),
        element(ASSIGNMENT)).successors(0));
    checker.check(cfg);
  }

  @Test
  void compound_assignment() {
    CFG cfg = buildCFG("""
      void foo() {
        myField *= 0;
      }
      int myField;
      """);

    CFGChecker checker = checker(
      block(
        element(IDENTIFIER, "myField"),
        element(INT_LITERAL, 0),
        element(MULTIPLY_ASSIGNMENT)
        ).successors(0));

    checker.check(cfg);
  }

  @Test
  void compound_assignment_member_select() {
    CFG cfg = buildCFG("""
      void foo() {
        this.myField *= 0;
      }
      int myField;
      """);

    CFGChecker checker = checker(
      block(
        element(IDENTIFIER, "this"),
        element(MEMBER_SELECT),
        element(INT_LITERAL, 0),
        element(MULTIPLY_ASSIGNMENT)
      ).successors(0));

    checker.check(cfg);
  }

  @Test
  void prefix_operators() {
    final CFG cfg = buildCFG("void fun() { ++i;i++; }");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "i"),
        element(PREFIX_INCREMENT),
        element(IDENTIFIER, "i"),
        element(POSTFIX_INCREMENT)).successors(0));
    cfgChecker.check(cfg);
  }
  @Test
  void exit_block_for_finally_with_if_statement() {
    CFG cfg = buildCFG("""
        void test(boolean fooCalled) {
          Object bar;
          try {
            bar = new Bar();
          } finally {
            if (fooCalled) {
              foo();
            }
          }
          bar.toString();
        }
      """);
    CFGChecker cfgChecker = checker(
      block(
        element(VARIABLE, "bar"),
        element(TRY_STATEMENT)
      ).successors(6),
      block(
        element(NEW_CLASS)
      ).successors(5).exceptions(4),
      block(
        element(ASSIGNMENT)
      ).successors(4),
      block(
        element(IDENTIFIER, "fooCalled")
      ).terminator(IF_STATEMENT).successors(2, 3),
      block(
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)
      ).successors(2),
      new BlockChecker(1, 0).exit(0),
      block(
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)
      ).successors(0)
    );
    cfgChecker.check(cfg);

  }

  @Test
  void catch_thrown_in_exception() {
    CFG cfg = buildCFG("""
        void  foo() throws MyException {
          try {
            try {
              foo();
            } catch (MyException e) {
              foo();
            }
          } catch (MyException e) {
            System.out.println("outercatch");
          }
        }
        class MyException extends Exception {}
      """);
    CFGChecker checker = checker(
      block(
        element(TRY_STATEMENT)
      ).successors(4),
      block(
        element(TRY_STATEMENT)
        ).successors(3),
      block(
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)
        ).successors(0).exceptions(0,2),
      block(
        element(VARIABLE, "e"),
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)
        ).successors(0).exceptions(0, 1),
      block(
        element(VARIABLE, "e"),
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(STRING_LITERAL, "outercatch"),
        element(METHOD_INVOCATION)
      ).successors(0).exceptions(0)
    );
    checker.check(cfg);
  }

  @Test
  void nested_try_finally() {

    CFG cfg = buildCFG("""
        void  foo() {
          try {
            java.util.zip.ZipFile file = new java.util.zip.ZipFile(fileName);
            try {
              file.foo();// do something with the file...
            } finally {
              file.close();
            }
          } catch (Exception e) {
            // Handle exception
          }
        }
      """);
    CFGChecker cfgChecker = checker(
      block(
        element(TRY_STATEMENT)
      ).successors(5),
      block(
        element(IDENTIFIER, "fileName"),
        element(NEW_CLASS)
      ).successors(4).exceptions(0,1),
      block(
        element(VARIABLE, "file"),
        element(TRY_STATEMENT)
      ).successors(3),
      block(
        element(IDENTIFIER, "file"),
        element(METHOD_INVOCATION)
      ).successors(2).exceptions(2),
      block(
        element(IDENTIFIER, "file"),
        element(METHOD_INVOCATION)
      ).successors(0).exceptions(0,1),
      block(
        element(VARIABLE, "e")
      )
      );
    cfgChecker.check(cfg);

  }

  @Test
  void catch_throwable() {
    CFG cfg = buildCFG("""
       public void reschedule() {
         try {
           getNextSchedule();
         } catch (Throwable t) {
           notifyFailed();
         }
       }
      """);
      CFGChecker cfgChecker = checker(
        block(
          element(TRY_STATEMENT)
        ).successors(2),
        block(
          element(IDENTIFIER, "getNextSchedule"),
          element(METHOD_INVOCATION)
          ).successors(0).exceptions(0, 1),
        block(
          element(VARIABLE, "t"),
          element(IDENTIFIER, "notifyFailed"),
          element(METHOD_INVOCATION)
        ).successors(0).exceptions(0)
      );
    cfgChecker.check(cfg);
  }

  @Test
  void catch_error() {
    CFG cfg = buildCFG("""
       public void foo() {
          try {
            doSomething();
          } catch (Error e) {
            throw e;
          }
        }
      """);
    CFGChecker cfgChecker = checker(
      block(
        element(TRY_STATEMENT)).successors(2),
      block(
        element(IDENTIFIER, "doSomething"),
        element(METHOD_INVOCATION)).successors(0).exceptions(0, 1),
      block(
        element(VARIABLE, "e"),
        element(IDENTIFIER, "e")).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void try_statement() {
    CFG cfg = buildCFG("void fun() {try {System.out.println('c');} finally { System.out.println('c'); }}");
    CFGChecker cfgChecker = checker(
        block(
            element(TRY_STATEMENT)
        ).successors(2),
        block(
          element(IDENTIFIER, "System"),
          element(MEMBER_SELECT),
          element(CHAR_LITERAL, "'c'"),
          element(METHOD_INVOCATION)
        ).successors(1).exceptions(1),
        block(
          element(IDENTIFIER, "System"),
          element(MEMBER_SELECT),
          element(CHAR_LITERAL, "'c'"),
          element(METHOD_INVOCATION)
      ).successors(0).isFinallyBlock());
    cfgChecker.check(cfg);
    cfg = buildCFG("void fun() {try {System.out.println('c');} catch(IllegalArgumentException e) { foo('i');} catch(Exception e){bar('e');}" +
        " finally { System.out.println(\"finally\"); }}");
    cfgChecker = checker(
        block(
            element(TRY_STATEMENT)
        ).successors(4),
      block(
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(CHAR_LITERAL, "'c'"),
        element(METHOD_INVOCATION)
      ).successors(1).exceptions(1, 2, 3),
      block(
        element(VARIABLE, "e"),
        element(IDENTIFIER, "foo"),
        element(CHAR_LITERAL, "'i'"),
        element(METHOD_INVOCATION)
      ).successors(1).exceptions(1).isCatchBlock(),
      block(
        element(VARIABLE, "e"),
        element(IDENTIFIER, "bar"),
        element(CHAR_LITERAL, "'e'"),
        element(METHOD_INVOCATION)
      ).successors(1).exceptions(1).isCatchBlock(),
      block(
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(STRING_LITERAL, "finally"),
        element(METHOD_INVOCATION)
      ).successors(0).isFinallyBlock()
    );
    cfgChecker.check(cfg);
    cfg = buildCFG("""
        private void f() {
          try {
          } catch (Exception e) {
            if (e instanceof IOException) {\s
            }
          }
        }
        """);
    cfgChecker = checker(
        block(
            element(TRY_STATEMENT)
        ).successors(0),
        block(
          element(VARIABLE, "e"),
          element(IDENTIFIER, "e"),
            element(INSTANCE_OF)
      ).terminator(IF_STATEMENT).ifTrue(0).ifFalse(0).isCatchBlock()
    );
    cfgChecker.check(cfg);
    cfg = buildCFG("""
          private void f() {
            try {
              return;
            } finally {
              foo();
            }
            bar();
          }
        """);
    cfgChecker = checker(
        block(
            element(TRY_STATEMENT)
        ).successors(3),
        terminator(RETURN_STATEMENT).successors(2).exit(2).successorWithoutJump(2),
        block(
            element(IDENTIFIER, "foo"),
            element(METHOD_INVOCATION)
      ).successors(0, 1).exit(0).isFinallyBlock(),
        block(
            element(IDENTIFIER, "bar"),
            element(METHOD_INVOCATION)
        ).successors(0)
    );
    cfgChecker.check(cfg);
  }

  @Test
  void throw_statement() {
    final CFG cfg = buildCFG("void fun(Object a) {if(a==null) { throw new Exception();} System.out.println('c'); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "a"),
        element(NULL_LITERAL),
        element(EQUAL_TO)
        ).terminator(IF_STATEMENT).successors(1, 2),
      block(
        element(NEW_CLASS)).terminator(THROW_STATEMENT).successors(0),
      block(
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(CHAR_LITERAL, "'c'"),
        element(METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void synchronized_statement() {
    final CFG cfg = buildCFG("void fun(Object a) {if(a==null) { synchronized(a) { foo();bar();} } System.out.println('c'); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "a"),
        element(NULL_LITERAL),
        element(EQUAL_TO)).terminator(IF_STATEMENT).successors(1, 3),
      block(
        element(IDENTIFIER, "a")).terminator(SYNCHRONIZED_STATEMENT).successors(2),
      block(
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION),
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)).successors(1),
      block(
        element(IDENTIFIER, "System"),
        element(MEMBER_SELECT),
        element(CHAR_LITERAL, "'c'"),
        element(METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void multiple_constructions() {
    final CFG cfg = buildCFG("void fun(Object a) {if(a instanceof String str) { Supplier<String> s = a::toString;foo(y -> y+1); a += (String) a;  } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "a"),
        element(TYPE_PATTERN),
        element(VARIABLE, "str"),
        element(PATTERN_INSTANCE_OF)
        ).terminator(IF_STATEMENT).successors(0, 1),
      block(
        element(METHOD_REFERENCE),
        element(VARIABLE, "s"),
        element(IDENTIFIER, "foo"),
        element(LAMBDA_EXPRESSION),
        element(METHOD_INVOCATION),
        element(IDENTIFIER, "a"),
        element(IDENTIFIER, "a"),
        element(TYPE_CAST),
        element(PLUS_ASSIGNMENT)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void catching_class_cast_exception() {
    CFG cfg = buildCFG("String fun(Object a) {try {return (String) a;} catch(ClassCastException cce) { return null;} }");
    CFGChecker cfgChecker = checker(
      block(
        element(TRY_STATEMENT)
      ),
      block(
        element(IDENTIFIER, "a"),
        element(TYPE_CAST)
      ).successors(1, 2),
      terminator(RETURN_STATEMENT, 0).successorWithoutJump(0),
      block(
        element(VARIABLE, "cce"),
        element(NULL_LITERAL)
      ).successors(0)
    );
    cfgChecker.check(cfg);

  }

  @Test
  void array_access_expression() {
    final CFG cfg = buildCFG("void fun(int[] array) { array[0] = 1; array[3+2] = 4; }");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "array"),
        element(INT_LITERAL, 0),
        element(ARRAY_ACCESS_EXPRESSION),
        element(INT_LITERAL, 1),
        element(ASSIGNMENT),
        element(IDENTIFIER, "array"),
        element(INT_LITERAL, 3),
        element(INT_LITERAL, 2),
        element(PLUS),
        element(ARRAY_ACCESS_EXPRESSION),
        element(INT_LITERAL, 4),
        element(ASSIGNMENT)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void try_with_resource() {
    final CFG cfg = buildCFG("void fun() { String path = \"\"; try (BufferedReader br = new BufferedReader(new FileReader(path))) {} }");
    final CFGChecker cfgChecker = checker(
      block(
        element(STRING_LITERAL, ""),
        element(VARIABLE, "path"),
        element(TRY_STATEMENT)).successors(3),
      block(
        element(IDENTIFIER, "path"),
        element(NEW_CLASS)).successors(2).exceptions(0),
      block(
          element(NEW_CLASS)).successors(1).exceptions(0),
      block(
        element(VARIABLE, "br")).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void try_with_resource_java9() {
    final CFG cfg = buildCFG("void fun() { final Resource r = new Resource(); try (r) {} }");
    final CFGChecker cfgChecker = checker(
      block(
        element(NEW_CLASS),
        element(VARIABLE, "r"),
        element(TRY_STATEMENT)).successors(1),
      block(
        element(IDENTIFIER, "r")).successors(0))
      ;
    cfgChecker.check(cfg);
  }

  @Test
  void returnCascadedAnd() {
    final CFG cfg = buildCFG(
      "boolean andAll(boolean a, boolean b, boolean c) { return a && b && c;}");
    final CFGChecker cfgChecker = checker(
      block(element(IDENTIFIER, "a")).terminator(CONDITIONAL_AND).ifTrue(4).ifFalse(3),
      block(element(IDENTIFIER, "b")).successors(3),
      terminator(CONDITIONAL_AND).ifTrue(2).ifFalse(1),
      block(element(IDENTIFIER, "c")).successors(1),
      terminator(RETURN_STATEMENT).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void returnCascadedOr() {
    final CFG cfg = buildCFG(
      "boolean orAll(boolean a, boolean b, boolean c) { return a || b || c;}");
    final CFGChecker cfgChecker = checker(
      block(element(IDENTIFIER, "a")).terminator(CONDITIONAL_OR).ifTrue(3).ifFalse(4),
      block(element(IDENTIFIER, "b")).successors(3),
      terminator(CONDITIONAL_OR).ifTrue(1).ifFalse(2),
      block(element(IDENTIFIER, "c")).successors(1),
      terminator(RETURN_STATEMENT).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void complex_boolean_expression() {
    final CFG cfg = buildCFG("""
        private boolean fun(boolean bool, boolean a, boolean b) {
          return (!bool && a) || (bool && b);
        }
      """);
    final CFGChecker cfgChecker = checker(
        block(
            element(IDENTIFIER, "bool"),
            element(LOGICAL_COMPLEMENT)
        ).terminator(CONDITIONAL_AND).ifTrue(5).ifFalse(4),
        block(element(IDENTIFIER, "a")).successors(4),
        terminator(CONDITIONAL_OR).ifTrue(1).ifFalse(3),
        block(element(IDENTIFIER, "bool")).terminator(CONDITIONAL_AND).ifTrue(2).ifFalse(1),
        block(element(IDENTIFIER, "b")).successors(1),
        terminator(RETURN_STATEMENT).successors(0));
    cfgChecker.check(cfg);

  }

  @Test
  void method_reference() {
    final CFG cfg = buildCFG("void fun() { foo(Object::toString); }");
    final CFGChecker cfgChecker = checker(
        block(
          element(IDENTIFIER, "foo"),
          element(METHOD_REFERENCE),
            element(METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void try_statement_with_CFG_blocks() {
    // method invocation after if
    CFG cfg = buildCFG("""
          private void f(boolean action) {
            try {
              if (action) {
                 performAction();
              }
              doSomething();
            } catch(Exception e) {
              foo();
            }
            bar();
          }
        """);
    CFGChecker cfgChecker = checker(
      block(
        element(TRY_STATEMENT)).successors(5),
      block(
        element(IDENTIFIER, "action")).terminator(IF_STATEMENT).successors(3, 4),
      block(
        element(IDENTIFIER, "performAction"),
        element(METHOD_INVOCATION)).successors(3).exceptions(0, 2).exit(0),
      block(
        element(IDENTIFIER, "doSomething"),
        element(METHOD_INVOCATION)).successors(1).exceptions(0, 2).exit(0),
      block(
        element(VARIABLE, "e"),
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)).successors(1).exceptions(0).exit(0),
      block(
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);

    // method invocation before if
    cfg = buildCFG("""
          private void f(boolean action) {
            try {
              doSomething();
              if (action) {
                performAction();
              }
            } catch(Exception e) {
              foo();
            }
            bar();
          }
        """);
    cfgChecker = checker(
      block(
        element(TRY_STATEMENT)).successors(5),
      block(
        element(IDENTIFIER, "doSomething"),
        element(METHOD_INVOCATION)).successors(4).exceptions(0, 2).exit(0),
      block(
        element(IDENTIFIER, "action")).terminator(IF_STATEMENT).successors(1, 3),
      block(
        element(IDENTIFIER, "performAction"),
        element(METHOD_INVOCATION)).successors(1).exceptions(0, 2),
      block(
        element(VARIABLE, "e"),
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)).successors(1).exceptions(0),
      block(
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);

    // finally
    cfg = buildCFG("""
          private void f(boolean action) {
            try {
              if (action) {
                performAction();
              }
              doSomething();
            } finally {
              foo();
            }
            bar();
          }
        """);
    cfgChecker = checker(
      block(
        element(TRY_STATEMENT)).successors(5),
      block(
        element(IDENTIFIER, "action")).terminator(IF_STATEMENT).successors(3, 4),
      block(
        element(IDENTIFIER, "performAction"),
        element(METHOD_INVOCATION)).successors(3).exceptions(2),
      block(
        element(IDENTIFIER, "doSomething"),
        element(METHOD_INVOCATION)).successors(2).exceptions(2),
      block(
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)).successors(0, 1),
      block(
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  void try_statement_with_checked_exceptions() {
    CFG cfg = buildCFG(
      " void foo(Object result) {" +
        "  try { " +
        "       result = new Plop();" +
        "   } catch(IllegalAccessException iae) {" +
        "        try{ " +
        "            result = new Plop();" +
        "        } catch(IllegalAccessException iae2) {" +
        "        }" +
        "      result.toString();   " +
        "    }" +
        "}" +
        "" +
        "class Plop{" +
        "   Plop() throws IllegalAccessException{}" +
        "}" );

    CFGChecker cfgChecker = checker(
      block(
        element(TRY_STATEMENT)).successors(7),
      block(
        element(NEW_CLASS)).successors(1).exceptions(0, 6),
      block(
        element(VARIABLE, "iae"),
        element(TRY_STATEMENT)).successors(5).isCatchBlock(),
      block(
        element(NEW_CLASS)).successors(3).exceptions(0, 4),
      block(
        element(VARIABLE, "iae2")
      ).successors(2),
      block(
        element(ASSIGNMENT)
      ).successors(2),
      block(
        element(IDENTIFIER, "result"),
        element(METHOD_INVOCATION)
        ).successors(0).exceptions(0),
      block(
        element(ASSIGNMENT)
      ).successors(0)
      );
    cfgChecker.check(cfg);
  }

  @Test
  void try_statement_with_runtime_exceptions() {
    CFG cfg = buildCFG(new File("src/test/files/cfg/CFGRuntimeExceptions.java"));
    CFGChecker cfgChecker = checker(
      block(
        element(TRY_STATEMENT)).successors(9),
      block(
        element(IDENTIFIER, "doSomething"),
        // trigger runtime exception -> branch to:
        // B8 : RuntimeException
        // B7 : Subtype of RuntimeException
        // B6 : Error
        // B5 : Subtype of Error
        // B4 : Throwable
        // B3 : Subtype of Throwable but not subtype of Exception
        // B2 : Exception
        element(METHOD_INVOCATION)).successors(0).exceptions(8, 7, 6, 5, 4, 3, 2, 0),
      block(
        element(VARIABLE, "re"),
        element(IDENTIFIER, "doSomethingElse"),
        element(METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock(),
      block(
        element(VARIABLE, "mre"),
        element(IDENTIFIER, "doSomethingElse"),
        element(METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock(),
      block(
        element(VARIABLE, "er"),
        element(IDENTIFIER, "doSomethingElse"),
        element(METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock(),
      block(
        element(VARIABLE, "mer"),
        element(IDENTIFIER, "doSomethingElse"),
        element(METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock(),
      block(
        element(VARIABLE, "t"),
        element(IDENTIFIER, "doSomethingElse"),
        element(METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock(),
      block(
        element(VARIABLE, "mt"),
        element(IDENTIFIER, "doSomethingElse"),
        element(METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock(),
      block(
        element(VARIABLE, "ex"),
        element(IDENTIFIER, "doSomethingElse"),
        element(METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock(),
      block(
        // no way to enter the block (checked Exception)
        element(VARIABLE, "mex"),
        element(IDENTIFIER, "doNothing"),
        element(METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock());
    cfgChecker.check(cfg);
  }

  @Test
  void catch_block_correctly_flagged_in_CFG() {
    CFG cfg = buildCFG(new File("src/test/files/cfg/CFGCatchBlocks.java"));

    CFGChecker cfgChecker = checker(
      block(
        element(TRY_STATEMENT)).successors(8),
      block(
        element(IDENTIFIER, "m1"),
        element(IDENTIFIER, "o1"),
        element(IDENTIFIER, "o2"),
        element(METHOD_INVOCATION)).successors(1).exceptions(7, 5, 3, 1),
      block(
        element(VARIABLE, "e"),
        element(IDENTIFIER, "m2"),
        element(METHOD_INVOCATION)).successors(6).exceptions(1).isCatchBlock(),
      block(
        element(IDENTIFIER, "m3"),
        element(METHOD_INVOCATION)).successors(1).exceptions(1),
      block(
        element(VARIABLE, "e"),
        element(IDENTIFIER, "o2"),
        element(NULL_LITERAL),
        element(EQUAL_TO)).terminator(IF_STATEMENT).ifTrue(4).ifFalse(1).isCatchBlock(),
      block(
        element(IDENTIFIER, "m4"),
        element(METHOD_INVOCATION)).successors(1).exceptions(1),
      block(
        element(VARIABLE, "e"),
        element(IDENTIFIER, "m5"),
        element(METHOD_INVOCATION)).successors(2).exceptions(1).isCatchBlock(),
      block(
        element(VARIABLE, "res")).successors(1),
      block(
        element(IDENTIFIER, "m6"),
        element(METHOD_INVOCATION)).successors(0).isFinallyBlock());
    cfgChecker.check(cfg);
  }

  @Test
  void successor_of_labeled_break_statement() {
    CFG cfg = buildCFG("""
        private static void test(long toRevision, boolean inverted, Object visitor) {
          testBlock: {
            if (inverted)\s
              break testBlock;
            test(0, false ? inverted : !inverted, visitor);
          }
        }
      """);
    CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "inverted")
      ).terminator(IF_STATEMENT)
        .ifTrue(5)
        .ifFalse(4),
      terminator(BREAK_STATEMENT).successors(0),
      block(
        element(IDENTIFIER, "test"),
        element(INT_LITERAL, 0),
        element(BOOLEAN_LITERAL, "false")
        ).terminator(CONDITIONAL_EXPRESSION)
        .ifTrue(3)
        .ifFalse(2),
      block(
        element(IDENTIFIER, "inverted")
      ).successors(1),
      block(
        element(IDENTIFIER, "inverted"),
        element(LOGICAL_COMPLEMENT)
      ).successors(1),
      block(
        element(IDENTIFIER, "visitor"),
        element(METHOD_INVOCATION)
      ).successors(0)
    );
    cfgChecker.check(cfg);

  }

  @Test
  void test_chained_method_invocation() {
    CFG cfg = buildCFG("""
        private void foo(Object p) {
          if(p == null) {
            NullArrayAccess
              .method(p.toString())
              .method2(p.hashCode());
          }
        }
      """);
    CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "p"),
        element(NULL_LITERAL),
        element(EQUAL_TO)
      ).terminator(IF_STATEMENT)
        .ifTrue(1)
        .ifFalse(0),
      block(
        element(IDENTIFIER, "NullArrayAccess"),
        element(IDENTIFIER, "p"),
        element(METHOD_INVOCATION),
        element(METHOD_INVOCATION),
        element(IDENTIFIER, "p"),
        element(METHOD_INVOCATION),
        element(METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }


  @Test
  void constructor_arguments_order() {
    CFG cfg = buildCFG("""
      private void foo(Exception e) {
      throw new IllegalArgumentException("iae", e);
      }\s"""
    );
    CFGChecker cfgChecker = checker(
      block(
        element(STRING_LITERAL, "iae"),
        element(IDENTIFIER, "e"),
        element(NEW_CLASS)
      ).terminator(THROW_STATEMENT).successors(0)
    );
    cfgChecker.check(cfg);
  }

  @Test
  void array_dim_initializer_order() {
    CFG cfg = buildCFG("""
      private void fun() {
      String[] plop = {foo(), bar()};
      String[][] plop2 = new String[qix()][baz()];
      }\s"""
    );
    CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION),
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION),
        element(NEW_ARRAY),
        element(VARIABLE, "plop"),
        element(IDENTIFIER, "qix"),
        element(METHOD_INVOCATION),
        element(IDENTIFIER, "baz"),
        element(METHOD_INVOCATION),
        element(NEW_ARRAY),
        element(VARIABLE, "plop2")
        ).successors(0)
    );
    cfgChecker.check(cfg);
  }

  @Test
  void assert_statement() {
    CFG cfg = buildCFG("""
      private void fun(boolean x) {
      assert x;
      }\s"""
    );
    CFGChecker cfgChecker = checker(
      block(element(IDENTIFIER, "x"),
        element(ASSERT_STATEMENT))
    );
    cfgChecker.check(cfg);

  }

  @Test
  void exception_raised_in_catch() {
    CFG cfg = buildCFG("""
      private void fun() {
           try {
            try {
              f();
            } catch (Exception e) {
              ex();
            } finally {
              fin();
            }
          } catch (Exception e) {
            outEx();
          }
      }\s"""
    );
    CFGChecker cfgChecker = checker(
      block(element(TRY_STATEMENT)),
      block(element(TRY_STATEMENT)),
      block(
        element(IDENTIFIER, "f"),
        element(METHOD_INVOCATION)
        ).exceptions(2,3),
      block(
        element(VARIABLE, "e"),
        element(IDENTIFIER, "ex"),
        element(METHOD_INVOCATION)
      ).exceptions(2),
      block(
        element(IDENTIFIER, "fin"),
        element(METHOD_INVOCATION)
      ).exceptions(0,1),
      block(
        element(VARIABLE, "e"),
        element(IDENTIFIER, "outEx"),
        element(METHOD_INVOCATION)
      ).exceptions(0)
    );
    cfgChecker.check(cfg);

  }

  @Test
  void break_in_nested_catch() {
    CFG cfg = buildCFG("""
          void foo(boolean a) {
            String[] types = new String[12];
            try {
              invoke0();
            for (int i = 0; i < files.length; i++) {
              A file = files[i];
              try{
                invoke1();
              }catch(Throwable e) {
                invoke2();
                invoke3();
                break;
              } finally {
                types[i] = invoke4();
              }
            }
            } finally {
              invoke10();
              invoke11();
            }
           \s
          }
        """);
    assertThat(CFGDebug.toString(cfg)).isEqualTo("""
      Starts at B13
      
      B13
      0:\tINT_LITERAL                         \t12
      1:\tNEW_ARRAY                           \tnew []
      2:\tVARIABLE                            \ttypes
      3:\tTRY_STATEMENT                       \t
      \tjumps to: B12
      
      B12
      0:\tIDENTIFIER                          \tinvoke0
      1:\tMETHOD_INVOCATION                   \tinvoke0()
      \tjumps to: B11
      \texceptions to: B1
      
      B11
      0:\tINT_LITERAL                         \t0
      1:\tVARIABLE                            \ti
      \tjumps to: B10
      
      B10
      0:\tIDENTIFIER                          \ti
      1:\tIDENTIFIER                          \tfiles
      2:\tMEMBER_SELECT                       \tfiles.length
      3:\tLESS_THAN                           \ti < files.length
      T:\tFOR_STATEMENT                       \tfor {i;i < files.length;i++}
      \tjumps to: B9(true) B1(false)
      
      B9
      0:\tIDENTIFIER                          \tfiles
      1:\tIDENTIFIER                          \ti
      2:\tARRAY_ACCESS_EXPRESSION             \tfiles[i]
      3:\tVARIABLE                            \tfile
      4:\tTRY_STATEMENT                       \t
      \tjumps to: B8
      
      B8
      0:\tIDENTIFIER                          \tinvoke1
      1:\tMETHOD_INVOCATION                   \tinvoke1()
      \tjumps to: B4
      \texceptions to: B4 B7
      
      B7
      0:\tVARIABLE                            \te
      1:\tIDENTIFIER                          \tinvoke2
      2:\tMETHOD_INVOCATION                   \tinvoke2()
      \tjumps to: B6
      \texceptions to: B4
      
      B6
      0:\tIDENTIFIER                          \tinvoke3
      1:\tMETHOD_INVOCATION                   \tinvoke3()
      \tjumps to: B5
      \texceptions to: B4
      
      B5
      T:\tBREAK_STATEMENT                     \tbreak
      \tjumps to: B4
      
      B4
      0:\tIDENTIFIER                          \ttypes
      1:\tIDENTIFIER                          \ti
      2:\tARRAY_ACCESS_EXPRESSION             \ttypes[i]
      3:\tIDENTIFIER                          \tinvoke4
      4:\tMETHOD_INVOCATION                   \tinvoke4()
      \tjumps to: B3
      \texceptions to: B1
      
      B3
      0:\tASSIGNMENT                          \ttypes[i]=invoke4()
      \tjumps to: B2 B1(exit)
      
      B2
      0:\tIDENTIFIER                          \ti
      1:\tPOSTFIX_INCREMENT                   \ti++
      \tjumps to: B10
      
      B1
      0:\tIDENTIFIER                          \tinvoke10
      1:\tMETHOD_INVOCATION                   \tinvoke10()
      2:\tIDENTIFIER                          \tinvoke11
      3:\tMETHOD_INVOCATION                   \tinvoke11()
      \tjumps to: B0(exit)
      
      B0 (Exit):
      
      """);
  }

  @Test
  void break_in_try_finally_within_while() {
    CFG cfg = buildCFG("""
      void run1() {
        while (true) {
          try {
            break;
          } finally {
            String s = true ? "trueLiteral" : "falseLiteral";
            System.out.println(s);
          }
        }
      }
      """);
    assertThat(CFGDebug.toString(cfg)).isEqualTo("""
      Starts at B7
      
      B7
      0:\tBOOLEAN_LITERAL                     \ttrue
      T:\tWHILE_STATEMENT                     \twhile (true)
      \tjumps to: B6(true) B0(false)
      
      B6
      0:\tTRY_STATEMENT                       \t
      \tjumps to: B5
      
      B5
      T:\tBREAK_STATEMENT                     \tbreak
      \tjumps to: B4
      
      B4
      0:\tBOOLEAN_LITERAL                     \ttrue
      T:\tCONDITIONAL_EXPRESSION              \ttrue ? "trueLiteral" : "falseLiteral"
      \tjumps to: B3(true) B2(false)
      
      B3
      0:\tSTRING_LITERAL                      \t"trueLiteral"
      \tjumps to: B1
      
      B2
      0:\tSTRING_LITERAL                      \t"falseLiteral"
      \tjumps to: B1
      
      B1
      0:\tVARIABLE                            \ts
      1:\tIDENTIFIER                          \tSystem
      2:\tMEMBER_SELECT                       \tSystem.out
      3:\tIDENTIFIER                          \ts
      4:\tMETHOD_INVOCATION                   \t.println(s)
      \tjumps to: B7 B0(exit)
      
      B0 (Exit):
      
      """);
  }
  @Test
  void continue_in_try_finally_within_while() {
    CFG cfg = buildCFG("""
        void run2() {
          while (true) {
            try {
              continue;
            } finally {
              System.out.println(true ? "trueLiteral" : "falseLiteral");
            }
          }
        }
      """);
    assertThat(CFGDebug.toString(cfg)).isEqualTo("""
      Starts at B7
      
      B7
      0:\tBOOLEAN_LITERAL                     \ttrue
      T:\tWHILE_STATEMENT                     \twhile (true)
      \tjumps to: B6(true) B0(false)
      
      B6
      0:\tTRY_STATEMENT                       \t
      \tjumps to: B5
      
      B5
      T:\tCONTINUE_STATEMENT                  \tcontinue
      \tjumps to: B4
      
      B4
      0:\tIDENTIFIER                          \tSystem
      1:\tMEMBER_SELECT                       \tSystem.out
      2:\tBOOLEAN_LITERAL                     \ttrue
      T:\tCONDITIONAL_EXPRESSION              \ttrue ? "trueLiteral" : "falseLiteral"
      \tjumps to: B3(true) B2(false)
      
      B3
      0:\tSTRING_LITERAL                      \t"trueLiteral"
      \tjumps to: B1
      
      B2
      0:\tSTRING_LITERAL                      \t"falseLiteral"
      \tjumps to: B1
      
      B1
      0:\tMETHOD_INVOCATION                   \t.println(true ? "trueLiteral" : "falseLiteral")
      \tjumps to: B7 B0(exit)
      
      B0 (Exit):
      
      """);
  }

  @Test
  void break_in_try_finally_within_for() {
    CFG cfg = buildCFG("""
       void run3() {
          for (int i = 0; i < 5; i++) {
            try {
              break;
            } finally {
              String s;
              System.out.println(true ? "trueLiteral" : "falseLiteral");
            }
          }
        }
      """);
    assertThat(CFGDebug.toString(cfg)).isEqualTo("""
      Starts at B9
      
      B9
      0:\tINT_LITERAL                         \t0
      1:\tVARIABLE                            \ti
      \tjumps to: B8
      
      B8
      0:\tIDENTIFIER                          \ti
      1:\tINT_LITERAL                         \t5
      2:\tLESS_THAN                           \ti < 5
      T:\tFOR_STATEMENT                       \tfor {i;i < 5;i++}
      \tjumps to: B7(true) B0(false)
      
      B7
      0:\tTRY_STATEMENT                       \t
      \tjumps to: B6
      
      B6
      T:\tBREAK_STATEMENT                     \tbreak
      \tjumps to: B5
      
      B5
      0:\tVARIABLE                            \ts
      1:\tIDENTIFIER                          \tSystem
      2:\tMEMBER_SELECT                       \tSystem.out
      3:\tBOOLEAN_LITERAL                     \ttrue
      T:\tCONDITIONAL_EXPRESSION              \ttrue ? "trueLiteral" : "falseLiteral"
      \tjumps to: B4(true) B3(false)
      
      B4
      0:\tSTRING_LITERAL                      \t"trueLiteral"
      \tjumps to: B2
      
      B3
      0:\tSTRING_LITERAL                      \t"falseLiteral"
      \tjumps to: B2
      
      B2
      0:\tMETHOD_INVOCATION                   \t.println(true ? "trueLiteral" : "falseLiteral")
      \tjumps to: B1 B0(exit)
      
      B1
      0:\tIDENTIFIER                          \ti
      1:\tPOSTFIX_INCREMENT                   \ti++
      \tjumps to: B8
      
      B0 (Exit):
      
      """);
  }
  @Test
  void break_in_try_and_complex_finally_within_while() {
    CFG cfg = buildCFG("""
       void run4() {
          while (true) {
            try {
              break;
            } finally {
              String s;
              if (true) { s = "trueLiteral"; } else { s = "falseLiteral"; }
              System.out.println(s);
            }
          }
        }
      """);
    assertThat(CFGDebug.toString(cfg)).isEqualTo("""
      Starts at B7
      
      B7
      0:\tBOOLEAN_LITERAL                     \ttrue
      T:\tWHILE_STATEMENT                     \twhile (true)
      \tjumps to: B6(true) B0(false)
      
      B6
      0:\tTRY_STATEMENT                       \t
      \tjumps to: B5
      
      B5
      T:\tBREAK_STATEMENT                     \tbreak
      \tjumps to: B4
      
      B4
      0:\tVARIABLE                            \ts
      1:\tBOOLEAN_LITERAL                     \ttrue
      T:\tIF_STATEMENT                        \tif (true)
      \tjumps to: B3(true) B2(false)
      
      B3
      0:\tSTRING_LITERAL                      \t"trueLiteral"
      1:\tASSIGNMENT                          \ts="trueLiteral"
      \tjumps to: B1
      
      B2
      0:\tSTRING_LITERAL                      \t"falseLiteral"
      1:\tASSIGNMENT                          \ts="falseLiteral"
      \tjumps to: B1
      
      B1
      0:\tIDENTIFIER                          \tSystem
      1:\tMEMBER_SELECT                       \tSystem.out
      2:\tIDENTIFIER                          \ts
      3:\tMETHOD_INVOCATION                   \t.println(s)
      \tjumps to: B7 B0(exit)
      
      B0 (Exit):
      
      """);
  }

  @Test
  void break_without_finally() {
    CFG cfg = buildCFG("""
        void fun(int highestLevel) {
          while (highestLevel >= lowestOddLevel) {
            int i = levelStart;
            for (;;) {
              while (i < levelLimit && levels[i] < highestLevel) {
                  i++;
              }
              int begin = i++;
              if (begin == levelLimit) {
                  break; // no more runs at this level
              }
              while (i < levelLimit && levels[i] >= highestLevel) {
                  i++;
              }
              int end = i - 1;
              begin += delta;
              end += delta;
              while (begin < end) {
                Object temp = objects[begin];
                objects[begin] = objects[end];
                objects[end] = temp;
                ++begin;
                --end;
              }
            }
            --highestLevel;
          }
        }
      """);
    assertThat(CFGDebug.toString(cfg)).isEqualTo("""
      Starts at B15
      
      B15
      0:\tIDENTIFIER                          \thighestLevel
      1:\tIDENTIFIER                          \tlowestOddLevel
      2:\tGREATER_THAN_OR_EQUAL_TO            \thighestLevel >= lowestOddLevel
      T:\tWHILE_STATEMENT                     \twhile (highestLevel >= lowestOddLevel)
      \tjumps to: B14(true) B0(false)
      
      B14
      0:\tIDENTIFIER                          \tlevelStart
      1:\tVARIABLE                            \ti
      \tjumps to: B13
      
      B13
      T:\tFOR_STATEMENT                       \tfor {;;}
      \tjumps to: B12
      
      B12
      0:\tIDENTIFIER                          \ti
      1:\tIDENTIFIER                          \tlevelLimit
      2:\tLESS_THAN                           \ti < levelLimit
      T:\tCONDITIONAL_AND                     \ti < levelLimit && levels[i] < highestLevel
      \tjumps to: B11(true) B9(false)
      
      B11
      0:\tIDENTIFIER                          \tlevels
      1:\tIDENTIFIER                          \ti
      2:\tARRAY_ACCESS_EXPRESSION             \tlevels[i]
      3:\tIDENTIFIER                          \thighestLevel
      4:\tLESS_THAN                           \tlevels[i] < highestLevel
      T:\tWHILE_STATEMENT                     \twhile (i < levelLimit && levels[i] < highestLevel)
      \tjumps to: B10(true) B9(false)
      
      B10
      0:\tIDENTIFIER                          \ti
      1:\tPOSTFIX_INCREMENT                   \ti++
      \tjumps to: B12
      
      B9
      0:\tIDENTIFIER                          \ti
      1:\tPOSTFIX_INCREMENT                   \ti++
      2:\tVARIABLE                            \tbegin
      3:\tIDENTIFIER                          \tbegin
      4:\tIDENTIFIER                          \tlevelLimit
      5:\tEQUAL_TO                            \tbegin == levelLimit
      T:\tIF_STATEMENT                        \tif (begin == levelLimit)
      \tjumps to: B8(true) B7(false)
      
      B8
      T:\tBREAK_STATEMENT                     \tbreak
      \tjumps to: B1
      
      B7
      0:\tIDENTIFIER                          \ti
      1:\tIDENTIFIER                          \tlevelLimit
      2:\tLESS_THAN                           \ti < levelLimit
      T:\tCONDITIONAL_AND                     \ti < levelLimit && levels[i] >= highestLevel
      \tjumps to: B6(true) B4(false)
      
      B6
      0:\tIDENTIFIER                          \tlevels
      1:\tIDENTIFIER                          \ti
      2:\tARRAY_ACCESS_EXPRESSION             \tlevels[i]
      3:\tIDENTIFIER                          \thighestLevel
      4:\tGREATER_THAN_OR_EQUAL_TO            \tlevels[i] >= highestLevel
      T:\tWHILE_STATEMENT                     \twhile (i < levelLimit && levels[i] >= highestLevel)
      \tjumps to: B5(true) B4(false)
      
      B5
      0:\tIDENTIFIER                          \ti
      1:\tPOSTFIX_INCREMENT                   \ti++
      \tjumps to: B7
      
      B4
      0:\tIDENTIFIER                          \ti
      1:\tINT_LITERAL                         \t1
      2:\tMINUS                               \ti - 1
      3:\tVARIABLE                            \tend
      4:\tIDENTIFIER                          \tbegin
      5:\tIDENTIFIER                          \tdelta
      6:\tPLUS_ASSIGNMENT                     \tbegin+=delta
      7:\tIDENTIFIER                          \tend
      8:\tIDENTIFIER                          \tdelta
      9:\tPLUS_ASSIGNMENT                     \tend+=delta
      \tjumps to: B3
      
      B3
      0:\tIDENTIFIER                          \tbegin
      1:\tIDENTIFIER                          \tend
      2:\tLESS_THAN                           \tbegin < end
      T:\tWHILE_STATEMENT                     \twhile (begin < end)
      \tjumps to: B13(false) B2(true)
      
      B2
      0:\tIDENTIFIER                          \tobjects
      1:\tIDENTIFIER                          \tbegin
      2:\tARRAY_ACCESS_EXPRESSION             \tobjects[begin]
      3:\tVARIABLE                            \ttemp
      4:\tIDENTIFIER                          \tobjects
      5:\tIDENTIFIER                          \tbegin
      6:\tARRAY_ACCESS_EXPRESSION             \tobjects[begin]
      7:\tIDENTIFIER                          \tobjects
      8:\tIDENTIFIER                          \tend
      9:\tARRAY_ACCESS_EXPRESSION             \tobjects[end]
      10:\tASSIGNMENT                          \tobjects[begin]=objects[end]
      11:\tIDENTIFIER                          \tobjects
      12:\tIDENTIFIER                          \tend
      13:\tARRAY_ACCESS_EXPRESSION             \tobjects[end]
      14:\tIDENTIFIER                          \ttemp
      15:\tASSIGNMENT                          \tobjects[end]=temp
      16:\tIDENTIFIER                          \tbegin
      17:\tPREFIX_INCREMENT                    \t++begin
      18:\tIDENTIFIER                          \tend
      19:\tPREFIX_DECREMENT                    \t--end
      \tjumps to: B3
      
      B1
      0:\tIDENTIFIER                          \thighestLevel
      1:\tPREFIX_DECREMENT                    \t--highestLevel
      \tjumps to: B15
      
      B0 (Exit):
      
      """);

  }

  @Test
  void break_in_try_finally_within_loop_do_not_always_lead_to_exit() {
    CFG cfg = buildCFG("""
        void test() {
          RuntimeException e = null;
          for (int i = 0; i < 2; ) {
            try {
              e = new RuntimeException();
              break;
            } finally {
              doSomething();
            }
          }
          throw e;
        }
      """);

    CFGChecker cfgChecker = checker(
      block(
        element(NULL_LITERAL),
        element(VARIABLE, "e"),
        element(INT_LITERAL, 0),
        element(VARIABLE, "i")
      ).successors(6),
      block(
        element(IDENTIFIER, "i"),
        element(INT_LITERAL, 2),
        element(LESS_THAN)
      ).terminator(FOR_STATEMENT).successors(1, 5),
      block(element(TRY_STATEMENT)).successors(4),
      block(element(NEW_CLASS)).successors(3).exceptions(2),
      block(
        element(ASSIGNMENT)
      ).terminator(BREAK_STATEMENT).successors(2),
      block(
        element(IDENTIFIER, "doSomething"),
        element(METHOD_INVOCATION)
      ).successors(0, 1),
      block(
        element(IDENTIFIER, "e")
      ).terminator(THROW_STATEMENT).successors(0));

    cfgChecker.check(cfg);
  }

  @Test
  void break_in_try_finally_in_for_without_condition() {
    CFG cfg = buildCFG("""
        void test() {
          RuntimeException e = null;
          for (;;) {
            try {
              e = new RuntimeException();
              break;
            } finally {
              doSomething();
            }
          }
          throw e;
        }
        """);

    CFGChecker cfgChecker = checker(
      block(
        element(NULL_LITERAL),
        element(VARIABLE, "e")
      ).successors(6),
      terminator(FOR_STATEMENT).successors(5),
      block(element(TRY_STATEMENT)).successors(4),
      block(element(NEW_CLASS)).successors(3).exceptions(2),
      block(
        element(ASSIGNMENT)
      ).terminator(BREAK_STATEMENT).successors(2),
      block(
        element(IDENTIFIER, "doSomething"),
        element(METHOD_INVOCATION)
      ).successors(0, 1),
      block(
        element(IDENTIFIER, "e")
      ).terminator(THROW_STATEMENT).successors(0));

    cfgChecker.check(cfg);
  }

  @Test
  void throw_statement_within_try_catch() {
    CFG cfg = buildCFG("void fun() { try {throw new MyException();} catch(MyException me){foo();} bar();} class MyException extends Exception{}");
    CFGChecker cfgChecker = checker(
      block(element(TRY_STATEMENT)).successors(4),
      block(element(NEW_CLASS)).successors(3).exceptions(0),
      terminator(THROW_STATEMENT).successors(2),
      block(element(VARIABLE, "me"),
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)).successors(1).exceptions(0),
    block(element(IDENTIFIER, "bar"),
      element(METHOD_INVOCATION)).successors(0)
      );
    cfgChecker.check(cfg);

    cfg = buildCFG("void fun() { try {throw new MyException();} catch(MySuperException me){foo();} bar();} class MyException extends MySuperException{} class MySuperException extends Exception{}");
    cfgChecker = checker(
      block(element(TRY_STATEMENT)).successors(4),
      block(element(NEW_CLASS)).successors(3).exceptions(0),
      terminator(THROW_STATEMENT).successors(2),
      block(element(VARIABLE, "me"),
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)).successors(1).exceptions(0),
      block(element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)).successors(0)
    );
    cfgChecker.check(cfg);

    cfg = buildCFG("void fun() { throw new MyException(); bar();} class MyException extends MySuperException{}");
    cfgChecker = checker(
      block(element(NEW_CLASS)).terminator(THROW_STATEMENT).successors(0),
      block(element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)).successors(0)
    );
    cfgChecker.check(cfg);

  }

  @Test
  void generic_record_pattern() {
    CFG cfg = buildCFG("""
        private static boolean testGen3(Object o) throws Throwable {
          return o instanceof GenRecord1<?, ?>(Integer i, var s) && i.intValue() == 3 && s.length() == 0;
        }
        """);

    CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "o"),
        element(RECORD_PATTERN),
        element(UNBOUNDED_WILDCARD),
        element(UNBOUNDED_WILDCARD),
        element(IDENTIFIER, "GenRecord1"),
        element(TYPE_PATTERN),
        element(VARIABLE, "i"),
        element(TYPE_PATTERN),
        element(VARIABLE, "s"),
        element(PATTERN_INSTANCE_OF)
      ).terminator(CONDITIONAL_AND).ifTrue(4).ifFalse(3),
      block(element(IDENTIFIER, "i"), element(METHOD_INVOCATION), element(INT_LITERAL, "3"), element(EQUAL_TO))
        .successors(3),
      terminator(CONDITIONAL_AND).ifTrue(2).ifFalse(1),
      block(element(IDENTIFIER, "s"), element(METHOD_INVOCATION), element(INT_LITERAL, "0"), element(EQUAL_TO))
        .successors(1),
      terminator(RETURN_STATEMENT).successors(0));

    cfgChecker.check(cfg);
  }

  @Test
  void build_partial_cfg_with_break() {
    build_partial_cfg("break");
  }

  @Test
  void build_partial_cfg_with_continue() {
    build_partial_cfg("continue");
  }

  @Test
  void connect_catch_blocks_with_unknown_exception_types() {
    CFG cfg = buildCFG("void fun() { " +
      " try {" +
      "   foo();" +
      " } catch (MyException me) {" +
      "   bar();" +
      " } " +
      "} " +
      "abstract void foo() throws UnknownSymbol;" +
      "class MyException extends Exception{}");

    CFGChecker cfgChecker = checker(
      block(element(TRY_STATEMENT)).successors(2),
      block(element(IDENTIFIER, "foo"), element(METHOD_INVOCATION)).successors(0).exceptions(0, 1),
      block(element(VARIABLE, "me"),
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)).successors(0).exceptions(0)
    );
    cfgChecker.check(cfg);
  }

  @Test
  void connect_catch_blocks_with_unknown_exception_types2() {
    CFG cfg = buildCFG("void fun() { " +
      " try {" +
      "   foo();" +
      " } catch (UnknownSymbol me) {" +
      "   bar();" +
      " } " +
      "} " +
      "abstract void foo() throws MyException;" +
      "class MyException extends Exception{}");

    CFGChecker cfgChecker = checker(
      block(element(TRY_STATEMENT)).successors(2),
      block(element(IDENTIFIER, "foo"), element(METHOD_INVOCATION)).successors(0).exceptions(0, 1),
      block(element(VARIABLE, "me"),
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)).successors(0).exceptions(0)
    );
    cfgChecker.check(cfg);
  }

  @Test
  void test_semantic_completeness() {
    assertCompleteSemantic("void foo() { bar(); } void bar() {}", true);
    assertCompleteSemantic("void foo() { bar(); }", false, "Incomplete Semantic, method invocation 'bar' line 1 col 24");

    assertCompleteSemantic("int foo(int arg) { return arg;     }", true);
    assertCompleteSemantic("int foo(int arg) { return unknown; }", false, "Incomplete Semantic, unknown identifier 'unknown' line 1 col 37");


    assertCompleteSemantic("void foo(boolean condition) { if (condition) {} }", true);
    assertCompleteSemantic("void foo(boolean condition) { if (unknown)   {} }", false, "Incomplete Semantic, unknown identifier 'unknown' line 1 col 45");

    assertCompleteSemantic("Object  foo() { return null; }", true);
    assertCompleteSemantic("Unknown foo() { return null; }", false, "Incomplete Semantic, unknown return type 'foo' line 1 col 19");

    assertCompleteSemantic("void foo(Object  arg) { }", true);
    assertCompleteSemantic("void foo(Unknown arg) { }", false, "Incomplete Semantic, unknown parameter type 'foo' line 1 col 16");

    assertCompleteSemantic("void foo(String arg) { arg.toString(); }", true);
    assertCompleteSemantic("void foo(String arg) { arg.unknown();  }", false, "Incomplete Semantic, method invocation 'unknown' line 1 col 38");

    assertCompleteSemantic("void foo() { Object  x; }", true);
    assertCompleteSemantic("void foo() { Unknown x; }", false, "Incomplete Semantic, unknown variable type 'Unknown' line 1 col 24");

    assertCompleteSemantic("void foo() { java.util.List<Object>  x; }", true);
    assertCompleteSemantic("void foo() { java.util.List<Unknown> x; }", false, "Incomplete Semantic, unknown variable type 'java' line 1 col 24");

    assertCompleteSemantic("void foo(Object...  args) { for(Object  arg : args) { } }", true);
    assertCompleteSemantic("void foo(Unknown... args) { for(Object  arg : args) { } }", false, "Incomplete Semantic, unknown parameter type 'foo' line 1 col 16");
    assertCompleteSemantic("void foo(Object...  args) { for(Unknown arg : args) { } }", false, "Incomplete Semantic, unknown variable type 'Unknown' line 1 col 43");

    assertCompleteSemantic("void foo() { Runnable ignoredLambdaContent = () -> { Unknown x; };  }", true);
    assertCompleteSemantic("void foo() { class IgnoredNestedClass { void foo() { Unknown x; } } }", true);
  }


  @Test
  void test_semantic_completeness_inside_lambda() {
    assertThat(buildCFGFromLambda("I i = () -> { return; };").hasCompleteSemantic()).isTrue();
    assertThat(buildCFGFromLambda("I i = () -> { foo(); }; ").hasCompleteSemantic()).isFalse();
  }

  void assertCompleteSemantic(String code, boolean hasCompleteSemantic, String... debugLogs) {
    logTester.setLevel(Level.DEBUG);
    logTester.clear();
    CFG cfg = buildCFG(code);
    assertThat(cfg.hasCompleteSemantic()).isEqualTo(hasCompleteSemantic);
    assertThat(logTester.logs(Level.DEBUG)).containsExactly(debugLogs);
  }

  private void build_partial_cfg(String breakOrContinue) {
    String methodCode = "void meth(){ try {fun(); } catch ( Exception e) {e.printStackTrace(); "+breakOrContinue+"; } }";
    CompilationUnitTree cut = JParserTestUtils.parse("class A {" + methodCode + "}");
    MethodTree methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    List<StatementTree> body = methodTree.block().body();
    CFG cfg = CFG.buildCFG(body, true);
    cfg.setMethodSymbol(methodTree.symbol());
    assertThat(cfg.blocks()).hasSize(5);
    assertThat(cfg.methodSymbol()).isSameAs(methodTree.symbol());

    try {
      CFG.buildCFG(body, false);
      fail("IllegalStateException should have been thrown");
    } catch (IllegalStateException iae) {
      assertThat(iae).hasMessage("'"+breakOrContinue+"' statement not in loop or switch statement");
    }
  }
}
