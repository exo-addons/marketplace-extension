/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.community.portlet.addon.search;


import java.util.List;


import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.exoplatform.addon.service.AddOnService;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;

import org.exoplatform.webui.core.lifecycle.Lifecycle;


@ComponentConfig(
		lifecycle = Lifecycle.class,
		template = "app:/templates/AddOnSearchPortlet/UIAddOnDetail.gtmpl",
		events = { 
			@EventConfig(listeners = UIAddOnSearchOne.EditActionListener.class)
			
			}	
	)
public class UIAddOnDetail extends UIAddOnSearchOne {

	public UIAddOnDetail() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public List<String> getImageGallery() throws PathNotFoundException, RepositoryException, Exception{
		
		return AddOnService.getImagesNode(this.getNode());
		
	}	

	
	
	
	
}
