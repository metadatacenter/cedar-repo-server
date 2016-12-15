package org.metadatacenter.cedar.repo.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.util.json.JsonUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;
import static org.metadatacenter.server.security.model.auth.CedarPermission.TEMPLATE_READ;

@Api(value = "/templates", description = "Template operations")
@Path("/templates")
@Produces(MediaType.APPLICATION_JSON)
public class TemplatesResource extends AbstractRepoResource {

  private static TemplateService<String, JsonNode> templateService;

  public TemplatesResource() {
  }

  public static void injectTemplateService(TemplateService<String, JsonNode> ts) {
    templateService = ts;
  }

  @ApiOperation(
      value = "Find template by id",
      httpMethod = "GET")
  @GET
  @Timed
  @Path("/{id}")
  public Response findTemplate(@PathParam("id") String id) throws CedarException {

    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(TEMPLATE_READ);

    String templateId = cedarConfig.getLinkedDataPrefix(CedarNodeType.TEMPLATE) + id;

    if (!userHasReadAccessToResource(folderBase, templateId, request)) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    try {
      JsonNode template = templateService.findTemplate(templateId);
      if (template != null) {
        // Remove autogenerated _id field to avoid exposing it
        template = JsonUtils.removeField(template, "_id");
        return Response.ok().entity(template).build();
      }
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e).build();
    } catch (Exception e) {
      return Response.serverError().entity(e).build();
    }
  }

}