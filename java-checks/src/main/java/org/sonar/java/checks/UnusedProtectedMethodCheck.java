/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.java.tag.Tag;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "UnusedProtectedMethod",
  name = "Unused protected methods should be removed",
  priority = Priority.MAJOR,
  tags = {Tag.UNUSED})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("10min")
public class UnusedProtectedMethodCheck extends BytecodeVisitor {

  private AsmClass asmClass;

  @Override
  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
  }

  @Override
  public void visitMethod(AsmMethod asmMethod) {
    if (isUnusedNonOverridenProtectedMethod(asmMethod) && !asmClass.isAbstract() && !SerializableContract.methodMatch(asmMethod)) {
      int methodLineNumber = getMethodLineNumber(asmMethod);
      getContext().reportIssue(this, getSourceFile(asmClass), "Protected method '" + asmMethod.getName() + "(...)' is never used.", methodLineNumber);
    }
  }

  private static boolean isUnusedNonOverridenProtectedMethod(AsmMethod asmMethod) {
    return !asmMethod.isUsed() && asmMethod.isProtected() && !asmMethod.isInherited();
  }

}
