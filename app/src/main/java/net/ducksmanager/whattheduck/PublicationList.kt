package net.ducksmanager.whattheduck

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import net.ducksmanager.apigateway.DmServer
import net.ducksmanager.persistence.models.coa.InducksPublication
import net.ducksmanager.persistence.models.composite.InducksPublicationWithPossession
import retrofit2.Response
import java.util.*

class PublicationList : ItemList<InducksPublicationWithPossession>() {
    override fun hasList(): Boolean {
        return false // FIXME
    }

    override fun downloadList(currentActivity: Activity?) {
        DmServer.api.getPublications(WhatTheDuckApplication.selectedCountry).enqueue(object : DmServer.Callback<HashMap<String, String>>("getInducksPublications", currentActivity) {
            override fun onSuccessfulResponse(response: Response<HashMap<String, String>>) {
                val publications: MutableList<InducksPublication> = ArrayList()
                for (publicationCode in response.body()!!.keys) {
                    publications.add(InducksPublication(publicationCode, response.body()!![publicationCode]))
                }
                WhatTheDuckApplication.appDB.inducksPublicationDao().insertList(publications)
                setData()
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WhatTheDuckApplication.selectedPublication = null
        show()
    }

    override val isPossessedByUser: Boolean
        get() {
            return data.any { it.possessed }
        }

    override fun setData() {
        WhatTheDuckApplication.appDB.inducksPublicationDao().findByCountry(WhatTheDuckApplication.selectedCountry).observe(this, Observer { items: List<InducksPublicationWithPossession> ->
            storeItemList(items)
        })
    }

    override fun shouldShow() = WhatTheDuckApplication.selectedCountry != null

    override fun shouldShowNavigationCountry() = true

    override fun shouldShowNavigationPublication() = false

    override fun shouldShowToolbar() = true

    override fun shouldShowAddToCollectionButton() = true

    override fun shouldShowFilter(items: List<InducksPublicationWithPossession>) = items.size > MIN_ITEM_NUMBER_FOR_FILTER

    override fun hasDividers() = true

    override val itemAdapter: ItemAdapter<InducksPublicationWithPossession>
        get() = PublicationAdapter(this, data)

    override fun onBackPressed() {
        if (type == WhatTheDuckApplication.CollectionType.COA.toString()) {
            onBackFromAddIssueActivity()
        } else {
            startActivity(Intent(this, CountryList::class.java))
        }
    }
}