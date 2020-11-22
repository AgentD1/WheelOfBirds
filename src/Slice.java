import java.util.function.*;

/**
 * A class representing a slice on the wheel.
 */
public class Slice {
	/**
	 * The text to display on the slice when spinning
	 */
	public String text;
	/**
	 * The ASCII colour code for the slice
	 */
	public String colour;
	/**
	 * The action to perform when the slice is first spun.
	 */
	public Consumer<Player> onSelected;
	/**
	 * The action to perform when the slice is spun and the player guesses a letter correctly.
	 */
	public Consumer<Player> onWon;
	/**
	 * Whether the turn should end after the player guesses regardless of whether their answer is correct. True means the turn ends.
	 */
	public boolean endsTurn;
	
	/**
	 * Constructs a new Slice class.
	 * @param text The text to display on the slice when spinning.
	 * @param colour The ASCII colour code for the slice
	 * @param onSelected The action to perform when the slice is first spun. It is assumed that null means no action is to be taken.
	 * @param onWon The action to perform when the slice is spun and the player guesses a letter correctly. It is assumed that null means no action is to be taken.
	 */
	public Slice(String text, String colour, Consumer<Player> onSelected, Consumer<Player> onWon) {
		this(text, colour, onSelected, onWon, false);
	}
	
	/**
	 * Constructs a new Slice class.
	 * @param text The text to display on the slice when spinning.
	 * @param colour The ASCII colour code for the slice
	 * @param onSelected The action to perform when the slice is first spun. It is assumed that null means no action is to be taken.
	 * @param onWon The action to perform when the slice is spun and the player guesses a letter correctly. It is assumed that null means no action is to be taken.
	 * @param endsTurn Whether the turn should end after the player guesses regardless of whether their answer is correct. True means the turn ends.
	 */
	public Slice(String text, String colour, Consumer<Player> onSelected, Consumer<Player> onWon, boolean endsTurn) {
		this.text = text;
		this.colour = colour;
		
		if (onSelected == null) {
			onSelected = (Player p) -> { };
		}
		this.onSelected = onSelected;
		
		if (onWon == null) {
			onWon = (Player p) -> { };
		}
		this.onWon = onWon;
		
		this.endsTurn = endsTurn;
	}
}
