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
                  expr("t1").times(expr("r")).times(expr("u1").minus(expr("u2"))).plus(expr("t2").times(expr("t3").minus(expr("t4"))))
                )
              )),
            Default(Block(

            ))
          )
        ),
        Block(
          expr("x").assig(cst(0)),
          expr("a").assig(cst("Hello"))
        )
      )
    );
    var str = Prettyprinter.prettyprint(ast, new FileConfig("  ", "\n"));
    System.out.println(str);
  }

}
