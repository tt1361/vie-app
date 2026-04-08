# vie-app-base-provider-decompiled

This project was reconstructed from:

- `../vie-app-base-provider-0.0.1-SNAPSHOT.jar`

Reconstruction details:

- Source code was decompiled with `CFR 0.152`
- Original embedded `pom.xml` was restored to the project root
- Non-class resources were restored to `src/main/resources`
- Embedded runtime dependency jars were extracted to `boot-lib/`

Project layout:

- `pom.xml`: original Maven descriptor from the jar
- `src/main/java`: decompiled Java sources
- `src/main/resources`: extracted resources from `BOOT-INF/classes`
- `boot-lib/`: jars extracted from `BOOT-INF/lib`
- `MANIFEST.MF.original`: original manifest from the jar

Notes:

- Decompiled code may differ from the original handwritten source in formatting, local variable names, comments, and some generic type details.
- The restored `pom.xml` references internal/private artifacts such as `com.iflytek.vie:*`; depending on your environment, Maven may not resolve them directly.
- The current `mvn package` path is wired to `original-classes/`, which contains bytecode/resources extracted from the original jar. This guarantees packaging succeeds even though the decompiled Java sources are not yet fully source-rebuildable.
- If you only need to browse or patch code, the current tree is already usable in the IDE.
