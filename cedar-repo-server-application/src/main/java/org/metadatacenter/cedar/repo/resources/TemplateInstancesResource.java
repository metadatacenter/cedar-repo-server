package org.metadatacenter.cedar.repo.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.id.CedarTemplateId;
import org.metadatacenter.id.CedarTemplateInstanceId;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.service.TemplateInstanceService;
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
import static org.metadatacenter.server.security.model.auth.CedarPermission.TEMPLATE_INSTANCE_READ;

@Path("/template-instances")
@Produces(MediaType.APPLICATION_JSON)
public class TemplateInstancesResource extends AbstractRepoResource {

  private static TemplateInstanceService<String, JsonNode> templateInstanceService;

  public TemplateInstancesResource(CedarConfig cedarConfig, TemplateInstanceService<String, JsonNode> templateInstanceService) {
    super(cedarConfig);
    TemplateInstancesResource.templateInstanceService = templateInstanceService;
  }

  @GET
  @Timed
  @Path("/{id}")
  public Response findTemplateElement(@PathParam(PP_ID) String id) throws CedarException {

    CedarRequestContext c = buildRequestContext();

    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(TEMPLATE_INSTANCE_READ);

    String templateInstanceId = linkedDataUtil.getLinkedDataId(CedarResourceType.INSTANCE, id);
    CedarTemplateInstanceId tid = CedarTemplateInstanceId.build(templateInstanceId);

    if (userHasNoReadAccessToArtifact(c, tid)) {
      return CedarResponse.unauthorized().build();
    }

    try {
      JsonNode templateInstance = templateInstanceService.findTemplateInstance(templateInstanceId);
      if (templateInstance != null) {
        // Remove autogenerated _id field to avoid exposing it
        templateInstance = JsonUtils.removeField(templateInstance, "_id");
        return Response.ok().entity(templateInstance).build();
      }
      return CedarResponse.notFound().build();
    } catch (Exception e) {
      return CedarResponse.internalServerError().exception(e).build();
    }
  }

}
