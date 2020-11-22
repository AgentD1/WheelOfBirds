import java.util.ArrayList;

/**
 * The abstract base class of all Players. Contains all functions for the game to interact with a player.
 * All contained functions are to be overridden and it is to be assumed that the overriding player will block execution for any amount of time.
 */
public abstract class Player {
	/**
	 * The amount of money this player has
	 */
	public int money = 0;
	
	/**
	 * The player's name on singleplayer and username on multiplayer
	 */
	public String name;
	
	// Guessing stuff
	
	/**
	 * Gets whether the player wants to guess a letter, buy a vowel, or guess the whole phrase
	 * @param knownPhrase The phrase with unguessed letters replaced with a box.
	 * @param guesses The player's guessed characters
	 * @return The player's guess preference
	 */
	abstract GuessChoice getGuessChoice(String knownPhrase, ArrayList<Character> guesses);
	
	/**
	 * Gets a player's letter guess
	 * @param guesses The player's guessed letters
	 * @return The player's letter guess
	 */
	abstract char getLetterGuess(ArrayList<Character> guesses);
	
	/**
	 * Gets a player's vowel guess
	 * @param guesses The player's guessed letters
	 * @return The player's vowel guess
	 */
	abstract char getVowelGuess(ArrayList<Character> guesses);
	
	/**
	 * Gets a player's phrase guess
	 * @param guesses The player's guessed letters
	 * @return The player's phrase guess
	 */
	abstract String getPhraseGuess(ArrayList<Character> guesses);
	
	// Spinning
	
	/**
	 * Informs the player that it's time to spin. The player can choose to block until a key is pressed or a network message is received
	 * @return Whether the wheel spin should be instant (false) or take time and print (true)
	 */
	abstract boolean prepareToSpin();
	
	// Resolving actions
	
	/**
	 * Concludes a Buy Vowel transaction by informing the player of how many occurrences of the bought vowel were in the phrase
	 * @param occurrences The number of occurrences of the bought vowel
	 */
	abstract void resolveBuyVowel(int occurrences);
	
	/**
	 * Concludes a Guess Letter transaction by informing the player of how many occurrences of the guessed letter were in the phrase
	 * @param occurrences The number of occurrences of the bought vowel
	 */
	abstract void resolveGuessLetter(int occurrences);
	
	/**
	 * Concludes a Guess Phrase transaction by informing the player of whether their guess was right or not
	 * @param correct Whether the player's previous phrase guess was right
	 */
	abstract void resolveGuessPhrase(boolean correct);
	
	// Printing stuff
	
	/**
	 * Informs the player that it is the start of its turn. The player can choose to block execution until a given action by a user.
	 */
	abstract void onTurnStart();
	
	/**
	 * Informs the player that their wheel spin has completed. The player can choose to block execution until a given action by a user.
	 * @param slice The slice spun by the player
	 */
	abstract void onSpinEnded(Slice slice);
	
	// Bonus round stuff
	
	/**
	 * Informs the player that it's time to spin for the bonus wheel. The player can choose to block until a key is pressed or a network message is received
	 * @return Whether the wheel spin should be instant (false) or take time and print (true)
	 */
	abstract boolean prepareBonusSpin();
	
	/**
	 *
	 * Gets whether the player wants to guess a letter or guess the whole phrase in the bonus round.
	 * @param knownPhrase The phrase with unguessed letters replaced with a box.
	 * @param guesses The player's guessed characters
	 * @param consonantsGuessed The number of consonants the player has guessed
	 * @param vowelsGuessed The number of vowels the player has guessed
	 * @return The player's guess preference
	 */
	abstract GuessChoice getGuessChoiceBonus(String knownPhrase, ArrayList<Character> guesses, int consonantsGuessed, int vowelsGuessed);
}
