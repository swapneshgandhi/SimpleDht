package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;



	public class LocalDumpListner implements OnClickListener {

		private final TextView mTextView;
		private final ContentResolver mContentResolver;
		private final Uri mUri;
		public String remotePort []= {"11108","11112","11116","11120","11124"};	
		
		private Uri buildUri(String scheme, String authority) {
			Uri.Builder uriBuilder = new Uri.Builder();
			uriBuilder.authority(authority);
			uriBuilder.scheme(scheme);
			return uriBuilder.build();
		}

		public LocalDumpListner (TextView _tv, ContentResolver _cr){
			mTextView = _tv;
			mContentResolver = _cr;
			mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
		}

		@Override
		public void onClick(View v) {
			Cursor resultCursor = mContentResolver.query(mUri, null,
					"@", null, null);
			Log.e("Recvd local dump op","");
			int keyIndex = resultCursor.getColumnIndex("key");
			int valueIndex = resultCursor.getColumnIndex("value");
			
			resultCursor.moveToFirst();
			
			while (resultCursor.isAfterLast() == false){
			String returnKey = resultCursor.getString(keyIndex);
			String returnValue = resultCursor.getString(valueIndex);
			
			mTextView.append(returnKey +" "+returnValue+"\n");
			resultCursor.moveToNext();
			}
		}
}
