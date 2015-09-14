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
package org.sonar.java.bytecode.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.HashSet;
import java.util.Set;

public class AsmSignatureVisitor extends SignatureVisitor {

  private final Set<String> internalNames = new HashSet<String>();


  public AsmSignatureVisitor() {
    super(Opcodes.ASM5);
  }

  public Set<String> getInternalNames() {
    return internalNames;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void visitClassType(String name) {
    internalNames.add(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SignatureVisitor visitArrayType() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void visitBaseType(char descriptor) {
    // No operation
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SignatureVisitor visitClassBound() {
    return this;
  }

  @Override
  public SignatureVisitor visitExceptionType() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void visitFormalTypeParameter(String name) {
    // No operation
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void visitInnerClassType(String name) {
    // No operation
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SignatureVisitor visitInterface() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SignatureVisitor visitInterfaceBound() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SignatureVisitor visitParameterType() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SignatureVisitor visitReturnType() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SignatureVisitor visitSuperclass() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void visitTypeArgument() {
    // No operation
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SignatureVisitor visitTypeArgument(char wildcard) {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void visitTypeVariable(String name) {
    // No operation
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void visitEnd() {
    // No operation
  }

}
