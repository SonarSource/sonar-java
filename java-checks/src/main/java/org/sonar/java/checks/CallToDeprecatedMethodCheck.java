/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.checks;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceFile;

@Rule(
  key = "CallToDeprecatedMethod",
  name = "Deprecated methods should not be used",
  tags = {"cwe", "obsolete", "owasp-a9", "security"},
  priority = Priority.MINOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SOFTWARE_RELATED_PORTABILITY)
@SqaleConstantRemediation("15min")
public class CallToDeprecatedMethodCheck extends BytecodeVisitor {

  private AsmClass asmClass;

  @Override
  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
  }

  @Override
  public void visitEdge(AsmEdge edge) {
    if (edge.getTo().isDeprecated() && edge.getTo() instanceof AsmMethod) {
      AsmMethod targetMethod = (AsmMethod) edge.getTo();
      SourceFile sourceFile = getSourceFile(asmClass);
      CheckMessage message = new CheckMessage(this, formatMessage(targetMethod));
      message.setLine(edge.getSourceLineNumber());
      sourceFile.log(message);
    }
  }

  public String formatMessage(AsmMethod asmMethod) {
    if (asmMethod.isConstructor()) {
      return "Constructor '" + getShortClassName(asmMethod.getParent()) + "(...)' is deprecated.";
    } else {
      return "Method '" + getShortClassName(asmMethod.getParent()) + "." + asmMethod.getName() + "(...)' is deprecated.";
    }
  }

  public String getShortClassName(AsmClass asmClass) {
    return StringUtils.substringAfterLast(asmClass.getInternalName(), "/");
  }

}
