/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.media3.demo.surface;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.FileDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.drm.FrameworkMediaDrm;
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.LegacyPlayerControlView;
import java.io.File;
import java.util.UUID;

/**
 * Activity that demonstrates use of {@link SurfaceControl} with ExoPlayer.
 */
public final class MainActivity extends Activity {

//  private static final String DEFAULT_MEDIA_URI =
//      "https://storage.googleapis.com/exoplayer-test-media-1/mkv/android-screens-lavf-56.36.100-aac-avc-main-1280x720.mkv";

  //个人开发板1
//  private static final String DEFAULT_MEDIA_URI="file:///storage/emulated/0/Download/92647-720p.mp4";
  //铁盒
//  private static final String DEFAULT_MEDIA_URI = "/storage/emulated/legacy/Download/92647-720p.mp4";


  private static final String SURFACE_CONTROL_NAME = "surfacedemo";

  private static final String ACTION_VIEW = "androidx.media3.demo.surface.action.VIEW";
  private static final String EXTENSION_EXTRA = "extension";
  private static final String DRM_SCHEME_EXTRA = "drm_scheme";
  private static final String DRM_LICENSE_URL_EXTRA = "drm_license_url";
  private static final String OWNER_EXTRA = "owner";
  Uri uri = null;

  private boolean isOwner;
  @Nullable
  private LegacyPlayerControlView playerControlView;
  @Nullable
  private SurfaceView fullScreenView;
  @Nullable
  private SurfaceView nonFullScreenView;
  @Nullable
  private SurfaceView currentOutputView;

  @Nullable
  private static ExoPlayer player;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);
    playerControlView = findViewById(R.id.player_control_view);
    fullScreenView = findViewById(R.id.full_screen_view);
    fullScreenView.setOnClickListener(
        v -> {
          setCurrentOutputView(nonFullScreenView);
          Assertions.checkNotNull(fullScreenView).setVisibility(View.GONE);
        });
    attachSurfaceListener(fullScreenView);
    isOwner = getIntent().getBooleanExtra(OWNER_EXTRA, /* defaultValue= */ true);
    SurfaceView surfaceView = findViewById(R.id.grid_layout);
    attachSurfaceListener(surfaceView);
    if (nonFullScreenView == null) {
      nonFullScreenView = surfaceView;
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.i("--=", "Build.VERSION.SDK_INT=" + Build.VERSION.SDK_INT);

    if (isOwner && player == null) {
      Log.i("--=", "Build.VERSION.SDK_INT=" + Build.VERSION.SDK_INT + ";isOwner=" + isOwner);

      initializePlayer();
    }

    setCurrentOutputView(nonFullScreenView);

    if (playerControlView != null) {
      playerControlView.setPlayer(player);
      playerControlView.show();
    }

  }

  @Override
  public void onPause() {
    super.onPause();
    if (playerControlView != null) {
      playerControlView.setPlayer(null);
    }

  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (isOwner && isFinishing()) {
      if (player != null) {
        player.release();
        player = null;
      }
    }
  }

//  private void initializePlayer() {
//    Intent intent = getIntent();
//    String action = intent.getAction();
//    Uri uri =
//        ACTION_VIEW.equals(action)
//            ? Assertions.checkNotNull(intent.getData())
//            : Uri.parse(DEFAULT_MEDIA_URI);
//    DrmSessionManager drmSessionManager;
//    if (intent.hasExtra(DRM_SCHEME_EXTRA)) {
//      String drmScheme = Assertions.checkNotNull(intent.getStringExtra(DRM_SCHEME_EXTRA));
//      String drmLicenseUrl = Assertions.checkNotNull(intent.getStringExtra(DRM_LICENSE_URL_EXTRA));
//      UUID drmSchemeUuid = Assertions.checkNotNull(Util.getDrmUuid(drmScheme));
//      DataSource.Factory licenseDataSourceFactory = new DefaultHttpDataSource.Factory();
//      HttpMediaDrmCallback drmCallback =
//          new HttpMediaDrmCallback(drmLicenseUrl, licenseDataSourceFactory);
//      drmSessionManager =
//          new DefaultDrmSessionManager.Builder()
//              .setUuidAndExoMediaDrmProvider(drmSchemeUuid, FrameworkMediaDrm.DEFAULT_PROVIDER)
//              .build(drmCallback);
//    } else {
//      drmSessionManager = DrmSessionManager.DRM_UNSUPPORTED;
//    }
//
//    DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this);
//    MediaSource mediaSource;
//    @Nullable String fileExtension = intent.getStringExtra(EXTENSION_EXTRA);
//    @C.ContentType
//    int type =
//        TextUtils.isEmpty(fileExtension)
//            ? Util.inferContentType(uri)
//            : Util.inferContentTypeForExtension(fileExtension);
//    if (type == C.CONTENT_TYPE_DASH) {
//      mediaSource =
//          new DashMediaSource.Factory(dataSourceFactory)
//              .setDrmSessionManagerProvider(unusedMediaItem -> drmSessionManager)
//              .createMediaSource(MediaItem.fromUri(uri));
//    } else if (type == C.CONTENT_TYPE_OTHER) {
//      mediaSource =
//          new ProgressiveMediaSource.Factory(dataSourceFactory)
//              .setDrmSessionManagerProvider(unusedMediaItem -> drmSessionManager)
//              .createMediaSource(MediaItem.fromUri(uri));
//    } else {
//      IllegalStateException illegalStateException=new IllegalStateException();
//      Log.i("--=","Build.VERSION.SDK_INT="+Build.VERSION.SDK_INT+";异常="+Log.getStackTraceString(illegalStateException));
//
//      throw illegalStateException;
//    }
//    Log.i("--=","Build.VERSION.SDK_INT="+Build.VERSION.SDK_INT+";1");
//
//    ExoPlayer player = new ExoPlayer.Builder(getApplicationContext()).build();
//    player.setMediaSource(mediaSource);
//    player.prepare();
//    player.play();
//    player.setRepeatMode(Player.REPEAT_MODE_OFF);//Player.REPEAT_MODE_ALL循环播放,Player.REPEAT_MODE_OFF不循环播放
//    // 添加播放状态监听
//    player.addListener(new Player.Listener() {
//      @Override
//      public void onPlaybackStateChanged(@Player.State int playbackState) {
//        if (playbackState == Player.STATE_ENDED) {
//          Log.d("PlayerListener", "播放结束");
//          // 这里处理播放完成逻辑
//        }
//      }
//    });
//
//    Surface surface = nonFullScreenView.getHolder().getSurface();
//    player.setVideoSurface(surface);
//
//    MainActivity.player = player;
//  }

  private void initializePlayer() {

//    File file = new File(Environment.getExternalStorageDirectory(), "Download/92647-720p.mp4");
//    Uri uri = Uri.fromFile(file); // 注意：Android 7.0+ 需要使用 FileProvider，这里我们在 Android 5.1 不用

//    String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//        .getAbsolutePath() + "/92647-720p.mp4";
    String path = "/storage/sdcard0/Download/92647-720p.mp4";
//    String path = "https://storage.googleapis.com/exoplayer-test-media-1/mkv/android-screens-lavf-56.36.100-aac-avc-main-1280x720.mkv";

//    uri = Uri.fromFile(new File(path));

    if (path.startsWith("http") || path.startsWith("https")) {
      // 网络地址
      uri = Uri.parse(path);
    } else {
      // 本地路径（可以是 /storage/emulated/0/xxx.mp4）
      File file=new File(path);
      Log.d("FileCheck", "exists=" + file.exists() + ", canRead=" + file.canRead());

      uri = Uri.fromFile(file);
    }

    Context context = getApplicationContext();
    Log.i("--=",
        "Build.VERSION.SDK_INT=" + Build.VERSION.SDK_INT + ";1,path=" + uri.getPath());
//    MediaItem mediaItem = MediaItem.fromUri(uri);

    // 统一使用 DefaultDataSource.Factory（会自动判断是本地 or 网络）
    DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context);

// 创建媒体源
    MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
        .createMediaSource(MediaItem.fromUri(uri));

    ExoPlayer player = new ExoPlayer.Builder(getApplicationContext()).build();
    player.setMediaSource(mediaSource);
    player.prepare();
    player.play();
    player.setRepeatMode(
        Player.REPEAT_MODE_OFF);//Player.REPEAT_MODE_ALL循环播放,Player.REPEAT_MODE_OFF不循环播放
    // 添加播放状态监听
    player.addListener(new Player.Listener() {
      @Override
      public void onPlaybackStateChanged(@Player.State int playbackState) {
        if (playbackState == Player.STATE_ENDED) {
          Log.d("PlayerListener", "播放结束");
          // 这里处理播放完成逻辑
        }
      }
    });

    Surface surface = nonFullScreenView.getHolder().getSurface();
    player.setVideoSurface(surface);

    MainActivity.player = player;
  }


  private void setCurrentOutputView(@Nullable SurfaceView surfaceView) {
    currentOutputView = surfaceView;
    if (player != null && surfaceView != null) {
      Surface surface = surfaceView.getHolder().getSurface();
      player.setVideoSurface(surface);
    }
  }

  private void attachSurfaceListener(SurfaceView surfaceView) {
    surfaceView
        .getHolder()
        .addCallback(
            new SurfaceHolder.Callback() {
              @Override
              public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (surfaceView == currentOutputView) {
                  reparent(surfaceView);
                }
              }

              @Override
              public void surfaceChanged(
                  SurfaceHolder surfaceHolder, int format, int width, int height) {
              }

              @Override
              public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
              }
            });
  }

  private static void reparent(@Nullable SurfaceView surfaceView) {
  }
}
