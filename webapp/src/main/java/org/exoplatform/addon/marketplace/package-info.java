/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
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

@Application(defaultController = CategoryManagement.class)
@Portlet
@Bindings({
    @Binding(value = MarketPlaceService.class)
})
@WebJars({
    @WebJar("x-editable-bootstrap"),
    @WebJar("selectize.js"),
    @WebJar("select2"),
    @WebJar("jquery-ui")
})
@Scripts({

    @Script(id = "marketplace-category-common-js", value = "javascript/marketplace-category-common-js.js"),
        @Script(id = "marketplace-category-add-js", value = "javascript/marketplace-category-add-js.js",
                depends = {"marketplace-category-common-js"}),
    @Script(id = "marketplace-category-js", value = "javascript/marketplace-category.js",
            depends = {"marketplace-category-common-js", "marketplace-category-add-js"})
})
@Less({
    @Stylesheet(id = "marketplace-category", value = "less/marketplace-category.less")
})
@Stylesheets({
    @Stylesheet(id = "marketplace-category.css", value = "less/marketplace-category.css"),
    @Stylesheet(id = "marketplace-common", value = "css/marketplace-common.css")
})
@Assets({"*"})
package org.exoplatform.addon.marketplace;

import org.exoplatform.addon.marketplace.service.MarketPlaceService;
import org.exoplatform.addon.marketplace.controller.CategoryManagement;
import juzu.Application;
import juzu.plugin.asset.Assets;
import juzu.plugin.asset.Script;
import juzu.plugin.asset.Scripts;
import juzu.plugin.asset.Stylesheet;
import juzu.plugin.asset.Stylesheets;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.less4j.Less;
import juzu.plugin.portlet.Portlet;
import juzu.plugin.webjars.WebJar;
import juzu.plugin.webjars.WebJars;

