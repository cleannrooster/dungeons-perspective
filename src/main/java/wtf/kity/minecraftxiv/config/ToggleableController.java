package wtf.kity.minecraftxiv.config;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.TickBoxController;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ToggleableController<T> implements Controller<Pair<Boolean, T>> {
    private final ToggleableOption<T> option;

    public ToggleableController(ToggleableOption<T> option) {
        this.option = option;
    }

    @Override
    public Option<Pair<Boolean, T>> option() {
        return this.option;
    }

    @Override
    public Text formatValue() {
        return null;
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new ToggleableControllerWidget<>(this, screen, widgetDimension);
//        DirectionalLayoutWidget layout = new DirectionalLayoutWidget(widgetDimension.x(), widgetDimension.y(), DirectionalLayoutWidget.DisplayAxis.HORIZONTAL);
//        this.tickBox = (TickBoxController.TickBoxControllerElement) control.tickBox.controller().provideWidget(screen, Dimension.ofInt(
//                dim.x(),
//                dim.y(),
//                dim.height(),
//                dim.height()
//        ));
//        this.inner = control.inner.controller().provideWidget(screen, Dimension.ofInt(
//                dim.x() + dim.height(),
//                dim.y(),
//                dim.width() - dim.height(),
//                dim.height()
//        ));
    }

    public static class ToggleableControllerWidget<T> extends AbstractWidget implements ParentElement {
        private final TickBoxController.TickBoxControllerElement tickBox;
        private final AbstractWidget inner;

        public ToggleableControllerWidget(ToggleableController<T> control, YACLScreen screen, Dimension<Integer> dim) {
            super(dim);
            this.tickBox = (TickBoxController.TickBoxControllerElement) control.option.enabled.controller().provideWidget(screen, dim);
            this.inner = control.option.inner.controller().provideWidget(screen, dim);
            this.setDimension(dim);
        }

        @Override
        public void render(DrawContext graphics, int mouseX, int mouseY, float delta) {
            this.tickBox.render(graphics, mouseX, mouseY, delta);
            this.inner.render(graphics, mouseX, mouseY, delta);
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
        public void setDimension(Dimension<Integer> dim) {
            super.setDimension(dim);
            this.tickBox.setDimension(Dimension.ofInt(
                    dim.x(),
                    dim.y(),
                    // Square
                    dim.height(),
                    dim.height()
            ));
            this.inner.setDimension(Dimension.ofInt(
                    dim.x() + dim.height(),
                    dim.y(),
                    dim.width() - dim.height(),
                    dim.height()
            ));
        }
    }
}
