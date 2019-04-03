/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.cfg;

import com.sonar.sslr.api.typed.ActionParser;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.plugins.java.api.tree.Tree.Kind.ASSERT_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.BREAK_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CONTINUE_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.EQUAL_TO;
import static org.sonar.plugins.java.api.tree.Tree.Kind.IDENTIFIER;
import static org.sonar.plugins.java.api.tree.Tree.Kind.INT_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.MEMBER_SELECT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.METHOD_INVOCATION;
import static org.sonar.plugins.java.api.tree.Tree.Kind.MULTIPLY_ASSIGNMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.NEW_ARRAY;
import static org.sonar.plugins.java.api.tree.Tree.Kind.NEW_CLASS;
import static org.sonar.plugins.java.api.tree.Tree.Kind.NULL_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.PLUS;
import static org.sonar.plugins.java.api.tree.Tree.Kind.RETURN_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.STRING_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.SWITCH_EXPRESSION;
import static org.sonar.plugins.java.api.tree.Tree.Kind.SWITCH_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.THROW_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.TRY_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.VARIABLE;
import static org.sonar.plugins.java.api.tree.Tree.Kind.WHILE_STATEMENT;

public class CFGTest {

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

    BlockChecker terminator(final Kind kind) {
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
        case VARIABLE:
        case IDENTIFIER:
        case CHAR_LITERAL:
        case STRING_LITERAL:
        case BOOLEAN_LITERAL:
        case INT_LITERAL:
        case METHOD_INVOCATION:
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
        case METHOD_INVOCATION:
        case ASSERT_STATEMENT:
        case METHOD_REFERENCE:
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
        case SWITCH_EXPRESSION:
        case LAMBDA_EXPRESSION:
        case TYPE_CAST:
        case PLUS_ASSIGNMENT:
        case ASSIGNMENT:
        case ARRAY_ACCESS_EXPRESSION:
        case LOGICAL_COMPLEMENT:
        case MULTIPLY_ASSIGNMENT:
        case PLUS:
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
            assertThat(select.identifier().toString()).as("Method").isEqualTo(name);
          }
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
        case SWITCH_EXPRESSION:
        case RETURN_STATEMENT:
        case FOR_STATEMENT:
        case FOR_EACH_STATEMENT:
        case WHILE_STATEMENT:
        case DO_STATEMENT:
        case THROW_STATEMENT:
        case SYNCHRONIZED_STATEMENT:
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

  public static final ActionParser<Tree> parser = JavaParser.createParser();

  public static CFG buildCFG(String methodCode) {
    return buildCFGFromCUT((CompilationUnitTree) parser.parse("class A { " + methodCode + " }"));
  }

  public static CFG buildCFG(File file) {
    return buildCFGFromCUT((CompilationUnitTree) parser.parse(file));
  }

  private static CFG buildCFGFromCUT(CompilationUnitTree cut) {
    SemanticModel.createFor(cut, new SquidClassLoader(Collections.emptyList()));
    final MethodTree tree = ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(0));
    return CFG.build(tree);
  }

  @Test
  public void empty_cfg() {
    final CFG cfg = buildCFG("void fun() {}");
    final CFGChecker cfgChecker = checker();
    cfgChecker.check(cfg);
    assertThat(cfg.entryBlock().isMethodExitBlock()).as("entry is an exit").isTrue();
  }

  @Test
  public void simplest_cfg() {
    final CFG cfg = buildCFG("void fun() { bar();}");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
    CFG.Block entry = cfg.entryBlock();
    assertThat(entry.isMethodExitBlock()).as("1st block is not an exit").isFalse();
    assertThat(entry.successors()).as("number of successors").hasSize(1);
    CFG.Block exit = entry.successors().iterator().next();
    assertThat(exit.isMethodExitBlock()).as("2nd block is an exit").isTrue();
  }

  @Test
  public void straight_method_calls() {
    final CFG cfg = buildCFG("void fun() { bar();qix();baz();}");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Tree.Kind.METHOD_INVOCATION),
        element(Tree.Kind.IDENTIFIER, "qix"),
        element(Tree.Kind.METHOD_INVOCATION),
        element(Tree.Kind.IDENTIFIER, "baz"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void single_declaration() {
    final CFG cfg = buildCFG("void fun() {Object o;}");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.VARIABLE, "o")).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void if_then() {
    final CFG cfg = buildCFG("void fun() {if(a) { foo(); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a")
        ).terminator(Tree.Kind.IF_STATEMENT).successors(0, 1),
      block(
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void if_then_else() {
    final CFG cfg = buildCFG("void fun() {if(a) { foo(); } else { bar(); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a"))
      .terminator(Tree.Kind.IF_STATEMENT).successors(1, 2),
      block(
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0),
      block(
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void if_then_elseif() {
    final CFG cfg = buildCFG("void fun() {\nif(a) {\n foo(); \n } else if(b) {\n bar();\n } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a")
        ).terminator(Tree.Kind.IF_STATEMENT).successors(2, 3),
      block(
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(0),
      block(
        element(Tree.Kind.IDENTIFIER, "b")
        ).terminator(Tree.Kind.IF_STATEMENT).successors(0, 1),
      block(
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void conditionalOR() {
    final CFG cfg = buildCFG("void fun() {if(a || b) { foo(); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a")
        ).terminator(Tree.Kind.CONDITIONAL_OR).successors(1, 2),
      block(
        element(Tree.Kind.IDENTIFIER, "b")
        ).terminator(Tree.Kind.IF_STATEMENT).successors(0, 1),
      block(
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void conditionalAND() {
    final CFG cfg = buildCFG("void fun() {if((a && b)) { foo(); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a")
        ).terminator(Tree.Kind.CONDITIONAL_AND).successors(0, 2),
      block(
        element(Tree.Kind.IDENTIFIER, "b")
        ).terminator(Tree.Kind.IF_STATEMENT).successors(0, 1),
      block(
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void assignmentAND() {
    final CFG cfg = buildCFG("void fun() {boolean bool = a && b;}");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a")).terminator(Tree.Kind.CONDITIONAL_AND).successors(1, 2),
      block(
        element(Tree.Kind.IDENTIFIER, "b")).successors(1),
      block(
        element(Tree.Kind.VARIABLE, "bool")).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void ternary_operator() {
    final CFG cfg = buildCFG("void fun() { Object c = foo ? a : b; a.toString();}");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "foo")).terminator(Tree.Kind.CONDITIONAL_EXPRESSION).successors(2, 3),
      block(
        element(Tree.Kind.IDENTIFIER, "a")).successors(1),
      block(
        element(Tree.Kind.IDENTIFIER, "b")).successors(1),
      block(
        element(Kind.VARIABLE, "c"),
        element(Tree.Kind.IDENTIFIER, "a"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void switch_statement() {
    CFG cfg = buildCFG("void foo(int i, int j, int k) {\n" +
        "    switch (i==-1 ? j:k) {\n" +
        "      default:;\n" +
        "    }\n" +
        "  }");

    assertThat(cfg.blocks().get(0).id()).isEqualTo(5);

    cfg = buildCFG("void fun(int foo) {\n" +
      "  int a;" +
      "  switch(foo) {\n" +
      "    case 1:\n" +
      "      System.out.println(bar);\n" +
      "    case 2:\n" +
      "      System.out.println(qix);\n" +
      "      break;\n" +
      "    default:\n" +
      "      System.out.println(baz);\n" +
      "  }\n" +
      "}");
    CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, "1"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).hasCaseGroup().successors(3),
      block(
        element(INT_LITERAL, "2"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.IDENTIFIER, "qix"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).hasCaseGroup().terminator(Tree.Kind.BREAK_STATEMENT).successors(0),
      block(
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.IDENTIFIER, "baz"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).hasCaseGroup().successors(0),
      block(
        element(Tree.Kind.VARIABLE, "a"),
        element(Tree.Kind.IDENTIFIER, "foo")
        ).terminator(Tree.Kind.SWITCH_STATEMENT).successors(2, 3, 4));
    cfgChecker.check(cfg);
  }

  @Test
  public void switch_statement_with_piledUpCases_againstDefault() {
    final CFG cfg = buildCFG("void fun(int foo) {\n" +
      "    int a;\n" +
      "    switch (foo) {\n" +
      "      case 1:\n" +
      "        System.out.println(bar);\n" +
      "      case 2:\n" +
      "        System.out.println(qix);\n" +
      "        break;\n" +
      "      case 3:\n" +
      "      case 4:\n" +
      "      default:\n" +
      "        System.out.println(baz);\n" +
      "    }\n" +
      "  }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, "1"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Tree.Kind.METHOD_INVOCATION)).hasCaseGroup().successors(3),
      block(
        element(INT_LITERAL, "2"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.IDENTIFIER, "qix"),
        element(Tree.Kind.METHOD_INVOCATION)).terminator(Tree.Kind.BREAK_STATEMENT).hasCaseGroup().successors(0),
      block(
        element(INT_LITERAL, "3"),
        element(INT_LITERAL, "4"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.IDENTIFIER, "baz"),
        element(Tree.Kind.METHOD_INVOCATION)).hasCaseGroup().successors(0),
      block(
        element(Tree.Kind.VARIABLE, "a"),
        element(Tree.Kind.IDENTIFIER, "foo")).terminator(Tree.Kind.SWITCH_STATEMENT).successors(2, 3, 4));
    cfgChecker.check(cfg);
  }

  @Test
  public void switch_statement_without_default() {
    final CFG cfg = buildCFG("void fun(int foo) {\n" +
      "    int a;\n" +
      "    switch (foo) {\n" +
      "      case 1:\n" +
      "        System.out.println(bar);\n" +
      "      case 2:\n" +
      "        System.out.println(qix);\n" +
      "        break;\n" +
      "    }\n" +
      "    Integer.toString(foo);\n" +
      "  }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, "1"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Tree.Kind.METHOD_INVOCATION)).hasCaseGroup().successors(3),
      block(
        element(INT_LITERAL, "2"),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.IDENTIFIER, "qix"),
        element(Tree.Kind.METHOD_INVOCATION)).terminator(Tree.Kind.BREAK_STATEMENT).hasCaseGroup().successors(1),
      block(
        element(Tree.Kind.VARIABLE, "a"),
        element(Tree.Kind.IDENTIFIER, "foo")).terminator(Tree.Kind.SWITCH_STATEMENT).successors(1, 3, 4),
      block(
        element(Tree.Kind.IDENTIFIER, "Integer"),
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  /**
   * Introduced with Java 12
   */
  @Test
  public void switch_statement_without_fallthrough() {
    final CFG cfg = buildCFG("void fun(int foo) throws Exception {\n" +
      "    int a;\n" +
      "    switch (foo) {\n" +
      "      case 1     -> {\n" +
      "        fun(bar1);\n" +
      "        fun(bar2);\n" +
      "      }\n" +
      "      case 2,3,4 -> fun(qix);\n" +
      "      case 5     -> fun(gul);\n" +
      "      case 6     -> throw new Exception(\"boom\");\n" +
      "      default    -> fun(def);\n" +
      "    }\n" +
      "    Integer.toString(foo);\n" +
      "  }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, "1"),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "bar1"),
        element(METHOD_INVOCATION),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "bar2"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(1),
      block(
        element(INT_LITERAL, "2"),
        element(INT_LITERAL, "3"),
        element(INT_LITERAL, "4"),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "qix"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(1),
      block(
        element(INT_LITERAL, "5"),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "gul"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(1),
      block(
        element(INT_LITERAL, "6"),
        element(STRING_LITERAL, "boom"),
        element(NEW_CLASS)).hasCaseGroup().terminator(THROW_STATEMENT).successors(0),
      block(
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "def"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(1),
      block(
        element(VARIABLE, "a"),
        element(IDENTIFIER, "foo")).terminator(SWITCH_STATEMENT).successors(3, 4, 5, 6, 7),
      block(
        element(IDENTIFIER, "Integer"),
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void switch_expression_without_fallthrough() {
    final CFG cfg = buildCFG("int fun(int foo) throws Exception {\n" +
      "    int a = switch (foo) {\n" +
      "      case 1 -> fun(bar1) + fun(bar2);\n" +
      "      case 2, 3, 4 -> fun(qix);\n" +
      "      case 5 -> throw new Exception(\"boom\");\n" +
      "      default -> fun(def);\n" +
      "    };\n" +
      "    return a;\n" +
      "  }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, "1"),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "bar1"),
        element(METHOD_INVOCATION),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "bar2"),
        element(METHOD_INVOCATION),
        element(PLUS)).hasCaseGroup().successors(1),
      block(
        element(INT_LITERAL, "2"),
        element(INT_LITERAL, "3"),
        element(INT_LITERAL, "4"),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "qix"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(1),
      block(
        element(INT_LITERAL, "5"),
        element(STRING_LITERAL, "boom"),
        element(NEW_CLASS)).hasCaseGroup().terminator(THROW_STATEMENT).successors(0),
      block(
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "def"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(1),
      block(
        element(IDENTIFIER, "foo")).terminator(SWITCH_EXPRESSION).successors(3, 4, 5, 6),
      block(
        element(SWITCH_EXPRESSION),
        element(VARIABLE, "a"),
        element(IDENTIFIER, "a")).terminator(RETURN_STATEMENT).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void switch_expression_with_fallthrough() {
    final CFG cfg = buildCFG("int fun(int foo) throws Exception {\n" +
      "    int a = switch (foo) {\n" +
      "      case 1:\n" +
      "        fun(bar);\n" +
      "      case 2:\n" +
      "      case 3:\n" +
      "      case 4:\n" +
      "        break fun(bar1) + fun(bar2);\n" +
      "      case 5:\n" +
      "        throw new Exception(\"boom\");\n" +
      "      case 6:\n" +
      "        break foo;\n" +
      "      default:\n" +
      "        break fun(def);\n" +
      "    };\n" +
      "    return a;\n" +
      "  }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, "1"),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "bar"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(6),
      block(
        element(INT_LITERAL, "2"),
        element(INT_LITERAL, "3"),
        element(INT_LITERAL, "4"),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "bar1"),
        element(METHOD_INVOCATION),
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "bar2"),
        element(METHOD_INVOCATION),
        element(PLUS)).hasCaseGroup().terminator(BREAK_STATEMENT).successors(1),
      block(
        element(INT_LITERAL, "5"),
        element(STRING_LITERAL, "boom"),
        element(NEW_CLASS)).hasCaseGroup().terminator(THROW_STATEMENT).successors(0),
      block(
        element(INT_LITERAL, "6"),
        element(IDENTIFIER, "foo")).hasCaseGroup().successors(1),
      block(
        element(IDENTIFIER, "fun"),
        element(IDENTIFIER, "def"),
        element(METHOD_INVOCATION)).hasCaseGroup().successors(1),
      block(
        element(IDENTIFIER, "foo")).terminator(SWITCH_EXPRESSION).successors(3, 4, 5, 6, 7),
      block(
        element(SWITCH_EXPRESSION),
        element(VARIABLE, "a"),
        element(IDENTIFIER, "a")).terminator(RETURN_STATEMENT).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void return_statement() {
    final CFG cfg = buildCFG("void fun(Object foo) { if(foo == null) return; }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.NULL_LITERAL),
        element(EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(0, 1),
      terminator(Tree.Kind.RETURN_STATEMENT, 0).successorWithoutJump(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void array_loop() {
    final CFG cfg = buildCFG("void fun(Object foo) {System.out.println('c'); for(int i =0;i<10;i++) { System.out.println(i); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.CHAR_LITERAL, "'c'"),
        element(Tree.Kind.METHOD_INVOCATION),
        element(INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(3),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.FOR_STATEMENT).successors(0, 2),
      block(
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(1),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT)
        ).successors(3));
    cfgChecker.check(cfg);
  }

  @Test
  public void array_loop_with_break() {
    final CFG cfg = buildCFG("void fun(Object foo) { for(int i =0;i<10;i++) { if(i == 5) break; } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(4),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.FOR_STATEMENT).successors(0, 3),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(1, 2),
      terminator(Tree.Kind.BREAK_STATEMENT, 0).successorWithoutJump(1),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT)
        ).successors(4));
    cfgChecker.check(cfg);
  }

  @Test
  public void array_loop_with_continue() {
    final CFG cfg = buildCFG("void fun(Object foo) { for(int i =0;i<10;i++) { if(i == 5) continue; } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(4),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.FOR_STATEMENT).successors(0, 3),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(1, 2),
      terminator(Tree.Kind.CONTINUE_STATEMENT, 1).successorWithoutJump(1),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT)
        ).successors(4));
    cfgChecker.check(cfg);
  }

  @Test
  public void foreach_loop_continue() {
    final CFG cfg = buildCFG("void fun(){ System.out.println(\"start\"); for(String foo:list) {System.out.println(foo); if(foo.length()> 2) {continue;}  System.out.println('c');} System.out.println(\"end\"); }");
    final CFGChecker cfgChecker = checker(
        block(
          element(Tree.Kind.IDENTIFIER, "System"),
          element(Tree.Kind.MEMBER_SELECT),
          element(Tree.Kind.STRING_LITERAL, "start"),
          element(Tree.Kind.METHOD_INVOCATION)).successors(6),
        block(
            element(Tree.Kind.IDENTIFIER, "list")).successors(2),
        block(
          element(Tree.Kind.IDENTIFIER, "System"),
          element(Tree.Kind.MEMBER_SELECT),
          element(Tree.Kind.IDENTIFIER, "foo"),
          element(Kind.METHOD_INVOCATION),
          element(Tree.Kind.IDENTIFIER, "foo"),
          element(Kind.METHOD_INVOCATION),
          element(INT_LITERAL, 2),
          element(Kind.GREATER_THAN)
        ).terminator(Kind.IF_STATEMENT).successors(3, 4),
        terminator(Kind.CONTINUE_STATEMENT).successors(2).successorWithoutJump(3),
        block(
          element(Tree.Kind.IDENTIFIER, "System"),
          element(Tree.Kind.MEMBER_SELECT),
          element(Tree.Kind.CHAR_LITERAL, "'c'"),
          element(Tree.Kind.METHOD_INVOCATION)).successors(2),
        block(
            element(Tree.Kind.VARIABLE, "foo")).terminator(Tree.Kind.FOR_EACH_STATEMENT).successors(1, 5),
        block(
          element(Tree.Kind.IDENTIFIER, "System"),
          element(Tree.Kind.MEMBER_SELECT),
          element(Tree.Kind.STRING_LITERAL, "end"),
          element(Tree.Kind.METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void foreach_loop() {
    CFG cfg = buildCFG("void fun(){ System.out.println('c'); for(String foo:list) {System.out.println(foo);} System.out.println('d'); }");
    CFGChecker cfgChecker = checker(
        block(
          element(Tree.Kind.IDENTIFIER, "System"),
          element(Tree.Kind.MEMBER_SELECT),
          element(Tree.Kind.CHAR_LITERAL, "'c'"),
          element(Tree.Kind.METHOD_INVOCATION)).successors(4),
        block(
            element(Tree.Kind.IDENTIFIER, "list")).successors(2),
        block(
          element(Tree.Kind.IDENTIFIER, "System"),
          element(Tree.Kind.MEMBER_SELECT),
          element(Tree.Kind.IDENTIFIER, "foo"),
          element(Tree.Kind.METHOD_INVOCATION)).successors(2),
        block(
            element(Tree.Kind.VARIABLE, "foo")).terminator(Tree.Kind.FOR_EACH_STATEMENT).successors(1, 3),
        block(
          element(Tree.Kind.IDENTIFIER, "System"),
          element(Tree.Kind.MEMBER_SELECT),
          element(Tree.Kind.CHAR_LITERAL, "'d'"),
          element(Tree.Kind.METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
    cfg = buildCFG("void fun(){ for (String n : dir.list(foo() ? \"**\" : \"\")) {\n" +
        "      if (s.isEmpty()) {\n" +
        "        relativePath = n;\n" +
        "      }\n" +
        "    }}");
    cfgChecker = new CFGChecker(
        block(
          element(Kind.IDENTIFIER, "dir"),
          element(Kind.IDENTIFIER, "foo"),
          element(Kind.METHOD_INVOCATION)).terminator(Kind.CONDITIONAL_EXPRESSION).ifTrue(6).ifFalse(5),
        block(element(Kind.STRING_LITERAL, "**")).successors(4),
        block(element(Kind.STRING_LITERAL, "")).successors(4),
        block(
            element(Kind.METHOD_INVOCATION)).successors(1),
        block(
            element(Kind.IDENTIFIER, "s"),
            element(Kind.METHOD_INVOCATION)).terminator(Kind.IF_STATEMENT).ifTrue(2).ifFalse(1),
        block(
            element(Kind.IDENTIFIER, "n"),
            element(Kind.ASSIGNMENT)).successors(1),
        block(element(Kind.VARIABLE, "n")).terminator(Kind.FOR_EACH_STATEMENT).ifFalse(0).ifTrue(3)
        );
    cfgChecker.check(cfg);
  }

  @Test
  public void while_loop() {
    final CFG cfg = buildCFG("void fun() {int i = 0; while(i < 10) {i++; System.out.println(i); } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(2),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.WHILE_STATEMENT).successors(0, 1),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(2));
    cfgChecker.check(cfg);
  }

  @Test
  public void while_loop_with_break() {
    final CFG cfg = buildCFG("void fun() {int i = 0; while(i < 10) {i++; if(i == 5) break; } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(3),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.WHILE_STATEMENT).successors(0, 2),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(1, 3),
      terminator(Tree.Kind.BREAK_STATEMENT, 0).successorWithoutJump(3));
    cfgChecker.check(cfg);
  }

  @Test
  public void while_loop_with_continue() {
    final CFG cfg = buildCFG("void fun() {int i = 0; while(i < 10) {i++; if(i == 5) continue; } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(3),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.WHILE_STATEMENT).successors(0, 2),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(1, 3),
      terminator(Tree.Kind.CONTINUE_STATEMENT, 3).successorWithoutJump(3));
    cfgChecker.check(cfg);
  }

  @Test
  public void continue_in_try_finally() {
    final CFG cfg = buildCFG("void fun() { while (foo()) {\n" +
      "      try {\n" +
      "        bar(\"try\");\n" +
      "        continue;\n" +
      "      } finally {\n" +
      "        qix(\"finally\");\n" +
      "      }\n" +
      "    }}");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)
      ).terminator(WHILE_STATEMENT).successors(0, 4),
      block(element(TRY_STATEMENT)).successors(3),
      block(
        element(Tree.Kind.IDENTIFIER, "bar"),
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
  public void break_in_try_finally() {
    final CFG cfg = buildCFG("void fun() { while (foo()) {\n" +
      "      try {\n" +
      "        bar(\"try\");\n" +
      "        break;\n" +
      "      } finally {\n" +
      "        qix(\"finally\");\n" +
      "      }\n" +
      "    }}");
    final CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "foo"),
        element(METHOD_INVOCATION)
      ).terminator(WHILE_STATEMENT).successors(0, 4),
      block(element(TRY_STATEMENT)).successors(3),
      block(
        element(Tree.Kind.IDENTIFIER, "bar"),
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
  public void do_while_loop() {
    final CFG cfg = buildCFG("void fun() {int i = 0; do {i++; System.out.println(i); }while(i < 10); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(2),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT),
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.METHOD_INVOCATION)
        ).successors(1),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.DO_STATEMENT).successors(0, 2));
    cfgChecker.check(cfg);
  }

  @Test
  public void do_while_loop_with_break() {
    final CFG cfg = buildCFG("void fun() {int i = 0; do { i++; if(i == 5) break; }while(i < 10); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(3),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(1, 2),
      terminator(Tree.Kind.BREAK_STATEMENT, 0).successorWithoutJump(1),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.DO_STATEMENT).successors(0, 3));
    cfgChecker.check(cfg);
  }

  @Test
  public void do_while_loop_with_continue() {
    final CFG cfg = buildCFG("void fun() {int i = 0; do{i++; if(i == 5) continue; }while(i < 10); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(3),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(1, 2),
      terminator(Tree.Kind.CONTINUE_STATEMENT, 1).successorWithoutJump(1),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.DO_STATEMENT).successors(0, 3));
    cfgChecker.check(cfg);
  }

  @Test
  public void break_on_label() {
    final CFG cfg = buildCFG("void fun() {\n" +
      "    foo: for (int i = 0; i < 10; i++) {\n" +
      "      if (i == 5)\n" +
      "        break foo;\n" +
      "    }\n" +
      "  }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(4),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.FOR_STATEMENT).successors(0, 3),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(1, 2),
      terminator(Tree.Kind.BREAK_STATEMENT, 0).successorWithoutJump(1),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT)
        ).successors(4));
    cfgChecker.check(cfg);
  }

  @Test
  public void continue_on_label() {
    final CFG cfg = buildCFG("void fun() {\n" +
      "    foo: for (int i = 0; i < 10; i++) {\n" +
      "      plop();\n" +
      "      if (i == 5)\n" +
      "        continue foo;\n" +
      "      plop();\n" +
      "    }\n" +
      "  }");
    final CFGChecker cfgChecker = checker(
      block(
        element(INT_LITERAL, 0),
        element(Tree.Kind.VARIABLE, "i")
        ).successors(5),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 10),
        element(Tree.Kind.LESS_THAN)
        ).terminator(Tree.Kind.FOR_STATEMENT).successors(0, 4),
      block(
        element(Tree.Kind.IDENTIFIER, "plop"),
        element(Kind.METHOD_INVOCATION),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(INT_LITERAL, 5),
        element(EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(2,3),
      terminator(Tree.Kind.CONTINUE_STATEMENT, 1).successorWithoutJump(2),
        block(
            element(Tree.Kind.IDENTIFIER, "plop"),
            element(Kind.METHOD_INVOCATION)
        ).successors(1),
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT)
        ).successors(5));
    cfgChecker.check(cfg);
  }

  @Test
  public void assignement_order_of_evaluation() throws Exception {
    CFG cfg = buildCFG("  void foo() {\n" +
      "    int[] a = {4,4};\n" +
      "    int b = 1;\n" +
      "    a[b] = b = 0;\n" +
      "   }");
    CFGChecker checker = checker(
      block(
        element(Tree.Kind.INT_LITERAL, 4),
        element(Tree.Kind.INT_LITERAL, 4),
        element(Tree.Kind.NEW_ARRAY),
        element(Tree.Kind.VARIABLE, "a"),
        element(Tree.Kind.INT_LITERAL, 1),
        element(Tree.Kind.VARIABLE, "b"),
        element(Tree.Kind.IDENTIFIER, "a"),
        element(Tree.Kind.IDENTIFIER, "b"),
        element(Tree.Kind.ARRAY_ACCESS_EXPRESSION),
        element(Tree.Kind.INT_LITERAL, 0),
        element(Tree.Kind.ASSIGNMENT),
        element(Tree.Kind.ASSIGNMENT)).successors(0));
    checker.check(cfg);
  }

  @Test
  public void compound_assignment() throws Exception {
    CFG cfg = buildCFG("void foo() {\n" +
      "  myField *= 0;\n" +
      "}\n" +
      "int myField;");

    CFGChecker checker = checker(
      block(
        element(IDENTIFIER, "myField"),
        element(INT_LITERAL, 0),
        element(MULTIPLY_ASSIGNMENT)
        ).successors(0));

    checker.check(cfg);
  }

  @Test
  public void compound_assignment_member_select() throws Exception {
    CFG cfg = buildCFG("void foo() {\n" +
      "  this.myField *= 0;\n" +
      "}\n" +
      "int myField;");

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
  public void prefix_operators() {
    final CFG cfg = buildCFG("void fun() { ++i;i++; }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.PREFIX_INCREMENT),
        element(Tree.Kind.IDENTIFIER, "i"),
        element(Tree.Kind.POSTFIX_INCREMENT)).successors(0));
    cfgChecker.check(cfg);
  }
  @Test
  public void exit_block_for_finally_with_if_statement() throws Exception {
    CFG cfg = buildCFG(" void test(boolean fooCalled) {\n" +
      "      Object bar;\n" +
      "      try {\n" +
      "        bar = new Bar();\n" +
      "      } finally {\n" +
      "        if (fooCalled) {foo();\n" +
      "        }\n" +
      "      }\n" +
      "      bar.toString();\n" +
      "    }");
    CFGChecker cfgChecker = checker(
      block(
        element(Kind.VARIABLE, "bar"),
        element(Kind.TRY_STATEMENT)
      ).successors(6),
      block(
        element(Kind.NEW_CLASS)
      ).successors(5).exceptions(4),
      block(
        element(Kind.ASSIGNMENT)
      ).successors(4),
      block(
        element(Kind.IDENTIFIER, "fooCalled")
      ).terminator(Kind.IF_STATEMENT).successors(2, 3),
      block(
        element(Kind.IDENTIFIER, "foo"),
        element(Kind.METHOD_INVOCATION)
      ).successors(2),
      new BlockChecker(1, 0).exit(0),
      block(
        element(Kind.IDENTIFIER, "bar"),
        element(Kind.METHOD_INVOCATION)
      ).successors(0)
    );
    cfgChecker.check(cfg);

  }

  @Test
  public void catch_thrown_in_exception() throws Exception {
    CFG cfg = buildCFG("  void  foo() throws MyException {\n"+
      "    try {\n"+
      "      try {\n"+
      "        foo();      \n"+
      "      } catch (MyException e) {\n"+
      "        foo();      \n"+
      "      }\n"+
      "    } catch (MyException e) {\n"+
      "      System.out.println(\"outercatch\");\n"+
      "    }\n"+
      "   }" +
      " class MyException extends Exception {}");
    CFGChecker checker = checker(
      block(
        element(Tree.Kind.TRY_STATEMENT)
      ).successors(4),
      block(
        element(Tree.Kind.TRY_STATEMENT)
        ).successors(3),
      block(
        element(Kind.IDENTIFIER, "foo"),
        element(Kind.METHOD_INVOCATION)
        ).successors(0).exceptions(0,2),
      block(
        element(Kind.VARIABLE, "e"),
        element(Kind.IDENTIFIER, "foo"),
        element(Kind.METHOD_INVOCATION)
        ).successors(0).exceptions(0, 1),
      block(
        element(Kind.VARIABLE, "e"),
        element(Kind.IDENTIFIER, "System"),
        element(Kind.MEMBER_SELECT),
        element(Kind.STRING_LITERAL, "outercatch"),
        element(Kind.METHOD_INVOCATION)
      ).successors(0).exceptions(0)
    );
    checker.check(cfg);
  }

  @Test
  public void nested_try_finally() throws Exception {

    CFG cfg = buildCFG("  void  foo() {\n"+
      "    try {\n"+
      "      java.util.zip.ZipFile file = new java.util.zip.ZipFile(fileName);\n"+
      "      try {\n"+
      "        file.foo();// do something with the file...\n"+
      "      } finally {\n"+
      "        file.close();\n"+
      "      }\n"+
      "    } catch (Exception e) {\n"+
      "      // Handle exception\n"+
      "    }\n"+
      "  }");
    CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.TRY_STATEMENT)
      ).successors(5),
      block(
        element(Tree.Kind.IDENTIFIER, "fileName"),
        element(Kind.NEW_CLASS)
      ).successors(4).exceptions(0,1),
      block(
        element(Kind.VARIABLE, "file"),
        element(Kind.TRY_STATEMENT)
      ).successors(3),
      block(
        element(Tree.Kind.IDENTIFIER, "file"),
        element(Tree.Kind.METHOD_INVOCATION)
      ).successors(2).exceptions(2),
      block(
        element(Tree.Kind.IDENTIFIER, "file"),
        element(Tree.Kind.METHOD_INVOCATION)
      ).successors(0).exceptions(0,1),
      block(
        element(Kind.VARIABLE, "e")
      )
      );
    cfgChecker.check(cfg);

  }

  @Test
  public void catch_throwable() throws Exception {
    CFG cfg = buildCFG(" public void reschedule() {\n" +
      "        try {\n" +
      "          getNextSchedule();\n" +
      "        } catch (Throwable t) {\n" +
      "          notifyFailed();\n" +
      "        }\n" +
      "      }");
      CFGChecker cfgChecker = checker(
        block(
          element(Kind.TRY_STATEMENT)
        ).successors(2),
        block(
          element(Kind.IDENTIFIER, "getNextSchedule"),
          element(Kind.METHOD_INVOCATION)
          ).successors(0).exceptions(0, 1),
        block(
          element(Kind.VARIABLE, "t"),
          element(Kind.IDENTIFIER, "notifyFailed"),
          element(Kind.METHOD_INVOCATION)
        ).successors(0).exceptions(0)
      );
    cfgChecker.check(cfg);
  }

  @Test
  public void catch_error() throws Exception {
    CFG cfg = buildCFG(" public void foo() {\n" +
      "        try {\n" +
      "          doSomething();\n" +
      "        } catch (Error e) {\n" +
      "          throw e;\n" +
      "        }\n" +
      "      }");
    CFGChecker cfgChecker = checker(
      block(
        element(Kind.TRY_STATEMENT)).successors(2),
      block(
        element(Kind.IDENTIFIER, "doSomething"),
        element(Kind.METHOD_INVOCATION)).successors(0).exceptions(0, 1),
      block(
        element(Kind.VARIABLE, "e"),
        element(Kind.IDENTIFIER, "e")).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void try_statement() {
    CFG cfg = buildCFG("void fun() {try {System.out.println('c');} finally { System.out.println('c'); }}");
    CFGChecker cfgChecker = checker(
        block(
            element(Tree.Kind.TRY_STATEMENT)
        ).successors(2),
        block(
          element(Tree.Kind.IDENTIFIER, "System"),
          element(Tree.Kind.MEMBER_SELECT),
          element(Tree.Kind.CHAR_LITERAL, "'c'"),
          element(Tree.Kind.METHOD_INVOCATION)
        ).successors(1).exceptions(1),
        block(
          element(Tree.Kind.IDENTIFIER, "System"),
          element(Tree.Kind.MEMBER_SELECT),
          element(Tree.Kind.CHAR_LITERAL, "'c'"),
          element(Tree.Kind.METHOD_INVOCATION)
      ).successors(0).isFinallyBlock());
    cfgChecker.check(cfg);
    cfg = buildCFG("void fun() {try {System.out.println('c');} catch(IllegalArgumentException e) { foo('i');} catch(Exception e){bar('e');}" +
        " finally { System.out.println(\"finally\"); }}");
    cfgChecker = checker(
        block(
            element(Tree.Kind.TRY_STATEMENT)
        ).successors(4),
      block(
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.CHAR_LITERAL, "'c'"),
        element(Tree.Kind.METHOD_INVOCATION)
      ).successors(1).exceptions(1, 2, 3),
      block(
        element(Kind.VARIABLE, "e"),
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.CHAR_LITERAL, "'i'"),
        element(Tree.Kind.METHOD_INVOCATION)
      ).successors(1).exceptions(1).isCatchBlock(),
      block(
        element(Kind.VARIABLE, "e"),
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Tree.Kind.CHAR_LITERAL, "'e'"),
        element(Tree.Kind.METHOD_INVOCATION)
      ).successors(1).exceptions(1).isCatchBlock(),
      block(
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Kind.STRING_LITERAL, "finally"),
        element(Tree.Kind.METHOD_INVOCATION)
      ).successors(0).isFinallyBlock()
    );
    cfgChecker.check(cfg);
    cfg = buildCFG(
        "  private void f() {\n" +
            "    try {\n" +
            "    } catch (Exception e) {\n" +
            "      if (e instanceof IOException) { \n" +
            "      }\n}}");
    cfgChecker = checker(
        block(
            element(Tree.Kind.TRY_STATEMENT)
        ).successors(0),
        block(
          element(Kind.VARIABLE, "e"),
          element(Tree.Kind.IDENTIFIER, "e"),
            element(Tree.Kind.INSTANCE_OF)
      ).terminator(Tree.Kind.IF_STATEMENT).ifTrue(0).ifFalse(0).isCatchBlock()
    );
    cfgChecker.check(cfg);
    cfg = buildCFG(
        "  private void f() {\n" +
            "    try {\n" +
            "    return;" +
            "} finally { foo();} bar(); }");
    cfgChecker = checker(
        block(
            element(Tree.Kind.TRY_STATEMENT)
        ).successors(3),
        terminator(Kind.RETURN_STATEMENT).successors(2).exit(2).successorWithoutJump(2),
        block(
            element(Tree.Kind.IDENTIFIER, "foo"),
            element(Kind.METHOD_INVOCATION)
      ).successors(0, 1).exit(0).isFinallyBlock(),
        block(
            element(Tree.Kind.IDENTIFIER, "bar"),
            element(Kind.METHOD_INVOCATION)
        ).successors(0)
    );
    cfgChecker.check(cfg);
  }

  @Test
  public void throw_statement() {
    final CFG cfg = buildCFG("void fun(Object a) {if(a==null) { throw new Exception();} System.out.println('c'); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a"),
        element(Tree.Kind.NULL_LITERAL),
        element(EQUAL_TO)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(1, 2),
      block(
        element(Tree.Kind.NEW_CLASS)).terminator(Tree.Kind.THROW_STATEMENT).successors(0),
      block(
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.CHAR_LITERAL, "'c'"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void synchronized_statement() {
    final CFG cfg = buildCFG("void fun(Object a) {if(a==null) { synchronized(a) { foo();bar();} } System.out.println('c'); }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a"),
        element(Tree.Kind.NULL_LITERAL),
        element(EQUAL_TO)).terminator(Tree.Kind.IF_STATEMENT).successors(1, 3),
      block(
        element(Tree.Kind.IDENTIFIER, "a")).terminator(Tree.Kind.SYNCHRONIZED_STATEMENT).successors(2),
      block(
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.METHOD_INVOCATION),
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(1),
      block(
        element(Tree.Kind.IDENTIFIER, "System"),
        element(Tree.Kind.MEMBER_SELECT),
        element(Tree.Kind.CHAR_LITERAL, "'c'"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void multiple_constructions() {
    final CFG cfg = buildCFG("void fun(Object a) {if(a instanceof String) { Supplier<String> s = a::toString;foo(y -> y+1); a += (String) a;  } }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "a"),
        element(Tree.Kind.INSTANCE_OF)
        ).terminator(Tree.Kind.IF_STATEMENT).successors(0, 1),
      block(
        element(Kind.METHOD_REFERENCE),
        element(Kind.VARIABLE, "s"),
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Tree.Kind.LAMBDA_EXPRESSION),
        element(Tree.Kind.METHOD_INVOCATION),
        element(Tree.Kind.IDENTIFIER, "a"),
        element(Tree.Kind.IDENTIFIER, "a"),
        element(Tree.Kind.TYPE_CAST),
        element(Tree.Kind.PLUS_ASSIGNMENT)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void catching_class_cast_exception() {
    CFG cfg = buildCFG("String fun(Object a) {try {return (String) a;} catch(ClassCastException cce) { return null;} }");
    CFGChecker cfgChecker = checker(
      block(
        element(TRY_STATEMENT)
      ),
      block(
        element(Tree.Kind.IDENTIFIER, "a"),
        element(Tree.Kind.TYPE_CAST)
      ).successors(1, 2),
      terminator(RETURN_STATEMENT, 0).successorWithoutJump(0),
      block(
        element(Kind.VARIABLE, "cce"),
        element(NULL_LITERAL)
      ).successors(0)
    );
    cfgChecker.check(cfg);

  }

  @Test
  public void array_access_expression() {
    final CFG cfg = buildCFG("void fun(int[] array) { array[0] = 1; array[3+2] = 4; }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.IDENTIFIER, "array"),
        element(INT_LITERAL, 0),
        element(Tree.Kind.ARRAY_ACCESS_EXPRESSION),
        element(INT_LITERAL, 1),
        element(Tree.Kind.ASSIGNMENT),
        element(Tree.Kind.IDENTIFIER, "array"),
        element(INT_LITERAL, 3),
        element(INT_LITERAL, 2),
        element(Tree.Kind.PLUS),
        element(Tree.Kind.ARRAY_ACCESS_EXPRESSION),
        element(INT_LITERAL, 4),
        element(Tree.Kind.ASSIGNMENT)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void try_with_resource() throws Exception {
    final CFG cfg = buildCFG("void fun() { String path = \"\"; try (BufferedReader br = new BufferedReader(new FileReader(path))) {} }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Kind.STRING_LITERAL, ""),
        element(Kind.VARIABLE, "path"),
        element(Kind.TRY_STATEMENT)).successors(3),
      block(
        element(Kind.IDENTIFIER, "path"),
        element(Kind.NEW_CLASS)).successors(2).exceptions(0),
      block(
          element(Kind.NEW_CLASS)).successors(1).exceptions(0),
      block(
        element(Kind.VARIABLE, "br")).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void try_with_resource_java9() throws Exception {
    final CFG cfg = buildCFG("void fun() { final Resource r = new Resource(); try (r) {} }");
    final CFGChecker cfgChecker = checker(
      block(
        element(Kind.NEW_CLASS),
        element(Kind.VARIABLE, "r"),
        element(Kind.TRY_STATEMENT)).successors(1),
      block(
        element(Kind.IDENTIFIER, "r")).successors(0))
      ;
    cfgChecker.check(cfg);
  }

  @Test
  public void returnCascadedAnd() throws Exception {
    final CFG cfg = buildCFG(
      "boolean andAll(boolean a, boolean b, boolean c) { return a && b && c;}");
    final CFGChecker cfgChecker = checker(
      block(element(Kind.IDENTIFIER, "a")).terminator(Kind.CONDITIONAL_AND).ifTrue(4).ifFalse(3),
      block(element(Kind.IDENTIFIER, "b")).successors(3),
      terminator(Kind.CONDITIONAL_AND).ifTrue(2).ifFalse(1),
      block(element(Kind.IDENTIFIER, "c")).successors(1),
      terminator(Kind.RETURN_STATEMENT).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void returnCascadedOr() throws Exception {
    final CFG cfg = buildCFG(
      "boolean orAll(boolean a, boolean b, boolean c) { return a || b || c;}");
    final CFGChecker cfgChecker = checker(
      block(element(Kind.IDENTIFIER, "a")).terminator(Kind.CONDITIONAL_OR).ifTrue(3).ifFalse(4),
      block(element(Kind.IDENTIFIER, "b")).successors(3),
      terminator(Kind.CONDITIONAL_OR).ifTrue(1).ifFalse(2),
      block(element(Kind.IDENTIFIER, "c")).successors(1),
      terminator(Kind.RETURN_STATEMENT).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void complex_boolean_expression() throws Exception {
    final CFG cfg = buildCFG(" private boolean fun(boolean bool, boolean a, boolean b) {\n" +
        "    return (!bool && a) || (bool && b);\n" +
        "  }");
    final CFGChecker cfgChecker = checker(
        block(
            element(Kind.IDENTIFIER, "bool"),
            element(Kind.LOGICAL_COMPLEMENT)
        ).terminator(Kind.CONDITIONAL_AND).ifTrue(5).ifFalse(4),
        block(element(Kind.IDENTIFIER, "a")).successors(4),
        terminator(Kind.CONDITIONAL_OR).ifTrue(1).ifFalse(3),
        block(element(Kind.IDENTIFIER, "bool")).terminator(Kind.CONDITIONAL_AND).ifTrue(2).ifFalse(1),
        block(element(Kind.IDENTIFIER, "b")).successors(1),
        terminator(Kind.RETURN_STATEMENT).successors(0));
    cfgChecker.check(cfg);

  }

  @Test
  public void method_reference() throws Exception {
    final CFG cfg = buildCFG("void fun() { foo(Object::toString); }");
    final CFGChecker cfgChecker = checker(
        block(
          element(Kind.IDENTIFIER, "foo"),
          element(Kind.METHOD_REFERENCE),
            element(Kind.METHOD_INVOCATION)
        ).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void try_statement_with_CFG_blocks() {
    // method invocation after if
    CFG cfg = buildCFG(
      "  private void f(boolean action) {\n" +
        "    try {\n" +
        "    if (action) {" +
        "       performAction();" +
        "    }" +
        "    doSomething();" +
        "} catch(Exception e) { foo();} bar(); }");
    CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.TRY_STATEMENT)).successors(5),
      block(
        element(Tree.Kind.IDENTIFIER, "action")).terminator(Kind.IF_STATEMENT).successors(3, 4),
      block(
        element(Tree.Kind.IDENTIFIER, "performAction"),
        element(Kind.METHOD_INVOCATION)).successors(3).exceptions(0, 2).exit(0),
      block(
        element(Tree.Kind.IDENTIFIER, "doSomething"),
        element(Kind.METHOD_INVOCATION)).successors(1).exceptions(0, 2).exit(0),
      block(
        element(Kind.VARIABLE, "e"),
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Kind.METHOD_INVOCATION)).successors(1).exceptions(0).exit(0),
      block(
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Kind.METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);

    // method invocation before if
    cfg = buildCFG(
      "  private void f(boolean action) {\n" +
        "    try {\n" +
        "    doSomething();" +
        "    if (action) {" +
        "       performAction();" +
        "    }" +
        "} catch(Exception e) { foo();} bar(); }");
    cfgChecker = checker(
      block(
        element(Tree.Kind.TRY_STATEMENT)).successors(5),
      block(
        element(Tree.Kind.IDENTIFIER, "doSomething"),
        element(Kind.METHOD_INVOCATION)).successors(4).exceptions(0, 2).exit(0),
      block(
        element(Tree.Kind.IDENTIFIER, "action")).terminator(Kind.IF_STATEMENT).successors(1, 3),
      block(
        element(Tree.Kind.IDENTIFIER, "performAction"),
        element(Kind.METHOD_INVOCATION)).successors(1).exceptions(0, 2),
      block(
        element(Kind.VARIABLE, "e"),
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Kind.METHOD_INVOCATION)).successors(1).exceptions(0),
      block(
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Kind.METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);

    // finally
    cfg = buildCFG(
      "  private void f(boolean action) {\n" +
        "    try {\n" +
        "    if (action) {" +
        "       performAction();" +
        "    }" +
        "    doSomething();" +
        "} finally { foo();} bar(); }");
    cfgChecker = checker(
      block(
        element(Tree.Kind.TRY_STATEMENT)).successors(5),
      block(
        element(Tree.Kind.IDENTIFIER, "action")).terminator(Kind.IF_STATEMENT).successors(3, 4),
      block(
        element(Tree.Kind.IDENTIFIER, "performAction"),
        element(Kind.METHOD_INVOCATION)).successors(3).exceptions(2),
      block(
        element(Tree.Kind.IDENTIFIER, "doSomething"),
        element(Kind.METHOD_INVOCATION)).successors(2).exceptions(2),
      block(
        element(Tree.Kind.IDENTIFIER, "foo"),
        element(Kind.METHOD_INVOCATION)).successors(0, 1),
      block(
        element(Tree.Kind.IDENTIFIER, "bar"),
        element(Kind.METHOD_INVOCATION)).successors(0));
    cfgChecker.check(cfg);
  }

  @Test
  public void try_statement_with_checked_exceptions() {
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
        element(Tree.Kind.TRY_STATEMENT)).successors(7),
      block(
        element(Kind.NEW_CLASS)).successors(1).exceptions(0, 6),
      block(
        element(Kind.VARIABLE, "iae"),
        element(Tree.Kind.TRY_STATEMENT)).successors(5).isCatchBlock(),
      block(
        element(Kind.NEW_CLASS)).successors(3).exceptions(0, 4),
      block(
        element(Kind.VARIABLE, "iae2")
      ).successors(2),
      block(
        element(Kind.ASSIGNMENT)
      ).successors(2),
      block(
        element(Kind.IDENTIFIER, "result"),
        element(Kind.METHOD_INVOCATION)
        ).successors(0).exceptions(0),
      block(
        element(Kind.ASSIGNMENT)
      ).successors(0)
      );
    cfgChecker.check(cfg);
  }

  @Test
  public void try_statement_with_runtime_exceptions() {
    CFG cfg = buildCFG(new File("src/test/files/cfg/CFGRuntimeExceptions.java"));
    CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.TRY_STATEMENT)).successors(9),
      block(
        element(Tree.Kind.IDENTIFIER, "doSomething"),
        // trigger runtime exception -> branch to:
        // B8 : RuntimeException
        // B7 : Subtype of RuntimeException
        // B6 : Error
        // B5 : Subtype of Error
        // B4 : Throwable
        // B3 : Subtype of Throwable but not subtype of Exception
        // B2 : Exception
        element(Tree.Kind.METHOD_INVOCATION)).successors(0).exceptions(8, 7, 6, 5, 4, 3, 2, 0),
      block(
        element(Tree.Kind.VARIABLE, "re"),
        element(Tree.Kind.IDENTIFIER, "doSomethingElse"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock(),
      block(
        element(Tree.Kind.VARIABLE, "mre"),
        element(Tree.Kind.IDENTIFIER, "doSomethingElse"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock(),
      block(
        element(Tree.Kind.VARIABLE, "er"),
        element(Tree.Kind.IDENTIFIER, "doSomethingElse"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock(),
      block(
        element(Tree.Kind.VARIABLE, "mer"),
        element(Tree.Kind.IDENTIFIER, "doSomethingElse"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock(),
      block(
        element(Tree.Kind.VARIABLE, "t"),
        element(Tree.Kind.IDENTIFIER, "doSomethingElse"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock(),
      block(
        element(Tree.Kind.VARIABLE, "mt"),
        element(Tree.Kind.IDENTIFIER, "doSomethingElse"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock(),
      block(
        element(Tree.Kind.VARIABLE, "ex"),
        element(Tree.Kind.IDENTIFIER, "doSomethingElse"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock(),
      block(
        // no way to enter the block (checked Exception)
        element(Tree.Kind.VARIABLE, "mex"),
        element(Tree.Kind.IDENTIFIER, "doNothing"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0).exceptions(0).isCatchBlock());
    cfgChecker.check(cfg);
  }

  @Test
  public void catch_block_correctly_flagged_in_CFG() {
    CFG cfg = buildCFG(new File("src/test/files/cfg/CFGCatchBlocks.java"));

    CFGChecker cfgChecker = checker(
      block(
        element(Tree.Kind.TRY_STATEMENT)).successors(8),
      block(
        element(Tree.Kind.IDENTIFIER, "m1"),
        element(Tree.Kind.IDENTIFIER, "o1"),
        element(Tree.Kind.IDENTIFIER, "o2"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(1).exceptions(7, 5, 3, 1),
      block(
        element(Tree.Kind.VARIABLE, "e"),
        element(Tree.Kind.IDENTIFIER, "m2"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(6).exceptions(1).isCatchBlock(),
      block(
        element(Tree.Kind.IDENTIFIER, "m3"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(1).exceptions(1),
      block(
        element(Tree.Kind.VARIABLE, "e"),
        element(Tree.Kind.IDENTIFIER, "o2"),
        element(Tree.Kind.NULL_LITERAL),
        element(Tree.Kind.EQUAL_TO)).terminator(Tree.Kind.IF_STATEMENT).ifTrue(4).ifFalse(1).isCatchBlock(),
      block(
        element(Tree.Kind.IDENTIFIER, "m4"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(1).exceptions(1),
      block(
        element(Tree.Kind.VARIABLE, "e"),
        element(Tree.Kind.IDENTIFIER, "m5"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(2).exceptions(1).isCatchBlock(),
      block(
        element(Tree.Kind.VARIABLE, "res")).successors(1),
      block(
        element(Tree.Kind.IDENTIFIER, "m6"),
        element(Tree.Kind.METHOD_INVOCATION)).successors(0).isFinallyBlock());
    cfgChecker.check(cfg);
  }

  @Test
  public void successor_of_labeled_break_statement() throws Exception {
    CFG cfg = buildCFG("private static void test(long toRevision, boolean inverted, Object visitor) {\n" +
      "\n" +
      "    testBlock: {\n" +
      "      if (inverted) \n" +
      "        break testBlock;\n" +
      "      test(0, false ? inverted : !inverted, visitor);\n" +
      "    }\n" +
      "  }");
    CFGChecker cfgChecker = checker(
      block(
        element(Kind.IDENTIFIER, "inverted")
      ).terminator(Kind.IF_STATEMENT)
        .ifTrue(5)
        .ifFalse(4),
      terminator(Kind.BREAK_STATEMENT).successors(0),
      block(
        element(Kind.IDENTIFIER, "test"),
        element(INT_LITERAL, 0),
        element(Kind.BOOLEAN_LITERAL, "false")
        ).terminator(Kind.CONDITIONAL_EXPRESSION)
        .ifTrue(3)
        .ifFalse(2),
      block(
        element(Kind.IDENTIFIER, "inverted")
      ).successors(1),
      block(
        element(Kind.IDENTIFIER, "inverted"),
        element(Kind.LOGICAL_COMPLEMENT)
      ).successors(1),
      block(
        element(Kind.IDENTIFIER, "visitor"),
        element(Kind.METHOD_INVOCATION)
      ).successors(0)
    );
    cfgChecker.check(cfg);

  }

  @Test
  public void test_chained_method_invocation() {
    CFG cfg = buildCFG("private void foo(Object p) {\n" +
      "    if(p == null) {\n" +
      "      NullArrayAccess\n" +
      "        .method(p.toString())\n" +
      "        .method2(p.hashCode());\n" +
      "    }\n" +
      "  }");
    CFGChecker cfgChecker = checker(
      block(
        element(IDENTIFIER, "p"),
        element(NULL_LITERAL),
        element(EQUAL_TO)
      ).terminator(Kind.IF_STATEMENT)
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
  public void constructor_arguments_order() throws Exception {
    CFG cfg = buildCFG("private void foo(Exception e) {\n" +
      "throw new IllegalArgumentException(\"iae\", e);\n" +
      "} "
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
  public void array_dim_initializer_order() throws Exception {
    CFG cfg = buildCFG("private void fun() {\n" +
      "String[] plop = {foo(), bar()};\n" +
      "String[][] plop2 = new String[qix()][baz()];\n" +
      "} "
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
  public void assert_statement() throws Exception {
    CFG cfg = buildCFG("private void fun(boolean x) {\n" +
      "assert x;\n" +
      "} "
    );
    CFGChecker cfgChecker = checker(
      block(element(IDENTIFIER, "x"),
        element(ASSERT_STATEMENT))
    );
    cfgChecker.check(cfg);

  }

  @Test
  public void exception_raised_in_catch() throws Exception {
    CFG cfg = buildCFG("private void fun() {\n" +
      "     try {\n" +
      "      try {\n" +
      "        f();\n" +
      "      } catch (Exception e) {\n" +
      "        ex();\n" +
      "      } finally {\n" +
      "        fin();\n" +
      "      }\n" +
      "    } catch (Exception e) {\n" +
      "      outEx();\n" +
      "    }\n"+
      "} "
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
  public void break_in_nested_catch() {
    CFG cfg = buildCFG(
      "  void foo(boolean a) {\n" +
      "    String[] types = new String[12];\n" +
      "    try {\n" +
      "      invoke0();\n" +
      "    for (int i = 0; i < files.length; i++) {\n" +
      "      A file = files[i];\n" +
      "      try{\n" +
      "        invoke1();\n" +
      "      }catch(Throwable e) {\n" +
      "        invoke2();\n" +
      "        invoke3();\n" +
      "        break;\n" +
      "      } finally {\n" +
      "        types[i] = invoke4();\n" +
      "      }\n" +
      "    }\n" +
      "    } finally {\n" +
      "      invoke10();\n" +
      "      invoke11();\n" +
      "    }\n" +
      "    \n" +
      "  }\n");
    assertThat(CFGDebug.toString(cfg)).isEqualTo("Starts at B13\n" +
      "\n" +
      "B13\n" +
      "0:\tINT_LITERAL                         \t12\n" +
      "1:\tNEW_ARRAY                           \tnew []\n" +
      "2:\tVARIABLE                            \ttypes\n" +
      "3:\tTRY_STATEMENT                       \t\n" +
      "\tjumps to: B12\n" +
      "\n" +
      "B12\n" +
      "0:\tIDENTIFIER                          \tinvoke0\n" +
      "1:\tMETHOD_INVOCATION                   \tinvoke0()\n" +
      "\tjumps to: B11\n" +
      "\texceptions to: B1\n" +
      "\n" +
      "B11\n" +
      "0:\tINT_LITERAL                         \t0\n" +
      "1:\tVARIABLE                            \ti\n" +
      "\tjumps to: B10\n" +
      "\n" +
      "B10\n" +
      "0:\tIDENTIFIER                          \ti\n" +
      "1:\tIDENTIFIER                          \tfiles\n" +
      "2:\tMEMBER_SELECT                       \tfiles.length\n" +
      "3:\tLESS_THAN                           \ti < files.length\n" +
      "T:\tFOR_STATEMENT                       \tfor {i;i < files.length;i++}\n" +
      "\tjumps to: B9(true) B1(false)\n" +
      "\n" +
      "B9\n" +
      "0:\tIDENTIFIER                          \tfiles\n" +
      "1:\tIDENTIFIER                          \ti\n" +
      "2:\tARRAY_ACCESS_EXPRESSION             \tfiles[i]\n" +
      "3:\tVARIABLE                            \tfile\n" +
      "4:\tTRY_STATEMENT                       \t\n" +
      "\tjumps to: B8\n" +
      "\n" +
      "B8\n" +
      "0:\tIDENTIFIER                          \tinvoke1\n" +
      "1:\tMETHOD_INVOCATION                   \tinvoke1()\n" +
      "\tjumps to: B4\n" +
      "\texceptions to: B4 B7\n" +
      "\n" +
      "B7\n" +
      "0:\tVARIABLE                            \te\n" +
      "1:\tIDENTIFIER                          \tinvoke2\n" +
      "2:\tMETHOD_INVOCATION                   \tinvoke2()\n" +
      "\tjumps to: B6\n" +
      "\texceptions to: B4\n" +
      "\n" +
      "B6\n" +
      "0:\tIDENTIFIER                          \tinvoke3\n" +
      "1:\tMETHOD_INVOCATION                   \tinvoke3()\n" +
      "\tjumps to: B5\n" +
      "\texceptions to: B4\n" +
      "\n" +
      "B5\n" +
      "T:\tBREAK_STATEMENT                     \tbreak\n" +
      "\tjumps to: B4\n" +
      "\n" +
      "B4\n" +
      "0:\tIDENTIFIER                          \ttypes\n" +
      "1:\tIDENTIFIER                          \ti\n" +
      "2:\tARRAY_ACCESS_EXPRESSION             \ttypes[i]\n" +
      "3:\tIDENTIFIER                          \tinvoke4\n" +
      "4:\tMETHOD_INVOCATION                   \tinvoke4()\n" +
      "\tjumps to: B3\n" +
      "\texceptions to: B1\n" +
      "\n" +
      "B3\n" +
      "0:\tASSIGNMENT                          \ttypes[i]=invoke4()\n" +
      "\tjumps to: B2 B1(exit)\n" +
      "\n" +
      "B2\n" +
      "0:\tIDENTIFIER                          \ti\n" +
      "1:\tPOSTFIX_INCREMENT                   \ti++\n" +
      "\tjumps to: B10\n" +
      "\n" +
      "B1\n" +
      "0:\tIDENTIFIER                          \tinvoke10\n" +
      "1:\tMETHOD_INVOCATION                   \tinvoke10()\n" +
      "2:\tIDENTIFIER                          \tinvoke11\n" +
      "3:\tMETHOD_INVOCATION                   \tinvoke11()\n" +
      "\tjumps to: B0(exit)\n" +
      "\n" +
      "B0 (Exit):\n" +
      "\n");
  }

  @Test
  public void break_in_try_finally_within_while() {
    CFG cfg = buildCFG("void run1() {\n" +
      "    while (true) {\n" +
      "      try {\n" +
      "        break;\n" +
      "      } finally {\n" +
      "        String s = true ? \"trueLiteral\" : \"falseLiteral\";\n" +
      "        System.out.println(s);\n" +
      "      }\n" +
      "    }\n" +
      "  }");
    assertThat(CFGDebug.toString(cfg)).isEqualTo("Starts at B7\n" +
      "\n" +
      "B7\n" +
      "0:\tBOOLEAN_LITERAL                     \ttrue\n" +
      "T:\tWHILE_STATEMENT                     \twhile (true)\n" +
      "\tjumps to: B6(true) B0(false)\n" +
      "\n" +
      "B6\n" +
      "0:\tTRY_STATEMENT                       \t\n" +
      "\tjumps to: B5\n" +
      "\n" +
      "B5\n" +
      "T:\tBREAK_STATEMENT                     \tbreak\n" +
      "\tjumps to: B4\n" +
      "\n" +
      "B4\n" +
      "0:\tBOOLEAN_LITERAL                     \ttrue\n" +
      "T:\tCONDITIONAL_EXPRESSION              \ttrue ? \"trueLiteral\" : \"falseLiteral\"\n" +
      "\tjumps to: B3(true) B2(false)\n" +
      "\n" +
      "B3\n" +
      "0:\tSTRING_LITERAL                      \t\"trueLiteral\"\n" +
      "\tjumps to: B1\n" +
      "\n" +
      "B2\n" +
      "0:\tSTRING_LITERAL                      \t\"falseLiteral\"\n" +
      "\tjumps to: B1\n" +
      "\n" +
      "B1\n" +
      "0:\tVARIABLE                            \ts\n" +
      "1:\tIDENTIFIER                          \tSystem\n" +
      "2:\tMEMBER_SELECT                       \tSystem.out\n" +
      "3:\tIDENTIFIER                          \ts\n" +
      "4:\tMETHOD_INVOCATION                   \t.println(s)\n" +
      "\tjumps to: B7 B0(exit)\n" +
      "\n" +
      "B0 (Exit):\n\n");
  }
  @Test
  public void continue_in_try_finally_within_while() {
    CFG cfg = buildCFG("void run2() {\n" +
      "    while (true) {\n" +
      "      try {\n" +
      "        continue;\n" +
      "      } finally {\n" +
      "        System.out.println(true ? \"trueLiteral\" : \"falseLiteral\");\n" +
      "      }\n" +
      "    }\n" +
      "  }");
    assertThat(CFGDebug.toString(cfg)).isEqualTo("Starts at B7\n" +
      "\n" +
      "B7\n" +
      "0:\tBOOLEAN_LITERAL                     \ttrue\n" +
      "T:\tWHILE_STATEMENT                     \twhile (true)\n" +
      "\tjumps to: B6(true) B0(false)\n" +
      "\n" +
      "B6\n" +
      "0:\tTRY_STATEMENT                       \t\n" +
      "\tjumps to: B5\n" +
      "\n" +
      "B5\n" +
      "T:\tCONTINUE_STATEMENT                  \tcontinue\n" +
      "\tjumps to: B4\n" +
      "\n" +
      "B4\n" +
      "0:\tIDENTIFIER                          \tSystem\n" +
      "1:\tMEMBER_SELECT                       \tSystem.out\n" +
      "2:\tBOOLEAN_LITERAL                     \ttrue\n" +
      "T:\tCONDITIONAL_EXPRESSION              \ttrue ? \"trueLiteral\" : \"falseLiteral\"\n" +
      "\tjumps to: B3(true) B2(false)\n" +
      "\n" +
      "B3\n" +
      "0:\tSTRING_LITERAL                      \t\"trueLiteral\"\n" +
      "\tjumps to: B1\n" +
      "\n" +
      "B2\n" +
      "0:\tSTRING_LITERAL                      \t\"falseLiteral\"\n" +
      "\tjumps to: B1\n" +
      "\n" +
      "B1\n" +
      "0:\tMETHOD_INVOCATION                   \t.println(true ? \"trueLiteral\" : \"falseLiteral\")\n" +
      "\tjumps to: B7 B0(exit)\n" +
      "\n" +
      "B0 (Exit):\n\n");
  }

  @Test
  public void break_in_try_finally_within_for() {
    CFG cfg = buildCFG(" void run3() {\n" +
      "    for (int i = 0; i < 5; i++) {\n" +
      "      try {\n" +
      "        break;\n" +
      "      } finally {\n" +
      "        String s;\n" +
      "        System.out.println(true ? \"trueLiteral\" : \"falseLiteral\");\n" +
      "      }\n" +
      "    }\n" +
      "  }\n");
    assertThat(CFGDebug.toString(cfg)).isEqualTo("Starts at B9\n" +
      "\n" +
      "B9\n" +
      "0:\tINT_LITERAL                         \t0\n" +
      "1:\tVARIABLE                            \ti\n" +
      "\tjumps to: B8\n" +
      "\n" +
      "B8\n" +
      "0:\tIDENTIFIER                          \ti\n" +
      "1:\tINT_LITERAL                         \t5\n" +
      "2:\tLESS_THAN                           \ti < 5\n" +
      "T:\tFOR_STATEMENT                       \tfor {i;i < 5;i++}\n" +
      "\tjumps to: B7(true) B0(false)\n" +
      "\n" +
      "B7\n" +
      "0:\tTRY_STATEMENT                       \t\n" +
      "\tjumps to: B6\n" +
      "\n" +
      "B6\n" +
      "T:\tBREAK_STATEMENT                     \tbreak\n" +
      "\tjumps to: B5\n" +
      "\n" +
      "B5\n" +
      "0:\tVARIABLE                            \ts\n" +
      "1:\tIDENTIFIER                          \tSystem\n" +
      "2:\tMEMBER_SELECT                       \tSystem.out\n" +
      "3:\tBOOLEAN_LITERAL                     \ttrue\n" +
      "T:\tCONDITIONAL_EXPRESSION              \ttrue ? \"trueLiteral\" : \"falseLiteral\"\n" +
      "\tjumps to: B4(true) B3(false)\n" +
      "\n" +
      "B4\n" +
      "0:\tSTRING_LITERAL                      \t\"trueLiteral\"\n" +
      "\tjumps to: B2\n" +
      "\n" +
      "B3\n" +
      "0:\tSTRING_LITERAL                      \t\"falseLiteral\"\n" +
      "\tjumps to: B2\n" +
      "\n" +
      "B2\n" +
      "0:\tMETHOD_INVOCATION                   \t.println(true ? \"trueLiteral\" : \"falseLiteral\")\n" +
      "\tjumps to: B1 B0(exit)\n" +
      "\n" +
      "B1\n" +
      "0:\tIDENTIFIER                          \ti\n" +
      "1:\tPOSTFIX_INCREMENT                   \ti++\n" +
      "\tjumps to: B8\n" +
      "\n" +
      "B0 (Exit):\n\n");
  }
  @Test
  public void break_in_try_and_complex_finally_within_while() {
    CFG cfg = buildCFG(" void run4() {\n" +
      "    while (true) {\n" +
      "      try {\n" +
      "        break;\n" +
      "      } finally {\n" +
      "        String s;\n" +
      "        if (true) { s = \"trueLiteral\"; } else { s = \"falseLiteral\"; }\n" +
      "        System.out.println(s);\n" +
      "      }\n" +
      "    }\n" +
      "  }");
    assertThat(CFGDebug.toString(cfg)).isEqualTo("Starts at B7\n" +
      "\n" +
      "B7\n" +
      "0:\tBOOLEAN_LITERAL                     \ttrue\n" +
      "T:\tWHILE_STATEMENT                     \twhile (true)\n" +
      "\tjumps to: B6(true) B0(false)\n" +
      "\n" +
      "B6\n" +
      "0:\tTRY_STATEMENT                       \t\n" +
      "\tjumps to: B5\n" +
      "\n" +
      "B5\n" +
      "T:\tBREAK_STATEMENT                     \tbreak\n" +
      "\tjumps to: B4\n" +
      "\n" +
      "B4\n" +
      "0:\tVARIABLE                            \ts\n" +
      "1:\tBOOLEAN_LITERAL                     \ttrue\n" +
      "T:\tIF_STATEMENT                        \tif (true)\n" +
      "\tjumps to: B3(true) B2(false)\n" +
      "\n" +
      "B3\n" +
      "0:\tSTRING_LITERAL                      \t\"trueLiteral\"\n" +
      "1:\tASSIGNMENT                          \ts=\"trueLiteral\"\n" +
      "\tjumps to: B1\n" +
      "\n" +
      "B2\n" +
      "0:\tSTRING_LITERAL                      \t\"falseLiteral\"\n" +
      "1:\tASSIGNMENT                          \ts=\"falseLiteral\"\n" +
      "\tjumps to: B1\n" +
      "\n" +
      "B1\n" +
      "0:\tIDENTIFIER                          \tSystem\n" +
      "1:\tMEMBER_SELECT                       \tSystem.out\n" +
      "2:\tIDENTIFIER                          \ts\n" +
      "3:\tMETHOD_INVOCATION                   \t.println(s)\n" +
      "\tjumps to: B7 B0(exit)\n" +
      "\n" +
      "B0 (Exit):\n\n");
  }

  @Test
  public void break_without_finally() {
    CFG cfg = buildCFG("void fun(int highestLevel) {       while (highestLevel >= lowestOddLevel) {\n" +
      "            int i = levelStart;\n" +
      "\n" +
      "            for (;;) {\n" +
      "                while (i < levelLimit && levels[i] < highestLevel) {\n" +
      "                    i++;\n" +
      "                }\n" +
      "                int begin = i++;\n" +
      "\n" +
      "                if (begin == levelLimit) {\n" +
      "                    break; // no more runs at this level\n" +
      "                }\n" +
      "\n" +
      "                while (i < levelLimit && levels[i] >= highestLevel) {\n" +
      "                    i++;\n" +
      "                }\n" +
      "                int end = i - 1;\n" +
      "\n" +
      "\t\tbegin += delta;\n" +
      "\t\tend += delta;\n" +
      "                while (begin < end) {\n" +
      "                    Object temp = objects[begin];\n" +
      "                    objects[begin] = objects[end];\n" +
      "                    objects[end] = temp;\n" +
      "                    ++begin;\n" +
      "                    --end;\n" +
      "                }\n" +
      "            }\n" +
      "\n" +
      "            --highestLevel;\n" +
      "        }}");
    assertThat(CFGDebug.toString(cfg)).isEqualTo("Starts at B15\n" +
      "\n" +
      "B15\n" +
      "0:\tIDENTIFIER                          \thighestLevel\n" +
      "1:\tIDENTIFIER                          \tlowestOddLevel\n" +
      "2:\tGREATER_THAN_OR_EQUAL_TO            \thighestLevel >= lowestOddLevel\n" +
      "T:\tWHILE_STATEMENT                     \twhile (highestLevel >= lowestOddLevel)\n" +
      "\tjumps to: B14(true) B0(false)\n" +
      "\n" +
      "B14\n" +
      "0:\tIDENTIFIER                          \tlevelStart\n" +
      "1:\tVARIABLE                            \ti\n" +
      "\tjumps to: B13\n" +
      "\n" +
      "B13\n" +
      "T:\tFOR_STATEMENT                       \tfor {;;}\n" +
      "\tjumps to: B12\n" +
      "\n" +
      "B12\n" +
      "0:\tIDENTIFIER                          \ti\n" +
      "1:\tIDENTIFIER                          \tlevelLimit\n" +
      "2:\tLESS_THAN                           \ti < levelLimit\n" +
      "T:\tCONDITIONAL_AND                     \ti < levelLimit && levels[i] < highestLevel\n" +
      "\tjumps to: B11(true) B9(false)\n" +
      "\n" +
      "B11\n" +
      "0:\tIDENTIFIER                          \tlevels\n" +
      "1:\tIDENTIFIER                          \ti\n" +
      "2:\tARRAY_ACCESS_EXPRESSION             \tlevels[i]\n" +
      "3:\tIDENTIFIER                          \thighestLevel\n" +
      "4:\tLESS_THAN                           \tlevels[i] < highestLevel\n" +
      "T:\tWHILE_STATEMENT                     \twhile (i < levelLimit && levels[i] < highestLevel)\n" +
      "\tjumps to: B10(true) B9(false)\n" +
      "\n" +
      "B10\n" +
      "0:\tIDENTIFIER                          \ti\n" +
      "1:\tPOSTFIX_INCREMENT                   \ti++\n" +
      "\tjumps to: B12\n" +
      "\n" +
      "B9\n" +
      "0:\tIDENTIFIER                          \ti\n" +
      "1:\tPOSTFIX_INCREMENT                   \ti++\n" +
      "2:\tVARIABLE                            \tbegin\n" +
      "3:\tIDENTIFIER                          \tbegin\n" +
      "4:\tIDENTIFIER                          \tlevelLimit\n" +
      "5:\tEQUAL_TO                            \tbegin == levelLimit\n" +
      "T:\tIF_STATEMENT                        \tif (begin == levelLimit)\n" +
      "\tjumps to: B8(true) B7(false)\n" +
      "\n" +
      "B8\n" +
      "T:\tBREAK_STATEMENT                     \tbreak\n" +
      "\tjumps to: B1\n" +
      "\n" +
      "B7\n" +
      "0:\tIDENTIFIER                          \ti\n" +
      "1:\tIDENTIFIER                          \tlevelLimit\n" +
      "2:\tLESS_THAN                           \ti < levelLimit\n" +
      "T:\tCONDITIONAL_AND                     \ti < levelLimit && levels[i] >= highestLevel\n" +
      "\tjumps to: B6(true) B4(false)\n" +
      "\n" +
      "B6\n" +
      "0:\tIDENTIFIER                          \tlevels\n" +
      "1:\tIDENTIFIER                          \ti\n" +
      "2:\tARRAY_ACCESS_EXPRESSION             \tlevels[i]\n" +
      "3:\tIDENTIFIER                          \thighestLevel\n" +
      "4:\tGREATER_THAN_OR_EQUAL_TO            \tlevels[i] >= highestLevel\n" +
      "T:\tWHILE_STATEMENT                     \twhile (i < levelLimit && levels[i] >= highestLevel)\n" +
      "\tjumps to: B5(true) B4(false)\n" +
      "\n" +
      "B5\n" +
      "0:\tIDENTIFIER                          \ti\n" +
      "1:\tPOSTFIX_INCREMENT                   \ti++\n" +
      "\tjumps to: B7\n" +
      "\n" +
      "B4\n" +
      "0:\tIDENTIFIER                          \ti\n" +
      "1:\tINT_LITERAL                         \t1\n" +
      "2:\tMINUS                               \ti - 1\n" +
      "3:\tVARIABLE                            \tend\n" +
      "4:\tIDENTIFIER                          \tbegin\n" +
      "5:\tIDENTIFIER                          \tdelta\n" +
      "6:\tPLUS_ASSIGNMENT                     \tbegin+=delta\n" +
      "7:\tIDENTIFIER                          \tend\n" +
      "8:\tIDENTIFIER                          \tdelta\n" +
      "9:\tPLUS_ASSIGNMENT                     \tend+=delta\n" +
      "\tjumps to: B3\n" +
      "\n" +
      "B3\n" +
      "0:\tIDENTIFIER                          \tbegin\n" +
      "1:\tIDENTIFIER                          \tend\n" +
      "2:\tLESS_THAN                           \tbegin < end\n" +
      "T:\tWHILE_STATEMENT                     \twhile (begin < end)\n" +
      "\tjumps to: B13(false) B2(true)\n" +
      "\n" +
      "B2\n" +
      "0:\tIDENTIFIER                          \tobjects\n" +
      "1:\tIDENTIFIER                          \tbegin\n" +
      "2:\tARRAY_ACCESS_EXPRESSION             \tobjects[begin]\n" +
      "3:\tVARIABLE                            \ttemp\n" +
      "4:\tIDENTIFIER                          \tobjects\n" +
      "5:\tIDENTIFIER                          \tbegin\n" +
      "6:\tARRAY_ACCESS_EXPRESSION             \tobjects[begin]\n" +
      "7:\tIDENTIFIER                          \tobjects\n" +
      "8:\tIDENTIFIER                          \tend\n" +
      "9:\tARRAY_ACCESS_EXPRESSION             \tobjects[end]\n" +
      "10:\tASSIGNMENT                          \tobjects[begin]=objects[end]\n" +
      "11:\tIDENTIFIER                          \tobjects\n" +
      "12:\tIDENTIFIER                          \tend\n" +
      "13:\tARRAY_ACCESS_EXPRESSION             \tobjects[end]\n" +
      "14:\tIDENTIFIER                          \ttemp\n" +
      "15:\tASSIGNMENT                          \tobjects[end]=temp\n" +
      "16:\tIDENTIFIER                          \tbegin\n" +
      "17:\tPREFIX_INCREMENT                    \t++begin\n" +
      "18:\tIDENTIFIER                          \tend\n" +
      "19:\tPREFIX_DECREMENT                    \t--end\n" +
      "\tjumps to: B3\n" +
      "\n" +
      "B1\n" +
      "0:\tIDENTIFIER                          \thighestLevel\n" +
      "1:\tPREFIX_DECREMENT                    \t--highestLevel\n" +
      "\tjumps to: B15\n" +
      "\n" +
      "B0 (Exit):\n\n");

  }

  @Test
  public void break_in_try_finally_within_loop_do_not_always_lead_to_exit() {
    CFG cfg = buildCFG("void test() {\n" +
      "    RuntimeException e = null;\n" +
      "    for (int i = 0; i < 2; ) {\n" +
      "      try {\n" +
      "        e = new RuntimeException();\n" +
      "        break;\n" +
      "      } finally {\n" +
      "        doSomething();\n" +
      "      }\n" +
      "    }\n" +
      "    throw e;\n" +
      "  }");

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
        element(Tree.Kind.LESS_THAN)
      ).terminator(Tree.Kind.FOR_STATEMENT).successors(1, 5),
      block(element(TRY_STATEMENT)).successors(4),
      block(element(NEW_CLASS)).successors(3).exceptions(2),
      block(
        element(Tree.Kind.ASSIGNMENT)
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
  public void break_in_try_finally_in_for_without_condition() {
    CFG cfg = buildCFG("void test() {\n" +
      "    RuntimeException e = null;\n" +
      "    for (;;) {\n" +
      "      try {\n" +
      "        e = new RuntimeException();\n" +
      "        break;\n" +
      "      } finally {\n" +
      "        doSomething();\n" +
      "      }\n" +
      "    }\n" +
      "    throw e;\n" +
      "  }");

    CFGChecker cfgChecker = checker(
      block(
        element(NULL_LITERAL),
        element(VARIABLE, "e")
      ).successors(6),
      terminator(Tree.Kind.FOR_STATEMENT).successors(5),
      block(element(TRY_STATEMENT)).successors(4),
      block(element(NEW_CLASS)).successors(3).exceptions(2),
      block(
        element(Tree.Kind.ASSIGNMENT)
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
  public void throw_statement_within_try_catch() {
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
  public void build_partial_cfg_with_break() throws Exception {
    build_partial_cfg("break");
  }

  @Test
  public void build_partial_cfg_with_continue() throws Exception {
    build_partial_cfg("continue");
  }

  @Test
  public void connect_catch_blocks_with_unknown_exception_types() throws Exception {
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
  public void connect_catch_blocks_with_unknown_exception_types2() throws Exception {
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

  private void build_partial_cfg(String breakOrContinue) {
    String methodCode = "void meth(){ try {fun(); } catch ( Exception e) {e.printStackTrace(); "+breakOrContinue+"; } }";
    CompilationUnitTree cut = (CompilationUnitTree) parser.parse("class A {" + methodCode + "}");
    SemanticModel.createFor(cut, new SquidClassLoader(Collections.emptyList()));
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
