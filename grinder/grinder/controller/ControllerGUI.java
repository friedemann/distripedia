/*
 *
 */
package grinder.controller;

import grinder.util.FileChooser;
import grinder.util.GrinderSettings;
import grinder.util.TestTicket;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
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
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumn;

/**
 * The ControllerGUI is the "wrapper" of the controller. It provides an user
 * interface to setup and send test tickets to the connected clients.
 *
 * @author Friedemann
 */
@SuppressWarnings("serial")
public class ControllerGUI extends JPanel {

	/** The Constant PROGRAM_VERSION. */
	private static final String PROGRAM_VERSION = "1.4.1";

	/** The Constant TITLE_SUFFIX. */
	private static final String TITLE_SUFFIX = " - © 2010 Distripedia Team / HTW Berlin";

	/** The Constant DEFAULT_WIDTH. */
	private static final int DEFAULT_WIDTH = 1100;

	/** The Constant DEFAULT_HEIGHT. */
	private static final int DEFAULT_HEIGHT = 750;

	/** The Constant MINIMUM_WIDTH. */
	private static final int MINIMUM_WIDTH = 900;

	/** The Constant MINIMUM_HEIGHT. */
	private static final int MINIMUM_HEIGHT = 750;

	/** The Constant TABLE_COLUMN_COUNT. */
	private static final int TABLE_COLUMN_COUNT = 4;

	/** The Constant TABLE_COLUMN_NAMES. */
	private static final String[] TABLE_COLUMN_NAMES = { "CLIENT IP", "CLIENT ID", "TEST ID",
			"STATUS" };

	// strings
	private static final String SLIDER_TITLE_USER_HTTP = "user http only";
	private static final String SLIDER_TITLE_USER_WSOCKET = "user websocket capable";
	private static final String SLIDER_TITLE_USER_STRATUS = "user stratus capable";
	private static final String SLIDER_TITLE_TAB_MIN = "minimum tab open time (sec)";
	private static final String SLIDER_TITLE_TAB_MAX = "maximum tab open time (sec)";
	private static final String SLIDER_TITLE_READ_MIN = "minimum reading time (sec)";
	private static final String SLIDER_TITLE_READ_MAX = "maximum reading time (sec)";
	private static final String SLIDER_TITLE_TEST_DUR = "test duration (sec)";

	// fields
	/** The controller instance. */
	private final Controller ctrl;

	// layout components
	/** The main frame. */
	private static JFrame mainframe;

	/** The gui. */
	@SuppressWarnings("unused")
	private final ControllerGUI gui;

	/** Panel showing the static controller information. */
	private JPanel panel_info;

	/** Panel showing the connected clients. */
	private JPanel panel_clients;

	/** The panel holding the testing settings. */
	private JPanel panel_testSetup;

	/** The panel with the client controls. */
	private JPanel panel_controls;

	/** The panel_log. */
	private JPanel panel_log;

	/** The table showing the connected clients. */
	private JTable table_clients;

	/** The custom table model for the client list. */
	private ControllerTableModel ctModel;

	/** scrollpane holding the client table list. */
	private JScrollPane scroll_tablePane;

	/** The label depicting the controller ip */
	private JLabel lbl_info;

	/** test case setting sliders */
	private JSlider slider_userHTTP;
	private JSlider slider_userWebSocket;
	private JSlider slider_userStratus;
	private JSlider slider_minTabOpenTime;
	private JSlider slider_maxTabOpenTime;
	private JSlider slider_minReadingTime;
	private JSlider slider_maxReadingTime;
	private JSlider slider_testDuration;

	private JTextField field_webServerAddress;
	private JTextField field_webSocketURI;

	/** The textfield start time. */
	private JTextField field_startTime;

	/** The textfield for the test id. */
	private JTextField field_testID;

	/** The lbl_article list status. */
	private JLabel lbl_articleListStatus;

	/** The button for deploying test tickets. */
	private JButton btn_resetStartTime;

	/** The button for deploying test tickets. */
	private JButton btn_deployTicket;

	/** The panic button. */
	private JButton btn_panic;

	private JButton btn_collectLogFiles;

	/** The logging text area. */
	private JTextArea area_log;

	/**
	 * Instantiates a new client gui.
	 */
	private ControllerGUI() {
		super(new GridBagLayout());

		buildCtrlInfoPanel();
		buildClientPanel();
		buildTestSettingsPanel();
		buildControlPanel();
		buildLogPanel();

		final GridBagConstraints gc = new GridBagConstraints();
		gc.insets = new Insets(3, 3, 3, 3);
		gc.fill = GridBagConstraints.BOTH;

		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 0;
		gc.weighty = 0;
		this.add(panel_info, gc);

		gc.gridy = 1;
		this.add(panel_clients, gc);

		gc.gridy = 2;
		this.add(panel_testSetup, gc);

		gc.gridy = 3;
		this.add(panel_controls, gc);

		// empty panel taking up all space left
		gc.gridy = 4;
		gc.weighty = 1;
		this.add(new JPanel(), gc);

		gc.gridx = 1;
		gc.gridy = 0;
		gc.weightx = 1;
		gc.gridheight = 5;
		gc.weighty = 1;
		this.add(panel_log, gc);

		gui = this;

		logMessage("gui started, initializing controller...");
		ctrl = new Controller(this);

		// set ip address
		lbl_info.setText(ctrl.getOwnIPAddress());

		// set preset values
		setGUIProperties(ctrl.getProps());
	}

	/**
	 * Sets the gui relevant controller properties.
	 *
	 * @param p the new properties
	 */
	private void setGUIProperties(final Properties p) {

		field_webServerAddress.setText(p.getProperty("http_uri", "http://www.example.com/"));
		field_webSocketURI.setText(p.getProperty("websocket_uri", "ws://www.example.com:8383/"));

		slider_userHTTP.setValue(Integer.valueOf(p.getProperty("user_http", "0")));
		slider_userWebSocket.setValue(Integer.valueOf(p.getProperty("user_ws", "0")));
		slider_userStratus.setValue(Integer.valueOf(p.getProperty("user_stratus", "0")));

		slider_minTabOpenTime.setValue(Integer.valueOf(p.getProperty("min_tab_open_time", "0")));
		slider_maxTabOpenTime.setValue(Integer.valueOf(p.getProperty("max_tab_open_time", "0")));
		slider_minReadingTime.setValue(Integer.valueOf(p.getProperty("min_reading_time", "0")));
		slider_maxReadingTime.setValue(Integer.valueOf(p.getProperty("max_reading_time", "0")));

		slider_testDuration.setValue(Integer.valueOf(p.getProperty("test_duration", "0")));
	}

	/**
	 * Builds the ctrl info panel.
	 */
	private void buildCtrlInfoPanel() {

		lbl_info = new JLabel();
		lbl_info.setFont(new Font("Arial", Font.BOLD, 11));

		btn_panic = new JButton(action_panic);
		btn_panic.setBackground(Color.RED);
		btn_panic.setFont(new Font("Arial", Font.BOLD, 12));

		panel_info = new JPanel(new BorderLayout());
		panel_info.add(new JLabel("controller network IP: "), BorderLayout.WEST);
		panel_info.add(lbl_info, BorderLayout.CENTER);
		panel_info.add(btn_panic, BorderLayout.EAST);

		setTitledBorder(panel_info, "controller info");
	}

	/**
	 * Builds the client panel.
	 */
	private void buildClientPanel() {

		panel_clients = new JPanel(new GridLayout());

		ctModel = new ControllerTableModel(GrinderSettings.CONTROLLER_MAX_CLIENTS,
				ControllerGUI.TABLE_COLUMN_COUNT);
		ctModel.setColumnNames(ControllerGUI.TABLE_COLUMN_NAMES);

		table_clients = new JTable(ctModel);

		// set column widths
		final TableColumn column = table_clients.getColumnModel().getColumn(1);
		column.setPreferredWidth(220);

		scroll_tablePane = new JScrollPane(table_clients);
		// table_clients.setFillsViewportHeight(true);

		panel_clients.add(scroll_tablePane);

		panel_clients.setMinimumSize(new Dimension(0, 220));
		setTitledBorder(panel_clients, "connected clients");
	}

	/**
	 * Builds the test settings panel.
	 */
	private void buildTestSettingsPanel() {
		panel_testSetup = new JPanel(new GridLayout(4, 2));

		slider_userHTTP = createTitledSilder(ControllerGUI.SLIDER_TITLE_USER_HTTP + ": 0", 0,
				16000, 0);
		slider_userWebSocket = createTitledSilder(ControllerGUI.SLIDER_TITLE_USER_WSOCKET
				+ ": 0", 0, 16000, 0);
		slider_userStratus = createTitledSilder(ControllerGUI.SLIDER_TITLE_USER_STRATUS + ": 0",
				0, 16000, 0);
		slider_minTabOpenTime = createTitledSilder(ControllerGUI.SLIDER_TITLE_TAB_MIN + ": 0",
				0, 3600, 0);
		slider_maxTabOpenTime = createTitledSilder(ControllerGUI.SLIDER_TITLE_TAB_MAX + ": 0",
				0, 3600, 0);
		slider_minReadingTime = createTitledSilder(ControllerGUI.SLIDER_TITLE_READ_MIN + ": 0",
				0, 3600, 0);
		slider_maxReadingTime = createTitledSilder(ControllerGUI.SLIDER_TITLE_READ_MAX + ": 0",
				0, 3600, 0);
		slider_testDuration = createTitledSilder(ControllerGUI.SLIDER_TITLE_TEST_DUR + ": 0", 0,
				3600, 0);

		panel_testSetup.add(slider_userHTTP);
		panel_testSetup.add(slider_minTabOpenTime);

		panel_testSetup.add(slider_userWebSocket);
		panel_testSetup.add(slider_maxTabOpenTime);

		panel_testSetup.add(slider_userStratus);
		panel_testSetup.add(slider_minReadingTime);

		panel_testSetup.add(slider_testDuration);
		panel_testSetup.add(slider_maxReadingTime);

		panel_testSetup.setMinimumSize(new Dimension(0, 230));
		setTitledBorder(panel_testSetup, "test case setup");
	}

	/**
	 * Builds the control panel.
	 */
	private void buildControlPanel() {
		panel_controls = new JPanel(new GridBagLayout());

		// article list
		final JLabel lbl_articleList = new JLabel("Article list status: ");
		lbl_articleListStatus = new JLabel("not loaded", SwingConstants.CENTER);
		lbl_articleListStatus.setFont(new Font("Arial", Font.BOLD, 12));
		final JButton btn_articleList = new JButton(action_loadArticleList);

		// web server address
		final JLabel lbl_webServerAddress = new JLabel("Webserver address: ");
		field_webServerAddress = new JTextField();

		// web server address
		final JLabel lbl_webSocketAddress = new JLabel("Websocket service address: ");
		field_webSocketURI = new JTextField();

		// test id
		final JLabel lbl_testID = new JLabel("Test id: ");
		field_testID = new JTextField("run01");

		// start time
		final JLabel lbl_startTime = new JLabel("Test start time: ");
		final Format f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		field_startTime = new JTextField(f.format(new Date()));

		btn_resetStartTime = new JButton(action_resetTime);

		btn_deployTicket = new JButton(action_deployTestTicket);

		btn_collectLogFiles = new JButton(action_collectLogs);

		final GridBagConstraints gc = new GridBagConstraints();
		gc.insets = new Insets(2, 2, 2, 2);
		gc.fill = GridBagConstraints.BOTH;

		gc.gridx = 0;
		gc.gridy = 0;
		panel_controls.add(lbl_testID, gc);
		gc.gridx = 1;
		gc.weightx = 0.75;
		panel_controls.add(field_testID, gc);
		gc.gridx = 2;
		gc.weightx = 0;
		panel_controls.add(lbl_articleList, gc);

		gc.gridx = 0;
		gc.gridy = 1;
		panel_controls.add(lbl_webServerAddress, gc);
		gc.gridx = 1;
		panel_controls.add(field_webServerAddress, gc);
		gc.gridx = 2;
		gc.gridheight = 3;
		gc.gridwidth = 1;
		panel_controls.add(lbl_articleListStatus, gc);

		gc.gridx = 0;
		gc.gridy = 2;
		gc.gridheight = 1;
		gc.gridwidth = 1;
		panel_controls.add(lbl_webSocketAddress, gc);
		gc.gridx = 1;
		panel_controls.add(field_webSocketURI, gc);

		gc.gridx = 0;
		gc.gridy = 3;
		panel_controls.add(lbl_startTime, gc);
		gc.gridx = 1;
		panel_controls.add(field_startTime, gc);

		gc.gridx = 0;
		gc.gridy = 4;
		panel_controls.add(btn_resetStartTime, gc);
		gc.gridx = 1;
		panel_controls.add(btn_deployTicket, gc);
		gc.gridx = 2;
		gc.gridwidth = 1;
		panel_controls.add(btn_articleList, gc);

		gc.gridx = 0;
		gc.gridy = 5;
		gc.gridwidth = 3;
		panel_controls.add(btn_collectLogFiles, gc);

		panel_controls.setMinimumSize(new Dimension(500, 200));
		panel_controls.setPreferredSize(new Dimension(500, 200));
		setTitledBorder(panel_controls, "test controls");
	}

	/**
	 * Builds the log panel.
	 */
	private void buildLogPanel() {
		panel_log = new JPanel(new GridLayout());
		area_log = new JTextArea();

		area_log.setEditable(false);
		area_log.setFont(new Font("Courier New", Font.PLAIN, 11));
		area_log.setLineWrap(true);

		// make area scrollable....
		final JScrollPane scroll_areaLog = new JScrollPane(area_log);

		panel_log.add(scroll_areaLog);
		setTitledBorder(panel_log, "log");
	}

	/** The action deploy test ticket. */
	private final Action action_deployTestTicket = new AbstractAction("deploy test ticket!") {

		@Override
		public void actionPerformed(final ActionEvent arg0) {
			if (ctrl.isArticleListLoaded())
			{
				final boolean success = ctrl.deployTestTickets();
				if (!success)
				{
					errorDialog("No clients connected", "There is not a single client connected.");
				}
			} else
			{
				errorDialog("Article list missing", "Please specify a proper article list first.");
			}
		}
	};

	/** The action deploy test ticket. */
	private final Action action_collectLogs = new AbstractAction("manually collect log files") {

		@Override
		public void actionPerformed(final ActionEvent arg0) {
			final int answer = confirmDialog("Manually collect log files",
					"Do you really want to collect the logfiles of all finished clients now?\n" +
					"Please take care of proper logfile documentation.");

			if (answer == JOptionPane.OK_OPTION)
			{
				ctrl.collectClientLogFiles();
			}
		}
	};

	/** The action deploy test ticket. */
	private final Action action_resetTime = new AbstractAction("set time now + 10s") {

		@Override
		public void actionPerformed(final ActionEvent arg0) {
			final Format f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			final Timestamp t = new Timestamp(System.currentTimeMillis() + 10000);
			field_startTime.setText(f.format(new Date(t.getTime())));
		}
	};

	/** The action deploy test ticket. */
	private final Action action_panic = new AbstractAction("PANIC!") {

		@Override
		public void actionPerformed(final ActionEvent arg0) {
			ctrl.panic();
		}
	};

	/** The action for loading an article list. */
	private final Action action_loadArticleList = new AbstractAction("(Re)load article list") {

		@Override
		public void actionPerformed(final ActionEvent arg0) {
			final String filePath = (new FileChooser()).openDialog("Choose article list...",
					"txt");
			if (filePath != null)
			{
				logMessage("Reading in: " + filePath);
				try
				{
					final int articleCount = ctrl.parseArticleList(filePath);
					lbl_articleListStatus.setText(articleCount + " IDs loaded");
				} catch (final FileNotFoundException e)
				{
					logMessage("ERROR - File not found: " + filePath);
				}
			}
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
		ControllerGUI.setNativeLookAndFeel();

		// setup a new frame
		ControllerGUI.mainframe = new JFrame(title + " " + ControllerGUI.PROGRAM_VERSION + " "
				+ ControllerGUI.TITLE_SUFFIX);
		ControllerGUI.mainframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ControllerGUI.mainframe.setSize(ControllerGUI.DEFAULT_WIDTH,
				ControllerGUI.DEFAULT_HEIGHT);
		ControllerGUI.mainframe.setMinimumSize(new Dimension(ControllerGUI.MINIMUM_WIDTH,
				ControllerGUI.MINIMUM_HEIGHT));

		// instantiate a new gui panel here...
		final JComponent guiContentPane = new ControllerGUI();
		guiContentPane.setOpaque(true);

		// ...and connect it to the main frame
		ControllerGUI.mainframe.setContentPane(guiContentPane);

		// resolve the screensize
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension screenSize = toolkit.getScreenSize();

		// center the frame
		ControllerGUI.mainframe.setLocation((screenSize.width - ControllerGUI.mainframe
				.getWidth()) / 2, (screenSize.height - ControllerGUI.mainframe.getHeight()) / 2);
		ControllerGUI.mainframe.setVisible(true);
	}

	/**
	 * Gets the frame.
	 *
	 * @return the frame
	 */
	public Frame getFrame() {
		return ControllerGUI.mainframe;
	}

	/**
	 * Updates table data visualizing connected client's details.
	 */
	public void updateClientTableData() {
		final String[][] clientData = ctrl.gatherClientData(
				GrinderSettings.CONTROLLER_MAX_CLIENTS, ControllerGUI.TABLE_COLUMN_COUNT);
		ctModel.setData(clientData);
	}

	/**
	 * Gets a test ticket canvas with all values except the article list
	 * pre-filled with the values currently entered in the gui.
	 *
	 * @return TestTicket the test ticket
	 * @throws URISyntaxException
	 */
	public TestTicket getTestTicketCanvas() throws URISyntaxException {

		final TestTicket ticket = new TestTicket();

		ticket.setTestID(field_testID.getText());
		ticket.setWebServerURI(new URI(field_webServerAddress.getText()));
		ticket.setWebSocketURI(new URI(field_webSocketURI.getText()));
		ticket.setUserCountHTTP(slider_userHTTP.getValue());
		ticket.setUserCountWebSocket(slider_userWebSocket.getValue());
		ticket.setUserCountStratus(slider_userStratus.getValue());
		ticket.setMinTabOpenTime(slider_minTabOpenTime.getValue());
		ticket.setMaxTabOpenTime(slider_maxTabOpenTime.getValue());
		ticket.setMinReadingTime(slider_minReadingTime.getValue());
		ticket.setMaxReadingTime(slider_maxReadingTime.getValue());
		ticket.setTestDuration(slider_testDuration.getValue());

		// yyyy-mm-dd hh:mm:ss
		final Timestamp ts = Timestamp.valueOf((field_startTime.getText()).trim());
		ticket.setStartTime(ts);

		return ticket;
	}

	/**
	 * Gets the test id.
	 *
	 * @return the test id
	 */
	public String getTestID() {
		return field_testID.getText();
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
	 * Open a Dialog to display an regular message.
	 *
	 * @param title - window title
	 * @param message - the message
	 */
	public void messageDialog(final String title, final String message) {
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
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
		return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE);
	}

	/**
	 * Adds or sets a new titled border around a panel.
	 *
	 * @param panel panel which to surround
	 * @param String the title
	 */
	private void setTitledBorder(final JPanel panel, final String s) {
		final TitledBorder tb = new TitledBorder(BorderFactory.createEtchedBorder(), " " + s
				+ " ", TitledBorder.LEFT, TitledBorder.TOP, new Font("Sans", Font.BOLD, 11),
				Color.BLACK);
		panel.setBorder(tb);
	}

	/**
	 * Builds and returns a titled slider.
	 *
	 * @param string
	 * @param minVal
	 * @param maxVal
	 * @param val
	 * @return
	 */
	private JSlider createTitledSilder(final String string, final int minVal, final int maxVal,
			final int val) {

		final JSlider slider = new JSlider(SwingConstants.HORIZONTAL, minVal, maxVal, val);
		slider.setPreferredSize(new Dimension(650, 45));
		final TitledBorder tb = new TitledBorder(BorderFactory.createEmptyBorder(), string,
				TitledBorder.LEFT, TitledBorder.ABOVE_BOTTOM, new Font("Sans", Font.PLAIN, 11));
		slider.setBorder(tb);
		slider.setMajorTickSpacing((maxVal - minVal) / 5);
		slider.setMinorTickSpacing((maxVal - minVal) / 20);
		slider.setPaintTicks(true);

		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent e) {
				slider_moved(e);
			}
		});
		return slider;
	}

	/**
	 * Method which gets called from the slider's addChange listener.
	 *
	 * @param event
	 */
	private void slider_moved(final ChangeEvent e) {
		final JSlider slider = (JSlider) e.getSource();

		if (slider == slider_userHTTP)
		{
			setSliderTitle(slider, ControllerGUI.SLIDER_TITLE_USER_HTTP + ": "
					+ slider.getValue());
		} else if (slider == slider_userWebSocket)
		{
			setSliderTitle(slider, ControllerGUI.SLIDER_TITLE_USER_WSOCKET + ": "
					+ slider.getValue());
		} else if (slider == slider_userStratus)
		{
			setSliderTitle(slider, ControllerGUI.SLIDER_TITLE_USER_STRATUS + ": "
					+ slider.getValue());
		} else if (slider == slider_minTabOpenTime)
		{
			setSliderTitle(slider, ControllerGUI.SLIDER_TITLE_TAB_MIN + ": " + slider.getValue());
		} else if (slider == slider_maxTabOpenTime)
		{
			setSliderTitle(slider, ControllerGUI.SLIDER_TITLE_TAB_MAX + ": " + slider.getValue());
		} else if (slider == slider_minReadingTime)
		{
			setSliderTitle(slider, ControllerGUI.SLIDER_TITLE_READ_MIN + ": " + slider.getValue());
		} else if (slider == slider_maxReadingTime)
		{
			setSliderTitle(slider, ControllerGUI.SLIDER_TITLE_READ_MAX + ": " + slider.getValue());
		} else if (slider == slider_testDuration)
		{
			setSliderTitle(slider, ControllerGUI.SLIDER_TITLE_TEST_DUR + ": " + slider.getValue());
		}
	}

	/**
	 * Sets a new description on a slider.
	 *
	 * @param slider
	 * @param string
	 */
	private void setSliderTitle(final JSlider slider, final String str) {
		final TitledBorder tb = new TitledBorder(BorderFactory.createEmptyBorder(), str,
				TitledBorder.LEFT, TitledBorder.ABOVE_BOTTOM, new Font("Sans", Font.PLAIN, 11));
		slider.setBorder(tb);
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
