package dev.dtp.guide;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Das DTP-Handbuch: Rechtsklick öffnet die Einführung. Rein clientseitig, der Server tut nichts. */
public class GuideItem extends Item {
    public GuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            // Erst hier geladen — ein dedizierter Server kommt nie in diesen Zweig.
            dev.dtp.client.GuideScreen.open();
        }
        return InteractionResult.SUCCESS;
    }
}
