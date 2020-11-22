/*
Wheel Of Bird by Jacob Parker
This program plays a game of Wheel of Bird with the player.
 */

import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class Main {
	/**
	 * The list of possible phrases
	 */
	public static String[] phrases;
	
	/**
	 * The list of vowels
	 */
	public static List<Character> vowels;
	
	/**
	 * An enum representing the different possible gamemodes. Using this enum is easier than making a bunch of booleans or using an int
	 */
	enum GameMode {
		SinglePlayer,
		LocalMultiplayer,
		OnlineMultiplayer
	}
	
	
	/**
	 * The current gameMode
	 */
	public static GameMode gameMode;
	
	static {
		// Initialize the vowels array (You can't do this up there for some reason)
		vowels = Arrays.asList('a', 'e', 'i', 'o', 'u');
	}
	
	/**
	 * Initializes the game, runs it, then displays the leaderboard
	 *
	 * @param args Command line arguments, unused
	 */
	public static void main(String[] args) {
		// Initialize the scanner, read the phrases, set the gamemode, and get the players
		Scanner in = new Scanner(System.in);
		readPhrasesFromFile("birds.txt");
		
		setGameMode(in);
		Player[] players = getPlayers(in);
		
		// Execute the main game
		doGame(in, players);
		
		// Display the leaderboards
		displayLeaderboard(players);
		
		System.out.println("Press enter to quit");
		in.nextLine();
	}
	
	//region Setup
	
	/**
	 * Gets all the players that will be participating in the game. Does this differently depending on the gamemode.
	 *
	 * @param in The main scanner
	 * @return Returns an array of players
	 */
	public static Player[] getPlayers(Scanner in) {
		ArrayList<Player> players = new ArrayList<>();
		
		if (gameMode == GameMode.SinglePlayer) {
			players.add(new LocalPlayer(in, "You")); // Add the local player to the player list
			System.out.println("How many, if any, AI opponents do you want? (0-4)"); // Ask the player to enter the number of AI opponents
			int selection;
			while (true) {
				try {
					String input = in.nextLine();
					selection = Integer.parseInt(input);
				} catch (Exception e) {
					System.out.println("Choose a number");
					continue;
				}
				if (selection > 4 || selection < 0) {
					System.out.println("Choose a number between 0 and 4");
					continue;
				}
				break;
			}
			for (int i = 0; i < selection; i++) { // Add the opponents to the list of players
				players.add(new AiPlayer("Opponent " + (i + 1)));
			}
		} else if (gameMode == GameMode.LocalMultiplayer) {
			System.out.println("How many people are playing? (2-5)"); // Ask the player to enter the number of players participating
			int playerCount;
			while (true) {
				try {
					String input = in.nextLine();
					playerCount = Integer.parseInt(input);
				} catch (Exception e) {
					System.out.println("Choose a number");
					continue;
				}
				if (playerCount > 5 || playerCount < 2) {
					System.out.println("Choose a number between 2 and 5");
					continue;
				}
				break;
			}
			for (int i = 0; i < playerCount; i++) { // Ask each player to enter their name, then add them to the list of players
				System.out.println("Player " + (i + 1) + ", enter your name");
				players.add(new LocalPlayer(in, in.nextLine()));
			}
			int botCountMax = 5 - playerCount;
			if (botCountMax != 0) { // Limit the max number of participants to 5
				System.out.println("How many, if any, AI opponents do you want? (0-" + botCountMax + ")"); // Ask the player to enter the number of AI opponents
				int selection;
				while (true) {
					try {
						String input = in.nextLine();
						selection = Integer.parseInt(input);
					} catch (Exception e) {
						System.out.println("Choose a number");
						continue;
					}
					if (selection > botCountMax || selection < 0) {
						System.out.println("Choose a number between 0 and " + botCountMax);
						continue;
					}
					break;
				}
				for (int i = 0; i < selection; i++) { // Add each AI opponent to the player list
					players.add(new AiPlayer("Opponent " + (i + 1 + playerCount)));
				}
			}
		} else {
			System.out.println("Do you want to (h)ost or (j)oin?"); // Ask the player if they want to host a game or join a game
			boolean hosting;
			while (true) {
				String option = in.nextLine();
				if (option.equalsIgnoreCase("h") || option.equalsIgnoreCase("host")) {
					hosting = true;
					break;
				} else if (option.equalsIgnoreCase("j") || option.equalsIgnoreCase("join")) {
					hosting = false;
					break;
				}
			}
			
			NetworkManager.in = in;
			
			if (hosting) {
				System.out.println("Starting your server on port 1273"); // Start the server
				NetworkManager.startServer(1273);
				System.out.println("Press enter to start the game when everyone is in");
				in.nextLine(); // Wait for the user to press the enter key, then get the player list from NetworkManager and start the server
				Collections.addAll(players, NetworkManager.getPlayers());
				
				NetworkManager.SendToAllRemotePlayers("0StartTheGame"); // Tell all the connected players that the game is starting
			} else {
				System.out.println("Enter an IP to join:");
				
				NetworkManager.connectToIp(new InetSocketAddress(in.nextLine(), 1273)); // Ask the user to enter an IP, then attempt a connection
				
				Collections.addAll(players, NetworkManager.getPlayers()); // Get the list of players (This function blocks until the host starts the game)
			}
		}
		
		Player[] p = new Player[1]; // Convert the ArrayList to an array
		return players.toArray(p);
	}
	
	//endregion
	
	//region Game
	
	/**
	 * Starts the game and runs 3 rounds plus a bonus round
	 *
	 * @param in      The main scanner
	 * @param players The list of players
	 */
	public static void doGame(Scanner in, Player[] players) {
		Wheel wheel = new Wheel();
		
		//region Wheel Initialization
		wheel.slices = new Slice[] {
				new Slice("$900", BYellow, null, (Player p) -> p.money += 900),
				new Slice("$700", BRed, null, (Player p) -> p.money += 700),
				new Slice("$1000", BPurple, null, (Player p) -> p.money += 1000),
				new Slice("$650", BYellow, null, (Player p) -> p.money += 650),
				new Slice("$800", BPurple, null, (Player p) -> p.money += 800),
				new Slice("$700", BYellow, null, (Player p) -> p.money += 700),
				new Slice("BANKRUPT", BBlack, (Player p) -> p.money = 0, null, true),
				new Slice("$600", BRed, null, (Player p) -> p.money += 600),
				new Slice("$550", BCyan, null, (Player p) -> p.money += 550),
				new Slice("$800", BGreen, null, (Player p) -> p.money += 800),
				new Slice("$600", BMagenta, null, (Player p) -> p.money += 600),
				new Slice("BANKRUPT", BBlack, (Player p) -> p.money = 0, null, true),
				new Slice("$650", BPurple, null, (Player p) -> p.money += 650),
				new Slice("$1200", BCyan, null, (Player p) -> p.money += 1200),
				new Slice("$1000", BPurple, null, (Player p) -> p.money += 1000),
				new Slice("LOSE A TURN", BWhite, null, null, true),
				new Slice("$800", BRed, null, (Player p) -> p.money += 1000),
				new Slice("$2000", BYellow, null, (Player p) -> p.money += 2000),
				new Slice("$650", BMagenta, null, (Player p) -> p.money += 650),
				new Slice("$800", BGreen, null, (Player p) -> p.money += 800),
				new Slice("$900", BBlue, null, (Player p) -> p.money += 900),
				new Slice("BANKRUPT", BBlack, (Player p) -> p.money = 0, null, true),
				new Slice("$3500", BRed, null, (Player p) -> p.money += 3500),
				new Slice("$800", BGreen, null, (Player p) -> p.money += 800)
		};
		//endregion
		
		System.out.println("How well do you know Ontario's birds? Let's find out!");
		
		for (int i = 0; i < 3; i++) { // There should be 3 rounds, then a bonus round
			System.out.println("Press enter to begin round " + i);
			doRegularRound(in, players, wheel);
		}
		
		List<Player> sortedByMoney = Arrays.asList(players); // Determine which player has the highest money, then start the bonus round with them.
		sortedByMoney.sort(Comparator.comparingInt((Player p) -> p.money));
		
		displayLeaderboard(players);
		
		doBonusRound(in, sortedByMoney.get(sortedByMoney.size() - 1));
	}
	
	/**
	 * Runs a regular round with the provided players
	 *
	 * @param in      The main scanner
	 * @param players The players participating
	 * @param wheel   The wheel to use for this round
	 */
	public static void doRegularRound(Scanner in, Player[] players, Wheel wheel) {
		String phrase = selectPhrase(); // Select a phrase
		ArrayList<Character> guesses = new ArrayList<>(Arrays.asList(' ', '-', '_', '\'')); // Start the guesses with punctuation so it doesn't hide it
		
		boolean guessed = false;
		boolean started = false;
		
		int startingPlayer = r.nextInt(players.length); // Pick a random starting player
		
		while (!guessed) {
			midOuterLoop:
			for (int i = 0; i < players.length; i++) {
				if (!started) { // If we haven't started yet, start with the starting player
					i = startingPlayer;
				}
				Player player = players[i];
				if (!started) { // Let the user know who goes first, then start
					System.out.println(player.name + " starts first!");
					started = true;
				}
				
				player.onTurnStart(); // Tell the player it's time to start the round
				
				while (true) {
					GuessChoice guessChoice = player.getGuessChoice(formatPhraseWithGuesses(phrase, guesses), guesses); // Get the player's action of choice
					
					if (guessChoice == GuessChoice.BUYVOWEL) {
						char guess = player.getVowelGuess(guesses); // Verifying the vowel is valid is left up to the Player objects at the moment
						
						player.money -= 250;
						guesses.add(guess);
						
						player.resolveBuyVowel(charOccurrencesInStringCaseInsensitive(phrase, guess));
					} else if (guessChoice == GuessChoice.GUESSPHRASE) {
						String guess = player.getPhraseGuess(guesses);
						if (stringsEqualWithoutPunctuation(phrase, guess)) { // Check if the answer is correct
							player.resolveGuessPhrase(true);
							guessed = true;
							player.money += 1000; // Give the player 1000$ for completing the phrase
							break midOuterLoop; // Break out of the entire loop
						} else {
							player.resolveGuessPhrase(false);
							break; // End the player's turn
						}
					} else { // Player is guessing a letter
						Slice slice = wheel.spin(player, in, player.prepareToSpin()); // Spin the wheel when the player is ready
						
						player.onSpinEnded(slice);
						
						slice.onSelected.accept(player);
						
						char guess = player.getLetterGuess(guesses);
						
						guesses.add(guess);
						if (stringContainsCharCaseInsensitive(phrase, guess)) { // If the player was right, give them their money and continue
							player.resolveGuessLetter(charOccurrencesInStringCaseInsensitive(phrase, guess));
							slice.onWon.accept(player);
						} else {
							player.resolveGuessLetter(0);
							break; // If the player was wrong, their turn ends
						}
						
						if (slice.endsTurn) {
							continue midOuterLoop; // If the slice spun ends the turn, end the turn
						}
					}
				}
			}
		}
	}
	
	/**
	 * Runs a bonus round with the provided player
	 *
	 * @param in     The main scanner
	 * @param player The player participating in the bonus round
	 */
	public static void doBonusRound(Scanner in, Player player) {
		String phrase = selectPhrase(); // Select a phrase
		ArrayList<Character> guesses = new ArrayList<>(Arrays.asList(' ', '-', '_', '\'', 'r', 's', 't', 'l', 'n', 'e')); // The guesses contains punctuation and the required letters
		
		Wheel wheel = new Wheel();
		
		//region Bonus Wheel Initialization
		wheel.slices = new Slice[] {
				new Slice("    I    ", BBlue, null, (Player p) -> p.money += 80_000),
				new Slice("    L    ", BBlack, null, (Player p) -> p.money += 55_000),
				new Slice("    O    ", BRed, null, (Player p) -> p.money += 40_000),
				new Slice("    V    ", BGreen, null, (Player p) -> p.money += 100_000),
				new Slice("    E    ", BRed, null, (Player p) -> p.money += 75_000),
				new Slice("    B    ", BBlue, null, (Player p) -> p.money += 250_000),
				new Slice("    I    ", BBlack, null, (Player p) -> p.money = 75_000),
				new Slice("    R    ", BRed, null, (Player p) -> p.money += 35_000),
				new Slice("    D    ", BGreen, null, (Player p) -> p.money += 8_000),
				new Slice("    S    ", BRed, null, (Player p) -> p.money += 69_000),
				new Slice("    \uD83D\uDC26    ", BWhite, null, (Player p) -> p.money += 1_000_000) // Bird emoji
		};
		//endregion
		
		Slice prize = wheel.spin(player, in, player.prepareBonusSpin()); // Spin the wheel when the player is ready
		
		System.out.println(player.name + " spun " + prize);
		
		int playerOriginalMoney = player.money; // Calculate the value of the slice
		prize.onWon.accept(player);
		
		int sliceValue = player.money - playerOriginalMoney;
		player.money = playerOriginalMoney;
		
		System.out.println("This slice is worth $" + sliceValue + "!");
		
		
		boolean guessed = false;
		
		int vowelsGuessed = 0, consonantsGuessed = 0;
		
		while (true) {
			player.onTurnStart();
			
			// Check if the player wants to guess the phrase or not
			boolean guessingPhrase = player.getGuessChoiceBonus(formatPhraseWithGuesses(phrase, guesses), guesses, consonantsGuessed, vowelsGuessed) == GuessChoice.GUESSPHRASE;
			
			
			if (guessingPhrase) {
				String guess = player.getPhraseGuess(guesses); // Get the player's guess and check if it's right
				if (stringsEqualWithoutPunctuation(phrase, guess)) {
					player.resolveGuessPhrase(true);
					guessed = true;
				} else {
					player.resolveGuessPhrase(false);
				}
				break; // The player has made their guess. Stop looping
			} else {
				char guess = player.getLetterGuess(guesses);
				
				if (vowels.contains(Character.toLowerCase(guess))) { // Increment the correct variable
					vowelsGuessed++;
				} else {
					consonantsGuessed++;
				}
				
				guesses.add(guess);
				if (stringContainsCharCaseInsensitive(phrase, guess)) { // Resolve the guess
					player.resolveGuessLetter(charOccurrencesInStringCaseInsensitive(phrase, guess));
				} else {
					player.resolveGuessLetter(0);
				}
			}
		}
		
		if (guessed) {
			prize.onWon.accept(player); // If the player guessed right, give them their prize money!
		}
	}
	
	//endregion
	
	//region Input
	
	/**
	 * Asks the player what gamemode they want, then sets <code>Main.gameMode</code> accordingly
	 *
	 * @param in The main scanner
	 */
	public static void setGameMode(Scanner in) {
		System.out.println("What gamemode do you want to play?");
		System.out.println("1. Singleplayer");
		System.out.println("2. Local Multiplayer");
		System.out.println("3. Online Multiplayer");
		
		int selection;
		
		
		while (true) { // Just loop until the user enters something correct
			try {
				String input = in.nextLine();
				selection = Integer.parseInt(input);
			} catch (Exception e) {
				System.out.println("Choose a number");
				continue;
			}
			if (selection > 3 || selection < 1) {
				System.out.println("Choose a number between 1 and 3");
				continue;
			}
			switch (selection) {
				case 1 -> gameMode = GameMode.SinglePlayer;
				case 2 -> gameMode = GameMode.LocalMultiplayer;
				case 3 -> gameMode = GameMode.OnlineMultiplayer;
			}
			break;
		}
	}
	
	//endregion
	
	//region Display
	
	/**
	 * Displays the leaderboard
	 *
	 * @param players The list of players
	 */
	public static void displayLeaderboard(Player[] players) {
		System.out.println("Leaderboard:");
		
		ArrayList<Player> playersByMoney = new ArrayList<>(Arrays.asList(players));
		playersByMoney.sort(Comparator.comparingInt((Player p) -> p.money));
		
		for (Player player : playersByMoney) {
			System.out.println("  " + player.name + ": " + player.money);
		}
	}
	
	/**
	 * Displays a phrase to the screen with unguessed letters replaced with ☐
	 *
	 * @param phrase  The unredacted phrase
	 * @param guesses The list of guessed characters
	 */
	public static void displayPhrase(String phrase, List<Character> guesses) {
		for (int i = 0; i < phrase.length(); i++) {
			if (guesses.contains(Character.toLowerCase(phrase.charAt(i)))) {
				System.out.print(phrase.charAt(i));
			} else {
				System.out.print("☐");
			}
		}
		System.out.println();
	}
	
	/**
	 * Displays all the player's guesses
	 *
	 * @param guesses The list of guessed characters
	 */
	public static void displayGuesses(List<Character> guesses) {
		System.out.println("You have guessed the following characters:");
		for (char guess : guesses) {
			if (Character.isAlphabetic(guess)) {
				System.out.print(guess + " ");
			}
		}
		System.out.println();
	}
	
	/**
	 * Displays a player's money
	 *
	 * @param p The player to display
	 */
	public static void displayPlayerStats(Player p) {
		System.out.println("You have $" + p.money);
	}
	
	//endregion
	
	//region Utility
	
	public static Random r = new Random();
	
	/**
	 * Selects a random phrase from the list of phrases
	 *
	 * @return A phrase selected from the list of phrases
	 */
	public static String selectPhrase() {
		return phrases[r.nextInt(phrases.length)];
	}
	
	/**
	 * Checks if 2 strings are equal without case or punctuation
	 *
	 * @param s1 The first string to check
	 * @param s2 The second string to check
	 * @return A boolean which is true if they are equal and false if they aren't
	 */
	public static boolean stringsEqualWithoutPunctuation(String s1, String s2) {
		String s1n = s1.replaceAll("[ \\-_]", "");
		String s2n = s2.replaceAll("[ \\-_]", "");
		return s1n.equalsIgnoreCase(s2n);
	}
	
	/**
	 * Counts the number of occurrences of a character in a string. Case sensitive.
	 *
	 * @param s The string to check
	 * @param c The character to check
	 * @return The number of occurrences
	 */
	public static int charOccurrencesInString(String s, char c) {
		int count = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == c) {
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Counts the number of occurrences of a character in a string. Case insensitive.
	 *
	 * @param s The string to check
	 * @param c The character to check
	 * @return The number of occurrences
	 */
	public static int charOccurrencesInStringCaseInsensitive(String s, char c) {
		int count = 0;
		for (int i = 0; i < s.length(); i++) {
			if (Character.toLowerCase(s.charAt(i)) == Character.toLowerCase(c)) {
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Replaces all unguessed characters in a string with ☐. Case insensitive
	 *
	 * @param phrase  The phrase
	 * @param guesses The guessed characters
	 * @return The formatted string
	 */
	public static String formatPhraseWithGuesses(String phrase, ArrayList<Character> guesses) {
		StringBuilder out = new StringBuilder();
		
		for (int i = 0; i < phrase.length(); i++) {
			if (guesses.contains(Character.toLowerCase(phrase.charAt(i)))) {
				out.append(phrase.charAt(i));
			} else {
				out.append("☐");
			}
		}
		
		return out.toString();
	}
	
	/**
	 * Read a list of phrases seperated by newlines from the provided path and places it in <code>Main.phrases</code>
	 *
	 * @param path The path to the file containing the phrases
	 */
	public static void readPhrasesFromFile(String path) {
		try {
			List<String> lines;
			try {
				lines = Files.readAllLines(Paths.get(Main.class.getResource(path).toURI())); // This one works in IDEA and OnlineGDB and throws an error in the the .jar version
			} catch (Exception e) {
				lines = Files.readAllLines(Paths.get("./birds.txt")); // This one works in the .jar version
			}
			phrases = lines.toArray(new String[1]);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Checks if a string contains a character. Case insensitive.
	 *
	 * @param s The string to check
	 * @param c The character to check for
	 * @return Returns true if the string contains the character and false if it doesn't.
	 */
	public static boolean stringContainsCharCaseInsensitive(String s, char c) {
		for (int i = 0; i < s.length(); i++) {
			if (Character.toLowerCase(s.charAt(i)) == Character.toLowerCase(c)) {
				return true;
			}
		}
		return false;
	}
	
	//endregion
	
	//region Colours
	
	// ASCII Colour Codes (https://www.lihaoyi.com/post/BuildyourownCommandLinewithANSIescapecodes.html)
	
	public static String FBlack = "\u001b[30m";
	public static String FRed = "\u001b[31m";
	public static String FGreen = "\u001b[32m";
	public static String FYellow = "\u001b[33m";
	public static String FBlue = "\u001b[34m";
	public static String FMagenta = "\u001b[35m";
	public static String FCyan = "\u001b[36m";
	public static String FWhite = "\u001b[37m";
	
	public static String BBlack = "\u001b[40m";
	public static String BRed = "\u001b[41m";
	public static String BGreen = "\u001b[42m";
	public static String BYellow = "\u001b[43m";
	public static String BBlue = "\u001b[44m";
	public static String BMagenta = "\u001b[45m";
	public static String BCyan = "\u001b[46m";
	public static String BWhite = "\u001b[47m";
	
	public static String BPurple = "\u001b[48;5;56m";
	
	public static String Bold = "\u001b[1m";
	public static String Underline = "\u001b[4m";
	public static String Reversed = "\u001b[7m";
	
	public static String Reset = "\u001b[0m";
	//endregion
}