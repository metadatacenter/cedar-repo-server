package org.metadatacenter.cedar.repo.resources;

import org.metadatacenter.bridge.FolderServerProxy;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.folderserver.currentuserpermissions.FolderServerResourceCurrentUserReport;
import org.metadatacenter.rest.context.CedarRequestContext;

public abstract class AbstractRepoResource extends CedarMicroserviceResource {

  public AbstractRepoResource(CedarConfig cedarConfig) {
    super(cedarConfig);
  }

  protected boolean userHasNoReadAccessToResource(CedarRequestContext context, String nodeId) throws CedarException {
    FolderServerResourceCurrentUserReport
        resourceCurrentUserReport = FolderServerProxy.getResourceCurrentUserReport(context, cedarConfig, nodeId);
    if (resourceCurrentUserReport == null) {
      throw new IllegalArgumentException("Resource not found:" + nodeId);
    }
    return !resourceCurrentUserReport.getCurrentUserPermissions().isCanRead();
  }

}
