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
package org.exoplatform.community.portlet.addon;

import org.exoplatform.addon.marketplace.Constants;
import org.exoplatform.addon.service.AddOnService;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

import javax.portlet.PortletPreferences;


@ComponentConfig(lifecycle = UIApplicationLifecycle.class, template = "app:/templates/AddOnPortlet/UIAddOnPortlet.gtmpl")
public class UIAddOnPortlet extends UIPortletApplication implements Constants {

	public UIAddOnPortlet() throws Exception {
			
	  AddOnService.createCommunityFolder();
		addChild(UIAddOnForm.class,null,null);
		
	}

	public String getPreferenceReceiver() {
		PortletPreferences preferences = Utils.getAllPortletPreferences();
		return preferences.getValue(PREFERENCE_RECEIVER, null);
	}
	
	public String getFolderPath() {
		PortletPreferences preferences = Utils.getAllPortletPreferences();
		return preferences.getValue(PREFERENCE_ITEM_PATH, null);
	   
	  }

	public String getPreferenceSubject() {
		PortletPreferences preferences = Utils.getAllPortletPreferences();
		return preferences.getValue(PREFERENCE_EMAIL_SUBJECT, null);
	}


}

