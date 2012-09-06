/*
 * Sonar Java
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
package org.sonar.java.bytecode.visitor;

import org.sonar.java.bytecode.asm.*;
import org.sonar.squid.api.SourceCodeEdgeUsage;
import org.sonar.squid.measures.Metric;

import java.util.*;

public class LCOM4Visitor extends BytecodeVisitor {

  private AsmClass asmClass;
  private List<Set<AsmResource>> unrelatedBlocks = null;
  private final Set<String> fieldsToExcludeFromLcom4Calculation;

  public LCOM4Visitor(Set<String> fieldsToExcludeFromLcom4Calculation) {
    this.fieldsToExcludeFromLcom4Calculation = fieldsToExcludeFromLcom4Calculation;
  }

  @Override
  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
    unrelatedBlocks = new ArrayList<Set<AsmResource>>();
  }

  @Override
  public void visitMethod(AsmMethod asmMethod) {
    if (isMethodElligibleForLCOM4Computation(asmMethod)) {
      ensureBlockIsCreated(asmMethod);
      for (AsmEdge edge : asmMethod.getOutgoingEdges()) {
        if (isCallToInternalFieldOrMethod(edge) && isNotCallToExcludedFieldFromLcom4Calculation(edge.getTo())) {
          AsmResource toResource = getAccessedFieldOrMethod(edge.getTo());
          linkAsmResources(asmMethod, toResource);
        }
      }
    }
  }

  private AsmResource getAccessedFieldOrMethod(AsmResource resource) {
    if (resource instanceof AsmMethod && ((AsmMethod) resource).isAccessor()) {
      return ((AsmMethod) resource).getAccessedField();
    } else {
      return resource;
    }
  }

  private boolean isNotCallToExcludedFieldFromLcom4Calculation(AsmResource to) {
    if (to instanceof AsmField) {
      AsmField field = (AsmField) to;
      return !fieldsToExcludeFromLcom4Calculation.contains(field.getName());
    }
    return true;
  }

  private boolean isMethodElligibleForLCOM4Computation(AsmMethod asmMethod) {
    return !asmMethod.isAbstract() && !asmMethod.isStatic() && !asmMethod.isConstructor() && !asmMethod.isEmpty()
        && !asmMethod.isAccessor() && asmMethod.isBodyLoaded();
  }

  private void removeIsolatedMethodBlocks() {
    Iterator<Set<AsmResource>> iterator = unrelatedBlocks.iterator();

    while (iterator.hasNext()) {
      Set<AsmResource> block = iterator.next();
      if (block.size() == 1) {
        iterator.remove();
      }
    }

  }

  @Override
  public void leaveClass(AsmClass asmClass) {
    removeIsolatedMethodBlocks();

    int lcom4 = unrelatedBlocks.size();
    if (lcom4 == 0) {
      lcom4 = 1;
    }

    getSourceClass(asmClass).add(Metric.LCOM4, lcom4);
    getSourceClass(asmClass).addData(Metric.LCOM4_BLOCKS, unrelatedBlocks);

    if (isMainPublicClassInFile(asmClass)) {
      getSourceFile(asmClass).add(Metric.LCOM4, lcom4);
      getSourceFile(asmClass).addData(Metric.LCOM4_BLOCKS, unrelatedBlocks);
    }
  }

  private void ensureBlockIsCreated(AsmResource resource) {
    getOrCreateResourceBlock(resource);
  }

  private void linkAsmResources(AsmResource resourceA, AsmResource resourceB) {
    Set<AsmResource> blockA = getOrCreateResourceBlock(resourceA);
    Set<AsmResource> blockB = getOrCreateResourceBlock(resourceB);

    // getOrCreateResourceBlock() returns the same block instance if resourceA and resourceB are identical or already in the same block
    // TODO: Avoid this violation by using a Disjoint Union Set which is also more efficient performance-wise
    // See: http://en.wikipedia.org/wiki/Disjoint-set_data_structure
    if (blockA == blockB) { // NOSONAR false-positive Compare Objects With Equals
      return;
    }

    blockA.addAll(blockB);
    unrelatedBlocks.remove(blockB);
  }

  private boolean isCallToInternalFieldOrMethod(AsmEdge edge) {
    return edge.getTargetAsmClass() == asmClass && (edge.getUsage() == SourceCodeEdgeUsage.CALLS_FIELD || edge.getUsage() == SourceCodeEdgeUsage.CALLS_METHOD);
  }

  private Set<AsmResource> getOrCreateResourceBlock(AsmResource resource) {
    for (Set<AsmResource> block : unrelatedBlocks) {
      if (block.contains(resource)) {
        return block;
      }
    }

    Set<AsmResource> block = new HashSet<AsmResource>();
    block.add(resource);
    unrelatedBlocks.add(block);
    return block;
  }

}
