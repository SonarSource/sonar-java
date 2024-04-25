/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

package org.sonar.java.checks.quickfixes;

import org.sonar.plugins.java.api.lighttree.LT;

import static org.sonar.plugins.java.api.lighttree.Operator.BinaryOperator.ADD;
import static org.sonar.plugins.java.api.lighttree.Operator.BinaryOperator.DIV;
import static org.sonar.plugins.java.api.lighttree.Operator.BinaryOperator.EQUALITY;
import static org.sonar.plugins.java.api.lighttree.Operator.BinaryOperator.MUL;
import static org.sonar.plugins.java.api.lighttree.Operator.BinaryOperator.SUB;

public final class TmpTestMain {

  public static void main(String[] args) {
    var ast = new LT.IfStat(new LT.BinOp(new LT.Id("x"), EQUALITY, new LT.Const("Hello")),
      new LT.Block(
        new LT.AssignmentExpr(new LT.Id("y"), new LT.Const(0))
      ),
      new LT.Block(
        new LT.VarDecl(new LT.TypeNode("int"), new LT.Id("p"),
          new LT.BinOp(
            new LT.Const(42),
            ADD,
            new LT.BinOp(
              new LT.BinOp(
                new LT.Const(10),
                ADD,
                new LT.Id("a")
              ),
              MUL,
              new LT.BinOp(
                new LT.Id("b"),
                SUB,
                new LT.BinOp(
                  new LT.Id("c"),
                  DIV,
                  new LT.Id("d")
                )
              )
            )
          )
        ),
        new LT.Switch(new LT.Invocation(new LT.Id("someMethod"), new LT.Args(new LT.Id("p"), new LT.Const(-10))),
          new LT.CaseGroup(new LT.CaseLabel(new LT.Const(10)), new LT.AssignmentExpr(new LT.Id("t"), new LT.Const(0)))
        )
      )
    );
    var str = PrettyPrinter.prettyPrint(ast, new FileConfig("  ", "\n"));
    System.out.println(str);
  }

}
