# Plan de travail avec Claude Code

Chaque étape est une session. Ouvre un terminal dans le dossier du projet, lance `claude`, et
colle la consigne. Une étape = une branche = une pull request, pour que l'historique raconte le
travail. Ne passe pas à l'étape suivante tant que `./gradlew build` n'est pas vert.

Trois habitudes qui font la différence entre cadrer un agent et le laisser faire :

- **Découpe avant de déléguer.** Une consigne par étape, pas « améliore le projet ».
- **Relis d'abord les tests.** Un test qui passe sans rien prouver est pire que pas de test :
  demande-toi ce qui devrait le faire échouer.
- **Tranche ce que l'agent ne tranche pas.** Compatibilité d'API, format de fichier, dépendances :
  ce sont tes décisions. Le fichier `CLAUDE.md` les lui rappelle, mais c'est toi qui les tiens.

---

## Étape 0 — Vérifier que tout tient sur ta machine

```
git checkout -b release/1.1.0
```

Consigne :

> Lis CLAUDE.md et README.md. Exécute `chmod +x gradlew` puis `./gradlew build`. Si le build
> échoue, diagnostique et corrige sans changer le comportement ni l'API publique ; explique-moi
> chaque correction. Ensuite crée `.github/workflows/ci.yml` avec le contenu de
> `docs/ci.yml.a-copier` et supprime ce fichier temporaire. Ne commite pas : je relis d'abord.

Quand c'est vert : relis `git diff`, puis

```
git add -A
git commit -m "release: 1.1.0 — corrections, zéro dépendance, 31 tests, Gradle 8, README"
git push -u origin release/1.1.0
```

Ouvre la PR vers `master` sur GitHub, fusionne, ajoute la description du dépôt (« About »).

---

## Étape 1 — Revue contradictoire de la 1.1.0

C'est l'étape la plus importante, parce que la 1.1.0 a été écrite vite. Fais-la sur une branche
`review/1.1.0`.

Consigne :

> Compare `git diff 87ea99d..HEAD -- src/main` (la 1.0.1 contre la 1.1.0) et fais une revue
> contradictoire, comme si tu relisais la PR d'un collègue. Cherche en priorité : régressions de
> comportement par rapport à 1.0.1, problèmes de concurrence entre les threads des chargeurs et le
> noyau, fuites de `URLClassLoader`, cas où `dispatchLogException` peut boucler (un composant de
> journalisation qui lève une exception), et tests qui passent sans prouver ce que leur nom
> annonce. Ne corrige rien tout de suite : donne-moi la liste, classée par gravité, avec le fichier
> et la ligne. Je choisis ce qu'on corrige.

Puis, pour chaque point retenu :

> Corrige le point N de ta revue. Ajoute ou modifie le test qui le prouve. Montre-moi le diff.

Note ce que tu as trouvé dans la section « Ce que je ferais autrement » du README : c'est une
deuxième couche d'honnêteté, et c'est ce qu'un gestionnaire d'embauche lira.

---

## Étape 2 — Découverte par `ServiceLoader`

Branche `feature/service-loader`.

> Ajoute la découverte des composants par `java.util.ServiceLoader` : un JAR qui contient
> `META-INF/services/rkernel.component.IComponent` est chargé sans scanner ses classes. Le scan
> actuel reste en repli quand le fichier de services est absent, pour que les JAR de 2021
> continuent de marcher. Étends `JarFixtures` pour produire les deux sortes de JAR et ajoute les
> tests : JAR avec fichier de services, JAR sans, JAR avec un fichier de services qui pointe vers
> une classe absente. Documente le nouveau mécanisme dans le README, section « Comment ça marche ».

---

## Étape 3 — Déchargement réel des modules

Branche `feature/unload`.

> Aujourd'hui `wipeComponent` retire le composant du routage mais son `URLClassLoader` reste
> ouvert. Propose-moi d'abord deux conceptions pour un chargeur fermable par module (par exemple :
> un `URLClassLoader` par JAR conservé dans `origins` et fermé au retrait ; ou un
> `ModuleLayer`), avec les compromis de chacune et l'impact sur l'API publique. Je choisis, puis tu
> implémentes avec un test qui prouve que le chargeur est fermé après suppression du JAR.

---

## Étape 4 — Nettoyage de l'API

Branche `chore/api-cleanup`. Une consigne par point, dans cet ordre :

> `SignalEvent`, `SignalListener` et `IComponent.addSignalListener` ne sont appelés nulle part.
> Propose : les brancher (le noyau notifie les listeners à chaque `processSignal`) ou les déprécier.
> Argumente en une phrase chacun, je tranche.

> Déprécie `BasicKernelLoader` et `Builder.setKernelLoader` avec un message qui renvoie vers
> la 2.0. Ne supprime rien.

> `FileWatcher.run()` avale `InterruptedException`. Ajoute une méthode `stop()` qui ferme le
> `WatchService` et termine le thread proprement, avec son test.

---

## Étape 5 — Publication 1.1.0 sur Maven Central

C'est toi qui fais cette étape, pas l'agent : elle demande tes identifiants Sonatype et ta clé GPG.
Claude Code peut t'aider à vérifier `./gradlew publishToMavenLocal` et à relire le `pom` généré
dans `build/publications/mavenJava/pom-default.xml`.

> Exécute `./gradlew publishToMavenLocal` et vérifie que le pom généré contient la licence, le
> développeur et le scm. Dis-moi ce qu'il manque par rapport aux exigences de Maven Central.

Puis, avec tes secrets dans `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`,
`signingKey`, `signingPassword`) : `./gradlew publish`, et clôture du staging sur s01.oss.sonatype.org.

---

## Étape 6 — Un exemple qu'on peut lancer

Branche `docs/example`.

> Crée un sous-projet Gradle `examples/caisse` qui démontre le noyau : une application `main` qui
> construit un `BasicKernel`, et un module `examples/caisse-paiements` compilé en JAR qu'on dépose
> dans `components/caisse/` pendant que l'application tourne. Un script `examples/demo.sh` qui
> lance l'application, dépose le JAR, envoie un signal et montre la réponse. Le README pointe
> vers l'exemple.

---

## Ce que tu notes à chaque étape

Dans `docs/journal.md` (à créer à l'étape 1), trois lignes par session : ce que tu as demandé, ce
que l'agent a proposé que tu as refusé, et pourquoi. C'est le document que tu montreras en
entrevue quand on te demandera comment tu travailles avec un agent — pas le code.
