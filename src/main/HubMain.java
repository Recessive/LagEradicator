package main;

import arc.*;
import arc.net.Server;
import arc.util.*;
import mindustry.core.Version;
import mindustry.entities.bullet.BulletType;
import mindustry.game.EventType;
import mindustry.game.EventType.*;
import mindustry.game.Gamemode;
import mindustry.game.Rules;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.*;
import mindustry.net.Net;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.rmi.Remote;

import static mindustry.Vars.*;

public class HubMain extends Plugin {

    private final Rules rules = new Rules();

    private final boolean[] serversUp = {false, false, false};

    private static int customPlayerCount = 0;

    private final static int playerCountTime = 60 * 1;

    private final static int timerPlayerCount = 0;
    private Interval interval = new Interval(1);



    // FFA pos: 2000, 2545
    @Override
    public void init(){


        // Disable bullet damage
        float distance;
        int ffax = 150*tilesize;
        int ffay = 225*tilesize;

        int plaguex = 150*tilesize;
        int plaguey = 62*tilesize;

        int campaignx = 230*tilesize;
        int campaigny = 150*tilesize;

        int assaultx = 65*tilesize;
        int assaulty = 150*tilesize;

        for (BulletType b : content.bullets()){
            b.damage = 0;
            b.splashDamage = 0;
        }

        try { // CREDIT TO QUEZLER FOR SHOWING ME THE RELEVANT CODE TO EDIT
            Field f = net.getClass().getDeclaredField("provider");
            f.setAccessible(true);
            Net.NetProvider prov = (Net.NetProvider ) f.get(net);
            Field f1 = prov.getClass().getDeclaredField("server");
            f1.setAccessible(true);
            Server ser = (Server) f1.get(prov);
            ser.setDiscoveryHandler((address, handler2) -> {
                ByteBuffer buffer = customWriteServerData();
                buffer.position(0);
                handler2.respond(buffer);
            });
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        // Initialise player count
        updatePlayerCount();

        // Disable building
        netServer.admins.addActionFilter((action) -> {
            return false;
        });

        Events.on(EventType.Trigger.class, event -> {

            for (Player player : Groups.player) {

                // plague
                if(serversUp[1] && Math.sqrt(Math.pow(player.x - plaguex, 2)+ Math.pow(player.y - plaguey, 2)) < 100){
                    Call.connect(player.con, "aamindustry.play.ai", 6571);
                }

                // assimilation
                if(serversUp[0] && Math.sqrt(Math.pow(player.x - ffax, 2)+ Math.pow(player.y - ffay, 2)) < 140){
                    Call.connect(player.con, "aamindustry.play.ai", 6572);
                }

                // campaign
                if(serversUp[2] && Math.sqrt(Math.pow(player.x - campaignx, 2)+ Math.pow(player.y - campaigny, 2)) < 100){
                    Call.connect(player.con, "aamindustry.play.ai", 6573);
                }

                if(serversUp[2] && Math.sqrt(Math.pow(player.x - assaultx, 2)+ Math.pow(player.y - assaulty, 2)) < 100){
                    Call.connect(player.con, "aamindustry.play.ai", 6573);
                }

            }

            // Refresh server player count every second and check server status
            if (interval.get(timerPlayerCount, playerCountTime)){
                updatePlayerCount();
                updateServerStatus();


            }

        });

        Events.on(PlayerJoin.class, event -> {
            updateServerStatus();
        });


    }

    @Override
    public void registerClientCommands(CommandHandler handler){

        handler.<Player>register("getpos", "Get (x,y)", (args, player) -> {
            player.sendMessage("(" + player.x + ", " + player.y + ")");
        });
    }


    private void updatePlayerCount(){
        if(LocalDate.now().getMonthValue() == 10 && LocalDate.now().getDayOfMonth() == 31){
            customPlayerCount = 666;
            return;
        }
        if(LocalDate.now().getMonthValue() == 4 && LocalDate.now().getDayOfMonth() == 1){
            customPlayerCount = 69;
            return;
        }

        customPlayerCount = Groups.player.size();
        net.pingHost("aamindustry.play.ai", 6571, this::addCount, e -> {});
        net.pingHost("aamindustry.play.ai", 6572, this::addCount, e -> {});
        net.pingHost("aamindustry.play.ai", 6573, this::addCount, e -> {});
        net.pingHost("aamindustry.play.ai", 6574, this::addCount, e -> {});
    }

    private void addCount(Host host){
        customPlayerCount += host.players;
    }

    private void updateServerStatus(){

        net.pingHost("aamindustry.play.ai", 6571, host ->{ // Plague
            serversUp[1] = true;
            Call.label("[gold]" + host.players + "[white] players",
                    1f, 150*tilesize, 86*tilesize);
        }, e ->{
            serversUp[1] = false;
            Call.label("[gray]Server offline",
                    1f, 150*tilesize, 86*tilesize);
        });

        net.pingHost("aamindustry.play.ai", 6572, host ->{ // Assimilation
            serversUp[0] = true;
            Call.label("[gold]" + host.players + "[white] players",
                    1f, 150*tilesize, 212*tilesize);
        }, e ->{
            serversUp[0] = false;
            Call.label("[gray]Server offline",
                    1f, 150*tilesize, 212*tilesize);
        });

        net.pingHost("aamindustry.play.ai", 6573, host ->{ // Campaign
            serversUp[2] = true;
            Call.label("[gold]" + host.players + "[white] players",
                    1f, 235*tilesize, 155*tilesize);
        }, e ->{
            serversUp[2] = false;
            Call.label("[gray]Server offline",
                    1f, 235*tilesize, 155*tilesize);
        });
        net.pingHost("aamindustry.play.ai", 6573, host ->{ // Campaign
            serversUp[2] = true;
            Call.label("[gold]" + host.players + "[white] players",
                    1f, 65*tilesize, 160*tilesize);
        }, e ->{
            serversUp[2] = false;
            Call.label("[gray]Server offline",
                    1f, 65*tilesize, 160*tilesize);
        });
    }

    public static ByteBuffer customWriteServerData(){
        String name = (headless ? Administration.Config.name.string() : player.name);
        String description = headless && !Administration.Config.desc.string().equals("off") ? Administration.Config.desc.string() : "";
        String map = state.map == null ? "None" : state.map.name();

        ByteBuffer buffer = ByteBuffer.allocate(512);

        writeString(buffer, name, 100);
        writeString(buffer, map, 32);

        buffer.putInt(customPlayerCount);
        buffer.putInt(state.wave);
        buffer.putInt(Version.build);
        writeString(buffer, Version.type, 32);

        buffer.put((byte) Gamemode.survival.ordinal());
        buffer.putInt(netServer.admins.getPlayerLimit());

        writeString(buffer, description, 100);
        return buffer;
    }

    private static void writeString(ByteBuffer buffer, String string, int maxlen){
        byte[] bytes = string.getBytes(charset);
        //todo truncating this way may lead to weird encoding errors at the ends of strings...
        if(bytes.length > maxlen){
            bytes = Arrays.copyOfRange(bytes, 0, maxlen);
        }

        buffer.put((byte)bytes.length);
        buffer.put(bytes);
    }



}
