package edu.buffalo.cse.cse486586.simpledht;

import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;

public class HashCompare implements Comparable <HashCompare>{

	public Socket socket;
	public String Port;
	public String Successor;
	public String Predecessor;
	public String hash;
	
	public HashCompare(String Hashval, String Port){		
		this.hash=Hashval;
		this.Port=Port;
	}

	@Override
	public int compareTo(HashCompare another) {
		// TODO Auto-generated method stub
		return this.hash.compareTo(another.hash);
	}
	
}

class ReplyArray implements Serializable{
	private static final long serialVersionUID = 1L;
	String Key;
	String Value;
}

class Message implements Serializable{
	private static final long serialVersionUID = 1L;
	String MessageType;
	String MsgInitiator;
	String [] Values;
	ArrayList <ReplyArray> Reply;
	String [] Peers;
	int delete;
	int NoofNodes=1;

	public Message(String MessageType, String MsgInitiator, String [] Values, String [] Peers){
		this.MessageType=MessageType;
		this.MsgInitiator=MsgInitiator;
		
		this.Values=Values;			
		this.Peers=Peers;
	}
	
	public Message(String MessageType, String MsgInitiator, String [] Values, String [] Peers, int nodes){
		this.MessageType=MessageType;
		this.MsgInitiator=MsgInitiator;
		
		this.Values=Values;			
		this.Peers=Peers;
		this.NoofNodes=nodes;
	}
}


