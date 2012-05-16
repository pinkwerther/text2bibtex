package com.pinkwerther;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JTextPane;
import javax.swing.text.StyledDocument;

public class Entry {
	
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
	public final static String NL=System.getProperty("line.separator");

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
	public void updateOriginal(JTextPane tpOriginal, StyledDocument docOriginal) {
		tpOriginal.setCharacterAttributes(StaticValues.cleanText, true);
		int index=0;
		for (int i=0; i<splitting.size(); i++) {
			if ( ! splitting.get(i).field.equals(""))
				docOriginal.setCharacterAttributes(index, splitting.get(i).content.length(), StaticValues.getFieldHL(splitting.get(i).field), true);
			index+=splitting.get(i).content.length();
		}
	}
}

