package grinder.util;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;

/**
 * Class providing a File-Opener dialog.
 *
 * @author Friedemann
 */
public class FileChooser {

	/**
	 * Open a dialog for selecting a file.
	 *
	 * @param title the dialog's title
	 * @param ext the ext
	 * @return String - the file name
	 */
	public String openDialog(final String title, final String ext) {

		// open filedialog
		FileDialog fd = new FileDialog(new Frame(), title, FileDialog.LOAD);
		fd.setFile("*." + ext);
		fd.setVisible(true);

		// wait for file
		String selectedItem = fd.getFile();

		if (selectedItem == null)
			return null;

		// get full path
		selectedItem = fd.getDirectory() + File.separator + selectedItem;

		return selectedItem;
	}
}