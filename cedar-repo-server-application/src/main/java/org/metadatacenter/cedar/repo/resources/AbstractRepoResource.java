package org.metadatacenter.cedar.repo.resources;

import org.metadatacenter.bridge.FolderServerProxy;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.folderserver.FolderServerResource;
import org.metadatacenter.exception.CedarProcessingException;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.security.model.auth.NodePermission;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

public abstract class AbstractRepoResource {

  protected
  @Context
  UriInfo uriInfo;

  protected
  @Context
  HttpServletRequest request;

  protected final CedarConfig cedarConfig;
  protected final String folderBase;
  protected static final String PREFIX_RESOURCES = "resources";

  public AbstractRepoResource(CedarConfig cedarConfig) {
    this.cedarConfig = cedarConfig;
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
