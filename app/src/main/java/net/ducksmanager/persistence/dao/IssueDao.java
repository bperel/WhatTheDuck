package net.ducksmanager.persistence.dao;

import net.ducksmanager.persistence.models.dm.Issue;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface IssueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertList(List<Issue> issueList);

    @Query("DELETE FROM issues")
    void deleteAll();
}