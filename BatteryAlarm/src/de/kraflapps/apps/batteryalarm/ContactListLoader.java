package de.kraflapps.apps.batteryalarm;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;

public class ContactListLoader extends
		AsyncTask<Void, Void, ArrayList<CustomContact>> {

	public Context context;
	public AsyncResult delegate;
	
	private static final String[] PROJECTION = new String[] {
	    ContactsContract.CommonDataKinds.Email.CONTACT_ID,
	    ContactsContract.Contacts.DISPLAY_NAME,
	    ContactsContract.CommonDataKinds.Email.DATA,
	    ContactsContract.CommonDataKinds.Email.TYPE
	};
	
	public ContactListLoader(Context c){
		super();
		context = c;
	}

	/*@Override
	protected ArrayList<CustomContact> doInBackground(Void... params) {

		ArrayList<CustomContact> contactList = new ArrayList<CustomContact>();

		ContentResolver cr = context.getContentResolver();

		Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
				null, null, null);

		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				String id = cur.getString(cur
						.getColumnIndex(ContactsContract.Contacts._ID));
				String name = cur
						.getString(cur
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

				Cursor emailCur = cr.query(
						ContactsContract.CommonDataKinds.Email.CONTENT_URI,
						null, ContactsContract.CommonDataKinds.Email.CONTACT_ID
								+ " = ?", new String[] { id }, null);
				while (emailCur.moveToNext()) {
					String email = emailCur
							.getString(emailCur
									.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
					int emailTypeNum = emailCur
							.getInt(emailCur
									.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
					String emailType = getEmailType(emailTypeNum);

					CustomContact cstCnt = new CustomContact(name, email,
							emailType);
					contactList.add(cstCnt);
				}
				emailCur.close();

				
			}

		}
		return contactList;
	}*/
	
	@Override
	protected ArrayList<CustomContact> doInBackground(Void... params) {

		ArrayList<CustomContact> contactList = new ArrayList<CustomContact>();

		ContentResolver cr = context.getContentResolver();
		
		Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, PROJECTION, null, null, null);
		
		if (cursor != null) {
		    try {
		        final int contactIdIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID);
		        final int displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
		        final int emailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
		        final int emailTypeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE);
		        long contactId;
		        String displayName, address, emailType;
		        while (cursor.moveToNext()) {
		            contactId = cursor.getLong(contactIdIndex);
		            displayName = cursor.getString(displayNameIndex);
		            address = cursor.getString(emailIndex);
		            emailType = cursor.getString(emailTypeIndex);
		            CustomContact cstCnt = new CustomContact(displayName, address,
							emailType);
					contactList.add(cstCnt);
		        }
		    } finally {
		        cursor.close();
		    }
		}
		return contactList;
	}
	
	@Override
	protected void onPostExecute(ArrayList<CustomContact> result) {
		delegate.asyncTaskContactLoaderFinished(result);
	}

	private String getEmailType(int emailTypeNum) {
		String typeString = null;
		
		switch (emailTypeNum) {
		case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
			typeString = context.getString(R.string.mail_type_home);
			break;
		case ContactsContract.CommonDataKinds.Email.TYPE_MOBILE:
			typeString = context.getString(R.string.mail_type_mobile);
			break;
		case ContactsContract.CommonDataKinds.Email.TYPE_OTHER:
			typeString = context.getString(R.string.mail_type_other);
			break;
		case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
			typeString = context.getString(R.string.mail_type_work);
			break;
		}

		return typeString;
	}
}
