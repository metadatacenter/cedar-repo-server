package org.metadatacenter.cedar.repo;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metadatacenter.bridge.CedarDataServices;
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
import org.metadatacenter.util.test.TestAuthUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Proves that a permitted Repo read reports a Mongo outage without leaking driver details. */
class RepoMongoOutageTest {

  static {
    EmbeddedCedarNeo4j.startAndRedirectEnvironment(Map.of(
        "CEDAR_REPO_HTTP_PORT", "19025",
        "CEDAR_REPO_ADMIN_PORT", "19125",
        "CEDAR_REPO_STOP_PORT", "19225",
        "CEDAR_MONGO_HOST", "127.0.0.1",
        "CEDAR_MONGO_PORT", "1",
        "CEDAR_REDIS_PERSISTENT_PORT", "1"));
  }

  private static final DropwizardTestSupport<RepoServerConfiguration> SERVER =
      new DropwizardTestSupport<>(RepoServerApplication.class,
          ResourceHelpers.resourceFilePath("test-config.yml"));

  private static final HttpClient CLIENT = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();

  private static String authorization;
  private static String templateUuid;

  @BeforeAll
  static void startServer() throws Exception {
    SERVER.before();
    CedarConfig cedarConfig = CedarConfig.getInstance(
        CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_REPO));
    TestAuthUtil.installInMemoryUserService(cedarConfig);
    authorization = TestAuthUtil.getTestUser1AuthHeader(cedarConfig);
    EmbeddedCedarNeo4j.seed(cedarConfig);

    CedarRequestContext context = CedarRequestContextFactory.fromUser(TestAuthUtil.getTestUser1(cedarConfig));
    FolderServiceSession folderSession = CedarDataServices.getInstance().getFolderServiceSession(context);
    FolderServerTemplate graphTemplate = new FolderServerTemplate();
    String templateId = cedarConfig.getLinkedDataUtil().buildNewLinkedDataId(CedarResourceType.TEMPLATE);
    templateUuid = templateId.substring(templateId.lastIndexOf('/') + 1);
    graphTemplate.setId(templateId);
    graphTemplate.setName("Repo Mongo outage fixture");
    graphTemplate.setDescription("Permitted graph record without an available artifact store");
    graphTemplate.setVersion("1.0.0");
    graphTemplate.setPublicationStatus("bibo:draft");
    graphTemplate.setLatestVersion(true);
    graphTemplate.setLatestDraftVersion(true);
    graphTemplate.setLatestPublishedVersion(false);
    Assertions.assertNotNull(folderSession.createResourceAsChildOfId(
        graphTemplate, folderSession.findHomeFolderOf().getResourceId()));
  }

  @AfterAll
  static void stopServer() {
    SERVER.after();
    EmbeddedCedarMongo.startAndRedirectEnvironment(Map.of(
        "CEDAR_REPO_HTTP_PORT", "19025",
        "CEDAR_REPO_ADMIN_PORT", "19125",
        "CEDAR_REPO_STOP_PORT", "19225",
        "CEDAR_REDIS_PERSISTENT_PORT", "1"));
  }

  @Test
  void permittedReadReturnsSanitizedServiceUnavailable() throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + "/templates/" + templateUuid))
        .timeout(Duration.ofSeconds(5))
        .header("Authorization", authorization)
        .GET()
        .build();

    HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

    Assertions.assertEquals(503, response.statusCode(), response.body());
    JsonNode error = JsonMapper.MAPPER.readTree(response.body());
    Assertions.assertEquals("SERVICE_UNAVAILABLE", error.path("status").asText(), response.body());
    Assertions.assertEquals("MongoDB is unavailable", error.path("message").asText(), response.body());
    Assertions.assertTrue(error.path("originalException").isMissingNode()
        || error.path("originalException").isNull(), response.body());
    Assertions.assertTrue(error.path("sourceException").isMissingNode()
        || error.path("sourceException").isNull(), response.body());
    Assertions.assertFalse(response.body().contains("127.0.0.1"), response.body());
  }
}
