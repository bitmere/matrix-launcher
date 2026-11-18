package com.matrix.launcher;

import android.app.Activity;
import android.os.Bundle;

/**
 * Answers com.android.tv.action.VIEW_INPUTS, the system input switcher that the remote's Input button turns into
 * (through the TV's Live TV app, or InputKeyService), by bringing Home forward with its Inputs menu open. The
 * component is disabled in the manifest and HomeKeyService enables it only while handle_view_inputs is set,
 * so a TV with its own input picker is never offered a chooser. No UI of its own (Theme.NoDisplay).
 */
public class InputsRequestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(HomeRequest.intent(this, HomeRequest.ACTION_VIEW_INPUTS));
        finish();
    }
}
