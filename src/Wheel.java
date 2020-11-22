import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a physical Wheel. Contains an array of <code>Slice</code> objects and functions to select them.
 */
public class Wheel {
	/**
	 * The array of slices this wheel contains
	 */
	public Slice[] slices;
	
	/**
	 * The main Random object
	 */
	static Random r;
	
	/**
	 * Spins the wheel and returns a <code>Slice</code> as a result
	 * @param player The player who is spinning the wheel
	 * @param in The main scanner
	 * @param blocking Whether the spin should block execution or not
	 * @return The Slice that has been spun
	 */
	public Slice spin(Player player, Scanner in, boolean blocking) {
		if (r == null) {
			r = Main.r;
		}
		
		if (blocking) { // When blocking, it prints and waits. When nonblocking, it shouldn't print or wait.
			System.out.println("Spinning the wheel! (Press enter to skip)");
			
			// Create and start the 2 helper threads
			WheelSpinThread spinThread = new WheelSpinThread(r, this);
			WheelListenForKeyPressThread wheelListenForKeyPressThread = new WheelListenForKeyPressThread(in);
			
			spinThread.start();
			wheelListenForKeyPressThread.start();
			
			// I couldn't figure out a better way to do this. If you read this and know a better way, can you let me know? Thanks
			//noinspection LoopConditionNotUpdatedInsideLoop,StatementWithEmptyBody
			while (spinThread.isAlive() && wheelListenForKeyPressThread.isAlive()) ;
			
			if (wheelListenForKeyPressThread.isAlive()) { // If the wheel spun until it stopped, stop listening for keypresses
				wheelListenForKeyPressThread.stopRequested.set(true);
			} else { // If a keypress happened, request that the wheel stop, then wait until it does.
				spinThread.stopRequested.set(true);
				try {
					spinThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			return spinThread.selectedSlice; // Return the slice it landed on
		} else {
			WheelSpinThread spin = new WheelSpinThread(Main.r, this);
			spin.stopRequested.set(true);
			// Run the thread synchronously (IDEA doesn't like this call because calling .run() on a thread isn't asynchronous and not what most people want
			//noinspection CallToThreadRun
			spin.run();
			return spin.selectedSlice; // Return the slice it landed on
		}
	}
}

/**
 * The thread which spins the wheel.
 */
class WheelSpinThread extends Thread {
	/**
	 * The main random object
	 */
	Random r;
	
	/**
	 * The wheel to spin
	 */
	Wheel wheel;
	
	/**
	 * The slice it started on
	 */
	public Slice selectedSlice = null;
	
	/**
	 * The generated wheel text
	 */
	String[] wheelText;
	
	/**
	 * The Atomic Boolean which indicates if the thread should stop spinning
	 */
	public AtomicBoolean stopRequested = new AtomicBoolean(false);
	
	/**
	 * Constructs a new WheelSpinThread
	 * @param r The main random object. <b>MUST BE THE SAME RANDOM OBJECT AS IS USED EVERYWHERE ELSE!</b> Using a new random object WILL result in a client desync.
	 * @param w The wheel to spin
	 */
	public WheelSpinThread(Random r, Wheel w) {
		this.r = r;
		wheel = w;
	}
	
	/**
	 * Runs the thread. Starts to spin the wheel until it either comes to a stop or stopRequested is set to true.
	 */
	@Override
	public void run() {
		wheelText = wheelAsString(); // Generate the wheel text and start at a random position and speed
		float speed = r.nextFloat() * 2 + 6;
		float i;
		for (i = r.nextFloat() * wheelText[0].length(); speed >= 0; i += speed) {
			drawWheelWithOffset(i % wheelText[0].length()); // Draw the wheel
			if(!stopRequested.get()) System.out.print("\u001b[1000D\u001b[2A"); // Move the cursor up 2 and all the way to the left
			speed -= 0.06f; // Decrease the speed by 0.06 each time (arbitrary value)
			try {
				if (!stopRequested.get()) { // Don't sleep if a stop has been requested
					sleep(100);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		stopRequested.set(false); // drawWheelWithOffset doesn't output when stopRequested is true
		drawWheelWithOffset(i % wheelText[0].length()); // Draw the wheel 1 final time
	}
	
	/**
	 * Draws the wheel object at a given offset
	 * @param offset The offset to draw the wheel from
	 */
	void drawWheelWithOffset(float offset) {
		String text = wheelText[0];
		
		int width = 31;
		
		String endString = "";
		
		// This is a lot of code that just places selects the correct slice and positions the string correctly. BOOOORING
		if ((int) offset + width >= text.length()) {
			if (!stopRequested.get()) { // This isn't required when stopRequested is true because the string won't be printed
				endString = text.substring((int) offset) + text.substring(0, (int) (offset + width) % text.length());
			}
			if (offset + (width / (float) 2) >= text.length()) { // Loop over when we hit the end
				selectedSlice = wheel.slices[countCharacterInString(text.substring(0, (int) (offset + width) % text.length()), '|')];
			} else {
				selectedSlice = wheel.slices[countCharacterInString(text.substring(0, (int) (offset + (width / 2))), '|') % wheel.slices.length];
			}
		} else {
			if (!stopRequested.get()) {
				endString = text.substring((int) offset, (int) offset + width);
			}
			selectedSlice = wheel.slices[countCharacterInString(text.substring(0, (int) (offset + (width / 2))), '|')];
		}
		
		// Print everything to the screen (only if stopRequested is false)
		// This bit isn't coded very well. Proceed with caution.
		if (!stopRequested.get()) {
			int secondPipe = endString.lastIndexOf('|'); // Locate the first and second pipe symbols (which conveniently mark the edge of each slice)
			int firstPipe = endString.indexOf('|');
			
			StringBuilder output = new StringBuilder();
			output.append(endString);
			
			// Count how many pipes are before the current offset. That will tell us which slice we're on
			int locationInArray = countCharacterInString(text.substring(0, (int) (offset) % text.length()), '|') % wheel.slices.length;
			
			// Figure out the colours of each slice (even ones potentially not in view)
			String firstColour = wheel.slices[locationInArray].colour;
			String secondColour = wheel.slices[(locationInArray + 1) % wheel.slices.length].colour;
			String thirdColour = wheel.slices[(locationInArray + 2) % wheel.slices.length].colour;
			
			if (secondPipe != firstPipe) { // secondPipe will be equal to firstPipe if there is only 1 pipe in the string
				output.insert(secondPipe, thirdColour); // There are 3 colours on screen, so insert the 3rd colour
			}
			
			// There will always be at least 1 pipe in the string section, so put the colours where they should be
			output.insert(firstPipe, secondColour);
			output.insert(0, firstColour);
			
			output.append(Main.Reset); // Reset the colouring at the end
			
			System.out.println(output); // Print the output and the pointer
			
			System.out.print(String.join("", Collections.nCopies(width / 2, " ")));
			System.out.println("^");
		}
	}
	
	/**
	 * Counts the number of occurrences of a certain character in a String.
	 * @param s The string to check
	 * @param c The character to count
	 * @return The number of occurrences of the character
	 */
	int countCharacterInString(String s, char c) {
		int count = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == c) {
				count++;
			}
		}
		return count;
	}
	
	/**
	 * Converts the Wheel object into a String, [0] is the colourless string and [1] is the coloured string
	 * @return An array of strings. [0] is the colourless string and [1] is the coloured string
	 */
	String[] wheelAsString() {
		int longest = longestNameLength() + 2;
		
		StringBuilder string = new StringBuilder();
		StringBuilder colouredString = new StringBuilder();
		
		for (int i = 0; i < wheel.slices.length; i++) { // For each wheel slice, centre it correctly with spaces then add it to the string.
			String text = wheel.slices[i].text;
			int longestMinusText = longest - text.length() / 2 - 1;
			int longestMinusTextPlus = longest - text.length() / 2 + text.length() % 2 - 1;
			
			string.append(String.join("", Collections.nCopies(longestMinusText, " "))); // Centre it correctly
			string.append(wheel.slices[i].text);
			string.append(String.join("", Collections.nCopies(longestMinusTextPlus, " "))); // Centre it correctly
			string.append("|");
			string.append('\0'); // I thought I might need a null character. Pipe worked instead. I'm not removing it
			
			colouredString.append(wheel.slices[i].colour); // Also append colour for the coloured string
			colouredString.append(String.join("", Collections.nCopies(longestMinusText, " ")));
			colouredString.append(wheel.slices[i].text);
			colouredString.append(String.join("", Collections.nCopies(longestMinusTextPlus, " ")));
			colouredString.append("|");
			colouredString.append('\0');
		}
		
		return new String[] { string.toString(), colouredString.toString() };
	}
	
	/**
	 * Finds the longest slice text and returns its length
	 * @return The length of the longest slice's name
	 */
	int longestNameLength() {
		int longest = 0;
		for (Slice slice : wheel.slices) {
			if (slice.text.length() > longest) {
				longest = slice.text.length();
			}
		}
		return longest;
	}
}

/**
 * The thread which watches out for an enter keypress. Stops when either the enter key is pressed or the stopRequested boolean is set to true
 */
class WheelListenForKeyPressThread extends Thread {
	Scanner in;
	public AtomicBoolean stopRequested = new AtomicBoolean(false);
	
	/**
	 * Constructs a new WheelListenForKeyPressThread
	 * @param in The main scanner
	 */
	public WheelListenForKeyPressThread(Scanner in) {
		this.in = in;
	}
	
	/**
	 * Runs the thread until either the user hits enter or the stopRequested boolean is set to true
	 */
	@Override
	public void run() {
		while (!stopRequested.get()) {
			try {
				sleep(100); // Only bother checking every 100 milliseconds. A 100ms delay is nothing.
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
			try {
				if (System.in.available() != 0) { // Check if there is a new line. This is the only non-blocking way of doing so.
					in.nextLine(); // It's important not to block because we need to check if stopRequested is true frequently.
					return;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
	}
}