package cl.valparaiso.faromacia;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class FarmaciaAdapter extends BaseAdapter implements Filterable {
    private Context context;
    private List<Farmacia> farmaciasOriginales;
    private List<Farmacia> farmaciasFiltradas;
    private CustomFilter filter;

    public FarmaciaAdapter(Context context, List<Farmacia> farmacias) {
        this.context = context;
        this.farmaciasOriginales = farmacias;
        this.farmaciasFiltradas = farmacias;
    }

    @Override
    public int getCount() { return farmaciasFiltradas.size(); }

    @Override
    public Object getItem(int position) { return farmaciasFiltradas.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_farmacia, parent, false);
        }

        Farmacia farmacia = farmaciasFiltradas.get(position);

        TextView tvNombre = convertView.findViewById(R.id.tv_nombre);
        TextView tvComuna = convertView.findViewById(R.id.tv_comuna);
        TextView tvHorario = convertView.findViewById(R.id.tv_horario);
        TextView tvEstado = convertView.findViewById(R.id.tv_estado);
        TextView tvDireccion = convertView.findViewById(R.id.tv_direccion);
        Button btnLlamar = convertView.findViewById(R.id.btn_llamar);
        Button btnMapa = convertView.findViewById(R.id.btn_mapa);

        tvNombre.setText(farmacia.getNombre());
        tvComuna.setText("Comuna: " + farmacia.getComuna());
        tvHorario.setText("Horario: " + farmacia.getApertura() + " - " + farmacia.getCierre());
        tvDireccion.setText("Direcci\u00f3n: " + farmacia.getDireccion());

        if (estaAbiertaAhora(farmacia.getApertura(), farmacia.getCierre())) {
            tvEstado.setText("\u25cf Abierto ahora");
            tvEstado.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            tvEstado.setText("\u25cf Cerrado");
            tvEstado.setTextColor(Color.parseColor("#F44336"));
        }

        String telefono = sanitizarTelefono(farmacia.getTelefono());
        if (telefono == null) {
            btnLlamar.setEnabled(false);
            btnLlamar.setAlpha(0.4f);
        } else {
            btnLlamar.setEnabled(true);
            btnLlamar.setAlpha(1f);
            btnLlamar.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + telefono));
                context.startActivity(intent);
            });
        }

        btnMapa.setOnClickListener(v -> {
            String uriStr = "geo:" + farmacia.getLatitud() + "," + farmacia.getLongitud() + "?q=" + Uri.encode(farmacia.getDireccion() + ", " + farmacia.getComuna());
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
            context.startActivity(intent);
        });

        return convertView;
    }

    private boolean estaAbiertaAhora(String apertura, String cierre) {
        try {
            Calendar now = Calendar.getInstance();
            int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

            String[] partsApertura = apertura.split(":");
            String[] partsCierre = cierre.split(":");

            int openMinutes = Integer.parseInt(partsApertura[0]) * 60 + Integer.parseInt(partsApertura[1]);
            int closeMinutes = Integer.parseInt(partsCierre[0]) * 60 + Integer.parseInt(partsCierre[1]);

            return nowMinutes >= openMinutes && nowMinutes < closeMinutes;
        } catch (Exception e) {
            return false;
        }
    }

    private String sanitizarTelefono(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) {
            return null;
        }
        String limpio = telefono.replaceAll("[^\\d]", "");
        if (limpio.isEmpty()) {
            return null;
        }
        if (limpio.startsWith("56") && limpio.length() > 9) {
            limpio = limpio.substring(2);
        }
        if (limpio.length() > 9) {
            limpio = limpio.substring(limpio.length() - 9);
        }
        if (limpio.length() >= 8) {
            return "+56" + limpio;
        }
        return null;
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new CustomFilter();
        }
        return filter;
    }

    private class CustomFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null || constraint.length() == 0) {
                results.values = farmaciasOriginales;
                results.count = farmaciasOriginales.size();
            } else {
                List<Farmacia> filteredList = new ArrayList<>();
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Farmacia f : farmaciasOriginales) {
                    if (f.getNombre().toLowerCase().contains(filterPattern)) {
                        filteredList.add(f);
                    }
                }
                results.values = filteredList;
                results.count = filteredList.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            farmaciasFiltradas = (List<Farmacia>) results.values;
            notifyDataSetChanged();
        }
    }
}
