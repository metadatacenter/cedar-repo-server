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
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.util.json.JsonUtils;
import play.mvc.Result;

@Api(value = "/templates", description = "Template operations")
public class TemplateController extends AbstractRepoServerController {

  private static TemplateService<String, JsonNode> templateService;

  public static void injectTemplateService(TemplateService<String, JsonNode> ts) {
    templateService = ts;
  }

  @ApiOperation(
      value = "Find template by id",
      httpMethod = "GET")
  public static Result findTemplate(String id) {
    String templateId = cedarConfig.getLinkedDataPrefix(CedarNodeType.TEMPLATE) + id;
    boolean canProceed = false;
    try {
      IAuthRequest frontendRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(frontendRequest, CedarPermission.TEMPLATE_READ);
      if (userHasReadAccessToResource(folderBase, templateId)) {
        canProceed = true;
      }
    } catch (CedarAccessException e) {
      play.Logger.error("Access Error while reading the template", e);
      return forbiddenWithError(e);
    }
    if (canProceed) {
      try {
        JsonNode template = templateService.findTemplate(templateId);
        if (template != null) {
          // Remove autogenerated _id field to avoid exposing it
          template = JsonUtils.removeField(template, "_id");
          return ok(template);
        }
        return notFound();
      } catch (IllegalArgumentException e) {
        return badRequestWithError(e);
      } catch (Exception e) {
        return internalServerErrorWithError(e);
      }
    } else {
      return forbidden("You do not have read access for this template");
    }
  }
}