package org.metadatacenter.cedar.repo.resources;

import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.bridge.GraphDbPermissionReader;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.folderserver.currentuserpermissions.FolderServerArtifactCurrentUserReport;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.FolderServiceSession;
import org.metadatacenter.server.PermissionServiceSession;

public abstract class AbstractRepoResource extends CedarMicroserviceResource {

  public AbstractRepoResource(CedarConfig cedarConfig) {
    super(cedarConfig);
  }

  protected boolean userHasNoReadAccessToResource(CedarRequestContext context, String nodeId) throws CedarException {
    FolderServiceSession folderSession = CedarDataServices.getFolderServiceSession(context);
    PermissionServiceSession permissionSession = CedarDataServices.getPermissionServiceSession(context);
    FolderServerArtifactCurrentUserReport
        resourceCurrentUserReport = GraphDbPermissionReader
        .getResourceCurrentUserReport(context, folderSession, permissionSession, cedarConfig, nodeId);
    if (resourceCurrentUserReport == null) {
      throw new IllegalArgumentException("Resource not found:" + nodeId);
    }
    return !resourceCurrentUserReport.getCurrentUserPermissions().isCanRead();
  }

}
