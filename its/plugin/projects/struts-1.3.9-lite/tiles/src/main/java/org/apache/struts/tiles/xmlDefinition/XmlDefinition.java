/*
 * $Id: XmlDefinition.java 471754 2006-11-06 14:55:09Z husted $
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

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.tiles.ComponentDefinition;
import org.apache.struts.tiles.NoSuchDefinitionException;

/**
  *A definition read from an XML definitions file.
  */
public class XmlDefinition extends ComponentDefinition
{
  /**
   * Extends attribute value.
   */
  private String inherit;

    /** Commons Logging instance. */
   protected static Log log = LogFactory.getLog(XmlDefinition.class);

  /**
   * Used for resolving inheritance.
   */
  private boolean isVisited=false;


     /**
      * Constructor.
      */
   public XmlDefinition()
   {
   super();
   //if(debug)
     //System.out.println( "create definition" );
   }

  /**
   * Add an attribute to this component.
   *
   * @param attribute Attribute to add.
   */
  public void addAttribute( XmlAttribute attribute)
    {
    putAttribute( attribute.getName(), attribute.getValue() );
    }

  /**
   * Set extends.
   *
   * @param name Name of the extended definition.
   */
  public void setExtends(String name)
    {
    inherit = name;
    }

  /**
   * Get extends.
   *
   * @return Name of the extended definition.
   */
  public String getExtends()
    {
    return inherit;
    }

  /**
   * Get extends flag.
   *
   */
  public boolean isExtending( )
    {
    return inherit!=null;
    }

  /**
   * Set isVisited.
   *
   */
  public void setIsVisited( boolean isVisited )
    {
    this.isVisited = isVisited;
    }

    /**
     * Resolve inheritance.
     * First, resolve parent's inheritance, then set path to the parent's path.
     * Also copy attributes setted in parent, and not set in child
     * If instance doesn't extend anything, do nothing.
     * @throws NoSuchDefinitionException If an inheritance can not be solved.
     */
  public void resolveInheritance( XmlDefinitionsSet definitionsSet )
    throws NoSuchDefinitionException
    {
      // Already done, or not needed ?
    if( isVisited || !isExtending() )
      return;

    if(log.isDebugEnabled())
      log.debug( "Resolve definition for child name='" + getName()
              + "' extends='" + getExtends() + "'.");

      // Set as visited to avoid endless recurisvity.
    setIsVisited( true );

      // Resolve parent before itself.
    XmlDefinition parent = definitionsSet.getDefinition( getExtends() );
    if( parent == null )
      { // error
      String msg = "Error while resolving definition inheritance: child '"
                           + getName() +    "' can't find its ancestor '"
                           + getExtends() +
                           "'. Please check your description file.";
      log.error( msg );
        // to do : find better exception
      throw new NoSuchDefinitionException( msg );
      }

    parent.resolveInheritance( definitionsSet );

      // Iterate on each parent's attribute and add it if not defined in child.
    Iterator parentAttributes = parent.getAttributes().keySet().iterator();
    while( parentAttributes.hasNext() )
      {
      String name = (String)parentAttributes.next();
      if( !getAttributes().containsKey(name) )
        putAttribute( name, parent.getAttribute(name) );
      }
      // Set path and role if not setted
    if( path == null )
      setPath( parent.getPath() );
    if( role == null )
      setRole( parent.getRole() );
    if( controller==null )
      {
      setController( parent.getController());
      setControllerType( parent.getControllerType());
      }
    }

  /**
   * Overload this definition with passed child.
   * All attributes from child are copied to this definition. Previous
   * attributes with same name are disguarded.
   * Special attribute 'path','role' and 'extends' are overloaded if defined
   * in child.
   * @param child Child used to overload this definition.
   */
  public void overload( XmlDefinition child )
    {
    if( child.getPath() != null )
      {
      path = child.getPath();
      }
    if( child.getExtends() != null )
      {
      inherit = child.getExtends();
      }
    if( child.getRole() != null )
      {
      role = child.getRole();
      }
    if( child.getController()!=null )
      {
      controller = child.getController();
      controllerType =  child.getControllerType();
      }
      // put all child attributes in parent.
    attributes.putAll( child.getAttributes());
    }
}
