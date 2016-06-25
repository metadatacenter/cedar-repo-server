package utils;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.TemplateElementRepoServerController;
import controllers.TemplateFieldRepoServerController;
import controllers.TemplateInstanceRepoServerController;
import controllers.TemplateRepoServerController;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.server.service.TemplateFieldService;
import org.metadatacenter.server.service.TemplateInstanceService;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.server.service.mongodb.TemplateElementServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateFieldServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateInstanceServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateServiceMongoDB;

public class DataServices {

  private static DataServices instance = new DataServices();
  public static TemplateElementService<String, JsonNode> templateElementService;
  public static TemplateService<String, JsonNode> templateService;
  public static TemplateFieldService<String, JsonNode> templateFieldService;
  public static TemplateInstanceService<String, JsonNode> templateInstanceService;
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

    TemplateElementRepoServerController.injectTemplateElementService(templateElementService);
    TemplateRepoServerController.injectTemplateService(templateService);
    TemplateInstanceRepoServerController.injectTemplateInstanceService(templateInstanceService);
    TemplateFieldRepoServerController.injectTemplateFieldService(templateFieldService);
  }

  public static TemplateElementService<String, JsonNode> getTemplateElementService() {
    return templateElementService;
  }
}
