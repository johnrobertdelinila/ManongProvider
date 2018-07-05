package com.example.johnrobert.manongprovider;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.design.button.MaterialButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity implements NavigationHost {

    private View scrim;
    private Toolbar toolbar;
    private ImageView toolbarNavigationIcon;
    private Drawable closeIcon, openIcon;
    private String currentFragment = "Request";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        scrim = findViewById(R.id.scrim);
        closeIcon = getResources().getDrawable(R.drawable.manong_close_menu);
        openIcon = getResources().getDrawable(R.drawable.shr_branded_menu);

        setUpToolbar();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Add shape
            findViewById(R.id.service_grid).setBackground(getDrawable(R.drawable.manong_container_grid_background_shape));
            scrim.setBackground(getDrawable(R.drawable.manong_scrim_background_shape));
        }
        scrim.setAlpha(0);
        scrim.setClickable(false);

        // Set default Fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new JobFragment())
                    .commit();
        }

    }

    private void setUpToolbar() {
        toolbar = findViewById(R.id.toolbar);
        toolbarNavigationIcon = (ImageView) toolbar.getChildAt(1);

        this.setSupportActionBar(toolbar);

        final NavigationIconClickListener customNavigation = new NavigationIconClickListener(this, findViewById(R.id.service_grid),
                new AccelerateDecelerateInterpolator(),
                openIcon,
                closeIcon,
                scrim,
                findViewById(R.id.container));

        toolbar.setNavigationOnClickListener(customNavigation);

        final LinearLayout backdropContainer = findViewById(R.id.backdrop_container);

        final ArrayList<View> allButtons = backdropContainer.getTouchables();
        for(View navBtn: allButtons) {
            navBtn.setOnClickListener(view -> {
                customNavigation.onClick(toolbarNavigationIcon);
                setNewMarker(allButtons, backdropContainer, view);
                String btnText = (String) ((MaterialButton) view).getText();
                if (btnText.equalsIgnoreCase("jobs") && !currentFragment.equalsIgnoreCase(btnText)) {
                    navigateFragment(new JobFragment());
                    currentFragment = btnText;
                    toolbar.setTitle(btnText);
                }else if (btnText.equalsIgnoreCase("inbox") && !currentFragment.equalsIgnoreCase(btnText)) {
                    navigateFragment(new MessageFragment());
                    currentFragment = btnText;
                    toolbar.setTitle(btnText);
                }else if (btnText.equalsIgnoreCase("more") && !currentFragment.equalsIgnoreCase(btnText)) {
                    navigateFragment(new MoreFragment());
                    currentFragment = btnText;
                    toolbar.setTitle(btnText);
                }
            });
        }

        scrim.setOnClickListener(view -> customNavigation.onClick(toolbarNavigationIcon));

    }

    private void navigateFragment(final Fragment fragment) {
        int defaultDelay = 345;
        if (currentFragment.equalsIgnoreCase("services"))
            defaultDelay = 350;
        new Handler().postDelayed(() -> navigateTo(fragment, false), defaultDelay);
    }

    private void setNewMarker(ArrayList<View> allButtons, LinearLayout backdropContainer, View excluded) {
        for(View button: allButtons) {
            if (button != excluded) {
                backdropContainer.getChildAt(backdropContainer.indexOfChild(button) + 1).setVisibility(View.GONE);
            }else {
                backdropContainer.getChildAt(backdropContainer.indexOfChild(button) + 1).setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void navigateTo(Fragment fragment, boolean addToBackstack) {
        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction();

        if (addToBackstack) {
            transaction.addToBackStack(null);
        }
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        transaction.replace(R.id.container, fragment);
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manong_toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.profile:
                Intent intent = new Intent(HomeActivity.this, MyProfileActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
