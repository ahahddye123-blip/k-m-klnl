package dev.bombowy.client;

import java.util.List;
import java.util.function.Consumer;

public final class Modules {
    private Modules() {}

    public enum Tab {
        ALL("Wszystkie"),
        ENABLED("Aktywne"),
        WALKA("Walka"),
        WYGLAD("Wygląd"),
        HUD("HUD"),
        NARZEDZIA("Narzędzia");

        public final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    public enum Icon {
        HITBOX, CLEAN_VIEW, NO_PARTICLES, COLOR_BOOST, INVENTORY, COOLDOWNS,
        FPS_HUD, STATS_HUD, NAMETAGS, OPTIMIZATION, QUALITY, BRIGHTNESS,
        UNLIMITED_FPS, ENTITY_DIST, CHUNK_CAP, QUICK_JOIN, AUTOSPRINT
    }

    public static final class Module {
        public final String id;
        public final String name;
        public final String desc;
        public final Tab tab;
        public final Icon icon;
        public final boolean hasSettings;
        private final Consumer<Boolean> action;

        Module(String id, String name, String desc, Tab tab, Icon icon, boolean hasSettings, Consumer<Boolean> action) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.tab = tab;
            this.icon = icon;
            this.hasSettings = hasSettings;
            this.action = action;
        }

        public boolean isOn() {
            return Boolean.TRUE.equals(BombowyConfig.get().modules.get(id));
        }

        public void setOn(boolean on) {
            action.accept(on);
            BombowyConfig.get().modules.put(id, on);
            if ("fpshud".equals(id)) BombowyConfig.get().hudFps.enabled = on;
            if ("statshud".equals(id)) BombowyConfig.get().hudStats.enabled = on;
            if ("inventoryhud".equals(id)) BombowyConfig.get().hudInventory.enabled = on;
            if ("cooldowns".equals(id)) BombowyConfig.get().hudCooldowns.enabled = on;
            Features.writeOptions();
            BombowyConfig.save();
        }

        public void toggle() {
            setOn(!isOn());
        }
    }

    public static final List<Module> ALL = List.of(
            new Module("hitboxes", "Hitboxy", "Ramki graczy przy celowaniu i z boku.", Tab.WALKA, Icon.HITBOX, true, on -> {}),
            new Module("autosprint", "AutoSprint", "Automatyczny sprint podczas ruchu do przodu.", Tab.WALKA, Icon.AUTOSPRINT, false, on -> {}),
            new Module("cleanview", "Czysty obraz", "Niski ogień, brak winiety i trzęsienia kamery.", Tab.WYGLAD, Icon.CLEAN_VIEW, true, on -> {}),
            new Module("noparticles", "Bez cząsteczek", "Bez okruchów bloków przy kopaniu i więcej FPS.", Tab.WYGLAD, Icon.NO_PARTICLES, false, on -> {}),
            new Module("colorboost", "Boost kolorów", "Żywe kolory świata i subtelne odcienie.", Tab.WYGLAD, Icon.COLOR_BOOST, true, on -> {}),
            new Module("quality", "Jakość 2K/4K", "Ostrzejsze tekstury i płynniejsze przejścia.", Tab.WYGLAD, Icon.QUALITY, false, Features::quality),
            new Module("brightness", "Jasność", "Wyraźny obraz w nocy i w jaskiniach.", Tab.WYGLAD, Icon.BRIGHTNESS, false, on -> {}),
            new Module("fpshud", "Pokaż FPS", "Licznik FPS na ekranie z własnym kolorem.", Tab.HUD, Icon.FPS_HUD, true, on -> BombowyConfig.get().hudFps.enabled = on),
            new Module("statshud", "CPS i Ping", "Licznik kliknięć na sekundę oraz pingu na ekranie.", Tab.HUD, Icon.STATS_HUD, true, on -> BombowyConfig.get().hudStats.enabled = on),
            new Module("inventoryhud", "Ekwipunek HUD", "Podgląd ekwipunku na ekranie.", Tab.HUD, Icon.INVENTORY, true, on -> BombowyConfig.get().hudInventory.enabled = on),
            new Module("cooldowns", "Cooldowny", "Odliczanie czasu dla przedmiotów.", Tab.HUD, Icon.COOLDOWNS, true, on -> BombowyConfig.get().hudCooldowns.enabled = on),
            new Module("nametags", "Nicki i Tag", "Tag (💣) na czarno i biały nick + własny nick w F5.", Tab.HUD, Icon.NAMETAGS, true, on -> {}),
            new Module("optimization", "Optymalizacja", "Duży boost FPS, tryb słabego PC i pogoda.", Tab.NARZEDZIA, Icon.OPTIMIZATION, true, Features::optimization),
            new Module("fpsboost", "FPS Boost", "Szybka grafika, mniej cząsteczek, bez chmur i AO.", Tab.NARZEDZIA, Icon.OPTIMIZATION, false, Features::fpsBoost),
            new Module("unlimitedfps", "Unlimited FPS", "Bez limitu FPS i bez VSync.", Tab.NARZEDZIA, Icon.UNLIMITED_FPS, false, Features::unlimitedFps),
            new Module("entitydist", "Dystans encji", "Mniejszy dystans renderowania encji dla FPS.", Tab.NARZEDZIA, Icon.ENTITY_DIST, false, Features::entityDistance),
            new Module("chunkcap", "Limit chunków", "Ogranicza dystans renderowania i symulacji.", Tab.NARZEDZIA, Icon.CHUNK_CAP, false, Features::chunkCap),
            new Module("quickjoin", "Szybki podgląd", "Podgląd serwera po wpisaniu adresu IP.", Tab.NARZEDZIA, Icon.QUICK_JOIN, false, on -> {})
    );

    public static boolean isOn(String id) {
        return Boolean.TRUE.equals(BombowyConfig.get().modules.get(id));
    }

    public static Module byId(String id) {
        for (Module m : ALL) {
            if (m.id.equals(id)) return m;
        }
        return null;
    }
}
