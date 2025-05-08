package it.unisalento.pas2425.userserviceproject.dto;public class RegistrationResultDTO {

    public final static int OK = 0;

    public final static int EMAIL_ALREADY_EXIST = 1;
    //AGGIUNGERE ALTRI CODICI ERRORE (ES: DATI MANCANTI)

    public final static int GENERIC_ERROR = 99;

    private int result;

    private String message;

    private UserDTO user;

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }
}
