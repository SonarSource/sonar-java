/*
 * $Id: XmlDefinitionsSet.java 471754 2006-11-06 14:55:09Z husted $
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.struts.tiles.NoSuchDefinitionException;

/**
 * A set of definitions read from XML definitions file.
*/
public class XmlDefinitionsSet
{
    /** Defined definitions. */
  protected Map definitions;

     /**
      * Constructor.
      */
  public XmlDefinitionsSet()
   {
   definitions = new HashMap();
   }

  /**
   * Put definition in set.
   * @param definition Definition to add.
   */
  public void putDefinition(XmlDefinition definition)
  {
  definitions.put( definition.getName(), definition );
  }

  /**
   * Get requested definition.
   * @param name Definition name.
   */
  public XmlDefinition getDefinition(String name)
  {
  return (XmlDefinition)definitions.get( name );
  }

  /**
   * Get definitions map.
   */
  public Map getDefinitions()
  {
  return definitions;
  }

  /**
   * Resolve extended instances.
   */
  public void resolveInheritances() throws NoSuchDefinitionException
    {
      // Walk through all definitions and resolve individual inheritance
    Iterator i = definitions.values().iterator();
    while( i.hasNext() )
      {
      XmlDefinition definition = (XmlDefinition)i.next();
      definition.resolveInheritance( this );
      }  // end loop
    }

  /**
   * Add definitions from specified child definitions set.
   * For each definition in child, look if it already exists in this set.
   * If not, add it, if yes, overload parent's definition with child definition.
   * @param child Definition used to overload this object.
   */
  public void extend( XmlDefinitionsSet child )
    {
    if(child==null)
      return;
    Iterator i = child.getDefinitions().values().iterator();
    while( i.hasNext() )
      {
      XmlDefinition childInstance = (XmlDefinition)i.next();
      XmlDefinition parentInstance = getDefinition(childInstance.getName() );
      if( parentInstance != null )
        {
        parentInstance.overload( childInstance );
        }
       else
        putDefinition( childInstance );
      } // end loop
    }
    /**
     * Get String representation.
     */
  public String toString()
    {
    return "definitions=" + definitions.toString() ;
    }

}
