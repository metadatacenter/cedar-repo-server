package org.metadatacenter.cedar.repo;

import com.mongodb.client.MongoClient;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.cedar.repo.health.RepoServerHealthCheck;
import org.metadatacenter.cedar.repo.resources.*;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplicationWithMongo;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.MongoConfig;
import org.metadatacenter.model.ServerName;

public class RepoServerApplication extends CedarMicroserviceApplicationWithMongo<RepoServerConfiguration> {

  public static void main(String[] args) throws Exception {
    new RepoServerApplication().run(args);
  }

  @Override
  protected ServerName getServerName() {
    return ServerName.REPO;
  }

  @Override
  protected void initializeWithBootstrap(Bootstrap<RepoServerConfiguration> bootstrap, CedarConfig cedarConfig) {
  }

  @Override
  public void initializeApp() {
    MongoConfig artifactServerConfig = cedarConfig.getArtifactServerConfig();
    CedarDataServices.initializeMongoClientFactoryForDocuments(artifactServerConfig.getMongoConnection());

    MongoClient mongoClientForDocuments = CedarDataServices.getMongoClientFactoryForDocuments().getClient();

    initMongoServices(mongoClientForDocuments, artifactServerConfig);
  }

  @Override
  public void runApp(RepoServerConfiguration configuration, Environment environment) {

    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    final TemplatesResource templates = new TemplatesResource(cedarConfig, templateService);
    environment.jersey().register(templates);

    final TemplateFieldsResource fields = new TemplateFieldsResource(cedarConfig, templateFieldService);
    environment.jersey().register(fields);

    final TemplateElementsResource elements = new TemplateElementsResource(cedarConfig, templateElementService);
    environment.jersey().register(elements);

    final TemplateInstancesResource instances = new TemplateInstancesResource(cedarConfig, templateInstanceService);
    environment.jersey().register(instances);

    final RepoServerHealthCheck healthCheck = new RepoServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);
  }
}
