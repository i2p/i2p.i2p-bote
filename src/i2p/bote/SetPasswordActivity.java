package i2p.bote;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SetPasswordActivity extends ActionBarActivity {
    TextView error;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_password);

        // Enable ActionBar app icon to behave as action to go back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final EditText oldField = (EditText) findViewById(R.id.password_old);
        final EditText newField = (EditText) findViewById(R.id.password_new);
        final EditText confirmField = (EditText) findViewById(R.id.password_confirm);
        final Button b = (Button) findViewById(R.id.submit_password);
        error = (TextView) findViewById(R.id.error);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String oldPassword = oldField.getText().toString();
                String newPassword = newField.getText().toString();
                String confirmNewPassword = confirmField.getText().toString();

                I2PBote.getInstance().changePasswordAsync(
                        oldPassword.getBytes(),
                        newPassword.getBytes(),
                        confirmNewPassword.getBytes());

                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(newField.getWindowToken(), 0);

                new PasswordWaiter().execute();
            }
        });
    }

    private class PasswordWaiter extends AsyncTask<Void, Void, String> {
        private final ProgressDialog dialog = new ProgressDialog(SetPasswordActivity.this);

        protected void onPreExecute() {
            dialog.setMessage("Changing password...");
            dialog.setCancelable(false);
            dialog.show();
        }

        protected String doInBackground(Void... params) {
            try {
                I2PBote.getInstance().waitForPasswordChange();
                return null;
            } catch (Throwable e) {
                cancel(false);
                return e.getMessage();
            }
        }

        protected void onCancelled(String result) {
            error.setText(result);
            dialog.dismiss();
        }

        protected void onPostExecute(String result) {
            dialog.dismiss();
            // Password changed successfully
            finish();
        }
    }
}
