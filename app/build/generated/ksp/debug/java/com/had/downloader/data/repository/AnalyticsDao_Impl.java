package com.had.downloader.data.repository;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AnalyticsDao_Impl implements AnalyticsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AnalyticsEvent> __insertionAdapterOfAnalyticsEvent;

  private final EntityInsertionAdapter<SpeedSample> __insertionAdapterOfSpeedSample;

  private final SharedSQLiteStatement __preparedStmtOfUpdateEvent;

  private final SharedSQLiteStatement __preparedStmtOfDeleteSpeedSamples;

  private final SharedSQLiteStatement __preparedStmtOfPurgeOldEvents;

  private final SharedSQLiteStatement __preparedStmtOfPurgeOldSamples;

  public AnalyticsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAnalyticsEvent = new EntityInsertionAdapter<AnalyticsEvent>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `analytics_events` (`id`,`downloadId`,`filename`,`url`,`startedAt`,`completedAt`,`durationMs`,`totalBytes`,`avgSpeedBps`,`peakSpeedBps`,`threads`,`retries`,`success`,`mode`,`dayOfWeek`,`hourOfDay`,`monthYear`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AnalyticsEvent entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getDownloadId());
        statement.bindString(3, entity.getFilename());
        statement.bindString(4, entity.getUrl());
        statement.bindLong(5, entity.getStartedAt());
        if (entity.getCompletedAt() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getCompletedAt());
        }
        statement.bindLong(7, entity.getDurationMs());
        statement.bindLong(8, entity.getTotalBytes());
        statement.bindLong(9, entity.getAvgSpeedBps());
        statement.bindLong(10, entity.getPeakSpeedBps());
        statement.bindLong(11, entity.getThreads());
        statement.bindLong(12, entity.getRetries());
        final int _tmp = entity.getSuccess() ? 1 : 0;
        statement.bindLong(13, _tmp);
        statement.bindString(14, entity.getMode());
        statement.bindLong(15, entity.getDayOfWeek());
        statement.bindLong(16, entity.getHourOfDay());
        statement.bindString(17, entity.getMonthYear());
      }
    };
    this.__insertionAdapterOfSpeedSample = new EntityInsertionAdapter<SpeedSample>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `speed_samples` (`id`,`downloadId`,`timestamp`,`speedBps`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SpeedSample entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getDownloadId());
        statement.bindLong(3, entity.getTimestamp());
        statement.bindLong(4, entity.getSpeedBps());
      }
    };
    this.__preparedStmtOfUpdateEvent = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE analytics_events SET completedAt = ?, durationMs = ?, totalBytes = ?, avgSpeedBps = ?, peakSpeedBps = ?, success = ?, retries = ? WHERE downloadId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteSpeedSamples = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM speed_samples WHERE downloadId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfPurgeOldEvents = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM analytics_events WHERE startedAt < ?";
        return _query;
      }
    };
    this.__preparedStmtOfPurgeOldSamples = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM speed_samples WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertEvent(final AnalyticsEvent event,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfAnalyticsEvent.insertAndReturnId(event);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertSpeedSample(final SpeedSample sample,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSpeedSample.insert(sample);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateEvent(final long downloadId, final long completedAt, final long durationMs,
      final long totalBytes, final long avgSpeed, final long peakSpeed, final boolean success,
      final int retries, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateEvent.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, completedAt);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, durationMs);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, totalBytes);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, avgSpeed);
        _argIndex = 5;
        _stmt.bindLong(_argIndex, peakSpeed);
        _argIndex = 6;
        final int _tmp = success ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 7;
        _stmt.bindLong(_argIndex, retries);
        _argIndex = 8;
        _stmt.bindLong(_argIndex, downloadId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateEvent.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSpeedSamples(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteSpeedSamples.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteSpeedSamples.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object purgeOldEvents(final long before, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPurgeOldEvents.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, before);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfPurgeOldEvents.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object purgeOldSamples(final long before, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPurgeOldSamples.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, before);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfPurgeOldSamples.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AnalyticsEvent>> observeRecentEvents() {
    final String _sql = "SELECT * FROM analytics_events ORDER BY startedAt DESC LIMIT 200";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"analytics_events"}, new Callable<List<AnalyticsEvent>>() {
      @Override
      @NonNull
      public List<AnalyticsEvent> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDownloadId = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadId");
          final int _cursorIndexOfFilename = CursorUtil.getColumnIndexOrThrow(_cursor, "filename");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfTotalBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBytes");
          final int _cursorIndexOfAvgSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "avgSpeedBps");
          final int _cursorIndexOfPeakSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "peakSpeedBps");
          final int _cursorIndexOfThreads = CursorUtil.getColumnIndexOrThrow(_cursor, "threads");
          final int _cursorIndexOfRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "retries");
          final int _cursorIndexOfSuccess = CursorUtil.getColumnIndexOrThrow(_cursor, "success");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfWeek");
          final int _cursorIndexOfHourOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "hourOfDay");
          final int _cursorIndexOfMonthYear = CursorUtil.getColumnIndexOrThrow(_cursor, "monthYear");
          final List<AnalyticsEvent> _result = new ArrayList<AnalyticsEvent>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AnalyticsEvent _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDownloadId;
            _tmpDownloadId = _cursor.getLong(_cursorIndexOfDownloadId);
            final String _tmpFilename;
            _tmpFilename = _cursor.getString(_cursorIndexOfFilename);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final long _tmpStartedAt;
            _tmpStartedAt = _cursor.getLong(_cursorIndexOfStartedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final long _tmpAvgSpeedBps;
            _tmpAvgSpeedBps = _cursor.getLong(_cursorIndexOfAvgSpeedBps);
            final long _tmpPeakSpeedBps;
            _tmpPeakSpeedBps = _cursor.getLong(_cursorIndexOfPeakSpeedBps);
            final int _tmpThreads;
            _tmpThreads = _cursor.getInt(_cursorIndexOfThreads);
            final int _tmpRetries;
            _tmpRetries = _cursor.getInt(_cursorIndexOfRetries);
            final boolean _tmpSuccess;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSuccess);
            _tmpSuccess = _tmp != 0;
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final int _tmpHourOfDay;
            _tmpHourOfDay = _cursor.getInt(_cursorIndexOfHourOfDay);
            final String _tmpMonthYear;
            _tmpMonthYear = _cursor.getString(_cursorIndexOfMonthYear);
            _item = new AnalyticsEvent(_tmpId,_tmpDownloadId,_tmpFilename,_tmpUrl,_tmpStartedAt,_tmpCompletedAt,_tmpDurationMs,_tmpTotalBytes,_tmpAvgSpeedBps,_tmpPeakSpeedBps,_tmpThreads,_tmpRetries,_tmpSuccess,_tmpMode,_tmpDayOfWeek,_tmpHourOfDay,_tmpMonthYear);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<AnalyticsEvent>> observeCompletedEvents() {
    final String _sql = "SELECT * FROM analytics_events WHERE success = 1 ORDER BY startedAt DESC LIMIT 100";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"analytics_events"}, new Callable<List<AnalyticsEvent>>() {
      @Override
      @NonNull
      public List<AnalyticsEvent> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDownloadId = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadId");
          final int _cursorIndexOfFilename = CursorUtil.getColumnIndexOrThrow(_cursor, "filename");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfTotalBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBytes");
          final int _cursorIndexOfAvgSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "avgSpeedBps");
          final int _cursorIndexOfPeakSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "peakSpeedBps");
          final int _cursorIndexOfThreads = CursorUtil.getColumnIndexOrThrow(_cursor, "threads");
          final int _cursorIndexOfRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "retries");
          final int _cursorIndexOfSuccess = CursorUtil.getColumnIndexOrThrow(_cursor, "success");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfWeek");
          final int _cursorIndexOfHourOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "hourOfDay");
          final int _cursorIndexOfMonthYear = CursorUtil.getColumnIndexOrThrow(_cursor, "monthYear");
          final List<AnalyticsEvent> _result = new ArrayList<AnalyticsEvent>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AnalyticsEvent _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDownloadId;
            _tmpDownloadId = _cursor.getLong(_cursorIndexOfDownloadId);
            final String _tmpFilename;
            _tmpFilename = _cursor.getString(_cursorIndexOfFilename);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final long _tmpStartedAt;
            _tmpStartedAt = _cursor.getLong(_cursorIndexOfStartedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final long _tmpAvgSpeedBps;
            _tmpAvgSpeedBps = _cursor.getLong(_cursorIndexOfAvgSpeedBps);
            final long _tmpPeakSpeedBps;
            _tmpPeakSpeedBps = _cursor.getLong(_cursorIndexOfPeakSpeedBps);
            final int _tmpThreads;
            _tmpThreads = _cursor.getInt(_cursorIndexOfThreads);
            final int _tmpRetries;
            _tmpRetries = _cursor.getInt(_cursorIndexOfRetries);
            final boolean _tmpSuccess;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSuccess);
            _tmpSuccess = _tmp != 0;
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final int _tmpHourOfDay;
            _tmpHourOfDay = _cursor.getInt(_cursorIndexOfHourOfDay);
            final String _tmpMonthYear;
            _tmpMonthYear = _cursor.getString(_cursorIndexOfMonthYear);
            _item = new AnalyticsEvent(_tmpId,_tmpDownloadId,_tmpFilename,_tmpUrl,_tmpStartedAt,_tmpCompletedAt,_tmpDurationMs,_tmpTotalBytes,_tmpAvgSpeedBps,_tmpPeakSpeedBps,_tmpThreads,_tmpRetries,_tmpSuccess,_tmpMode,_tmpDayOfWeek,_tmpHourOfDay,_tmpMonthYear);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<SpeedSample>> observeSpeedHistory(final long id) {
    final String _sql = "SELECT * FROM speed_samples WHERE downloadId = ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"speed_samples"}, new Callable<List<SpeedSample>>() {
      @Override
      @NonNull
      public List<SpeedSample> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDownloadId = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "speedBps");
          final List<SpeedSample> _result = new ArrayList<SpeedSample>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpeedSample _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDownloadId;
            _tmpDownloadId = _cursor.getLong(_cursorIndexOfDownloadId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpSpeedBps;
            _tmpSpeedBps = _cursor.getLong(_cursorIndexOfSpeedBps);
            _item = new SpeedSample(_tmpId,_tmpDownloadId,_tmpTimestamp,_tmpSpeedBps);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getSpeedHistory(final long id,
      final Continuation<? super List<SpeedSample>> $completion) {
    final String _sql = "SELECT * FROM speed_samples WHERE downloadId = ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SpeedSample>>() {
      @Override
      @NonNull
      public List<SpeedSample> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDownloadId = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "speedBps");
          final List<SpeedSample> _result = new ArrayList<SpeedSample>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpeedSample _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDownloadId;
            _tmpDownloadId = _cursor.getLong(_cursorIndexOfDownloadId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpSpeedBps;
            _tmpSpeedBps = _cursor.getLong(_cursorIndexOfSpeedBps);
            _item = new SpeedSample(_tmpId,_tmpDownloadId,_tmpTimestamp,_tmpSpeedBps);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MonthlyStats>> observeMonthlyStats() {
    final String _sql = "SELECT monthYear, SUM(totalBytes) as bytes, COUNT(*) as count, AVG(avgSpeedBps) as avgSpeed FROM analytics_events WHERE success = 1 GROUP BY monthYear ORDER BY monthYear DESC LIMIT 12";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"analytics_events"}, new Callable<List<MonthlyStats>>() {
      @Override
      @NonNull
      public List<MonthlyStats> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfMonthYear = 0;
          final int _cursorIndexOfBytes = 1;
          final int _cursorIndexOfCount = 2;
          final int _cursorIndexOfAvgSpeed = 3;
          final List<MonthlyStats> _result = new ArrayList<MonthlyStats>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MonthlyStats _item;
            final String _tmpMonthYear;
            _tmpMonthYear = _cursor.getString(_cursorIndexOfMonthYear);
            final long _tmpBytes;
            _tmpBytes = _cursor.getLong(_cursorIndexOfBytes);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            final long _tmpAvgSpeed;
            _tmpAvgSpeed = _cursor.getLong(_cursorIndexOfAvgSpeed);
            _item = new MonthlyStats(_tmpMonthYear,_tmpBytes,_tmpCount,_tmpAvgSpeed);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<HourlyStats>> observeHourlyDistribution() {
    final String _sql = "SELECT hourOfDay, COUNT(*) as count FROM analytics_events WHERE success = 1 GROUP BY hourOfDay ORDER BY hourOfDay ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"analytics_events"}, new Callable<List<HourlyStats>>() {
      @Override
      @NonNull
      public List<HourlyStats> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfHourOfDay = 0;
          final int _cursorIndexOfCount = 1;
          final List<HourlyStats> _result = new ArrayList<HourlyStats>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HourlyStats _item;
            final int _tmpHourOfDay;
            _tmpHourOfDay = _cursor.getInt(_cursorIndexOfHourOfDay);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            _item = new HourlyStats(_tmpHourOfDay,_tmpCount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<OverallStats> observeOverallStats() {
    final String _sql = "SELECT COUNT(*) as total, SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as successful, SUM(totalBytes) as totalBytes, AVG(avgSpeedBps) as avgSpeed, MAX(peakSpeedBps) as peakSpeed FROM analytics_events";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"analytics_events"}, new Callable<OverallStats>() {
      @Override
      @NonNull
      public OverallStats call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTotal = 0;
          final int _cursorIndexOfSuccessful = 1;
          final int _cursorIndexOfTotalBytes = 2;
          final int _cursorIndexOfAvgSpeed = 3;
          final int _cursorIndexOfPeakSpeed = 4;
          final OverallStats _result;
          if (_cursor.moveToFirst()) {
            final int _tmpTotal;
            _tmpTotal = _cursor.getInt(_cursorIndexOfTotal);
            final int _tmpSuccessful;
            _tmpSuccessful = _cursor.getInt(_cursorIndexOfSuccessful);
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final long _tmpAvgSpeed;
            _tmpAvgSpeed = _cursor.getLong(_cursorIndexOfAvgSpeed);
            final long _tmpPeakSpeed;
            _tmpPeakSpeed = _cursor.getLong(_cursorIndexOfPeakSpeed);
            _result = new OverallStats(_tmpTotal,_tmpSuccessful,_tmpTotalBytes,_tmpAvgSpeed,_tmpPeakSpeed);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getEventByDownloadId(final long id,
      final Continuation<? super AnalyticsEvent> $completion) {
    final String _sql = "SELECT * FROM analytics_events WHERE downloadId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AnalyticsEvent>() {
      @Override
      @Nullable
      public AnalyticsEvent call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDownloadId = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadId");
          final int _cursorIndexOfFilename = CursorUtil.getColumnIndexOrThrow(_cursor, "filename");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfTotalBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBytes");
          final int _cursorIndexOfAvgSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "avgSpeedBps");
          final int _cursorIndexOfPeakSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "peakSpeedBps");
          final int _cursorIndexOfThreads = CursorUtil.getColumnIndexOrThrow(_cursor, "threads");
          final int _cursorIndexOfRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "retries");
          final int _cursorIndexOfSuccess = CursorUtil.getColumnIndexOrThrow(_cursor, "success");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfWeek");
          final int _cursorIndexOfHourOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "hourOfDay");
          final int _cursorIndexOfMonthYear = CursorUtil.getColumnIndexOrThrow(_cursor, "monthYear");
          final AnalyticsEvent _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDownloadId;
            _tmpDownloadId = _cursor.getLong(_cursorIndexOfDownloadId);
            final String _tmpFilename;
            _tmpFilename = _cursor.getString(_cursorIndexOfFilename);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final long _tmpStartedAt;
            _tmpStartedAt = _cursor.getLong(_cursorIndexOfStartedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final long _tmpAvgSpeedBps;
            _tmpAvgSpeedBps = _cursor.getLong(_cursorIndexOfAvgSpeedBps);
            final long _tmpPeakSpeedBps;
            _tmpPeakSpeedBps = _cursor.getLong(_cursorIndexOfPeakSpeedBps);
            final int _tmpThreads;
            _tmpThreads = _cursor.getInt(_cursorIndexOfThreads);
            final int _tmpRetries;
            _tmpRetries = _cursor.getInt(_cursorIndexOfRetries);
            final boolean _tmpSuccess;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSuccess);
            _tmpSuccess = _tmp != 0;
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final int _tmpHourOfDay;
            _tmpHourOfDay = _cursor.getInt(_cursorIndexOfHourOfDay);
            final String _tmpMonthYear;
            _tmpMonthYear = _cursor.getString(_cursorIndexOfMonthYear);
            _result = new AnalyticsEvent(_tmpId,_tmpDownloadId,_tmpFilename,_tmpUrl,_tmpStartedAt,_tmpCompletedAt,_tmpDurationMs,_tmpTotalBytes,_tmpAvgSpeedBps,_tmpPeakSpeedBps,_tmpThreads,_tmpRetries,_tmpSuccess,_tmpMode,_tmpDayOfWeek,_tmpHourOfDay,_tmpMonthYear);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
