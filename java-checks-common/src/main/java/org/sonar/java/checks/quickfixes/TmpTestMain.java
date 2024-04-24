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

import org.sonar.plugins.java.api.lighttree.Operator;

public final class TmpTestMain {

  public static void main(String[] args) {
    var ast = new P.IfStat(new P.BinOp(new P.Id("x"), Operator.BinaryOperator.EQUALITY, new P.Const("Hello")),
      new P.Block(
        new P.AssignmentExpr(new P.Id("y"), new P.Const(0))
      ),
      new P.Block(
        new P.VarDecl(new P.TypeNode("int"), new P.Id("p"), new P.Const(42))
      )
    );
    var str = PrettyPrinter.prettyPrint(ast, new FileConfig("  ", "\n"));
    System.out.println(str);
  }

}
