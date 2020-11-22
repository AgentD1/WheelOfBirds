import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages network interactions like connecting to a server, hosting a server, and broadcasting information
 */
public class NetworkManager {
	/**
	 * The current list of players as a thread-safe ArrayList
	 */
	public static CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();
	
	/**
	 * The local player
	 */
	public static NetworkedLocalPlayer localPlayer;
	
	/**
	 * The thread that listens for new connections. Null for clients.
	 */
	static NetworkListenerThread listenerThread;
	
	/**
	 * The main input scanner
	 */
	public static Scanner in;
	
	/**
	 * The random seed. This must be kept in order to synchronise the Pseudo-random number generators on every client to prevent desync and limit network communication
	 */
	public static long randomSeed;
	
	/**
	 * Tries to connect to a server at the given IP address
	 * @param address The address to attempt a connection to
	 */
	public static void connectToIp(SocketAddress address) {
		Socket s = new Socket(); // Create a new socket to connect to the server with
		
		try {
			System.out.println("Connecting");
			s.connect(address); // Connect to the given address and create the data streams
			DataInputStream inputStream = new DataInputStream(s.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(s.getOutputStream());
			
			String username = "RemotePlayer" + new Random().nextInt(1000); // Generate a new name. I'll make the user choose this at some point, but I'm running out of time
			
			System.out.println("Connected! Your name is " + username); // If we made it this far without throwing any errors, it looks like we're in the clear
			
			outputStream.writeUTF(username); // Tell the server our generated username
			
			int numRemotePlayers = inputStream.readInt(); // Read the number of players from the server
			System.out.println(numRemotePlayers + " players");
			for (int i = 0; i < numRemotePlayers; i++) { // Read the list of players from the server.
				RemotePlayer p = new RemotePlayer(inputStream, outputStream, inputStream.readUTF()); // Create a new RemotePlayer for each one and add it to the player list
				System.out.println(i + ": " + p.name); // Print the player's name for the client
				players.add(p);
			}
			
			randomSeed = inputStream.readLong(); // Read the random seed from the server. This prevents desync.
			
			Main.r = new Random(randomSeed); // Configure the main random object to use the seed from the server
			
			localPlayer = new NetworkedLocalPlayer(in, username, inputStream, outputStream); // Create a player for ourselves and add it to the list
			players.add(localPlayer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Starts a server and begins to listen for client connections on the given port
	 * @param port The port to listen on
	 */
	public static void startServer(int port) {
		try {
			ServerSocket s = new ServerSocket(); // Create a new server socket, bind it to the given port.
			s.bind(new InetSocketAddress(port));
			listenerThread = new NetworkListenerThread(); // Create a new listener thread and give it the server socket
			listenerThread.serverSocket = s;
			
			randomSeed = Main.r.nextLong(); // Generate a new random seed for the random number generators with the current one. Weird
			
			listenerThread.start(); // Start the listener
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Server: Stops listening for connections and tells the clients to start the game.
	 * Client: Waits for the server to start the game.
	 * @return The list of networked players in the game
	 */
	public static Player[] getPlayers() {
		if (listenerThread == null) { // Client
			DataInputStream inputStream = localPlayer.inputStream;
			DataOutputStream outputStream = localPlayer.outputStream;
			while (true) {
				String line;
				try {
					line = inputStream.readUTF(); // Try to read a string from the server. Blocks until we get something
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
				
				if (line.equals("0StartTheGame")) { // This arbitrary string is the server's way of telling the client to start the game
					break;
				} else { // This is just another player. Add it to the list.
					RemotePlayer player = new RemotePlayer(inputStream, outputStream, line);
					players.add(player);
					System.out.println("Player " + player.name + " joins");
				}
			}
		} else { // This is the server
			listenerThread.stopRequested.set(true); // Stop the listener thread, then join with it
			try {
				listenerThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			Main.r = new Random(randomSeed); // Configure the main random number generator with the seed we made earlier
		}
		
		
		return players.toArray(new Player[0]); // Return our list of players
	}
	
	/**
	 * Sends a String object to all players
	 * @param ob The string to send
	 */
	public static void SendToAllRemotePlayers(String ob) { // There's a lot of these, one for String, int, char, and boolean. I won't document them.
		for (Player player : players) {
			if (player instanceof RemotePlayer) { // We only want to send to RemotePlayers
				try {
					((RemotePlayer) player).outputStream.writeUTF(ob); // Send it to the player
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Sends a Character to all players
	 * @param ob The character to send
	 */
	public static void SendToAllRemotePlayers(char ob) {
		for (Player player : players) {
			if (player instanceof RemotePlayer) {
				try {
					((RemotePlayer) player).outputStream.writeChar(ob);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Sends an Integer to all players
	 * @param ob The integer to send
	 */
	public static void SendToAllRemotePlayers(int ob) {
		for (Player player : players) {
			if (player instanceof RemotePlayer) {
				try {
					((RemotePlayer) player).outputStream.writeInt(ob);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Sends a Boolean to all players
	 * @param ob The boolean to send
	 */
	public static void SendToAllRemotePlayers(boolean ob) {
		for (Player player : players) {
			if (player instanceof RemotePlayer) {
				try {
					((RemotePlayer) player).outputStream.writeBoolean(ob);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Sends a String to all players except the provided player
	 * @param ob The String to send
	 * @param except The player to exclude
	 */
	public static void SendToAllRemotePlayersExcept(String ob, Player except) { // These functions send to all players except the one provided. I won't document them.
		for (Player player : players) {
			if (player instanceof RemotePlayer && player != except) {
				try {
					((RemotePlayer) player).outputStream.writeUTF(ob);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Sends a Character to all players except the provided player
	 * @param ob The character to send
	 * @param except The player to exclude
	 */
	public static void SendToAllRemotePlayersExcept(char ob, Player except) {
		for (Player player : players) {
			if (player instanceof RemotePlayer && player != except) {
				try {
					((RemotePlayer) player).outputStream.writeChar(ob);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Sends an integer to all players except the provided player
	 * @param ob The integer to send
	 * @param except The player to exclude
	 */
	public static void SendToAllRemotePlayersExcept(int ob, Player except) {
		for (Player player : players) {
			if (player instanceof RemotePlayer && player != except) {
				try {
					((RemotePlayer) player).outputStream.writeInt(ob);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Sends a boolean to all players except the provided player
	 * @param ob The boolean to send
	 * @param except The player to exclude
	 */
	public static void SendToAllRemotePlayersExcept(boolean ob, Player except) {
		for (Player player : players) {
			if (player instanceof RemotePlayer && player != except) {
				try {
					((RemotePlayer) player).outputStream.writeBoolean(ob);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

/**
 * A thread object that listens for server connections and adds to the <code>NetworkManager.players</code> list.
 * Set <code>NetworkListenerThread.stopRequested</code> to true to request the thread stop.
 */
class NetworkListenerThread extends Thread {
	ServerSocket serverSocket;
	
	AtomicBoolean stopRequested = new AtomicBoolean(false);
	
	/**
	 * Runs the thread. Starts listening for new network connections and responds to each accordingly
	 */
	@Override
	public void run() {
		try {
			serverSocket.setSoTimeout(100); // Set the socket timeout to 100ms. This means it will stop blocking and throw an error after waiting for a connection for 100ms.
											// This is important because we want to check if stopRequested is true frequently.
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		// Create the local player with the name "Host" first
		NetworkedLocalPlayer localPlayer = new NetworkedLocalPlayer(NetworkManager.in, "Host", null, null);
		
		localPlayer.onServer = true;
		
		NetworkManager.localPlayer = localPlayer;
		
		NetworkManager.players.add(localPlayer);
		
		// Loop till we can't no more
		while (!stopRequested.get()) {
			try {
				Socket conn = serverSocket.accept(); // Accept an incoming connection (or continue looping if it's been more than 100ms). Create their data streams.
				DataInputStream connIn = new DataInputStream(conn.getInputStream());
				DataOutputStream connOut = new DataOutputStream(conn.getOutputStream());
				
				localPlayer.inputStream = connIn;
				localPlayer.outputStream = connOut;
				
				String username = connIn.readUTF(); // Read their username from the output stream
				
				connOut.writeInt(NetworkManager.players.size()); // Send the client the list of connected players
				
				System.out.println(localPlayer.name);
				connOut.writeUTF(localPlayer.name); // Since the local player isn't a RemotePlayer, it won't get sent in the loop later. Send it here instead.
				
				for (Player p : NetworkManager.players) {
					if (p instanceof RemotePlayer) {
						RemotePlayer player = (RemotePlayer) p;
						connOut.writeUTF(player.name); // Send each player's username to the client
						player.onNewPlayerJoin(username); // Tell each client a new user has connected
					}
				}
				
				System.out.println(username + " is joining..."); // Tell the user a new client has connected
				
				connOut.writeLong(NetworkManager.randomSeed); // Synchronise the client's random number generator
				
				RemotePlayer player = new RemotePlayer(connIn, connOut, username); // Create the client's RemotePlayer and add it to the list.
				player.onServer = true;
				NetworkManager.players.add(player);
			} catch (IOException e) {
				if (!e.getMessage().contains("Accept timed out")) { // Accept timed out is the 100ms thing. We can ignore that. Don't ignore anything else.
					e.printStackTrace();
				}
			}
		}
	}
}
