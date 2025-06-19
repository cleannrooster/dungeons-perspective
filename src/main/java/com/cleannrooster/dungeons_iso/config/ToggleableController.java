package com.cleannrooster.dungeons_iso.config;

import dev.isxander.yacl3.api.Binding;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.StateManager;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.TickBoxController;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ToggleableController<T> implements Controller<T> {
    public final Option<Boolean> enabled;
    public final Controller<T> inner;
    private final Option<T> option;
    private final Supplier<Text> tickBoxTooltipFunction;

    public ToggleableController(
            Option<T> option,
            Function<Option<T>, ControllerBuilder<T>> inner,
            Binding<Boolean> availableBinding,
            Supplier<Text> tickBoxTooltipFunction
    ) {
        this.option = option;
        this.inner = inner.apply(option).build();
        this.tickBoxTooltipFunction = tickBoxTooltipFunction;

        this.enabled = Option
                .<Boolean>createBuilder()
                .name(Text.empty())
                .stateManager(StateManager.createInstant(availableBinding))
                .controller(TickBoxControllerBuilder::create)
                .addListener((opt, event) -> this.option.setAvailable(opt.pendingValue()))
                .build();
    }

    @Override
    public Option<T> option() {
        return this.option;
    }

    @Override
    public Text formatValue() {
        return null;
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new ToggleableControllerWidget<>(this, screen, widgetDimension);
    }

    public static class ToggleableControllerWidget<T> extends AbstractWidget implements ParentElement {
        private final ToggleableController<T> control;
        private final TickBoxController.TickBoxControllerElement tickBox;
        private final AbstractWidget inner;
/*
        private final TooltipState tooltip = new TooltipState();
*/

        public ToggleableControllerWidget(ToggleableController<T> control, YACLScreen screen, Dimension<Integer> dim) {
            super(dim);
            this.control = control;
            this.tickBox = (TickBoxController.TickBoxControllerElement) control.enabled
                    .controller()
                    .provideWidget(screen, dim);
            this.inner = control.inner.provideWidget(screen, dim);
            this.setDimension(dim);
        }

        @Override
        public void render(DrawContext graphics, int mouseX, int mouseY, float delta) {
            this.tickBox.render(graphics, mouseX, mouseY, delta);
            this.inner.render(graphics, mouseX, mouseY, delta);
    /*        this.tooltip.setTooltip(Tooltip.of(this.control.tickBoxTooltipFunction.get()));
            this.tooltip.render(
                    this.tickBox.isMouseOver(mouseX, mouseY),
                    this.tickBox.isFocused(),
                    this.tickBox.getNavigationFocus()
            );*/
        }

        @Override
        public List<? extends Element> children() {
            return List.of(this.tickBox, this.inner);
        }

        @Override
        public boolean isDragging() {
            return false;
        }

        @Override
        public void setDragging(boolean dragging) {

        }

        @Override
        public @Nullable Element getFocused() {
            if (this.tickBox.isFocused()) {
                return tickBox;
            } else if (this.inner.isFocused()) {
                return inner;
            }
            return null;
        }

        @Override
        public void setFocused(@Nullable Element focused) {
            if (focused instanceof AbstractWidget) {
                focused.setFocused(true);
            }
        }

        @Override
        public boolean canReset() {
            return this.tickBox.canReset() || this.inner.canReset();
        }

        @Override
        public void setDimension(Dimension<Integer> dim) {
            super.setDimension(dim);
            this.tickBox.setDimension(Dimension.ofInt(dim.x(), dim.y(),
                    // Square
                    dim.height(), dim.height()
            ));
            this.inner.setDimension(Dimension.ofInt(
                    dim.x() + dim.height(),
                    dim.y(),
                    dim.width() - dim.height(),
                    dim.height()
            ));
        }

        @Override
        public void unfocus() {
            this.tickBox.unfocus();
            this.inner.unfocus();
        }

        @Override
        public boolean matchesSearch(String query) {
            return this.inner.matchesSearch(query);
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {
            this.tickBox.appendNarrations(builder);
            this.inner.appendNarrations(builder);
        }

    }
}
