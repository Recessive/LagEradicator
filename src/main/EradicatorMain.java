package main;

import arc.*;
import arc.graphics.Color;
import arc.net.Server;
import arc.struct.Seq;
import arc.util.*;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.core.Version;
import mindustry.entities.Effect;
import mindustry.entities.bullet.BulletType;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.*;
import mindustry.net.Net;
import mindustry.type.ItemStack;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

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

public class EradicatorMain extends Plugin {

    private Seq<ItemStack> monoItems;

    private float realTime = 0f;
    private int seconds;
    private static long startTime = System.currentTimeMillis();

    private RTInterval planClearInterval = new RTInterval(10),
                        lagMessageInterval = new RTInterval(120);

    private String lagMessage = "[scarlet]LOW TPS DETECTED ([gold]<15[scarlet])\n" +
            "[blue]Clearing all poly build plans to reduce lag!";

    public void init(){


        // Reduce lag by clearing build plans
        Events.on(EventType.Trigger.class, event ->{
            if(planClearInterval.get(seconds)){
                if(Core.graphics.getFramesPerSecond() < 15){
                    for(Teams.TeamData t : state.teams.getActive()){
                        t.blocks.clear();
                    }
                    if(lagMessageInterval.get(seconds)){
                        Call.sendMessage(lagMessage);
                    }
                }
            }

            realTime = System.currentTimeMillis() - startTime;
            seconds = (int) (realTime / 1000);
        });

        monoItems = ItemStack.list(
                Items.copper, 300,
                Items.lead, 300);

        Events.on(EventType.UnitDestroyEvent.class, event ->{
            if(event.unit.type == UnitTypes.mono) {
                if(event.unit.team.core() == null){
                    return;
                }
                // Let players know they got resources
                for(Player player: Groups.player){
                    if(player.team() == event.unit.team()){
                        Call.label(player.con, "\uF838+" + monoItems.get(0).amount + "\n\uF837+" + monoItems.get(1).amount,
                                5f, event.unit.tileX() * 8, event.unit.tileY() * 8);
                    }
                }



                Tile tile = event.unit.team.core().tile;
                for (ItemStack stack : monoItems) {
                    Call.transferItemTo(null, stack.item, stack.amount, tile.drawx(), tile.drawy(), tile.build);
                }
            }
        });

        Events.on(EventType.UnitCreateEvent.class, event -> {
            if(event.unit.type == UnitTypes.mono) {
                event.unit.health = 0;
                event.unit.dead = true;
            }
        });

    }
}
