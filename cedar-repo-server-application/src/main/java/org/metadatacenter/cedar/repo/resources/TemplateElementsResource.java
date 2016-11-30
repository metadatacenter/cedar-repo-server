package org.metadatacenter.cedar.repo.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.util.json.JsonUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;
import static org.metadatacenter.server.security.model.auth.CedarPermission.TEMPLATE_ELEMENT_READ;

@Api(value = "/template-elements", description = "Template element operations")
@Path("/template-elements")
@Produces(MediaType.APPLICATION_JSON)
public class TemplateElementsResource extends AbstractRepoResource {

  private static TemplateElementService<String, JsonNode> templateElementService;

  public TemplateElementsResource() {
  }

  public static void injectTemplateElementService(TemplateElementService<String, JsonNode> tes) {
    templateElementService = tes;
  }

  @ApiOperation(
      value = "Find template element by id",
      httpMethod = "GET")
  @GET
  @Timed
  @Path("/{id}")
  public Response findTemplateElement(@PathParam("id") String id) throws
      CedarAssertionException {

    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(TEMPLATE_ELEMENT_READ);

    String templateElementId = cedarConfig.getLinkedDataPrefix(CedarNodeType.ELEMENT) + id;

    try {
      if (!userHasReadAccessToResource(folderBase, templateElementId, request)) {
        return Response.status(Response.Status.UNAUTHORIZED).build();
      }
    } catch (CedarAccessException e) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    try {
      JsonNode templateElement = templateElementService.findTemplateElement(templateElementId);
      if (templateElement != null) {
        // Remove autogenerated _id field to avoid exposing it
        templateElement = JsonUtils.removeField(templateElement, "_id");
        return Response.ok().entity(templateElement).build();
      }
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e).build();
    } catch (Exception e) {
      return Response.serverError().entity(e).build();
    }
  }

}