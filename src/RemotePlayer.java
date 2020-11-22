import java.io.*;
import java.util.ArrayList;

/**
 * A class representing a player that is not on the user's computer. All function documentation can be found in <code>Player</code>
 */
public class RemotePlayer extends Player {
	public DataInputStream inputStream;
	public DataOutputStream outputStream;
	
	public boolean onServer = false;
	
	/**
	 * Construct a new RemotePlayer.
	 * @param inputStream The input stream that input will be received through. On the server, this will be the associated client's inputStream. On the client, this will be the server's input stream.
	 * @param outputStream The output stream that input will be received through. On the server, this will be the associated client's outputtream. On the client, this will be the server's output stream.
	 * @param username The remote player's username
	 */
	public RemotePlayer(DataInputStream inputStream, DataOutputStream outputStream, String username) { // Initialize
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.name = username;
	}
	
	/**
	 * The callback for when a new player joins. This is only called on the server.
	 * @param username The username of the new player
	 */
	public void onNewPlayerJoin(String username) {
		try {
			outputStream.writeUTF(username); // Inform the associated client that a new player has joined.
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	GuessChoice getGuessChoice(String knownPhrase, ArrayList<Character> guesses) {
		System.out.println(knownPhrase); // Print the knownPhrase for the convenience of the end user
		String guessChoiceName = null;
		try {
			guessChoiceName = inputStream.readUTF(); // Attempt to read the guess choice from the remote player
			
			if(onServer) { // If this is the server, relay it to all the other players
				NetworkManager.SendToAllRemotePlayersExcept(guessChoiceName, this);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Parse the guess choice string and return it
		return GuessChoice.valueOf(guessChoiceName);
	}
	
	@Override
	char getLetterGuess(ArrayList<Character> guesses) { // These all work pretty much the same, so I'll only document new things
		char letterGuess = 0;
		try {
			letterGuess = inputStream.readChar();
			
			if(onServer) {
				NetworkManager.SendToAllRemotePlayersExcept(letterGuess, this);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(name + " guesses " + letterGuess);
		
		return letterGuess;
	}
	
	@Override
	char getVowelGuess(ArrayList<Character> guesses) {
		return getLetterGuess(guesses);
	}
	
	@Override
	String getPhraseGuess(ArrayList<Character> guesses) {
		String phraseGuess = null;
		try {
			phraseGuess = inputStream.readUTF();
			
			if(onServer) {
				NetworkManager.SendToAllRemotePlayersExcept(phraseGuess, this);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(name + " guesses the phrase " + phraseGuess);
		
		return phraseGuess;
	}
	
	@Override
	boolean prepareToSpin() {
		boolean blocking = false;
		try {
			blocking = inputStream.readBoolean();
			
			if(onServer) {
				NetworkManager.SendToAllRemotePlayersExcept(blocking, this);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return blocking;
	}
	
	@Override
	void resolveBuyVowel(int occurrences) { // Print the result of the guess to the user
		System.out.println("It was in the phrase " + occurrences + " times.");
	}
	
	@Override
	void resolveGuessLetter(int occurrences) {
		System.out.println("It was in the phrase " + occurrences + " times.");
	}
	
	@Override
	void resolveGuessPhrase(boolean correct) {
		System.out.println("It was " + (correct ? "right!" : "wrong."));
	}
	
	@Override
	void onTurnStart() { // Print the fact it's a new turn to the user
		System.out.println();
		System.out.println(Main.BGreen + "It's " + name + "'s turn!" + Main.Reset);
		System.out.println(name + " has " + money + "$");
		System.out.println();
	}
	
	@Override
	void onSpinEnded(Slice slice) {
		System.out.println(name + " got a " + slice.text + " on the wheel!");
	}
	
	@Override
	boolean prepareBonusSpin() {
		return prepareToSpin();
	}
	
	@Override
	GuessChoice getGuessChoiceBonus(String knownPhrase, ArrayList<Character> guesses, int consonantsGuessed, int vowelsGuessed) {
		return getGuessChoice(knownPhrase, guesses);
	}
}
