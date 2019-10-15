package org.metadatacenter.cedar.repo.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.id.CedarElementId;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.service.TemplateElementService;
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
import static org.metadatacenter.server.security.model.auth.CedarPermission.TEMPLATE_ELEMENT_READ;

@Path("/template-elements")
@Produces(MediaType.APPLICATION_JSON)
public class TemplateElementsResource extends AbstractRepoResource {

  private static TemplateElementService<String, JsonNode> templateElementService;

  public TemplateElementsResource(CedarConfig cedarConfig, TemplateElementService<String, JsonNode> templateElementService) {
    super(cedarConfig);
    TemplateElementsResource.templateElementService = templateElementService;
  }

  @GET
  @Timed
  @Path("/{id}")
  public Response findTemplateElement(@PathParam(PP_ID) String id) throws CedarException {

    CedarRequestContext c = buildRequestContext();

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(TEMPLATE_ELEMENT_READ);

    String templateElementId = linkedDataUtil.getLinkedDataId(CedarResourceType.ELEMENT, id);
    CedarElementId eid = CedarElementId.buildSafe(templateElementId);

    if (userHasNoReadAccessToArtifact(c, eid)) {
      return CedarResponse.unauthorized().build();
    }

    try {
      JsonNode templateElement = templateElementService.findTemplateElement(templateElementId);
      if (templateElement != null) {
        // Remove autogenerated _id field to avoid exposing it
        templateElement = JsonUtils.removeField(templateElement, "_id");
        return Response.ok().entity(templateElement).build();
      }
      return CedarResponse.notFound().id(id).build();
    } catch (Exception e) {
      return CedarResponse.internalServerError().exception(e).build();
    }
  }

}
