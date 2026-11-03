package cl.duoc.dsy1102.timecapsule.model;

public class MensajeFuturo {
    private String autor;
    private String texto;
    private int plazoAnios;

    // Jackson necesita poder reconstruir objetos al leer JSON.
    public MensajeFuturo() {
    }

    public MensajeFuturo(String autor, String texto, int plazoAnios) {
        this.autor = autor;
        this.texto = texto;
        this.plazoAnios = plazoAnios;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public int getPlazoAnios() {
        return plazoAnios;
    }

    public void setPlazoAnios(int plazoAnios) {
        this.plazoAnios = plazoAnios;
    }
}
