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

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AsmClassProviderImpl extends AsmClassProvider {

  private static final Logger LOG = LoggerFactory.getLogger(AsmClassProviderImpl.class);

  private final ClassLoader classLoader;
  private final Map<String, AsmClass> asmClassCache = new HashMap<String, AsmClass>();

  public AsmClassProviderImpl() {
    this.classLoader = Thread.currentThread().getContextClassLoader();
  }

  public AsmClassProviderImpl(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public AsmClass getClass(String internalName, DETAIL_LEVEL level) {
    if (internalName == null) {
      // TODO Godin: I believe that we should throw IllegalArgumentException instead
      throw new IllegalStateException("You can try to load a class whose internalName = 'null'");
    }
    AsmClass asmClass = getAsmClassFromCacheOrCreateIt(internalName);
    if (level.isGreaterThan(asmClass.getDetailLevel())) {
      decoracteAsmClassFromBytecode(asmClass, level);
    }
    return asmClass;
  }

  private AsmClass getAsmClassFromCacheOrCreateIt(String internalName) {
    AsmClass asmClass = asmClassCache.get(internalName);
    if (asmClass == null) {
      asmClass = new AsmClass(internalName, DETAIL_LEVEL.NOTHING);
      asmClassCache.put(internalName, asmClass);
    }
    return asmClass;
  }

  private void decoracteAsmClassFromBytecode(AsmClass asmClass, DETAIL_LEVEL level) {
    InputStream input = null;
    try {

      AsmClassVisitor classVisitor = new AsmClassVisitor(this, asmClass, level);
      input = classLoader.getResourceAsStream(asmClass.getInternalName() + ".class");
      ClassReader asmReader = new ClassReader(input);
      asmReader.accept(classVisitor, 0);
    } catch (IOException e) {
      LOG.warn("Class '" + asmClass.getInternalName() + "' is not accessible through the ClassLoader.");
    } catch (SecurityException e) {
      LOG.warn("Class '" + asmClass.getInternalName() + "' is not accessible through the ClassLoader. One signed jar seems to be corrupted.");
    } catch (Exception e) {
      LOG.error("Unable to process bytecode of class '" + asmClass.getInternalName() + "'", e);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

}
