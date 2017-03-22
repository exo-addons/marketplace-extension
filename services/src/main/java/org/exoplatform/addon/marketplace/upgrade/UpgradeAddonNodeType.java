package org.exoplatform.addon.marketplace.upgrade;

import org.exoplatform.addon.marketplace.Constants;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.picocontainer.Startable;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kmenzli on 18/11/2016.
 */
public class UpgradeAddonNodeType implements Startable, Constants {

    private static final Log LOG = ExoLogger.getLogger(UpgradeAddonNodeType.class.getName());

    private String addonPath = "";

    private RepositoryService repositoryService;

    public UpgradeAddonNodeType(ThreadLocalSessionProviderService providerService, RepositoryService repoService,
                                DMSConfiguration dmsConfiguration, InitParams initParams) {

        this.repositoryService = repoService;

    }

    public void start() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Start " + this.getClass().getName() + ".............");
        }
        SessionProvider sessionProvider = null;
        Session session = null;
        QueryManager queryManager = null;
        StringBuilder statement = null;
        Query query = null;
        QueryResult queryResult = null;
        //--- Mixin Table
        List<Value> defaultMixinList = null;
        try {

            //--- GEt session
            sessionProvider = SessionProvider.createSystemProvider();

            //--- Fetch addon items path
            LivePortalManagerService livePortalManagerService = WCMCoreUtils.getService(LivePortalManagerService.class);
            //TODO : Get site node should be dynamic, think to a way to remove ad-hoc code to get to «intranet» site node
            Node siteNode = livePortalManagerService.getLivePortal(sessionProvider, ADDON_SITE_NAME);
            addonPath = siteNode.getPath() + "/" + ADDON_ITEM_PATH + "/";
            //--- Fin fetch addon items path


            //--- Add mixin to all Addon nodes
            //--- Get Session
            ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
            session = sessionProvider.getSession(manageableRepository.getConfiguration().getDefaultWorkspaceName(), repositoryService.getCurrentRepository());

            //--- Get QueryManager
            queryManager = session.getWorkspace().getQueryManager();
            //--- Create query to fetch addons
            statement = new StringBuilder("SELECT * FROM "+ADDON_NODE_TYPE);
            //--- First constraint : select only nodes having property exo:mpkaceAddonCategory null
            statement.append(" WHERE "+ADDON_MIXIN_PROPPERTY_NAME+" is null");
            //--- Add clause
            statement.append(" AND ");
            //--- Second constraint : jcr path
            statement.append("jcr:path LIKE '" + addonPath + "%'");
            //--- Set order
            statement.append(" ORDER BY exo:dateCreated DESC ");
            //--- Create JCR query
            query = queryManager.createQuery(statement.toString(), Query.SQL);
            //--- Launch query
            queryResult = query.execute();
            //--- Prepare Mixin Table
            defaultMixinList = new ArrayList<>();
            defaultMixinList.add(session.getValueFactory().createValue(ADDON_DEFAULT_MIXIN_VALUE));
            //--- Iterate then add the mixin
            for(NodeIterator iter = queryResult.getNodes(); iter.hasNext();) {

                Node addon = iter.nextNode();
                if(!addon.isNodeType(ADDON_MIXIN_CATEGORY)){
                    if(addon.canAddMixin(ADDON_MIXIN_CATEGORY)) {
                        List<Value> newValues = new ArrayList<>();
                        Value[] values;

                        addon.addMixin(ADDON_MIXIN_CATEGORY);
                        addon.setProperty(ADDON_MIXIN_PROPPERTY_NAME, defaultMixinList.toArray(new Value[defaultMixinList.size()]));
                        addon.save();
                    }
                }

                //--- Save
                session.save();

            }

        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("An unexpected error occurs when migrating scripts", e);
            }
        } finally {
            if (sessionProvider != null) {
                sessionProvider.close();
            }
            if (LOG.isInfoEnabled()) {
                LOG.info("Update of nodetype EXO:ADDON has been finished ");
            }
        }
    }

    public void stop() {
    }
}
