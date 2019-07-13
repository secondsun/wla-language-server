package net.saga.snes.dev.wlalanguageserver.completion;

import java.util.function.Function;

public enum RetroRootKeys {
  MAIN("main", (in) -> "\"\""),
  MAIN_ARCH("main-arch", (in) -> "\"\""),
  ARCH_ROOTS("arch-roots", (in) -> "[]");

  public final String key;
  private final Function<String, String> createValue;

  RetroRootKeys(String key, Function<String, String> createValue) {
    this.key = key;
    this.createValue = createValue;
  }

  public String calculateValue(String input) {
    return createValue.apply(input);
  }
}
