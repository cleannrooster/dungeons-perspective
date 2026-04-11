package com.cleannrooster.dungeons_iso.ui;

import com.cleannrooster.dungeons_iso.api.MinecraftClientAccessor;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.text.Text;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LootUI extends BaseOwoScreen<FlowLayout> {
    @Override
    protected @NotNull OwoUIAdapter createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    public boolean shouldPause() {
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
                Containers.horizontalFlow(Sizing.content(),Sizing.content()).child(Components.label(Text.translatable("Nearby Items")))
                        .surface(Surface.panelWithInset(2))
                        .alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER)
                        .padding(Insets.of(10))));
        if(MinecraftClient.getInstance().player != null) {
            List<ItemEntity> itemEntityList = MinecraftClient.getInstance().player.getWorld().getEntitiesByType(TypeFilter.instanceOf(ItemEntity.class),MinecraftClient.getInstance().player.getBoundingBox().expand(16),(entity) ->{
                return MinecraftClient.getInstance().player.canSee(entity);
            });
            int ii = 0;
            var vertChildList = Containers.horizontalFlow(Sizing.content(),Sizing.content()).alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER);
            var horizontalList = new ArrayList<ParentComponent>();
            var verticalList = new ArrayList<Component>();
            for(ItemEntity entity : itemEntityList) {
                if(ii < 8) {

                    verticalList.add(
                            Containers.horizontalFlow(Sizing.content(),Sizing.content())  .child(Components.button(entity.getName(), button -> {
                                ((MinecraftClientAccessor)MinecraftClient.getInstance()).setLocation(new EntityHitResult(entity,entity.getPos()));
                                ((MinecraftClientAccessor)MinecraftClient.getInstance()).setOriginalLocation(MinecraftClient.getInstance().player.getPos());

                            })).alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER));


                    ii++;
                }
                else{
                    ii=0;
                    horizontalList.add(Containers.verticalFlow(Sizing.content(),Sizing.content()).children(verticalList).surface(Surface.panelWithInset(2))
                            .alignment(HorizontalAlignment.CENTER,VerticalAlignment.CENTER)
                            .padding(Insets.of(10))); ;
                    verticalList = new ArrayList<Component>();
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
