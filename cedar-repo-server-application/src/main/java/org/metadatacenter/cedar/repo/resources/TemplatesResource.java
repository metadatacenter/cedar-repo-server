package org.metadatacenter.cedar.repo.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.json.JsonUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.metadatacenter.constant.CedarPathParameters.PP_ID;
import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;
import static org.metadatacenter.server.security.model.auth.CedarPermission.TEMPLATE_READ;

@Path("/templates")
@Produces(MediaType.APPLICATION_JSON)
public class TemplatesResource extends AbstractRepoResource {

  private static TemplateService<String, JsonNode> templateService;

  public TemplatesResource(CedarConfig cedarConfig, TemplateService<String, JsonNode> templateService) {
    super(cedarConfig);
    TemplatesResource.templateService = templateService;
  }

  @GET
  @Timed
  @Path("/{id}")
  public Response findTemplate(@PathParam(PP_ID) String id) throws CedarException {

    CedarRequestContext c = buildRequestContext();

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(TEMPLATE_READ);

    String templateId = linkedDataUtil.getLinkedDataId(CedarNodeType.TEMPLATE, id);

    if (userHasNoReadAccessToResource(microserviceUrlUtil, templateId, c)) {
      return CedarResponse.unauthorized().build();
    }

    try {
      JsonNode template = templateService.findTemplate(templateId);
      if (template != null) {
        // Remove autogenerated _id field to avoid exposing it
        template = JsonUtils.removeField(template, "_id");
        return Response.ok().entity(template).build();
      }
      return CedarResponse.notFound().build();
    } catch (Exception e) {
      return CedarResponse.internalServerError().exception(e).build();
    }
  }

}