package com.example.devicemonitorapp.utils;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.devicemonitorapp.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * General purpose utility class.
 * Most related to internal tasks/helpers
 */
public final class Utils {

    /**
     * Runs a command line command and waits for its output
     *
     * @param command the command to tun
     * @param defaultOutput value to return in case of error
     * @return the command output or defaultOutput
     */
    public static String runCommand(String command, String defaultOutput) {
        List<String> results = Shell.sh(command).exec().getOut();
        if (ShellUtils.isValidOutput(results)) {
            return results.get(results.size() - 1);
        }
        return defaultOutput;
    }
    public static String runRootCommand(String command, String defaultOutput) {
        List<String> results = Shell.su(command).exec().getOut();
        if (ShellUtils.isValidOutput(results)) {
            return results.get(results.size() - 1);
        }
        return defaultOutput;
    }


}
