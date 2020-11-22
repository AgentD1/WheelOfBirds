import java.util.ArrayList;

/**
 * A class representing a local AI player. All function documentation can be found in <code>Player</code>
 */
public class AiPlayer extends Player {
	/**
	 * A list of possible consonants
	 */
	char[] consonants = "bcdfghjklmnpqrstvwxyz".toCharArray();
	
	/**
	 * The last known phrase
	 */
	String lastKnownPhrase;
	
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
	boolean bonusRound;
	
	/**
	 * The last known number of guessed consonants
	 */
	int lastKnownConsonants;
	
	/**
	 * The last known number of guessed vowels
	 */
	int lastKnownVowels;
	
	/**
	 * Constructs a new AI Player
	 * @param name The player's name
	 */
	public AiPlayer(String name) {
		this.name = name;
	}
	
	@Override
	GuessChoice getGuessChoice(String knownPhrase, ArrayList<Character> guesses) {
		lastKnownPhrase = knownPhrase;
		bonusRound = false;
		
		if (guesses.size() < 4 && money > 500) { // Some decent but not too good AI logic
			return GuessChoice.BUYVOWEL;
		} else if (guesses.size() > 16 && !guesses.containsAll(Main.vowels)) {
			return GuessChoice.GUESSPHRASE;
		} else {
			return GuessChoice.GUESSLETTER;
		}
	}
	
	@Override
	char getLetterGuess(ArrayList<Character> guesses) {
		if (!bonusRound || lastKnownVowels != 0) { // Pick a random consonant
			while (true) {
				int guessIndex = Main.r.nextInt(consonants.length);
				if (!guesses.contains(consonants[guessIndex])) {
					return lastGuess = consonants[guessIndex];
				}
			}
		} else { // Pick a random vowel
			while (true) {
				int guessIndex = Main.r.nextInt(Main.vowels.size());
				if (!guesses.contains(Main.vowels.get(guessIndex))) {
					return lastGuess = Main.vowels.get(guessIndex);
				}
			}
		}
	}
	
	@Override
	char getVowelGuess(ArrayList<Character> guesses) {
		while (true) { // Pick a random vowel
			int guessIndex = Main.r.nextInt(Main.vowels.size());
			if (!guesses.contains(Main.vowels.get(guessIndex))) {
				return lastGuess = Main.vowels.get(guessIndex);
			}
		}
	}
	
	@Override
	String getPhraseGuess(ArrayList<Character> guesses) {
		ArrayList<String> possibleGuesses = new ArrayList<>(); // Go through all phrases and pick the ones that work with the current known phrase
		for (String bird : Main.phrases) {
			if (guessesCompatible(lastKnownPhrase, bird)) {
				possibleGuesses.add(bird);
			}
		}
		
		if (possibleGuesses.size() != 0) { // Pick randomly from the valid guesses
			return lastPhraseGuess = possibleGuesses.get(Main.r.nextInt(possibleGuesses.size()));
		} else {
			return lastPhraseGuess = "Why are we here just to suffer"; // This should never happen but if it does it's obvious
		}
	}
	
	/**
	 * Checks if a guess is compatible with the phrase
	 * @param guess The possible guess
	 * @param phrase The known phrase
	 * @return Whether the guess is compatible (true) or not (false)
	 */
	boolean guessesCompatible(String guess, String phrase) {
		if (guess.length() != phrase.length()) {
			return false;
		}
		
		int mistakes = 0;
		
		for (int i = 0; i < guess.length(); i++) {
			char guessChar = guess.charAt(i), phraseChar = phrase.charAt(i);
			if (guessChar != '☐' && guessChar != phraseChar) { // Let the AI make mistakes sometimes
				mistakes++;
			}
			if(mistakes > 1) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	boolean prepareToSpin() {
		System.out.println(name + " has $" + money);
		return false; // Don't block or display the entire spin for AI opponents.
	}
	
	@Override
	void resolveBuyVowel(int occurrences) { // Display stuff for the user
		System.out.println(name +" " + (bonusRound ? "guessed" : "bought") + " the vowel " + lastGuess);
		System.out.println(lastGuess + " was in the word " + occurrences + " times.");
	}
	
	@Override
	void resolveGuessLetter(int occurrences) {
		System.out.println(name + " guessed the letter " + lastGuess);
		System.out.println(lastGuess + " was in the word " + occurrences + " times.");
	}
	
	@Override
	void resolveGuessPhrase(boolean correct) {
		System.out.println(name + " guessed the phrase " + lastPhraseGuess);
		System.out.println("They were " + (correct ? "right" : "wrong"));
	}
	
	@Override
	void onTurnStart() {
		System.out.println(Main.BRed + Main.FWhite + name + "'s turn starts" + Main.Reset);
	}
	
	@Override
	void onSpinEnded(Slice slice) {
		System.out.println(name + " spun a " + slice.text + "!");
	}
	
	@Override
	boolean prepareBonusSpin() {
		bonusRound = true;
		System.out.println(name + " is now doing the bonus round!");
		return false; // Don't block
	}
	
	@Override
	GuessChoice getGuessChoiceBonus(String knownPhrase, ArrayList<Character> guesses, int consonants, int vowels) {
		lastKnownConsonants = consonants;
		lastKnownVowels = vowels;
		lastKnownPhrase = knownPhrase;
		
		System.out.println(knownPhrase);
		if (consonants == 3 && vowels == 1) { // Only guess the phrase when all the letter guesses are used up
			return GuessChoice.GUESSPHRASE;
		} else {
			return GuessChoice.GUESSLETTER;
		}
	}
	
	
}
