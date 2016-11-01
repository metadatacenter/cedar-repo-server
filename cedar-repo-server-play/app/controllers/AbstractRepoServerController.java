package controllers;

import org.metadatacenter.cedar.resource.util.FolderServerProxy;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.folderserver.FolderServerResource;
import org.metadatacenter.server.play.AbstractCedarController;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.auth.NodePermission;

public abstract class AbstractRepoServerController extends AbstractCedarController {

  protected static final String PREFIX_RESOURCES = "resources";

  protected static final CedarConfig cedarConfig;
  protected static final String folderBase;

  static {
    cedarConfig = CedarConfig.getInstance();
    folderBase = cedarConfig.getServers().getFolder().getBase();
  }

  protected static boolean userHasReadAccessToResource(String folderBase, String
      nodeId) throws CedarAccessException {
    String url = folderBase + PREFIX_RESOURCES;
    FolderServerResource fsResource = FolderServerProxy.getResource(url, nodeId, request());
    if (fsResource == null) {
      throw new IllegalArgumentException("Resource not found:" + nodeId);
    }
    return fsResource.currentUserCan(NodePermission.READ);
  }
}
