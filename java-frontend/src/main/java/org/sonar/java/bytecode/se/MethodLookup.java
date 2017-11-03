/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.Convert;
import org.sonar.java.resolve.Flags;
import org.sonar.java.resolve.Java9Support;

public class MethodLookup {

  private static final Logger LOG = Loggers.get(MethodLookup.class);
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
  public static MethodLookup lookup(String signature, SquidClassLoader classLoader, MethodVisitor methodVisitor) {
    String className = signature.substring(0, signature.indexOf('#'));
    return lookup(className, signature, classLoader, methodVisitor);
  }

  private static MethodLookup lookup(String className, String signature, SquidClassLoader classLoader, MethodVisitor methodVisitor) {
    byte[] bytes = getClassBytes(className, classLoader);
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

  @CheckForNull
  private static byte[] getClassBytes(String className, SquidClassLoader classLoader) {
    try (InputStream is = classLoader.getResourceAsStream(Convert.bytecodeName(className) + ".class")) {
      if (is == null) {
        LOG.debug(".class not found for {}", className);
        return null;
      }
      byte[] bytes = ByteStreams.toByteArray(is);
      // to read bytecode with ASM not supporting Java 9, we will set major version to Java 8
      if (Java9Support.isJava9Class(bytes)) {
        Java9Support.setJava8MajorVersion(bytes);
      }
      return bytes;
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private static class LookupClassVisitor extends ClassVisitor {

    private final MethodVisitor methodVisitor;
    private final String methodSignature;
    private boolean isFinalClass = false;
    private boolean methodFound;
    private String superClassName;
    private String[] interfaces;
    private List<String> declaredExceptions;
    private boolean isStatic;
    private boolean isVarArgs;

    public LookupClassVisitor(MethodVisitor methodVisitor, String targetedMethodSignatures) {
      super(Opcodes.ASM5);
      this.methodVisitor = methodVisitor;
      this.methodSignature = targetedMethodSignatures;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      isFinalClass = Flags.isFlagged(Flags.filterAccessBytecodeFlags(access), Flags.FINAL);
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
        if (isOverridableOrNativeMethod(access)) {
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

    private boolean isOverridableOrNativeMethod(int methodFlags) {
      if (Flags.isFlagged(methodFlags, Flags.NATIVE)) {
        return true;
      }
      return Flags.isFlagged(methodFlags, Flags.ABSTRACT) || !(isFinalClass || Flags.isFlagged(methodFlags, Flags.PRIVATE | Flags.FINAL | Flags.STATIC));
    }
  }

}
