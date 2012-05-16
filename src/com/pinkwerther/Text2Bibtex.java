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
import java.util.HashMap;
import java.util.Vector;

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
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
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
	MutableAttributeSet hlText, cleanText;

	HashMap<String,FieldType> fieldTypes;
	
	boolean paragraph_mode=true, auto_pilot=true, erkennung=true;
	
	public final static String NL=System.getProperty("line.separator");
	
	public final static String[] optionalFields = {
		"keywords"
		};

	class FieldType {
		String description;
		String[] mandatory;
		String[] optional;
		public FieldType(String description,String[] mandatory,String[] optional){
			this.description = description;
			this.mandatory = mandatory;
			this.optional = optional;
		}
		Vector<String> getAllItems(){
			Vector<String> ret = new Vector<String>();
			for (int i = 0; i<mandatory.length; i++)
				ret.add(mandatory[i]);
			for (int i = 0; i<optional.length; i++)
				ret.add(optional[i]);
			for (int i = 0; i<optionalFields.length; i++)
				ret.add(optionalFields[i]);
			return ret; //TODO
		}
	}
	
	class Split {
		String field="",content="";
		public Split(String content) {
			this.content = content;
		}
		public Split(String field, String content) {
			this.field = field;
			this.content = content;
		}
	}
	class Entry {
		String type,key;
		HashMap<String,String> v;
		ArrayList<Split> splitting;//first value the key, second value the string, key is null if string is a filler
		public String splittingJoined() {
			StringBuffer sb = new StringBuffer();
			for (int i=0; i<splitting.size(); i++)
				sb.append(splitting.get(i).content);
			return sb.toString();
		}
		public void select(String field, String input, int start, int end){
			String newcontent=input.substring(start,end);
			v.put(field, newcontent);
			if ( (splitting == null) || (! splittingJoined().equals(input)) ){
				splitting = new ArrayList<Split>();
				if (start>0)
					splitting.add(new Split(input.substring(0,start)));
				splitting.add(new Split(field,newcontent));
				if (end<input.length())
					splitting.add(new Split(input.substring(end)));
				return;
			}
			int i1=-1,i2=-1;
			int pos=0;
			String pre=null,post=null;
			for (int i=0; i<splitting.size(); i++) {
				pos += splitting.get(i).content.length();
				if ((i1<0) && (start<pos)) {
					i1=i;
					int relstart = splitting.get(i).content.length()-(pos-start);
					if (start>0)
						pre=splitting.get(i1).content.substring(0,relstart);
				}
				if ((i2<0) && (end<=pos)) {
					i2=i;
					int relend = splitting.get(i).content.length()-(pos-end);
					if (end<pos)
						post=splitting.get(i).content.substring(relend);
				}
				if (splitting.get(i).field.equals(field))
					splitting.get(i).field="";
			}
			for (int i=i2; i>=i1; i--) {
				if (!splitting.get(i).field.equals("")) {
					v.remove(splitting.get(i).field);
					splitting.get(i).field="";
				}
				splitting.remove(i);
			}
			if(pre!=null) {
				splitting.add(i1,new Split(pre));
				i1++;
			}
			splitting.add(i1,new Split(field,newcontent));
			v.put(field, newcontent);
			i1++;
			if(post!=null)
				splitting.add(i1,new Split(post));
		}
		public void put(String field, String content){
			v.put(field, content);
			if (field.equals("author") || field.equals("year") )
				refactorKey();
		}
		public void refactorKey() {
			key = "";
			String at = v.get("author");
			if (at==null)
				key+=type;
			else {
				String[] strs = v.get("author").split("[-, /]");
				for (int i=0; i<strs.length; i++)
					if (strs[i].matches(".*[a-z].*"))
						continue;
					else
						key+=strs[i];
			}
			at = v.get("year");
			if (at != null)
				key+=at;
		}
		public String get(String field) {
			return v.get(field);
		}
		public void remove(String field) {
			v.remove(field);
		}
		public Entry() {
			this.v = new HashMap<String,String>();
			this.key = "";
			this.type = "";
		}
		public Entry(String type) {
			this();
			this.type = type;
		}
		public Entry(Entry e) {
			this(e.type);
		}
		@Override
		public String toString() {
			StringBuffer ret = new StringBuffer();
			ret.append("@"+type+"{"+key);
			for(String s : v.keySet())
				ret.append(","+NL+"  "+s+" = {"+v.get(s)+"}");
			ret.append(NL+"}"+NL+NL);
			return ret.toString();
		}
		public void updateOriginal() {
			tpOriginal.setCharacterAttributes(cleanText, true);
			int index=0;
			for (int i=0; i<splitting.size(); i++) {
				if ( ! splitting.get(i).field.equals(""))
					docOriginal.setCharacterAttributes(index, splitting.get(i).content.length(), hlText, true);
				index+=splitting.get(i).content.length();
			}
		}
	}
	
	JTextPane tpOriginal, tpImport;
	StyledDocument docOriginal, docImport;
	MutableComboBoxModel fieldBoxes;
	
	public Text2Bibtex() {
		window = this;
		addWindowListener(this);
		
		postponedLines = new ArrayList<String>();
		initializeFieldTypes();
		
		hlText = new SimpleAttributeSet();
		StyleConstants.setBold(hlText, true);
		
		cleanText = new SimpleAttributeSet();
		
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
		
		JComboBox cbType = new JComboBox(fieldTypes.keySet().toArray());
		cbType.setBounds(296, 42, 104, 27);
		panel.add(cbType);
		cbType.setSelectedItem(booS);
		setType(booS);
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
				docOriginal.setCharacterAttributes(0, docOriginal.getLength(), cleanText, true);
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
				entry.updateOriginal();
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
				docOriginal.insertString(docOriginal.getLength(), l, cleanText);
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
				docOriginal.insertString(pos, ns.toString(), cleanText);
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
		entry.updateOriginal();
	}
	public void setType(String t) {
		entry = new Entry(t);
		while (fieldBoxes.getSize()>0)
			fieldBoxes.removeElementAt(0);
		for (String s : fieldTypes.get(t).getAllItems()){
			fieldBoxes.addElement(s);
		}
		//TODO possible update procedures
	}
	public void updateOutput() {
		tpImport.setDocument(new DefaultStyledDocument());
		docImport = tpImport.getStyledDocument();
		try {
			docImport.insertString(0, entry.toString(), cleanText);
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

	public void initializeFieldTypes() {
		fieldTypes = new HashMap<String,FieldType>(14);
		fieldTypes.put(artS,new FieldType(artD,artM,artO));
		fieldTypes.put(booS,new FieldType(booD,booM,booO));
		fieldTypes.put(bokS,new FieldType(bokD,bokM,bokO));
		fieldTypes.put(conS,new FieldType(conD,conM,conO));
		fieldTypes.put(inbS,new FieldType(inbD,inbM,inbO));
		fieldTypes.put(incS,new FieldType(incD,incM,incO));
		fieldTypes.put(inpS,new FieldType(inpD,inpM,inpO));
		fieldTypes.put(manS,new FieldType(manD,manM,manO));
		fieldTypes.put(masS,new FieldType(masD,masM,masO));
		fieldTypes.put(misS,new FieldType(misD,misM,misO));
		fieldTypes.put(phdS,new FieldType(phdD,phdM,phdO));
		fieldTypes.put(proS,new FieldType(proD,proM,proO));
		fieldTypes.put(tecS,new FieldType(tecD,tecM,tecO));
		fieldTypes.put(unpS,new FieldType(unpD,unpM,unpO));
	}
    public final static String artS = "article";
    public final static String artD = "Zeitungs- oder Zeitschriftenartikel";
    public final static String[] artM = {"author", "title", "journal", "year"};
    public final static String[] artO = {"volume", "number", "pages", "month", "note"};
    public final static String booS = "book";
    public final static String booD = "Buch";
    public final static String[] booM = {"author", "editor", "title", "publisher", "year"};
    public final static String[] booO = {"volume", "number", "series", "address", "edition", "month", "note", "isbn"};
    public final static String bokS = "booklet";
    public final static String bokD = "Gebundenes Druckwerk";
    public final static String[] bokM = {"title"};
    public final static String[] bokO = {"author", "howpublished", "address", "month", "year", "note"};
    public final static String conS = "conference";
    public final static String conD = "Wissenschaftliche Konferenz";
    public final static String[] conM = {"author", "title", "booktitle", "year"};
    public final static String[] conO = {"editor", "volume", "number", "series", "pages", "address", "month", "organization", "publisher", "note"};
    public final static String inbS = "inbook";
    public final static String inbD = "Teil eines Buches";
    public final static String[] inbM = {"author", "editor", "title", "booktitle", "chapter", "pages", "publisher", "year"};
    public final static String[] inbO = {"volume", "number", "series", "type", "address", "edition", "month", "note"};
    public final static String incS = "incollection";
    public final static String incD = "Teil eines Buches (z. B. Aufsatz in einem Sammelband) mit einem eigenen Titel";
    public final static String[] incM = {"author", "title", "booktitle", "publisher", "year"};
    public final static String[] incO = {"editor", "volume", "number", "series", "type", "chapter", "pages", "address", "edition", "month", "note"};
    public final static String inpS = "inproceedings";
    public final static String inpD = "Artikel in einem Konferenzbericht";
    public final static String[] inpM = {"author", "title", "booktitle", "year"};
    public final static String[] inpO = {"editor", "volume", "number", "series", "pages", "address", "month", "organization", "publisher", "note"};
    public final static String manS = "manual";
    public final static String manD = "Technische Dokumentation";
    public final static String[] manM = {"address", "title"};
    public final static String[] manO = {"author", "organization", "edition", "month", "year", "note"};
    public final static String masS = "mastersthesis";
    public final static String masD = "Diplom-, Magister- oder andere Abschlussarbeit (außer Promotion)";
    public final static String[] masM = {"author", "title", "school", "year"};
    public final static String[] masO = {"type", "address", "month", "note"};
    public final static String misS = "misc";
    public final static String misD = "beliebiger Eintrag (wenn nichts anderes passt)";
    public final static String[] misM = {};
    public final static String[] misO = {"author", "title", "howpublished", "month", "year", "note"};
    public final static String phdS = "phdthesis";
    public final static String phdD = "Doktor- oder andere Promotionsarbeit";
    public final static String[] phdM = {"author", "title", "school", "year"};
    public final static String[] phdO = {"type", "address", "month", "note"};
    public final static String proS = "proceedings";
    public final static String proD = "Konferenzbericht";
    public final static String[] proM = {"title", "year"};
    public final static String[] proO = {"editor", "volume", "number", "series", "address", "month", "organization", "publisher", "note"};
    public final static String tecS = "techreport";
    public final static String tecD = "veröffentlichter Bericht einer Hochschule oder anderen Institution";
    public final static String[] tecM = {"author", "title", "institution", "year"};
    public final static String[] tecO = {"type", "note", "number", "address", "month"};
    public final static String unpS = "unpublished";
    public final static String unpD = "nicht formell veröffentlichtes Dokument";
    public final static String[] unpM = {"author", "title", "note"};
    public final static String[] unpO = {"month", "year"};

}
