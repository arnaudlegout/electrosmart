/*
 * BSD 3-Clause License
 *
 *       Copyright (c) 2014-2022, Arnaud Legout (arnaudlegout), centre Inria de
 *       l'Université Côte d'Azur, France. Contact: arnaud.legout@inria.fr
 *       All rights reserved.
 *
 *       Redistribution and use in source and binary forms, with or without
 *       modification, are permitted provided that the following conditions are met:
 *
 *       1. Redistributions of source code must retain the above copyright notice, this
 *       list of conditions and the following disclaimer.
 *
 *       2. Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *
 *       3. Neither the name of the copyright holder nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 *       THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *       AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *       IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *       DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 *       FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *       DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *       SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *       CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *       OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *       OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package fr.inria.es.electrosmart.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.activities.MainActivity;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.signalsdatastructures.SignalsSlot;

/**
 * ArticleWebViewFragment is basically a webview. The URL of any page that needs to be shown in the
 * webview is passed to this fragment as a String argument.
 * <p>
 * We use this to show the ElectroSmart exposure index page that we have created on electrosmart.app.
 */
public class ArticleWebViewFragment extends MainActivityFragment {
    private static final String TAG = "ArticleWebViewFragment";
    private Context mContext;
    private MainActivity mActivity;
    private SignalsSlot mSignalsSlot;
    private ProgressBar mProgressbarLoading;

    /**
     * Creates a new instance of the fragment to which we pass {@code arguments} as arguments bundle.
     *
     * @param arguments the arguments bundle to pass to the newly created fragment
     * @return the created fragment
     */
    public static MainActivityFragment newInstance(Bundle arguments) {
        MainActivityFragment fragment = new ArticleWebViewFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_ARTICLE_WEBVIEW_FRAGMENT_ON_RESUME);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "in onPause()");
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_ARTICLE_WEBVIEW_FRAGMENT_ON_PAUSE);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "in onCreateView()");
        View mFragmentInfoView = inflater.inflate(R.layout.fragment_article_webview, container, false);
        WebView mWebView = mFragmentInfoView.findViewById(R.id.webview);
        // Not sure why, when we set the android:color for the webview in the xml, it does not work
        // So, doing it here and this seems to work.
        mWebView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.setWebViewClient(new WebViewClient() {
            // We want the WebView to continue to the URL even if it contains a redirection
            // and not open a new browser instance (which is the default handler)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            // For API level 24.
            // shouldOverrideUrlLoading was deprecated since 24
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            // The progressbar component is visible by default. We set it to invisible once we have
            // finished loading the page.
            // Note: this works when the url given to the webview does not include a redirection
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // hide progressbar
                mProgressbarLoading.setVisibility(View.INVISIBLE);
            }
        });

        mSignalsSlot = MainActivity.stateAdviceFragmentSignalSlot;

        mProgressbarLoading = mFragmentInfoView.findViewById(R.id.progressbar_loading);

        String webViewURL = (String) getArguments().getSerializable(
                Const.MAIN_ACTIVITY_ARTICLE_WEBVIEW_FRAGMENT_URL_ARG_KEY);
        mWebView.loadUrl(webViewURL);

        return mFragmentInfoView;
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "in onAttach()");
        super.onAttach(context);
        mContext = context;
        mActivity = (MainActivity) getActivity();
    }

    @Override
    public boolean onBackPressed() {
        if (mSignalsSlot != null && mSignalsSlot.containsValidSignals()) {
            mActivity.showAdviceFragment(mSignalsSlot);
        } else {
            // if the SignalsSlot is null or invalid we fallback to the HomeFragment
            mActivity.onBottomNavigateTo(R.id.bottom_nav_home);
        }
        return true;
    }
}
