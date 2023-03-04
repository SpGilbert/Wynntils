/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.players;

import com.mojang.blaze3d.platform.InputConstants;
import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.screens.gearviewer.GearViewerScreen;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.RaycastUtils;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.PLAYERS)
public class GearViewerFeature extends UserFeature {
    @RegisterKeyBind
    private final KeyBind gearViewerKeybind = new KeyBind(
            "View player's gear",
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            InputConstants.Type.MOUSE,
            true,
            this::tryOpenGearViewer);

    private void tryOpenGearViewer() {
        Optional<Player> hitPlayer = RaycastUtils.getHoveredPlayer();
        if (hitPlayer.isEmpty()) return;

        if (!Models.Player.isLocalPlayer(hitPlayer.get())) return;

        McUtils.mc().setScreen(GearViewerScreen.create(hitPlayer.get()));
    }
}