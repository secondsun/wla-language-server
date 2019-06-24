package net.saga.snes.dev.wlalanguageserver.features;

import com.google.gson.JsonObject;
import java.net.URI;
import net.sagaoftherealms.tools.snes.assembler.main.Project;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.visitor.Visitor;

/**
 * Features describe to the language server how to initialize and use the capabilities.
 *
 * <p>For example a DocumentLink feature will provide default options for the initialize respose as
 * well as visitors for the parser and the handler for a document link request
 */
public interface Feature<PARAMS, OUTPUT> {

  void initializeFeature(URI workspaceRoot, JsonObject initializeData);

  Visitor getFeatureVisitor();

  OUTPUT handle(Project project, PARAMS params);
}
