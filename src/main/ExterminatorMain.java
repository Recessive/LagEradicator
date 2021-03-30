package main;

import arc.*;
import arc.net.Server;
import arc.struct.Seq;
import arc.util.*;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.core.Version;
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

public class ExterminatorMain extends Plugin {

    private HashMap<String, Integer> teamMonos = new HashMap<>();

    private final static int tenSecondTime = 10;
    private RTInterval tenSecondInterval = new RTInterval(tenSecondTime);

    private float realTime = 0f;
    private int seconds = 0;

    private static long startTime = System.currentTimeMillis();

    public void init(){

        Seq<ItemStack> monoCost = ItemStack.list(Items.silicon, 30, Items.lead, 15);

        Events.on(EventType.UnitCreateEvent.class, event -> {
            if(event.unit.type == UnitTypes.mono){
                String teamName = event.unit.team().name;
                teamMonos.putIfAbsent(teamName, 0);
                int monoCount = teamMonos.get(teamName)+1;
                int monoLimit = 0;
                for(CoreBlock.CoreBuild core : event.unit.team.cores()){
                    if(core.block == Blocks.coreShard){
                        monoLimit += 8;
                    }else if (core.block == Blocks.coreFoundation){
                        monoLimit += 16;
                    }else if (core.block == Blocks.coreNucleus){
                        monoLimit += 24;
                    }
                }

                if (monoCount > monoLimit){
                    Tile tile = event.unit.team().core().tile;
                    for(ItemStack stack : monoCost){
                        Call.transferItemTo(null, stack.item, stack.amount, tile.drawx(), tile.drawy(), tile.build);
                    }
                }else{
                    teamMonos.put(teamName, monoCount);
                }

                event.unit.kill();
                event.unit.dead(true);
            }
        });

        Events.on(EventType.Trigger.class, event -> {

            if (tenSecondInterval.get(seconds)) {
                for (Teams.TeamData team : state.teams.active){
                    if(teamMonos.getOrDefault(team.team.name, 0) > 0){
                        Tile tile = team.core().tile;

                        Seq<ItemStack> monoItems = ItemStack.list(
                                Items.copper, 10*teamMonos.getOrDefault(team.team.name, 0),
                                Items.lead, 5*teamMonos.getOrDefault(team.team.name, 0));

                        for(ItemStack stack : monoItems){
                            Call.transferItemTo(null, stack.item, stack.amount, tile.drawx(), tile.drawy(), tile.build);
                        }

                        Call.label("+" + monoItems.get(0).amount + "\uF838" + monoItems.get(1).amount + "\uF837",
                                1f, tile.drawx(), tile.drawy());
                    }
                }
            }

            realTime = System.currentTimeMillis() - startTime;
            seconds = (int) (realTime / 1000);
        });
    }
}
