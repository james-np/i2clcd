package jnp.dev;

import java.io.IOException;
import java.util.Arrays;

import jnp.dev.LCDDevice;
import jnp.dev.LCDDevice.Icons;

import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

public class LCDTest {
	public static void main(String... strings) {
		try {
			LCDDevice lcd = new LCDDevice();
			lcd.clear();
			lcd.loadCustomChars(Icons.SMILEY_FACE);
			for (int y = 1; y <= 3; y += 2) {
				for (int x = 1; x <= 16; x += 3) {
					lcd.setCursor(y, x);
					lcd.writeChar(0);
					lcd.writeChar(1);
					lcd.writeChar(2);
					lcd.setCursor(y + 1, x);
					lcd.writeChar(3);
					lcd.writeChar(4);
					lcd.writeChar(5);
				}
		

			Thread.sleep(1000);
			lcd.clear();
			}
			lcd.loadCustomChar(Icons.clock, 0);
			lcd.loadCustomChar(Icons.check, 1);
			lcd.loadCustomChar(Icons.duck, 2);
			lcd.loadCustomChar(Icons.heart, 3);
			lcd.loadCustomChar(Icons.note, 4);
			lcd.loadCustomChar(Icons.retarrow, 5);
			lcd.clear();
			lcd.writeChar(255);
			Thread.sleep(3000);
			lcd.setCursor(3, 3);

			lcd.writeChar(0x00);
			lcd.writeChar(0x01);
			lcd.writeChar(0x02);
			lcd.writeChar(0x03);
			lcd.writeChar(0x04);
			lcd.writeChar(0x05);
			Thread.sleep(3000);

			lcd.clear();
			lcd.displayStringPos("Test Shift", 2, 1);
			Thread.sleep(1000);
			lcd.scrollRight();
			Thread.sleep(300);
			lcd.scrollRight();
			Thread.sleep(300);
			lcd.scrollRight();
			Thread.sleep(300);
			lcd.scrollRight();
			Thread.sleep(300);
			lcd.scrollRight();
			Thread.sleep(300);
			lcd.scrollRight();
			Thread.sleep(4000);
			lcd.clear();
			Thread.sleep(3000);

			lcd.clear();
			lcd.backlight(false);
		} catch (IOException | UnsupportedBusNumberException |InterruptedException e) {
			e.printStackTrace();
		}

	}
}