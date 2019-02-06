package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Slider extends OnePinWidget {

    public boolean sendOnReleaseOn;

    public int frequency;

    public volatile int maximumFractionDigits;

    public boolean showValueOn = true;

    @Override
    public PinMode getModeType() {
        return PinMode.out;
    }

    @Override
    public boolean setProperty(WidgetProperty property, String propertyValue) {
        if (property == WidgetProperty.FRACTION) {
            this.maximumFractionDigits = Integer.parseInt(propertyValue);
            return true;
        }
        return super.setProperty(property, propertyValue);
    }
}
