package dev.bombowy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class BombowyExtras {
    private BombowyExtras() {}

    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();
    private static final Set<String> knownBombUsers = new HashSet<>();

    public static void recordClick(int button) {
        long now = System.currentTimeMillis();
        if (button == 0) leftClicks.addLast(now);
        else if (button == 1) rightClicks.addLast(now);
        prune(now);
    }

    public static void tick(MinecraftClient client) {
        prune(System.currentTimeMillis());
        if (client.player != null) {
            String selfName = client.player.getGameProfile().getName();
            if (selfName != null && !selfName.isBlank()) {
                knownBombUsers.add(selfName.toLowerCase(Locale.ROOT));
            }
            if (Modules.isOn("autosprint") && client.options.forwardKey.isPressed() && !client.player.isSneaking()) {
                if (!client.player.isSprinting()) {
                    client.player.setSprinting(true);
                }
            }
        }
    }

    private static void prune(long now) {
        while (!leftClicks.isEmpty() && now - leftClicks.peekFirst() > 1000L) leftClicks.pollFirst();
        while (!rightClicks.isEmpty() && now - rightClicks.peekFirst() > 1000L) rightClicks.pollFirst();
    }

    public static int getLeftCps() {
        prune(System.currentTimeMillis());
        return leftClicks.size();
    }

    public static int getRightCps() {
        prune(System.currentTimeMillis());
        return rightClicks.size();
    }

    public static int getPing(MinecraftClient client) {
        if (client.getNetworkHandler() == null || client.player == null) return 0;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        if (entry == null) {
            entry = client.getNetworkHandler().getPlayerListEntry(client.player.getGameProfile().getName());
        }
        return entry != null ? Math.max(0, entry.getLatency()) : 0;
    }

    public static boolean isBombUser(String playerName) {
        if (playerName == null || playerName.isBlank()) return false;
        BombowyConfig cfg = BombowyConfig.get();
        if (!cfg.bombTagEnabled) return false;
        if (cfg.bombTagAllPlayers) return true;
        String lower = playerName.toLowerCase(Locale.ROOT);
        if (knownBombUsers.contains(lower)) return true;
        for (String extra : cfg.bombTagPlayers) {
            if (extra != null && extra.trim().equalsIgnoreCase(playerName.trim())) {
                return true;
            }
        }
        return false;
    }

    public static Text formatPlayerText(Text original, String rawPlayerName) {
        if (original == null || rawPlayerName == null || rawPlayerName.isBlank()) return original;
        if (!isBombUser(rawPlayerName)) return original;

        String full = original.getString();
        int idx = full.toLowerCase(Locale.ROOT).indexOf(rawPlayerName.toLowerCase(Locale.ROOT));
        MutableText out = Text.empty();

        if (idx > 0) {
            String prefix = full.substring(0, idx);
            out.append(Text.literal(prefix).setStyle(findStyleAt(original, 0)));
        }

        String actualNick = (idx >= 0) ? full.substring(idx, idx + rawPlayerName.length()) : rawPlayerName;
        out.append(Text.literal("(💣) ").setStyle(Style.EMPTY.withColor(Formatting.BLACK)));
        out.append(Text.literal(actualNick).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));

        if (idx >= 0 && idx + actualNick.length() < full.length()) {
            String suffix = full.substring(idx + actualNick.length());
            out.append(Text.literal(suffix).setStyle(findStyleAt(original, idx + actualNick.length())));
        }

        return out;
    }

    private static Style findStyleAt(Text text, int targetIndex) {
        final int[] cursor = {0};
        final Style[] found = {Style.EMPTY};
        text.visit((style, str) -> {
            int next = cursor[0] + str.length();
            if (targetIndex >= cursor[0] && targetIndex < next) {
                found[0] = style;
                return java.util.Optional.empty();
            }
            cursor[0] = next;
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }
}
