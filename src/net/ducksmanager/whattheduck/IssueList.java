package net.ducksmanager.whattheduck;

import java.util.ArrayList;
import java.util.Locale;

import net.ducksmanager.inducks.coa.CoaListing;
import net.ducksmanager.inducks.coa.CoaListing.ListType;
import net.ducksmanager.whattheduck.Collection.CollectionType;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class IssueList extends List {
    protected static final int ACTIVITY_LISTENUMEROS=2;
    Issue selectedIssue = null;

    private ArrayList<Issue> issues = null;
    private IssueAdapter issueAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (type.equals(CollectionType.USER.toString())) {
	        setTitle(getString(R.string.my_collection)
	        		+">"+CoaListing.getCountryFullName(getCollection().getSelectedCountry())
	        		+">"+CoaListing.getPublicationFullName(getCollection().getSelectedCountry(), 
	        											   getCollection().getSelectedPublication()));
	        show();
        }
        else {
        	setTitle(getString(R.string.insert_issue_menu)+">"
        			+CoaListing.getCountryFullName(getCollection().getSelectedCountry())+">"
        			+CoaListing.getPublicationFullName(getCollection().getSelectedCountry(), 
							   						   getCollection().getSelectedPublication()));
    		new CoaListing(this, ListType.ISSUE_LIST, R.id.progressBarLoading, getCollection().getSelectedCountry(), getCollection().getSelectedPublication()).execute(new Object[0]);
        	
        }
    }
    
    public void show() {
        show(getCollection().getIssueList(getCollection().getSelectedCountry(), 
		   								  getCollection().getSelectedPublication(), 
		   								  this.type),
		   								  Boolean.FALSE);
    }
    
    public void show(ArrayList<Issue> issues, Boolean useless) {
    	this.issues = issues;

        this.issueAdapter = new IssueAdapter(this, R.layout.row, this.issues);
        setListAdapter(this.issueAdapter);
        
    	if (issues.size() > 20) {
    		EditText filterEditText = (EditText) this.findViewById(R.id.filter);
    		filterEditText.setVisibility(EditText.VISIBLE);
    		filterEditText.requestFocus();
    		
    		filterEditText.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) { }
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                	String typedText = s.toString();
                	ArrayList<Issue> filteredIssues = new ArrayList<Issue>();
                	for (Issue issue : IssueList.this.issues)
                		if (issue.getIssueNumber().replace("* ", "").toLowerCase(Locale.FRANCE).contains(typedText.toLowerCase()))
                			filteredIssues.add(issue);

                    IssueList.this.issueAdapter = new IssueAdapter(IssueList.this, R.layout.row, filteredIssues);
                    setListAdapter(IssueList.this.issueAdapter);
                }
            });
    	}
    }
    
    @Override
    public void onBackPressed() {
    	Intent i = new Intent(WhatTheDuck.wtd, PublicationList.class);
        i.putExtra("type", type);
        startActivity(i);
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case INSERT_ID:
            	WhatTheDuck.coaCollection.setSelectedCountry(getCollection().getSelectedCountry());
            	WhatTheDuck.coaCollection.setSelectedPublication(getCollection().getSelectedPublication());
                Intent i = new Intent(WhatTheDuck.wtd, IssueList.class);
                i.putExtra("type", CollectionType.COA.toString());
                startActivity(i);
                return super.onMenuItemSelected(featureId, item, true);
        }
        return super.onMenuItemSelected(featureId, item, false);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (type.equals(CollectionType.COA.toString())) {
        	selectedIssue = (Issue) this.getListView().getItemAtPosition(((Long)id).intValue());
        	if (selectedIssue.getIssueNumber().startsWith("* "))
        		WhatTheDuck.wtd.info(this, R.string.input_error__issue_already_possessed);
        	else {
        		final CharSequence[] items = {getString(R.string.condition_bad), getString(R.string.condition_notsogood), getString(R.string.condition_good)};

        		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        		builder.setTitle(getString(R.string.insert_issue__confirm,selectedIssue.getIssueNumber()))
        			   .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
        				   public void onClick(DialogInterface dialog, int item) {}
        		    	})
		        	   .setCancelable(true)
        		       .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
        		           public void onClick(DialogInterface dialog, int id) {
        		        	   dialog.dismiss();                
        		        	   int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
        		        	   if (selectedPosition == -1) {
        		        			WhatTheDuck.wtd.info(IssueList.this, R.string.input_error__select_condition);
        		                	return;
        		        	   }
        		        	   String condition = items[selectedPosition].toString();
        		        	   String DMcondition = null;
        		        	   if (condition.equals(getString(R.string.condition_bad)))
        		        		   DMcondition = Issue.BAD_CONDITION;
        		        	   else if (condition.equals(getString(R.string.condition_notsogood)))
        		        		   DMcondition = Issue.NOTSOGOOD_CONDITION;
        		        	   else
        		        		   DMcondition = Issue.GOOD_CONDITION;
        		        	   selectedIssue.setIssueCondition(Issue.issueConditionStrToIssueCondition(DMcondition));
        		        	   new AddIssue(IssueList.this, R.id.progressBarInsert, getCollection().getSelectedPublication(), selectedIssue).execute(new Object[0]);
        		           }
        		       })
        		       .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
        		           public void onClick(DialogInterface dialog, int id) {
        		                dialog.cancel();
        		           }
        		       });
        		builder.create().show();
        	}
        }
    	super.onListItemClick(l, v, position, id);
    }

    private void saveState() {
    }
}
