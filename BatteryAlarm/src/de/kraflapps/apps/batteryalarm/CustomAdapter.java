package de.kraflapps.apps.batteryalarm;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class CustomAdapter extends ArrayAdapter<CustomContact> implements Filterable{


		private LayoutInflater mInflater = null;
	    private Activity activity;
	    private ArrayList<CustomContact> cntArrList = null;
	    private ArrayList<CustomContact> mOriginalValues;
	    private ArrayFilter mFilter;

	    public CustomAdapter(Activity a, ArrayList<CustomContact> items) {
	        super(a, 0, items);
	        cntArrList = items;
	        mOriginalValues = new ArrayList<CustomContact>(cntArrList);
	        activity = a;
	        mInflater = (LayoutInflater) activity
	                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    }
	    
	    @Override
	    public int getCount() {
	    	   return cntArrList.size();
	    	}
	    
	    @Override
	    public CustomContact getItem(int position) {
	        return mOriginalValues.get(position);
	    }

	    public static class ViewHolder {

	        public TextView name;
	        public TextView email;
	        public TextView type;
	    }

	    @Override
	    public Filter getFilter() 
	    {
	        /*Filter myFilter = new Filter() 
	        {
	            @Override
	            protected FilterResults performFiltering(CharSequence constraint) 
	            {
	                FilterResults filterResults = new FilterResults();
	                if(constraint != null) 
	                {
	                	filterResults.values = cntArrList.get(5);
	                    filterResults.count = 1;
	                	//filterResults.values = cntArrList;
	                    //filterResults.count = cntArrList.size();
	                }
	                return filterResults;
	            }

	            @Override
	            protected void publishResults(CharSequence contraint, FilterResults results) 
	            {
	                if(results != null && results.count > 0) 
	                {
	                    notifyDataSetChanged();
	                }
	                else {
	                    notifyDataSetInvalidated();
	                }
	            }
	        };
	        return myFilter;*/
	    	
	    	 if (mFilter == null) {
	             mFilter = new ArrayFilter();
	         }
	         return mFilter;
	    }
	    
	    private class ArrayFilter extends Filter {
	        private Object lock;

	        @Override
	        protected FilterResults performFiltering(CharSequence constraint) {
	            FilterResults results = new FilterResults();

	            if (mOriginalValues == null) {
	                synchronized (lock) {
	                    mOriginalValues = new ArrayList<CustomContact>(cntArrList);
	                }
	            }

	            if (constraint == null || constraint.length() == 0) {
	                synchronized (lock) {
	                    ArrayList<CustomContact> list = new ArrayList<CustomContact>(mOriginalValues);
	                    results.values = list;
	                    results.count = list.size();
	                }
	            } else {
	                final String constraintString = constraint.toString().toLowerCase();

	                ArrayList<CustomContact> values = mOriginalValues;
	                int count = values.size();

	                ArrayList<CustomContact> newValues = new ArrayList<CustomContact>();

	                for (int i = 0; i < count; i++) {
	                    CustomContact item = values.get(i);
	                    if (item.getName().toLowerCase().contains(constraintString)) {
	                        newValues.add(item);
	                    }

	                }

	                results.values = newValues;
	                results.count = newValues.size();
	            }

	            return results;
	        }

	        @SuppressWarnings("unchecked")
	        @Override
	        protected void publishResults(CharSequence constraint, FilterResults results) {

	        if(results.values!=null){
	        cntArrList = (ArrayList<CustomContact>) results.values;
	        }else{
	            cntArrList = new ArrayList<CustomContact>();
	        }
	            if (results.count > 0) {
	                notifyDataSetChanged();
	            } else {
	                notifyDataSetInvalidated();
	            }
	        }
	    }

	
	    
	    
	    
	    @Override
	    public View getView(final int position, View convertView, ViewGroup parent) {

	        ViewHolder holder;

	        if (convertView == null) {

	            holder = new ViewHolder();

	            convertView = mInflater.inflate(R.layout.autocomplete_view,
	                    parent, false);
	            holder.name = (TextView) convertView.findViewById(R.id.cntName);
	            holder.email = (TextView) convertView.findViewById(R.id.cntEmail);
	            holder.type = (TextView) convertView.findViewById(R.id.cntType);

	            convertView.setTag(holder);
	        } else {
	            holder = (ViewHolder) convertView.getTag();
	        }
	        
	        if (position < cntArrList.size()){
	        
	        	CustomContact cnt = cntArrList.get(position);
		        
		        holder.name.setText(cnt.getName());
		        holder.email.setText(cnt.getEmail());
		        holder.type.setText(cnt.getType());


	        }
	        
      	        return convertView;
	    }
	}
