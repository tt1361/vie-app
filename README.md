# vie-app-base-provider-decompiled-generated

This project was reconstructed from:

- `/Users/tangmingxiang/Desktop/vie-app-base-provider-0.0.1-SNAPSHOT.jar`

Reconstruction details:

- Java classes were decompiled with IntelliJ's built-in Java decompiler
- Non-class resources were restored to `src/main/resources`
- Embedded runtime dependency jars were extracted to `boot-lib/`
- Original bytecode/resources were preserved in `original-classes/`
- Embedded Maven metadata was restored to the project root

Project layout:

- `pom.xml`: original Maven descriptor from the jar
- `src/main/java`: decompiled Java sources
- `src/main/resources`: extracted resources from `BOOT-INF/classes`
- `boot-lib/`: jars extracted from `BOOT-INF/lib`
- `original-classes/`: original classes/resources extracted from the jar
- `MANIFEST.MF.original`: original manifest from the jar

Build:

```bash
mvn -q -DskipTests package
```

Notes:

- Decompiled code may differ from the original handwritten source in formatting, local variable names, comments, and some generic type details.
- The restored `pom.xml` references system-scope dependencies under `boot-lib/`.
