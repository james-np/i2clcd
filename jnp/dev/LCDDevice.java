package jnp.dev;

import java.io.IOException;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;
/**
 * This is a class written to control a I2C LCD 2004 display
 * 
 * Much of the code is transcribed to Java from C code found here:
 * http://wiki.sunfounder.cc/index.php?title=I2C_LCD2004 
 * and Python code found here: 
 * https://www.recantha.co.uk/blog/?p=4849
 */
@SuppressWarnings("unused")
public class LCDDevice {

	// commands
	private final int LCD_CLEARDISPLAY = 0x01, LCD_RETURNHOME = 0x02, LCD_ENTRYMODESET = 0x04, LCD_DISPLAYCONTROL = 0x08,
			LCD_CURSORSHIFT = 0x10, LCD_FUNCTIONSET = 0x20, LCD_SETCGRAMADDR = 0x40, LCD_SETDDRAMADDR = 0x80;

	//flags for display entry mode
	private final int LCD_ENTRYRIGHT = 0x00, LCD_ENTRYLEFT = 0x02, LCD_ENTRYSHIFTINCREMENT = 0x01,
			LCD_ENTRYSHIFTDECREMENT = 0x00;

	//flags for display on/off control
	private final int LCD_DISPLAYON = 0x04, LCD_DISPLAYOFF = 0x00, LCD_CURSORON = 0x02, LCD_CURSOROFF = 0x00,
			LCD_BLINKON = 0x01, LCD_BLINKOFF = 0x00;

	//flags for display/cursor shift
	private final int LCD_DISPLAYMOVE = 0x08, LCD_CURSORMOVE = 0x00, LCD_MOVERIGHT = 0x04, LCD_MOVELEFT = 0x00;

	//flags for function set
	private final int LCD_8BITMODE = 0x10, LCD_4BITMODE = 0x00, LCD_2LINE = 0x08, LCD_1LINE = 0x00, LCD_5x10DOTS = 0x04,
			LCD_5x8DOTS = 0x00;

	// flags for backlight control
	private int LCD_BACKLIGHT = 0x08, LCD_NOBACKLIGHT = 0x00;

	private int En = 0b00000100; // Enable bit
	private int Rw = 0b00000010; // Read/Write bit
	private int Rs = 0b00000001;// Register select bit

	private static I2CDevice device;

	/**
	 * This constructor initializes the LCD screen and prepares it to be written to
	 * @param bus bus number to use
	 * @param port address to use
	 * @throws IOException
	 * @throws UnsupportedBusNumberException
	 * @throws InterruptedException
	 */
	public LCDDevice(int bus, int port) throws IOException, UnsupportedBusNumberException, InterruptedException {
		device = I2CFactory.getInstance(bus).getDevice(port);
		write(0x03);
		write(0x03);
		write(0x03);
		write(0x02);

		write(LCD_FUNCTIONSET | LCD_2LINE | LCD_5x8DOTS | LCD_4BITMODE);
		Thread.sleep(20);
		write(LCD_DISPLAYCONTROL /*| LCD_CURSORON*/ | LCD_DISPLAYON);
		write(LCD_CLEARDISPLAY);
		Thread.sleep(20);
		write(LCD_ENTRYMODESET | LCD_ENTRYLEFT);
	}
	/**
	 * This is the default constructor.  The values loaded here are for the Raspberry Pi3.
	 * Use the other constructor if the bus configuration is different on your device.
	 * 
	 * @throws IOException
	 * @throws UnsupportedBusNumberException
	 * @throws InterruptedException
	 */
	public LCDDevice() throws IOException, UnsupportedBusNumberException, InterruptedException
	{
		this(I2CBus.BUS_1,0x27);
	}
	

	/**
	 * Writes a string with a starting position on the screen
	 * @param string String to write
	 * @param line Line to write on
	 * @param pos Position within that line to start
	 * @throws InterruptedException
	 */
	public void displayStringPos(String string, int line, int pos) throws InterruptedException {
		int positionNew = getRowOffset(line) + pos;

		write(LCD_SETDDRAMADDR + positionNew);
		for (char c : string.toCharArray()) {
			writeChar(c);
		}
	}

	
	public void strobe(int data) throws InterruptedException {
		writeCmd(data | En | LCD_BACKLIGHT);
		Thread.sleep(5);
		writeCmd(((data & ~En) | LCD_BACKLIGHT));
		Thread.sleep(1);

	}

	/**
	 * writes a character to the screen
	 * @param charvalue Integer value of the character to write to the screen
	 * @throws InterruptedException
	 */
	public void writeChar(int charvalue) throws InterruptedException {
		write(charvalue,Rs);
	}

	/**
	 * Writes a command to the display
	 * @param cmd command to send to the screen
	 * @throws InterruptedException
	 */
	public void write(int cmd) throws InterruptedException {
		write(cmd, 0);
	}

	/**
	 * depending on mode a command or a character will be written to the device.
	 * see writeChar() and write(cmd)
	 * @param data Data to send to the device
	 * @param mode Either command mode or dats mode
	 * @throws InterruptedException
	 */
	public void write(int data, int mode) throws InterruptedException {
		writeFourBits(mode | (data & 0xF0));
		writeFourBits(mode | ((data << 4) & 0xF0));
	}

	/**
	 * Converts and writes the data in four bit mode
	 * @param data Data to be written to the device
	 * @throws InterruptedException
	 */
	public void writeFourBits(int data) throws InterruptedException {
		writeCmd(data | LCD_BACKLIGHT);
		strobe(data);
	}

	/**
	 * Clears the screen and returns the cursor to the home location
	 * @throws InterruptedException
	 */
	public void clear() throws InterruptedException {
		write(LCD_CLEARDISPLAY);
		write(LCD_RETURNHOME);
	}

	/**
	 * Turn backlight on or off
	 * @param state true - turn light on, false - turn light off
	 * @throws InterruptedException
	 */
	public void backlight(boolean state) throws InterruptedException {
		if (state) {
			writeCmd(LCD_BACKLIGHT);
		} else {
			writeCmd(LCD_NOBACKLIGHT);
		}
	}

	/**
	 * Scroll all items on the display one cell to the left
	 * @throws InterruptedException
	 */
	public void scrollLeft() throws InterruptedException {
		write(LCD_CURSORSHIFT | LCD_DISPLAYMOVE | LCD_MOVELEFT);
	}

	/**
	 * Scroll all items on the display one cell to the right
	 * @throws InterruptedException
	 */
	public void scrollRight() throws InterruptedException {
		write(LCD_CURSORSHIFT | LCD_DISPLAYMOVE | LCD_MOVERIGHT);
	}

	/**
	 * After all the formatting and command encoding send data to the device
	 * Here is the direct pipe to the device.
	 * @param cmd byte command to send to the device
	 * @throws InterruptedException
	 */
	public void writeCmd(int cmd) throws InterruptedException {
		try {
			device.write((byte) cmd);
			Thread.sleep(1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method loads in an array for a user defined character.  The character is
	 * 5 columns by 8 rows and is created using the bitmask of the array values
	 * @param character an integer array of line values for the character
	 * @param memLoc the character map location to store the character in (0 - 7)
	 * @see <a href="http://www.8051projects.net/lcd-interfacing/lcd-custom-character.php" target="_TOP">Creating CGRAM</a> 
	 * @throws InterruptedException
	 */
	public void loadCustomChar(int[] character, int mapLoc) throws InterruptedException {
		
		//we have 8 RAM locations to store our custom characters in
		//0 - 7.   These character map locations can be accessed by writing their
		//map location later on
		//i.e. writeChar(0x00) will display the character stored at mapLoc 0x00
		
		mapLoc &= 0x7; // incase a number larger than 7 was entered use a bitwise and to cut it down <= 7
		write(LCD_SETCGRAMADDR | ((mapLoc << 3)));
		for (int line : character) {
			writeChar(line);
		}

	}

	/**
	 * This a quick loader of multiple icons.  It starts at map location 0
	 * and loads each icon into consecutive map locations
	 * @param icons - array  of icons
	 * @throws InterruptedException
	 */
	public void loadCustomChars(int[][] icons) throws InterruptedException {
		write(LCD_SETCGRAMADDR);
		for (int[] character : icons) {
			for (int line : character) {
				writeChar(line);
			}
		}
	}

	/**
	 *  Moves the cursor to a specific location.  Does not write anything to the 
	 *  screen
	 * @param row Row to move cursor
	 * @param pos Position within that row to move cursor
	 * @throws InterruptedException
	 */
	public void setCursor(int row, int pos) throws InterruptedException {
		write(LCD_SETDDRAMADDR | pos + getRowOffset(row));
	}

	/**
	 * Given a line number get its memory offset
	 * @param line Line number 
	 * @return Memory offset of the given line
	 */
	private int getRowOffset(int line) {
		int offset = 0;
		switch (line) {
		case 1:
			offset = 0;
			break;
		case 2:
			offset = 0x40;
			break;
		case 3:
			offset = 0x14;
			break;
		case 4:
			offset = 0x54;
			break;
		}

		return offset;
	}

	/**
	 * This class contains a list of different icons to load into the LCD
	 * <b>Note<b> dim(2) array icons will take up more than 1 memory location
	 *
	 */
	public static class Icons {
		public final static int[][] SMILEY_FACE = {

				{ 0x00, 0x00, 0x03, 0x04, 0x08, 0x19, 0x11, 0x10 },

				{ 0x00, 0x1F, 0x00, 0x00, 0x00, 0x11, 0x11, 0x00 },

				{ 0x00, 0x00, 0x18, 0x04, 0x02, 0x13, 0x11, 0x01 },

				{ 0x12, 0x13, 0x1b, 0x09, 0x04, 0x03, 0x00, 0x00 },

				{ 0x00, 0x11, 0x1f, 0x1f, 0x0e, 0x00, 0x1F, 0x00 },

				{ 0x09, 0x19, 0x1b, 0x12, 0x04, 0x18, 0x00, 0x00 },

				{ 0x1f, 0x0, 0x4, 0xe, 0x0, 0x1f, 0x1f, 0x1f } };

		public final static int[] bell = { 0x4, 0xe, 0xe, 0xe, 0x1f, 0x0, 0x4 };
		public final static int[] note = { 0x2, 0x3, 0x2, 0xe, 0x1e, 0xc, 0x0 };
		public final static int[] clock = { 0x0, 0xe, 0x15, 0x17, 0x11, 0xe, 0x0 };
		public final static int[] heart = { 0x0, 0xa, 0x1f, 0x1f, 0xe, 0x4, 0x0 };
		public final static int[] duck = { 0x0, 0xc, 0x1d, 0xf, 0xf, 0x6, 0x0 };
		public final static int[] check = { 0x0, 0x1, 0x3, 0x16, 0x1c, 0x8, 0x0 };
		public final static int[] cross = { 0x0, 0x1b, 0xe, 0x4, 0xe, 0x1b, 0x0 };
		public final static int[] retarrow = { 0x1, 0x1, 0x5, 0x9, 0x1f, 0x8, 0x4 };

	}

}
