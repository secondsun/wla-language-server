package net.saga.snes.dev.wlalanguageserver.features;

import com.google.gson.JsonObject;
import dev.secondsun.wla4j.assembler.main.Project;
import dev.secondsun.wla4j.assembler.pass.parse.visitor.Visitor;
import java.net.URI;

/**
 * Features describe to the language server how to initialize and use the capabilities.
 *
 * <p>
 * For example a DocumentLink feature will provide default options for the initialize respose as well as visitors for
 * the parser and the handler for a document link request
 */
public interface Feature<PARAMS, OUTPUT> {

    void initializeFeature(URI workspaceRoot, JsonObject initializeData);

    Visitor getFeatureVisitor();

    OUTPUT handle(Project project, PARAMS params);
}
