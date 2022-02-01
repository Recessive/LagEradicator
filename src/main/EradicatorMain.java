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

    private Seq<ItemStack> monoItems = ItemStack.list(
            Items.copper, 300,
                   Items.lead, 300);

    public void init(){

        Events.on(EventType.UnitCreateEvent.class, event -> {
            if(event.unit.type == UnitTypes.mono) {
                event.unit.health = 0;


                // Let players know they got resources
                Call.label("\uF838+" + monoItems.get(0) + "\n+" + monoItems.get(1) + "\uF837", 5, event.unit.x, event.unit.y);

                Tile tile = event.unit.team.core().tile;

                for (ItemStack stack : monoItems) {
                    Call.transferItemTo(null, stack.item, stack.amount, tile.drawx(), tile.drawy(), tile.build);
                }
            }
        });

    }
}
