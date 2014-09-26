/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.java.signature.MethodSignatureScanner;
import org.sonar.java.signature.Parameter;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceFile;

import java.util.List;

@Rule(key = UnusedPrivateMethodCheck.RULE_KEY, priority = Priority.MAJOR,
  tags = {"unused"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class UnusedPrivateMethodCheck extends BytecodeVisitor {

  public static final String RULE_KEY = "UnusedPrivateMethod";
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
        for (Parameter param : MethodSignatureScanner.scan(asmMethod.getSignature()).getArgumentTypes()) {
          String paramName = param.getClassName();
          if (StringUtils.isEmpty(paramName)) {
            paramName = MethodSignatureScanner.getReadableType(param.getJvmJavaType());
          }
          params.add(paramName + (param.isArray() ? "[]" : ""));
        }
        messageStr += Joiner.on(",").join(params) + ")' is never used.";
      }
      CheckMessage message = new CheckMessage(this, messageStr);
      int line = getMethodLineNumber(asmMethod);
      if (line > 0) {
        message.setLine(line);
      }
      SourceFile file = getSourceFile(asmClass);
      file.log(message);
    }
  }

  private boolean isPrivateUnused(AsmMethod asmMethod) {
    return !asmMethod.isUsed() && asmMethod.isPrivate();
  }

  private boolean isExcludedFromCheck(AsmMethod asmMethod) {
    return asmMethod.isSynthetic() || asmMethod.isDefaultConstructor() || SerializableContract.methodMatch(asmMethod);
  }

  @Override
  public String toString() {
    return RULE_KEY + " rule";
  }

}
