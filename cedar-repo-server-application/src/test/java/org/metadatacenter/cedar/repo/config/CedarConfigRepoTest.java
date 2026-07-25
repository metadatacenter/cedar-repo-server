package org.metadatacenter.cedar.repo.config;

import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.util.test.AbstractCedarConfigTest;

public class CedarConfigRepoTest extends AbstractCedarConfigTest {

  @Override
  protected SystemComponent getSystemComponent() {
    return SystemComponent.SERVER_REPO;
  }

}
