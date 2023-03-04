/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.inventory;

import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.TypeOverride;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.mc.event.ContainerClickEvent;
import com.wynntils.mc.event.ContainerRenderEvent;
import com.wynntils.mc.event.DropHeldItemEvent;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.INVENTORY)
public class ItemLockFeature extends UserFeature {
    @RegisterKeyBind
    private final KeyBind lockSlotKeyBind =
            new KeyBind("Lock Slot", GLFW.GLFW_KEY_H, true, null, this::tryChangeLockStateOnHoveredSlot);

    @Config(visible = false)
    private final Map<String, Set<Integer>> classSlotLockMap = new HashMap<>();

    @TypeOverride
    private final Type classSlotLockMapType = new TypeToken<HashMap<String, Set<Integer>>>() {}.getType();

    @Config
    public boolean blockAllActionsOnLockedItems = false;

    @Config
    public boolean allowClickOnEmeraldPouchInBlockingMode = true;

    @SubscribeEvent
    public void onContainerRender(ContainerRenderEvent event) {
        AbstractContainerScreen<?> abstractContainerScreen = event.getScreen();

        // Don't render lock on ability tree slots
        if (Models.Container.isAbilityTreeScreen(abstractContainerScreen)) return;

        for (Integer slotId : classSlotLockMap.getOrDefault(Models.Character.getId(), Set.of())) {
            Optional<Slot> lockedSlot = abstractContainerScreen.getMenu().slots.stream()
                    .filter(slot -> slot.container instanceof Inventory && slot.getContainerSlot() == slotId)
                    .findFirst();

            if (lockedSlot.isEmpty()) {
                continue;
            }

            renderLockedSlot(event.getPoseStack(), abstractContainerScreen, lockedSlot.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onInventoryClickEvent(ContainerClickEvent event) {
        // Don't lock ability tree slots
        if (!(McUtils.mc().screen instanceof AbstractContainerScreen<?> abstractContainerScreen)
                || Models.Container.isAbilityTreeScreen(abstractContainerScreen)) return;
        if (!blockAllActionsOnLockedItems && event.getClickType() != ClickType.THROW) return;

        // We have to match slot.index here, because the event slot number is an index as well
        Optional<Slot> slotOptional = abstractContainerScreen.getMenu().slots.stream()
                .filter(slot -> slot.container instanceof Inventory && slot.index == event.getSlotNum())
                .findFirst();

        if (slotOptional.isEmpty()) {
            return;
        }

        // We want to allow opening emerald pouch even if locked
        if (allowClickOnEmeraldPouchInBlockingMode
                && event.getClickType() == ClickType.PICKUP
                && Models.Emerald.isEmeraldPouch(slotOptional.get().getItem())) {
            return;
        }

        if (classSlotLockMap
                .getOrDefault(Models.Character.getId(), Set.of())
                .contains(slotOptional.get().getContainerSlot())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDrop(DropHeldItemEvent event) {
        ItemStack selected = McUtils.inventory().getSelected();
        Optional<Slot> heldItemSlot = McUtils.inventoryMenu().slots.stream()
                .filter(slot -> slot.getItem() == selected)
                .findFirst();
        if (heldItemSlot.isEmpty()) return;

        if (classSlotLockMap
                .getOrDefault(Models.Character.getId(), Set.of())
                .contains(heldItemSlot.get().getContainerSlot())) {
            event.setCanceled(true);
        }
    }

    private void renderLockedSlot(PoseStack poseStack, AbstractContainerScreen<?> containerScreen, Slot lockedSlot) {
        RenderUtils.drawTexturedRect(
                poseStack,
                Texture.ITEM_LOCK.resource(),
                ((containerScreen.leftPos + lockedSlot.x)) + 12,
                ((containerScreen.topPos + lockedSlot.y)) - 4,
                400,
                8,
                8,
                Texture.ITEM_LOCK.width() / 2,
                Texture.ITEM_LOCK.height() / 2);
    }

    private void tryChangeLockStateOnHoveredSlot(Slot hoveredSlot) {
        if (hoveredSlot == null || !(hoveredSlot.container instanceof Inventory)) return;

        classSlotLockMap.putIfAbsent(Models.Character.getId(), new HashSet<>());

        Set<Integer> classSet = classSlotLockMap.get(Models.Character.getId());

        if (classSet.contains(hoveredSlot.getContainerSlot())) {
            classSet.remove(hoveredSlot.getContainerSlot());
        } else {
            classSet.add(hoveredSlot.getContainerSlot());
        }

        Managers.Config.saveConfig();
    }
}