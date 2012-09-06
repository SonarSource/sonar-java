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
package org.sonar.plugins.java.bridges;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Resource;
import org.sonar.java.api.JavaClass;
import org.sonar.java.api.JavaMethod;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceMethod;
import org.sonar.squid.measures.Metric;

public final class PublicUndocumentedApiBridge extends Bridge {

  protected PublicUndocumentedApiBridge() {
    super(false);
  }

  @Override
  public void onFile(SourceFile squidFile, Resource sonarFile) {
    copyValue(squidFile, sonarFile);
  }

  @Override
  public void onClass(SourceClass squidClass, JavaClass sonarClass) {
    copyValue(squidClass, sonarClass);
  }

  @Override
  public void onMethod(SourceMethod squidMethod, JavaMethod sonarMethod) {
    copyValue(squidMethod, sonarMethod);
  }

  private void copyValue(SourceCode squidResource, Resource sonarResource) {
    double undocumentedApi = squidResource.getDouble(Metric.PUBLIC_API) - squidResource.getInt(Metric.PUBLIC_DOC_API);
    context.saveMeasure(sonarResource, CoreMetrics.PUBLIC_UNDOCUMENTED_API, undocumentedApi);
  }

}
