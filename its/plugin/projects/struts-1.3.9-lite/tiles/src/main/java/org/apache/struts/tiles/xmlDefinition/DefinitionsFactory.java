/*
 * $Id: DefinitionsFactory.java 471754 2006-11-06 14:55:09Z husted $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.struts.tiles.xmlDefinition;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.apache.struts.tiles.ComponentDefinition;
import org.apache.struts.tiles.DefinitionsFactoryException;
import org.apache.struts.tiles.NoSuchDefinitionException;

/**
 * A factory for definitions.
 * This factory allows to retrieve definitions by their keys.
 */
public class DefinitionsFactory implements Serializable
{
     /** Underlying map containing all definitions.*/
   protected Map definitions;

   /**
     * Get a definition by its name.
     * @param name Name of the definition.
     * @param request Servlet request.
     * @param servletContext Servlet context.
     * @throws DefinitionsFactoryException An error occur while getting
     * definition.
     * @throws NoSuchDefinitionException No definition found for specified name
     * Implementation can throw more accurate exception as a subclass of this
     * exception.
     */
   public ComponentDefinition getDefinition(String name, ServletRequest request, ServletContext servletContext)
             throws NoSuchDefinitionException, DefinitionsFactoryException
   {
   return (ComponentDefinition)definitions.get(name);
   }

  /**
   * Put definition in set.
   * @param definition Definition to put.
   */
  public void putDefinition(ComponentDefinition definition)
  {
  definitions.put( definition.getName(), definition );
  }

   /**
    * Constructor.
    * Create a factory initialized with definitions from {@link XmlDefinitionsSet}.
    * @param xmlDefinitions Resolved definition from XmlDefinitionSet.
    * @throws NoSuchDefinitionException If an error occurs while resolving inheritance
    */
   public DefinitionsFactory(XmlDefinitionsSet xmlDefinitions)
    throws NoSuchDefinitionException
    {
    definitions = new HashMap();

      // First, resolve inheritance
    xmlDefinitions.resolveInheritances();

      // Walk thru xml set and copy each definitions.
    Iterator i = xmlDefinitions.getDefinitions().values().iterator();
    while( i.hasNext() )
      {
      XmlDefinition xmlDefinition = (XmlDefinition)i.next();
        putDefinition( new ComponentDefinition( xmlDefinition) );
      }  // end loop
   }
    /**
     * Return String representation.
     * @return String representation.
     */
  public String toString()
    {
    return definitions.toString();
    }

}
