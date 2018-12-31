package net.saga.snes.dev.wlalanguageserver;

import java.util.logging.Logger;
import org.javacs.lsp.LSP;

public class Main {

  private static final Logger LOG = Logger.getLogger(Main.class.getName());

  public static void main(String... args) {

    LSP.connect(WLALanguageServer::new, System.in, System.out);
  }
}
