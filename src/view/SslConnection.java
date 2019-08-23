package view;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import lib.EventEmitter;

/**
 * A class that represents a secure connection to an email server.
 * @author Janty Azmat
 */
public class SslConnection extends EventEmitter<SslConnection.ConnectionEvent> implements AutoCloseable {

	/**
	 * An enum that represents the events an 'SslConnection' object can emit.
	 * @author Janty Azmat
	 */
	public static enum ConnectionEvent {

		/**
		 * Informs that a line was received from server.
		 */
		LINE_RECEIVED,

		/**
		 * Informs that receiving data from server was interrupted (most likely due to server dropped the connection).
		 */
		RECEIVE_ITERRUPTED
	}

	// Fields
	private InetSocketAddress meAddr;
	private SSLSocket meSock;
	private Thread meReadThrd;

	/**
	 * A constructor that takes the server's address and port as parameters.
	 * @param serverAddress				the server's address.
	 * @param serverPort				the server's port.
	 * @throws UnknownHostException		when the provided host is not a proper host name or IP address.
	 * @throws SecurityException		when some security issues don't allow resolving host name to IP.
	 * @throws IllegalArgumentException	when invalid port specified.
	 */
	public SslConnection(String serverAddress, int serverPort) throws UnknownHostException, SecurityException, IllegalArgumentException {
		super(ConnectionEvent.class);
		this.meAddr = new InetSocketAddress(InetAddress.getByName(serverAddress), serverPort);
	}

	/**
	 * A method run by 'meReadThrd' read-thread to independently read from socket's InputStream.
	 */
	private void receiveRunner() {
		final var tmpLine = new LinkedList<byte[]>(); // Holds every line data
		var tmpRead = 0;
		try (final var tmpWord = new ByteArrayOutputStream()) { // 'tmpWord' holds the bytes of a single word in the read line
			Runnable tmpRun = () -> { // Just a block of code to avoid code duplication
				if (tmpWord.size() > 0) {
					tmpLine.add(tmpWord.toByteArray());
					tmpWord.reset();
				}
			};
			while ((tmpRead = this.meSock.getInputStream().read()) > -1) {
				if (tmpRead == 32) {
					tmpRun.run(); // Extract last formed word (if any)
				} else if (tmpRead == 10) {
					tmpRun.run(); // Extract last formed word (if any)
					this.emitEvent(ConnectionEvent.LINE_RECEIVED, tmpLine.toArray()); // The 'eventData' would be 'byte[][]' in this case
					tmpLine.clear(); // Start a new line
				} else if (tmpRead != 13) {
					tmpWord.write(tmpRead); // Form part of a word
				}
			}
			if (tmpWord.size() > 0) { // In case there was something left
				tmpRun.run(); // Extract last formed word (if any)
				this.emitEvent(ConnectionEvent.LINE_RECEIVED, tmpLine.toArray()); // The 'eventData' would be 'byte[][]' in this case
			}
		} catch (IOException e) {
			if (!this.meSock.isClosed()) {
				this.emitEvent(ConnectionEvent.RECEIVE_ITERRUPTED);
			}
		}
	}

	/**
	 * Start an SSL connection to server.
	 * @throws IOException				when a socket cannot be created, error while connecting, or network error while SSL handshake.
	 * @throws IllegalStateException	when this SslConnection object is already connected to server.
	 */
	public void connect() throws IOException, IllegalStateException {
		if (this.meSock != null && !this.meSock.isClosed()) {
			throw new IllegalStateException("Already connected.");
		}
		this.meSock = (SSLSocket)SSLSocketFactory.getDefault().createSocket();
		this.meSock.connect(this.meAddr);
		this.meSock.startHandshake();
		this.meReadThrd = new Thread(this::receiveRunner);	// A separate thread to read incoming data
		this.meReadThrd.start();
	}

	/**
	 * Sends the specified data using this SslConnection object.
	 * @param dataToSend				the data to be sent.
	 * @throws IOException				when an I/O error occurs while writing.
	 * @throws IllegalStateException	when this SslConnection object is not yet connected to server.
	 */
	public void send(byte[] dataToSend) throws IOException, IllegalStateException {
		if (this.meSock == null || !this.meSock.isConnected()) {
			throw new IllegalStateException("Not yet connected.");
		}
		this.meSock.getOutputStream().write(dataToSend);
	}

	/**
	 * Stops the currently established SSL connection to server if any.
	 * @throws IOException	when an I/O error occurs while closing.
	 */
	public void disconnect() throws IOException {
		if (this.meSock != null && !this.meSock.isClosed()) {
			if (!this.meSock.isInputShutdown()) {
				this.meSock.getInputStream().close();	// Seem better this way to prevent unneeded
//				this.meSock.shutdownInput();			// exception when closing socket
				if (Thread.currentThread() != this.meReadThrd) {
					try {
						this.meReadThrd.join();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
			if (!this.meSock.isOutputShutdown()) {
				this.meSock.getOutputStream().close();
			}
			this.meSock.close();
		}
	}

	@Override
	public void close() throws IOException, InterruptedException {
		this.disconnect();
	}

	/**
	 * Used to get the server's address.
	 * @return	the server's address.
	 */
	public String getServerAddress() {
		return this.meAddr.getHostString();
	}

	/**
	 * Used to get the server's port.
	 * @return	the server's port.
	 */
	public int getServerPort() {
		return this.meAddr.getPort();
	}
}
