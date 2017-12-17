package cc.blynk.server.core.model.widgets;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.web.product.metafields.MeasurementUnit;
import cc.blynk.server.core.model.widgets.controls.RGB;
import cc.blynk.server.core.model.widgets.controls.TwoAxisJoystick;
import cc.blynk.server.core.model.widgets.outputs.LCD;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.11.17.
 */
public class MultiPinWidgetsToJsonTest {

    @Test
    public void testJoystick() {
        TwoAxisJoystick joystick = new TwoAxisJoystick();
        joystick.dataStreams = new DataStream[] {
                new DataStream(1, (byte) 1, false, false, PinType.VIRTUAL, "value", 0, 250, "label", MeasurementUnit.Celsius),
                new DataStream(2, (byte) 2, false, false, PinType.VIRTUAL, "value2", 0, 250, "label", MeasurementUnit.Celsius)
        };
        assertEquals("[\"value\"]", joystick.getJsonValue());

        joystick.split = true;
        assertEquals("[\"value\",\"value2\"]", joystick.getJsonValue());
    }

    @Test
    public void testRGB() {
        RGB rgb = new RGB();
        rgb.dataStreams = new DataStream[] {
                new DataStream(1, (byte) 1, false, false, PinType.VIRTUAL, "value", 0, 250, "label", MeasurementUnit.Celsius),
                new DataStream(2, (byte) 2, false, false, PinType.VIRTUAL, "value2", 0, 250, "label", MeasurementUnit.Celsius)
        };
        assertEquals("[\"value\"]", rgb.getJsonValue());

        rgb.splitMode = true;
        assertEquals("[\"value\",\"value2\"]", rgb.getJsonValue());
    }

    @Test
    public void testLCD() {
        LCD lcd = new LCD();
        lcd.advancedMode = true;
        lcd.dataStreams = new DataStream[] {
                new DataStream(1, (byte) 1, false, false, PinType.VIRTUAL, "value", 0, 250, "label", MeasurementUnit.Celsius),
                new DataStream(2, (byte) 2, false, false, PinType.VIRTUAL, "value2", 0, 250, "label", MeasurementUnit.Celsius)
        };
        assertEquals("[\"value\"]", lcd.getJsonValue());

        lcd.advancedMode = false;
        assertEquals("[\"value\",\"value2\"]", lcd.getJsonValue());
    }

    @Test
    public void testJoystickMultiValue() {
        TwoAxisJoystick joystick = new TwoAxisJoystick();
        joystick.dataStreams = new DataStream[] {
                new DataStream(1, (byte) 1, false, false, PinType.VIRTUAL, "value\0value2", 0, 250, "label", MeasurementUnit.Celsius)
        };
        assertEquals("[\"value\",\"value2\"]", joystick.getJsonValue());
    }
}
