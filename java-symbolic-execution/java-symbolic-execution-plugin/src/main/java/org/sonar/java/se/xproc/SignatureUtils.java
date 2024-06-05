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
package org.sonar.java.se.xproc;

/**
 * Introduced when dropping ASM from project, in order to stay consistent in method signature
 */
class SignatureUtils {

  private static final String VOID = "V";
  private static final String BOOLEAN = "Z";

  private SignatureUtils() {
  }

  static boolean isConstructor(String signature) {
    return signature.contains("<init>");
  }

  static int numberOfArguments(String signature) {
    return argumentTypes(signature).length;
  }

  static boolean isVoidMethod(String signature) {
    return VOID.equals(returnType(signature));
  }

  static boolean isBoolean(String signature, int argumentIndex) {
    String type;
    if (argumentIndex == -1) {
      type = returnType(signature);
    } else {
      String[] argumentTypes = argumentTypes(signature);
      type = argumentTypes[argumentIndex];
    }
    return BOOLEAN.equals(type);
  }

  private static String returnType(String signature) {
    return getReturnType(methodDescriptor(signature));
  }

  private static String[] argumentTypes(String signature) {
    return getArgumentTypes(methodDescriptor(signature));
  }

  private static String methodDescriptor(String signature) {
    return signature.substring(signature.indexOf('('));
  }

  /**
   * Adapted from org.objectweb.asm.Type.getArgumentTypes(String) method
   * to get types as String[] instead of asm Type[].
   */
  private static String[] getArgumentTypes(String methodDescriptor) {
    // First step: compute the number of argument types in methodDescriptor.
    int numArgumentTypes = 0;
    // Skip the first character, which is always a '('.
    int currentOffset = 1;
    // Parse the argument types, one at a each loop iteration.
    while (methodDescriptor.charAt(currentOffset) != ')') {
      while (methodDescriptor.charAt(currentOffset) == '[') {
        currentOffset++;
      }
      if (methodDescriptor.charAt(currentOffset++) == 'L') {
        // Skip the argument descriptor content.
        int semiColumnOffset = methodDescriptor.indexOf(';', currentOffset);
        currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
      }
      ++numArgumentTypes;
    }

    // Second step: get the corresponding String for each argument type.
    String[] argumentTypes = new String[numArgumentTypes];
    // Skip the first character, which is always a '('.
    currentOffset = 1;
    // Parse and create the argument types, one at each loop iteration.
    int currentArgumentTypeIndex = 0;
    while (methodDescriptor.charAt(currentOffset) != ')') {
      final int currentArgumentTypeOffset = currentOffset;
      while (methodDescriptor.charAt(currentOffset) == '[') {
        currentOffset++;
      }
      if (methodDescriptor.charAt(currentOffset++) == 'L') {
        // Skip the argument descriptor content.
        int semiColumnOffset = methodDescriptor.indexOf(';', currentOffset);
        currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
      }
      argumentTypes[currentArgumentTypeIndex++] = methodDescriptor.substring(currentArgumentTypeOffset, currentOffset);
    }
    return argumentTypes;
  }

  /**
   * Adapted from org.objectweb.asm.Type.getReturnTypeOffset(String) method
   * to get corresponding type as String, instead of asm Type.
   */
  private static String getReturnType(String methodDescriptor) {
    // Skip the first character, which is always a '('.
    int currentOffset = 1;
    // Skip the argument types, one at a each loop iteration.
    while (methodDescriptor.charAt(currentOffset) != ')') {
      while (methodDescriptor.charAt(currentOffset) == '[') {
        currentOffset++;
      }
      if (methodDescriptor.charAt(currentOffset++) == 'L') {
        // Skip the argument descriptor content.
        int semiColumnOffset = methodDescriptor.indexOf(';', currentOffset);
        currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
      }
    }
    return methodDescriptor.substring(currentOffset + 1);
  }

}
