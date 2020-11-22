import java.io.*;
import java.util.*;

/**
 * A class representing a player on the user's computer which is participating in multiplayer. All function documentation can be found in <code>Player</code>
 */
public class NetworkedLocalPlayer extends LocalPlayer {
	/**
	 * The inputStream to receive information from. Currently unused but may be used in the future
	 */
	public DataInputStream inputStream;
	
	/**
	 * The outputStream to output actions to. On the client, this is the outputStream to the server. On the server, it's unused.
	 */
	public DataOutputStream outputStream;
	
	/**
	 * Whether this NetworkedLocalPlayer is running on the server (true) or client (false)
	 */
	public boolean onServer = false;
	
	public NetworkedLocalPlayer(Scanner in, String username, DataInputStream inputStream, DataOutputStream outputStream) {
		super(in, username);
		this.inputStream = inputStream;
		this.outputStream = outputStream;
	}
	
	@Override
	void onTurnStart() {
		System.out.println();
		System.out.println(Main.BGreen + "It's your turn!" + Main.Reset);
		System.out.println("You have " + money + "$");
		System.out.println();
	}
	
	@Override
	char getLetterGuess(ArrayList<Character> guesses) {
		char letterGuess = super.getLetterGuess(guesses); // This inherits from LocalPlayer, which defines a way of asking the user for a character. Use this.
		try {
			if(onServer) { // On the server, relay this information to all clients.
				NetworkManager.SendToAllRemotePlayers(letterGuess);
			} else { // On the client, only send it to the server. The server will relay it.
				outputStream.writeChar(letterGuess);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return letterGuess;
	}
	
	@Override
	char getVowelGuess(ArrayList<Character> guesses) { // These are all basically the same as GetLetterGuess.
		char letterGuess = super.getVowelGuess(guesses);
		try {
			if(onServer) {
				NetworkManager.SendToAllRemotePlayers(letterGuess);
			} else {
				outputStream.writeChar(letterGuess);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return letterGuess;
	}
	
	@Override
	String getPhraseGuess(ArrayList<Character> guesses) {
		String phraseGuess = super.getPhraseGuess(guesses);
		try {
			if(onServer) {
				NetworkManager.SendToAllRemotePlayers(phraseGuess);
			} else {
				outputStream.writeUTF(phraseGuess);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return phraseGuess;
	}
	
	@Override
	GuessChoice getGuessChoice(String knownPhrase, ArrayList<Character> guesses) {
		GuessChoice choice = super.getGuessChoice(knownPhrase, guesses);
		try {
			if(onServer) {
				NetworkManager.SendToAllRemotePlayers(choice.name());
			} else {
				outputStream.writeUTF(choice.name());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return choice;
	}
	
	@Override
	GuessChoice getGuessChoiceBonus(String knownPhrase, ArrayList<Character> guesses, int consonants, int vowels) {
		GuessChoice choice = super.getGuessChoiceBonus(knownPhrase, guesses, consonants, vowels);
		try {
			if(onServer) {
				NetworkManager.SendToAllRemotePlayers(choice.name());
			} else {
				outputStream.writeUTF(choice.name());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return choice;
	}
	
	@Override
	boolean prepareToSpin() {
		boolean result = super.prepareToSpin();
		try {
			if(onServer) {
				NetworkManager.SendToAllRemotePlayers(result);
			} else {
				outputStream.writeBoolean(result);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	@Override
	boolean prepareBonusSpin() {
		boolean result = super.prepareBonusSpin();
		try {
			if(onServer) {
				NetworkManager.SendToAllRemotePlayers(result);
			} else {
				outputStream.writeBoolean(result);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
}
