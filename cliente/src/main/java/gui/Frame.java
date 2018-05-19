package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import core.Cliente;
import core.SocketClient;;

@SuppressWarnings("serial")
public class Frame extends JFrame implements Observer{
	
	private JPanel panelButton;
	private JTextArea jTextAreaLogs;
	private Cliente cliente;
	private JTextField ipServer;
	
	// Utilitarios
	private SimpleDateFormat formatHora;
	
	public Frame() {
		super("Cliente File - FLF");
		try {
	        this.setLayout(new BorderLayout());
			this.setMinimumSize(new Dimension(400, 600));
			this.setMaximumSize(this.getMinimumSize());
			this.add(panelCenter(), BorderLayout.CENTER);
			createPanelButton();
			this.add(panelButton, BorderLayout.SOUTH);
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			this.pack();
			this.setLocationRelativeTo(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    /**
     * Abre um arquivo selecionado pelo usuário
     * @author Vanilson Pires
     * @date 2018-02-26
     * @return
     */
    public File openFile() {
        JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home")));
       
        int returnVal = chooser.showOpenDialog(this);

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            // Cancelar
            return null;
        }
     
        return chooser.getSelectedFile();
    }
	
	private JPanel panelCenter(){
		
		JPanel base = new JPanel();
		base.setLayout(new FlowLayout(FlowLayout.CENTER));
		
		JPanel jPanel = new JPanel();
		jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));
		
		JLabel jLabel = new JLabel("Digite aqui o ip do servidor:");
		jLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		jPanel.add(jLabel);
		ipServer = new JTextField();
		ipServer.setText(getIP());
		jPanel.add(ipServer);
		
		JPanel panelCenter = new JPanel();
		panelCenter.setLayout(new FlowLayout(FlowLayout.CENTER));
		
		JButton enviarArquivo = new JButton("Enviar arquivo ");
		enviarArquivo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				//Instância um socket
				SocketClient socketClient = new SocketClient();
				
				//Inicializa o mesmo
				socketClient.ligar(9999, ipServer.getText());
				
				File file = openFile();
				
				if(file==null)
					return;
				
				//Avisa o servidor que irá enviar o arquivo..
				socketClient.send(file.getName());
				
				//Envia o arquivo
				enviarFile(file);
			}
		});
		JButton receberArquivo = new JButton("Receber arquivo");
		
		panelCenter.add(enviarArquivo);
		panelCenter.add(receberArquivo);
		
		jPanel.add(panelCenter);
		
		base.add(jPanel);
		
		return base;
	}
	
	public void enviarFile(File file){
		cliente = new Cliente(file, ipServer.getText());
		cliente.addObserver(this);
		cliente.init();
	}
	
	/**
	 * Retorna o IP atual da máquina
	 * 
	 * @autor Vanilson Pires
	 * @date 3 de mai de 2018
	 * @return
	 * @throws SocketException
	 */
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
	
	/**
	 * Cria o painel do fundo
	 * 
	 * @author Vanilson Pires Date 12 de mai de 2018
	 */
	private void createPanelButton() {
		this.panelButton = new JPanel();
		panelButton.setLayout(new BorderLayout());

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		JLabel labelLogs = new JLabel("Logs: ");
		labelLogs.setHorizontalAlignment(JLabel.CENTER);
		panel.add(labelLogs, BorderLayout.NORTH);

		JPanel panelTextLogs = new JPanel();
		panelTextLogs.setLayout(new FlowLayout(FlowLayout.CENTER));
		this.jTextAreaLogs = new JTextArea(25, 70);
		this.jTextAreaLogs.setText("");
		this.jTextAreaLogs.setAutoscrolls(true);
		// jTextAreaLogs.setPreferredSize(new Dimension(750, 150));
		jTextAreaLogs.setEditable(false);
		JScrollPane scroll = new JScrollPane(jTextAreaLogs);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		panelTextLogs.add(scroll);

		panel.add(panelTextLogs, BorderLayout.CENTER);

		panelButton.add(panel, BorderLayout.CENTER);
		this.add(panelButton, BorderLayout.SOUTH);
	}

	// Adiciona um log
	public void addLog(String msg) {
		
		if(jTextAreaLogs.getText().length()>5000){
			jTextAreaLogs.setText(jTextAreaLogs.getText().substring(5000, jTextAreaLogs.getText().length()));
			this.jTextAreaLogs.setText(jTextAreaLogs.getText().concat("\n ".concat(getHoraAtual()).concat(" " + msg.trim())));
		}else{
			this.jTextAreaLogs.setText(jTextAreaLogs.getText().concat("\n ".concat(getHoraAtual()).concat(" " + msg.trim())));
		}
		jTextAreaLogs.setCaretPosition(jTextAreaLogs.getText().length());
		this.jTextAreaLogs.repaint();
	}
	
	/**
	 * Retorna a data/hora atual formatado
	 * 
	 * @author Vanilson Pires Date 12 de mai de 2018
	 * @return
	 */
	private String getHoraAtual() {
		if (this.formatHora == null)
			this.formatHora = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
		return "[" + this.formatHora.format(new Date()) + "]";
	}

	public void update(Observable arg0, Object arg1) {
		if(arg1!=null)
			addLog(String.valueOf(arg1));
	}
}
