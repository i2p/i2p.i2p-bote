package i2p.bote.android.intro;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import i2p.bote.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class IntroActivityTest {

    @Rule
    public ActivityTestRule<IntroActivity> mRule = new ActivityTestRule<>(IntroActivity.class);

    @Test
    public void enterSetup() {
        onView(withId(R.id.pager)).perform(swipeLeft(), swipeLeft(), swipeLeft(), swipeLeft(), swipeLeft());
        onView(withId(R.id.start_setup_wizard)).perform(click());
        // TODO check result
    }

    @Test
    public void closeIntro() {
        onView(withId(R.id.skip_intro)).perform(click());
        // TODO check result
    }
}