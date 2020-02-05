package org.metadatacenter.cedar.repo.resources;

import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.bridge.GraphDbPermissionReader;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.id.CedarArtifactId;
import org.metadatacenter.model.folderserver.currentuserpermissions.FolderServerArtifactCurrentUserReport;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.FolderServiceSession;
import org.metadatacenter.server.ResourcePermissionServiceSession;

public abstract class AbstractRepoResource extends CedarMicroserviceResource {

  public AbstractRepoResource(CedarConfig cedarConfig) {
    super(cedarConfig);
  }

  protected boolean userHasNoReadAccessToArtifact(CedarRequestContext context, CedarArtifactId artifactId) throws CedarException {
    FolderServiceSession folderSession = CedarDataServices.getFolderServiceSession(context);
    ResourcePermissionServiceSession permissionSession = CedarDataServices.getResourcePermissionServiceSession(context);
    FolderServerArtifactCurrentUserReport resourceCurrentUserReport = GraphDbPermissionReader.getArtifactCurrentUserReport(context, folderSession,
        permissionSession, cedarConfig, artifactId);
    if (resourceCurrentUserReport == null) {
      throw new IllegalArgumentException("Artifact not found:" + artifactId);
    }
    return !resourceCurrentUserReport.getCurrentUserPermissions().isCanRead();
  }

}
