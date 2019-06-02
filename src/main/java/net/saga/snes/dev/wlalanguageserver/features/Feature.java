package net.saga.snes.dev.wlalanguageserver.features;

import com.google.gson.JsonObject;
import net.sagaoftherealms.tools.snes.assembler.main.Project;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.visitor.Visitor;

/**
 * Features describe to the language server how to initialize and use the capabilities.
 *
 * For example a DocumentLink feature will provide default options for the initialize respose as well as visitors for the parser and the handler for a document link request
 */
public interface Feature<PARAMS, OUTPUT> {

    void initializeFeature(String workspaceRoot, JsonObject initializeData);

    Visitor getFeatureVisitor(Project project);

    OUTPUT handle(PARAMS params);
}
