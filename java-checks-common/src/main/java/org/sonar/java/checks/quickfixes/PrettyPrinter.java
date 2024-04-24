package org.sonar.java.checks.quickfixes;

import java.util.List;
import java.util.function.Consumer;
import org.sonar.plugins.java.api.lighttree.LightAssignExpr;
import org.sonar.plugins.java.api.lighttree.LightBinOp;
import org.sonar.plugins.java.api.lighttree.LightBlock;
import org.sonar.plugins.java.api.lighttree.LightExpr;
import org.sonar.plugins.java.api.lighttree.LightId;
import org.sonar.plugins.java.api.lighttree.LightIfStat;
import org.sonar.plugins.java.api.lighttree.LightLiteral;
import org.sonar.plugins.java.api.lighttree.LightStat;
import org.sonar.plugins.java.api.lighttree.LightTree;
import org.sonar.plugins.java.api.lighttree.LightTreeVisitor;
import org.sonar.plugins.java.api.lighttree.LightTypeNode;
import org.sonar.plugins.java.api.lighttree.LightVarDecl;
import org.sonar.plugins.java.api.lighttree.Operator;

public final class PrettyPrinter implements LightTreeVisitor {
  private final PrettyprintStringBuilder pps;

  public PrettyPrinter(PrettyprintStringBuilder pps) {
    this.pps = pps;
  }

  public static String prettyPrint(LightTree ast, FileConfig fileConfig){
    var pps = new PrettyprintStringBuilder(fileConfig);
    ast.accept(new PrettyPrinter(pps));
    return pps.toString();
  }

  @Override
  public void visitLightAssignExpr(LightAssignExpr assignExpr) {
    assignExpr.variable().accept(this);
    pps.add(" = ");
    assignExpr.expression().accept(this);
  }

  @Override
  public void visitLightBinOp(LightBinOp binop) {
    var parenthesizeLhs = binop.hasPrecedenceOver(binop.leftOperand());
    maybeParenthesize(binop.leftOperand(), parenthesizeLhs);
    pps.addSpace().add(binop.operator().code()).addSpace();
    var parenthesizeRhs = !binop.rightOperand().hasPrecedenceOver(binop) && !isSameAssociativeOperator(binop.rightOperand(), binop.operator());
    maybeParenthesize(binop.rightOperand(), parenthesizeRhs);
  }

  @Override
  public void visitLightBlock(LightBlock block) {
    pps.add("{").incIndent().newLine();
    printAllWithSep(block.body(), PrettyprintStringBuilder::semicolonAndNewLine);
    pps.decIndent().semicolonAndNewLine().add("}");
  }

  @Override
  public void visitLightId(LightId id) {
    pps.add(id.name());
  }

  @Override
  public void visitLightIfStat(LightIfStat ifStat) {
    pps.add("if (");
    ifStat.condition().accept(this);
    pps.add(") ");
    ifStat.thenStatement().accept(this);
    var elseStat = ifStat.elseStatement();
    if (elseStat != null){
      pps.add(" else ");
      elseStat.accept(this);
    }
  }

  @Override
  public void visitLightLiteral(LightLiteral literal) {
    pps.add(literal.value());
  }

  @Override
  public void visitLightTypeNode(LightTypeNode typeNode) {
    pps.add(typeNode.typeName());
  }

  @Override
  public void visitLightVarDecl(LightVarDecl varDecl) {
    varDecl.type().accept(this);
    pps.addSpace();
    varDecl.simpleName().accept(this);
    var initializer = varDecl.initializer();
    if (initializer != null) {
      pps.add(" = ");
      initializer.accept(this);
    }
  }

  private <T extends LightTree> void printAllWithSep(List<T> asts, Consumer<PrettyprintStringBuilder> sep) {
    var iter = asts.iterator();
    while (iter.hasNext()) {
      iter.next().accept(this);
      if (iter.hasNext()) {
        sep.accept(pps);
      }
    }
  }

  private static boolean isSameAssociativeOperator(LightExpr subExpr, Operator topLevelOp) {
    return subExpr instanceof LightBinOp subBinop && subBinop.operator() == topLevelOp && topLevelOp.isAssociative();
  }

  private void maybeParenthesize(LightExpr expr, boolean parenthesize) {
    if (parenthesize) {
      pps.add("(");
    }
    expr.accept(this);
    if (parenthesize) {
      pps.add(")");
    }
  }
}
