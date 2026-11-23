package dev.dtp.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

import dev.dtp.Dtp;

/**
 * Schwarzmarkthändler: Modell des Vanilla-Händlers mit eigener Textur unter
 * {@code assets/dtp/textures/entity/blackmarket_trader.png} (gleiches 64×64-UV-Layout wie der Vanilla-Händler).
 */
public class BlackmarketTraderRenderer extends WanderingTraderRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Dtp.MODID, "textures/entity/blackmarket_trader.png");

    public BlackmarketTraderRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(VillagerRenderState state) {
        return TEXTURE;
    }
}
