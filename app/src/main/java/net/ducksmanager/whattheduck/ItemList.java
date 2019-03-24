package net.ducksmanager.whattheduck;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import net.ducksmanager.apigateway.DmServer;
import net.ducksmanager.persistence.models.composite.CoverSearchResults;
import net.ducksmanager.persistence.models.composite.IssueWithFullUrl;
import net.ducksmanager.util.CoverFlowActivity;
import net.ducksmanager.util.CoverFlowFileHandler;
import net.ducksmanager.util.Settings;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static net.ducksmanager.whattheduck.WhatTheDuck.trackEvent;

public abstract class ItemList<Item> extends AppCompatActivity {
    public static String type = WhatTheDuck.CollectionType.USER.toString();
    static final int MIN_ITEM_NUMBER_FOR_FILTER = 20;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private Boolean requiresDataDownload = false;
    protected List<Item> data = new ArrayList<>();

    protected abstract boolean hasList();
    protected abstract void downloadList();
    protected abstract boolean hasDividers();

    protected abstract boolean isPossessedByUser();
    protected abstract void setData();

    protected abstract boolean shouldShow();
    protected abstract boolean shouldShowNavigationCountry();
    protected abstract boolean shouldShowNavigationPublication();
    protected abstract boolean shouldShowToolbar();
    protected abstract boolean shouldShowAddToCollectionButton();
    protected abstract boolean shouldShowFilter(List<Item> items);

    protected abstract ItemAdapter<Item> getItemAdapter();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.wtd_list);

        View navigationAllCountries = this.findViewById(R.id.navigationAllCountries);
        if (navigationAllCountries != null) {
            navigationAllCountries.setOnClickListener(view ->
                goToView(CountryList.class)
            );
        }

        View navigationCurrentCountry = this.findViewById(R.id.navigationCountry);
        if (navigationCurrentCountry != null) {
            navigationCurrentCountry.findViewById(R.id.selected).setOnClickListener(view ->
                goToView(PublicationList.class)
            );
        }

        loadList();
    }

    void loadList() {
        ((WhatTheDuckApplication) getApplication()).trackActivity(this);
        if (!hasList()) {
            this.requiresDataDownload = true;
            downloadList();
        }
        else {
            setData();
        }
    }

    private void goToView(Class<?> cls) {
        if (!ItemList.this.getClass().equals(cls)) {
            startActivity(new Intent(WhatTheDuck.wtd, cls));
        }
    }

    void goToAlternativeView() {
        type = (type.equals(WhatTheDuck.CollectionType.USER.toString()) ? WhatTheDuck.CollectionType.COA.toString() : WhatTheDuck.CollectionType.USER.toString());
        loadList();
        show();
    }

    void show() {
        if (!shouldShow()) {
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (type.equals(WhatTheDuck.CollectionType.USER.toString())) {
                actionBar.setDisplayHomeAsUpEnabled(false);
            }
            else {
                actionBar.setDisplayHomeAsUpEnabled(true);
                ((Toolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> onBackFromAddIssueActivity());
            }
        }

        setTitle(
            type.equals(WhatTheDuck.CollectionType.USER.toString())
                ? getString(R.string.my_collection)
                : getString(R.string.add_issue)
        );

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (shouldShowToolbar()) {
            toolbar.setVisibility(VISIBLE);
            setSupportActionBar(toolbar);
        }
        else {
            toolbar.setVisibility(GONE);
        }

        showNavigation();

        FloatingActionMenu addToCollection = this.findViewById(R.id.addToCollectionWrapper);
        if (shouldShowAddToCollectionButton()) {
            if (addToCollection != null) {
                addToCollection.setMenuButtonColorNormalResId(R.color.holo_green_dark);
                addToCollection.setMenuButtonColorPressedResId(R.color.holo_green_dark);
                addToCollection.setVisibility(type.equals(WhatTheDuck.CollectionType.USER.toString()) ? VISIBLE : GONE);
                addToCollection.close(false);

                if (type.equals(WhatTheDuck.CollectionType.USER.toString())) {
                    FloatingActionButton addToCollectionByPhotoButton = this.findViewById(R.id.addToCollectionByPhotoButton);
                    addToCollectionByPhotoButton.setOnClickListener(view ->
                        takeCoverPicture()
                    );

                    FloatingActionButton addToCollectionBySelectionButton = this.findViewById(R.id.addToCollectionBySelectionButton);
                    addToCollectionBySelectionButton.setOnClickListener(view -> {
                        addToCollection.setVisibility(GONE);
                        ItemList.this.goToAlternativeView();
                    });

                    Settings.saveSettings();
                }
            }
        }
        else {
            addToCollection.setVisibility(GONE);
        }

        ItemAdapter<Item> itemAdapter = getItemAdapter();
        if (this.requiresDataDownload) {
            itemAdapter.resetItems();
        }

        java.util.List<Item> items = itemAdapter.getItems();
        TextView emptyListText = this.findViewById(R.id.emptyList);
        if (emptyListText != null) {
            emptyListText.setVisibility(this.requiresDataDownload || items.size() > 0 ? INVISIBLE : VISIBLE);
        }

        RecyclerView recyclerView = findViewById(R.id.itemList);
        recyclerView.setAdapter(itemAdapter);

        EditText filterEditText = this.findViewById(R.id.filter);
        if (shouldShowFilter(items)) {
            itemAdapter.addOrReplaceFilterOnChangeListener(filterEditText);
        }
        else {
            filterEditText.setVisibility(GONE);
        }

        while (recyclerView.getItemDecorationCount() > 0) {
            recyclerView.removeItemDecorationAt(0);
        }
        if (hasDividers()) {
            DividerItemDecoration dividerDecoration = new DividerItemDecoration(
                recyclerView.getContext(),
                new LinearLayoutManager(this).getOrientation()
            );
            recyclerView.addItemDecoration(dividerDecoration);
        }
    }

    private void takeCoverPicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            CoverFlowFileHandler.current = new CoverFlowFileHandler();
            Uri photoURI = CoverFlowFileHandler.current.createEmptyFileForCamera(ItemList.this);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            this.findViewById(R.id.addToCollectionWrapper).setVisibility(View.VISIBLE);
            this.findViewById(R.id.progressBar).setVisibility(VISIBLE);

            CoverFlowFileHandler.current.resizeUntilFileSize(this, new CoverFlowFileHandler.TransformationCallback() {
                @Override
                public void onComplete(File file) {
                    RequestBody requestBody = RequestBody.create(MediaType.parse("*/*"), file);
                    MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("wtd_jpg", file.getName(), requestBody);
                    RequestBody fileName = RequestBody.create(MediaType.parse("text/plain"), file.getName());

                    System.out.println("Starting cover search : " + System.currentTimeMillis());
                    trackEvent("coversearch/start");
                    DmServer.api.searchFromCover(fileToUpload, fileName).enqueue(new DmServer.Callback<CoverSearchResults>(ItemList.this.findViewById(R.id.progressBar)) {
                        @Override
                        public void onSuccessfulResponse(Response<CoverSearchResults> response) {
                            trackEvent("coversearch/finish");
                            System.out.println("Ending cover search : " + System.currentTimeMillis());

                            if (response.body().getIssues() != null) {
                                for (IssueWithFullUrl issue : response.body().getIssues()) {
                                    issue.setFullUrl(
                                            WhatTheDuckApplication.config.getProperty(WhatTheDuckApplication.CONFIG_KEY_API_ENDPOINT_URL) + "/cover-id/download/" + issue.getCoverId()
                                    );
                                }
                                ItemList.this.startActivity(new Intent(ItemList.this, CoverFlowActivity.class));
                            }
                            else {
                                if (response.body().getType() != null) {
                                    switch(response.body().getType()) {
                                        case "SEARCH_RESULTS":
                                            WhatTheDuck.wtd.alert(new WeakReference<>(ItemList.this), R.string.add_cover_no_results);
                                            break;
                                        default:
                                            WhatTheDuck.wtd.alert(new WeakReference<>(ItemList.this), response.body().getType());
                                    }
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call call, Throwable t) {
                            if (t.getMessage().contains("exceeds your upload")) {
                                WhatTheDuck.wtd.alert(new WeakReference<>(ItemList.this), R.string.add_cover_error_file_too_big);
                            }
                            else {
                                WhatTheDuck.wtd.alert(new WeakReference<>(ItemList.this), R.string.internal_error);
                            }
                        }
                    });
                }

                @Override
                public void onFail() {
                    ItemList.this.findViewById(R.id.addToCollectionWrapper).setVisibility(VISIBLE);
                    ItemList.this.findViewById(R.id.progressBar).setVisibility(GONE);
                }
            });
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_user, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i = null;

        switch (item.getItemId()) {
            case R.id.action_logout:
                Settings.setUsername(null);
                Settings.setPassword(null);
                Settings.saveSettings();
                i = new Intent(WhatTheDuck.wtd, WhatTheDuck.class);

                break;
            case R.id.action_about:
                WhatTheDuck.showAbout(this);

        }
        if (i == null) {
            return super.onOptionsItemSelected(item);
        }
        else {
            startActivity(i);
            return true;
        }
    }

    private void showNavigation() {
        if (shouldShowNavigationCountry()) {
            WhatTheDuck.appDB.inducksCountryDao().findByCountryCode(WhatTheDuck.getSelectedCountry())
                .observe(this, inducksCountryName ->
                    setNavigationCountry(inducksCountryName.getCountryCode(), inducksCountryName.getCountryName())
                );
        }
        else {
            this.findViewById(R.id.navigationCountry).setVisibility(INVISIBLE);
        }
        if (shouldShowNavigationPublication()) {
            WhatTheDuck.appDB.inducksPublicationDao().findByPublicationCode(WhatTheDuck.getSelectedPublication())
                .observe(this, inducksPublication ->
                    setNavigationPublication(inducksPublication.getPublicationCode(), inducksPublication.getTitle())
                );
        }
        else {
            this.findViewById(R.id.navigationPublication).setVisibility(INVISIBLE);
        }
    }

    private void setNavigationCountry(String selectedCountry, String countryFullName) {
        final View countryNavigationView = this.findViewById(R.id.navigationCountry);
        String uri = "@drawable/flags_" + selectedCountry;
        int imageResource = getResources().getIdentifier(uri, null, getPackageName());

        if (imageResource == 0) {
            imageResource = R.drawable.flags_unknown;
        }

        ImageView currentCountryFlag = countryNavigationView.findViewById(R.id.selectedBadgeImage);
        currentCountryFlag.setImageResource(imageResource);

        TextView currentCountryText = countryNavigationView.findViewById(R.id.selectedText);
        currentCountryText.setText(countryFullName);
    }

    private void setNavigationPublication(String selectedPublication, String publicationFullName) {
        final View publicationNavigationView = this.findViewById(R.id.navigationPublication);

        TextView currentPublicationBadgeText = publicationNavigationView.findViewById(R.id.selectedBadge);
        currentPublicationBadgeText.setText(selectedPublication.split("/")[1]);

        TextView currentPublicationText = publicationNavigationView.findViewById(R.id.selectedText);
        currentPublicationText.setText(publicationFullName);
    }

    protected void storeItemList(List<Item> items) {
        this.data = items;
        this.requiresDataDownload = false;
        this.show();
    }

    void onBackFromAddIssueActivity() {
        if (isPossessedByUser()) {
            goToAlternativeView();
        }
        else {
            type = WhatTheDuck.CollectionType.USER.toString();
            startActivity(new Intent(WhatTheDuck.wtd, CountryList.class));
        }
    }
}
