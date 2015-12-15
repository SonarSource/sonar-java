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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.java.signature.MethodSignatureScanner;
import org.sonar.java.signature.Parameter;
import org.sonar.java.tag.Tag;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "UnusedPrivateMethod",
  name = "Unused private method should be removed",
  priority = Priority.MAJOR,
  tags = {Tag.UNUSED})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class UnusedPrivateMethodCheck extends BytecodeVisitor {

  private AsmClass asmClass;

  @Override
  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
  }

  @Override
  public void visitMethod(AsmMethod asmMethod) {
    if (isPrivateUnused(asmMethod) && !isExcludedFromCheck(asmMethod)) {
      String messageStr = "Private method '" + asmMethod.getName() + "' is never used.";
      if ("<init>".equals(asmMethod.getName())) {
        messageStr = "Private constructor '" + asmClass.getDisplayName() + "(";
        List<String> params = Lists.newArrayList();
        for (Parameter param : MethodSignatureScanner.scan(asmMethod.getGenericKey()).getArgumentTypes()) {
          String paramName = param.getClassName();
          if (StringUtils.isEmpty(paramName)) {
            paramName = MethodSignatureScanner.getReadableType(param.getJvmJavaType());
          }
          params.add(paramName + (param.isArray() ? "[]" : ""));
        }
        messageStr += Joiner.on(",").join(params) + ")' is never used.";
      }
      int line = getMethodLineNumber(asmMethod);
      getContext().reportIssue(this, getSourceFile(asmClass), messageStr, line);
    }
  }

  private static boolean isPrivateUnused(AsmMethod asmMethod) {
    return !asmMethod.isUsed() && asmMethod.isPrivate();
  }

  private static boolean isExcludedFromCheck(AsmMethod asmMethod) {
    return asmMethod.isSynthetic() || asmMethod.isDefaultConstructor() || SerializableContract.methodMatch(asmMethod);
  }

}
