package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarAuthFromRequestFactory;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.TemplateInstanceService;
import org.metadatacenter.util.json.JsonUtils;
import play.mvc.Result;

@Api(value = "/template-instances", description = "Template instance operations")
public class TemplateInstanceController extends AbstractRepoServerController {

  private static TemplateInstanceService<String, JsonNode> templateInstanceService;

  public static void injectTemplateInstanceService(TemplateInstanceService<String, JsonNode> tis) {
    templateInstanceService = tis;
  }

  @ApiOperation(
      value = "Find template instance by id",
      httpMethod = "GET")
  public static Result findTemplateInstance(String id) {
    String templateInstanceId = cedarConfig.getLinkedDataPrefix(CedarNodeType.INSTANCE) + id;
    boolean canProceed = false;
    try {
      IAuthRequest frontendRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(frontendRequest, CedarPermission.TEMPLATE_INSTANCE_READ);
      if (userHasReadAccessToResource(folderBase, templateInstanceId)) {
        canProceed = true;
      }
    } catch (CedarAccessException e) {
      play.Logger.error("Access Error while reading the template instance", e);
      return forbiddenWithError(e);
    }
    if (canProceed) {
      try {
        JsonNode templateInstance = templateInstanceService.findTemplateInstance(templateInstanceId);
        if (templateInstance != null) {
          // Remove autogenerated _id field to avoid exposing it
          templateInstance = JsonUtils.removeField(templateInstance, "_id");
          return ok(templateInstance);
        }
        return notFound();
      } catch (IllegalArgumentException e) {
        return badRequestWithError(e);
      } catch (Exception e) {
        return internalServerErrorWithError(e);
      }
    } else {
      return forbidden("You do not have read access for this instance");
    }
  }
}