package it.unisalento.pas2425.userserviceproject.dto;

import it.unisalento.pas2425.userserviceproject.domain.Role;

public class UserDTO {

    private String id;
    private String nome;
    private String cognome;
    private String email;
    private String password;
    private Role ruolo;

    public Role getRuolo() {
        return ruolo;
    }
    public void setRuolo(Role ruolo) {
        this.ruolo = ruolo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
