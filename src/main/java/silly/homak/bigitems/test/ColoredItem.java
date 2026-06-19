package silly.homak.bigitems.test;

import net.minecraft.item.Item;
import silly.homak.bigitems.interfaces.IBigItem;

import java.awt.*;

public class ColoredItem extends Item implements IBigItem {
    public ColoredItem(Settings settings) {
        super(settings);
    }

    @Override
    public int[] getScale() {
        return new int[] {3, 2};
    }

    @Override
    public int getColor() {
        return new Color(244, 195, 61).getRGB();
    }
}
