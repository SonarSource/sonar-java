/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.java.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstNode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class TreeVisitorsDispatcherTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void complete_visitor() {
    Set<Class> interfaces = Sets.newHashSet();
    for (Tree.Kind kind : Tree.Kind.values()) {
      interfaces.add(kind.associatedInterface);
    }
    for (Method method : CompleteVisitor.class.getMethods()) {
      if ("visit".equals(method.getName())) {
        interfaces.remove(method.getParameterTypes()[0]);
      }
    }
    assertThat(interfaces).isEmpty();
  }

  @Test
  public void should_invoke_methods() {
    EmptyStatementTree emptyStatement = new JavaTree.EmptyStatementTreeImpl(Mockito.mock(AstNode.class));
    IfStatementTree ifStatement = new JavaTree.IfStatementTreeImpl(Mockito.mock(AstNode.class),
      new JavaTree.IdentifierTreeImpl(Mockito.mock(AstNode.class), "i"),
      emptyStatement,
      null
    );

    FakeVisitor visitor1 = Mockito.mock(FakeVisitor.class);
    FakeVisitor visitor2 = Mockito.mock(FakeVisitor.class);
    TreeVisitorsDispatcher dispatcher = new TreeVisitorsDispatcher(ImmutableList.of(visitor1, visitor2));
    JavaTree.scan(ifStatement, dispatcher);

    InOrder inOrder = Mockito.inOrder(visitor1, visitor2);
    inOrder.verify(visitor1).visit(ifStatement);
    inOrder.verify(visitor2).visit(ifStatement);
    inOrder.verify(visitor1).visit(emptyStatement);
    inOrder.verify(visitor2).visit(emptyStatement);

    inOrder.verify(visitor2).leave(emptyStatement);
    inOrder.verify(visitor1).leave(emptyStatement);
    inOrder.verify(visitor2).leave(ifStatement);
    inOrder.verify(visitor1).leave(ifStatement);
  }

  @Test
  public void should_propagate_exceptions() {
    EmptyStatementTree emptyStatement = new JavaTree.EmptyStatementTreeImpl(Mockito.mock(AstNode.class));
    IfStatementTree ifStatement = new JavaTree.IfStatementTreeImpl(Mockito.mock(AstNode.class),
      new JavaTree.IdentifierTreeImpl(Mockito.mock(AstNode.class), "i"),
      emptyStatement,
      null
    );

    FakeVisitor visitor = Mockito.mock(FakeVisitor.class);
    Mockito.doThrow(RuntimeException.class).when(visitor).visit(Mockito.any(StatementTree.class));

    TreeVisitorsDispatcher dispatcher = new TreeVisitorsDispatcher(ImmutableList.of(visitor));
    thrown.expect(RuntimeException.class);
    JavaTree.scan(ifStatement, dispatcher);
  }

  private static interface FakeVisitor extends TreeVisitor {
    void visit(StatementTree tree);
    void leave(StatementTree tree);

    void visit(EmptyStatementTree tree);
    void leave(EmptyStatementTree tree);
  }

}
