package org.metadatacenter.cedar.repo;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.cedar.repo.health.RepoServerHealthCheck;
import org.metadatacenter.cedar.repo.resources.IndexResource;
import org.metadatacenter.cedar.repo.resources.TemplateElementsResource;
import org.metadatacenter.cedar.repo.resources.TemplateInstancesResource;
import org.metadatacenter.cedar.repo.resources.TemplatesResource;
import org.metadatacenter.cedar.util.dw.CedarDropwizardApplicationUtil;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.AuthorizationKeycloakAndApiKeyResolver;
import org.metadatacenter.server.security.IAuthorizationResolver;
import org.metadatacenter.server.security.KeycloakDeploymentProvider;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.server.service.TemplateInstanceService;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.server.service.mongodb.TemplateElementServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateInstanceServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateServiceMongoDB;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;

import static org.eclipse.jetty.servlets.CrossOriginFilter.*;

public class RepoServerApplication extends Application<RepoServerConfiguration> {

  protected static CedarConfig cedarConfig;
  protected static TemplateElementService<String, JsonNode> templateElementService;
  protected static TemplateService<String, JsonNode> templateService;
  protected static TemplateInstanceService<String, JsonNode> templateInstanceService;

  public static void main(String[] args) throws Exception {
    new RepoServerApplication().run(args);
  }

  @Override
  public String getName() {
    return "repo-server";
  }

  @Override
  public void initialize(Bootstrap<RepoServerConfiguration> bootstrap) {

    CedarDropwizardApplicationUtil.setupKeycloak();

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

    TemplatesResource.injectTemplateService(templateService);
    TemplateElementsResource.injectTemplateElementService(templateElementService);
    TemplateInstancesResource.injectTemplateInstanceService(templateInstanceService);
  }

  @Override
  public void run(RepoServerConfiguration configuration, Environment environment) {

    //environment.jersey().register(new ApiListingResource());

    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    final TemplatesResource templates = new TemplatesResource();
    environment.jersey().register(templates);

    final TemplateElementsResource elements = new TemplateElementsResource();
    environment.jersey().register(elements);

    final TemplateInstancesResource instances = new TemplateInstancesResource();
    environment.jersey().register(instances);

    final RepoServerHealthCheck healthCheck = new RepoServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);

    CedarDropwizardApplicationUtil.setupEnvironment(environment);

    /*BeanConfig config = new BeanConfig();
    config.setTitle("CEDAR Repo Server");
    config.setVersion("0.8.7");
    config.setResourcePackage("org.metadatacenter.cedar.repo");
    config.setScan(true);*/

  }
}
