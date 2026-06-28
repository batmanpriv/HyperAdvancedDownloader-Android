package com.had.downloader.data.repository;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.had.downloader.data.model.DownloadItem;
import com.had.downloader.data.model.DownloadMode;
import com.had.downloader.data.model.DownloadStatus;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class DownloadDao_Impl implements DownloadDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DownloadItem> __insertionAdapterOfDownloadItem;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<DownloadItem> __deletionAdapterOfDownloadItem;

  private final EntityDeletionOrUpdateAdapter<DownloadItem> __updateAdapterOfDownloadItem;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfUpdateProgress;

  private final SharedSQLiteStatement __preparedStmtOfMarkCompleted;

  private final SharedSQLiteStatement __preparedStmtOfMarkFailed;

  public DownloadDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDownloadItem = new EntityInsertionAdapter<DownloadItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `downloads` (`id`,`url`,`filename`,`outputDir`,`mode`,`threads`,`proxy`,`userAgent`,`customHeaders`,`cookies`,`httpMethod`,`useResume`,`verbose`,`maxRetries`,`timeoutSec`,`maxSpeedBps`,`mirrors`,`checksumAlgo`,`checksumExpected`,`scheduleFrom`,`scheduleTo`,`hlsSegmentCount`,`hlsSegmentsDone`,`status`,`progress`,`downloadedBytes`,`totalBytes`,`speedBps`,`etaSeconds`,`activeThreads`,`createdAt`,`completedAt`,`errorMessage`,`queuePriority`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DownloadItem entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getUrl());
        statement.bindString(3, entity.getFilename());
        statement.bindString(4, entity.getOutputDir());
        final String _tmp = __converters.fromMode(entity.getMode());
        statement.bindString(5, _tmp);
        statement.bindLong(6, entity.getThreads());
        if (entity.getProxy() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getProxy());
        }
        statement.bindString(8, entity.getUserAgent());
        statement.bindString(9, entity.getCustomHeaders());
        statement.bindString(10, entity.getCookies());
        statement.bindString(11, entity.getHttpMethod());
        final int _tmp_1 = entity.getUseResume() ? 1 : 0;
        statement.bindLong(12, _tmp_1);
        final int _tmp_2 = entity.getVerbose() ? 1 : 0;
        statement.bindLong(13, _tmp_2);
        statement.bindLong(14, entity.getMaxRetries());
        statement.bindLong(15, entity.getTimeoutSec());
        statement.bindLong(16, entity.getMaxSpeedBps());
        statement.bindString(17, entity.getMirrors());
        statement.bindString(18, entity.getChecksumAlgo());
        statement.bindString(19, entity.getChecksumExpected());
        statement.bindString(20, entity.getScheduleFrom());
        statement.bindString(21, entity.getScheduleTo());
        statement.bindLong(22, entity.getHlsSegmentCount());
        statement.bindLong(23, entity.getHlsSegmentsDone());
        final String _tmp_3 = __converters.fromStatus(entity.getStatus());
        statement.bindString(24, _tmp_3);
        statement.bindDouble(25, entity.getProgress());
        statement.bindLong(26, entity.getDownloadedBytes());
        statement.bindLong(27, entity.getTotalBytes());
        statement.bindLong(28, entity.getSpeedBps());
        statement.bindLong(29, entity.getEtaSeconds());
        statement.bindLong(30, entity.getActiveThreads());
        statement.bindLong(31, entity.getCreatedAt());
        if (entity.getCompletedAt() == null) {
          statement.bindNull(32);
        } else {
          statement.bindLong(32, entity.getCompletedAt());
        }
        if (entity.getErrorMessage() == null) {
          statement.bindNull(33);
        } else {
          statement.bindString(33, entity.getErrorMessage());
        }
        statement.bindLong(34, entity.getQueuePriority());
      }
    };
    this.__deletionAdapterOfDownloadItem = new EntityDeletionOrUpdateAdapter<DownloadItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `downloads` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DownloadItem entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfDownloadItem = new EntityDeletionOrUpdateAdapter<DownloadItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `downloads` SET `id` = ?,`url` = ?,`filename` = ?,`outputDir` = ?,`mode` = ?,`threads` = ?,`proxy` = ?,`userAgent` = ?,`customHeaders` = ?,`cookies` = ?,`httpMethod` = ?,`useResume` = ?,`verbose` = ?,`maxRetries` = ?,`timeoutSec` = ?,`maxSpeedBps` = ?,`mirrors` = ?,`checksumAlgo` = ?,`checksumExpected` = ?,`scheduleFrom` = ?,`scheduleTo` = ?,`hlsSegmentCount` = ?,`hlsSegmentsDone` = ?,`status` = ?,`progress` = ?,`downloadedBytes` = ?,`totalBytes` = ?,`speedBps` = ?,`etaSeconds` = ?,`activeThreads` = ?,`createdAt` = ?,`completedAt` = ?,`errorMessage` = ?,`queuePriority` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DownloadItem entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getUrl());
        statement.bindString(3, entity.getFilename());
        statement.bindString(4, entity.getOutputDir());
        final String _tmp = __converters.fromMode(entity.getMode());
        statement.bindString(5, _tmp);
        statement.bindLong(6, entity.getThreads());
        if (entity.getProxy() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getProxy());
        }
        statement.bindString(8, entity.getUserAgent());
        statement.bindString(9, entity.getCustomHeaders());
        statement.bindString(10, entity.getCookies());
        statement.bindString(11, entity.getHttpMethod());
        final int _tmp_1 = entity.getUseResume() ? 1 : 0;
        statement.bindLong(12, _tmp_1);
        final int _tmp_2 = entity.getVerbose() ? 1 : 0;
        statement.bindLong(13, _tmp_2);
        statement.bindLong(14, entity.getMaxRetries());
        statement.bindLong(15, entity.getTimeoutSec());
        statement.bindLong(16, entity.getMaxSpeedBps());
        statement.bindString(17, entity.getMirrors());
        statement.bindString(18, entity.getChecksumAlgo());
        statement.bindString(19, entity.getChecksumExpected());
        statement.bindString(20, entity.getScheduleFrom());
        statement.bindString(21, entity.getScheduleTo());
        statement.bindLong(22, entity.getHlsSegmentCount());
        statement.bindLong(23, entity.getHlsSegmentsDone());
        final String _tmp_3 = __converters.fromStatus(entity.getStatus());
        statement.bindString(24, _tmp_3);
        statement.bindDouble(25, entity.getProgress());
        statement.bindLong(26, entity.getDownloadedBytes());
        statement.bindLong(27, entity.getTotalBytes());
        statement.bindLong(28, entity.getSpeedBps());
        statement.bindLong(29, entity.getEtaSeconds());
        statement.bindLong(30, entity.getActiveThreads());
        statement.bindLong(31, entity.getCreatedAt());
        if (entity.getCompletedAt() == null) {
          statement.bindNull(32);
        } else {
          statement.bindLong(32, entity.getCompletedAt());
        }
        if (entity.getErrorMessage() == null) {
          statement.bindNull(33);
        } else {
          statement.bindString(33, entity.getErrorMessage());
        }
        statement.bindLong(34, entity.getQueuePriority());
        statement.bindLong(35, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM downloads WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateProgress = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE downloads SET status = ?, progress = ?, downloadedBytes = ?, totalBytes = ?, speedBps = ?, etaSeconds = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkCompleted = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE downloads SET status = ?, completedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkFailed = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE downloads SET status = ?, errorMessage = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final DownloadItem item, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfDownloadItem.insertAndReturnId(item);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final DownloadItem item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDownloadItem.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final DownloadItem item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfDownloadItem.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
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
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateProgress(final long id, final DownloadStatus status, final float progress,
      final long downloaded, final long total, final long speed, final int eta,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateProgress.acquire();
        int _argIndex = 1;
        final String _tmp = __converters.fromStatus(status);
        _stmt.bindString(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindDouble(_argIndex, progress);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, downloaded);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, total);
        _argIndex = 5;
        _stmt.bindLong(_argIndex, speed);
        _argIndex = 6;
        _stmt.bindLong(_argIndex, eta);
        _argIndex = 7;
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
          __preparedStmtOfUpdateProgress.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markCompleted(final long id, final DownloadStatus status, final long completedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkCompleted.acquire();
        int _argIndex = 1;
        final String _tmp = __converters.fromStatus(status);
        _stmt.bindString(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, completedAt);
        _argIndex = 3;
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
          __preparedStmtOfMarkCompleted.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markFailed(final long id, final DownloadStatus status, final String error,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkFailed.acquire();
        int _argIndex = 1;
        final String _tmp = __converters.fromStatus(status);
        _stmt.bindString(_argIndex, _tmp);
        _argIndex = 2;
        if (error == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, error);
        }
        _argIndex = 3;
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
          __preparedStmtOfMarkFailed.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<DownloadItem>> observeAll() {
    final String _sql = "SELECT * FROM downloads ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"downloads"}, new Callable<List<DownloadItem>>() {
      @Override
      @NonNull
      public List<DownloadItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfFilename = CursorUtil.getColumnIndexOrThrow(_cursor, "filename");
          final int _cursorIndexOfOutputDir = CursorUtil.getColumnIndexOrThrow(_cursor, "outputDir");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfThreads = CursorUtil.getColumnIndexOrThrow(_cursor, "threads");
          final int _cursorIndexOfProxy = CursorUtil.getColumnIndexOrThrow(_cursor, "proxy");
          final int _cursorIndexOfUserAgent = CursorUtil.getColumnIndexOrThrow(_cursor, "userAgent");
          final int _cursorIndexOfCustomHeaders = CursorUtil.getColumnIndexOrThrow(_cursor, "customHeaders");
          final int _cursorIndexOfCookies = CursorUtil.getColumnIndexOrThrow(_cursor, "cookies");
          final int _cursorIndexOfHttpMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "httpMethod");
          final int _cursorIndexOfUseResume = CursorUtil.getColumnIndexOrThrow(_cursor, "useResume");
          final int _cursorIndexOfVerbose = CursorUtil.getColumnIndexOrThrow(_cursor, "verbose");
          final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
          final int _cursorIndexOfTimeoutSec = CursorUtil.getColumnIndexOrThrow(_cursor, "timeoutSec");
          final int _cursorIndexOfMaxSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "maxSpeedBps");
          final int _cursorIndexOfMirrors = CursorUtil.getColumnIndexOrThrow(_cursor, "mirrors");
          final int _cursorIndexOfChecksumAlgo = CursorUtil.getColumnIndexOrThrow(_cursor, "checksumAlgo");
          final int _cursorIndexOfChecksumExpected = CursorUtil.getColumnIndexOrThrow(_cursor, "checksumExpected");
          final int _cursorIndexOfScheduleFrom = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleFrom");
          final int _cursorIndexOfScheduleTo = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleTo");
          final int _cursorIndexOfHlsSegmentCount = CursorUtil.getColumnIndexOrThrow(_cursor, "hlsSegmentCount");
          final int _cursorIndexOfHlsSegmentsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "hlsSegmentsDone");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfDownloadedBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedBytes");
          final int _cursorIndexOfTotalBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBytes");
          final int _cursorIndexOfSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "speedBps");
          final int _cursorIndexOfEtaSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "etaSeconds");
          final int _cursorIndexOfActiveThreads = CursorUtil.getColumnIndexOrThrow(_cursor, "activeThreads");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final int _cursorIndexOfQueuePriority = CursorUtil.getColumnIndexOrThrow(_cursor, "queuePriority");
          final List<DownloadItem> _result = new ArrayList<DownloadItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DownloadItem _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpFilename;
            _tmpFilename = _cursor.getString(_cursorIndexOfFilename);
            final String _tmpOutputDir;
            _tmpOutputDir = _cursor.getString(_cursorIndexOfOutputDir);
            final DownloadMode _tmpMode;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfMode);
            _tmpMode = __converters.toMode(_tmp);
            final int _tmpThreads;
            _tmpThreads = _cursor.getInt(_cursorIndexOfThreads);
            final String _tmpProxy;
            if (_cursor.isNull(_cursorIndexOfProxy)) {
              _tmpProxy = null;
            } else {
              _tmpProxy = _cursor.getString(_cursorIndexOfProxy);
            }
            final String _tmpUserAgent;
            _tmpUserAgent = _cursor.getString(_cursorIndexOfUserAgent);
            final String _tmpCustomHeaders;
            _tmpCustomHeaders = _cursor.getString(_cursorIndexOfCustomHeaders);
            final String _tmpCookies;
            _tmpCookies = _cursor.getString(_cursorIndexOfCookies);
            final String _tmpHttpMethod;
            _tmpHttpMethod = _cursor.getString(_cursorIndexOfHttpMethod);
            final boolean _tmpUseResume;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfUseResume);
            _tmpUseResume = _tmp_1 != 0;
            final boolean _tmpVerbose;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfVerbose);
            _tmpVerbose = _tmp_2 != 0;
            final int _tmpMaxRetries;
            _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
            final int _tmpTimeoutSec;
            _tmpTimeoutSec = _cursor.getInt(_cursorIndexOfTimeoutSec);
            final long _tmpMaxSpeedBps;
            _tmpMaxSpeedBps = _cursor.getLong(_cursorIndexOfMaxSpeedBps);
            final String _tmpMirrors;
            _tmpMirrors = _cursor.getString(_cursorIndexOfMirrors);
            final String _tmpChecksumAlgo;
            _tmpChecksumAlgo = _cursor.getString(_cursorIndexOfChecksumAlgo);
            final String _tmpChecksumExpected;
            _tmpChecksumExpected = _cursor.getString(_cursorIndexOfChecksumExpected);
            final String _tmpScheduleFrom;
            _tmpScheduleFrom = _cursor.getString(_cursorIndexOfScheduleFrom);
            final String _tmpScheduleTo;
            _tmpScheduleTo = _cursor.getString(_cursorIndexOfScheduleTo);
            final int _tmpHlsSegmentCount;
            _tmpHlsSegmentCount = _cursor.getInt(_cursorIndexOfHlsSegmentCount);
            final int _tmpHlsSegmentsDone;
            _tmpHlsSegmentsDone = _cursor.getInt(_cursorIndexOfHlsSegmentsDone);
            final DownloadStatus _tmpStatus;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toStatus(_tmp_3);
            final float _tmpProgress;
            _tmpProgress = _cursor.getFloat(_cursorIndexOfProgress);
            final long _tmpDownloadedBytes;
            _tmpDownloadedBytes = _cursor.getLong(_cursorIndexOfDownloadedBytes);
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final long _tmpSpeedBps;
            _tmpSpeedBps = _cursor.getLong(_cursorIndexOfSpeedBps);
            final int _tmpEtaSeconds;
            _tmpEtaSeconds = _cursor.getInt(_cursorIndexOfEtaSeconds);
            final int _tmpActiveThreads;
            _tmpActiveThreads = _cursor.getInt(_cursorIndexOfActiveThreads);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            final int _tmpQueuePriority;
            _tmpQueuePriority = _cursor.getInt(_cursorIndexOfQueuePriority);
            _item = new DownloadItem(_tmpId,_tmpUrl,_tmpFilename,_tmpOutputDir,_tmpMode,_tmpThreads,_tmpProxy,_tmpUserAgent,_tmpCustomHeaders,_tmpCookies,_tmpHttpMethod,_tmpUseResume,_tmpVerbose,_tmpMaxRetries,_tmpTimeoutSec,_tmpMaxSpeedBps,_tmpMirrors,_tmpChecksumAlgo,_tmpChecksumExpected,_tmpScheduleFrom,_tmpScheduleTo,_tmpHlsSegmentCount,_tmpHlsSegmentsDone,_tmpStatus,_tmpProgress,_tmpDownloadedBytes,_tmpTotalBytes,_tmpSpeedBps,_tmpEtaSeconds,_tmpActiveThreads,_tmpCreatedAt,_tmpCompletedAt,_tmpErrorMessage,_tmpQueuePriority);
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
  public Object getAllSync(final Continuation<? super List<DownloadItem>> $completion) {
    final String _sql = "SELECT * FROM downloads ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DownloadItem>>() {
      @Override
      @NonNull
      public List<DownloadItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfFilename = CursorUtil.getColumnIndexOrThrow(_cursor, "filename");
          final int _cursorIndexOfOutputDir = CursorUtil.getColumnIndexOrThrow(_cursor, "outputDir");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfThreads = CursorUtil.getColumnIndexOrThrow(_cursor, "threads");
          final int _cursorIndexOfProxy = CursorUtil.getColumnIndexOrThrow(_cursor, "proxy");
          final int _cursorIndexOfUserAgent = CursorUtil.getColumnIndexOrThrow(_cursor, "userAgent");
          final int _cursorIndexOfCustomHeaders = CursorUtil.getColumnIndexOrThrow(_cursor, "customHeaders");
          final int _cursorIndexOfCookies = CursorUtil.getColumnIndexOrThrow(_cursor, "cookies");
          final int _cursorIndexOfHttpMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "httpMethod");
          final int _cursorIndexOfUseResume = CursorUtil.getColumnIndexOrThrow(_cursor, "useResume");
          final int _cursorIndexOfVerbose = CursorUtil.getColumnIndexOrThrow(_cursor, "verbose");
          final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
          final int _cursorIndexOfTimeoutSec = CursorUtil.getColumnIndexOrThrow(_cursor, "timeoutSec");
          final int _cursorIndexOfMaxSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "maxSpeedBps");
          final int _cursorIndexOfMirrors = CursorUtil.getColumnIndexOrThrow(_cursor, "mirrors");
          final int _cursorIndexOfChecksumAlgo = CursorUtil.getColumnIndexOrThrow(_cursor, "checksumAlgo");
          final int _cursorIndexOfChecksumExpected = CursorUtil.getColumnIndexOrThrow(_cursor, "checksumExpected");
          final int _cursorIndexOfScheduleFrom = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleFrom");
          final int _cursorIndexOfScheduleTo = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleTo");
          final int _cursorIndexOfHlsSegmentCount = CursorUtil.getColumnIndexOrThrow(_cursor, "hlsSegmentCount");
          final int _cursorIndexOfHlsSegmentsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "hlsSegmentsDone");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfDownloadedBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedBytes");
          final int _cursorIndexOfTotalBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBytes");
          final int _cursorIndexOfSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "speedBps");
          final int _cursorIndexOfEtaSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "etaSeconds");
          final int _cursorIndexOfActiveThreads = CursorUtil.getColumnIndexOrThrow(_cursor, "activeThreads");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final int _cursorIndexOfQueuePriority = CursorUtil.getColumnIndexOrThrow(_cursor, "queuePriority");
          final List<DownloadItem> _result = new ArrayList<DownloadItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DownloadItem _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpFilename;
            _tmpFilename = _cursor.getString(_cursorIndexOfFilename);
            final String _tmpOutputDir;
            _tmpOutputDir = _cursor.getString(_cursorIndexOfOutputDir);
            final DownloadMode _tmpMode;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfMode);
            _tmpMode = __converters.toMode(_tmp);
            final int _tmpThreads;
            _tmpThreads = _cursor.getInt(_cursorIndexOfThreads);
            final String _tmpProxy;
            if (_cursor.isNull(_cursorIndexOfProxy)) {
              _tmpProxy = null;
            } else {
              _tmpProxy = _cursor.getString(_cursorIndexOfProxy);
            }
            final String _tmpUserAgent;
            _tmpUserAgent = _cursor.getString(_cursorIndexOfUserAgent);
            final String _tmpCustomHeaders;
            _tmpCustomHeaders = _cursor.getString(_cursorIndexOfCustomHeaders);
            final String _tmpCookies;
            _tmpCookies = _cursor.getString(_cursorIndexOfCookies);
            final String _tmpHttpMethod;
            _tmpHttpMethod = _cursor.getString(_cursorIndexOfHttpMethod);
            final boolean _tmpUseResume;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfUseResume);
            _tmpUseResume = _tmp_1 != 0;
            final boolean _tmpVerbose;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfVerbose);
            _tmpVerbose = _tmp_2 != 0;
            final int _tmpMaxRetries;
            _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
            final int _tmpTimeoutSec;
            _tmpTimeoutSec = _cursor.getInt(_cursorIndexOfTimeoutSec);
            final long _tmpMaxSpeedBps;
            _tmpMaxSpeedBps = _cursor.getLong(_cursorIndexOfMaxSpeedBps);
            final String _tmpMirrors;
            _tmpMirrors = _cursor.getString(_cursorIndexOfMirrors);
            final String _tmpChecksumAlgo;
            _tmpChecksumAlgo = _cursor.getString(_cursorIndexOfChecksumAlgo);
            final String _tmpChecksumExpected;
            _tmpChecksumExpected = _cursor.getString(_cursorIndexOfChecksumExpected);
            final String _tmpScheduleFrom;
            _tmpScheduleFrom = _cursor.getString(_cursorIndexOfScheduleFrom);
            final String _tmpScheduleTo;
            _tmpScheduleTo = _cursor.getString(_cursorIndexOfScheduleTo);
            final int _tmpHlsSegmentCount;
            _tmpHlsSegmentCount = _cursor.getInt(_cursorIndexOfHlsSegmentCount);
            final int _tmpHlsSegmentsDone;
            _tmpHlsSegmentsDone = _cursor.getInt(_cursorIndexOfHlsSegmentsDone);
            final DownloadStatus _tmpStatus;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toStatus(_tmp_3);
            final float _tmpProgress;
            _tmpProgress = _cursor.getFloat(_cursorIndexOfProgress);
            final long _tmpDownloadedBytes;
            _tmpDownloadedBytes = _cursor.getLong(_cursorIndexOfDownloadedBytes);
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final long _tmpSpeedBps;
            _tmpSpeedBps = _cursor.getLong(_cursorIndexOfSpeedBps);
            final int _tmpEtaSeconds;
            _tmpEtaSeconds = _cursor.getInt(_cursorIndexOfEtaSeconds);
            final int _tmpActiveThreads;
            _tmpActiveThreads = _cursor.getInt(_cursorIndexOfActiveThreads);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            final int _tmpQueuePriority;
            _tmpQueuePriority = _cursor.getInt(_cursorIndexOfQueuePriority);
            _item = new DownloadItem(_tmpId,_tmpUrl,_tmpFilename,_tmpOutputDir,_tmpMode,_tmpThreads,_tmpProxy,_tmpUserAgent,_tmpCustomHeaders,_tmpCookies,_tmpHttpMethod,_tmpUseResume,_tmpVerbose,_tmpMaxRetries,_tmpTimeoutSec,_tmpMaxSpeedBps,_tmpMirrors,_tmpChecksumAlgo,_tmpChecksumExpected,_tmpScheduleFrom,_tmpScheduleTo,_tmpHlsSegmentCount,_tmpHlsSegmentsDone,_tmpStatus,_tmpProgress,_tmpDownloadedBytes,_tmpTotalBytes,_tmpSpeedBps,_tmpEtaSeconds,_tmpActiveThreads,_tmpCreatedAt,_tmpCompletedAt,_tmpErrorMessage,_tmpQueuePriority);
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
  public Flow<List<DownloadItem>> observeByStatus(final DownloadStatus status) {
    final String _sql = "SELECT * FROM downloads WHERE status = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final String _tmp = __converters.fromStatus(status);
    _statement.bindString(_argIndex, _tmp);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"downloads"}, new Callable<List<DownloadItem>>() {
      @Override
      @NonNull
      public List<DownloadItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfFilename = CursorUtil.getColumnIndexOrThrow(_cursor, "filename");
          final int _cursorIndexOfOutputDir = CursorUtil.getColumnIndexOrThrow(_cursor, "outputDir");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfThreads = CursorUtil.getColumnIndexOrThrow(_cursor, "threads");
          final int _cursorIndexOfProxy = CursorUtil.getColumnIndexOrThrow(_cursor, "proxy");
          final int _cursorIndexOfUserAgent = CursorUtil.getColumnIndexOrThrow(_cursor, "userAgent");
          final int _cursorIndexOfCustomHeaders = CursorUtil.getColumnIndexOrThrow(_cursor, "customHeaders");
          final int _cursorIndexOfCookies = CursorUtil.getColumnIndexOrThrow(_cursor, "cookies");
          final int _cursorIndexOfHttpMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "httpMethod");
          final int _cursorIndexOfUseResume = CursorUtil.getColumnIndexOrThrow(_cursor, "useResume");
          final int _cursorIndexOfVerbose = CursorUtil.getColumnIndexOrThrow(_cursor, "verbose");
          final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
          final int _cursorIndexOfTimeoutSec = CursorUtil.getColumnIndexOrThrow(_cursor, "timeoutSec");
          final int _cursorIndexOfMaxSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "maxSpeedBps");
          final int _cursorIndexOfMirrors = CursorUtil.getColumnIndexOrThrow(_cursor, "mirrors");
          final int _cursorIndexOfChecksumAlgo = CursorUtil.getColumnIndexOrThrow(_cursor, "checksumAlgo");
          final int _cursorIndexOfChecksumExpected = CursorUtil.getColumnIndexOrThrow(_cursor, "checksumExpected");
          final int _cursorIndexOfScheduleFrom = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleFrom");
          final int _cursorIndexOfScheduleTo = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleTo");
          final int _cursorIndexOfHlsSegmentCount = CursorUtil.getColumnIndexOrThrow(_cursor, "hlsSegmentCount");
          final int _cursorIndexOfHlsSegmentsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "hlsSegmentsDone");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfDownloadedBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedBytes");
          final int _cursorIndexOfTotalBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBytes");
          final int _cursorIndexOfSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "speedBps");
          final int _cursorIndexOfEtaSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "etaSeconds");
          final int _cursorIndexOfActiveThreads = CursorUtil.getColumnIndexOrThrow(_cursor, "activeThreads");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final int _cursorIndexOfQueuePriority = CursorUtil.getColumnIndexOrThrow(_cursor, "queuePriority");
          final List<DownloadItem> _result = new ArrayList<DownloadItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DownloadItem _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpFilename;
            _tmpFilename = _cursor.getString(_cursorIndexOfFilename);
            final String _tmpOutputDir;
            _tmpOutputDir = _cursor.getString(_cursorIndexOfOutputDir);
            final DownloadMode _tmpMode;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfMode);
            _tmpMode = __converters.toMode(_tmp_1);
            final int _tmpThreads;
            _tmpThreads = _cursor.getInt(_cursorIndexOfThreads);
            final String _tmpProxy;
            if (_cursor.isNull(_cursorIndexOfProxy)) {
              _tmpProxy = null;
            } else {
              _tmpProxy = _cursor.getString(_cursorIndexOfProxy);
            }
            final String _tmpUserAgent;
            _tmpUserAgent = _cursor.getString(_cursorIndexOfUserAgent);
            final String _tmpCustomHeaders;
            _tmpCustomHeaders = _cursor.getString(_cursorIndexOfCustomHeaders);
            final String _tmpCookies;
            _tmpCookies = _cursor.getString(_cursorIndexOfCookies);
            final String _tmpHttpMethod;
            _tmpHttpMethod = _cursor.getString(_cursorIndexOfHttpMethod);
            final boolean _tmpUseResume;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfUseResume);
            _tmpUseResume = _tmp_2 != 0;
            final boolean _tmpVerbose;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfVerbose);
            _tmpVerbose = _tmp_3 != 0;
            final int _tmpMaxRetries;
            _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
            final int _tmpTimeoutSec;
            _tmpTimeoutSec = _cursor.getInt(_cursorIndexOfTimeoutSec);
            final long _tmpMaxSpeedBps;
            _tmpMaxSpeedBps = _cursor.getLong(_cursorIndexOfMaxSpeedBps);
            final String _tmpMirrors;
            _tmpMirrors = _cursor.getString(_cursorIndexOfMirrors);
            final String _tmpChecksumAlgo;
            _tmpChecksumAlgo = _cursor.getString(_cursorIndexOfChecksumAlgo);
            final String _tmpChecksumExpected;
            _tmpChecksumExpected = _cursor.getString(_cursorIndexOfChecksumExpected);
            final String _tmpScheduleFrom;
            _tmpScheduleFrom = _cursor.getString(_cursorIndexOfScheduleFrom);
            final String _tmpScheduleTo;
            _tmpScheduleTo = _cursor.getString(_cursorIndexOfScheduleTo);
            final int _tmpHlsSegmentCount;
            _tmpHlsSegmentCount = _cursor.getInt(_cursorIndexOfHlsSegmentCount);
            final int _tmpHlsSegmentsDone;
            _tmpHlsSegmentsDone = _cursor.getInt(_cursorIndexOfHlsSegmentsDone);
            final DownloadStatus _tmpStatus;
            final String _tmp_4;
            _tmp_4 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toStatus(_tmp_4);
            final float _tmpProgress;
            _tmpProgress = _cursor.getFloat(_cursorIndexOfProgress);
            final long _tmpDownloadedBytes;
            _tmpDownloadedBytes = _cursor.getLong(_cursorIndexOfDownloadedBytes);
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final long _tmpSpeedBps;
            _tmpSpeedBps = _cursor.getLong(_cursorIndexOfSpeedBps);
            final int _tmpEtaSeconds;
            _tmpEtaSeconds = _cursor.getInt(_cursorIndexOfEtaSeconds);
            final int _tmpActiveThreads;
            _tmpActiveThreads = _cursor.getInt(_cursorIndexOfActiveThreads);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            final int _tmpQueuePriority;
            _tmpQueuePriority = _cursor.getInt(_cursorIndexOfQueuePriority);
            _item = new DownloadItem(_tmpId,_tmpUrl,_tmpFilename,_tmpOutputDir,_tmpMode,_tmpThreads,_tmpProxy,_tmpUserAgent,_tmpCustomHeaders,_tmpCookies,_tmpHttpMethod,_tmpUseResume,_tmpVerbose,_tmpMaxRetries,_tmpTimeoutSec,_tmpMaxSpeedBps,_tmpMirrors,_tmpChecksumAlgo,_tmpChecksumExpected,_tmpScheduleFrom,_tmpScheduleTo,_tmpHlsSegmentCount,_tmpHlsSegmentsDone,_tmpStatus,_tmpProgress,_tmpDownloadedBytes,_tmpTotalBytes,_tmpSpeedBps,_tmpEtaSeconds,_tmpActiveThreads,_tmpCreatedAt,_tmpCompletedAt,_tmpErrorMessage,_tmpQueuePriority);
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
  public Object getById(final long id, final Continuation<? super DownloadItem> $completion) {
    final String _sql = "SELECT * FROM downloads WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DownloadItem>() {
      @Override
      @Nullable
      public DownloadItem call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfFilename = CursorUtil.getColumnIndexOrThrow(_cursor, "filename");
          final int _cursorIndexOfOutputDir = CursorUtil.getColumnIndexOrThrow(_cursor, "outputDir");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfThreads = CursorUtil.getColumnIndexOrThrow(_cursor, "threads");
          final int _cursorIndexOfProxy = CursorUtil.getColumnIndexOrThrow(_cursor, "proxy");
          final int _cursorIndexOfUserAgent = CursorUtil.getColumnIndexOrThrow(_cursor, "userAgent");
          final int _cursorIndexOfCustomHeaders = CursorUtil.getColumnIndexOrThrow(_cursor, "customHeaders");
          final int _cursorIndexOfCookies = CursorUtil.getColumnIndexOrThrow(_cursor, "cookies");
          final int _cursorIndexOfHttpMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "httpMethod");
          final int _cursorIndexOfUseResume = CursorUtil.getColumnIndexOrThrow(_cursor, "useResume");
          final int _cursorIndexOfVerbose = CursorUtil.getColumnIndexOrThrow(_cursor, "verbose");
          final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
          final int _cursorIndexOfTimeoutSec = CursorUtil.getColumnIndexOrThrow(_cursor, "timeoutSec");
          final int _cursorIndexOfMaxSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "maxSpeedBps");
          final int _cursorIndexOfMirrors = CursorUtil.getColumnIndexOrThrow(_cursor, "mirrors");
          final int _cursorIndexOfChecksumAlgo = CursorUtil.getColumnIndexOrThrow(_cursor, "checksumAlgo");
          final int _cursorIndexOfChecksumExpected = CursorUtil.getColumnIndexOrThrow(_cursor, "checksumExpected");
          final int _cursorIndexOfScheduleFrom = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleFrom");
          final int _cursorIndexOfScheduleTo = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduleTo");
          final int _cursorIndexOfHlsSegmentCount = CursorUtil.getColumnIndexOrThrow(_cursor, "hlsSegmentCount");
          final int _cursorIndexOfHlsSegmentsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "hlsSegmentsDone");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfDownloadedBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedBytes");
          final int _cursorIndexOfTotalBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBytes");
          final int _cursorIndexOfSpeedBps = CursorUtil.getColumnIndexOrThrow(_cursor, "speedBps");
          final int _cursorIndexOfEtaSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "etaSeconds");
          final int _cursorIndexOfActiveThreads = CursorUtil.getColumnIndexOrThrow(_cursor, "activeThreads");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final int _cursorIndexOfQueuePriority = CursorUtil.getColumnIndexOrThrow(_cursor, "queuePriority");
          final DownloadItem _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpFilename;
            _tmpFilename = _cursor.getString(_cursorIndexOfFilename);
            final String _tmpOutputDir;
            _tmpOutputDir = _cursor.getString(_cursorIndexOfOutputDir);
            final DownloadMode _tmpMode;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfMode);
            _tmpMode = __converters.toMode(_tmp);
            final int _tmpThreads;
            _tmpThreads = _cursor.getInt(_cursorIndexOfThreads);
            final String _tmpProxy;
            if (_cursor.isNull(_cursorIndexOfProxy)) {
              _tmpProxy = null;
            } else {
              _tmpProxy = _cursor.getString(_cursorIndexOfProxy);
            }
            final String _tmpUserAgent;
            _tmpUserAgent = _cursor.getString(_cursorIndexOfUserAgent);
            final String _tmpCustomHeaders;
            _tmpCustomHeaders = _cursor.getString(_cursorIndexOfCustomHeaders);
            final String _tmpCookies;
            _tmpCookies = _cursor.getString(_cursorIndexOfCookies);
            final String _tmpHttpMethod;
            _tmpHttpMethod = _cursor.getString(_cursorIndexOfHttpMethod);
            final boolean _tmpUseResume;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfUseResume);
            _tmpUseResume = _tmp_1 != 0;
            final boolean _tmpVerbose;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfVerbose);
            _tmpVerbose = _tmp_2 != 0;
            final int _tmpMaxRetries;
            _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
            final int _tmpTimeoutSec;
            _tmpTimeoutSec = _cursor.getInt(_cursorIndexOfTimeoutSec);
            final long _tmpMaxSpeedBps;
            _tmpMaxSpeedBps = _cursor.getLong(_cursorIndexOfMaxSpeedBps);
            final String _tmpMirrors;
            _tmpMirrors = _cursor.getString(_cursorIndexOfMirrors);
            final String _tmpChecksumAlgo;
            _tmpChecksumAlgo = _cursor.getString(_cursorIndexOfChecksumAlgo);
            final String _tmpChecksumExpected;
            _tmpChecksumExpected = _cursor.getString(_cursorIndexOfChecksumExpected);
            final String _tmpScheduleFrom;
            _tmpScheduleFrom = _cursor.getString(_cursorIndexOfScheduleFrom);
            final String _tmpScheduleTo;
            _tmpScheduleTo = _cursor.getString(_cursorIndexOfScheduleTo);
            final int _tmpHlsSegmentCount;
            _tmpHlsSegmentCount = _cursor.getInt(_cursorIndexOfHlsSegmentCount);
            final int _tmpHlsSegmentsDone;
            _tmpHlsSegmentsDone = _cursor.getInt(_cursorIndexOfHlsSegmentsDone);
            final DownloadStatus _tmpStatus;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toStatus(_tmp_3);
            final float _tmpProgress;
            _tmpProgress = _cursor.getFloat(_cursorIndexOfProgress);
            final long _tmpDownloadedBytes;
            _tmpDownloadedBytes = _cursor.getLong(_cursorIndexOfDownloadedBytes);
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final long _tmpSpeedBps;
            _tmpSpeedBps = _cursor.getLong(_cursorIndexOfSpeedBps);
            final int _tmpEtaSeconds;
            _tmpEtaSeconds = _cursor.getInt(_cursorIndexOfEtaSeconds);
            final int _tmpActiveThreads;
            _tmpActiveThreads = _cursor.getInt(_cursorIndexOfActiveThreads);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            final int _tmpQueuePriority;
            _tmpQueuePriority = _cursor.getInt(_cursorIndexOfQueuePriority);
            _result = new DownloadItem(_tmpId,_tmpUrl,_tmpFilename,_tmpOutputDir,_tmpMode,_tmpThreads,_tmpProxy,_tmpUserAgent,_tmpCustomHeaders,_tmpCookies,_tmpHttpMethod,_tmpUseResume,_tmpVerbose,_tmpMaxRetries,_tmpTimeoutSec,_tmpMaxSpeedBps,_tmpMirrors,_tmpChecksumAlgo,_tmpChecksumExpected,_tmpScheduleFrom,_tmpScheduleTo,_tmpHlsSegmentCount,_tmpHlsSegmentsDone,_tmpStatus,_tmpProgress,_tmpDownloadedBytes,_tmpTotalBytes,_tmpSpeedBps,_tmpEtaSeconds,_tmpActiveThreads,_tmpCreatedAt,_tmpCompletedAt,_tmpErrorMessage,_tmpQueuePriority);
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

  @Override
  public Flow<Integer> countAll() {
    final String _sql = "SELECT COUNT(*) FROM downloads";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"downloads"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Flow<Integer> countCompleted() {
    final String _sql = "SELECT COUNT(*) FROM downloads WHERE status = 'COMPLETED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"downloads"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Flow<Long> totalDownloaded() {
    final String _sql = "SELECT SUM(downloadedBytes) FROM downloads WHERE status = 'COMPLETED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"downloads"}, new Callable<Long>() {
      @Override
      @Nullable
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final Long _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(0);
            }
            _result = _tmp;
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
