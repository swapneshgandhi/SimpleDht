package edu.buffalo.cse.cse486586.simpledht;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

public class SimpleDhtProvider extends ContentProvider {
	private SQLLiteDBHelper database;
	SQLiteDatabase sqlDB;
	String TAG="provider";
	static final int SERVER_PORT = 10000;
	public String remotePort []= {"11108","11112","11116","11120","11124"};
	public static String myPort;
	public static String Successor;
	public static String Predecessor;
	static public ArrayList <ReplyArray> RepArray; 
	public static Object numDeletes;
	public int rows_del=0;
	static int NumofReplyes=0;
	int NumofNodes=0;
	public static String minNode;
	public static String maxNode;
	String controller = "11108";

	@Override
	public String getType(Uri uri) {
		// You do not need to implement this.
		return null;
	}


	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// You do not need to implement this.
		return 0;
	}

	public void reply_to_Delete(String Recipient, int rows, boolean global){

		Message message;
		if (global){
			Log_dis("DeleteReply "+rows, " :"+NumofNodes);
			message=new Message("DeleteReply",myPort,null,null,NumofNodes);
		}
		else{
			Log_dis("DeleteReply "+rows, " :"+1);
			message=new Message("DeleteReply",myPort,null,null,1);	
		}
		message.delete=rows;
		Wrapper wrap=new Wrapper();
		wrap.Receipient=Recipient;
		wrap.message=message;
		new SenderTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, wrap, null);	
	}

	public void Peer_Delete(String [] values, String Recipient){

		
		Log_dis("Peer Delete", values[0]);
		try{
			String hashVal=(String) genHash(values[0]);
			
			int PreVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(Predecessor)/2)));
			int SuccVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(myPort)/2)));
			int minVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(minNode)/2)));
			int maxVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(maxNode)/2)));

			if (values[0].equals("*")){
				String [] selectArgs = {values[0]};
				int rows=DeletefromDB(selectArgs);
				reply_to_Delete(Recipient, rows,true);
			}

			else if( PreVal>0  && SuccVal < 0 ){
				Log_dis("Peer Delete belomgs to me", values[0]);
				String [] selectArgs = {values[0]};
				int rows=DeletefromDB(selectArgs);
				reply_to_Delete(Recipient, rows,false);
			}

			else if((minVal < 0 || maxVal > 0) && minNode.equals(myPort)){
				Log_dis("Peer Delete belomgs to minNode", values[0]);
				String [] selectArgs = {values[0]};
				int rows=DeletefromDB(selectArgs);
				reply_to_Delete(Recipient, rows,false);
			}

			else{
				Log_dis("Peer Delete belomgs to succ", values[0]);
				InitDeletetheSuccessor(values,Recipient,false);
			}
		}
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int DeletefromDB(String[] selectionArgs) {
		if ( selectionArgs[0].equals("@") || selectionArgs[0].equals("*")){
			int rows =	sqlDB.delete(SQLLiteDBHelper.TABLE_NAME,null,null);
			return rows;
		}

		int rows = sqlDB.delete(SQLLiteDBHelper.TABLE_NAME, SQLLiteDBHelper.Key_column + "=?",selectionArgs);

		return rows;
	}

	@Override
	public int delete(Uri uri,String selection, String[] selectionArgs) {
		int rows;
		String Key=(String) selection;
		String hashVal;
		String [] values = new String [2];  

		try {
			hashVal = genHash(Key);
			Log_dis("Delete ", selection);
			int PreVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(Predecessor)/2)));
			int SuccVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(myPort)/2)));
			int minVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(minNode)/2)));
			int maxVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(maxNode)/2)));

			if( selection.equals("@") ){
				String selectArgs[]={selection}; 

				rows=DeletefromDB(selectArgs);
				return rows;
			}

			else if (selection.equals("*")){
				rows=globalDelete();
				return rows;
			}

			else if (Successor.equals(myPort)){
				//Log.e(Key,hashVal);
				String selectArgs[]={selection};
				rows=DeletefromDB(selectArgs);
				return rows;
			}
		
			else if( PreVal>0  && SuccVal < 0){
				Log_dis("Delete belongs to me", selection);
				String selectArgs[]={selection};
				rows=DeletefromDB(selectArgs);
				return rows;
			}

			else if(minVal < 0 || maxVal > 0 ){
				Log_dis("Delete belongs to minNode", selection);
				values[0]=selection;
				rows=InitDeletetheminNode(values, myPort, true);
				return rows;
			}

			else{
				Log_dis("Delete belongs to successor", selection);
				values[0]=selection;
				rows=InitDeletetheSuccessor(values,myPort,true);
				return rows;
			}
		}
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	private int globalDelete(){

		int del=0;
		String [] Values= new String[2];
		Values[0]="*";
		try{	
			for (int i =0;i<5;i++){
				Message message=new Message("Delete",myPort,Values,null);

				Wrapper wrap=new Wrapper();
				wrap.Receipient=remotePort[i];
				wrap.message=message;
				new SenderTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, wrap, null);
			}	
			synchronized(numDeletes){
				numDeletes.wait();
				Log_dis("delete global","wait over..");
				del=rows_del;
				rows_del=0;
			}
		}
		catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return del;
	}

	public int InitDeletetheSuccessor(String [] Values, String Initiator, boolean recursive )throws InterruptedException{
		Socket socket;
		int del=0;
		try {
			socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
					Integer.parseInt(Successor));
			Message message=new Message("Delete",Initiator,Values,null);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(message);
			Log_dis("delete sent to Successor","waiting now..");

			out.close();    
			socket.close(); 
			if(recursive){
				synchronized(numDeletes){
					numDeletes.wait();
					Log_dis("delete succ","wait over..");
					del=rows_del;
					rows_del=0;
				}
			}
		}
		catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return del;
	}

	public int InitDeletetheminNode(String [] Values, String Initiator, boolean recursive )throws InterruptedException{
		Socket socket;
		int del=0;
		try {
			socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
					Integer.parseInt(minNode));
			Message message=new Message("Delete",Initiator,Values,null);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(message);
			Log_dis("Delete the minNode","waiting now..");

			out.close();    
			socket.close();
			if(recursive){
				synchronized(numDeletes){
					numDeletes.wait();
					Log_dis("delete min","wait over..");
					del=rows_del;
					rows_del=0;
				}
			}
		}
		catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return del;
	}

	public void Peer_insert(Uri uri, String [] values) {

		try{
			String hashVal=(String) genHash(values[0]);

			int PreVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(Predecessor)/2)));
			int SuccVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(myPort)/2)));
			int minVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(minNode)/2)));
			int maxVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(maxNode)/2)));

			if( PreVal > 0  && SuccVal < 0){
				Log_dis("Writing to my content prov ", values[0]);
				InserttoDB(uri,values);
			}

			else if((minVal < 0 || maxVal > 0 ) && minNode.equals(myPort)){
				Log_dis("Writing I am minNode ", values[0]);
				InserttoDB(uri,values);
			}

			else{
				Log_dis("Successor->insert ", values[0]);
				InserttoSuccessor(values);
			}

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		String Key=(String) values.get(SQLLiteDBHelper.Key_column);

		String hashVal;

		try {
			hashVal = genHash(Key);

			int PreVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(Predecessor)/2)));
			int SuccVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(myPort)/2)));
			int minVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(minNode)/2)));
			int maxVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(maxNode)/2)));

			if(Successor.equals(myPort)){
				String [] vals = new String [2];
				vals[0]=(String)values.get(SQLLiteDBHelper.Key_column);
				vals[1]=(String)values.get(SQLLiteDBHelper.Value_Column);
				InserttoDB(uri,vals);
			}

			else if( PreVal>0  && SuccVal < 0){
				String [] vals = new String [2];
				vals[0]=(String)values.get(SQLLiteDBHelper.Key_column);
				vals[1]=(String)values.get(SQLLiteDBHelper.Value_Column);
				InserttoDB(uri,vals);
				Log_dis("ins key belongs to me",vals[0]);
			}

			else if(minVal < 0 || maxVal > 0 ){
				String [] vals = new String [2];
				vals[0]=(String)values.get(SQLLiteDBHelper.Key_column);
				vals[1]=(String)values.get(SQLLiteDBHelper.Value_Column);
				Log_dis("ins Sending to minNode ", vals[0]);
				InserttominNode(vals);
			}

			else{
				String [] vals = new String [2];
				vals[0]=(String)values.get(SQLLiteDBHelper.Key_column);
				vals[1]=(String)values.get(SQLLiteDBHelper.Value_Column);
				InserttoSuccessor(vals);
				Log_dis("ins key sent to successor",vals[0]);
			}
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return uri;
	}


	public void InserttominNode(String [] Values){
		Socket socket;
		try {
			socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
					Integer.parseInt(minNode));
			Message message=new Message("Insert",myPort,Values,null);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(message);
			Log_dis("sent to minNode","");

			out.close();    
			socket.close(); 
		}
		catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}


	public void InserttoSuccessor(String [] Values){
		Socket socket;
		try {
			socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
					Integer.parseInt(Successor));
			Message message=new Message("Insert",myPort,Values,null);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(message);
			Log_dis("sent to Successor","");

			out.close();    
			socket.close(); 
		}
		catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void reply_to_Query(String Recipient, Cursor cur, boolean global){
		ArrayList<ReplyArray> reply = new ArrayList <ReplyArray>();

		cur.moveToFirst();
		int keyIndex = cur.getColumnIndex(SQLLiteDBHelper.Key_column);
		int valueIndex = cur.getColumnIndex(SQLLiteDBHelper.Value_Column);
		Message message;
		while (cur.isAfterLast() == false) 
		{
			ReplyArray rep=new ReplyArray();
			rep.Key  = cur.getString(keyIndex);
			rep.Value  = cur.getString(valueIndex);
			reply.add(rep);
			cur.moveToNext();
		}
		if (global){
			Log.e("global query","num of nodes"+NumofNodes);
			message=new Message("QueryReply",myPort,null,null,NumofNodes);
		}
		else{
			message=new Message("QueryReply",myPort,null,null,1);	
		}
		message.Reply=reply;
		Wrapper wrap=new Wrapper();
		wrap.Receipient=Recipient;
		wrap.message=message;
		new SenderTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, wrap, null);

	}

	public void Peer_Query(String [] values, String Recipient){

		try{
			String hashVal=(String) genHash(values[0]);

			int PreVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(Predecessor)/2)));
			int SuccVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(myPort)/2)));
			int minVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(minNode)/2)));
			int maxVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(maxNode)/2)));

			if (values[0].equals("*")){
				Log_dis("recevied req for global dump","");
				Cursor cursor=QuerytheDB(values[0]);
				reply_to_Query(Recipient, cursor,true);
			}

			else if( PreVal>0  && SuccVal < 0 ){

				Cursor cursor=QuerytheDB(values[0]);
				Log_dis("Replying to query ", values[0]);
				reply_to_Query(Recipient, cursor,false);
			}

			else if((minVal < 0 || maxVal > 0) && minNode.equals(myPort)){
				Cursor cursor=QuerytheDB(values[0]);
				Log_dis("Replying I am minNode ", values[0]);
				reply_to_Query(Recipient, cursor,false);
			}

			else{
				Log_dis("Successor query ", values[0]);
				InitQuerytheSuccessor(values,Recipient,false);
			}
		}
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		Cursor cursor;
		String Key=(String) selection;
		String hashVal;
		String [] values = new String [2];
		try {
			hashVal = genHash(Key);
			Log_dis("Query ",selection);
			int PreVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(Predecessor)/2)));
			int SuccVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(myPort)/2)));
			int minVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(minNode)/2)));
			int maxVal=hashVal.compareTo(genHash(String.valueOf(Integer.parseInt(maxNode)/2)));

			if( selection.equals("@") ){ 
				Log.e("@","@");
				cursor=QuerytheDB(selection);
				return cursor;
			}

			else if (selection.equals("*")){
				Log.e("*","*");
				cursor=globalQuery();
				return cursor;
			}
			else if (Successor.equals(myPort)){
				//Log.e(Key,hashVal);
				cursor=QuerytheDB(selection);
				return cursor;
			}

			else if( PreVal > 0  && SuccVal < 0){
				Log_dis("Que key belongs to me", selection);
				cursor=QuerytheDB(selection);
				return cursor;
			}

			else if(minVal < 0 || maxVal > 0 ){
				values[0]=selection;
				Log_dis("Que key belongs minNode", selection);
				cursor=InitQuerytheminNode(values, myPort, true);
				return cursor;
			}

			else{
				values[0]=selection;
				Log_dis("Que querying Successor", selection);
				cursor=InitQuerytheSuccessor(values,myPort,true);
				return cursor;
			}
		}
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null; 
	}

	private MatrixCursor globalQuery(){
		String[] columns = new String[] {
				SQLLiteDBHelper.Key_column,
				SQLLiteDBHelper.Value_Column
		};

		MatrixCursor  Cur = new MatrixCursor(columns);
		String [] Values = new String [2];
		Values[0]= "*";
		try {
			for (int i=0;i<5;i++){
				Message message=new Message("Query",myPort,Values,null);
				Wrapper wrap=new Wrapper();
				wrap.Receipient=remotePort[i];
				wrap.message=message;
				new SenderTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, wrap, null);
			}	

			synchronized(RepArray){
				Log_dis("sent global dump","waiting now..");	
				RepArray.wait();
				int i=0;
				Log_dis("recvded response","wait over");
				while (i<RepArray.size()){

					Cur.addRow(new Object[] {RepArray.get(i).Key,RepArray.get(i).Value});
					i++;
				}
				Cur.moveToFirst();
				RepArray.clear();
			}
		}

		catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log_dis("replied to glbal query","");
		return Cur;
	}

	public MatrixCursor InitQuerytheSuccessor(String [] Values, String Initiator, boolean recursive )throws InterruptedException{
		Socket socket;
		String[] columns = new String[] {
				SQLLiteDBHelper.Key_column,
				SQLLiteDBHelper.Value_Column
		};

		MatrixCursor  Cur = new MatrixCursor(columns);

		try {
			socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
					Integer.parseInt(Successor));
			Message message=new Message("Query",Initiator,Values,null);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(message);
			Log_dis("Que sent to Successor","waiting now..");

			out.close();    
			socket.close(); 
			if(recursive){
				synchronized(RepArray){
					RepArray.wait();

					int i=0;

					while (i<RepArray.size()){

						Cur.addRow(new Object[] {RepArray.get(i).Key,RepArray.get(i).Value});
						i++;
					}
					RepArray.clear();
				}
				Cur.moveToFirst();
			}

		}
		catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Cur;
	}

	public void Log_dis(String Tag, String msg){
		Log.e(Tag,msg);
		new Display().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, Tag+msg, null);
	}

	private class Display extends AsyncTask<String, String, Void>  {

		@Override
		protected Void doInBackground(String... wrap) {
			publishProgress(wrap[0]);
			return null;
		}


		public void onProgressUpdate(String...messages) {
			SimpleDhtActivity.tv.append(messages[0]+"\n");
		}
	}


	public MatrixCursor InitQuerytheminNode(String [] Values, String Initiator, boolean recursive )throws InterruptedException{
		Socket socket;
		String[] columns = new String[] {
				SQLLiteDBHelper.Key_column,
				SQLLiteDBHelper.Value_Column
		};

		MatrixCursor  Cur = new MatrixCursor(columns);

		try {
			socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
					Integer.parseInt(minNode));
			Message message=new Message("Query",Initiator,Values,null);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(message);
			Log_dis("Query the minNode","waiting now..");

			out.close();    
			socket.close(); 
			if(recursive){
				synchronized(RepArray){
					RepArray.wait();

					int i=0;
					while (i<RepArray.size()){

						Cur.addRow(new String[] {RepArray.get(i).Key,RepArray.get(i).Value});
						i++;
					}
					Cur.moveToFirst();
					RepArray.clear();
				}
			}
		}
		catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Cur;
	}


	public Cursor QuerytheDB(String Selection){

		String [] SelectionArgs = {Selection};

		if ( SelectionArgs[0].equals("@") || SelectionArgs[0].equals("*")){
			Cursor cursor =	sqlDB.query(SQLLiteDBHelper.TABLE_NAME, null, null,null,null,null,null);
			int keyIndex = cursor.getColumnIndex("key");
			int valueIndex = cursor.getColumnIndex("value");

			cursor.moveToFirst();

			while (cursor.isAfterLast() == false){
				String returnKey = cursor.getString(keyIndex);
				String returnValue = cursor.getString(valueIndex);

				Log_dis(returnKey +"-"+returnValue,"-\n");
				cursor.moveToNext();
			}
			return cursor;
		}

		Cursor cursor = sqlDB.query(SQLLiteDBHelper.TABLE_NAME, null, SQLLiteDBHelper.Key_column + "=?",SelectionArgs,null,null,null);
		//Log.e(cursor.getString(1),cursor.getString(2));
		int keyIndex = cursor.getColumnIndex("key");
		int valueIndex = cursor.getColumnIndex("value");

		cursor.moveToFirst();

		while (cursor.isAfterLast() == false){
			String returnKey = cursor.getString(keyIndex);
			String returnValue = cursor.getString(valueIndex);

			Log_dis(returnKey +"-"+returnValue,"-");
			cursor.moveToNext();
		}
		return cursor;
	}

	private String genHash(String input) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		byte[] sha1Hash = sha1.digest(input.getBytes());
		Formatter formatter = new Formatter();
		for (byte b : sha1Hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}

	public void InserttoDB(Uri uri, String [] values) {
		ContentValues vals = new ContentValues();
		vals.put("key",values[0]);
		vals.put("value",values[1]);
		sqlDB.insertWithOnConflict(SQLLiteDBHelper.TABLE_NAME, null, vals, SQLiteDatabase.CONFLICT_REPLACE);
		Log_dis("Inserted " + vals.getAsString("key"), vals.getAsString("value"));
	}

	@Override
	public boolean onCreate() {
		database = new SQLLiteDBHelper(getContext());
		sqlDB=database.getWritableDatabase();

		sqlDB.execSQL("DROP TABLE IF EXISTS " + SQLLiteDBHelper.TABLE_NAME);
		Log.e("Droped Table", SQLLiteDBHelper.TABLE_NAME);  	
		database.onCreate(sqlDB); 
		sqlDB=database.getWritableDatabase();

		TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		myPort = String.valueOf((Integer.parseInt(portStr) * 2));
		RepArray=new ArrayList<ReplyArray>();
		numDeletes=new Object();
		Successor=myPort;
		Predecessor=myPort;
		minNode=myPort;
		maxNode=myPort;
		CreateServerSocket();

		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, null, null);

		return false;
	}

	public void CreateServerSocket(){

		try {
			ServerSocket serverSocket = new ServerSocket(10000);
			Ports ports=new Ports();
			ports.myPort=myPort;
			ports.serversocket=serverSocket;
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ports);
		} catch (IOException e) {
			/*
			 * Log is a good way to debug your code. LogCat prints out all the messages that
			 * Log class writes.
			 * 
			 * Please read http://developer.android.com/tools/debugging/debugging-projects.html
			 * and http://developer.android.com/tools/debugging/debugging-log.html
			 * for more information on debugging.
			 */
			Log.e(TAG, "Can't create a ServerSocket");
			return;
		}
	}

	private class Ports{
		ServerSocket serversocket;
		String myPort;	
	}

	private class Wrapper{
		Message message;
		String Receipient;
	}

	// ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, null, null);

	private class SenderTask extends AsyncTask<Wrapper, Void, Void>  {

		@Override
		protected Void doInBackground(Wrapper... wrap) {
			try {
				Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
						Integer.parseInt(wrap[0].Receipient));

				ObjectOutputStream out;	
				out = new ObjectOutputStream(socket.getOutputStream());
				out.writeObject(wrap[0].message);
				Log.e("SenderTask", "sent out the message "+wrap[0].Receipient);
				//Log.e(TAG, remotePort[i]);
				out.close();    
				socket.close();
			} 
			catch (UnknownHostException e) {
				Log.e(TAG, "ClientTask UnknownHostException");
			} 
			catch (IOException e) {
				Log.e("ClientTask socket IOException","sender");

				//	Log.e("ClientTask socket IOException",e.getMessage());
			}			
			return null;
		}
	}

	private class ServerTask extends AsyncTask<Ports, Message, Void> {
		List <HashCompare> peerList = new ArrayList <HashCompare> ();

		protected Void doInBackground(Ports... ports) {
			ServerSocket serverSocket = ports[0].serversocket;

			if(myPort.equals("11108")){
				controller_init();
			}
			while(true){
				try {	
					Log.e("my port","server is listening");
					Log.e("my port",ports[0].myPort);

					Socket client=serverSocket.accept();

					ObjectInputStream instream = new ObjectInputStream(client.getInputStream());

					Message message;
					message = (Message)instream.readObject();
					instream.close();
					client.close();
					if(message.MessageType.equals("Peers")){
						if(!SimpleDhtProvider.Predecessor.equals(message.Peers[0])){
							handleDataMove(message.Peers[0]);
						}
						SimpleDhtProvider.Predecessor=message.Peers[0];
						SimpleDhtProvider.Successor=message.Peers[1];
						SimpleDhtProvider.minNode=message.Peers[2];
						SimpleDhtProvider.maxNode=message.Peers[3];
						NumofNodes=message.NoofNodes;
						Log.e("my predecessor is",SimpleDhtProvider.Predecessor);
						Log.e("my successor is",SimpleDhtProvider.Successor);

						publishProgress(message);
					}
					else if(message.MessageType.equals("Controller") && !controller.equals("11108")){
						async_dis("controller is back", "");
						message_pinger();
					}
					else if(message.MessageType.equals("Insert")){
						Peer_insert(buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider"), message.Values);
					}

					else if(message.MessageType.equals("InsertMove")){
						InsertMoveHandler(message);
					}
					else if(message.MessageType.equals("Query")){
						Peer_Query(message.Values, message.MsgInitiator);
					}

					else if(message.MessageType.equals("QueryReply")){
						HandleQueryReply(message.Reply, message.NoofNodes);
					}

					else if(message.MessageType.equals("Delete")){
						Peer_Delete(message.Values, message.MsgInitiator);
					}

					else if(message.MessageType.equals("DeleteReply")){
						HandleDeleteReply(message.delete, message.NoofNodes);
					}

					else if (controller.equals(myPort) && message.MessageType.equals("Ping")){
						HashCompare hashcompare;
						try {

							hashcompare = new HashCompare(genHash(String.valueOf(Integer.parseInt(message.MsgInitiator)/2)),message.MsgInitiator);
							peerList.add(hashcompare);
						} catch (NoSuchAlgorithmException e1) {	
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						Find_reply_succ_pred();
					}

				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					async_dis(TAG,"Not able to accpet client");
					async_dis(TAG,e.getLocalizedMessage());

				} 
				catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		private void async_dis(String tag, String msg){
			Log.e(tag,msg);

			Message message= new Message(tag,msg,null,null);
			publishProgress(message);

		}

		public void onProgressUpdate(Message...messages) {

			if(messages[0].MessageType.equals("Peers")){
				String Succ = messages[0].Peers[0];
				String Pred = messages[0].Peers[1];

				SimpleDhtActivity.tv.append(myPort+" successor"+Succ + "\t\n");
				SimpleDhtActivity.tv.append(myPort+" predecessor"+Pred + "\t\n");
			}
			else{
				SimpleDhtActivity.tv.append(messages[0].MessageType+" " + messages[0].MsgInitiator+ "\t\n");
			}
		}	

		public void InsertMoveHandler(Message message){

			int i=0;

			String [] Values = new String [2];
			async_dis("InsertMoveHandler"," "+message.Reply.size());
			while(i<message.Reply.size()){
				Values[0]=message.Reply.get(i).Key ;
				Values[1]=message.Reply.get(i).Value ;
				Peer_insert(buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider"), Values);
				i++;
			}
		}

		public void handleDataMove(String Recipient) {

			ArrayList<ReplyArray> reply = new ArrayList <ReplyArray>();

			Cursor cur = QuerytheDB("*");	
			cur.moveToFirst();
			int keyIndex = cur.getColumnIndex(SQLLiteDBHelper.Key_column);
			int valueIndex = cur.getColumnIndex(SQLLiteDBHelper.Value_Column);

			while (cur.isAfterLast() == false) 
			{
				ReplyArray rep=new ReplyArray();
				rep.Key  = cur.getString(keyIndex);
				rep.Value  = cur.getString(valueIndex);

				try {
					if(rep.Key.compareTo(genHash(String.valueOf(Integer.parseInt(Recipient)/2))) < 0){

						reply.add(rep);
						cur.moveToNext();
					}
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if(reply.size()>0){
				async_dis("moving data","");
				Message message = new Message("InsertMove",myPort, null ,null);
				message.Reply=reply;
				try {
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							Integer.parseInt(Recipient));
					ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
					out.writeObject(message);
					async_dis("InsertMove",Recipient);
					out.close();    
					socket.close();
				} 

				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		}

		public void HandleQueryReply(ArrayList <ReplyArray>arr, int noofnodes){

			NumofReplyes++;
			synchronized(RepArray){
				RepArray.addAll(RepArray.size(), arr);	

				if(NumofReplyes == noofnodes){
					async_dis("HandleQueryReply", String.valueOf(noofnodes));
					NumofReplyes=0;
					RepArray.notify();
				}
			}
		}

		public void HandleDeleteReply(int rows, int noofnodes){

			NumofReplyes++;
			synchronized (numDeletes) {
				rows_del+=rows;	
				
				if(NumofReplyes == noofnodes){
					async_dis("HandleDeleteReply" +" "+rows_del+" ", String.valueOf(noofnodes));
					NumofReplyes=0;
					numDeletes.notify();
				}
			}
		}

		public void Find_reply_succ_pred(){

			Collections.sort(peerList);
			for (int i=0;i<peerList.size();i++){

				if(peerList.size()==1){
					peerList.get(i).Predecessor=peerList.get(i).Port;
					peerList.get(i).Successor=peerList.get(i).Port;
				}

				else if(i==0){
					peerList.get(i).Predecessor=peerList.get((peerList.size()-1)).Port;
					peerList.get(i).Successor=peerList.get((i+1)).Port;
				}
				else if(i==peerList.size()-1) {
					peerList.get(i).Predecessor=peerList.get((i-1)).Port;
					peerList.get(i).Successor=peerList.get(0).Port;	
				}

				else{
					peerList.get(i).Predecessor=peerList.get((i-1)).Port;
					peerList.get(i).Successor=peerList.get((i+1)).Port;
				}
				String [] peers = new String [4];
				peers[0]=peerList.get(i).Predecessor;
				peers[1]=peerList.get(i).Successor;
				peers[2]=peerList.get(0).Port;
				peers[3]=peerList.get(peerList.size()-1).Port;
				Message message=new Message("Peers",myPort,null,peers,peerList.size());
				Log.e("Peers",peers[0]);
				Log.e("Peers",peers[1]);

				try {
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							Integer.parseInt(peerList.get(i).Port));
					ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
					out.writeObject(message);
					Log.e("sent the message",peerList.get(i).Port);
					out.close();    
					socket.close();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					Log.e("IO exception controller",peerList.get(i).Port);
					e.printStackTrace();
				} 
			}

		}

		private void controller_init(){
			Message message=new Message("Controller",myPort,null,null);
			for (int i=1; i<5;i++){
				try {
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							Integer.parseInt(remotePort[i]));
					ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
					out.writeObject(message);
					out.close();    
					socket.close();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		}

	}

	private class ClientTask extends AsyncTask<String, Void, Void>  {

		@Override
		protected Void doInBackground(String... msgs) {
			Log.e(TAG, "client!!");
			controller="11108";
			for (int i=0;i<2;i++){
				try {
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							Integer.parseInt(controller));

					Message message=new Message("Ping",myPort,null,null);
					ObjectOutputStream out;	
					out = new ObjectOutputStream(socket.getOutputStream());
					out.writeObject(message);
					Log.e("pinged the message",controller);
					//Log.e(TAG, remotePort[i]);
					out.close();    
					socket.close();
					break;
				} 
				catch (UnknownHostException e) {
					Log.e(TAG, "ClientTask UnknownHostException");
				} 
				catch (IOException e) {
					Log.e("ClientTask socket IOException",controller);
					if (controller.equals("11108")){
						controller=myPort;
					}
				}
			}
			return null;
		}
	}

	private void message_pinger(){
		controller="11108";
		try {
			Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
					Integer.parseInt(controller));
			Message message=new Message("Ping",myPort,null,null);
			ObjectOutputStream out;	
			out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(message);
			Log.e( "pinged the message to",controller);
			//Log.e(TAG, remotePort[i]);
			out.close();    
			socket.close();
		} 
		catch (UnknownHostException e) {
			Log.e(TAG, "ClientTask UnknownHostException");
		} catch (IOException e) {
			if (controller.equals("11108")){
				controller=myPort;
			}
			Log.e("ClientTask socket IOException",myPort);
			//	Log.e("ClientTask socket IOException",e.getMessage());
		}
	}
	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}
}
