package net.ducksmanager.whattheduck;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.ducksmanager.util.NaturalOrderComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

abstract class ItemAdapter<Item> extends ArrayAdapter<Item> {

    private final ArrayList<Item> items;
    private ArrayList<Item> filteredItems;

    ItemAdapter(List list, ArrayList<Item> items) {
        super(list, R.layout.row_edge, items);
        this.items = items;
        Collections.sort(this.items, getComparator());

        this.filteredItems = new ArrayList<>(items);
    }

    void updateFilteredList(String textFilter) {
        filteredItems = new ArrayList<>();
        for (Item item : items)
            if (getText(item).toLowerCase(Locale.FRANCE).contains(textFilter.toLowerCase()))
                filteredItems.add(item);
    }

    private NaturalOrderComparator<Item> getComparator() {
        return new NaturalOrderComparator<Item>() {
            @Override
            public int compare(Item i1, Item i2) {
                return super.compareObject(getText(i1), getText(i2));
            }
        };
    }

    LayoutInflater getLayoutInflater() {
        return (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    protected View getNewView() {
        return getLayoutInflater().inflate(R.layout.row, null);
    }

    protected void processView(View v, Item i) {
        TextView itemTitle = v.findViewById(R.id.itemtitle);
        itemTitle.setText(getText(i));

        itemTitle.setTypeface(null, isHighlighted(i) ? Typeface.BOLD : Typeface.NORMAL);

        ImageView prefixImage = v.findViewById(R.id.prefiximage);
        if (prefixImage != null) {
            Integer imageResource = getImageResource(i, (Activity) this.getContext());
            if (imageResource == null) {
                prefixImage.setVisibility(View.GONE);
            } else {
                prefixImage.setVisibility(View.VISIBLE);
                prefixImage.setImageResource(imageResource);
            }
        }
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = this.getNewView();
        }

        Item i = getItem(position);
        if (i != null) {
            processView(v, i);
        }
        return v;
    }

    @Override
    public int getCount() {
        return filteredItems.size();
    }

    @Override
    public Item getItem(int position) {
        return filteredItems.get(position);
    }

    protected abstract boolean isHighlighted(Item i);

    protected abstract Integer getImageResource(Item i, Activity activity);

    protected abstract String getText(Item i);

    public ArrayList<Item> getItems() {
        return items;
    }
}