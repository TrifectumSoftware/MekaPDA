package com.trifectumsoftware.mekapda;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.trifectumsoftware.mekapda.item.MekaPdaItem;
import com.trifectumsoftware.mekapda.network.MekaPdaNetwork;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(MekaPDA.MODID)
public class MekaPDA {

    public static final String MODID = "mekapda";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
          DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredItem<MekaPdaItem> MEKA_PDA = ITEMS.registerItem("meka_pda", MekaPdaItem::new,
          new Item.Properties().stacksTo(1));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("main",
          () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mekapda"))
                .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .icon(() -> new ItemStack(MEKA_PDA.get()))
                .displayItems((parameters, output) -> output.accept(MEKA_PDA.get()))
                .build());

    public MekaPDA(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MekaPdaNetwork.register(modEventBus);
        LOGGER.info("Meka-PDA online");
    }
}
