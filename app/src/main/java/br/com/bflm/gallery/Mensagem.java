package br.com.bflm.gallery;

import java.util.Date;

class Mensagem implements Comparable <Mensagem> {

    @Override
    public int compareTo(Mensagem mensagem) {
        return this.data.compareTo(mensagem.data);
    }

    private String texto;
    private Date data;
    private String email;
    private Boolean isImage;
    private String linkImage;

    public Mensagem() {
    }

    public Mensagem(String texto, Date data, String email) {
        this.texto = texto;
        this.data = data;
        this.email = email;
        this.isImage = false;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public Date getData() {
        return data;
    }

    public void setData(Date data) {
        this.data = data;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getIsImage() {
        return isImage;
    }

    public void setIsImage(Boolean image) {
        isImage = image;
    }

    public String getLinkImage() {
        return linkImage;
    }

    public void setLinkImage(String image) {
        linkImage = image;
    }
}
