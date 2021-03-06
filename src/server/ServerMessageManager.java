package server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import server.handling.TCPHandlerServer;
import server.handling.UDPHandlerServer;
import common.ErrorTypes;
import common.Message;
import common.MessageInfoStrings;
import common.MessageManager;
import common.MessageType;
import common.handling.Handler;
import common.handling.HandlingException;
import common.logging.EventType;
import common.logging.Log;

/**
 * This class determines the server's behavior upon receiving a Message from a client 
 * and prepares an appropriate answer.
 * <br />Note that the actual reception and response are delegated to the Handler objects.
 * <br />See {@link TCPHandlerServer} and {@link UDPHandlerServer}
 * @author etudiant
 * @see Handler
 */
public class ServerMessageManager implements MessageManager {
	
	private Server server;
	private Handler handler;
	private Map<String, InetAddress> clientIps;
	private Map<String, String> clientPorts;
	private Log log;
	
	/**
	 * Constructs a message manager for the specified server and handler.
	 * @param server
	 * @param handler
	 */
	public ServerMessageManager(Server server, Handler handler) {
		this.server = server;
		this.handler = handler;
		clientIps = server.getClientIps();
		clientPorts = server.getClientPorts();
		log = server.getLog();
	}
	
	@Override
	public void handleMessage(Message message, Socket socket) throws IOException, HandlingException {
		InetAddress ip = socket.getInetAddress();
		String login = message.getInfo(MessageInfoStrings.LOGIN);
		switch (message.getType()) {
		
		
		case CONNECT:
			if (clientIps.containsKey(login)) {
				Message errorMsg = new Message(
						MessageType.ERROR,
						"Error: client already connected.");
				errorMsg.addInfo(MessageInfoStrings.ERROR_TYPE, ErrorTypes.ALREADY_CONNECTED);
				handler.sendMessage(errorMsg, socket);
				log.log(EventType.ERROR, "Rejected client authentification: "
						+ login + " is already connected.");
				break;
			}
			
			String pass = message.getInfo(MessageInfoStrings.PASSWORD);
			if (server.authenticateClient(login, pass)) {
				clientIps.put(login, ip);
				clientPorts.put(login, socket.getPort() + "");
				server.getTimeoutHandler().addClient(login);
				Message okMsg = new Message(
						MessageType.OK,
						"Able to authenticate client.");
				handler.sendMessage(okMsg, socket);
				log.log(EventType.INFO, "Client authenticated: " + login);
			} else {
				Message errorMsg = new Message(
						MessageType.ERROR,
						"Unable to authenticate client: wrong login or password.");
				handler.sendMessage(errorMsg, socket);
				log.log(EventType.ERROR, "Rejected client authentification: " + login);
			}
			break;
			
		
		default:
			throw new HandlingException("Message type " + message.getType() + " not handled by " + handler.getClass());
		}
	}

	@Override
	public void handleMessage(Message message, DatagramSocket socket, DatagramPacket paquet) throws HandlingException, IOException, ClassNotFoundException {
		String login = message.getInfo(MessageInfoStrings.LOGIN);
		server.getTimeoutHandler().updateClient(login, new Date());
		switch (message.getType()) {
		case REQUEST_LIST:
			if (clientIps.containsKey(login)) {
				
				Message clientListMsg = new Message(MessageType.CLIENT_LIST);
				List<String> clientLogins = getClientLogins();
				
				clientListMsg.addObject(MessageInfoStrings.REQUEST_LIST_CLIENT_LOGINS, clientLogins);
				clientListMsg.addInfo(MessageInfoStrings.PORT, message.getInfo(MessageInfoStrings.PORT));
				
				clientPorts.put(login, message.getInfo(MessageInfoStrings.PORT));
				handler.sendMessage(clientListMsg, socket, paquet);
				
			} else {
				Message errorMsg = new Message(
						MessageType.ERROR,
						"Client unknown, please reconnect.");
				errorMsg.addInfo(MessageInfoStrings.ERROR_TYPE, ErrorTypes.CLIENT_UNKNOWN);
				handler.sendMessage(errorMsg, socket, paquet);
			}
			break;
			
		case REQUEST_IP:
			if (clientIps.containsKey(login)) {
				
				Message clientIPMsg = new Message(MessageType.CLIENT_IP);
				clientIPMsg.addObject(MessageInfoStrings.REQUEST_IP_TARGET_IP, clientIps.get(login));
				clientIPMsg.addInfo(MessageInfoStrings.REQUEST_IP_TARGET_PORT, clientPorts.get(login));
				clientIPMsg.addInfo(MessageInfoStrings.LOGIN, login);
				clientIPMsg.addInfo(MessageInfoStrings.PORT, message.getInfo(MessageInfoStrings.PORT));
				handler.sendMessage(clientIPMsg, socket, paquet);
				
			} else {
				Message errorMsg = new Message(
						MessageType.ERROR,
						"Client unknown, please reconnect.");
				handler.sendMessage(errorMsg, socket, paquet);
			}
			break;
			
		case DISCONNECT:
			clientIps.remove(login);
			clientPorts.remove(login);
			server.getTimeoutHandler().removeClient(login);
			break;
			
		default:
			throw new HandlingException("Message type " + message.getType() + " not handled by " + handler.getClass());
	}
	}

	/**
	 * Get the list of logins.
	 * @return A List containing the name of every connected clients.
	 */
	public List<String> getClientLogins() {
		List<String> res = new ArrayList<String>();
		for (String current : clientIps.keySet()) {
			res.add(current);
		}
		return res;
	}

	/**
	 * Not implemented. Throws a {@link HandlingException}.
	 */
	@Override
	public void handleMessage(Message message, DatagramSocket socket)
			throws HandlingException, IOException, ClassNotFoundException {
		throw new HandlingException("This method has not been implemented on this MessageManager");
	}
}
