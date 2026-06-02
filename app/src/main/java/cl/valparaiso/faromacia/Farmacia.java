package cl.valparaiso.faromacia;

public class Farmacia {
    private String nombre;
    private String comuna;
    private String apertura;
    private String cierre;
    private String direccion;
    private String telefono;
    private String latitud;
    private String longitud;

    public Farmacia(String nombre, String comuna, String apertura, String cierre, String direccion, String telefono, String latitud, String longitud) {
        this.nombre = nombre;
        this.comuna = comuna;
        this.apertura = apertura;
        this.cierre = cierre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getNombre() { return nombre; }
    public String getComuna() { return comuna; }
    public String getApertura() { return apertura; }
    public String getCierre() { return cierre; }
    public String getDireccion() { return direccion; }
    public String getTelefono() { return telefono; }
    public String getLatitud() { return latitud; }
    public String getLongitud() { return longitud; }
}
