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
package org.sonar.java.resolve;

import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumSet;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.sonar.java.resolve.BytecodeCompleter.ASM_API_VERSION;

public class AsmExample {
  enum Flag {
    PUBLIC,
    PRIVATE,
    PROTECTED,
    STATIC,
    FINAL,
    SYNCHRONIZED,
    VOLATILE,
    TRANSIENT,
    NATIVE,
    INTERFACE,
    ABSTRACT,
    STRICTFP,
    SYNTHETIC,
    ANNOTATION,
    ENUM;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  public static void main(String[] args) throws Exception {
    ClassVisitor cv = new ClassVisitor(ASM_API_VERSION) {
      @Override
      public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        System.out.println("CLASS");
        System.out.println("access: " + asFlagSet(access));
        System.out.println("name: " + name);
        System.out.println("signature: " + signature);
        System.out.println("superName: " + superName);
        System.out.println("interfaces: " + Arrays.toString(interfaces));
        System.out.println();
      }

      @Override
      public void visitSource(String source, String debug) {
        System.out.println("SOURCE");
        System.out.println();
      }

      @Override
      public void visitOuterClass(String owner, String name, String desc) {
        System.out.println("OUTER CLASS");
        System.out.println("owner: " + owner);
        System.out.println("name: " + name);
        System.out.println("desc: " + desc);
        System.out.println();
      }

      @Override
      public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        System.out.println("ANNOTATION");
        System.out.println("desc: " + desc);
        System.out.println("visible: " + visible);
        return null;
      }

      @Override
      public void visitAttribute(Attribute attr) {
        System.out.println("ATTRIBUTE");
        System.out.println();
      }

      @Override
      public void visitInnerClass(String name, String outerName, String innerName, int access) {
        System.out.println("INNER CLASS");
        System.out.println("access: " + asFlagSet(access));
        System.out.println("name: " + name);
        System.out.println("outerName: " + outerName);
        System.out.println("innerName: " + innerName);
        System.out.println();
      }

      @Override
      public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        System.out.println("FIELD");
        System.out.println("access: " + asFlagSet(access));
        System.out.println("name: " + name);
        System.out.println("desc: " + desc);
        System.out.println("signature: " + desc);
        System.out.println();
        return null;
      }

      @Override
      public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        System.out.println("METHOD");
        System.out.println("access: " + asFlagSet(access));
        System.out.println("name: " + name);
        System.out.println("desc: " + desc);
        System.out.println("signature: " + desc);
        System.out.println("exceptions: " + Arrays.toString(exceptions));
        System.out.println();
        return null;
      }

      @Override
      public void visitEnd() {
        System.out.println("END");
      }

      public EnumSet<Flag> asFlagSet(int flags) {
        EnumSet<Flag> result = EnumSet.noneOf(Flag.class);
        int mask = 1;
        for (int i = 0; i < 15; i++) {
          if ((flags & mask) != 0) {
            result.add(Flag.values()[i]);
          }
          mask = mask << 1;
        }
        return result;
      }

    };

    InputStream in = AsmExample.class.getResourceAsStream("/org/sonar/java/resolve/AsmExample.class");
    ClassReader classReader = new ClassReader(in);
    classReader.accept(cv, 0);
  }

}
