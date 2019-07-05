package net.saga.snes.dev.wlalanguageserver.features;

import dev.secondsun.lsp.CompletionList;
import dev.secondsun.lsp.TextDocumentPositionParams;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import net.saga.snes.dev.wlalanguageserver.RetroCompletionCalculator;
import net.sagaoftherealms.tools.snes.assembler.main.Project;
import net.sagaoftherealms.tools.snes.assembler.pass.parse.visitor.Visitor;

public class RetroJsonCompletionFeature
    implements Feature<TextDocumentPositionParams, Optional<CompletionList>> {
  private URI workspaceRoot;

  @Override
  public void initializeFeature(URI workspaceRoot, JsonObjectBuilder initializeData) {
    this.workspaceRoot = workspaceRoot;
    var completionRegistrationOptions = Json.createObjectBuilder();
    completionRegistrationOptions.add("resolveProvider", false);

    initializeData.add("completionProvider", completionRegistrationOptions);
  }

  @Override
  public Visitor getFeatureVisitor() {
    return (ignore) -> {};
  }

  @Override
  public Optional<CompletionList> handle(
      Project project, TextDocumentPositionParams textDocumentPositionParams) {
    var retroFile = Paths.get(workspaceRoot).resolve("retro.json");
    if (retroFile.toFile().exists()) {
      RetroCompletionCalculator calc = new RetroCompletionCalculator();
      try {
        var file = Files.readString(retroFile, StandardCharsets.UTF_8);
        return calc.calculate(file, textDocumentPositionParams.position);
      } catch (IOException e) {
        e.printStackTrace();
        return Optional.empty();
      }
    } else {
      return Optional.empty();
    }
  }
}
