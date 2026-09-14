# rkernel — contexte pour Claude Code

Bibliothèque Java publiée sur Maven Central (`io.github.rb958:rkernel`, 1.0 et 1.0.1 en 2021).
Micro-noyau où des modules se greffent sous forme de JAR : détection par `WatchService`, chargement
par réflexion, communication par signaux typés routés via un registre persisté en XML.
Version en cours : 1.1.0 (remise à niveau de septembre 2026). Auteur unique : Richie Akawa.

## Commandes

```bash
./gradlew build          # compile (release 17), javadoc, tests — doit rester vert
./gradlew test           # rapport : build/reports/tests/test/index.html
./gradlew javadoc        # aucune erreur tolérée, doclint désactivé volontairement
```

Java 17 ou 21. Aucune dépendance à l'exécution — c'est une décision, pas un oubli.

## Carte du code

| Fichier | Rôle |
|---|---|
| `rkernel/IKernel`, `BasicKernel` | Le noyau et son `Builder`. Possède composants, noyaux secondaires, `SignalManager`. |
| `rkernel/component/IComponent` | Contrat d'un module : nom, types de signaux, `processSignal`. |
| `rkernel/component/IComponentLoader` | `loadSingleFile` : trouve dans un JAR la première classe qui implémente l'interface. |
| `rkernel/component/BasicComponentLoader` | Charge les JAR d'un dossier, surveille le dossier, retire un composant dont le JAR disparaît. |
| `rkernel/BasicKernelLoader` | Même chose pour des noyaux secondaires (fonction peu utilisée, candidate au retrait). |
| `rkernel/signal/SignalRegistry` | Table type → composant ou noyau. Un type, un interprète. Insensible à la casse. |
| `rkernel/signal/SignalManager` | Routage + persistance du registre dans `<documentRoot>/registries/<nom>.xml`. |
| `rkernel/utils/file/FileManager` | Lecture/écriture du registre avec le DOM du JDK. Une instance par dossier. |
| `rkernel/utils/file/FileWatcher` | Boucle `WatchService` sur un thread démon, listeners création/suppression/modification. |
| `src/test/java/rkernel/support/JarFixtures` | Compile un composant à la volée et l'empaquette en JAR pour les tests du chargeur. |

## Règles à respecter

1. **Compatibilité de l'API publique.** La bibliothèque est publiée. Une méthode publique ne
   disparaît pas : on la déprécie avec `@Deprecated` et un `default` qui délègue (voir
   `IComponent.getSIgnalType()`). Un changement cassant attend une 2.0.
2. **Format du registre XML inchangé.** `<signalRegistry><Kernel>…</Kernel><SignalTypeEntries type=… componentName=… kernelName=…/>`.
   Le test `readsARegistryWrittenByVersion1` doit continuer de passer.
3. **Zéro dépendance d'exécution.** JUnit en test seulement. Ne pas ajouter de bibliothèque sans
   en discuter d'abord.
4. **Tout changement de comportement arrive avec son test.** Les tests sont en JUnit 5, nommés
   en phrases (`aJarWithoutAComponentIsSkippedNotACrash`). Pas de test qui passe sans rien prouver.
5. **Langues.** Code, identifiants et Javadoc en anglais (c'est la convention du dépôt depuis 2021).
   README et documents dans `docs/` en français. Messages de commit en français, format
   `type(portée): description` comme l'historique (`fix: (loader) …`).
6. **Ne pas réécrire l'histoire.** Le README nomme les défauts de 2021 volontairement. On
   n'efface pas cette section, on l'enrichit si on trouve autre chose.
7. **Threads.** Les chargeurs tournent sur des threads démons ; toute structure partagée avec le
   noyau est concurrente (`ConcurrentHashMap`). Vérifier avant d'ajouter un état.

## Ce que je sais être encore faible (candidats à la 1.2)

- Un `URLClassLoader` par JAR, jamais fermé : retirer un composant le sort du routage mais ses
  classes restent en mémoire. Un vrai déchargement demande un chargeur fermable par module.
- La découverte scanne toutes les entrées du JAR et charge chaque classe pour tester l'interface.
  `ServiceLoader` (`META-INF/services/rkernel.component.IComponent`) serait plus propre et plus
  rapide ; à ajouter en priorité, le scan restant en repli.
- `SignalEvent` / `SignalListener` / `IComponent.addSignalListener` existent mais rien ne les
  appelle. Les brancher ou les retirer (dépréciés).
- `BasicKernel.signals` est une `ArrayList` exposée par `getSignalType()` ; le noyau lui-même
  n'interprète rien aujourd'hui.
- Les noyaux secondaires (`BasicKernelLoader`) chargés depuis la racine du projet : idée jamais
  exploitée, à déprécier.
- `FileWatcher.run()` avale `InterruptedException` en imprimant une trace ; pas de mécanisme
  d'arrêt propre du thread.
- Pas de `module-info.java`.

Le plan détaillé, avec les consignes à me donner session par session, est dans
`docs/PLAN-CLAUDE-CODE.md`.
