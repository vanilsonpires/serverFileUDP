package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import core.Connection;

@SuppressWarnings("serial")
public class Frame extends JFrame implements Observer {

	private JPanel panelButton;
	private JTextArea jTextAreaLogs;
	private Connection connection;
	private JButton onOff;
	private static final String FOLDER_RAIZ = "C:\\SERVER-FLF\\A.txt";

	// Utilitarios
	private SimpleDateFormat formatHora;

	public Frame() {
		super("Server - FLF");
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
	
	public void ligarDesligarServer() {
		try {
			if (connection == null) {
				onOff.setText("Desligar");
				onOff.repaint();

				// Fica aguardando novas conexões
				connection = new Connection();
				connection.addObserver(this);
				connection.init();
				
			} else {
				onOff.setText("Ligar");
				onOff.repaint();
				if (connection != null)
					try {
						connection.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				connection = null;
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "ERRO", JOptionPane.ERROR_MESSAGE);
		}
	}

	private JPanel panelCenter() {

		JPanel base = new JPanel();
		base.setLayout(new FlowLayout(FlowLayout.CENTER));

		JPanel jPanel = new JPanel();
		jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));
		JLabel labelRaiz = new JLabel("Pasta Raiz:");
		labelRaiz.setPreferredSize(new Dimension(150, 20));
		labelRaiz.setHorizontalAlignment(JLabel.CENTER);
		jPanel.add(labelRaiz);

		JPanel panelPasta = new JPanel();
		panelPasta.setLayout(new FlowLayout(FlowLayout.CENTER));

		JTextField folderRaiz = new JTextField();
		folderRaiz.setText(FOLDER_RAIZ);
		folderRaiz.setPreferredSize(new Dimension(300, 20));
		folderRaiz.setEditable(false);
		panelPasta.add(folderRaiz);
		jPanel.add(panelPasta);

		onOff = new JButton("Ligar");
		onOff.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ligarDesligarServer();
			}
		});
		panelPasta.add(onOff);

		base.add(jPanel);

		return base;
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
		this.jTextAreaLogs
				.setText(jTextAreaLogs.getText().concat("\n ".concat(getHoraAtual()).concat(" " + msg.trim())));
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
		if (arg1 != null)
			addLog(String.valueOf(arg1));
	}
}
