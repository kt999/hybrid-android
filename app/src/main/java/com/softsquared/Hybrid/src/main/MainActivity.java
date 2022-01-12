package com.softsquared.Hybrid.src.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import com.softsquared.Hybrid.R;
import com.softsquared.Hybrid.src.BaseActivity;
import java.net.URISyntaxException;

public class MainActivity extends BaseActivity {
    public final Handler javascripthandler = new Handler();

    //웹 주소
    private String webHost = "10.20.181.253:3000";
    private String webHostUrl = "http://"+webHost+"/" ;

    public WebView mWebView;
    private WebSettings mWebSettings;


    // 마지막으로 뒤로가기 버튼을 눌렀던 시간 저장
    private long backKeyPressedTerminateTime = 0;
    // 첫 번째 뒤로가기 버튼을 누를때 표시
    private Toast toast;


    ValueCallback mFilePathCallback;

    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startService(new Intent(this, ForcedTerminationService.class));

        setContentView(R.layout.activity_main);

        //인터넷 연결상태 확인
        int status = MyConnectStatus.getConnectivityStatus(getApplicationContext());

        //인터넷 연결이 안되어있으면 앱 종료
        if(status == MyConnectStatus.TYPE_NOT_CONNECTED){

            toast = Toast.makeText(MainActivity.this, "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT);
            toast.show();

            finishAndRemoveTask();
        }

        // 웹뷰 셋팅
        mWebView = (WebView) findViewById(R.id.webView);                //xml 자바코드 연결

        mWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH); //더 높은 렌더링 우선 순위 설정 (API 18+에서 더 이상 사용되지 않음)

        //하드웨어 가속 활성화 / 비활성화 :
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // chromium, enable hardware acceleration
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            // older android version, disable hardware acceleration
            mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        mWebSettings = mWebView.getSettings();


        //시스템 폰트크기 무시
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mWebSettings.setTextZoom(100);
        }

        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setSupportMultipleWindows(true);                  //새창 띄우기 허용 여부
        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(false);   //자바스크립트 새창(멀티뷰) 띄우기 허용 여부
        mWebSettings.setUseWideViewPort(true);                          //화면 사이즈 맞추기 허용
        mWebSettings.setSupportZoom(false);                             //화면 줌 허용 여부
        mWebSettings.setBuiltInZoomControls(false);                     //화면 확대 축소 허용 여부
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);     //컨텐츠 사이즈 맞추기
        mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);           //브라우저 캐시 허용 여부
        mWebSettings.setDomStorageEnabled(true);                        //로컬저장소 허용 여부
        mWebSettings.setSaveFormData(true);                             //입력된 데이터 저장 허용 여부

        mWebView.addJavascriptInterface(new AndroidBridge(), "android");

        mWebView.setWebChromeClient(new WebChromeClient(){

            //alert 대응
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result)
            {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                result.confirm();
                return true;
            }

            //사진 업로드
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback filePathCallback, FileChooserParams fileChooserParams) {

                mFilePathCallback = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");

                startActivityForResult(intent, 0);
                return true;

            }


            @Override
            public boolean onCreateWindow( WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg )
            {
                view.removeAllViews();
                WebView childView = new WebView(view.getContext());

                childView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onCloseWindow(WebView window) {
                        window.setVisibility(View.GONE);
                        mWebView.removeView(window);
                    }
                });
                childView.getSettings().setJavaScriptEnabled(true);
                childView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
                childView.getSettings().setSupportMultipleWindows(false);
                childView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                childView.setWebViewClient(new PopupWebViewClient());
                view.addView(childView);
                WebView.WebViewTransport transport = (WebView.WebViewTransport)resultMsg.obj;
                transport.setWebView(childView);
                resultMsg.sendToTarget();

                return true;
            };

        });
        mWebView.setWebViewClient(new MyWebViewClient());
        mWebView.loadUrl(webHostUrl);                      //웹뷰 실행
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            //외부 APP 관련
            if (url != null && url.startsWith("intent:")) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    Intent existPackage = getPackageManager().getLaunchIntentForPackage(intent.getPackage());
                    if (existPackage != null) {
                        startActivity(intent);
                    } else {
                        Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                        marketIntent.setData(Uri.parse("market://details?id=" + intent.getPackage()));
                        startActivity(marketIntent);
                    }
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (url != null && url.startsWith("market://")) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        startActivity(intent);
                    }
                    return true;
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }

            //web 관련
            if (webHost.equals(Uri.parse(url).getHost())) {
                view.loadUrl(url);
                return true;
            }
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;
        }

    }

    private class PopupWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            //외부 APP 관련
            if (url != null && url.startsWith("intent:")) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    Intent existPackage = getPackageManager().getLaunchIntentForPackage(intent.getPackage());
                    if (existPackage != null) {
                        startActivity(intent);
                    } else {
                        Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                        marketIntent.setData(Uri.parse("market://details?id=" + intent.getPackage()));
                        startActivity(marketIntent);
                    }
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (url != null && url.startsWith("market://")) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        startActivity(intent);
                    }
                    return true;
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            } else if (url != null && (!url.startsWith("http://") && !url.startsWith("https://"))) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return true;
                }
            }

            try {

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);

                mWebView.removeAllViews();

                return true;

            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;
        }

    }



    //자바스크립트 통신 함수 (웹에서 호출 할 함수를 미리 만들어 둔 것)
    public class AndroidBridge {

        //versonName 가져오기 함수
        @JavascriptInterface
        public String getVersionName(){

            try {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(),0);
                return packageInfo.versionName;
            }
            catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return null;
            }

        }

        //종료 함수
        @JavascriptInterface
        public void terminateApp(){

            javascripthandler.post(new Runnable() {
                @Override
                public void run() {

                    // 마지막으로 뒤로가기 버튼을 눌렀던 시간에 2초를 더해 현재시간과 비교 후
                    // 마지막으로 뒤로가기 버튼을 눌렀던 시간이 2초가 지났으면 Toast Show
                    // 2000 milliseconds = 2 seconds
                    if (System.currentTimeMillis() > backKeyPressedTerminateTime + 2000) {
                        backKeyPressedTerminateTime = System.currentTimeMillis();
                        toast = Toast.makeText(MainActivity.this, "\'뒤로\' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }
                    // 마지막으로 뒤로가기 버튼을 눌렀던 시간에 2초를 더해 현재시간과 비교 후
                    // 마지막으로 뒤로가기 버튼을 눌렀던 시간이 2초가 지나지 않았으면 종료
                    // 현재 표시된 Toast 취소
                    if (System.currentTimeMillis() <= backKeyPressedTerminateTime + 2000) {
                        finish();
                        toast.cancel();
                    }
                }
            });

        }



    }


    //뒤로가기 두번 종료 (웹의 android_back_press() 란 이름의 함수 호출)
    @Override
    public void onBackPressed() {
        mWebView.loadUrl("javascript:android_back_press()");
    }

    //사진 업로드 관련 모듈
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.e("resultCode:: ", String.valueOf(resultCode));
        if(requestCode == 0 && resultCode == Activity.RESULT_OK){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mFilePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            }else{
                mFilePathCallback.onReceiveValue(new Uri[]{data.getData()});
            }
            mFilePathCallback = null;
        }else{
            mFilePathCallback.onReceiveValue(null);
        }
    }

}
