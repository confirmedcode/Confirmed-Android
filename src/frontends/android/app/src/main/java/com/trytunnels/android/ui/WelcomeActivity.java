package com.trytunnels.android.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.trytunnels.android.utils.SubscriptionUtil;

import org.json.JSONObject;
import org.strongswan.android.R;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WelcomeActivity extends AppCompatActivity {

    private static final int SIGN_IN_RESULT = 1;
    private static final int SIGN_UP_RESULT = 2;

    private ViewPager viewPager;
    private MyViewPagerAdapter myViewPagerAdapter;
    private LinearLayout dotsLayout;
    private TextView[] dots;
    private ArrayList<Integer> layouts;
    //private Button btnSkip, btnNext;
    private Button btnSignUp, btnSignIn;
    //private Button btnPurchase;
    private Button btnCloseWelcome;

    private SubscriptionUtil mSubscriptionUtil;

    private String androidPrice = "$4.99";
    private String unlimitedPrice = "$9.99";
    private String androidAnnualPrice = "$49.99";
    private String unlimitedAnnualPrice = "$99.99";
    private Double androidPriceBD = 4.99;
    private Double unlimitedPriceBD = 9.99;
    private Double androidAnnualPriceBD = 49.99;
    private Double unlimitedAnnualPriceBD = 99.99;
    private String androidCurrencyCode = "USD";
    private String unlimitedCurrencyCode = "USD";
    private String introPrice = "$0.99";

    private boolean hasSubscription = false;

    private static final String TAG = "WelcomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Making notification bar transparent
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        //getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().hide();

        setContentView(R.layout.tt_welcome_activity);

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        dotsLayout = (LinearLayout) findViewById(R.id.layoutDots);
        //btnSkip = (Button) findViewById(R.id.btn_skip);
        //btnNext = (Button) findViewById(R.id.btn_next);
        btnSignIn = (Button) findViewById(R.id.btn_goto_sign_in);
        btnSignUp = (Button) findViewById(R.id.btn_goto_sign_up);
        btnCloseWelcome = (Button) findViewById(R.id.btn_close_welcome);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            hasSubscription = extras.getBoolean("HAS_SUBSCRIPTION");
        }

        // layouts of all welcome sliders
        // add few more layouts if you want
        layouts = new ArrayList<>(Arrays.asList(R.layout.tt_welcome_screen1,
                R.layout.tt_welcome_screen2,
                R.layout.tt_welcome_screen3/*,
                R.layout.tt_welcome_screen4*/));

        if (!hasSubscription) {
            layouts.add(R.layout.tt_welcome_screen5);
            btnCloseWelcome.setVisibility(View.INVISIBLE);
            btnSignUp.setVisibility(View.VISIBLE);
            btnSignIn.setVisibility(View.VISIBLE);
        }
        else
        {
            btnCloseWelcome.setVisibility(View.VISIBLE);
            btnSignUp.setVisibility(View.INVISIBLE);
            btnSignIn.setVisibility(View.INVISIBLE);
        }

        // adding bottom dots
        addBottomDots(0);

        // making notification bar transparent
        changeStatusBarColor();

        mSubscriptionUtil = new SubscriptionUtil(this, new SubscriptionUtil.SubscriptionSetupListener() {
            @Override
            public void onSuccess() {
                getIAPPrices();
            }
        });

        myViewPagerAdapter = new MyViewPagerAdapter();
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signUpIntent = new Intent(v.getContext(), SignUpActivity.class);
                //welcomeIntent.putExtra("HAS_SUBSCRIPTION", true);
                signUpIntent.putExtra("UNLIMITED_PRICE", unlimitedPrice);
                signUpIntent.putExtra("UNLIMITED_CC", unlimitedCurrencyCode);
                signUpIntent.putExtra("UNLIMITED_PRICE_BD", unlimitedPriceBD);
                signUpIntent.putExtra("ANDROID_PRICE", androidPrice);
                signUpIntent.putExtra("ANDROID_CC", androidCurrencyCode);
                signUpIntent.putExtra("ANDROID_PRICE_BD", androidPriceBD);
                signUpIntent.putExtra("INTRO_PRICE", introPrice);
                signUpIntent.putExtra("UNLIMITED_ANNUAL_PRICE", unlimitedAnnualPrice);
                signUpIntent.putExtra("UNLIMITED_ANNUAL_PRICE_BD", unlimitedAnnualPriceBD);
                signUpIntent.putExtra("ANDROID_ANNUAL_PRICE", androidAnnualPrice);
                signUpIntent.putExtra("ANDROID_ANNUAL_PRICE_BD", androidAnnualPriceBD);
                startActivityForResult(signUpIntent, SIGN_UP_RESULT);
            }
        });

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = new Intent(v.getContext(), SignInActivity.class);
                startActivityForResult(signInIntent, SIGN_IN_RESULT);
            }
        });

        btnCloseWelcome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        /*btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchHomeScreen();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // checking for last page
                // if last page home screen will be launched
                int current = getItem(+1);
                if (current < layouts.size()) {
                    // move to next screen
                    viewPager.setCurrentItem(current);
                } else {
                    launchHomeScreen();
                }
            }
        });*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSubscriptionUtil != null) {
            mSubscriptionUtil.dispose();
        }
    }

    @Override
    public void onBackPressed()
    {
        if(hasSubscription)
        {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SIGN_IN_RESULT:
                if(resultCode==1)
                {
                    finish();
                }
                break;
            case SIGN_UP_RESULT:
                if(resultCode==1)
                {
                    setResult(1);
                    finish();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void getIAPPrices()
    {
        if(mSubscriptionUtil != null)
        {
            ArrayList<String> subTypes = new ArrayList<String>();
            subTypes.add(getString(R.string.tt_paid_unlimited_sub_key));
            subTypes.add(getString(R.string.tt_paid_sub_key));
            subTypes.add(getString(R.string.tt_paid_sub_key_annual));
            subTypes.add(getString(R.string.tt_paid_unlimited_sub_key_annual));

            mSubscriptionUtil.getSubInfo(subTypes, new SubscriptionUtil.SubscriptionListListener() {
                @Override
                public void onSuccess(ArrayList<JSONObject> resultList) {
                    for(int i=0;i<resultList.size();i++)
                    {
                        JSONObject sku = resultList.get(i);
                        if(sku.has("productId"))
                        {
                            try {
                                String productId = sku.getString("productId");
                                if(productId.equals(getString(R.string.tt_paid_unlimited_sub_key)))
                                {
                                    unlimitedPrice = sku.getString("price");
                                    unlimitedCurrencyCode = sku.getString("price_currency_code");
                                    unlimitedPriceBD = sku.getDouble("price_amount_micros") / 1000000.0;
                                    if(sku.has("introductoryPrice"))
                                    {
                                        introPrice = sku.getString("introductoryPrice");
                                    }
                                }
                                else if(productId.equals(getString(R.string.tt_paid_sub_key)))
                                {
                                    androidPrice = sku.getString("price");
                                    androidCurrencyCode = sku.getString("price_currency_code");
                                    androidPriceBD = sku.getDouble("price_amount_micros") / 1000000.0;
                                }
                                else if(productId.equals(getString(R.string.tt_paid_unlimited_sub_key_annual)))
                                {
                                    unlimitedAnnualPrice = sku.getString("price");
                                    unlimitedAnnualPriceBD = sku.getDouble("price_amount_micros") / 1000000.0;
                                }
                                else if(productId.equals(getString(R.string.tt_paid_sub_key_annual)))
                                {
                                    androidAnnualPrice = sku.getString("price");
                                    androidAnnualPriceBD = sku.getDouble("price_amount_micros") / 1000000.0;
                                }
                            }
                            catch(Exception e)
                            {

                            }
                        }
                    }
                }
            });
        }
    }

    private void addBottomDots(int currentPage) {
        dots = new TextView[layouts.size()];

        int colorActive = getResources().getColor(R.color.tt_welcome_dot_active);
        int colorInActive = getResources().getColor(R.color.tt_welcome_dot_inactive);

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(colorInActive);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0)
            dots[currentPage].setTextColor(colorActive);
    }

    private int getItem(int i) {
        return viewPager.getCurrentItem() + i;
    }

    private void launchHomeScreen() {
        finish();
    }

    private void launchPurchaseWorkflow() {
        mSubscriptionUtil.initSubscription("paid_sub", new SubscriptionUtil.SubscriptionFinishedListener() {
            @Override
            public void onSuccess() {
                //Do whatever you want on subscription success
                Toast.makeText(WelcomeActivity.this, "success", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //  viewpager change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);

            // changing the next button text 'NEXT' / 'GOT IT'
            /*if (position == layouts.size() - 1) {
                // last page. make button text to GOT IT
                btnNext.setText(getString(R.string.tt_welcome_start));
                btnSkip.setVisibility(View.GONE);
            } else {
                // still pages are left
                btnNext.setText(getString(R.string.tt_welcome_next));
                btnSkip.setVisibility(View.VISIBLE);
            }*/
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    };

    /**
     * Making notification bar transparent
     */
    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    /**
     * View pager adapter
     */
    public class MyViewPagerAdapter extends PagerAdapter {
        private LayoutInflater layoutInflater;

        public MyViewPagerAdapter() {
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = layoutInflater.inflate(layouts.get(position), container, false);

            /*btnPurchase = (Button) view.findViewById(R.id.btn_sign_up);
            if (btnPurchase != null) {
                btnPurchase.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        launchPurchaseWorkflow();
                    }
                });
            }*/

            container.addView(view);

            return view;
        }

        @Override
        public int getCount() {
            return layouts.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }
}