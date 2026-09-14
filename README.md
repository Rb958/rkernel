# rkernel

*A small Java kernel onto which modules are grafted as JAR plugins, hot-loaded and talking through
signals. French documentation below; the code and its Javadoc are in English.*

Un micro-noyau Java sur lequel des modules se greffent sous forme de JAR. On dépose un JAR dans un
dossier, le noyau le détecte, le charge et l'intègre — sans redémarrer l'application. Les modules ne
se connaissent pas entre eux : ils communiquent par des signaux typés que le noyau route vers celui
qui sait les interpréter.

Publié sur Maven Central en octobre 2021 sous `io.github.rb958:rkernel`. Projet personnel, écrit
seul, remis à niveau en 2026 — la section [Ce que je ferais autrement](#ce-que-je-ferais-autrement-et-ce-que-jai-fait) dit ce qui a changé et pourquoi.

```groovy
implementation 'io.github.rb958:rkernel:1.1.0'
```

## Pourquoi

En 2021 je travaillais sur une plateforme où chaque nouvelle fonctionnalité — un canal de paiement,
un connecteur, un rapport — obligeait à redéployer tout le service. Je voulais un socle où une
fonctionnalité serait un fichier qu'on dépose, et où la retirer serait le supprimer. Ce dépôt est ce
socle, réduit à l'essentiel : le chargement à chaud, le routage des signaux et la persistance de la
table de routage.

## Comment ça marche

```
                        components/<nom du noyau>/
                        ├── paiements.jar   ──┐
                        └── journal.jar     ──┤  FileWatcher (java.nio WatchService)
                                              │  création → chargement, suppression → retrait
                                              ▼
   signal "paiement"  ──►  BasicKernel  ──►  SignalManager  ──►  registries/<nom>.xml
                              │                  │
                              │                  └── quel composant interprète quel type ?
                              ▼
                        IComponent.processSignal(signal)  ──►  réponse
```

**Le noyau** (`BasicKernel`) est construit par un `Builder`. Il possède ses composants, un
gestionnaire de signaux, et éventuellement des noyaux secondaires eux-mêmes chargés depuis des JAR.

**Un composant** (`IComponent`) est une classe publique avec un constructeur sans argument, dans un
JAR. Elle déclare les types de signaux qu'elle interprète (`getSignalTypes()`) et les traite
(`processSignal`). Le chargeur (`BasicComponentLoader`) ouvre le JAR, trouve la première classe qui
implémente `IComponent`, l'instancie par réflexion et enregistre ses types auprès du noyau.

**Un signal** (`BasicSignal<T>`) est un type et une charge utile. Le noyau cherche l'interprète du
type dans son registre et lui passe le signal. Un type a exactement un interprète ; enregistrer un
second le remplace.

**Le registre** (`SignalRegistry`) est persisté en XML dans `registries/<nom du noyau>.xml`. Un
noyau redémarré retrouve sa table de routage sans recharger tous les JAR d'abord.

### Exemple

```java
BasicKernel kernel = new BasicKernel.Builder()
        .setName("caisse")
        .setDocumentRoot(new File("/opt/caisse"))
        .setComponentLoader(new BasicComponentLoader())
        .build();
kernel.load();   // charge /opt/caisse/components/caisse/*.jar, puis surveille le dossier

Object recu = kernel.processSignal(new BasicSignal<String>("paiement", "25000 XAF") {});
```

Un composant minimal :

```java
public class Paiements implements IComponent {
    public Collection<String> getSignalTypes() { return List.of("paiement"); }
    public Object processSignal(BasicSignal<?> s) { return "reçu : " + s.getPayload(); }
    public String getName() { return "paiements"; }
    public void load(IKernel k) {}
    // …
}
```

Compilé en `paiements.jar` et déposé dans `/opt/caisse/components/caisse/`, il est chargé sans
redémarrage. Le supprimer le retire.

## Essayer

```bash
./gradlew build      # compile, javadoc, 31 tests
./gradlew test       # rapport dans build/reports/tests/test/index.html
```

Java 17 ou plus. Aucune dépendance à l'exécution.

Les tests du chargeur compilent de vrais composants au moment du test, dans des paquetages qui
n'existent pas sur le classpath, et les empaquettent en JAR : le chargeur doit réellement les
trouver dans le fichier. Un test dépose un JAR dans un dossier surveillé et vérifie que le
composant répond, sans redémarrage — c'est la promesse du projet, donc c'est testé.

## Ce que je ferais autrement, et ce que j'ai fait

J'ai écrit ce code en octobre 2021, avec un an et demi de Java professionnel. Je l'ai relu en
septembre 2026 avec six ans. Voici ce que j'y ai vu, et ce que la version 1.1.0 corrige. Je laisse
les défauts nommés plutôt que de les effacer de l'historique : c'est le chemin qui est intéressant.

**Une boucle qui plantait sur le cas normal.** `loadSingleFile` parcourait les entrées du JAR avec
`while (hasMoreElements() || !found)`. Sur un JAR sans composant — un cas parfaitement normal —
elle dépassait la dernière entrée et mourait en `NoSuchElementException`. C'était un `&&`. Le
test `aJarWithoutAComponentIsSkippedNotACrash` garde le souvenir.

**Un `NullPointerException` dans le constructeur.** `Builder.build()` appelait
`componentLoader.setKernel()` sans vérifier que le chargeur avait été fourni. Un noyau sans
composants ne pouvait pas exister.

**Un singleton qui ignorait ses arguments.** `FileManager.getInstance(dossier)` gardait la
première instance et ignorait tous les dossiers suivants. Deux noyaux dans la même JVM lisaient le
même dossier. Même chose pour le registre, qui était un champ `static` partagé par tous les noyaux :
ils écrasaient mutuellement leur table de routage. Les deux sont maintenant des instances.

**Deux JAXB incompatibles.** `javax.xml.bind` 2.3 et `jakarta.xml.bind` 3.0 dans le même build,
deux espaces de noms qui ne se parlent pas. Le registre est un XML de vingt lignes ; le DOM du JDK
suffit, et la bibliothèque n'a plus aucune dépendance. Le format produit est identique, un test
lit un fichier écrit par la 1.0.1.

**Pas un seul test.** JUnit était déclaré, aucun test n'existait. J'ai vérifié le code à la main,
en 2021, en déposant des JAR dans un dossier et en regardant la console. Ça a marché jusqu'à ce
que ça ne marche plus, et je n'avais aucun moyen de savoir quand. Il y en a 31 maintenant, dont
un qui teste le rechargement à chaud avec un vrai `WatchService`.

**Le retrait d'un composant relisait un JAR supprimé.** `wipeComponent` rouvrait le fichier pour
retrouver le nom du composant — fichier qui venait d'être effacé. Le chargeur note maintenant
d'où vient chaque composant au moment du chargement.

**Une faute de frappe dans une API publique.** `getSIgnalType()`. La bibliothèque étant publiée,
l'ancien nom reste, déprécié, et délègue au nouveau.

**Ce que je referais pareil.** Le découpage `IKernel` / `IComponent` / `ISignalManager` avec des
implémentations `Basic*`, la persistance du registre pour survivre à un redémarrage, le git-flow
avec branches `hotfix/` et tags. Et la publication sur Maven Central : passer par Sonatype, la
signature GPG et la validation du domaine m'a appris plus sur la chaîne de livraison
Java que n'importe quel cours.

**Ce que je ne ferais plus du tout.** Un `URLClassLoader` par JAR sans jamais le fermer ni
l'isoler, ce qui rend le déchargement réel impossible — la 1.1.0 retire le composant du routage
mais ses classes restent en mémoire. Une vraie version 2 utiliserait `ServiceLoader` et un
chargeur par module qu'on peut fermer, ou tout simplement le système de modules de Java 9. Et les
noyaux secondaires chargés depuis la racine du projet sont une idée que je n'ai jamais utilisée en
pratique ; je la retirerais.

## Historique

| Version | Date | |
|---|---|---|
| 1.0 | 25 octobre 2021 | Première publication sur Maven Central |
| 1.0.1 | 16 novembre 2021 | Réponse renvoyée après traitement d'un signal |
| 1.1.0 | septembre 2026 | Corrections ci-dessus, zéro dépendance, 31 tests, Gradle 8, Java 17 |

## Licence

Apache 2.0. Voir [LICENSE](LICENSE).
