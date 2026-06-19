package silly.homak.bigitems.test;

import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterial;
import silly.homak.bigitems.interfaces.IBigItem;

public class SmallerItem extends PickaxeItem implements IBigItem {
    public SmallerItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public int[] getScale() {
        return new int[] {3, 1};
    }
}
