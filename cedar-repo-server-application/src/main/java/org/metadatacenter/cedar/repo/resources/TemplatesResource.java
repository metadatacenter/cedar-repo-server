package org.metadatacenter.cedar.repo.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.cedar.resource.util.FolderServerProxy;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.model.folderserver.FolderServerResource;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.auth.NodePermission;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.util.json.JsonUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;
import static org.metadatacenter.server.security.model.auth.CedarPermission.TEMPLATE_READ;

@Path("/templates")
@Produces(MediaType.APPLICATION_JSON)
public class TemplatesResource {

  private
  @Context
  UriInfo uriInfo;

  @Context
  HttpServletRequest request;

  private static final CedarConfig cedarConfig;
  private static final String folderBase;
  private static final String PREFIX_RESOURCES = "resources";
  private static TemplateService<String, JsonNode> templateService;

  static {
    cedarConfig = CedarConfig.getInstance();
    folderBase = cedarConfig.getServers().getFolder().getBase();
  }

  public TemplatesResource() {
  }

  public static void injectTemplateService(TemplateService<String, JsonNode> tes) {
    templateService = tes;
  }

  @GET
  @Timed
  @Path("/{id}")
  public static Response findTemplate(@PathParam("id") String id, @Context HttpServletRequest request) throws
      CedarAssertionException {

    System.out.println("IN TEMPLATES RESOURCE" + id);
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(TEMPLATE_READ);

    String templateId = cedarConfig.getLinkedDataPrefix(CedarNodeType.TEMPLATE) + id;

    try {
      if (!userHasReadAccessToResource(folderBase, templateId, request)) {
        System.out.println("UNAUTH 1");
        return Response.status(Response.Status.UNAUTHORIZED).build();
      }
    } catch (CedarAccessException e) {
      System.out.println("UNAUTH 2");
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }


    try {
      JsonNode template = templateService.findTemplate(templateId);
      if (template != null) {
        // Remove autogenerated _id field to avoid exposing it
        template = JsonUtils.removeField(template, "_id");
        return Response.ok().entity(template).build();
      }
      System.out.println("NOT FOUND");
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (IllegalArgumentException e) {
      System.out.println("BAD REQ");
      return Response.status(Response.Status.BAD_REQUEST).entity(e).build();
    } catch (Exception e) {
      System.out.println("INTERNAL");
      return Response.serverError().entity(e).build();
    }
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