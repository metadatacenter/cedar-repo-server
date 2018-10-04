package org.metadatacenter.cedar.repo.resources;

import org.metadatacenter.bridge.FolderServerProxy;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarProcessingException;
import org.metadatacenter.model.folderserver.currentuserpermissions.FolderServerResourceCurrentUserReport;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.url.MicroserviceUrlUtil;

public abstract class AbstractRepoResource extends CedarMicroserviceResource {

  public AbstractRepoResource(CedarConfig cedarConfig) {
    super(cedarConfig);
  }

  protected static boolean userHasNoReadAccessToResource(MicroserviceUrlUtil microserviceUrlUtil, String nodeId,
                                                         CedarRequestContext context) throws CedarProcessingException {
    String url = microserviceUrlUtil.getWorkspace().getResources();
    FolderServerResourceCurrentUserReport
        resourceCurrentUserReport = FolderServerProxy.getResourceCurrentUserReport(url, nodeId, context);
    if (resourceCurrentUserReport == null) {
      throw new IllegalArgumentException("Resource not found:" + nodeId);
    }
    return !resourceCurrentUserReport.getCurrentUserPermissions().isCanRead();
  }

}
