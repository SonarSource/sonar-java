/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.se;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.java.cfg.CFG;
import org.sonar.java.se.ExplodedGraph.Node;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.HappyPathYield;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.viewer.DotGraph;
import org.sonar.java.viewer.Viewer;
import org.sonar.plugins.java.api.semantic.Symbol;

import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import static org.assertj.core.api.Assertions.assertThat;

public class EGDotNodeTest {
  private BehaviorCache mockBehaviorCache;
  private ExplodedGraph eg;

  @Before
  public void setUp() {
    mockBehaviorCache = Mockito.mock(BehaviorCache.class);
    eg = new ExplodedGraph();
  }

  @Test
  public void lost_node_has_specific_highlighting() {
    String source = "class A {"
      + "  void foo() {"
      + "    doSomething();"
      + "  }"
      + "}";

    Viewer.Base base = new Viewer.Base(source);

    // no parent, fake id of first block being 42, block id being 0
    Node node = newNode(base.cfgFirstMethod.blocks().get(0), 0);
    EGDotNode egDotNode = new EGDotNode(0, node, mockBehaviorCache, false, 42);

    assertThat(egDotNode.highlighting()).isEqualTo(DotGraph.Highlighting.LOST_NODE);

  }

  @Test
  public void node_without_method_invocation_has_nothing_regarding_methods() {
    String source = "class A {"
      + "  void foo() {"
      + "    int i = 1;"
      + "  }"
      + "}";

    Viewer.Base base = new Viewer.Base(source);

    Node node = newNode(base.cfgFirstMethod.blocks().get(0), 0);
    EGDotNode egDotNode = new EGDotNode(0, node, mockBehaviorCache, false, 1);

    JsonObject details = egDotNode.details();
    assertThat(details.get("methodName")).isNull();
    assertThat(details.get("methodYields")).isNull();
  }

  @Test
  public void unknown_method_does_not_populate_methodYields_and_methodName() {
    String source = "class A {"
      + "  void foo() {"
      + "    doSomething();"
      + "  }"
      + "}";

    Viewer.Base base = new Viewer.Base(source);

    // node of method invocation
    Node node = newNode(base.cfgFirstMethod.blocks().get(0), 1);
    EGDotNode egDotNode = new EGDotNode(0, node, mockBehaviorCache, false, 1);

    JsonObject details = egDotNode.details();
    assertThat(details.get("methodName")).isNull();
    assertThat(details.get("methodYields")).isNull();
  }

  @Test
  public void method_with_behavior_populate_methodYields_and_methodName() {
    String source = "abstract class A {"
      + "  void foo() {"
      + "    doSomething();"
      + "  }"
      + "  abstract Boolean doSomething();"
      + "}";

    Viewer.Base base = new Viewer.Base(source);

    Mockito.when(mockBehaviorCache.get(Mockito.any(Symbol.MethodSymbol.class))).thenAnswer(new Answer<MethodBehavior>() {
      @Override
      public MethodBehavior answer(InvocationOnMock invocation) throws Throwable {
        Symbol.MethodSymbol methodSymbol = invocation.getArgument(0);
        MethodBehavior mb = new MethodBehavior(methodSymbol);
        HappyPathYield hpy = new HappyPathYield(mb);
        hpy.setResult(-1, null);
        mb.addYield(hpy);
        return mb;
      }
    });

    Node node = newNode(base.cfgFirstMethod.blocks().get(0), 1);
    EGDotNode egDotNode = new EGDotNode(0, node, mockBehaviorCache, false, 1);

    JsonObject details = egDotNode.details();

    JsonValue methodName = details.get("methodName");
    assertThat(methodName).isNotNull();
    assertThat(methodName.toString()).isEqualTo("\"doSomething\"");

    JsonValue yields = details.get("methodYields");
    assertThat(yields).isNotNull();
    assertThat(yields.getValueType()).isEqualTo(ValueType.ARRAY);
    assertThat(yields.toString()).isEqualTo("[{\"params\":[],\"result\":[\"no constraint\"],\"resultIndex\":-1}]");
  }

  @Test
  public void method_with_behavior_populate_methodYields_and_methodName_with_constraints_and_index() {
    String source = "abstract class A {"
      + "  void foo() {"
      + "    doSomething();"
      + "  }"
      + "  abstract Boolean doSomething();"
      + "}";

    Viewer.Base base = new Viewer.Base(source);

    Mockito.when(mockBehaviorCache.get(Mockito.any(Symbol.MethodSymbol.class))).thenAnswer(new Answer<MethodBehavior>() {
      @Override
      public MethodBehavior answer(InvocationOnMock invocation) throws Throwable {
        Symbol.MethodSymbol methodSymbol = invocation.getArgument(0);
        MethodBehavior mb = new MethodBehavior(methodSymbol);
        HappyPathYield hpy = new HappyPathYield(mb);
        hpy.setResult(2, ConstraintsByDomain.empty().put(ObjectConstraint.NOT_NULL).put(BooleanConstraint.FALSE));
        mb.addYield(hpy);
        return mb;
      }
    });

    Node node = newNode(base.cfgFirstMethod.blocks().get(0), 1);
    EGDotNode egDotNode = new EGDotNode(0, node, mockBehaviorCache, false, 1);

    JsonObject details = egDotNode.details();

    JsonValue methodName = details.get("methodName");
    assertThat(methodName).isNotNull();
    assertThat(methodName.toString()).isEqualTo("\"doSomething\"");

    JsonValue yields = details.get("methodYields");
    assertThat(yields).isNotNull();
    assertThat(yields.getValueType()).isEqualTo(ValueType.ARRAY);
    // order is alphabetical for constraint
    assertThat(yields.toString()).isEqualTo("[{\"params\":[],\"result\":[\"FALSE\",\"NOT_NULL\"],\"resultIndex\":2}]");
  }


  private Node newNode(CFG.Block block, int i) {
    ProgramPoint pp = new ProgramPoint(block);
    for (int j = 0; j < i; j++) {
      pp = pp.next();
    }
    return eg.node(pp, ProgramState.EMPTY_STATE);
  }

}
