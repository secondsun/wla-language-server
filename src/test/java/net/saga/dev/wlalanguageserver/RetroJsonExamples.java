package net.saga.dev.wlalanguageserver;

/**
 * This class is just a static bag of same retro.json files as multiline strings.
 */
public final class RetroJsonExamples {
    private RetroJsonExamples() {

    }


    public static final String EMPTY = "";
    public static final String BLANK = """
                                          {
                                          }""";
    public static final String BLANK_MAIN = """
                                            {
                                            "main" :""
                                            }
                                            """;
    public static final String BLANK_MA = """
                                            {
                                            "ma" :""
                                            }
                                          """;
    public static final String SUGGEST_ARCH_ROOT = """
                                                    {
                                                    "main" : "main.s",
                                                    "":""
                                                    }""";

    public static final String SUGGEST_ARCH_ROOT_WITH_INCOMPLETE = """
                                                    {
                                                    "main" : "main.s",
                                                    }""";

    public static final String SUGGEST_ARCHES = """
            {
            "main": "",
            "main-arch":
            }
            """;

    public static final String SUGGEST_ARCH_ROOTS = """
                                            {
                                                "main": "",
                                                "main-arch": "gb",
                                                "arch-roots": [        ]
                                            }
                                            """;

    public static final String SUGGEST_ARCH_ROOT_OBJECT = """
                                            {
                                                "main": "",
                                                "main-arch": "gb",
                                                "arch-roots": [{        }]
                                            }
                                            """;

    public static final String SUGGEST_ARCH_ROOT_OBJECT_KEYS ="""
                            {

                            "arch-roots": [  {
                            "path":""

                            }   ]

                            }
    """;

}
