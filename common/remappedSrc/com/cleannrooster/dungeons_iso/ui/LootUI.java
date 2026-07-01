package com.cleannrooster.dungeons_iso.ui;

import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.EntityHitResult;

public class LootUI extends BaseOwoScreen<FlowLayout> {
    @Override
    protected @NotNull OwoUIAdapter createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);
        var child = Containers.verticalFlow(Sizing.content(),Sizing.content()).alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER);
        var components = new ArrayList<ParentComponent>();
        components.add((
                Containers.horizontalFlow(Sizing.content(),Sizing.content()).child(Components.label(Component.translatable("Nearby Items")))
                        .surface(Surface.panelWithInset(2))
                        .alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER)
                        .padding(Insets.of(10))));
        if(Minecraft.getInstance().player != null) {
            List<ItemEntity> itemEntityList = Minecraft.getInstance().player.level().getEntities(EntityTypeTest.forClass(ItemEntity.class),Minecraft.getInstance().player.getBoundingBox().inflate(16),(entity) ->{
                return Minecraft.getInstance().player.hasLineOfSight(entity);
            });
            int ii = 0;
            var vertChildList = Containers.horizontalFlow(Sizing.content(),Sizing.content()).alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER);
            var horizontalList = new ArrayList<ParentComponent>();
            var verticalList = new ArrayList<io.wispforest.owo.ui.core.Component>();
            for(ItemEntity entity : itemEntityList) {
                if(ii < 8) {

                    verticalList.add(
                            Containers.horizontalFlow(Sizing.content(),Sizing.content())  .child(Components.button(entity.getName(), button -> {
                                ((MinecraftClientAccessor)Minecraft.getInstance()).setLocation(new EntityHitResult(entity,entity.position()));
                                ((MinecraftClientAccessor)Minecraft.getInstance()).setOriginalLocation(Minecraft.getInstance().player.position());

                            })).alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER));


                    ii++;
                }
                else{
                    ii=0;
                    horizontalList.add(Containers.verticalFlow(Sizing.content(),Sizing.content()).children(verticalList).surface(Surface.panelWithInset(2))
                            .alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER)
                            .padding(Insets.of(10))); ;
                    verticalList = new ArrayList<io.wispforest.owo.ui.core.Component>();
                }


            }
            horizontalList.add(Containers.verticalFlow(Sizing.content(),Sizing.content()).children(verticalList).surface(Surface.panelWithInset(2)).padding(Insets.of(10)).alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER)); ;
            ((FlowLayout)vertChildList).children(horizontalList);
            components.add(vertChildList);

        }
        ((FlowLayout)child).children(components);
        rootComponent.child((child));
    }
}
