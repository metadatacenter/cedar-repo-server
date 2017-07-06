package org.metadatacenter.cedar.repo.resources;

import org.metadatacenter.bridge.FolderServerProxy;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarProcessingException;
import org.metadatacenter.model.folderserver.FolderServerResource;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.jsonld.LinkedDataUtil;
import org.metadatacenter.server.security.model.auth.NodePermission;
import org.metadatacenter.server.url.MicroserviceUrlUtil;

public abstract class AbstractRepoResource extends CedarMicroserviceResource {

  protected final LinkedDataUtil linkedDataUtil;
  protected final MicroserviceUrlUtil microserviceUrlUtil;

  public AbstractRepoResource(CedarConfig cedarConfig) {
    super(cedarConfig);
    linkedDataUtil = cedarConfig.getLinkedDataUtil();
    microserviceUrlUtil = cedarConfig.getMicroserviceUrlUtil();
  }

  protected static boolean userHasReadAccessToResource(MicroserviceUrlUtil microserviceUrlUtil, String nodeId,
                                                       CedarRequestContext context) throws CedarProcessingException {
    String url = microserviceUrlUtil.getWorkspace().getResources();
    FolderServerResource fsResource = FolderServerProxy.getResource(url, nodeId, context);
    if (fsResource == null) {
      throw new IllegalArgumentException("Resource not found:" + nodeId);
    }
    return fsResource.currentUserCan(NodePermission.READ);
  }

}
