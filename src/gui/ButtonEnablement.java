package gui;

import javax.swing.ButtonModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

public class ButtonEnablement implements DocumentListener {

	private ButtonModel buttonModel;
	private Document document;

	public ButtonEnablement(ButtonModel buttonModel) {
		this.buttonModel = buttonModel;
	}

	public void addDocument(Document document) {
		document.addDocumentListener(this);
		this.document = document;
		documentChanged();
	}

	public void documentChanged() {
		boolean buttonEnabled = false;

		if (document.getLength() > 0) {
			buttonEnabled = true;
		}

		buttonModel.setEnabled(buttonEnabled);
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		documentChanged();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		documentChanged();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		documentChanged();
	}
}
