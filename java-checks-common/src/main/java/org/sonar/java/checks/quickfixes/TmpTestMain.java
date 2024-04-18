package org.sonar.java.checks.quickfixes;

import org.sonar.java.checks.quickfixes.Ast.Case;
import org.sonar.java.checks.quickfixes.Ast.HardCodedExpr;
import org.sonar.java.checks.quickfixes.Ast.HardCodedStat;
import org.sonar.java.checks.quickfixes.Ast.RecordPattern;
import org.sonar.java.checks.quickfixes.Ast.Switch;
import org.sonar.java.checks.quickfixes.Ast.VariablePattern;

import static org.sonar.java.checks.quickfixes.Ast.Block;
import static org.sonar.java.checks.quickfixes.Ast.IfStat;

public final class TmpTestMain {

  public static void main(String[] args) {
    var ast = new IfStat(new HardCodedExpr("myVar == 0"),
      new Block(
        new HardCodedStat("x = y + z"),
        new HardCodedStat("foo(x, y);")
      ),
      new IfStat(new HardCodedExpr("myVar < 0"),
        new Block(
          new IfStat(new HardCodedExpr("y % 2 == 0"),
            new Block(new HardCodedStat("bar(u, -1)")))
        ),
        new Block(
          new Switch(new HardCodedExpr("tree"),
            new Case(new RecordPattern("Fork", new VariablePattern("var", "left"), new VariablePattern("var", "right")),
              new Block(
                new HardCodedStat("total = sum(left) + sum(right)")
              )),
            new Case(new VariablePattern("int", "value"),
              new Block(
                new HardCodedStat("total = value")
              ))
          )
        )
      )
    );
    var str = Prettyprinter.prettyprint(ast, new FileConfig("  ", "\n"));
    System.out.println(str);
  }

}
