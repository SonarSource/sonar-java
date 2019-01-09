/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.bytecode.se;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.Flags;

import static org.sonar.java.resolve.BytecodeCompleter.ASM_API_VERSION;

public class MethodLookup {



  final boolean isStatic;
  final boolean isVarArgs;
  final List<String> declaredExceptions;

  private MethodLookup(boolean isStatic, boolean isVarArgs, List<String> declaredExceptions) {
    this.isStatic = isStatic;
    this.isVarArgs = isVarArgs;
    this.declaredExceptions = declaredExceptions;
  }

  /**
   *  Lookup method as described in JVM spec https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-5.html#jvms-5.4.3.3
   *  Some steps of the algorithm are not followed precisely, mostly the concept of maximally-specific superinterface,
   *  this should be OK, because such code should not compile anyway (i.e this can happen only if runtime and compile-time
   *  dependencies are different)
   *
   */
  @CheckForNull
  public static MethodLookup lookup(String signature, SquidClassLoader classLoader, LookupMethodVisitor methodVisitor) {
    String className = signature.substring(0, signature.indexOf('#'));
    return lookup(className, signature, classLoader, methodVisitor);
  }

  private static MethodLookup lookup(String className, String signature, SquidClassLoader classLoader, LookupMethodVisitor methodVisitor) {
    byte[] bytes = classLoader.getBytesForClass(className);
    if (bytes == null) {
      return null;
    }
    ClassReader cr = new ClassReader(bytes);
    LookupClassVisitor lookupVisitor = new LookupClassVisitor(methodVisitor, signature);
    cr.accept(lookupVisitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    if (lookupVisitor.methodFound) {
      return new MethodLookup(lookupVisitor.isStatic, lookupVisitor.isVarArgs, lookupVisitor.declaredExceptions);
    }
    // we didn't succeed to find the method in the class, try recursively on superclasses and interfaces
    if (lookupVisitor.superClassName != null) {
      MethodLookup result = lookup(lookupVisitor.superClassName, signature, classLoader, methodVisitor);
      if (result != null) {
        return result;
      }
    }
    if (lookupVisitor.interfaces != null) {
      return Arrays.stream(lookupVisitor.interfaces)
          .map(iface -> lookup(iface, signature, classLoader, methodVisitor))
          .filter(Objects::nonNull)
          .findAny().orElse(null);
    }
    return null;
  }

  public static class LookupMethodVisitor extends MethodVisitor {

    public LookupMethodVisitor() {
      super(ASM_API_VERSION);
    }

    /**
     *
     * @param methodFlags bytecode flags as provided by {@link ClassVisitor#visitMethod(int, String, String, String, String[])}
     * @param methodSignature method signature
     * @return true if method should be visited by visitor
     */
    public boolean shouldVisitMethod(int methodFlags, String methodSignature) {
      return true;
    }
  }

  private static class LookupClassVisitor extends ClassVisitor {

    private final LookupMethodVisitor methodVisitor;
    private final String methodSignature;
    private boolean methodFound;
    private String superClassName;
    private String[] interfaces;
    private List<String> declaredExceptions;
    private boolean isStatic;
    private boolean isVarArgs;

    public LookupClassVisitor(LookupMethodVisitor methodVisitor, String targetedMethodSignatures) {
      super(ASM_API_VERSION);
      this.methodVisitor = methodVisitor;
      this.methodSignature = targetedMethodSignatures;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      superClassName = superName;
      this.interfaces = interfaces;
      super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      if (name.equals(methodSignature.substring(methodSignature.indexOf('#') + 1, methodSignature.indexOf('(')))
          && desc.equals(methodSignature.substring(methodSignature.indexOf('(')))) {
        methodFound = true;
        declaredExceptions = convertExceptions(exceptions);
        isStatic = Flags.isFlagged(access, Flags.STATIC);
        isVarArgs = Flags.isFlagged(access, Flags.VARARGS);
        if (!methodVisitor.shouldVisitMethod(access, methodSignature)) {
          // avoid computing CFG when the method behavior won't be used
          return null;
        }
        return new JSRInlinerAdapter(methodVisitor, access, name, desc, signature, exceptions);
      }
      return null;
    }

    private static List<String> convertExceptions(@Nullable String[] exceptions) {
      return exceptions == null ? Collections.emptyList() : Arrays.stream(exceptions)
          .map(Type::getObjectType)
          .map(Type::getClassName)
          .collect(Collectors.toList());
    }
  }
}
