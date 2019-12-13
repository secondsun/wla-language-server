package net.saga.snes.dev.wlalanguageserver.completion;

public enum RetroArches {
  Z80("z80"),
  SPC_700("spc700"),
  SNES_65816("65816"),
  GB("gb");

  public final String arch;

  RetroArches(String arch) {
    this.arch = arch;
  }
}
