package www.pai.mobi;

import java.util.ArrayList;
import java.util.Hashtable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;


public class IconAdapter extends BaseAdapter {
    private ArrayList<String> Sites     = null;
    private ArrayList<String> Sites_str = null;
    private Hashtable<String, Bitmap> Icons = null;
    
    private ArrayList<String> checked = null;
    
    private LayoutInflater mInflater;
    
    private boolean visiable = false;

    public IconAdapter(Context context, ArrayList<String> s, ArrayList<String>s_str, boolean v) {
        // Cache the LayoutInflate to avoid asking for a new one each time.
        mInflater = LayoutInflater.from(context);

        Sites = s;
        Sites_str = s_str;
        visiable = v;
        
        checked = new ArrayList<String>();

        // create the Hashtable dynamically
        Icons = new Hashtable<String, Bitmap>();
        for ( int i = 0; i < Sites.size(); i++ ) {
            String tmp = Sites_str.get( i );
            if ( "qzone".equals(tmp) ) {
                Icons.put("qzone", BitmapFactory.decodeResource(context.getResources(), R.drawable.qzone));
            } else if ( "renren".equals(tmp) ) {
                Icons.put("renren", BitmapFactory.decodeResource(context.getResources(), R.drawable.renren));
            } else if ( "kaixin".equals(tmp) ) {
                Icons.put("kaixin", BitmapFactory.decodeResource(context.getResources(), R.drawable.kaixin));
            } else if ( "msn".equals(tmp) ) {
                Icons.put("msn", BitmapFactory.decodeResource(context.getResources(), R.drawable.msn));
            } else if ( "douban".equals(tmp) ) {
                Icons.put("douban", BitmapFactory.decodeResource(context.getResources(), R.drawable.douban));
            } else if ( "_360quan".equals(tmp) ) {
                Icons.put("_360quan", BitmapFactory.decodeResource(context.getResources(), R.drawable.i360));
            } else if ( "_51".equals(tmp) ) {
                Icons.put("_51", BitmapFactory.decodeResource(context.getResources(), R.drawable.i51));
            } else if ( "sina".equals(tmp) ) {
                Icons.put("sina", BitmapFactory.decodeResource(context.getResources(), R.drawable.sina));
            } else if ( "_139".equals(tmp) ) {
                Icons.put("_139", BitmapFactory.decodeResource(context.getResources(), R.drawable.i139));
            } else if ( "flickr".equals(tmp) ) {
                Icons.put("flickr", BitmapFactory.decodeResource(context.getResources(), R.drawable.flickr));
            } else if ( "yupoo".equals(tmp) ) {
                Icons.put("yupoo", BitmapFactory.decodeResource(context.getResources(), R.drawable.yupoo));
            } else if ( "yssy".equals(tmp) ) {
                Icons.put("yssy", BitmapFactory.decodeResource(context.getResources(), R.drawable.yssy));
            } else if ( "tianya".equals(tmp) ) {
                Icons.put("tianya", BitmapFactory.decodeResource(context.getResources(), R.drawable.tianya));
            } else if ( "mop".equals(tmp) ) {
                Icons.put("mop", BitmapFactory.decodeResource(context.getResources(), R.drawable.mop));
            } else if ( "tongxue".equals(tmp) ) {
                Icons.put("tongxue", BitmapFactory.decodeResource(context.getResources(), R.drawable.tongxue));
            } else if ( "rygh".equals(tmp) ) {
                Icons.put("rygh", BitmapFactory.decodeResource(context.getResources(), R.drawable.rygh));
            } else if ( "baidu".equals(tmp) ) {
                Icons.put("baidu", BitmapFactory.decodeResource(context.getResources(), R.drawable.baidu));
            } else if ( "baixing".equals(tmp) ) {
                Icons.put("baixing", BitmapFactory.decodeResource(context.getResources(), R.drawable.baixing));
            } else if ( "ucenter".equals(tmp) ) {
                Icons.put("ucenter", BitmapFactory.decodeResource(context.getResources(), R.drawable.ucenter));
            } else if ( "discuz".equals(tmp) ) {
                Icons.put("discuz", BitmapFactory.decodeResource(context.getResources(), R.drawable.discuz));
            } else if ( "addsite".equals(tmp) ) {
                Icons.put("addsite", BitmapFactory.decodeResource(context.getResources(), R.drawable.add));
            } else if ( "multisite".equals(tmp) ) {
                Icons.put("multisite", BitmapFactory.decodeResource(context.getResources(), R.drawable.up));
            } else if ( "savetodisk".equals(tmp) ) {
                Icons.put("savetodisk", BitmapFactory.decodeResource(context.getResources(), R.drawable.save));
            } else {
                // a default icon
                Icons.put(tmp, BitmapFactory.decodeResource(context.getResources(), R.drawable.boto));
            }
        }
    }

    /**
     * The number of items in the list is determined by the number of speeches
     * in our array.
     *
     * @see android.widget.ListAdapter#getCount()
     */
    @Override
    public int getCount() {
        return Sites.size();
    }

    public int getCheckedSize() {
        return checked.size();
    }
    
    public ArrayList<String> getChecked() {
        return this.checked;
    }
    
    /**
     * Since the data comes from an array, just returning the index is
     * sufficent to get at the data. If we were using a more complex data
     * structure, we would return whatever object represents one row in the
     * list.
     *
     * @see android.widget.ListAdapter#getItem(int)
     */
    @Override
    public Object getItem(int position) {
        return position;
    }

    /**
     * Use the array index as a unique id.
     *
     * @see android.widget.ListAdapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Make a view to hold each row.
     *
     * @see android.widget.ListAdapter#getView(int, android.view.View,
     *      android.view.ViewGroup)
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid unneccessary calls
        // to findViewById() on each row.
        ViewHolder holder;

        // When convertView is not null, we can reuse it directly, there is no need
        // to reinflate it. We only inflate a new View when the convertView supplied
        // by ListView is null.
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.all_sites_list, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolder();
            holder.text     = (TextView)  convertView.findViewById(R.id.text);
            holder.icon     = (ImageView) convertView.findViewById(R.id.icon);
            holder.checkbox = (CheckBox)  convertView.findViewById(R.id.checkbox);
            if (!visiable) {
                holder.checkbox.setVisibility(View.GONE);
            }

            holder.checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton button, boolean isSelected) {
                    if (isSelected == true) {
                        checked.add("" + position);
                    } else {
                        checked.remove("" + position);
                    }
                }
            });
            
            convertView.setTag(holder);
        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        holder.text.setText(Sites.get(position));
        String tmp = Sites_str.get(position);
        holder.icon.setImageBitmap(Icons.get(tmp));

        return convertView;
    }

    class ViewHolder {
        TextView  text;
        ImageView icon;
        CheckBox  checkbox;
    }
}