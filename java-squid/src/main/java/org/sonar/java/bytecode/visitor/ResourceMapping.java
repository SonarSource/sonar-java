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
package org.sonar.java.bytecode.visitor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import org.sonar.api.design.Dependency;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Resource;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ResourceMapping {

  private Multimap<Directory, File> directories;
  private Multimap<Dependency, Dependency> subDependencies;
  private Map<File, String> fileKeyByResource;

  public ResourceMapping() {
    directories = ArrayListMultimap.create();
    subDependencies = ArrayListMultimap.create();
    fileKeyByResource = new HashMap<>();
  }

  public void addResource(File resource, String fileKey) {
    directories.put(resource.getParent(), resource);
    fileKeyByResource.put(resource, fileKey);
  }

  public String getFileKeyByResource(File resource) {
    return fileKeyByResource.get(resource);
  }

  public Set<Resource> directories() {
    //order of directories. Required for package cycle reliability
    return ImmutableSortedSet.orderedBy(new Comparator<Resource>() {
      @Override
      public int compare(Resource resource, Resource resource2) {
        return resource.getKey().compareTo(resource2.getKey());
      }
    }).addAll(directories.keySet()).build();
  }

  public Collection<Resource> files(Directory directory) {
    return ImmutableSet.<Resource>builder().addAll(directories.get(directory)).build();
  }

  public void addSubDependency(Dependency parent, Dependency subDependency) {
    subDependencies.put(parent, subDependency);
  }

  public Collection<Dependency> getSubDependencies(Dependency parent) {
    return subDependencies.get(parent);
  }


}
