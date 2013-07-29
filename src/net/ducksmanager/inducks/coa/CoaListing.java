package net.ducksmanager.inducks.coa;


import java.util.HashMap;
import java.util.Iterator;

import net.ducksmanager.whattheduck.CountryList;
import net.ducksmanager.whattheduck.Issue;
import net.ducksmanager.whattheduck.IssueList;
import net.ducksmanager.whattheduck.List;
import net.ducksmanager.whattheduck.PublicationList;
import net.ducksmanager.whattheduck.R;
import net.ducksmanager.whattheduck.WhatTheDuck;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

public class CoaListing extends AsyncTask {
	public static List displayedList;
	public static enum ListType {COUNTRY_LIST, PUBLICATION_LIST, ISSUE_LIST};
	public static HashMap<String,String> countryNames=new HashMap<String,String>();
	public static HashMap<String,HashMap<String,String>> publicationNames=new HashMap<String,HashMap<String,String>>();

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private int progressBarId;
	private ListType listType;
	private String countryShortName;
	private String publicationShortName;
	
	
	public CoaListing(List list, ListType type, int progressBarId,  String countryShortName, String publicationShortName) {
		displayedList = list; 
		this.listType = type;
		this.progressBarId = progressBarId;
		this.countryShortName = countryShortName;
		this.publicationShortName = publicationShortName;
	}
	
	public static String getCountryFullName (String shortCountryName) {
		return countryNames.get(shortCountryName);
	}
	
	public static String getPublicationFullName (String shortCountryName, String shortPublicationName) {
		if (publicationNames.get(shortCountryName) == null) {
			System.out.println("Can't get publications of country "+shortCountryName);
		}
		return publicationNames.get(shortCountryName).get(shortPublicationName);
	}
	
	public static String getCountryShortName(String fullCountryName) {
		for (String shortCountryName : countryNames.keySet()) {
			if (countryNames.get(shortCountryName).equals(fullCountryName))
				return shortCountryName;
		}
		return null;
	}
	
	public static String getPublicationShortName (String shortCountryName, String fullPublicationName) {
		HashMap<String,String> countryPublications = publicationNames.get(shortCountryName);
		for (String shortPublicationName : countryPublications.keySet()) {
			if (countryPublications.get(shortPublicationName).equals(fullPublicationName))
				return shortPublicationName;
		}
		return null;
	}
		
	public static void resetCountries() {
		countryNames=new HashMap<String,String>();
	}
	
	public static void resetPublications() {
		publicationNames=new HashMap<String,HashMap<String,String>>();
	}
	
	
	public static void addCountry(String shortName, String fullName) {
		countryNames.put(shortName, fullName);
	}
	
	public static void addPublication(String countryAndPublicationShortNames, String fullName) {
		String country=countryAndPublicationShortNames.split("/")[0];
		addPublication(country, countryAndPublicationShortNames, fullName);
	}
	
	public static void addPublication(String countryShortName, String shortName, String fullName) {
		if (publicationNames.get(countryShortName) == null)
			publicationNames.put(countryShortName, new HashMap<String, String>());
		publicationNames.get(countryShortName).put(shortName, fullName);
		
	}
	
	@Override
	protected Object doInBackground(Object... params) {
		mHandler.post(new Runnable() {
	        public void run() {
				try {
		        	if (CoaListing.this.listType.equals(ListType.COUNTRY_LIST)
				     || CoaListing.this.listType.equals(ListType.PUBLICATION_LIST)
		        	 || CoaListing.this.listType.equals(ListType.ISSUE_LIST)) {
		        		if (WhatTheDuck.coaCollection.isEmpty()) {
			        		String response = WhatTheDuck.wtd.retrieveOrFail(progressBarId, "&coa=true&liste_pays=true");
							if (response == null)
								return;
							resetCountries();
							JSONObject object = new JSONObject(response);
							JSONObject countryName = object.getJSONObject("static").getJSONObject("pays");
							Iterator<String> countryIterator = countryName.keys();
							while (countryIterator.hasNext()) {
								String shortName=countryIterator.next();
								String fullName = countryName.getString(shortName);
								addCountry(shortName, fullName);
								WhatTheDuck.coaCollection.addCountry(shortName);
							}
		        		}
		        		
						if (displayedList instanceof CountryList)
							((CountryList)displayedList).show();
					}
		        	if (CoaListing.this.listType.equals(ListType.PUBLICATION_LIST)
		        	 || CoaListing.this.listType.equals(ListType.ISSUE_LIST)) {
		        		String countryShortName = CoaListing.this.countryShortName;
		        		if (! WhatTheDuck.coaCollection.hasCountry(countryShortName)) {
							String response = WhatTheDuck.wtd.retrieveOrFail(progressBarId, "&coa=true&liste_magazines=true&pays="+countryShortName);
							if (response == null)
								return;
							resetPublications();
							JSONObject object = new JSONObject(response);
							JSONObject publicationName = object.getJSONObject("static").getJSONObject("magazines");
							Iterator<String> publicationIterator = publicationName.keys();
							while (publicationIterator.hasNext()) {
								String shortName=publicationIterator.next();
								String fullName = publicationName.getString(shortName);
								addPublication(countryShortName, shortName, fullName);
								WhatTheDuck.coaCollection.addPublication(countryShortName, shortName);
							}
		        		}
		        		
		        		if (displayedList instanceof PublicationList)
							((PublicationList)displayedList).show();
		        	}
		        	if (CoaListing.this.listType.equals(ListType.ISSUE_LIST)) {
		        		String shortCountryName = CoaListing.this.countryShortName;
		        		String shortPublicationName = CoaListing.this.publicationShortName;
		        		if (! WhatTheDuck.coaCollection.hasPublication(shortCountryName, shortPublicationName)) {		        		
							String response = WhatTheDuck.wtd.retrieveOrFail(progressBarId, "&coa=true&liste_numeros=true&magazine="+shortPublicationName);
							if (response == null)
								return;
							JSONObject object = new JSONObject(response);
							JSONArray issues = object.getJSONObject("static").getJSONArray("numeros");
							for (int i = 0; i< issues.length();i++) {
								String issue=(String) issues.get(i);
								WhatTheDuck.coaCollection.addIssue(shortCountryName, shortPublicationName, new Issue(issue, Boolean.FALSE, Issue.NO_CONDITION));
							}
		        		}
		        		if (displayedList instanceof IssueList)
							((IssueList)displayedList).show();
		        	}
					
				} catch (JSONException e) {
					WhatTheDuck.wtd.alert(R.string.internal_error,"",
				   			   			  R.string.internal_error__malformed_list," : " + e.getMessage());
				}
	        }
		});
		return null;
	}
}
