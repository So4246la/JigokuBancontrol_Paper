package jp.example.jigokubancontrol;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class JigokuBanControlPlugin extends JavaPlugin implements Listener, PluginMessageListener {

    private static final String CHANNEL = "myserver:bancontrol";

    private boolean isNight(World world) {
        long time = world.getTime();
        return time >= 12000 && time < 24000;
    }

    @Override
    public void onEnable() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("JigokuBanControlが有効になりました。");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             DataOutputStream data = new DataOutputStream(out)) {
            data.writeUTF("death");
            data.writeUTF(player.getUniqueId().toString());
            player.sendPluginMessage(this, CHANNEL, out.toByteArray());
        } catch (IOException e) {
            getLogger().warning("死亡通知の送信中にエラーが発生しました。");
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isNight(player.getWorld())) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 DataOutputStream data = new DataOutputStream(out)) {
                data.writeUTF("night_logout");
                data.writeUTF(player.getUniqueId().toString());
                player.sendPluginMessage(this, CHANNEL, out.toByteArray());
            } catch (IOException e) {
                getLogger().warning("夜間ログアウト通知の送信中にエラーが発生しました。");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) return;
        try {
            String sub;
            try (var in = new java.io.DataInputStream(new java.io.ByteArrayInputStream(message))) {
                sub = in.readUTF();
                if ("get_time".equals(sub)) {
                    String worldName = in.readUTF();
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        long time = world.getTime();
                        try (var out = new ByteArrayOutputStream();
                             var data = new DataOutputStream(out)) {
                            data.writeUTF("time_response");
                            data.writeUTF(worldName);
                            data.writeLong(time);
                            player.sendPluginMessage(this, CHANNEL, out.toByteArray());
                        }
                    }
                }
            }
        } catch (IOException e) {
            getLogger().warning("PluginMessage受信処理中にエラーが発生しました。");
            e.printStackTrace();
        }
    }
}
