package grinder.controller;

/**
 * The controller for the distripedia grinder. Developed May 2010 for the
 * distripedia semester project @ HTW Berlin.
 *
 * @author Friedemann Kiersch
 * @version 1.0
 */
public class Main {
	/**
	 * Initiate loading of the GUI
	 *
	 * @param args
	 */
	public static void main(final String[] args) {
		final Runnable gui = new Runnable() {
			public void run() {
				ControllerGUI.loadGUI("Grinding Controller");
			}
		};
		// start gui within event dispatch thread
		javax.swing.SwingUtilities.invokeLater(gui);
	}
}
