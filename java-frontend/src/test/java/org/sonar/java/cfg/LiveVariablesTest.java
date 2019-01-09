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
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LiveVariablesTest {

  public static final ActionParser<Tree> PARSER = JavaParser.createParser();

  private static CFG buildCFG(String methodCode) {
    CompilationUnitTree cut = (CompilationUnitTree) PARSER.parse("class A { int field1; int field2; static int staticField; " + methodCode + " }");
    SemanticModel.createFor(cut, new SquidClassLoader(Collections.emptyList()));
    MethodTree tree = ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(3));
    return CFG.build(tree);
  }

  @Test
  public void test_simple_live() {
    CFG cfg = buildCFG("void foo(int a) {  int i; /* should be live here */ if (false) ; foo(i); }");
    LiveVariables liveVariables = LiveVariables.analyze(cfg);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(3))).hasSize(1);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(3)).iterator().next().name()).isEqualTo("i");
  }

  @Test
  public void test_try_finally_liveness() throws Exception {
    CFG cfg = buildCFG("void foo() {   Object object = null;\n" +
      "    try {\n" +
      "      object = new Object();\n" +
      "    } catch (Exception e) {\n" +
      "      object.hashCode(); // Noncompliant\n" +
      "    } finally {\n" +
      "      object.hashCode();// Noncompliant\n" +
      "    } }");
    LiveVariables liveVariables = LiveVariables.analyze(cfg);
    cfg.reversedBlocks().stream().filter(block -> block.id() > 1).forEach(block -> assertThat(liveVariables.getOut(block)).as("Issue with block B" + block.id()).hasSize(1));
  }

  @Test
  public void test_simple_death()  {
    CFG cfg = buildCFG("void foo(int a) {  int i; /* should not be live here */ if (false) ; i = 0; }");
    LiveVariables liveVariables = LiveVariables.analyze(cfg);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(3))).isEmpty();
  }

  @Test
  public void test_field_not_tracked()  {
    CFG cfg = buildCFG("void foo(int a) { field = 0; /* fields should not be tracked */ if (false) ; foo(field); }");
    LiveVariables liveVariables = LiveVariables.analyze(cfg);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(3))).isEmpty();
    cfg = buildCFG("void foo(int a) { a = 0; /* but arguments should be tracked */ if (false) ; foo(a); }");
    liveVariables = LiveVariables.analyze(cfg);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(3))).hasSize(1);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(3)).iterator().next().name()).isEqualTo("a");
  }

  @Test
  public void test_while_loop()  {
    CFG cfg = buildCFG("void foo(boolean condition) { while (condition) { int x = 0; use(x); x = 1; /* x should not be live here*/}}");
    LiveVariables liveVariables = LiveVariables.analyze(cfg);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(1))).hasSize(1);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(2))).hasSize(1);
  }

  @Test
  public void in_of_first_block_should_be_empty()  {
    CFG cfg = buildCFG("boolean foo(int a) { foo(a);}");
    LiveVariables liveVariables = LiveVariables.analyze(cfg);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(0))).isEmpty();
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(1))).isEmpty();
  }

  @Test
  public void lambdas_read_liveness() {
    CFG cfg = buildCFG("boolean foo(int a) { if(true) { System.out.println(''); }foo(x -> a + 1);}");
    LiveVariables liveVariables = LiveVariables.analyze(cfg);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(0))).isEmpty();
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(2))).hasSize(1);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(3))).hasSize(1);

    cfg = buildCFG("boolean foo(int a) { if(true) { System.out.println(''); }foo(x -> a = 2);}");
    liveVariables = LiveVariables.analyze(cfg);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(0))).isEmpty();
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(2))).isEmpty();
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(3))).isEmpty();
  }

  @Test
  public void method_ref_liveness() throws Exception {
    CFG cfg = buildCFG("boolean foo(Object a) { if(true) { System.out.println(''); }foo(a::toString);}");
    LiveVariables liveVariables = LiveVariables.analyze(cfg);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(0))).isEmpty();
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(2))).hasSize(1);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(3))).hasSize(1);
  }

  @Test
  public void anonymous_class_liveness()  {
    CFG cfg = buildCFG("boolean foo(int a) { if(true) { System.out.println(''); } new Object() { String toString(){return a;} }; }");
    LiveVariables liveVariables = LiveVariables.analyze(cfg);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(0))).isEmpty();
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(2))).hasSize(1);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(3))).hasSize(1);

    cfg = buildCFG("boolean foo(int a) { if(true) { new A().field = 2;System.out.println(''); } new Object() { String toString(){return a = 2;} }; }");
    liveVariables = LiveVariables.analyze(cfg);
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(0))).isEmpty();
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(2))).isEmpty();
    assertThat(liveVariables.getOut(cfg.reversedBlocks().get(3))).isEmpty();
  }

  @Test
  public void test_fields_live() {
    assertFieldsByMethodEntry("void foo(int a) {  foo(field1); foo(); field2 = 1;}", "field1");
    assertFieldsByMethodEntry("void foo(int a) {  foo(this.field1); this.foo(); this.field2 = 1; }", "field1");
    assertFieldsByMethodEntry("void foo(int a) { A that = new A(); foo(that.field1); foo(that.staticField); }", "staticField");
    assertFieldsByMethodEntry("void foo(int a) { foo(new A().staticField);  foo(new A().field1); }", "staticField");
    assertFieldsByMethodEntry("void foo(int a) { B that = new B(); foo(that.field1); }");
  }

  private void assertFieldsByMethodEntry(String methodCode, String ...inEntryNames) {
    CFG cfg = buildCFG(methodCode);
    LiveVariables liveVariables = LiveVariables.analyzeWithFields(cfg);
    assertThat(liveVariables.getOut(cfg.entryBlock())).isEmpty();

    List<Symbol> in = new ArrayList<>(liveVariables.getIn(cfg.entryBlock()));
    assertThat(in).hasSize(inEntryNames.length);
    in.forEach(symbol -> assertThat(symbol.name()).isIn(inEntryNames));
  }
}
