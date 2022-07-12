package device.policy.manager;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

public class DevicePolicyManagerPlugin
        implements FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.ActivityResultListener {

    private final String CHANNEL_TAG = "x-slayer/device_policy_manager";

    private MethodChannel channel;
    private Context appContext;
    private Activity mActivity;
    private Result pendingResult;
    final int REQUEST_CODE_FOR_DEVICE_POLICY_MANAGER = 2999;
    DevicePolicyManager deviceManger;
    ComponentName compName;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        appContext = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), CHANNEL_TAG);
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        compName = new ComponentName(appContext, DeviceAdmin.class);
        deviceManger = (DevicePolicyManager) appContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        pendingResult = result;

        switch (call.method) {
            case "enablePermission":
                String message = Objects.requireNonNull(call.argument("message")).toString();
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, message);
                mActivity.startActivityForResult(intent, REQUEST_CODE_FOR_DEVICE_POLICY_MANAGER);
                break;
            case "removeActiveAdmin":
                deviceManger.removeActiveAdmin(compName);
                break;
            case "isPermissionGranted":
                result.success(deviceManger.isAdminActive(compName));
                break;
            case "isCameraDisabled":
                result.success(deviceManger.getCameraDisabled(compName));
                break;
            case "lockScreen": {
                boolean active = deviceManger.isAdminActive(compName);
                if (active) {
                    deviceManger.lockNow();
                    result.success(null);
                } else {
                    result.error("ERROR", "You need to enable the Admin Device Features", null);
                }
                break;
            }
            case "insertLockTaskMode": {
                boolean active = deviceManger.isAdminActive(compName);

                if (active) {
                    String packageName = Objects.requireNonNull(call.argument("package-name")).toString();

                    deviceManger.setLockTaskPackages(compName, new String[]{packageName});

                    result.success(deviceManger.isLockTaskPermitted(packageName));
                } else {
                    result.error("ERROR", "You need to enable the Admin Device Features", null);
                }
                break;
            }
            case "removeLockTaskMode": {
                boolean active = deviceManger.isAdminActive(compName);

                if (active) {
                    String packageName = Objects.requireNonNull(call.argument("package-name")).toString();
                    List<String> packages = Arrays.asList(deviceManger.getLockTaskPackages(compName));
                    packages.removeIf(e -> e.equals(packageName));

                    deviceManger.setLockTaskPackages(compName, (String[]) packages.toArray());

                    result.success(!deviceManger.isLockTaskPermitted(packageName));
                } else {
                    result.error("ERROR", "You need to enable the Admin Device Features", null);
                }
                break;
            }
            case "isInLockTask": {
                boolean active = deviceManger.isAdminActive(compName);

                if (active) {
                    String packageName = Objects.requireNonNull(call.argument("package-name")).toString();

                    result.success(deviceManger.isLockTaskPermitted(packageName));
                } else {
                    result.error("ERROR", "You need to enable the Admin Device Features", null);
                }
                break;
            }
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding activityPluginBinding) {
        this.mActivity = activityPluginBinding.getActivity();
        activityPluginBinding.addActivityResultListener(this);
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding activityPluginBinding) {
        onAttachedToActivity(activityPluginBinding);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.mActivity = null;
    }

    @Override
    public void onDetachedFromActivity() {
        this.mActivity = null;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FOR_DEVICE_POLICY_MANAGER) {
            if (resultCode == Activity.RESULT_OK) {
                pendingResult.success(true);
            } else {
                pendingResult.success(false);
            }
            return true;
        }
        return false;
    }
}
