package core;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Observable;

public class ServerFile extends Observable implements Runnable , AutoCloseable{

	public final int CABECALHO = 4;
	public final int TAMANHO_PACOTE = 1000 + CABECALHO;
	public static final int PORTA_SERVIDOR = 8002;
	public static final int PORTA_ACK = 8003;
	private int portaEntrada;
	private int portaDestino;
	private String caminho;
	private DatagramSocket socketEntrada, socketSaida;

	// construtor
	public ServerFile(int portaEntrada, int portaDestino, String caminho) {
		this.portaDestino = portaDestino;
		this.portaEntrada = portaEntrada;
		this.caminho = caminho;
	}

	public void run() {
		
		setChanged();
		notifyObservers("Servidor: porta de entrada: " + portaEntrada + ", " + "porta de destino: " + portaDestino + ".");

		int ultimoNumSeq = -1;
		int proxNumSeq = 0; // proximo numero de sequencia
		boolean transferenciaCompleta = false; // flag caso a transferencia nao for completa

		// criando sockets
		try {
			socketEntrada = new DatagramSocket(portaEntrada);
			socketSaida = new DatagramSocket();
			
			setChanged();
			notifyObservers("Servidor conectado...");
			
			try {
				byte[] recebeDados = new byte[TAMANHO_PACOTE];
				DatagramPacket recebePacote = new DatagramPacket(recebeDados, recebeDados.length);

				FileOutputStream fos = null;

				while (!transferenciaCompleta) {
					socketEntrada.receive(recebePacote);
					InetAddress enderecoIP = recebePacote.getAddress();

					int numSeq = ByteBuffer.wrap(Arrays.copyOfRange(recebeDados, 0, CABECALHO)).getInt();
					
					setChanged();
					notifyObservers("Servidor: Numero de sequencia recebido " + numSeq);

					// se o pacote for recebido em ordem
					if (numSeq == proxNumSeq) {
						// se for ultimo pacote (sem dados), enviar ack de
						// encerramento
						if (recebePacote.getLength() == CABECALHO) {
							byte[] pacoteAck = gerarPacote(-2); // ack de encerramento
							socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, enderecoIP, portaDestino));
							transferenciaCompleta = true;
							
							setChanged();
							notifyObservers("Servidor: Todos pacotes foram recebidos! Arquivo criado!");
							
						} else {
							proxNumSeq = numSeq + TAMANHO_PACOTE - CABECALHO; // atualiza proximo numero de sequencia
							byte[] pacoteAck = gerarPacote(proxNumSeq);
							socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, enderecoIP, portaDestino));
							
							setChanged();
							notifyObservers("Servidor: Ack enviado " + proxNumSeq);
						}

						// se for o primeiro pacote da transferencia
						if (numSeq == 0 && ultimoNumSeq == -1) {
							// cria arquivo
							File arquivo = new File(caminho);
							if (!arquivo.exists()) {
								arquivo.createNewFile();
							}
							fos = new FileOutputStream(arquivo);
						}
						// escreve dados no arquivo
						fos.write(recebeDados, CABECALHO, recebePacote.getLength() - CABECALHO);

						ultimoNumSeq = numSeq; // atualiza o ultimo numero de sequencia enviado
					} else { // se pacote estiver fora de ordem, mandar duplicado
						byte[] pacoteAck = gerarPacote(ultimoNumSeq);
						socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, enderecoIP, portaDestino));
						
						setChanged();
						notifyObservers("Servidor: Ack duplicado enviado " + ultimoNumSeq);
					}

				}
				if (fos != null) {
					fos.close();
				}
			} catch (Exception e) {
				if(!e.getMessage().equals("socket closed"))
					e.printStackTrace();
			} finally {
				try {
					close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				setChanged();
				notifyObservers("Servidor: Socket de entrada fechado!");
				
				setChanged();
				notifyObservers("Servidor: Socket de saida fechado!");
			}
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
	}

	// gerar pacote de ACK
	public byte[] gerarPacote(int numAck) {
		byte[] numAckBytes = ByteBuffer.allocate(CABECALHO).putInt(numAck).array();
		ByteBuffer bufferPacote = ByteBuffer.allocate(CABECALHO);
		bufferPacote.put(numAckBytes);
		return bufferPacote.array();
	}

	public void close() throws Exception {
		socketEntrada.close();
		socketSaida.close();
	}
}
