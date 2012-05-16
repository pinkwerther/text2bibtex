package com.pinkwerther;

import java.awt.Color;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;


public class StaticValues {
	public StaticValues() {
		initializeFieldTypes();
		initializeFieldColors();
		initializeHighlightings();
	}
	
	private final static String HL_MARKUP="markup",HL_CLEAN="clean";
	public final static MutableAttributeSet
		hlText  = getHighlighting(HL_MARKUP),
		cleanText = getHighlighting(HL_CLEAN);
	private static HashMap<String,MutableAttributeSet> hl_sets = new HashMap<String,MutableAttributeSet>();
	public static MutableAttributeSet getFieldHL(String f) {
		MutableAttributeSet ret = hl_sets.get(f);
		if (ret != null)
			return ret;
		if (f.equals(HL_CLEAN))
			return cleanText;
		return hlText;
	}
	private final static MutableAttributeSet getHighlighting(String t) {
		MutableAttributeSet ret = new SimpleAttributeSet();
		if (t.equals(HL_MARKUP))
			StyleConstants.setBold(ret, true);
		return ret;
	}
	private static void initializeHighlightings() {
		HashMap<String,MutableAttributeSet> hl_sets = new HashMap<String,MutableAttributeSet>();
		MutableAttributeSet atr;
		for (String s : fieldColors.keySet()) {
			atr = getHighlighting(HL_MARKUP);
			StyleConstants.setForeground(atr, fieldColors.get(s));
			hl_sets.put(s, atr);
		}
	}
	
	private static void initializeFieldColors() {
		/*
		int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
		for (String s : fieldColors.keySet()) {
			max = Math.max(max, s.hashCode());
			min = Math.min(min, s.hashCode());
		}
		double factor = ((double)Integer.MAX_VALUE-(double)Integer.MIN_VALUE)/((double)max-(double)min);
		*/
		for (String s : fieldColors.keySet()) {
			//double pv = factor*((double)s.hashCode()-(double)min);
			//int v = Integer.MIN_VALUE + (int)pv;
			//fieldColors.put(s,new Color(v));
			fieldColors.put(s, new Color(s.hashCode()|0xff000000));
		}
	}
	
	public final static String NL=System.getProperty("line.separator");
	public static HashMap<String,FieldType> fieldTypes;
	public static String defaultType,defaultField;
	private void initializeFieldTypes() {
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
		defaultType = "book";
		defaultField = "title";
	}
	public final static String[] optionalFields = {
		"keywords"
		};
	private static HashMap<String,Color> fieldColors = new HashMap<String,Color>();
	class FieldType {
		String description;
		String[] mandatory;
		String[] optional;
		public FieldType(String description,String[] mandatory,String[] optional){
			this.description = description;
			this.mandatory = mandatory;
			this.optional = optional;
			for (String s : mandatory)
				fieldColors.put(s,null);
			for (String s : optional)
				fieldColors.put(s, null);
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
