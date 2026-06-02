package cl.valparaiso.faromacia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword;
    private Button btnReenviar, btnRegister;
    private LinearLayout layoutFormulario, layoutVerificacion;
    private VideoView videoBackground;
    private FirebaseAuth mAuth;
    private CountDownTimer resendTimer;
    private Handler verificationHandler;
    private Runnable verificationRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        try {
            mAuth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            Toast.makeText(this, "Error: Google Play Services no disponible", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        etEmail = findViewById(R.id.et_email_register);
        etPassword = findViewById(R.id.et_password_register);
        etConfirmPassword = findViewById(R.id.et_confirm_password_register);
        btnRegister = findViewById(R.id.btn_register);
        btnReenviar = findViewById(R.id.btn_reenviar_correo);
        layoutFormulario = findViewById(R.id.layout_formulario);
        layoutVerificacion = findViewById(R.id.layout_verificacion);
        videoBackground = findViewById(R.id.video_bg_register);

        try {
            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.fondologin);
            videoBackground.setVideoURI(uri);
            videoBackground.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mp.setVolume(0, 0);
                float videoRatio = mp.getVideoWidth() / (float) mp.getVideoHeight();
                float screenRatio = videoBackground.getWidth() / (float) videoBackground.getHeight();
                float scaleX = videoRatio / screenRatio;
                if (scaleX >= 1f) videoBackground.setScaleX(scaleX);
                else videoBackground.setScaleY(1f / scaleX);
            });
            videoBackground.start();
        } catch (Exception e) {
            videoBackground.setVisibility(android.view.View.GONE);
        }

        btnRegister.setOnClickListener(v -> attemptRegister());
        btnReenviar.setOnClickListener(v -> sendVerificationEmail());

        TextView tvGoToLogin = findViewById(R.id.tv_go_to_login);
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Requerido");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Mínimo 6 caracteres");
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("No coinciden");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        layoutFormulario.setVisibility(View.GONE);
                        layoutVerificacion.setVisibility(View.VISIBLE);
                        sendVerificationEmail();
                        startVerificationPolling();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error al registrar usuario", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    startResendTimer();
                } else {
                    Toast.makeText(RegisterActivity.this, "Fallo al enviar correo de verificación", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void startVerificationPolling() {
        verificationHandler = new Handler();
        verificationRunnable = new Runnable() {
            @Override
            public void run() {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    user.reload().addOnCompleteListener(task -> {
                        if (user.isEmailVerified()) {
                            if (resendTimer != null) resendTimer.cancel();
                            verificationHandler.removeCallbacks(verificationRunnable);
                            Toast.makeText(RegisterActivity.this, "Cuenta verificada", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            verificationHandler.postDelayed(this, 3000);
                        }
                    });
                }
            }
        };
        verificationHandler.post(verificationRunnable);
    }

    private void startResendTimer() {
        if (resendTimer != null) resendTimer.cancel();
        btnReenviar.setEnabled(false);
        resendTimer = new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long ms) {
                btnReenviar.setText("Reenviar (" + (ms / 1000) + "s)");
            }
            @Override
            public void onFinish() {
                btnReenviar.setEnabled(true);
                btnReenviar.setText("Reenviar Correo");
            }
        }.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoBackground != null) videoBackground.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoBackground != null) videoBackground.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) resendTimer.cancel();
        if (verificationHandler != null && verificationRunnable != null) {
            verificationHandler.removeCallbacks(verificationRunnable);
        }
    }
}