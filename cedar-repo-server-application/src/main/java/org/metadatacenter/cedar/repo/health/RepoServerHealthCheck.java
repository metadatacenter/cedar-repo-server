package org.metadatacenter.cedar.repo.health;

import com.codahale.metrics.health.HealthCheck;

public class RepoServerHealthCheck extends HealthCheck {

  public RepoServerHealthCheck() {
  }

  @Override
  protected Result check() throws Exception {
    if (2 * 2 == 5) {
      return Result.unhealthy("Unhealthy, because 2 * 2 == 5");
    }
    return Result.healthy();
  }
}