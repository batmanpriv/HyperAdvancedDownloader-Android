package com.had.downloader.data.repository;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HADDatabase_Impl extends HADDatabase {
  private volatile DownloadDao _downloadDao;

  private volatile AnalyticsDao _analyticsDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `downloads` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `url` TEXT NOT NULL, `filename` TEXT NOT NULL, `outputDir` TEXT NOT NULL, `mode` TEXT NOT NULL, `threads` INTEGER NOT NULL, `proxy` TEXT, `userAgent` TEXT NOT NULL, `customHeaders` TEXT NOT NULL, `cookies` TEXT NOT NULL, `httpMethod` TEXT NOT NULL, `useResume` INTEGER NOT NULL, `verbose` INTEGER NOT NULL, `maxRetries` INTEGER NOT NULL, `timeoutSec` INTEGER NOT NULL, `maxSpeedBps` INTEGER NOT NULL, `mirrors` TEXT NOT NULL, `checksumAlgo` TEXT NOT NULL, `checksumExpected` TEXT NOT NULL, `scheduleFrom` TEXT NOT NULL, `scheduleTo` TEXT NOT NULL, `hlsSegmentCount` INTEGER NOT NULL, `hlsSegmentsDone` INTEGER NOT NULL, `status` TEXT NOT NULL, `progress` REAL NOT NULL, `downloadedBytes` INTEGER NOT NULL, `totalBytes` INTEGER NOT NULL, `speedBps` INTEGER NOT NULL, `etaSeconds` INTEGER NOT NULL, `activeThreads` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `completedAt` INTEGER, `errorMessage` TEXT, `queuePriority` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `analytics_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `downloadId` INTEGER NOT NULL, `filename` TEXT NOT NULL, `url` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `completedAt` INTEGER, `durationMs` INTEGER NOT NULL, `totalBytes` INTEGER NOT NULL, `avgSpeedBps` INTEGER NOT NULL, `peakSpeedBps` INTEGER NOT NULL, `threads` INTEGER NOT NULL, `retries` INTEGER NOT NULL, `success` INTEGER NOT NULL, `mode` TEXT NOT NULL, `dayOfWeek` INTEGER NOT NULL, `hourOfDay` INTEGER NOT NULL, `monthYear` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `speed_samples` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `downloadId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `speedBps` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '0a65c3b401276d553e39eeedf4f7b7ef')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `downloads`");
        db.execSQL("DROP TABLE IF EXISTS `analytics_events`");
        db.execSQL("DROP TABLE IF EXISTS `speed_samples`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsDownloads = new HashMap<String, TableInfo.Column>(34);
        _columnsDownloads.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("url", new TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("filename", new TableInfo.Column("filename", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("outputDir", new TableInfo.Column("outputDir", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("mode", new TableInfo.Column("mode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("threads", new TableInfo.Column("threads", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("proxy", new TableInfo.Column("proxy", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("userAgent", new TableInfo.Column("userAgent", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("customHeaders", new TableInfo.Column("customHeaders", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("cookies", new TableInfo.Column("cookies", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("httpMethod", new TableInfo.Column("httpMethod", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("useResume", new TableInfo.Column("useResume", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("verbose", new TableInfo.Column("verbose", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("maxRetries", new TableInfo.Column("maxRetries", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("timeoutSec", new TableInfo.Column("timeoutSec", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("maxSpeedBps", new TableInfo.Column("maxSpeedBps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("mirrors", new TableInfo.Column("mirrors", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("checksumAlgo", new TableInfo.Column("checksumAlgo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("checksumExpected", new TableInfo.Column("checksumExpected", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("scheduleFrom", new TableInfo.Column("scheduleFrom", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("scheduleTo", new TableInfo.Column("scheduleTo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("hlsSegmentCount", new TableInfo.Column("hlsSegmentCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("hlsSegmentsDone", new TableInfo.Column("hlsSegmentsDone", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("progress", new TableInfo.Column("progress", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("downloadedBytes", new TableInfo.Column("downloadedBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("totalBytes", new TableInfo.Column("totalBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("speedBps", new TableInfo.Column("speedBps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("etaSeconds", new TableInfo.Column("etaSeconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("activeThreads", new TableInfo.Column("activeThreads", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("completedAt", new TableInfo.Column("completedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("errorMessage", new TableInfo.Column("errorMessage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("queuePriority", new TableInfo.Column("queuePriority", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDownloads = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDownloads = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDownloads = new TableInfo("downloads", _columnsDownloads, _foreignKeysDownloads, _indicesDownloads);
        final TableInfo _existingDownloads = TableInfo.read(db, "downloads");
        if (!_infoDownloads.equals(_existingDownloads)) {
          return new RoomOpenHelper.ValidationResult(false, "downloads(com.had.downloader.data.model.DownloadItem).\n"
                  + " Expected:\n" + _infoDownloads + "\n"
                  + " Found:\n" + _existingDownloads);
        }
        final HashMap<String, TableInfo.Column> _columnsAnalyticsEvents = new HashMap<String, TableInfo.Column>(17);
        _columnsAnalyticsEvents.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("downloadId", new TableInfo.Column("downloadId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("filename", new TableInfo.Column("filename", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("url", new TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("startedAt", new TableInfo.Column("startedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("completedAt", new TableInfo.Column("completedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("durationMs", new TableInfo.Column("durationMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("totalBytes", new TableInfo.Column("totalBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("avgSpeedBps", new TableInfo.Column("avgSpeedBps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("peakSpeedBps", new TableInfo.Column("peakSpeedBps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("threads", new TableInfo.Column("threads", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("retries", new TableInfo.Column("retries", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("success", new TableInfo.Column("success", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("mode", new TableInfo.Column("mode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("dayOfWeek", new TableInfo.Column("dayOfWeek", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("hourOfDay", new TableInfo.Column("hourOfDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalyticsEvents.put("monthYear", new TableInfo.Column("monthYear", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAnalyticsEvents = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAnalyticsEvents = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAnalyticsEvents = new TableInfo("analytics_events", _columnsAnalyticsEvents, _foreignKeysAnalyticsEvents, _indicesAnalyticsEvents);
        final TableInfo _existingAnalyticsEvents = TableInfo.read(db, "analytics_events");
        if (!_infoAnalyticsEvents.equals(_existingAnalyticsEvents)) {
          return new RoomOpenHelper.ValidationResult(false, "analytics_events(com.had.downloader.data.repository.AnalyticsEvent).\n"
                  + " Expected:\n" + _infoAnalyticsEvents + "\n"
                  + " Found:\n" + _existingAnalyticsEvents);
        }
        final HashMap<String, TableInfo.Column> _columnsSpeedSamples = new HashMap<String, TableInfo.Column>(4);
        _columnsSpeedSamples.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpeedSamples.put("downloadId", new TableInfo.Column("downloadId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpeedSamples.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpeedSamples.put("speedBps", new TableInfo.Column("speedBps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSpeedSamples = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSpeedSamples = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSpeedSamples = new TableInfo("speed_samples", _columnsSpeedSamples, _foreignKeysSpeedSamples, _indicesSpeedSamples);
        final TableInfo _existingSpeedSamples = TableInfo.read(db, "speed_samples");
        if (!_infoSpeedSamples.equals(_existingSpeedSamples)) {
          return new RoomOpenHelper.ValidationResult(false, "speed_samples(com.had.downloader.data.repository.SpeedSample).\n"
                  + " Expected:\n" + _infoSpeedSamples + "\n"
                  + " Found:\n" + _existingSpeedSamples);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "0a65c3b401276d553e39eeedf4f7b7ef", "8b1757a98d8452439c0850ef5022bf87");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "downloads","analytics_events","speed_samples");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `downloads`");
      _db.execSQL("DELETE FROM `analytics_events`");
      _db.execSQL("DELETE FROM `speed_samples`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(DownloadDao.class, DownloadDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AnalyticsDao.class, AnalyticsDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public DownloadDao downloadDao() {
    if (_downloadDao != null) {
      return _downloadDao;
    } else {
      synchronized(this) {
        if(_downloadDao == null) {
          _downloadDao = new DownloadDao_Impl(this);
        }
        return _downloadDao;
      }
    }
  }

  @Override
  public AnalyticsDao analyticsDao() {
    if (_analyticsDao != null) {
      return _analyticsDao;
    } else {
      synchronized(this) {
        if(_analyticsDao == null) {
          _analyticsDao = new AnalyticsDao_Impl(this);
        }
        return _analyticsDao;
      }
    }
  }
}
