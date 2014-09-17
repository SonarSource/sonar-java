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
package org.sonar.plugins.java.bridges;

import com.google.common.collect.Lists;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.CheckFactory;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.java.JavaSquid;
import org.sonar.java.bytecode.visitor.DSMMapping;

import java.util.ArrayList;
import java.util.List;

public final class BridgeFactory {

  private BridgeFactory() {
    // only static methods
  }

  private static List<Bridge> create(boolean skipPackageDesignAnalysis) {
    List<Bridge> result = Lists.<Bridge>newArrayList(new ChecksBridge());
    if (!skipPackageDesignAnalysis) {
      result.add(new DesignBridge());
    }
    return result;
  }

  public static List<Bridge> create(boolean bytecodeScanned, boolean skipPackageDesignAnalysis, SensorContext context, CheckFactory checkFactory,
                                    JavaSquid squid, RulesProfile profile, DSMMapping DSMMapping) {
    List<Bridge> result = new ArrayList<Bridge>();
    for (Bridge bridge : create(skipPackageDesignAnalysis)) {
      bridge.setCheckFactory(checkFactory);
      if (!bridge.needsBytecode() || bytecodeScanned) {
        bridge.setContext(context);
        bridge.setGraph(squid.getGraph());
        bridge.setProfile(profile);
        bridge.setDSMMapping(DSMMapping);
        result.add(bridge);
      }
    }
    return result;
  }

}
