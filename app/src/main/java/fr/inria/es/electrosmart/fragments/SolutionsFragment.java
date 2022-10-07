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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import java.util.Locale;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.FeedbackActivity;
import fr.inria.es.electrosmart.activities.MainActivity;
import fr.inria.es.electrosmart.database.DbRequestHandler;

public class SolutionsFragment extends MainActivityFragment {

    private static final String TAG = "SolutionsFragment";

    // The current context
    private Context mContext;

    private MainActivity mActivity;

    /**
     * Creates a new instance of the fragment to which we pass {@code arguments} as arguments bundle.
     *
     * @param arguments the arguments bundle to pass to the newly created fragment
     * @return the created fragment
     */
    public static MainActivityFragment newInstance(Bundle arguments) {
        MainActivityFragment fragment = new SolutionsFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_solutions, container, false);

        // Owayn specific code
        View testOwaynTextView = view.findViewById(R.id.button_solutions_owayn_test);
        testOwaynTextView.setOnClickListener(
                v -> {
                    DbRequestHandler.dumpEventToDatabase(Const.EVENT_SOLUTIONS_OWAYN_TEST_CLICKED);
                    mActivity.onTestOwayn();
                }
        );
        View learnMoreOwaynView = view.findViewById(R.id.block_owayn);
        learnMoreOwaynView.setOnClickListener(v -> {
            DbRequestHandler.dumpEventToDatabase(Const.EVENT_SOLUTIONS_OWAYN_LEARN_MORE_CLICKED);
            showSolutionInBrowser(Const.SOLUTION_OWAYN, DbRequestHandler.getProfileIdFromDB());
        });

        // Sybox specific code
        View testSyboxTextView = view.findViewById(R.id.button_solutions_sybox_test);
        testSyboxTextView.setOnClickListener(
                v -> {
                    DbRequestHandler.dumpEventToDatabase(Const.EVENT_SOLUTIONS_SYBOX_TEST_CLICKED);
                    Toast.makeText(mContext, R.string.solution_test_coming_soon_toast,
                            Toast.LENGTH_SHORT).show();
                }
        );

        View learnMoreSyboxView = view.findViewById(R.id.block_sybox);
        learnMoreSyboxView.setOnClickListener(v -> {
            DbRequestHandler.dumpEventToDatabase(Const.EVENT_SOLUTIONS_SYBOX_LEARN_MORE_CLICKED);
            showSolutionInBrowser(Const.SOLUTION_SYBOX, DbRequestHandler.getProfileIdFromDB());
        });

        View suggestSolutionView = view.findViewById(R.id.solutions_suggest_view);
        suggestSolutionView.setOnClickListener(
                v -> {
                    DbRequestHandler.dumpEventToDatabase(Const.EVENT_SOLUTIONS_SUGGEST_CLICKED);
                    Intent intent = new Intent(mContext,
                            FeedbackActivity.class);
                    intent.putExtra(Const.FEEDBACK_DEFAULT_TYPE_EXTRA_KEY,
                            Const.FEEDBACK_DEFAULT_EXTRA_VALUE_SUGGEST_SOLUTION);
                    startActivity(intent);
                }
        );


//        // We hide the sybox block if it is in a country in which they does not sell
//        View syboxSeparator = view.findViewById(R.id.solutions_separator3);
//        if (Tools.checkIfUserIsInCountry("fr") ||
//                Tools.checkIfUserIsInCountry("at") ||
//                Tools.checkIfUserIsInCountry("be") ||
//                Tools.checkIfUserIsInCountry("dk") ||
//                Tools.checkIfUserIsInCountry("de") ||
//                Tools.checkIfUserIsInCountry("hu") ||
//                Tools.checkIfUserIsInCountry("it") ||
//                Tools.checkIfUserIsInCountry("lu") ||
//                Tools.checkIfUserIsInCountry("nl") ||
//                Tools.checkIfUserIsInCountry("no") ||
//                Tools.checkIfUserIsInCountry("pt") ||
//                Tools.checkIfUserIsInCountry("es") ||
//                Tools.checkIfUserIsInCountry("se") ||
//                Tools.checkIfUserIsInCountry("ch") ||
//                Tools.checkIfUserIsInCountry("li")) {
//            learnMoreSyboxView.setVisibility(View.VISIBLE);
//            testSyboxTextView.setVisibility(View.VISIBLE);
//            syboxSeparator.setVisibility(View.VISIBLE);
//        } else {
//            learnMoreSyboxView.setVisibility(View.GONE);
//            testSyboxTextView.setVisibility(View.GONE);
//            syboxSeparator.setVisibility(View.GONE);
//        }


        // This code is for old android devices (4.1, galaxy S) which seem to set the background
        // color of the svg image to blue by default, even though in the layout we have specified
        // it as gray_100
        View suggestImageView = view.findViewById(R.id.image_solutions_suggest);
        suggestImageView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));

        return view;
    }

    /**
     * Show the solution Web site in a WebView within the app.
     * <p>
     * In this process, we asynchronously notify our server that the user is visiting the
     * solution Web site. We record the user unique Id and the solution name. This way we have
     * statistics for each solution Web site visit that we can use later to attribute purchases.
     *
     * @param solution The solution that the user wants to see
     * @param uniqueId The identifier of the user
     */
    private void showSolutionInBrowser(String solution, String uniqueId) {
        CustomTabsIntent intent = new CustomTabsIntent.Builder().
                setToolbarColor(ContextCompat.getColor(mContext, R.color.regular_background_color)).build();
        String url = "";
        try {
            // Save asynchronously the request on our server
            // new Tools.RecordUserIsVisitingSolutionWebSiteAsyncTask(solution, uniqueId).execute();

            // build the correct Web site URL according to the solution
            switch (solution) {
                case Const.SOLUTION_OWAYN:
                    url = Const.SOLUTION_OWAYN_URL;
                    break;
                case Const.SOLUTION_SYBOX:
                    if (Tools.getDefaultDeviceLocale().equals(Locale.FRENCH)) {
                        url = Const.SOLUTION_SYBOX_URL_FR;
                    } else {
                        url = Const.SOLUTION_SYBOX_URL_EN;
                    }
                    break;
            }

            if (!url.isEmpty()) {
                // Open a new browser instance
                intent.launchUrl(mContext, Uri.parse(url));
            }
        } catch (ActivityNotFoundException ex) {
            Tools.createInformationalDialog(mContext,
                    mContext.getString(R.string.solutions_no_browser_error_title),
                    mContext.getString(R.string.solutions_no_browser_error_text, url));
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = (MainActivity) getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "in onResume()");
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_SOLUTIONS_FRAGMENT_ON_RESUME);

        // we force the correct selection of the item in the drawer and bottom navigation bar
        mActivity.selectAndGetNavItem(R.id.nav_home);
        mActivity.selectAndGetBottomNavItem(R.id.bottom_nav_solutions);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "in onPause()");
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_SOLUTIONS_FRAGMENT_ON_PAUSE);
    }

    @Override
    public boolean onBackPressed() {
        mActivity.onBottomNavigateTo(R.id.bottom_nav_home);
        return true;
    }

    /**
     * A listener interface that is used to implement the behavior to be executed
     * for the owayn test.
     */
    public interface OnTestOwaynListener {
        void onTestOwayn();
    }
}
