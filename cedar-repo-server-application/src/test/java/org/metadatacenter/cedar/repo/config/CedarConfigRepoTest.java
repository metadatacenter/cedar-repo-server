package org.metadatacenter.cedar.repo.config;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentVariable;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.util.test.TestUtil;

import java.util.HashMap;
import java.util.Map;

public class CedarConfigRepoTest {

  @Before
  public void setEnvironment() {
    Map<String, String> env = new HashMap<>();

    env.put(CedarEnvironmentVariable.CEDAR_HOST.getName(), "metadatacenter.orgx");

    env.put(CedarEnvironmentVariable.CEDAR_NET_GATEWAY.getName(), "127.0.0.1");

    env.put(CedarEnvironmentVariable.CEDAR_MONGO_APP_USER_NAME.getName(), "cedarUser");
    env.put(CedarEnvironmentVariable.CEDAR_MONGO_APP_USER_PASSWORD.getName(), "password");
    env.put(CedarEnvironmentVariable.CEDAR_MONGO_HOST.getName(), "localhost");
    env.put(CedarEnvironmentVariable.CEDAR_MONGO_PORT.getName(), "27017");

    env.put(CedarEnvironmentVariable.CEDAR_REPO_HTTP_PORT.getName(), "9002");
    env.put(CedarEnvironmentVariable.CEDAR_REPO_ADMIN_PORT.getName(), "9102");
    env.put(CedarEnvironmentVariable.CEDAR_REPO_STOP_PORT.getName(), "9202");

    env.put(CedarEnvironmentVariable.CEDAR_WORKSPACE_HTTP_PORT.getName(), "9008");
    env.put(CedarEnvironmentVariable.CEDAR_USER_HTTP_PORT.getName(), "9005");

    TestUtil.setEnv(env);
  }

  @Test
  public void testGetInstance() throws Exception {
    SystemComponent systemComponent = SystemComponent.SERVER_REPO;
    Map<String, String> environment = CedarEnvironmentVariableProvider.getFor(systemComponent);
    CedarConfig instance = CedarConfig.getInstance(environment);
    Assert.assertNotNull(instance);
  }

}