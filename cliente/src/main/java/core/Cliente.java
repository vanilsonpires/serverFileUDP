package core;

import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class Cliente extends Observable implements Runnable, AutoCloseable{
	
	public static final int CABECALHO = 4;
	public static final int TAMANHO_PACOTE = 1000; // (numSeq:4, dados=1000) Bytes : 1004 Bytes total
	public static final int TAMANHO_JANELA = 10;
	public static final int VALOR_TEMPORIZADOR = 1000;
	public static final int PORTA_SERVIDOR = 8002;
	public static final int PORTA_ACK = 8003;

	private int base; // numero da janela
	private int proxNumSeq; // proximo numero de sequencia na janela
	private String caminho; // diretorio + nome do arquivo
	private List<byte[]> listaPacotes;
	private Timer timer;
	private Semaphore semaforo;
	private boolean transferenciaCompleta;
	private int portaDestino;
	private String enderecoIp;
	private int portaEntrada;
	private ThreadEntrada tEntrada;
	private ThreadSaida tSaida;

	// construtor
	public Cliente(File file, String enderecoIp){ 
		if(file!=null)
			this.caminho = file.getAbsolutePath();
		this.portaDestino = PORTA_SERVIDOR;
		this.enderecoIp = enderecoIp;
		this.portaEntrada = PORTA_ACK;
	}
	// fim do construtor

	/**
	 * Representa um temporizador para controlar o tempo máximo de espera por uma resposta do servidor
	 * @author vanilson
	 *
	 */
	public class Temporizador extends TimerTask {

		public void run() {
			try {
				//Tenta adquirir um novo recurso
				semaforo.acquire();
				setChanged();
				notifyObservers("Cliente: Tempo expirado!");
				proxNumSeq = base; // reseta numero de sequencia
				semaforo.release();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// para iniciar ou parar o temporizador
	public void manipularTemporizador(boolean novoTimer) {
		if (timer != null) {
			timer.cancel();
		}
		if (novoTimer) {
			timer = new Timer();
			timer.schedule(new Temporizador(), VALOR_TEMPORIZADOR);
		}
	}

	public class ThreadSaida extends Thread implements AutoCloseable{

		private DatagramSocket socketSaida;
		private int portaDestino;
		private InetAddress enderecoIP;

		// construtor
		public ThreadSaida(DatagramSocket socketSaida, int portaDestino, int portaEntrada, String enderecoIP) throws UnknownHostException {
			this.socketSaida = socketSaida;
			this.portaDestino = portaDestino;
			this.enderecoIP = InetAddress.getByName(enderecoIP);
		}

		// cria o pacote com numero de sequencia e os dados
		public byte[] gerarPacote(int numSeq, byte[] dadosByte) {
			byte[] numSeqByte = ByteBuffer.allocate(CABECALHO).putInt(numSeq).array();
			ByteBuffer bufferPacote = ByteBuffer.allocate(CABECALHO + dadosByte.length);
			bufferPacote.put(numSeqByte);
			bufferPacote.put(dadosByte);
			return bufferPacote.array();
		}

		public void run() {
			try {
				
				if(caminho==null){
					setChanged();
					notifyObservers("Arquivo não selecionado. Operação cancelada");
					return;
				}	
				
				FileInputStream fis = new FileInputStream(new File(caminho));

				try {
					while (!transferenciaCompleta) { // envia pacotes se a janela nao estiver cheia
						if (proxNumSeq < base + (TAMANHO_JANELA * TAMANHO_PACOTE)) {
							semaforo.acquire();
							if (base == proxNumSeq) { // se for primeiro pacote da janela, inicia temporizador
								manipularTemporizador(true);
							}
							byte[] enviaDados = new byte[CABECALHO];
							boolean ultimoNumSeq = false;

							if (proxNumSeq < listaPacotes.size()) {
								enviaDados = listaPacotes.get(proxNumSeq);
							} else {
								byte[] dataBuffer = new byte[TAMANHO_PACOTE];
								int tamanhoDados = fis.read(dataBuffer, 0, TAMANHO_PACOTE);
								if (tamanhoDados == -1) { // sem dados para enviar, envia pacote vazio
									ultimoNumSeq = true;
									enviaDados = gerarPacote(proxNumSeq, new byte[0]);
								} else { // ainda ha dados para enviar
									byte[] dataBytes = Arrays.copyOfRange(dataBuffer, 0, tamanhoDados);
									enviaDados = gerarPacote(proxNumSeq, dataBytes);
								}
								listaPacotes.add(enviaDados);
							}
							// enviando pacotes
							socketSaida.send(new DatagramPacket(enviaDados, enviaDados.length, enderecoIP, portaDestino));
							setChanged();
							notifyObservers("Cliente: Numero de sequencia enviado " + proxNumSeq);

							// atualiza numero de sequencia se nao estiver no
							// fim
							if (!ultimoNumSeq) {
								proxNumSeq += TAMANHO_PACOTE;
							}
							semaforo.release();
						}
						sleep(5);
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					manipularTemporizador(false);
					socketSaida.close();
					fis.close();
					
					setChanged();
					notifyObservers("Cliente: Socket de saida fechado!");
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

		public void close() throws Exception {
			if(socketSaida!=null)
				socketSaida.close();
		}
	}

	public class ThreadEntrada extends Thread implements AutoCloseable {

		private DatagramSocket socketEntrada;

		// construtor
		public ThreadEntrada(DatagramSocket socketEntrada) {
			this.socketEntrada = socketEntrada;
		}

		// retorna ACK
		int getnumAck(byte[] pacote) {
			byte[] numAckBytes = Arrays.copyOfRange(pacote, 0, CABECALHO);
			return ByteBuffer.wrap(numAckBytes).getInt();
		}

		public void run() {
			try {
				byte[] recebeDados = new byte[CABECALHO]; // pacote ACK sem
															// dados
				DatagramPacket recebePacote = new DatagramPacket(recebeDados, recebeDados.length);
				try {
					while (!transferenciaCompleta) {
						socketEntrada.receive(recebePacote);
						int numAck = getnumAck(recebeDados);
						
						setChanged();
						notifyObservers("Cliente: Ack recebido " + numAck);

						// se for ACK duplicado
						if (base == numAck + TAMANHO_PACOTE) {
							semaforo.acquire();
							manipularTemporizador(false);
							proxNumSeq = base;
							semaforo.release();
						} else if (numAck == -2) {
							transferenciaCompleta = true;
						} // ACK normal
						else {
							base = numAck + TAMANHO_PACOTE;
							semaforo.acquire();
							if (base == proxNumSeq) {
								manipularTemporizador(false);
							} else {
								manipularTemporizador(true);
							}
							semaforo.release();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					socketEntrada.close();
					setChanged();
					notifyObservers("Cliente: Socket de entrada fechado!");
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		public void close() throws Exception {
			if(socketEntrada!=null)
				socketEntrada.close();
		}
	}

	public void run() {
		base = 0;
		proxNumSeq = 0;
		listaPacotes = new ArrayList<byte[]>(TAMANHO_JANELA);
		transferenciaCompleta = false;
		DatagramSocket socketSaida, socketEntrada;
		semaforo = new Semaphore(1);
		
		setChanged();
		notifyObservers("Cliente: porta de destino: " + portaDestino + ", porta de entrada: " + portaEntrada + ", caminho: " + caminho);

		try {
			// criando sockets
			socketSaida = new DatagramSocket();
			socketEntrada = new DatagramSocket(portaEntrada);

			// criando threads para processar os dados
			tEntrada = new ThreadEntrada(socketEntrada);
			tSaida = new ThreadSaida(socketSaida, portaDestino, portaEntrada, enderecoIp);
			tEntrada.start();
			tSaida.start();

		} catch (Exception e) {
			e.printStackTrace();
			setChanged();
			notifyObservers(e.getMessage());
		}		
	}
	
	public void init() {
		Executors.newSingleThreadExecutor().execute(this);
	}

	public void close() throws Exception {
		if(tEntrada!=null)
			tEntrada.close();
		if(tSaida!=null)
			tSaida.close();
	}
}
