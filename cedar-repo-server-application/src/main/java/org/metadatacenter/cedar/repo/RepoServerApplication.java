package org.metadatacenter.cedar.repo;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.cedar.repo.health.RepoServerHealthCheck;
import org.metadatacenter.cedar.repo.resources.IndexResource;
import org.metadatacenter.cedar.repo.resources.TemplateElementsResource;
import org.metadatacenter.cedar.repo.resources.TemplateInstancesResource;
import org.metadatacenter.cedar.repo.resources.TemplatesResource;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.server.service.TemplateInstanceService;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.server.service.mongodb.TemplateElementServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateInstanceServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateServiceMongoDB;

public class RepoServerApplication extends CedarMicroserviceApplication<RepoServerConfiguration> {

  protected static TemplateElementService<String, JsonNode> templateElementService;
  protected static TemplateService<String, JsonNode> templateService;
  protected static TemplateInstanceService<String, JsonNode> templateInstanceService;

  public static void main(String[] args) throws Exception {
    new RepoServerApplication().run(args);
  }

  @Override
  public String getName() {
    return "cedar-repo-server";
  }

  @Override
  public void initializeApp(Bootstrap<RepoServerConfiguration> bootstrap) {
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

    TemplatesResource.injectTemplateService(templateService);
    TemplateElementsResource.injectTemplateElementService(templateElementService);
    TemplateInstancesResource.injectTemplateInstanceService(templateInstanceService);
  }

  @Override
  public void runApp(RepoServerConfiguration configuration, Environment environment) {

    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    final TemplatesResource templates = new TemplatesResource(cedarConfig);
    environment.jersey().register(templates);

    final TemplateElementsResource elements = new TemplateElementsResource(cedarConfig);
    environment.jersey().register(elements);

    final TemplateInstancesResource instances = new TemplateInstancesResource(cedarConfig);
    environment.jersey().register(instances);

    final RepoServerHealthCheck healthCheck = new RepoServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);
  }
}
