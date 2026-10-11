# MegaHarvest (Fabric, Minecraft Java 26.3)

Client-side mod. Jedním stiskem klávesy (výchozí **H**, mění se v Ovládání → MegaHarvest)
sklidí všechny dozrálé plodiny v oblasti **3 × 3** kolem bloku, na který míříš, a znovu je zasadí,
pokud máš v inventáři semena / sadbu. Žádné automatické opakování – jeden stisk = jedna sklizeň.

Podporováno: pšenice, mrkev, brambory, červená řepa, netherová bradavice.

## Chování
- Míříš na plodinu nebo na půdu pod ní; střed oblasti je ta plodina.
- Sklidí se jen plně dozrálé plodiny v dosahu hráče.
- Sadba se bere z hotbaru, jinak z inventáře (po sklizni se inventář vrátí do původního stavu).
- Znovu se sází jen tím, co máš u sebe v okamžiku stisku; úrodu, která ještě leží na zemi, mod nesbírá.

## Sestavení
Požadavky: JDK 25.

```
gradle wrapper --gradle-version 9.5.1   # jednou, vytvoří gradlew
./gradlew build
```
Hotový JAR: `build/libs/megaharvest-1.0.0.jar` (soubor `-sources` nenahrávej do `mods`).

Bez lokálního nastavení: nahraj projekt na GitHub, workflow `.github/workflows/build.yml`
JAR sestaví a nabídne ho jako artefakt.

## Instalace
`megaharvest-1.0.0.jar` do složky `mods` spolu s Fabric Loader 0.19.5+ a Fabric API pro 26.3.
