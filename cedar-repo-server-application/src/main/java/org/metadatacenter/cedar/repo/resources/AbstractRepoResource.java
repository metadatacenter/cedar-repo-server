package org.metadatacenter.cedar.repo.resources;

import org.metadatacenter.bridge.FolderServerProxy;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarProcessingException;
import org.metadatacenter.model.folderserver.FolderServerResource;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.jsonld.LinkedDataUtil;
import org.metadatacenter.server.security.model.auth.NodePermission;

public abstract class AbstractRepoResource extends CedarMicroserviceResource {

  protected final LinkedDataUtil linkedDataUtil;
  protected final String folderBase;
  protected static final String PREFIX_RESOURCES = "resources";

  public AbstractRepoResource(CedarConfig cedarConfig) {
    super(cedarConfig);
    this.linkedDataUtil = cedarConfig.getLinkedDataUtil();
    this.folderBase = cedarConfig.getServers().getFolder().getBase();
  }

  protected static boolean userHasReadAccessToResource(String folderBase, String nodeId, CedarRequestContext context)
      throws CedarProcessingException {
    String url = folderBase + PREFIX_RESOURCES;
    FolderServerResource fsResource = FolderServerProxy.getResource(url, nodeId, context);
    if (fsResource == null) {
      throw new IllegalArgumentException("Resource not found:" + nodeId);
    }
    return fsResource.currentUserCan(NodePermission.READ);
  }

}
