package org.metadatacenter.cedar.repo.resources;

import org.metadatacenter.cedar.resource.util.FolderServerProxy;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.folderserver.FolderServerResource;
import org.metadatacenter.server.security.exception.CedarAccessException;
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

  protected static final CedarConfig cedarConfig;
  protected static final String folderBase;
  protected static final String PREFIX_RESOURCES = "resources";

  static {
    cedarConfig = CedarConfig.getInstance();
    folderBase = cedarConfig.getServers().getFolder().getBase();
  }

  protected static boolean userHasReadAccessToResource(String folderBase, String
      nodeId, HttpServletRequest request) throws CedarAccessException {
    String url = folderBase + PREFIX_RESOURCES;
    FolderServerResource fsResource = FolderServerProxy.getResource(url, nodeId, request);
    if (fsResource == null) {
      throw new IllegalArgumentException("Resource not found:" + nodeId);
    }
    return fsResource.currentUserCan(NodePermission.READ);
  }

}
