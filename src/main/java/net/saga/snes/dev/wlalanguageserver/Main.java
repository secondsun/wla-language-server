package net.saga.snes.dev.wlalanguageserver;

import dev.secondsun.lsp.LSP;
import java.util.logging.Logger;

public class Main {

  private static final Logger LOG = Logger.getLogger(Main.class.getName());

  public static void main(String... args) {

    LSP.connect(WLALanguageServer::new, System.in, System.out);
  }
}
