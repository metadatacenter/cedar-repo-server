package org.metadatacenter.cedar.repo;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.bson.Document;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.cedar.repo.resources.TemplateElementsResource;
import org.metadatacenter.cedar.repo.resources.TemplateFieldsResource;
import org.metadatacenter.cedar.repo.resources.TemplateInstancesResource;
import org.metadatacenter.cedar.repo.resources.TemplatesResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.model.folderserver.basic.FolderServerTemplate;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.server.FolderServiceSession;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.test.EmbeddedCedarMongo;
import org.metadatacenter.util.test.EmbeddedCedarNeo4j;
import org.metadatacenter.util.test.RouteSurface;
import org.metadatacenter.util.test.TestAuthUtil;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Route safety net: probes every endpoint the four repo artifact resources declare,
 * unauthenticated, and requires each to answer 401. A 404/405 means the route vanished or changed
 * verb; any other status means an endpoint lost its authentication assertion. The same application
 * boot also exercises a permitted Mongo-backed read against its graph permission record.
 */
public class RepoRoutesRespondTest {

  static {
    // Must run before the test support boots the server, which reads the port env vars. Ports are
    // distinct from the dev server and from every other booting test class.
    EmbeddedCedarMongo.startAndRedirectEnvironment(Map.of(
        "CEDAR_REPO_HTTP_PORT", "19023",
        "CEDAR_REPO_ADMIN_PORT", "19123",
        "CEDAR_REPO_STOP_PORT", "19223"));
    EmbeddedCedarNeo4j.startAndRedirectEnvironment();
  }

  private static final DropwizardTestSupport<RepoServerConfiguration> SERVER =
      new DropwizardTestSupport<>(RepoServerApplication.class, ResourceHelpers.resourceFilePath("test-config.yml"));

  private static final HttpClient CLIENT = HttpClient.newHttpClient();
  private static String authorization;
  private static String templateId;

  @BeforeAll
  public static void startServer() throws Exception {
    SERVER.before();
    CedarConfig cedarConfig = CedarConfig.getInstance(
        CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_REPO));
    TestAuthUtil.installInMemoryUserService(cedarConfig);
    authorization = TestAuthUtil.getTestUser1AuthHeader(cedarConfig);
    EmbeddedCedarNeo4j.seed(cedarConfig);

    CedarRequestContext context = CedarRequestContextFactory.fromUser(TestAuthUtil.getTestUser1(cedarConfig));
    FolderServiceSession folderSession = CedarDataServices.getInstance().getFolderServiceSession(context);
    FolderServerTemplate graphTemplate = new FolderServerTemplate();
    templateId = cedarConfig.getLinkedDataUtil().buildNewLinkedDataId(CedarResourceType.TEMPLATE);
    graphTemplate.setId(templateId);
    graphTemplate.setName("Repo readable template");
    graphTemplate.setDescription("Repo success-path fixture");
    graphTemplate.setVersion("1.0.0");
    graphTemplate.setPublicationStatus("bibo:draft");
    graphTemplate.setLatestVersion(true);
    graphTemplate.setLatestDraftVersion(true);
    graphTemplate.setLatestPublishedVersion(false);
    Assertions.assertNotNull(folderSession.createResourceAsChildOfId(
        graphTemplate, folderSession.findHomeFolderOf().getResourceId()));

    com.mongodb.client.MongoClient mongoClient =
        CedarDataServices.getInstance().getMongoClientFactoryForDocuments().getClient();
    org.metadatacenter.config.MongoConfig mongoConfig = cedarConfig.getArtifactServerConfig();
    mongoClient.getDatabase(mongoConfig.getDatabaseName())
        .getCollection(mongoConfig.getMongoCollectionName(CedarResourceType.TEMPLATE))
        .insertOne(new Document("_id", "private-mongo-id")
            .append("@id", templateId)
            .append("schema:name", "Repo readable template"));
  }

  @AfterAll
  public static void stopServer() {
    SERVER.after();
  }

  @Test
  public void everyRouteRejectsAnUnauthenticatedRequest() {
    RouteSurface.assertEveryRouteAnswers(
        "http://localhost:" + SERVER.getLocalPort(),
        RouteSurface.endpoints(
            TemplatesResource.class,
            TemplateElementsResource.class,
            TemplateFieldsResource.class,
            TemplateInstancesResource.class),
        401);
  }

  @Test
  public void permittedReadReturnsArtifactWithoutMongoId() throws Exception {
    String encodedId = URLEncoder.encode(templateId.substring(templateId.lastIndexOf('/') + 1),
        StandardCharsets.UTF_8);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + "/templates/" + encodedId))
        .header("Authorization", authorization)
        .GET()
        .build();

    HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

    Assertions.assertEquals(200, response.statusCode(), response.body());
    JsonNode artifact = JsonMapper.MAPPER.readTree(response.body());
    Assertions.assertEquals(templateId, artifact.path("@id").asText());
    Assertions.assertEquals("Repo readable template", artifact.path("schema:name").asText());
    Assertions.assertTrue(artifact.path("_id").isMissingNode(), response.body());
  }

}
