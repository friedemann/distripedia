package grinder.client;

import grinder.util.GrinderSettings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

/**
 * The ClientGUI is the "wrapper" of the client. It provides an user interface
 * to initiate a server connection and then monitor the client's activity while
 * load testing.
 *
 * @author Friedemann
 */
@SuppressWarnings("serial")
public class ClientGUI extends JPanel {

	/** The Constant PROGRAM_VERSION. */
	private static final String PROGRAM_VERSION = " 1.4.1";

	/** The Constant TITLE_SUFFIX. */
	private static final String TITLE_SUFFIX = " - © 2010 Distripedia Team / HTW Berlin";

	/** The Constant DEFAULT_WIDTH. */
	private static final int DEFAULT_WIDTH = 600;

	/** The Constant DEFAULT_HEIGHT. */
	private static final int DEFAULT_HEIGHT = 600;

	/** The Constant MINIMUM_WIDTH. */
	private static final int MINIMUM_WIDTH = 600;

	/** The Constant MINIMUM_HEIGHT. */
	private static final int MINIMUM_HEIGHT = 600;

	/** The client instance */
	private final Client c;

	// layout components
	/** The mainframe. */
	private static JFrame mainframe;

	/** The gui. */
	@SuppressWarnings("unused")
	private final ClientGUI gui;

	/** The panel_connect. */
	private JPanel panel_connect;

	/** The panel_status. */
	private JPanel panel_status;

	/** The panel_log. */
	private JPanel panel_log;

	/** The text_ip. */
	private JTextField field_ip;

	/** The btn_connect. */
	public JButton btn_connect;

	/** The lbl_users. */
	private JLabel lbl_users;

	/** The lbl_requests. */
	private JLabel lbl_requests;

	/** The lbl_current task. */
	private JLabel lbl_status;

	/** The area_log. */
	private JTextArea area_log;

	/**
	 * Instantiates a new client gui.
	 */
	private ClientGUI() {
		super(new GridBagLayout());

		buildConnectPanel();
		buildStatusPanel();
		buildLogPanel();

		final GridBagConstraints gc = new GridBagConstraints();
		gc.insets = new Insets(3, 3, 3, 3);
		gc.fill = GridBagConstraints.BOTH;

		gc.gridx = 1;
		gc.gridy = 1;
		gc.weightx = 1;
		gc.weighty = 0;
		this.add(panel_connect, gc);

		gc.gridx = 2;
		gc.gridy = 1;
		gc.weightx = 0;
		gc.weighty = 0;
		this.add(panel_status, gc);

		gc.gridx = 1;
		gc.gridy = 2;
		gc.gridwidth = 2;
		gc.weightx = 1;
		gc.weighty = 1;
		gc.fill = GridBagConstraints.BOTH;
		this.add(panel_log, gc);

		gui = this;

		logMessage("gui started, initializing client...");
		c = new Client(this);

		// set preset values
		final Properties props = c.getProps();
		if (props != null)
		{
			setGUIProperties(props);			
			if (props.getProperty("autoconnect", "false").equals("true"))
			{
				btn_connect.doClick();
			}
		}
	}

	private void setGUIProperties(final Properties p) {
		field_ip.setText((p.getProperty("controller_ip", "127.0.0.1")).trim());
	}

	/**
	 * Builds the connect panel.
	 */
	private void buildConnectPanel() {
		panel_connect = new JPanel(new GridBagLayout());

		field_ip = new JTextField("127.0.0.1");
		btn_connect = new JButton(actionConnect);

		final GridBagConstraints gc = new GridBagConstraints();

		gc.gridx = 1;
		gc.weightx = 0.8;
		gc.fill = GridBagConstraints.BOTH;
		panel_connect.add(field_ip, gc);

		gc.gridx = 2;
		gc.weightx = 0;
		gc.fill = GridBagConstraints.NONE;
		panel_connect.add(btn_connect, gc);

		setTitledBorder(panel_connect, "connect to server");
	}

	/**
	 * Builds the status panel.
	 */
	private void buildStatusPanel() {
		panel_status = new JPanel(new GridLayout(3, 3));

		lbl_users = new JLabel("0");
		lbl_requests = new JLabel("0");
		lbl_status = new JLabel("IDLE");

		panel_status.add(new JLabel("Active users: "));
		panel_status.add(lbl_users);
		panel_status.add(new JLabel("Requests performed: "));
		panel_status.add(lbl_requests);
		panel_status.add(new JLabel("Client status: "));
		panel_status.add(lbl_status);

		lbl_status.setFont(new Font("Arial", Font.BOLD, 16));
		lbl_status.setForeground(Color.BLUE);

		setTitledBorder(panel_status, "client status");
	}

	/**
	 * Builds the logging panel.
	 */
	private void buildLogPanel() {

		panel_log = new JPanel(new GridLayout());

		area_log = new JTextArea();
		area_log.setEditable(false);
		area_log.setFont(new Font("Courier New", Font.PLAIN, 11));
		area_log.setLineWrap(true);

		// make area scrollable....
		final JScrollPane scroll_areaLog = new JScrollPane(area_log);

		panel_log.setMinimumSize(new Dimension(500, 500));
		panel_log.add(scroll_areaLog);
		setTitledBorder(panel_log, "log");
	}

	/** The action connect. */
	public Action actionConnect = new AbstractAction(" connect!  ") {
		@Override
		public void actionPerformed(final ActionEvent e) {
			btn_connect.setEnabled(false);

			final String ip = field_ip.getText();

			if (!ip.matches(GrinderSettings.REGEX_IPV4))
			{
				errorDialog("Wrong input",
						"The IP you entered seems not correctly formatted.");
				field_ip.selectAll();
				btn_connect.setEnabled(true);
			} else
			{
				try
				{
					c.setControllerIP(InetAddress.getByName(ip));

				} catch (final UnknownHostException e1)
				{
					errorDialog("Wrong input", "The host ip can't be resolved. " + e);
					btn_connect.setEnabled(true);
					return;
				}

				c.connectToController();
			}

			// evaluate connection attempt
			if (c.isConnected())
			{
				field_ip.setEnabled(false);
				btn_connect.setAction(actionDisconnect);
			}
			btn_connect.setEnabled(true);
		}
	};

	/** The action disconnect. */
	public Action actionDisconnect = new AbstractAction("disconnect") {
		@Override
		public void actionPerformed(final ActionEvent e) {

			if (c.isConnected())
			{
				final int dlg =
						confirmDialog("really disconnect?",
								"Are you sure you want to disconnect this client? "
										+ "This results in a complete loss of data this "
										+ "client has gathered so far.");
				if (dlg == JOptionPane.OK_OPTION)
				{
					c.disconnectFromController();
				}
			}
			field_ip.setEnabled(true);
			btn_connect.setAction(actionConnect);

		}
	};

	/**
	 * Static method - can be called from outer space to initiate generation of
	 * frame. Constructor of this class is invoked within.
	 *
	 * @param title - name of the program
	 */
	public static void loadGUI(final String title) {
		// try to set the UI to native win/mac layout
		ClientGUI.setNativeLookAndFeel();

		// setup a new frame
		ClientGUI.mainframe =
				new JFrame(title + ClientGUI.PROGRAM_VERSION + ClientGUI.TITLE_SUFFIX);
		ClientGUI.mainframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ClientGUI.mainframe.setSize(ClientGUI.DEFAULT_WIDTH, ClientGUI.DEFAULT_HEIGHT);
		ClientGUI.mainframe.setMinimumSize(new Dimension(ClientGUI.MINIMUM_WIDTH,
				ClientGUI.MINIMUM_HEIGHT));

		// instantiate a new gui panel here...
		final JComponent guiContentPane = new ClientGUI();
		guiContentPane.setOpaque(true);

		// ...and connect it to the main frame
		ClientGUI.mainframe.setContentPane(guiContentPane);

		// resolve the screensize
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension screenSize = toolkit.getScreenSize();

		// center the frame
		ClientGUI.mainframe.setLocation(
				(screenSize.width - ClientGUI.mainframe.getWidth()) / 2,
				(screenSize.height - ClientGUI.mainframe.getHeight()) / 2);
		ClientGUI.mainframe.setVisible(true);
	}

	/**
	 * Sets the status on the GUI
	 *
	 * @param state the new status
	 */
	public void setStatus(final ClientState state) {
		lbl_status.setText(state.toString());
	}

	/**
	 * Updates the gui's user count.
	 */
	public void updateUserCount() {

		Integer uCount = 0;
		uCount += c.userHTTP.size();
		uCount += c.userWS.size();
		uCount += c.userSTRATUS.size();

		lbl_users.setText(uCount.toString());
	}

	/**
	 * Increase request counter.
	 */
	public void increaseRequestCounter() {
		Integer i = new Integer(lbl_requests.getText());
		i++;
		lbl_requests.setText(i.toString());
	}

	/**
	 * Resets request counter.
	 */
	public void resetRequestCounter() {
		lbl_requests.setText("0");
	}

	/**
	 * Log message to the gui.
	 *
	 * @param msg the msg
	 */
	public synchronized void logMessage(final String msg) {
		final Format formatter = new SimpleDateFormat(GrinderSettings.SIMPLE_DATE_FORMAT);
		final String nowDateStr = formatter.format(new Date());
		area_log.append("[" + nowDateStr + "]> " + msg + "\n");

		// autoscroll
		area_log.setCaretPosition(area_log.getText().length());
	}

	/**
	 * Resets connection panel items. Is invoked after unforseen controller
	 * disconnection.
	 */
	public void resetClientGUIControls() {
		c.setTestTicket(null);
		setStatus(ClientState.IDLE);
		field_ip.setEnabled(true);
		btn_connect.setAction(actionConnect);
	}

	/**
	 * Open a Dialog to display an regular message.
	 *
	 * @param title - window title
	 * @param message - the message
	 */
	public void messageDialog(final String title, final String message) {
		JOptionPane
				.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Open a Dialog to display an error message.
	 *
	 * @param title - window title
	 * @param message - the error message
	 */
	public void errorDialog(final String title, final String message) {
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Open a Dialog to let the user confirm his action.
	 *
	 * @param title - window title
	 * @param message - the error message
	 * @return eg JOptionPane.OK_OPTION or CANCEL_OPTION
	 */
	public int confirmDialog(final String title, final String message) {
		return JOptionPane.showConfirmDialog(this, message, title,
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
	}

	/**
	 * Adds or sets a new titled border around a panel.
	 *
	 * @param panel panel which to surround
	 * @param s the s
	 */
	private void setTitledBorder(final JPanel panel, final String s) {
		final TitledBorder tb =
				new TitledBorder(BorderFactory.createEtchedBorder(), " " + s + " ",
						TitledBorder.LEFT, TitledBorder.TOP, new Font("Sans", Font.BOLD, 11),
						Color.BLACK);
		panel.setBorder(tb);
	}

	/**
	 * Tries to set the userinterface manager's look and feel to the native
	 * layout.
	 */
	private static void setNativeLookAndFeel() {
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final Exception e)
		{
			System.out.println("Could not set native Look and Feel: " + e);
		}
	}
}