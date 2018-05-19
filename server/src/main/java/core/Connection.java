package core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executors;

public class Connection extends Observable implements Observer, AutoCloseable{

	private boolean isClose = false;
	private static final int porta = 9999;
	private ServerSocket socket;
	private static final String FOLDER_RAIZ = "C:\\SERVER-FLF\\";
	
	public Connection() throws UnknownHostException, IOException {
		this.socket = new ServerSocket(porta);
	}
	
	public void init(){
		
		final Connection connection = this;
		
		Executors.newSingleThreadExecutor().execute(new Runnable() {			
			public synchronized void run() {
				
				setChanged();
				notifyObservers("Pronto para receber arquivos");
				
				while(!isClose && !socket.isClosed()){					
					ServerFile servidor = null;	
					Socket cliente = null;
					
					try {
						cliente = socket.accept();
						DataInputStream din = new DataInputStream(cliente.getInputStream());
						String mensagem = din.readUTF();
						
						servidor = new ServerFile(ServerFile.PORTA_SERVIDOR, ServerFile.PORTA_ACK, FOLDER_RAIZ+mensagem);
						servidor.addObserver(connection);
						setChanged();
						notifyObservers("Se preparando para receber o arquivo: "+mensagem);
						servidor.run();		
						
					} catch (Exception e) {
						DataOutputStream dataOutputStream = null;
						try {
							dataOutputStream = new DataOutputStream(cliente.getOutputStream());
							dataOutputStream.writeUTF("ERROR");
						} catch (Exception e2) {
							e2.printStackTrace();
						}finally {
							try {
								dataOutputStream.close();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}finally {
						if(servidor!=null)
							try {
								servidor.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
						if(cliente!=null)
							try {
								cliente.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
					}
				}
			}
		});
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static String getIP() {
		try {
			boolean preferIpv4 = true;
			boolean preferIPv6 = false;
			Enumeration en = NetworkInterface.getNetworkInterfaces();
			while (en.hasMoreElements()) {
				NetworkInterface i = (NetworkInterface) en.nextElement();
				for (Enumeration en2 = i.getInetAddresses(); en2.hasMoreElements();) {
					InetAddress addr = (InetAddress) en2.nextElement();
					if (!addr.isLoopbackAddress()) {
						if (addr instanceof Inet4Address) {
							if (preferIPv6) {
								continue;
							}
							return addr.getHostAddress();
						}
						if (addr instanceof Inet6Address) {
							if (preferIpv4) {
								continue;
							}
							return addr.getHostAddress();
						}
					}
				}
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	//Repassa a notificação
	public void update(Observable o, Object arg) {
		setChanged();
		notifyObservers(arg);
	}
	
	public void close() throws Exception {
		if(socket!=null)
			socket.close();
		this.isClose = true;	
	}
	
}
