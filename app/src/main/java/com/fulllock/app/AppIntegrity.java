package com.fulllock.app;

import android.content.Context;
import android.content.pm.*;
import android.os.Build;
import java.security.MessageDigest;

/** Defense in depth against simple re-signing; not a replacement for server authorization. */
final class AppIntegrity {
    static boolean trusted(Context context) {
        if (BuildConfig.DEBUG) return true;
        try {
            if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) return false;
            if (!"com.fulllock.app".equals(context.getPackageName()) || BuildConfig.SIGNER_SHA256.length() != 64) return false;
            Signature[] signatures;
            if (Build.VERSION.SDK_INT >= 28) {
                PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
                if (info.signingInfo == null) return false;
                signatures=info.signingInfo.getApkContentsSigners();
            } else {
                signatures=context.getPackageManager().getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNATURES).signatures;
            }
            if (signatures == null || signatures.length != 1) return false;
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray());
            StringBuilder hex=new StringBuilder();
            for(byte b:digest)hex.append(String.format(java.util.Locale.ROOT,"%02x",b & 255));
            return MessageDigest.isEqual(hex.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII),BuildConfig.SIGNER_SHA256.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        } catch (Exception ignored) { return false; }
    }
}
