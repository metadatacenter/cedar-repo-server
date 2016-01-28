package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.constant.ConfigConstants;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.util.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Result;

public class TemplateRepoServerController extends AbstractRepoServerController {
  private static Logger log = LoggerFactory.getLogger(TemplateRepoServerController.class);

  private static TemplateService<String, JsonNode> templateService;

  public static void injectTemplateService(TemplateService<String, JsonNode> ts) {
    templateService = ts;
  }

  public static Result findTemplate(String id) {
    try {
      String templateId = config.getString(ConfigConstants.LINKED_DATA_ID_PATH_BASE) + config.getString(ConfigConstants
          .LINKED_DATA_ID_PATH_SUFFIX_TEMPLATES) + id;
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
  }

}
