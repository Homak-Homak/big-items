package silly.homak.bigitems.test;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import silly.homak.bigitems.interfaces.IBigItem;

public class BigItem extends Item implements IBigItem {
    public BigItem(Settings settings) {
        super(settings);
    }

    @Override
    public int[] getScale() {
        return new int[] {6, 3};
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.getItemCooldownManager().set(this, 200);
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
