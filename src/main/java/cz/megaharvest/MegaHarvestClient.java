package cz.megaharvest;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class MegaHarvestClient implements ClientModInitializer {
    public static final String MOD_ID = "megaharvest";

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static KeyMapping harvestKey;

    @Override
    public void onInitializeClient() {
        harvestKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.megaharvest.harvest", InputConstants.Type.KEYSYM, InputConstants.KEY_H, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Jeden stisk = jedna sklizeň. Případné nahromaděné stisky zahodíme,
            // držení klávesy nic neopakuje.
            if (harvestKey.consumeClick()) {
                while (harvestKey.consumeClick()) {
                    // drain
                }
                Harvester.run(client);
            }
        });
    }
}
