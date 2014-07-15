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
import com.google.common.collect.ImmutableList;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceMethod;

import java.util.Collections;
import java.util.List;

@Rule(key = ThrowsSeveralCheckedExceptionCheck.RULE_KEY, priority = Priority.MAJOR, tags={"error-handling"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ThrowsSeveralCheckedExceptionCheck extends BytecodeVisitor {

  public static final String RULE_KEY = "S1160";
  private AsmClass asmClass;

  @Override
  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
  }

  @Override
  public void visitMethod(AsmMethod asmMethod) {
    if (asmMethod.isPublic() && !isOverriden(asmMethod)) {
      List<String> thrownCheckedExceptions = getThrownCheckedExceptions(asmMethod);

      if (thrownCheckedExceptions.size() > 1) {
        CheckMessage message = new CheckMessage(
          this,
          "Refactor this method to throw at most one checked exception instead of: " + Joiner.on(", ").join(thrownCheckedExceptions));

        SourceMethod sourceMethod = getSourceMethod(asmMethod);
        if (sourceMethod != null) {
          message.setLine(sourceMethod.getStartAtLine());
        }
        SourceFile file = getSourceFile(asmClass);
        file.log(message);
      }
    }
  }

  private List<String> getThrownCheckedExceptions(AsmMethod asmMethod) {
    List<AsmClass> thrownClasses = asmMethod.getThrows();

    if (thrownClasses.size() > 1) {
      ImmutableList.Builder<String> builder = ImmutableList.builder();

      for (AsmClass thrownClass : thrownClasses) {
        if (!isSubClassOfRuntimeException(thrownClass)) {
          builder.add(thrownClass.getDisplayName());
        }
      }

      return builder.build();
    } else {
      return Collections.emptyList();
    }
  }

  private static boolean isOverriden(AsmMethod method) {
    return isOverridenFromClass(method) ||
      isOverridenFromInterface(method);
  }

  private static boolean isOverridenFromClass(AsmMethod method) {
    AsmClass superClass = method.getParent().getSuperClass();
    return superClass != null && superClass.getMethod(method.getKey()) != null;
  }

  private static boolean isOverridenFromInterface(AsmMethod method) {
    for (AsmClass implementedInterface : method.getParent().getImplementedInterfaces()) {
      if (implementedInterface.getMethod(method.getKey()) != null) {
        return true;
      }
    }
    return false;
  }

  private static boolean isSubClassOfRuntimeException(AsmClass thrownClass) {
    for (AsmClass current = thrownClass; current != null; current = current.getSuperClass()) {
      if ("java/lang/RuntimeException".equals(current.getInternalName())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return RULE_KEY + " rule";
  }

}
