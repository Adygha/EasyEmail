package view;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import lib.AbsEventEmitter;

/**
 * A class that represents a secure connection to an email server.
 * @author Janty Azmat
 */
public class SslConnection extends AbsEventEmitter<SslConnection.SslEvents> implements AutoCloseable {

	/**
	 * An enum that represents the events an 'AbsSslConnection' object can emit.
	 * @author Janty Azmat
	 */
	public enum SslEvents {
		ResponseReceived,
		ServerLost
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
		super(SslEvents.class);
		this.meAddr = new InetSocketAddress(InetAddress.getByName(serverAddress), serverPort);
		this.addEventListener(null, null);
	}

	private void readRunner() {
		// TODO:: Reading responses here.
	}

	public void connect() throws IOException {
		if (this.meSock != null && !this.meSock.isClosed()) {
			throw new IllegalStateException("Already connected.");
		}
		this.meSock = (SSLSocket)SSLSocketFactory.getDefault().createSocket();
		this.meSock.connect(this.meAddr);
		this.meSock.startHandshake();
		this.meReadThrd = new Thread(this::readRunner);
	}

	public void disconnect() throws IOException, InterruptedException {
		if (this.meSock != null && !this.meSock.isClosed()) {
			if (!this.meSock.isInputShutdown()) {
				this.meSock.getInputStream().close();	// Seem better this way to prevent unneeded
//				this.meSock.shutdownInput();			// exception when closing socket
				this.meReadThrd.join();
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
