import java.util.*;

/**
 * A class representing a player on the user's computer which is participating in singleplayer or local multiplayer. All function documentation can be found in <code>Player</code>
 */
public class LocalPlayer extends Player {
	/**
	 * The main scanner
	 */
	Scanner in;
	
	/**
	 * The last character that was guessed
	 */
	char lastGuess;
	
	/**
	 * The last phrase that was guessed
	 */
	String lastPhraseGuess;
	
	/**
	 * Whether the current round is the bonus round
	 */
	boolean bonusRound = false;
	
	/**
	 * The last known number of guessed consonants
	 */
	int lastKnownConsonants;
	
	/**
	 * The last known number of guessed vowels
	 */
	int lastKnownVowels;
	
	/**
	 * Constructs a new LocalPlayer
	 * @param in The main input Scanner
	 * @param name This player's name
	 */
	public LocalPlayer(Scanner in, String name) {
		this.in = in;
		this.name = name;
	}
	
	@Override
	GuessChoice getGuessChoice(String knownPhrase, ArrayList<Character> guesses) {
		System.out.println(knownPhrase);
		Main.displayGuesses(guesses); // Display the phrase, guesses, and amount of money
		Main.displayPlayerStats(this);
		
		
		while (true) { // Ask the player to make a choice until they make one that is valid
			System.out.println("Do you want to guess a (l)etter, the whole (p)hrase, or buy a (v)owel for $250?");
			String request = in.nextLine();
			if (request.equalsIgnoreCase("l") || request.equalsIgnoreCase("letter")) {
				return GuessChoice.GUESSLETTER;
			} else if (request.equalsIgnoreCase("p") || request.equalsIgnoreCase("phrase")) {
				return GuessChoice.GUESSPHRASE;
			} else if (request.equalsIgnoreCase("v") || request.equalsIgnoreCase("vowel")) {
				if(guesses.containsAll(Main.vowels)) {
					System.out.println("You have guessed all the vowels!");
					continue;
				}
				if (money < 250) {
					System.out.println("You can't afford to buy a vowel!");
					continue;
				} else {
					return GuessChoice.BUYVOWEL;
				}
			}
			System.out.println("I don't understand.");
		}
	}
	
	@Override
	char getLetterGuess(ArrayList<Character> guesses) {
		System.out.println("Which letter do you want to guess?");
		
		if(!bonusRound) { // Ask the player for a letter. In the regular round, this can only be a consonant
			return lastGuess = getLetter(in, false, true, guesses);
		} else { // On the bonus round, this might be able to be either, depending on how many consonants and vowels have been guessed
			return lastGuess = getLetter(in, lastKnownVowels != 1, lastKnownConsonants != 3, guesses);
		}
	}
	
	@Override
	char getVowelGuess(ArrayList<Character> guesses) {
		System.out.println("Which vowel do you want to buy?");
		
		// Ask the user for a vowel
		return lastGuess = getLetter(in, true, false, guesses);
	}
	
	@Override
	String getPhraseGuess(ArrayList<Character> guesses) {
		System.out.println("Guess the phrase: ");
		return lastPhraseGuess = in.nextLine(); // Ask the user for a phrase
	}
	
	@Override
	boolean prepareToSpin() {
		bonusRound = false;
		System.out.println("Spin the wheel!"); // Wait for the user to hit enter before spinning
		in.nextLine();
		return true; // The wheel spin should block and display
	}
	
	@Override
	void resolveBuyVowel(int occurrences) { // Display the number of occurrences of the last vowel bought to the user
		if (occurrences > 0) {
			System.out.println(lastGuess + " is in the phrase " + occurrences + " times!");
		} else {
			System.out.println("There is no letter " + lastGuess + " in the phrase!");
		}
	}
	
	@Override
	void resolveGuessLetter(int occurrences) { // Display the number of occurrences of the last character guessed to the user
		if (occurrences > 0) {
			System.out.println(lastGuess + " is in the phrase " + occurrences + " times!");
		} else {
			System.out.println("There is no letter " + lastGuess + " in the phrase!");
		}
	}
	
	@Override
	void resolveGuessPhrase(boolean correct) { // Display whether the guessed phrase was right or not to the user
		if (correct) {
			System.out.println("Congrats! The word was " + lastPhraseGuess);
		} else {
			System.out.println("That's not it!");
		}
	}
	
	@Override
	void onTurnStart() { // Tell the player it is now their turn
		if(name.equals("You")) {
			System.out.println(Main.BGreen + "It's your turn!" + Main.Reset);
		} else {
			System.out.println(Main.BGreen + "It's " + name + "'s turn!" + Main.Reset);
		}
	}
	
	@Override
	void onSpinEnded(Slice slice) {
		System.out.println("You spun a " + slice.text + "!"); // Tell the user what they spun
	}
	
	@Override
	boolean prepareBonusSpin() {
		bonusRound = true; // Tell the user the rules of the bonus round
		System.out.println("Welcome to the bonus round! The letters R S T L N and E have already been revealed.");
		System.out.println("You can guess 3 consonants and 1 vowel. You only get 1 attempt to guess the phrase.");
		System.out.println("Spin the bonus wheel to begin! (Press enter)");
		
		in.nextLine(); // Wait for the user to press enter before spinning
		
		return true; // The wheel spin should block and display
	}
	
	@Override
	GuessChoice getGuessChoiceBonus(String knownPhrase, ArrayList<Character> guesses, int consonants, int vowels) {
		lastKnownConsonants = consonants; // "Remember" the consonants, vowels, and the phrase
		lastKnownVowels = vowels;
		lastPhraseGuess = knownPhrase;
		
		System.out.println(knownPhrase); // Print everything of worth
		Main.displayGuesses(guesses);
		Main.displayPlayerStats(this);
		
		System.out.println("You have " + (3 - consonants) + " consonants left and " + (1 - vowels) + " vowels left"); // Tell the user how many letters they have left
		
		while (true) { // Ask the user to pick letter or phrase until they pick something valid
			System.out.println("Do you want to guess a (l)etter or the whole (p)hrase?");
			String request = in.nextLine();
			if (request.equalsIgnoreCase("l") || request.equalsIgnoreCase("letter")) {
				if (consonants == 3 && vowels == 1) {
					System.out.println("You're all out of guesses!");
				} else {
					return GuessChoice.GUESSLETTER;
				}
			} else if (request.equalsIgnoreCase("p") || request.equalsIgnoreCase("phrase")) {
				return GuessChoice.GUESSPHRASE;
			}
			System.out.println("I don't understand.");
		}
	}
	
	/**
	 * Ask the user for a letter with the specified restrictions
	 * @param in The main input Scanner
	 * @param allowVowels Whether vowels should be allowed
	 * @param allowConsonants Whether consonants should be allowed
	 * @param guesses The letters that can't be chosen
	 * @return The chosen character
	 */
	public static char getLetter(Scanner in, boolean allowVowels, boolean allowConsonants, List<Character> guesses) {
		char guess;
		while (true) {
			try {
				guess = Character.toLowerCase(in.nextLine().charAt(0));
			} catch (Exception e) {
				continue;
			}
			if (!Character.isAlphabetic(guess)) {
				System.out.println("Enter a " + Main.Underline + "letter" + Main.Reset);
			} else if (guesses.contains(guess)) {
				System.out.println("You've already guessed that letter");
			} else if (!allowVowels && Main.vowels.contains(guess)) {
				System.out.println("Enter a consonant please");
			} else if (!allowConsonants && !Main.vowels.contains(guess)) {
				System.out.println("Enter a vowel please");
			} else {
				break;
			}
		}
		
		return guess;
	}
}
