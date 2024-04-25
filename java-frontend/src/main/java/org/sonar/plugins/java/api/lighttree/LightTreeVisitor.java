package org.sonar.plugins.java.api.lighttree;

public interface LightTreeVisitor {

  void visitLightAssignExpr(LightAssignExpr assignExpr);

  void visitLightBinOp(LightBinOp binop);

  void visitLightBlock(LightBlock block);

  void visitLightId(LightId id);

  void visitLightIfStat(LightIfStat ifStat);

  void visitLightLiteral(LightLiteral literal);

  void visitLightTypeNode(LightTypeNode typeNode);

  void visitLightVarDecl(LightVarDecl varDecl);

  void visitLightMethodInvocation(LightMethodInvocation invocation);

  void visitLightArguments(LightArguments args);

  void visitLightSwitch(LightSwitch swtch);

  void visitLightCaseGroup(LightCaseGroup caseGroup);

  void visitLightCaseLabel(LightCaseLabel label);

}
