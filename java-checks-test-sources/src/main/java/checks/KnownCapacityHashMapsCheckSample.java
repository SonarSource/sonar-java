/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package checks;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

class KnownCapacityHashMapsCheckSample {

  private static final int cap = 1;

  void nonCompliant() {
    HashMap<String, String> hashMap2 = new HashMap<>(100); // Noncompliant
    HashSet<String> hashSet2 = new HashSet<>(100); // Noncompliant
    Integer capacity = 10;
    HashMap<String, String> hm = new HashMap<>(capacity); // Noncompliant
    HashSet<String> hs = new HashSet<>(cap); // Noncompliant
    WeakHashMap<String, String> whm = new WeakHashMap<>(2); // Noncompliant
  }

  void compliant() {
    HashMap<String, String> hashMap = new HashMap<>();
    HashSet<String> hashSet = new HashSet<>();

    HashMap<String, String> hashMap2 = new HashMap<>(Map.of("key", "val"));
    HashSet<String> hashSet2 = new HashSet<>(Collections.singleton(""));
    WeakHashMap<String, String> whm2 = new WeakHashMap<>(2, 2f); 
  }

}
