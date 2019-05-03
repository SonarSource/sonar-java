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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map.Entry;

/**
 * Predefined symbols.
 */
public class Symbols {

  static final JavaSymbol.PackageJavaSymbol rootPackage;
  final JavaSymbol.PackageJavaSymbol defaultPackage;

  /**
   * Owns all predefined symbols (builtin types, operators).
   */
  final JavaSymbol.TypeJavaSymbol predefClass;

  /**
   * Type, which can't be modelled for the moment.
   */
  public static final UnknownType unknownType;
  public static final JavaSymbol.TypeJavaSymbol unknownSymbol;
  public static final JavaSymbol.MethodJavaSymbol unknownMethodSymbol;

  final JavaSymbol.TypeJavaSymbol arrayClass;

  final JavaSymbol.TypeJavaSymbol methodClass;
  final JavaSymbol.TypeJavaSymbol noSymbol;

  // builtin types
  final JavaType byteType;
  final JavaType charType;
  final JavaType shortType;
  final JavaType intType;
  final JavaType longType;
  final JavaType floatType;
  final JavaType doubleType;
  final JavaType booleanType;
  final JavaType nullType;
  final JavaType voidType;

  final BiMap<JavaType, JavaType> boxedTypes;

  // predefined types

  /**
   * {@link java.lang.Object}
   */
  final JavaType objectType;

  final JavaType cloneableType;
  final JavaType serializableType;
  final JavaType classType;
  final JavaType stringType;

  final WildCardType unboundedWildcard;

  /**
   * {@link java.lang.annotation.Annotation}
   */
  final JavaType annotationType;

  /**
   * {@link java.lang.Enum}
   */
  final JavaType enumType;

  static {
    rootPackage = new JavaSymbol.PackageJavaSymbol("", null);
    unknownSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "!unknownSymbol!", rootPackage) {
      @Override
      public void addUsage(IdentifierTree tree) {
        // noop
      }

      @Override
      public boolean isTypeSymbol() {
        return false;
      }

      @Override
      public boolean isUnknown() {
        return true;
      }
    };
    unknownSymbol.members = new Scope(unknownSymbol) {
      @Override
      public void enter(JavaSymbol symbol) {
        // noop
      }

    };
    unknownType = new UnknownType(unknownSymbol);
    unknownSymbol.type = unknownType;
    unknownMethodSymbol = new JavaSymbol.MethodJavaSymbol(0, "!unknown!", unknownSymbol) {
      @Override
      public boolean isMethodSymbol() {
        return false;
      }

      @Override
      public TypeJavaSymbol getReturnType() {
        return unknownSymbol;
      }

      @Override
      public TypeSymbol returnType() {
        return unknownSymbol;
      }

      @Override
      public boolean isUnknown() {
        return true;
      }
    };
  }

  public Symbols(BytecodeCompleter bytecodeCompleter) {
    defaultPackage = new JavaSymbol.PackageJavaSymbol("", rootPackage);

    predefClass = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "", rootPackage);
    predefClass.members = new Scope(predefClass);
    ((ClassJavaType) predefClass.type).interfaces = Collections.emptyList();

    // TODO should have type "noType":
    noSymbol = new JavaSymbol.TypeJavaSymbol(0, "", rootPackage);

    methodClass = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "", noSymbol);

    // builtin types
    byteType = initType(JavaType.BYTE, "byte");
    charType = initType(JavaType.CHAR, "char");
    shortType = initType(JavaType.SHORT, "short");
    intType = initType(JavaType.INT, "int");
    longType = initType(JavaType.LONG, "long");
    floatType = initType(JavaType.FLOAT, "float");
    doubleType = initType(JavaType.DOUBLE, "double");
    booleanType = initType(JavaType.BOOLEAN, "boolean");
    nullType = initType(JavaType.BOT, "<nulltype>");
    voidType = initType(JavaType.VOID, "void");

    bytecodeCompleter.init(this);

    // predefined types for java lang
    JavaSymbol.PackageJavaSymbol javalang = bytecodeCompleter.enterPackage("java.lang");
    // define a star import scope to let resolve types to java.lang when needed.
    javalang.members = new Scope.StarImportScope(javalang, bytecodeCompleter);
    javalang.members.enter(javalang);

    objectType = bytecodeCompleter.loadClass("java.lang.Object").type;
    classType = bytecodeCompleter.loadClass("java.lang.Class").type;
    stringType = bytecodeCompleter.loadClass("java.lang.String").type;
    cloneableType = bytecodeCompleter.loadClass("java.lang.Cloneable").type;
    serializableType = bytecodeCompleter.loadClass("java.io.Serializable").type;
    annotationType = bytecodeCompleter.loadClass("java.lang.annotation.Annotation").type;
    enumType = bytecodeCompleter.loadClass("java.lang.Enum").type;

    unboundedWildcard = new WildCardType(objectType, WildCardType.BoundType.UNBOUNDED);

    // Associate boxed types
    boxedTypes = HashBiMap.create();
    boxedTypes.put(byteType, bytecodeCompleter.loadClass("java.lang.Byte").type);
    boxedTypes.put(charType, bytecodeCompleter.loadClass("java.lang.Character").type);
    boxedTypes.put(shortType, bytecodeCompleter.loadClass("java.lang.Short").type);
    boxedTypes.put(intType, bytecodeCompleter.loadClass("java.lang.Integer").type);
    boxedTypes.put(longType, bytecodeCompleter.loadClass("java.lang.Long").type);
    boxedTypes.put(floatType, bytecodeCompleter.loadClass("java.lang.Float").type);
    boxedTypes.put(doubleType, bytecodeCompleter.loadClass("java.lang.Double").type);
    boxedTypes.put(booleanType, bytecodeCompleter.loadClass("java.lang.Boolean").type);

    for (Entry<JavaType, JavaType> entry : boxedTypes.entrySet()) {
      entry.getKey().primitiveWrapperType = entry.getValue();
      entry.getValue().primitiveType = entry.getKey();
    }

    arrayClass = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "Array", noSymbol);
    ClassJavaType arrayClassType = (ClassJavaType) arrayClass.type;
    arrayClassType.supertype = objectType;
    arrayClassType.interfaces = ImmutableList.of(cloneableType, serializableType);
    // clone method return type is handled during method resolution.
    arrayClass.members = new Scope(arrayClass);
    arrayClass.members().enter(new JavaSymbol.VariableJavaSymbol(Flags.PUBLIC | Flags.FINAL, "length", intType, arrayClass));

    // java.lang.Synthetic is a virtual annotation added by ASM to workaround a bug in javac on inner classes parameter numbers.
    // Predefining this type avoids to look it up in classpath where it will not be found. We rely on this to detect synthetic parameters on some enum constructor for instance.
    JavaSymbol.TypeJavaSymbol syntheticAnnotation = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC | Flags.ANNOTATION, "Synthetic", javalang);
    javalang.members.enter(syntheticAnnotation);
    bytecodeCompleter.registerClass(syntheticAnnotation);
    enterOperators();
  }

  /**
   * Registers builtin types as symbols, so that they can be found as an usual identifiers.
   */
  private JavaType initType(int tag, String name) {
    JavaSymbol.TypeJavaSymbol symbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, name, rootPackage);
    symbol.members = new Scope(symbol);
    predefClass.members.enter(symbol);
    ((ClassJavaType) symbol.type).interfaces = Collections.emptyList();
    symbol.type.tag = tag;
    return symbol.type;
  }

  /**
   * Registers operators as methods, so that they can be found as an usual methods.
   */
  private void enterOperators() {
    for (String op : new String[] {"+", "-", "*", "/", "%"}) {
      for (JavaType type : Arrays.asList(doubleType, floatType, longType, intType)) {
        enterBinop(op, type, type, type);
      }
    }
    for (String op : new String[] {"&", "|", "^"}) {
      for (JavaType type : Arrays.asList(booleanType, longType, intType)) {
        enterBinop(op, type, type, type);
      }
    }
    for (String op : new String[] {"<<", ">>", ">>>"}) {
      enterBinop(op, longType, longType, longType);
      enterBinop(op, intType, longType, intType);
      enterBinop(op, longType, intType, longType);
      enterBinop(op, intType, intType, intType);
    }
    for (String op : new String[] {"<", ">", ">=", "<="}) {
      for (JavaType type : Arrays.asList(doubleType, floatType, longType, intType)) {
        enterBinop(op, type, type, booleanType);
      }
    }
    for (String op : new String[] {"==", "!="}) {
      for (JavaType type : Arrays.asList(objectType, booleanType, doubleType, floatType, longType, intType)) {
        enterBinop(op, type, type, booleanType);
      }
    }
    enterBinop("&&", booleanType, booleanType, booleanType);
    enterBinop("||", booleanType, booleanType, booleanType);

    // string concatenation
    for (JavaType type : Arrays.asList(nullType, objectType, booleanType, doubleType, floatType, longType, intType)) {
      enterBinop("+", stringType, type, stringType);
      enterBinop("+", type, stringType, stringType);
    }
    enterBinop("+", stringType, stringType, stringType);
  }

  private void enterBinop(String name, JavaType left, JavaType right, JavaType result) {
    JavaType type = new MethodJavaType(ImmutableList.of(left, right), result, Collections.emptyList(), methodClass);
    JavaSymbol symbol = new JavaSymbol.MethodJavaSymbol(Flags.PUBLIC | Flags.STATIC, name, type, predefClass);
    predefClass.members.enter(symbol);
  }

  public JavaType getPrimitiveFromDescriptor(char descriptor) {
    switch (descriptor) {
      case 'S':
        return shortType;
      case 'I':
        return intType;
      case 'C':
        return charType;
      case 'Z':
        return booleanType;
      case 'B':
        return byteType;
      case 'J':
        return longType;
      case 'F':
        return floatType;
      case 'D':
        return doubleType;
      case 'V':
        return voidType;
      default:
        throw new IllegalStateException("Descriptor '" + descriptor + "' cannot be mapped to a primitive type");
    }
  }

  public JavaType deferedType(AbstractTypedTree tree) {
    return new DeferredType(tree);
  }

  public JavaType deferedType(JavaType uninferedType) {
    return new DeferredType(uninferedType);
  }
}
