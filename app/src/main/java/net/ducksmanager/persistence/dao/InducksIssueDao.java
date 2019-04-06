package net.ducksmanager.persistence.dao;

import net.ducksmanager.persistence.models.coa.InducksIssue;
import net.ducksmanager.persistence.models.composite.InducksIssueWithUserIssueDetails;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface InducksIssueDao {
    @Query("SELECT inducks_issue.*, issues.*, purchases.* FROM inducks_issue LEFT JOIN issues ON inducks_issue.inducksPublicationCode = issues.country || '/' || issues.magazine AND inducks_issue.inducksIssueNumber = issues.issueNumber LEFT JOIN purchases ON issues.issuePurchaseId = purchases.purchaseId WHERE inducksPublicationCode = :publicationCode")
    LiveData<List<InducksIssueWithUserIssueDetails>> findByPublicationCode(String publicationCode);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertList(List<InducksIssue> issueList);

}
