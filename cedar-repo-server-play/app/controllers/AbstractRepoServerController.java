package controllers;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.server.play.AbstractCedarController;

public abstract class AbstractRepoServerController extends AbstractCedarController {

  protected static CedarConfig cedarConfig;

  static {
    cedarConfig = CedarConfig.getInstance();
  }
}
