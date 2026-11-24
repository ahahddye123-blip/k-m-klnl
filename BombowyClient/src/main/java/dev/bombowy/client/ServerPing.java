package dev.bombowy.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;

final class ServerPing {
    private ServerPing() {}

    static final class Result {
        boolean ok;
        String motd = "";
        String version = "";
        int online;
        int max;
        long ms = -1;
        String error = "";
    }

    static Result ping(String input) {
        Result r = new Result();
        try {
            String host = input.trim();
            int port = 25565;
            boolean explicitPort = false;
            int colon = host.lastIndexOf(':');
            if (colon > 0 && colon < host.length() - 1) {
                try {
                    port = Integer.parseInt(host.substring(colon + 1));
                    host = host.substring(0, colon);
                    explicitPort = true;
                } catch (NumberFormatException ignored) {}
            }
            String connectHost = host;
            if (!explicitPort) {
                String[] srv = resolveSrv(host);
                if (srv != null) {
                    connectHost = srv[0];
                    port = Integer.parseInt(srv[1]);
                }
            }

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(connectHost, port), 2500);
                socket.setSoTimeout(2500);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream());

                ByteArrayOutputStream hs = new ByteArrayOutputStream();
                DataOutputStream h = new DataOutputStream(hs);
                writeVarInt(h, 0x00);
                writeVarInt(h, 769);
                writeString(h, host);
                h.writeShort(port);
                writeVarInt(h, 1);
                writeVarInt(out, hs.size());
                out.write(hs.toByteArray());

                writeVarInt(out, 1);
                writeVarInt(out, 0x00);
                out.flush();

                readVarInt(in);
                int id = readVarInt(in);
                if (id != 0x00) throw new IOException("bad packet");
                int len = readVarInt(in);
                byte[] data = new byte[len];
                in.readFully(data);
                String json = new String(data, StandardCharsets.UTF_8);

                JsonObject o = JsonParser.parseString(json).getAsJsonObject();
                if (o.has("players") && o.get("players").isJsonObject()) {
                    JsonObject p = o.getAsJsonObject("players");
                    r.online = p.has("online") ? p.get("online").getAsInt() : 0;
                    r.max = p.has("max") ? p.get("max").getAsInt() : 0;
                }
                if (o.has("version") && o.get("version").isJsonObject()) {
                    JsonObject v = o.getAsJsonObject("version");
                    r.version = v.has("name") ? v.get("name").getAsString() : "";
                }
                if (o.has("description")) r.motd = clean(flatten(o.get("description")));

                long t0 = System.nanoTime();
                ByteArrayOutputStream ps = new ByteArrayOutputStream();
                DataOutputStream p2 = new DataOutputStream(ps);
                writeVarInt(p2, 0x01);
                p2.writeLong(t0);
                writeVarInt(out, ps.size());
                out.write(ps.toByteArray());
                out.flush();
                try {
                    readVarInt(in); readVarInt(in); in.readLong();
                    r.ms = (System.nanoTime() - t0) / 1_000_000L;
                } catch (IOException ignored) {}
                r.ok = true;
            }
        } catch (Exception e) {
            r.ok = false;
            r.error = "brak odpowiedzi";
        }
        return r;
    }

    private static String[] resolveSrv(String host) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("java.naming.provider.url", "dns:");
            env.put("com.sun.jndi.dns.timeout.initial", "1500");
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes("_minecraft._tcp." + host, new String[]{"SRV"});
            Attribute a = attrs.get("srv");
            if (a != null && a.size() > 0) {
                String[] parts = a.get(0).toString().split(" ");
                String target = parts[3];
                if (target.endsWith(".")) target = target.substring(0, target.length() - 1);
                return new String[]{target, parts[2]};
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String flatten(JsonElement e) {
        if (e == null || e.isJsonNull()) return "";
        if (e.isJsonPrimitive()) return e.getAsString();
        if (e.isJsonArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonElement x : e.getAsJsonArray()) sb.append(flatten(x));
            return sb.toString();
        }
        JsonObject o = e.getAsJsonObject();
        StringBuilder sb = new StringBuilder();
        if (o.has("text")) sb.append(o.get("text").getAsString());
        if (o.has("extra")) sb.append(flatten(o.get("extra")));
        return sb.toString();
    }

    private static String clean(String s) {
        return s.replaceAll("§.", "").replace('
', ' ').replaceAll("\s+", " ").trim();
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int result = 0, shift = 0;
        while (true) {
            int b = in.readUnsignedByte();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
            if (shift > 35) throw new IOException("VarInt too big");
        }
    }

    private static void writeString(DataOutputStream out, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, b.length);
        out.write(b);
    }
}
