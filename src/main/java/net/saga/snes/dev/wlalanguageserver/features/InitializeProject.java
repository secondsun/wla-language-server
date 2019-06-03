package net.saga.snes.dev.wlalanguageserver.features;

import java.util.Collections;
import java.util.List;
import net.sagaoftherealms.tools.snes.assembler.main.Project;

/** This class will handle setting up a project, connecting visitors, and registering features. */
public class InitializeProject {

  private final String workspaceRoot;
  private final List<Feature> features;

  public InitializeProject(String workspaceRoot, List<Feature> features) {
    this.workspaceRoot = workspaceRoot;
    this.features = Collections.unmodifiableList(features);
  }

  public Project getProject() {
    var builder = new Project.Builder(this.workspaceRoot);

    features.stream().map(Feature::getFeatureVisitor).forEach(builder::addVisitor);

    return builder.build();
  }
}
