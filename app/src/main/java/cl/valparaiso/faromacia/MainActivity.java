package cl.valparaiso.faromacia;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity {

    private ListView listView;
    private EditText etBuscar;
    private FarmaciaAdapter adaptador;
    private List<Farmacia> listaFarmacias;
    private SwipeRefreshLayout swipeRefresh;
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final long REFRESH_INTERVAL_MS = 15 * 60 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listview_farmacias);
        etBuscar = findViewById(R.id.et_buscar_farmacia);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        listaFarmacias = new ArrayList<>();

        swipeRefresh.setOnRefreshListener(() -> cargarFarmaciasValparaiso());
        swipeRefresh.setColorSchemeResources(android.R.color.holo_blue_dark, android.R.color.holo_green_dark);

        FloatingActionButton fabLogout = findViewById(R.id.fab_logout);
        fabLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adaptador != null) {
                    adaptador.getFilter().filter(s);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        cargarFarmaciasValparaiso();
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = () -> {
            cargarFarmaciasValparaiso();
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    private void cargarFarmaciasValparaiso() {
        String direccion = "https://midas.minsal.cl/farmacia_v2/WS/getLocalesTurnos.php";

        new Thread(() -> {
            try {
                InetAddress.getByName("midas.minsal.cl");

                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() { return null; }
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                        }
                };
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                URL url = new URL(direccion);
                HttpsURLConnection conexion = (HttpsURLConnection) url.openConnection();
                conexion.setRequestMethod("GET");
                conexion.setConnectTimeout(15000);
                conexion.setReadTimeout(15000);
                conexion.connect();

                InputStream inputStream = conexion.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder resultado = new StringBuilder();
                String linea;

                while ((linea = reader.readLine()) != null) {
                    resultado.append(linea);
                }

                JSONArray arreglo = new JSONArray(resultado.toString());
                listaFarmacias.clear();

                for (int i = 0; i < arreglo.length(); i++) {
                    JSONObject objeto = arreglo.getJSONObject(i);
                    String comuna = objeto.optString("comuna_nombre", "");
                    String dia = objeto.optString("funcionamiento_dia", "");

                    if ((comuna.equalsIgnoreCase("VALPARAISO") || comuna.equalsIgnoreCase("VALPARA\u00cdSO"))
                            && esDiaActual(dia)) {
                        Farmacia farmacia = new Farmacia(
                                objeto.optString("local_nombre", "Sin Nombre"),
                                comuna,
                                objeto.optString("funcionamiento_hora_apertura", "--:--"),
                                objeto.optString("funcionamiento_hora_cierre", "--:--"),
                                objeto.optString("local_direccion", "Sin Direcci\u00f3n"),
                                objeto.optString("local_telefono", ""),
                                objeto.optString("local_lat", "0"),
                                objeto.optString("local_lng", "0")
                        );
                        listaFarmacias.add(farmacia);
                    }
                }

                runOnUiThread(() -> {
                    adaptador = new FarmaciaAdapter(MainActivity.this, listaFarmacias);
                    listView.setAdapter(adaptador);
                    swipeRefresh.setRefreshing(false);
                    if (listaFarmacias.isEmpty()) {
                        Toast.makeText(MainActivity.this, "No hay farmacias de turno en Valpara\u00edso", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(MainActivity.this, "Error: " + msg, Toast.LENGTH_LONG).show();
                });
            }
            }).start();
        }

    private boolean esDiaActual(String diaSemana) {
        if (diaSemana == null || diaSemana.isEmpty()) return false;

        String[] dias = {"domingo", "lunes", "martes", "mi\u00e9rcoles", "jueves", "viernes", "s\u00e1bado"};
        int todayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        String hoy = dias[todayIndex];
        return diaSemana.equalsIgnoreCase(hoy);
    }
}
