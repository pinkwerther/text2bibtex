package com.pinkwerther;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.MutableComboBoxModel;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

public class Text2Bibtex extends JFrame implements WindowListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7020203300050028115L;

	JFileChooser fc;
	JFrame window;
	DataInputStream filestream;
	BufferedReader filereader;
	File outfile;
	Entry entry;
	ArrayList<String> postponedLines;
	

	boolean paragraph_mode=true, auto_pilot=true, erkennung=true;
	
	JTextPane tpOriginal, tpImport;
	StyledDocument docOriginal, docImport;
	MutableComboBoxModel fieldBoxes;
	
	public static StaticValues V;
	
	public Text2Bibtex() {
		V = new StaticValues();
		
		window = this;
		addWindowListener(this);
		
		postponedLines = new ArrayList<String>();
		
		setTitle("Text to Bibtex Conversion tool");
		
		JPanel panel = new JPanel();
		getContentPane().add(panel);

		panel.setLayout(null);
		
		tpOriginal = new JTextPane();
		docOriginal = tpOriginal.getStyledDocument();
		tpOriginal.setBounds(10, 42, 285, 150);
		panel.add(tpOriginal);
		
		tpImport = new JTextPane();
		docImport = tpImport.getStyledDocument();
		tpImport.setEditable(false);
		tpImport.setBounds(10, 204, 285, 131);
		panel.add(tpImport);
		
		JButton btnSeparate = new JButton("Trennung einf\u00FCgen");
		btnSeparate.setBounds(10, 343, 156, 29);
		panel.add(btnSeparate);
		btnSeparate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tpOriginal.select(tpOriginal.getCaretPosition(), tpOriginal.getWidth());
				postponedLines.add(0, tpOriginal.getSelectedText());
				tpOriginal.replaceSelection("");
			}
		});
		
		JButton btnInfile = new JButton("Textdatei festlegen");
		btnInfile.setBounds(10, 6, 156, 29);
		panel.add(btnInfile);
		btnInfile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (fc == null)
					fc = new JFileChooser();
				int returnVal = fc.showOpenDialog(window);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File infile = fc.getSelectedFile();
		            try {
		            	if (filestream != null)
		            		filestream.close();
						filestream = new DataInputStream(new FileInputStream(infile));
						filereader = new BufferedReader(new InputStreamReader(filestream,"UTF8"));
						appendParagraph();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        } else {
		        }
			}
		});
		
		JButton btnOutfile = new JButton("Ausgabedatei festlegen");
		btnOutfile.setBounds(215, 6, 185, 29);
		panel.add(btnOutfile);
		btnOutfile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (fc == null)
					fc = new JFileChooser();
				int returnVal = fc.showOpenDialog(window);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
		            outfile = fc.getSelectedFile();
		        } else {
		        }
			}
		});
		
		JButton btnJoin = new JButton("Mehr Text holen");
		btnJoin.setBounds(158, 343, 137, 29);
		panel.add(btnJoin);
		btnJoin.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				appendParagraph();
			}
		});
		
		JButton btnImport = new JButton("Import");
		btnImport.setBounds(296, 306, 104, 29);
		panel.add(btnImport);
		btnImport.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if ( (filestream == null) || (outfile == null) || (entry == null))
					return;
				exportEntry();
				clear();
				appendParagraph();
			}
		});
		
		fieldBoxes = new DefaultComboBoxModel();//new JComboBox(fieldTypes.get(booS).getAllItems());
		JComboBox cbField = new JComboBox(fieldBoxes);
		cbField.setBounds(296, 96, 104, 29);
		panel.add(cbField);
		cbField.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				setField((String)((JComboBox)e.getSource()).getSelectedItem());
			}
		});
		
		JComboBox cbType = new JComboBox(StaticValues.fieldTypes.keySet().toArray());
		cbType.setBounds(296, 42, 104, 27);
		panel.add(cbType);
		cbType.setSelectedItem(StaticValues.defaultType);
		setType(StaticValues.defaultType);
		cbType.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				setType((String)((JComboBox)e.getSource()).getSelectedItem());
			}
		});
				
		JButton btnFieldReset = new JButton("Feld-Reset");
		btnFieldReset.setBounds(296, 122, 104, 29);
		panel.add(btnFieldReset);
		btnFieldReset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String s = (String)fieldBoxes.getSelectedItem();
				entry.remove(s);
				updateOutput();
			}
		});
		
		JButton btnReset = new JButton("Reset");
		btnReset.setBounds(296, 148, 104, 29);
		panel.add(btnReset);
		btnReset.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				entry = new Entry(entry);
				docOriginal.setCharacterAttributes(0, docOriginal.getLength(), StaticValues.cleanText, true);
				updateOutput();
			}
		});
		
		JCheckBox chckbxErkennung = new JCheckBox("Erkennung");
		chckbxErkennung.setSelected(true);
		chckbxErkennung.setBounds(296, 204, 104, 23);
		panel.add(chckbxErkennung);
		chckbxErkennung.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e) {
		        if (e.getStateChange() == ItemEvent.DESELECTED)
		            erkennung = false;
		        else
		        	erkennung = true;
			}
		});
		
		JCheckBox chckbxAutopilot = new JCheckBox("Autopilot");
		chckbxAutopilot.setSelected(true);
		chckbxAutopilot.setBounds(296, 229, 104, 23);
		panel.add(chckbxAutopilot);
		chckbxAutopilot.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e) {
		        if (e.getStateChange() == ItemEvent.DESELECTED)
		            auto_pilot = false;
		        else
		        	auto_pilot = true;
			}
		});
		
		JButton btnSkip = new JButton("\u00DCberspringen");
		btnSkip.setBounds(296, 276, 104, 29);
		panel.add(btnSkip);
		btnSkip.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				clear();
				appendParagraph();
			}
		});
		
		JButton btnExit = new JButton("Exit");
		btnExit.setBounds(325, 343, 75, 29);
		panel.add(btnExit);
		btnExit.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
	            Text2Bibtex.this.processWindowEvent(
	                    new WindowEvent(
	                          Text2Bibtex.this, WindowEvent.WINDOW_CLOSED));
			}
		});
		
		JButton btnUpdate = new JButton("Update");
		btnUpdate.setBounds(296, 70, 104, 29);
		panel.add(btnUpdate);
		btnUpdate.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				updateOutput();
				entry.updateOriginal(tpOriginal,docOriginal);
			}
		});
		
		JCheckBox chckbxParagraphed = new JCheckBox("Paragraphed");
		chckbxParagraphed.setSelected(true);
		chckbxParagraphed.setBounds(296, 252, 104, 23);
		panel.add(chckbxParagraphed);
		chckbxParagraphed.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e) {
		        if (e.getStateChange() == ItemEvent.DESELECTED)
		            paragraph_mode = false;
		        else
		        	paragraph_mode = true;
			}
		});

		setSize(400, 400);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	       
	}
	
	public void windowOpened(WindowEvent e){}
	public void windowClosing(WindowEvent e){}

	    //the event that we are interested in
	public void windowClosed(WindowEvent we){
		if (filestream != null)
			try {
				filestream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    System.exit(0);
	}

	public void windowIconified(WindowEvent e){}
	public void windowDeiconified(WindowEvent e){}
	public void windowActivated(WindowEvent e){}
	public void windowDeactivated(WindowEvent e){}
	
	public void exportEntry() {
		try {
			if ( (entry != null) && (outfile != null) ) {
				BufferedWriter out = new BufferedWriter(new FileWriter(outfile)); 
				out.write(entry.toString()); 
				out.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void clear() {
		entry = new Entry(entry);
		tpImport.setDocument(new DefaultStyledDocument());
		docImport = tpImport.getStyledDocument();
		tpOriginal.setDocument(new DefaultStyledDocument());
		docOriginal = tpOriginal.getStyledDocument();
	}
	public void appendParagraph() {
		//TODO
		if (!postponedLines.isEmpty()) {
			String l = postponedLines.remove(0);
			//tpOriginal.setC//setCaretPosition(tpOriginal.getText().length());
			try {
				docOriginal.insertString(docOriginal.getLength(), l, StaticValues.cleanText);
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				String str = filereader.readLine();
				while (emptyString(str))
					str = filereader.readLine();
				StringBuffer ns = new StringBuffer();
				ns.append(str);
				while ( ! emptyString(str)) {
					str = filereader.readLine();
					ns.append(str);
				}
				int pos = docOriginal.getLength();
				docOriginal.insertString(pos, ns.toString(), StaticValues.cleanText);
				tpOriginal.setCaretPosition(pos);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public static boolean emptyString(String s) {
		if (s==null)
			return true;
		if (s.isEmpty())
			return true;
		if (s.matches(" *"))
			return true;
		return false;
	}
	public void setField(String f) {
		if (entry == null)
			entry = new Entry();
		entry.select(f, tpOriginal.getText(),tpOriginal.getSelectionStart(), tpOriginal.getSelectionEnd());
		entry.updateOriginal(tpOriginal,docOriginal);
	}
	public void setType(String t) {
		entry = new Entry(t);
		while (fieldBoxes.getSize()>0)
			fieldBoxes.removeElementAt(0);
		for (String s : StaticValues.fieldTypes.get(t).getAllItems()){
			fieldBoxes.addElement(s);
		}
		//TODO possible update procedures
	}
	public void updateOutput() {
		tpImport.setDocument(new DefaultStyledDocument());
		docImport = tpImport.getStyledDocument();
		try {
			docImport.insertString(0, entry.toString(), StaticValues.cleanText);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Text2Bibtex ex = new Text2Bibtex();
                ex.setVisible(true);
            }
        });
	}


}
