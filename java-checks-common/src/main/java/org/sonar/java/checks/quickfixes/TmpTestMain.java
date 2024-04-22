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

import static org.sonar.java.checks.quickfixes.Syntax.*;

public final class TmpTestMain {

  public static void main(String[] args) {

    var ast = If(expr("myVar").eq(cst(0)),
      Block(
        expr("x").assig(expr("y").minus(expr("z")))
      ), If(expr("boolVar"),
        Block(
          Switch(expr("tree"),
            Case(Pat("Fork", Pat("Tree", "left"), Pat("Tree", "right"))
                .Where(expr("y").eq(cst(-1))),
              Block(
                expr("sum").assig(
                  expr("t1").times(expr("r").times(expr("s"))).times(expr("u1").minus(expr("u2"))).plus(expr("t2").times(expr("t3").minus(expr("t4"))))
                )
              )),
            Default(Block(
              Decl("int", "tmp", cst(2).times(expr("i"))),
              expr("sum").assig(expr("tmp").times(expr("tmp")))
            ))
          )
        ),
        hardCodedBlock(
          """
              {
                var t = foo(x, y);
                bar(t, -1, x-y);
                System.out.println(t);
              }
          """
        )
      )
    );
    var str = Prettyprinter.prettyprint(ast, new FileConfig("  ", "\n"));
    System.out.println(str);
  }

}
