/*
 * $Id: AttributeToScopeTag.java 471754 2006-11-06 14:55:09Z husted $
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

package org.apache.struts.tiles.taglib;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.struts.tiles.taglib.util.TagUtils;
import org.apache.struts.tiles.ComponentContext;


/**
  *  Custom  tag  that  puts  component's  attributes  in  a  scope  (request,  page,  ...).
  *  @deprecated  Is  it  still  in  use  ?
  */
public  final  class  AttributeToScopeTag  extends  TagSupport  {


        //  ----------------------------------------------------- Instance Variables


    /**
     * The scope name.
     */
    private String scopeName = null;

    /**
     * The scope value.
     */
    private int scope = PageContext.PAGE_SCOPE;



    /**
     * The property name to be exposed.
     */
    private String property = null;


    // ------------------------------------------------------------- Properties



    /**
     * Return the property name.
     */
    public String getProperty()
    {
    return  (this.property);
    }


     /**
       *  Set  the  property  name.
       *
       *  @param  property  The  property  name
       */
    public  void  setProperty(String  property)
    {
    this.property  =  property;
    }

    /**
      *  Set  the  scope.
      *
      *  @param  scope  The  new  scope
      */
    public  void  setScope(String  scope)
    {
    this.scopeName  =  scope;
    }

        //  ---------------------------------------------------------  Public  Methods


        /**
          *  Expose  the  requested  property  from  component  context.
          *
          *  @exception  JspException  if  a  JSP  exception  has  occurred
          */
    public  int  doStartTag()  throws  JspException
        {
        if(  id==null  )
            id=property;

        ComponentContext  compContext  =  (ComponentContext)pageContext.getAttribute(  ComponentConstants.COMPONENT_CONTEXT,  PageContext.REQUEST_SCOPE);

        if(  compContext  ==  null  )
            throw  new  JspException  (  "Error  -  tag.useProperty  :  component  context  is  not  defined.  Check  tag  syntax"  );

        Object  value  =  compContext.getAttribute(property);
        if(  value  ==  null  )
            throw  new  JspException  (  "Error  -  tag.useProperty  :  property  '"+  property  +  "'  not  found  in  context.  Check  tag  syntax"  );

        if(  scopeName  !=  null  )
            {
            scope  =  TagUtils.getScope(  scopeName,  PageContext.PAGE_SCOPE  );
            pageContext.setAttribute(id,  value,  scope);
            }
          else
            pageContext.setAttribute(id,  value);

          //  Continue  processing  this  page
        return  SKIP_BODY;
        }




        /**
          *  Clean  up  after  processing  this  enumeration.
          *
          *  @exception  JspException  if  a  JSP  exception  has  occurred
          */
    public  int  doEndTag()  throws  JspException
        {
        return  (EVAL_PAGE);
        }

        /**
          *  Release  all  allocated  resources.
          */
        public  void  release()  {

                super.release();
                property  =  null;
                scopeName  =  null;
                scope  =  PageContext.PAGE_SCOPE;
        }

}
