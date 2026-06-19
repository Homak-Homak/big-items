package silly.homak.bigitems;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silly.homak.bigitems.test.BigItem;
import silly.homak.bigitems.test.ColoredItem;
import silly.homak.bigitems.test.SmallerItem;

public class BigItemsAPI implements ModInitializer {
	public static final String MOD_ID = "big-items";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Item BIG_ITEM;
	public static Item SMALLER_ITEM;
	public static Item COLORED_ITEM;
	@Override
	public void onInitialize() {
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			BIG_ITEM = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "big_item"), new BigItem(new Item.Settings()));
			SMALLER_ITEM = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "smaller_item"), new SmallerItem(ToolMaterials.DIAMOND, new Item.Settings()));
			COLORED_ITEM = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "colored_item"), new ColoredItem(new Item.Settings()));
			ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(BIG_ITEM));
			ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(SMALLER_ITEM));
			ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(COLORED_ITEM));
		}
	}
}