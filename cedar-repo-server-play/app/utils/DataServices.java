package utils;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.TemplateElementController;
import controllers.TemplateFieldController;
import controllers.TemplateInstanceController;
import controllers.TemplateController;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.server.service.*;
import org.metadatacenter.server.service.mongodb.*;

public class DataServices {

  private static DataServices instance = new DataServices();
  public static TemplateElementService<String, JsonNode> templateElementService;
  public static TemplateService<String, JsonNode> templateService;
  public static TemplateFieldService<String, JsonNode> templateFieldService;
  public static TemplateInstanceService<String, JsonNode> templateInstanceService;
  private static UserService userService;
  private static CedarConfig cedarConfig;

  public static DataServices getInstance() {
    return instance;
  }

  private DataServices() {
    cedarConfig = CedarConfig.getInstance();

    templateElementService = new TemplateElementServiceMongoDB(
        cedarConfig.getMongoConfig().getDatabaseName(),
        cedarConfig.getMongoCollectionName(CedarNodeType.ELEMENT));

    templateService = new TemplateServiceMongoDB(
        cedarConfig.getMongoConfig().getDatabaseName(),
        cedarConfig.getMongoCollectionName(CedarNodeType.TEMPLATE),
        templateElementService);

    templateInstanceService = new TemplateInstanceServiceMongoDB(
        cedarConfig.getMongoConfig().getDatabaseName(),
        cedarConfig.getMongoCollectionName(CedarNodeType.INSTANCE));

    templateFieldService = new TemplateFieldServiceMongoDB(
        cedarConfig.getMongoConfig().getDatabaseName(),
        cedarConfig.getMongoCollectionName(CedarNodeType.FIELD));

    userService = new UserServiceMongoDB(cedarConfig.getMongoConfig().getDatabaseName(),
        cedarConfig.getMongoCollectionName(CedarNodeType.USER));

    TemplateElementController.injectTemplateElementService(templateElementService);
    TemplateController.injectTemplateService(templateService);
    TemplateInstanceController.injectTemplateInstanceService(templateInstanceService);
    TemplateFieldController.injectTemplateFieldService(templateFieldService);
  }

  public static TemplateElementService<String, JsonNode> getTemplateElementService() {
    return templateElementService;
  }

  public UserService getUserService() {
    return userService;
  }
}
